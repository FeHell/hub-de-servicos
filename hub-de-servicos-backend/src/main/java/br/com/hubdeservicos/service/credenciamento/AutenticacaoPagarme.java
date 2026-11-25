package br.com.hubdeservicos.service.credenciamento;

import br.com.hubdeservicos.service.configuracao.AmbienteServicosService.Ambiente;
import br.com.hubdeservicos.service.configuracao.ConfiguracoesExternasService;
import br.com.hubdeservicos.service.configuracao.ConfiguracoesExternasService.StoneVO;
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

    public ContaVO selecionarConta() {
        StoneVO stone = configuracoes.lerStone();
        String segredo = stone.getSecretKey();
        if (segredo == null || segredo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Preencha a chave secreta Stone do ambiente " + stone.getAmbiente() + ".");
        }
        boolean chaveDev = segredo.startsWith("sk_test_") && segredo.length() > "sk_test_".length();
        boolean chaveProd = segredo.startsWith("sk_") && segredo.length() > "sk_".length()
                && !segredo.startsWith("sk_test_");
        if (segredo.contains(":")
                || segredo.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c))
                || (stone.getAmbiente() == Ambiente.DEV ? !chaveDev : !chaveProd)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A chave secreta Stone é incompatível com o ambiente selecionado.");
        }
        var contaPreparado = new ContaVO(stone);
        return contaPreparado;
    }

    public static final class ContaVO {
        private final StoneVO stone;

        private ContaVO(StoneVO stone) {
            this.stone = stone;
        }

        public Ambiente getAmbiente() {
            return stone.getAmbiente();
        }

        public String getAccountId() {
            return stone.getAccountId();
        }

        public void aplicarAutenticacao(HttpHeaders cabecalhos) {
            cabecalhos.setBasicAuth(stone.getSecretKey(), "", StandardCharsets.UTF_8);
        }
    }
}
