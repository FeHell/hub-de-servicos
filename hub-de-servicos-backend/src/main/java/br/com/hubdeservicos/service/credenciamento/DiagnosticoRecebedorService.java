package br.com.hubdeservicos.service.credenciamento;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

import br.com.hubdeservicos.entity.credenciamento.DiagnosticoRecebedorEntity;
import br.com.hubdeservicos.entity.credenciamento.RecebedorExternoEntity.Chave;
import br.com.hubdeservicos.repository.credenciamento.DiagnosticoRecebedorRepository;
import br.com.hubdeservicos.service.configuracao.AmbienteServicosService.Ambiente;
import br.com.hubdeservicos.service.configuracao.CriptografiaCredenciais;
import br.com.hubdeservicos.service.configuracao.CriptografiaCredenciais.CredencialIndisponivel;
import br.com.hubdeservicos.service.credenciamento.ClienteRecebedorPagarme.DiagnosticoHttpBean;
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
    private static final Pattern FRASES_SEGURAS;
    static {

        FRASES_SEGURAS = Pattern
                .compile("(?iu)\\b(precondition failed|invalid api key|account not found|recipient already exists|"
                        + "marketplace is not enabled|split is not enabled|sandbox is not supported|"
                        + "operation is not supported in (?:test mode|sandbox)|invalid bank account|"
                        + "pré-condição falhou|recebedor já existe|conta não encontrada|"
                        + "split não habilitado|marketplace não habilitado)\\b");
    }

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
    public void arquivar(Chave chave, UUID referencia, DiagnosticoHttpBean captura) {
        try {
            var conteudo = criptografia.criptografar(contexto(referencia), json.writeValueAsString(captura));
            var diagnosticoRecebedorEntityPreparado = new DiagnosticoRecebedorEntity(referencia, chave.credenciamento(),
                    chave.ambiente(), relogio.instant(), conteudo);
            diagnosticos.save(diagnosticoRecebedorEntityPreparado);
        } catch (JsonProcessingException | CredencialIndisponivel erro) {
            throw indisponivel();
        }
    }

    @PreAuthorize("@acessoAdministrativo.plataforma(authentication)")
    @Transactional(readOnly = true)
    public ResumoBean consultar(UUID tentativa) {
        var registro = diagnosticos.findById(tentativa).orElseThrow(() -> {
            var responseStatusExceptionPreparado = new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Captura de diagnóstico indisponível para esta tentativa.");
            return responseStatusExceptionPreparado;
        });
        var captura = recuperar(registro, tentativa);
        JsonNode corpo;
        try {
            corpo = json.readTree(captura.getCorpo());
        } catch (IOException erro) {
            corpo = null;
        }
        if (corpo == null) {
            corpo = json.createObjectNode();
        }
        var gateway = corpo.path("gateway_response");
        var codigos = new LinkedHashSet<String>();
        var mensagens = new LinkedHashSet<String>();
        extrair(corpo, codigos, mensagens);
        extrair(gateway, codigos, mensagens);
        ResumoBean resumoPreparado = new ResumoBean();
        resumoPreparado.setTentativa(tentativa);
        resumoPreparado.setCredenciamento(registro.credenciamento());
        resumoPreparado.setAmbiente(registro.ambiente());
        resumoPreparado.setCapturadoEm(registro.capturadoEm());
        resumoPreparado.setStatusHttp(captura.getStatusHttp());
        resumoPreparado.setCabecalhos(captura.getCabecalhos());
        resumoPreparado.setCorpoArquivado(captura.getCorpo().length > 0);
        resumoPreparado.setCorpoTruncado(captura.getCorpoTruncado());
        resumoPreparado.setCorpoIndisponivel(captura.getCorpoIndisponivel());
        resumoPreparado.setTemGatewayResponse(gateway.isObject());
        resumoPreparado.setCodigoGateway(codigoSeguro(gateway.path("code")));
        resumoPreparado.setCodigos(List.copyOf(codigos));
        resumoPreparado.setTrechosSeguros(List.copyOf(mensagens));
        resumoPreparado.setCamposRecusados(ClienteRecebedorPagarme.camposRecusados(corpo));
        return resumoPreparado;
    }

    boolean confirmaRecusaDeDados(UUID tentativa) {
        return tentativa != null && diagnosticos.findById(tentativa).map(registro -> {
            try {
                int status = recuperar(registro, tentativa).getStatusHttp();
                return status == 400 || status == 422;
            } catch (ResponseStatusException erro) {
                return false;
            }
        }).orElse(false);
    }

    private DiagnosticoHttpBean recuperar(DiagnosticoRecebedorEntity registro, UUID tentativa) {
        try {
            return json.readValue(criptografia.descriptografar(contexto(tentativa), registro.respostaProtegida()),
                    DiagnosticoHttpBean.class);
        } catch (JsonProcessingException | CredencialIndisponivel erro) {
            throw indisponivel();
        }
    }

    private static void extrair(JsonNode raiz, LinkedHashSet<String> codigos, LinkedHashSet<String> mensagens) {
        incluir(raiz, codigos, mensagens);
        var erros = raiz.path("errors");
        if (erros.isTextual()) {
            incluirMensagem(erros.asText(), mensagens);
        }
        if (erros.isArray() || erros.isObject()) {
            for (JsonNode erro : erros) {
                incluir(erro, codigos, mensagens);
                if (erro.isArray()) {
                    erro.forEach(item -> incluir(item, codigos, mensagens));
                }
            }
        }
    }

    private static void incluir(JsonNode item, LinkedHashSet<String> codigos, LinkedHashSet<String> mensagens) {
        String codigo = codigoSeguro(item.path("code"));
        if (codigo != null) {
            codigos.add(codigo);
        }
        incluirMensagem(item.isTextual() ? item.asText() : item.path("message").asText(), mensagens);
    }

    private static void incluirMensagem(String original, LinkedHashSet<String> mensagens) {
        var reconhecidas = FRASES_SEGURAS.matcher(original);
        while (reconhecidas.find()) {
            mensagens.add(reconhecidas.group());
        }
    }

    private static String codigoSeguro(JsonNode no) {
        String codigo = no.asText("");
        return codigo.matches("[0-9]{2,6}|[A-Z]{2,24}(?:_[A-Z]{2,24}){1,3}") ? codigo : null;
    }

    private static String contexto(UUID tentativa) {
        return "pagarme:diagnostico:" + tentativa;
    }

    private static ResponseStatusException indisponivel() {
        var responseStatusExceptionPreparado2 = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Proteção do diagnóstico Pagar.me indisponível.");
        return responseStatusExceptionPreparado2;
    }

    @Schema(name = "Resumo")
    public static final class ResumoBean {
        private UUID tentativa;

        private UUID credenciamento;

        private Ambiente ambiente;

        private Instant capturadoEm;

        private int statusHttp;

        private Map<String, String> cabecalhos;

        private boolean corpoArquivado;

        private boolean corpoTruncado;

        private boolean corpoIndisponivel;

        private boolean temGatewayResponse;

        private String codigoGateway;

        private List<String> codigos;

        private List<String> trechosSeguros;

        private List<String> camposRecusados;

        public ResumoBean() {
        }

        public UUID getTentativa() {
            return tentativa;
        }

        public void setTentativa(UUID tentativa) {
            this.tentativa = tentativa;
        }

        public UUID getCredenciamento() {
            return credenciamento;
        }

        public void setCredenciamento(UUID credenciamento) {
            this.credenciamento = credenciamento;
        }

        public Ambiente getAmbiente() {
            return ambiente;
        }

        public void setAmbiente(Ambiente ambiente) {
            this.ambiente = ambiente;
        }

        public Instant getCapturadoEm() {
            return capturadoEm;
        }

        public void setCapturadoEm(Instant capturadoEm) {
            this.capturadoEm = capturadoEm;
        }

        public int getStatusHttp() {
            return statusHttp;
        }

        public void setStatusHttp(int statusHttp) {
            this.statusHttp = statusHttp;
        }

        public Map<String, String> getCabecalhos() {
            return cabecalhos;
        }

        public void setCabecalhos(Map<String, String> cabecalhos) {
            this.cabecalhos = cabecalhos;
        }

        public boolean getCorpoArquivado() {
            return corpoArquivado;
        }

        public void setCorpoArquivado(boolean corpoArquivado) {
            this.corpoArquivado = corpoArquivado;
        }

        public boolean getCorpoTruncado() {
            return corpoTruncado;
        }

        public void setCorpoTruncado(boolean corpoTruncado) {
            this.corpoTruncado = corpoTruncado;
        }

        public boolean getCorpoIndisponivel() {
            return corpoIndisponivel;
        }

        public void setCorpoIndisponivel(boolean corpoIndisponivel) {
            this.corpoIndisponivel = corpoIndisponivel;
        }

        public boolean getTemGatewayResponse() {
            return temGatewayResponse;
        }

        public void setTemGatewayResponse(boolean temGatewayResponse) {
            this.temGatewayResponse = temGatewayResponse;
        }

        public String getCodigoGateway() {
            return codigoGateway;
        }

        public void setCodigoGateway(String codigoGateway) {
            this.codigoGateway = codigoGateway;
        }

        public List<String> getCodigos() {
            return codigos;
        }

        public void setCodigos(List<String> codigos) {
            this.codigos = codigos;
        }

        public List<String> getTrechosSeguros() {
            return trechosSeguros;
        }

        public void setTrechosSeguros(List<String> trechosSeguros) {
            this.trechosSeguros = trechosSeguros;
        }

        public List<String> getCamposRecusados() {
            return camposRecusados;
        }

        public void setCamposRecusados(List<String> camposRecusados) {
            this.camposRecusados = camposRecusados;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof ResumoBean outro)) {
                return false;
            }
            return Objects.equals(tentativa, outro.tentativa) && Objects.equals(credenciamento, outro.credenciamento)
                    && Objects.equals(ambiente, outro.ambiente) && Objects.equals(capturadoEm, outro.capturadoEm)
                    && Objects.equals(statusHttp, outro.statusHttp) && Objects.equals(cabecalhos, outro.cabecalhos)
                    && Objects.equals(corpoArquivado, outro.corpoArquivado)
                    && Objects.equals(corpoTruncado, outro.corpoTruncado)
                    && Objects.equals(corpoIndisponivel, outro.corpoIndisponivel)
                    && Objects.equals(temGatewayResponse, outro.temGatewayResponse)
                    && Objects.equals(codigoGateway, outro.codigoGateway) && Objects.equals(codigos, outro.codigos)
                    && Objects.equals(trechosSeguros, outro.trechosSeguros)
                    && Objects.equals(camposRecusados, outro.camposRecusados);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(tentativa);
            resultado = 31 * resultado + Objects.hashCode(credenciamento);
            resultado = 31 * resultado + Objects.hashCode(ambiente);
            resultado = 31 * resultado + Objects.hashCode(capturadoEm);
            resultado = 31 * resultado + Objects.hashCode(statusHttp);
            resultado = 31 * resultado + Objects.hashCode(cabecalhos);
            resultado = 31 * resultado + Objects.hashCode(corpoArquivado);
            resultado = 31 * resultado + Objects.hashCode(corpoTruncado);
            resultado = 31 * resultado + Objects.hashCode(corpoIndisponivel);
            resultado = 31 * resultado + Objects.hashCode(temGatewayResponse);
            resultado = 31 * resultado + Objects.hashCode(codigoGateway);
            resultado = 31 * resultado + Objects.hashCode(codigos);
            resultado = 31 * resultado + Objects.hashCode(trechosSeguros);
            resultado = 31 * resultado + Objects.hashCode(camposRecusados);
            return resultado;
        }

        @Override
        public String toString() {
            return "Resumo[" + "tentativa=" + tentativa + ", credenciamento=" + credenciamento + ", ambiente="
                    + ambiente + ", capturadoEm=" + capturadoEm + ", statusHttp=" + statusHttp + ", cabecalhos="
                    + cabecalhos + ", corpoArquivado=" + corpoArquivado + ", corpoTruncado=" + corpoTruncado
                    + ", corpoIndisponivel=" + corpoIndisponivel + ", temGatewayResponse=" + temGatewayResponse
                    + ", codigoGateway=" + codigoGateway + ", codigos=" + codigos + ", trechosSeguros=" + trechosSeguros
                    + ", camposRecusados=" + camposRecusados + "]";
        }
    }
}
