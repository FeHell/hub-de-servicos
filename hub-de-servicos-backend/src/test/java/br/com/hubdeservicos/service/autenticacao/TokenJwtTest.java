package br.com.hubdeservicos.service.autenticacao;

import br.com.hubdeservicos.entity.aplicacao.AplicacaoEntity;
import br.com.hubdeservicos.entity.aplicacao.TipoAplicacao;
import br.com.hubdeservicos.config.autenticacao.ConfiguracaoAutenticacao;
import br.com.hubdeservicos.exception.autenticacao.AutenticacaoNegadaException;
import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.entity.sessao.SessaoEntity;
import br.com.hubdeservicos.entity.sessao.StatusSessao;

import com.nimbusds.jose.jwk.JWKSet;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenJwtTest {

    private static final Instant AGORA = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void validaJwtEPublicaSomenteMaterialPublico() throws Exception {
        TokenJwt jwt = jwtLocal();
        SessaoEntity sessao = sessao();
        String token = jwt.emitir(sessao, "portal", AGORA, AGORA.plusSeconds(300));

        assertThat(jwt.validar(token)).isEqualTo(new IdentidadeJwt(
                sessao.getIdUsuario(), sessao.getIdAplicacao(), sessao.getId(), "portal"));
        var chave = JWKSet.parse(jwt.chavesPublicas()).getKeys().getFirst();
        assertThat(chave.isPrivate()).isFalse();
        assertThat(chave.getAlgorithm().getName()).isEqualTo("RS256");
    }

    @Test
    void rejeitaTokenExpiradoOuComAudienciaAdulterada() {
        TokenJwt jwt = jwtLocal();
        SessaoEntity sessao = sessao();
        String expirado = jwt.emitir(sessao, "portal", AGORA.minusSeconds(300), AGORA);
        String[] partes = jwt.emitir(sessao, "portal", AGORA, AGORA.plusSeconds(300)).split("\\.");
        String claims = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        partes[1] = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(claims.replace("portal", "outra-aplicacao").getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> jwt.validar(expirado)).isInstanceOf(AutenticacaoNegadaException.class);
        assertThatThrownBy(() -> jwt.validar(String.join(".", partes))).isInstanceOf(AutenticacaoNegadaException.class);
    }

    @Test
    void clienteConfidencialExigeSeuProprioSegredo() {
        AplicacaoEntity confidencial = new AplicacaoEntity("Servidor", "servidor", TipoAplicacao.CONFIDENCIAL, null,
                StatusCadastro.ATIVO);
        UUID id = UUID.randomUUID();
        ReflectionTestUtils.setField(confidencial, "id", id);
        AplicacaoEntity publica = new AplicacaoEntity("Portal", "portal", TipoAplicacao.PUBLICO, null, StatusCadastro.ATIVO);
        var semCredencial = new SegredosAutenticacao(new ConfiguracaoAutenticacao(null, null, null, Map.of()));
        String segredo = semCredencial.gerarTokenAtualizacao();
        var credenciais = new SegredosAutenticacao(new ConfiguracaoAutenticacao(null, null, null,
                Map.of(id, semCredencial.hash(segredo))));

        assertThat(Base64.getUrlDecoder().decode(segredo)).hasSize(32);
        assertThat(credenciais.autorizaCliente(confidencial, segredo)).isTrue();
        assertThat(credenciais.autorizaCliente(confidencial, null)).isFalse();
        assertThat(credenciais.autorizaCliente(confidencial, semCredencial.gerarTokenAtualizacao())).isFalse();
        assertThat(credenciais.autorizaCliente(publica, null)).isTrue();
        assertThat(credenciais.autorizaCliente(publica, segredo)).isFalse();
    }

    private static TokenJwt jwtLocal() {
        return new TokenJwt(new ConfiguracaoAutenticacao(null, null, null, Map.of()),
                new MockEnvironment().withProperty("spring.profiles.active", "local"),
                Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private static SessaoEntity sessao() {
        SessaoEntity sessao = new SessaoEntity(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                StatusSessao.ATIVO, AGORA.plusSeconds(3600), AGORA.plusSeconds(1800), AGORA);
        ReflectionTestUtils.setField(sessao, "id", UUID.randomUUID());
        return sessao;
    }
}
