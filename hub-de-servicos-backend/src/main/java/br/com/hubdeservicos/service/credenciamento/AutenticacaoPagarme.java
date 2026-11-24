package br.com.hubdeservicos.service.credenciamento;

import br.com.hubdeservicos.service.configuracao.AmbienteServicosService.Ambiente;
import br.com.hubdeservicos.service.configuracao.ConfiguracoesExternasService;
import br.com.hubdeservicos.service.configuracao.ConfiguracoesExternasService.Stone;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AutenticacaoPagarme {
    private final ConfiguracoesExternasService configuracoes;

    public AutenticacaoPagarme(ConfiguracoesExternasService configuracoes) {
        this.configuracoes = configuracoes;
    }

    public Conta selecionarConta() {
        Stone stone = configuracoes.lerStone();
        String segredo = stone.secretKey();
        if (segredo == null || segredo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Preencha a chave secreta Stone do ambiente " + stone.ambiente() + ".");
        }
        boolean chaveDev = segredo.startsWith("sk_test_") && segredo.length() > "sk_test_".length();
        boolean chaveProd = segredo.startsWith("sk_") && segredo.length() > "sk_".length() && !segredo.startsWith("sk_test_");
        if (segredo.contains(":") || segredo.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c))
                || (stone.ambiente() == Ambiente.DEV ? !chaveDev : !chaveProd)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A chave secreta Stone é incompatível com o ambiente selecionado.");
        }
        return new Conta(stone);
    }

    public static final class Conta {
        private final Stone stone;

        private Conta(Stone stone) { this.stone = stone; }

        public Ambiente ambiente() { return stone.ambiente(); }
        public String accountId() { return stone.accountId(); }
        public void aplicarAutenticacao(HttpHeaders cabecalhos) { cabecalhos.setBasicAuth(stone.secretKey(), "", StandardCharsets.UTF_8); }
    }
}
