package br.com.hubdeservicos.service.app;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LimiteTentativasApp {
    private static final int MAX_CHAVES = 10_000;
    private static final Duration JANELA = Duration.ofMinutes(1);

    private record Chave(String operacao, String valor) {
    }

    private record Tentativas(Instant expira, int quantidade) {
    }

    private final Clock relogio;
    private final Map<Chave, Tentativas> tentativas = new HashMap<>();
    private Instant proximaLimpeza = Instant.MIN;

    LimiteTentativasApp(Clock relogio) {
        this.relogio = relogio;
    }

    public synchronized void cadastro(String ip) {
        registrar(new Chave("cadastro-ip", ip), 5, relogio.instant());
    }

    public synchronized void login(String ip, String email) {
        Instant agora = relogio.instant();
        registrar(new Chave("login-ip", ip), 30, agora);
        registrar(new Chave("login-email", email.strip().toLowerCase(Locale.ROOT)), 10, agora);
    }

    private void registrar(Chave chave, int limite, Instant agora) {
        if (!agora.isBefore(proximaLimpeza)) {
            tentativas.values().removeIf(item -> !agora.isBefore(item.expira()));
            proximaLimpeza = agora.plus(JANELA);
        }
        Tentativas atual = tentativas.get(chave);
        if (atual == null && tentativas.size() >= MAX_CHAVES) {
            throw muitasTentativas();
        }
        if (atual == null || !agora.isBefore(atual.expira())) {
            tentativas.put(chave, new Tentativas(agora.plus(JANELA), 1));
            return;
        }
        if (atual.quantidade() >= limite) {
            throw muitasTentativas();
        }
        tentativas.put(chave, new Tentativas(atual.expira(), atual.quantidade() + 1));
    }

    private static ResponseStatusException muitasTentativas() {
        return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Muitas tentativas. Aguarde um minuto e tente novamente.");
    }
}
