package br.com.hubdeservicos.service.credenciamento;

import br.com.hubdeservicos.service.usuario.TelefoneUsuarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class ComparacaoCredenciamento {
    private final ObjectMapper json;

    ComparacaoCredenciamento(ObjectMapper json) {
        this.json = json;
    }

    boolean iguais(Object anterior, Object proposto) {
        return normalizar("", json.valueToTree(anterior)).equals(normalizar("", json.valueToTree(proposto)));
    }

    private JsonNode normalizar(String campo, JsonNode valor) {
        if (valor.isObject()) {
            var resultado = json.createObjectNode();
            valor.fields().forEachRemaining(item -> {
                var filho = normalizar(item.getKey(), item.getValue());
                if (!filho.isNull() && !(filho.isObject() && filho.isEmpty())) {
                    resultado.set(item.getKey(), filho);
                }
            });
            return resultado;
        }
        if (valor.isArray()) {
            var resultado = json.createArrayNode();
            valor.forEach(item -> resultado.add(normalizar(campo, item)));
            return resultado;
        }
        if (!valor.isTextual()) {
            return valor;
        }
        String texto = valor.asText().strip().replaceAll("\\p{Zs}+", " ");
        if (texto.isEmpty()) {
            return json.nullNode();
        }
        texto = switch (campo) {
        case "cpf", "cnpj", "documento", "document", "holder_document" -> ValidacaoCredenciamento.documento(texto);
        case "telefone" -> telefone(texto);
        case "cep", "zip_code" -> texto.replace("-", "");
        case "uf", "state", "digitoAgencia", "digitoConta", "branch_check_digit", "account_check_digit" ->
            texto.toUpperCase(Locale.ROOT);
        case "renda", "rendaMensal", "faturamento", "faturamentoAnual" ->
            new BigDecimal(texto).stripTrailingZeros().toString();
        default -> texto;
        };
        return json.getNodeFactory().textNode(texto);
    }

    private static String telefone(String texto) {
        try {
            return TelefoneUsuarioService.normalizar(texto);
        } catch (ResponseStatusException erro) {
            return texto;
        }
    }
}
