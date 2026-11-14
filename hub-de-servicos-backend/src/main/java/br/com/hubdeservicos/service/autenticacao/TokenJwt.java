package br.com.hubdeservicos.service.autenticacao;

import br.com.hubdeservicos.config.autenticacao.ConfiguracaoAutenticacao;
import br.com.hubdeservicos.exception.autenticacao.AutenticacaoNegadaException;
import br.com.hubdeservicos.entity.sessao.SessaoEntity;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class TokenJwt {

    static final Duration VALIDADE = Duration.ofMinutes(5);
    private static final JOSEObjectType TIPO = new JOSEObjectType("at+jwt");

    private final String emissor;
    private final Clock relogio;
    private final RSAKey chave;
    private final RSASSASigner assinador;
    private final RSASSAVerifier verificador;

    public TokenJwt(ConfiguracaoAutenticacao configuracao, Environment ambiente, Clock relogio) {
        boolean local = ambiente.acceptsProfiles(Profiles.of("local"));
        this.emissor = validarEmissor(configuracao.emissor(), local);
        this.relogio = relogio;

        try {
            KeyPair par = carregarChaves(configuracao, local);
            if (!(par.getPrivate() instanceof RSAPrivateCrtKey privada)
                    || !(par.getPublic() instanceof RSAPublicKey publica)
                    || publica.getModulus().bitLength() < 2048
                    || !publica.getModulus().equals(privada.getModulus())
                    || !publica.getPublicExponent().equals(privada.getPublicExponent())) {
                throw new IllegalArgumentException("Configure um par RSA correspondente de, no mínimo, 2048 bits.");
            }

            chave = new RSAKey.Builder(publica)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyIDFromThumbprint()
                    .build();
            assinador = new RSASSASigner(privada);
            verificador = new RSASSAVerifier(publica);
        } catch (IOException | GeneralSecurityException | JOSEException | IllegalArgumentException erro) {
            throw new IllegalStateException("Não foi possível inicializar a assinatura JWT. Confira o par de chaves RSA.");
        }
    }

    String emitir(SessaoEntity sessao, String audiencia, Instant agora, Instant expiracao) {
        var cabecalho = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(TIPO)
                .keyID(chave.getKeyID())
                .build();
        var claims = new JWTClaimsSet.Builder()
                .issuer(emissor)
                .subject(sessao.getIdUsuario().toString())
                .audience(audiencia)
                .issueTime(Date.from(agora))
                .expirationTime(Date.from(expiracao))
                .jwtID(UUID.randomUUID().toString())
                .claim("client_id", sessao.getIdAplicacao().toString())
                .claim("sid", sessao.getId().toString())
                .build();
        var jwt = new SignedJWT(cabecalho, claims);
        try {
            jwt.sign(assinador);
            return jwt.serialize();
        } catch (JOSEException erro) {
            throw new IllegalStateException("Falha ao assinar o token de acesso.", erro);
        }
    }

    public IdentidadeJwt validar(String token) {
        if (token.length() > 4096) {
            throw new AutenticacaoNegadaException();
        }
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            JWSHeader cabecalho = jwt.getHeader();
            if (!JWSAlgorithm.RS256.equals(cabecalho.getAlgorithm())
                    || !TIPO.equals(cabecalho.getType())
                    || !chave.getKeyID().equals(cabecalho.getKeyID())
                    || !jwt.verify(verificador)) {
                throw new AutenticacaoNegadaException();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Instant agora = relogio.instant();
            Date emitido = claims.getIssueTime();
            Date expira = claims.getExpirationTime();
            Date inicio = claims.getNotBeforeTime();
            if (!emissor.equals(claims.getIssuer())
                    || claims.getAudience().size() != 1 || claims.getAudience().getFirst().isBlank()
                    || emitido == null || expira == null || !expira.after(emitido)
                    || emitido.toInstant().isAfter(agora)
                    || !expira.toInstant().isAfter(agora)
                    || expira.toInstant().isAfter(emitido.toInstant().plus(VALIDADE))
                    || (inicio != null && inicio.toInstant().isAfter(agora))) {
                throw new AutenticacaoNegadaException();
            }
            return new IdentidadeJwt(
                    lerUuid(claims.getSubject()),
                    lerUuid(claims.getStringClaim("client_id")),
                    lerUuid(claims.getStringClaim("sid")),
                    claims.getAudience().getFirst());
        } catch (ParseException | JOSEException | IllegalArgumentException erro) {
            throw new AutenticacaoNegadaException();
        }
    }

    public Map<String, Object> chavesPublicas() {
        return new JWKSet(chave).toJSONObject();
    }

    private static UUID lerUuid(String valor) {
        if (valor == null || valor.length() != 36) {
            throw new AutenticacaoNegadaException();
        }
        return UUID.fromString(valor);
    }

    private static String validarEmissor(URI valor, boolean local) {
        URI uri = valor == null && local ? URI.create("http://localhost:8080") : valor;
        if (uri == null || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getRawQuery() != null || uri.getRawFragment() != null
                || (!"https".equalsIgnoreCase(uri.getScheme()) && !(local && "http".equalsIgnoreCase(uri.getScheme())))) {
            throw new IllegalStateException("Configure hub.autenticacao.emissor com a URL HTTPS do Hub de Serviços.");
        }
        return uri.toString();
    }

    private static KeyPair carregarChaves(ConfiguracaoAutenticacao configuracao, boolean local)
            throws IOException, GeneralSecurityException {
        Path privada = configuracao.chavePrivada();
        Path publica = configuracao.chavePublica();
        if (privada == null && publica == null && local) {
            KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
            gerador.initialize(2048);
            return gerador.generateKeyPair();
        }
        if (privada == null || publica == null) {
            throw new IllegalStateException("Configure os arquivos PEM de chave privada e chave pública do JWT.");
        }
        KeyFactory fabrica = KeyFactory.getInstance("RSA");
        return new KeyPair(
                fabrica.generatePublic(new X509EncodedKeySpec(lerPem(publica, "PUBLIC KEY"))),
                fabrica.generatePrivate(new PKCS8EncodedKeySpec(lerPem(privada, "PRIVATE KEY"))));
    }

    private static byte[] lerPem(Path arquivo, String tipo) throws IOException {
        String conteudo = Files.readString(arquivo).strip();
        String inicio = "-----BEGIN " + tipo + "-----";
        String fim = "-----END " + tipo + "-----";
        if (!conteudo.startsWith(inicio) || !conteudo.endsWith(fim)) {
            throw new IllegalArgumentException("Formato PEM incorreto.");
        }
        String base64 = conteudo.substring(inicio.length(), conteudo.length() - fim.length());
        return Base64.getDecoder().decode(base64.replaceAll("\\s", ""));
    }
}
