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

        var identidadeJwtPreparado = new IdentidadeJwtVO(sessao.getIdUsuario(), sessao.getIdAplicacao(), sessao.getId(),
                "portal");
        assertThat(jwt.validar(token)).isEqualTo(identidadeJwtPreparado);
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
        AplicacaoEntity publica = new AplicacaoEntity("Portal", "portal", TipoAplicacao.PUBLICO, null,
                StatusCadastro.ATIVO);
        var configuracaoAutenticacaoPreparado = new ConfiguracaoAutenticacao(null, null, null, Map.of());
        var argumentoPreparado = configuracaoAutenticacaoPreparado;
        var segredosAutenticacaoPreparado = new SegredosAutenticacao(argumentoPreparado);
        var semCredencial = segredosAutenticacaoPreparado;
        String segredo = semCredencial.gerarTokenAtualizacao();
        var configuracaoAutenticacaoPreparado2 = new ConfiguracaoAutenticacao(null, null, null,
                Map.of(id, semCredencial.hash(segredo)));
        var argumentoPreparado2 = configuracaoAutenticacaoPreparado2;
        var segredosAutenticacaoPreparado2 = new SegredosAutenticacao(argumentoPreparado2);
        var credenciais = segredosAutenticacaoPreparado2;

        assertThat(Base64.getUrlDecoder().decode(segredo)).hasSize(32);
        assertThat(credenciais.autorizaCliente(confidencial, segredo)).isTrue();
        assertThat(credenciais.autorizaCliente(confidencial, null)).isFalse();
        assertThat(credenciais.autorizaCliente(confidencial, semCredencial.gerarTokenAtualizacao())).isFalse();
        assertThat(credenciais.autorizaCliente(publica, null)).isTrue();
        assertThat(credenciais.autorizaCliente(publica, segredo)).isFalse();
    }

    private static TokenJwt jwtLocal() {
        var configuracaoAutenticacaoPreparado3 = new ConfiguracaoAutenticacao(null, null, null, Map.of());
        var argumentoPreparado3 = configuracaoAutenticacaoPreparado3;
        var mockEnvironmentPreparado = new MockEnvironment();
        var argumentoPreparado4 = mockEnvironmentPreparado.withProperty("spring.profiles.active", "local");
        var argumentoPreparado5 = Clock.fixed(AGORA, ZoneOffset.UTC);
        var tokenJwtPreparado2 = new TokenJwt(argumentoPreparado3, argumentoPreparado4, argumentoPreparado5);
        var tokenJwtPreparado = tokenJwtPreparado2;
        return tokenJwtPreparado;
    }

    private static SessaoEntity sessao() {
        SessaoEntity sessao = new SessaoEntity(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                StatusSessao.ATIVO, AGORA.plusSeconds(3600), AGORA.plusSeconds(1800), AGORA);
        ReflectionTestUtils.setField(sessao, "id", UUID.randomUUID());
        return sessao;
    }
}
