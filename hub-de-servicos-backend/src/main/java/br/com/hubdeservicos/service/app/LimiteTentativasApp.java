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

    private record ChaveVO(String operacao, String valor) {

        public String getOperacao() {
            return operacao;
        }

        public String getValor() {
            return valor;
        }
    }

    private record TentativasVO(Instant expira, int quantidade) {

        public Instant getExpira() {
            return expira;
        }

        public int getQuantidade() {
            return quantidade;
        }
    }

    private final Clock relogio;
    private final Map<ChaveVO, TentativasVO> tentativas = new HashMap<>();
    private Instant proximaLimpeza = Instant.MIN;

    LimiteTentativasApp(Clock relogio) {
        this.relogio = relogio;
    }

    public synchronized void cadastro(String ip) {
        var chavePreparado = new ChaveVO("cadastro-ip", ip);
        registrar(chavePreparado, 5, relogio.instant());
    }

    public synchronized void login(String ip, String email) {
        Instant agora = relogio.instant();
        var chavePreparado2 = new ChaveVO("login-ip", ip);
        registrar(chavePreparado2, 30, agora);
        var chavePreparado3 = new ChaveVO("login-email", email.strip().toLowerCase(Locale.ROOT));
        registrar(chavePreparado3, 10, agora);
    }

    private void registrar(ChaveVO chave, int limite, Instant agora) {
        if (!agora.isBefore(proximaLimpeza)) {
            tentativas.values().removeIf(item -> !agora.isBefore(item.getExpira()));
            proximaLimpeza = agora.plus(JANELA);
        }
        TentativasVO atual = tentativas.get(chave);
        if (atual == null && tentativas.size() >= MAX_CHAVES) {
            throw muitasTentativas();
        }
        if (atual == null || !agora.isBefore(atual.getExpira())) {
            var tentativasPreparado = new TentativasVO(agora.plus(JANELA), 1);
            tentativas.put(chave, tentativasPreparado);
            return;
        }
        if (atual.getQuantidade() >= limite) {
            throw muitasTentativas();
        }
        var tentativasPreparado2 = new TentativasVO(atual.getExpira(), atual.getQuantidade() + 1);
        tentativas.put(chave, tentativasPreparado2);
    }

    private static ResponseStatusException muitasTentativas() {
        var responseStatusExceptionPreparado = new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Muitas tentativas. Aguarde um minuto e tente novamente.");
        return responseStatusExceptionPreparado;
    }
}
