package br.com.hubdeservicos.service.app;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LimiteTentativasAppTest {
    @Test
    void limitaCadastrosPorIpERestauraAposAJanela() {
        Clock relogio = mock(Clock.class);
        when(relogio.instant()).thenReturn(Instant.EPOCH);
        var limite = new LimiteTentativasApp(relogio);
        IntStream.range(0, 5).forEach(i -> limite.cadastro("192.0.2.1"));
        assertThatThrownBy(() -> limite.cadastro("192.0.2.1")).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("429");
        limite.cadastro("192.0.2.2");
        when(relogio.instant()).thenReturn(Instant.EPOCH.plusSeconds(60));
        limite.cadastro("192.0.2.1");
    }

    @Test
    void limitaLoginPeloMesmoEmailMesmoComIpsECaixasDiferentes() {
        var limite = new LimiteTentativasApp(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        IntStream.range(0, 10).forEach(i -> limite.login("192.0.2." + i, "user@example.test"));
        assertThatThrownBy(() -> limite.login("198.51.100.1", "USER@example.test"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("429");
    }
}
