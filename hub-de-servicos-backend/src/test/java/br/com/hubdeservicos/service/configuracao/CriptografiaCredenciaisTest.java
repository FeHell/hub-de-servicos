package br.com.hubdeservicos.service.configuracao;

import br.com.hubdeservicos.service.configuracao.CriptografiaCredenciais.CredencialIndisponivel;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CriptografiaCredenciaisTest {
    private static final String CONTEXTO = "INFOBIP:INFOBIP_API_KEY";
    private static final String SEGREDO = "credencial-ficticia";
    private final CriptografiaCredenciais protecao = new CriptografiaCredenciais(novaChave());

    @Test
    void autenticaContextoEUsaNonceDiferenteACadaGravacao() {
        String selado = protecao.criptografar(CONTEXTO, SEGREDO);
        assertThat(selado).doesNotContain(SEGREDO).isNotEqualTo(protecao.criptografar(CONTEXTO, SEGREDO));
        assertThat(protecao.descriptografar(CONTEXTO, selado)).isEqualTo(SEGREDO);
        assertThatThrownBy(() -> protecao.descriptografar("STONE:STONE_API_KEY", selado))
                .isInstanceOf(CredencialIndisponivel.class).hasMessageNotContaining(selado);
        assertThatThrownBy(() -> {
            var criptografiaCredenciaisPreparado = new CriptografiaCredenciais(novaChave());
            criptografiaCredenciaisPreparado.descriptografar(CONTEXTO, selado);
        }).isInstanceOf(CredencialIndisponivel.class);
        byte[] alterado = Base64.getDecoder().decode(selado.substring(3));
        alterado[alterado.length - 1] ^= 1;
        assertThatThrownBy(
                () -> protecao.descriptografar(CONTEXTO, "v1:" + Base64.getEncoder().encodeToString(alterado)))
                .isInstanceOf(CredencialIndisponivel.class);
    }

    @Test
    void falhaComSegurancaQuandoAChaveMestraNaoFoiConfigurada() {
        for (String ausente : new String[] { "", "chave-invalida" }) {
            assertThatThrownBy(() -> {
                var criptografiaCredenciaisPreparado2 = new CriptografiaCredenciais(ausente);
                criptografiaCredenciaisPreparado2.criptografar(CONTEXTO, SEGREDO);
            }).isInstanceOf(CredencialIndisponivel.class).hasMessageNotContaining(SEGREDO);
        }
    }

    private static String novaChave() {
        byte[] chave = new byte[32];
        var secureRandomPreparado = new SecureRandom();
        secureRandomPreparado.nextBytes(chave);
        return Base64.getEncoder().encodeToString(chave);
    }
}
