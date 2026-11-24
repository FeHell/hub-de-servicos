package br.com.hubdeservicos.service.credenciamento;

import br.com.hubdeservicos.entity.credenciamento.DiagnosticoRecebedorEntity;
import br.com.hubdeservicos.entity.credenciamento.RecebedorExternoEntity.Chave;
import br.com.hubdeservicos.repository.credenciamento.DiagnosticoRecebedorRepository;
import br.com.hubdeservicos.service.configuracao.AmbienteServicosService.Ambiente;
import br.com.hubdeservicos.service.configuracao.CriptografiaCredenciais;
import br.com.hubdeservicos.service.configuracao.CriptografiaCredenciais.CredencialIndisponivel;
import br.com.hubdeservicos.service.credenciamento.ClienteRecebedorPagarme.DiagnosticoHttp;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DiagnosticoRecebedorService {
    private static final Pattern FRASES_SEGURAS = Pattern.compile(
            "(?iu)\\b(precondition failed|invalid api key|account not found|recipient already exists|"
                    + "marketplace is not enabled|split is not enabled|sandbox is not supported|"
                    + "operation is not supported in (?:test mode|sandbox)|invalid bank account|"
                    + "pré-condição falhou|recebedor já existe|conta não encontrada|"
                    + "split não habilitado|marketplace não habilitado)\\b");

    private final DiagnosticoRecebedorRepository diagnosticos;
    private final CriptografiaCredenciais criptografia;
    private final ObjectMapper json;
    private final Clock relogio;

    public DiagnosticoRecebedorService(DiagnosticoRecebedorRepository diagnosticos,
            CriptografiaCredenciais criptografia, ObjectMapper json, Clock relogio) {
        this.diagnosticos = diagnosticos;
        this.criptografia = criptografia;
        this.json = json;
        this.relogio = relogio;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void arquivar(Chave chave, UUID referencia, DiagnosticoHttp captura) {
        try {
            var conteudo = criptografia.criptografar(contexto(referencia), json.writeValueAsString(captura));
            diagnosticos.save(new DiagnosticoRecebedorEntity(referencia, chave.credenciamento(),
                    chave.ambiente(), relogio.instant(), conteudo));
        } catch (JsonProcessingException | CredencialIndisponivel erro) {
            throw indisponivel();
        }
    }

    @PreAuthorize("@acessoAdministrativo.plataforma(authentication)")
    @Transactional(readOnly = true)
    public Resumo consultar(UUID tentativa) {
        var registro = diagnosticos.findById(tentativa).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Captura de diagnóstico indisponível para esta tentativa."));
        var captura = recuperar(registro, tentativa);
        JsonNode corpo;
        try {
            corpo = json.readTree(captura.corpo());
        } catch (IOException erro) {
            corpo = null;
        }
        if (corpo == null) corpo = json.createObjectNode();
        var gateway = corpo.path("gateway_response");
        var codigos = new LinkedHashSet<String>();
        var mensagens = new LinkedHashSet<String>();
        extrair(corpo, codigos, mensagens);
        extrair(gateway, codigos, mensagens);
        return new Resumo(tentativa, registro.credenciamento(), registro.ambiente(), registro.capturadoEm(),
                captura.statusHttp(), captura.cabecalhos(), captura.corpo().length > 0, captura.corpoTruncado(),
                captura.corpoIndisponivel(), gateway.isObject(), codigoSeguro(gateway.path("code")),
                List.copyOf(codigos), List.copyOf(mensagens), ClienteRecebedorPagarme.camposRecusados(corpo));
    }

    boolean confirmaRecusaDeDados(UUID tentativa) {
        return tentativa != null && diagnosticos.findById(tentativa).map(registro -> {
            try {
                int status = recuperar(registro, tentativa).statusHttp();
                return status == 400 || status == 422;
            } catch (ResponseStatusException erro) {
                return false;
            }
        }).orElse(false);
    }

    private DiagnosticoHttp recuperar(DiagnosticoRecebedorEntity registro, UUID tentativa) {
        try {
            return json.readValue(criptografia.descriptografar(contexto(tentativa), registro.respostaProtegida()),
                    DiagnosticoHttp.class);
        } catch (JsonProcessingException | CredencialIndisponivel erro) {
            throw indisponivel();
        }
    }

    private static void extrair(JsonNode raiz, LinkedHashSet<String> codigos, LinkedHashSet<String> mensagens) {
        incluir(raiz, codigos, mensagens);
        var erros = raiz.path("errors");
        if (erros.isTextual()) incluirMensagem(erros.asText(), mensagens);
        if (erros.isArray() || erros.isObject()) {
            for (JsonNode erro : erros) {
                incluir(erro, codigos, mensagens);
                if (erro.isArray()) erro.forEach(item -> incluir(item, codigos, mensagens));
            }
        }
    }

    private static void incluir(JsonNode item, LinkedHashSet<String> codigos, LinkedHashSet<String> mensagens) {
        String codigo = codigoSeguro(item.path("code"));
        if (codigo != null) codigos.add(codigo);
        incluirMensagem(item.isTextual() ? item.asText() : item.path("message").asText(), mensagens);
    }

    private static void incluirMensagem(String original, LinkedHashSet<String> mensagens) {
        var reconhecidas = FRASES_SEGURAS.matcher(original);
        while (reconhecidas.find()) mensagens.add(reconhecidas.group());
    }

    private static String codigoSeguro(JsonNode no) {
        String codigo = no.asText("");
        return codigo.matches("[0-9]{2,6}|[A-Z]{2,24}(?:_[A-Z]{2,24}){1,3}") ? codigo : null;
    }

    private static String contexto(UUID tentativa) { return "pagarme:diagnostico:" + tentativa; }

    private static ResponseStatusException indisponivel() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Proteção do diagnóstico Pagar.me indisponível.");
    }

    public record Resumo(UUID tentativa, UUID credenciamento, Ambiente ambiente, Instant capturadoEm, int statusHttp,
                         Map<String, String> cabecalhos, boolean corpoArquivado, boolean corpoTruncado,
                         boolean corpoIndisponivel, boolean temGatewayResponse, String codigoGateway,
                         List<String> codigos, List<String> trechosSeguros, List<String> camposRecusados) {}
}
