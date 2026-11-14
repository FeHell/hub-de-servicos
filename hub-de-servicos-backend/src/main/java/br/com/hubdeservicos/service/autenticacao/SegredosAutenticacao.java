package br.com.hubdeservicos.service.autenticacao;

import br.com.hubdeservicos.entity.aplicacao.AplicacaoEntity;
import br.com.hubdeservicos.entity.aplicacao.TipoAplicacao;
import br.com.hubdeservicos.config.autenticacao.ConfiguracaoAutenticacao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
class SegredosAutenticacao {

    private static final Pattern SEGREDO = Pattern.compile("[A-Za-z0-9_-]{43}");
    private final SecureRandom aleatorio = new SecureRandom();
    private final ConfiguracaoAutenticacao configuracao;

    SegredosAutenticacao(ConfiguracaoAutenticacao configuracao) {
        this.configuracao = configuracao;
    }

    String gerarTokenAtualizacao() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    boolean autorizaCliente(AplicacaoEntity aplicacao, String segredo) {
        if (aplicacao.getTipo() == TipoAplicacao.PUBLICO) {
            return segredo == null;
        }

        String hashCadastrado = configuracao.clientesConfidenciais().get(aplicacao.getId());
        return hashCadastrado != null && segredo != null && SEGREDO.matcher(segredo).matches()
                && MessageDigest.isEqual(HexFormat.of().parseHex(hashCadastrado), sha256(segredo));
    }

    String hash(String segredo) {
        return HexFormat.of().formatHex(sha256(segredo));
    }

    private byte[] sha256(String segredo) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(segredo.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("SHA-256 indisponível.", erro);
        }
    }
}
