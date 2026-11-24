package br.com.hubdeservicos.service.configuracao;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CriptografiaCredenciais {
    private static final String VERSAO = "v1:";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private final SecureRandom aleatorio = new SecureRandom();
    private final SecretKeySpec chave;

    public CriptografiaCredenciais(@Value("${HUB_CONFIGURACAO_CHAVE_MESTRA:}") String configurada) {
        this.chave = carregar(configurada);
    }

    public String criptografar(String contexto, String valor) {
        byte[] nonce = new byte[NONCE_BYTES];
        aleatorio.nextBytes(nonce);
        byte[] aberto = valor.getBytes(StandardCharsets.UTF_8);
        try {
            byte[] selado = cifra(Cipher.ENCRYPT_MODE, contexto, nonce).doFinal(aberto);
            return VERSAO + Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + selado.length)
                    .put(nonce).put(selado).array());
        } catch (GeneralSecurityException erro) {
            throw new CredencialIndisponivel();
        } finally {
            Arrays.fill(aberto, (byte) 0);
        }
    }

    public String descriptografar(String contexto, String valor) {
        if (!valor.startsWith(VERSAO)) throw new CredencialIndisponivel();
        try {
            byte[] dados = Base64.getDecoder().decode(valor.substring(VERSAO.length()));
            if (dados.length <= NONCE_BYTES + TAG_BITS / 8) throw new CredencialIndisponivel();
            byte[] nonce = Arrays.copyOf(dados, NONCE_BYTES);
            byte[] aberto = cifra(Cipher.DECRYPT_MODE, contexto, nonce)
                    .doFinal(dados, NONCE_BYTES, dados.length - NONCE_BYTES);
            try {
                return new String(aberto, StandardCharsets.UTF_8);
            } finally {
                Arrays.fill(aberto, (byte) 0);
            }
        } catch (GeneralSecurityException | IllegalArgumentException erro) {
            throw new CredencialIndisponivel();
        }
    }

    public String identificadorProtegido(String contexto, String valor) {
        if (chave == null) throw new CredencialIndisponivel();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chave.getEncoded(), mac.getAlgorithm()));
            byte[] derivada = mac.doFinal(("identificador:v1:" + contexto).getBytes(StandardCharsets.UTF_8));
            try {
                mac.init(new SecretKeySpec(derivada, mac.getAlgorithm()));
                return HexFormat.of().formatHex(mac.doFinal(valor.getBytes(StandardCharsets.UTF_8)));
            } finally {
                Arrays.fill(derivada, (byte) 0);
            }
        } catch (GeneralSecurityException erro) {
            throw new CredencialIndisponivel();
        }
    }

    private Cipher cifra(int modo, String contexto, byte[] nonce) throws GeneralSecurityException {
        if (chave == null) throw new CredencialIndisponivel();
        Cipher cifra = Cipher.getInstance("AES/GCM/NoPadding");
        cifra.init(modo, chave, new GCMParameterSpec(TAG_BITS, nonce));
        cifra.updateAAD(contexto.getBytes(StandardCharsets.UTF_8));
        return cifra;
    }

    private static SecretKeySpec carregar(String configurada) {
        try {
            byte[] dados = Base64.getDecoder().decode(configurada.strip());
            try {
                return dados.length == 32 ? new SecretKeySpec(dados, "AES") : null;
            } finally {
                Arrays.fill(dados, (byte) 0);
            }
        } catch (IllegalArgumentException erro) {
            return null;
        }
    }

    public static final class CredencialIndisponivel extends RuntimeException {
        public CredencialIndisponivel() {
            super("Não foi possível acessar a credencial protegida.");
        }
    }
}
