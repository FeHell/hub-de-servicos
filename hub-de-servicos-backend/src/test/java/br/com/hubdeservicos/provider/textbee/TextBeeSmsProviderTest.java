package br.com.hubdeservicos.provider.textbee;

import br.com.hubdeservicos.dto.sms.SmsSendRequest;
import br.com.hubdeservicos.dto.sms.SmsSendResult.Erro;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(OutputCaptureExtension.class)
class TextBeeSmsProviderTest {

    private static final String URL = "https://api.textbee.dev/api/v1/gateway/send-sms";
    private static final String CHAVE = "chave-simulada-nao-publicar";
    private static final SmsSendRequest SMS = new SmsSendRequest("+12025550123", "Conteudo confidencial 123456");

    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://api.textbee.dev");
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();

    @Test
    void enviaNoContratoOficialEMapeiaAceitacaoComIdentificador() {
        servidor.expect(requestTo(URL)).andExpect(method(HttpMethod.POST)).andExpect(header("x-api-key", CHAVE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"recipients":["+12025550123"],"message":"Conteudo confidencial 123456","deviceId":"dispositivo-simulado"}
                        """, true))
                .andRespond(withSuccess("""
                        {"data":{"success":true,"smsBatchId":"lote-simulado","recipientCount":1}}
                        """, MediaType.APPLICATION_JSON));

        var resultado = provedor(CHAVE, "dispositivo-simulado").send(SMS);

        assertThat(resultado.aceito()).isTrue();
        assertThat(resultado.codigoProvedor()).isEqualTo("TEXTBEE");
        assertThat(resultado.idExterno()).isEqualTo("lote-simulado");
        assertThat(resultado.erro()).isNull();
        servidor.verify();
    }

    @Test
    void aceitaRespostaDeDespachoImediatoSemIdentificador() {
        servidor.expect(requestTo(URL))
                .andExpect(content().json("""
                        {"recipients":["+12025550123"],"message":"Conteudo confidencial 123456"}
                        """, true))
                .andRespond(withSuccess("""
                        {"data":{"successCount":1,"failureCount":0}}
                        """, MediaType.APPLICATION_JSON));

        var resultado = provedor(CHAVE, "").send(SMS);

        assertThat(resultado.aceito()).isTrue();
        assertThat(resultado.idExterno()).isNull();
        servidor.verify();
    }

    @ParameterizedTest
    @CsvSource({"400,REJEITADO_PELO_PROVEDOR", "401,ACESSO_NEGADO_PELO_PROVEDOR",
            "403,ACESSO_NEGADO_PELO_PROVEDOR", "429,LIMITE_DO_PROVEDOR", "500,FALHA_DO_PROVEDOR"})
    void trataStatusSemRepassarCorpoOuSegredos(int status, Erro erro, CapturedOutput captura) {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.valueOf(status))
                .body(CHAVE + SMS.mensagem()));

        var resultado = provedor(CHAVE, "").send(SMS);

        assertThat(resultado.aceito()).isFalse();
        assertThat(resultado.erro()).isEqualTo(erro);
        assertThat(resultado.toString()).doesNotContain(CHAVE, SMS.mensagem());
        assertThat(captura.getAll()).doesNotContain(CHAVE, SMS.mensagem());
        servidor.verify();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "x|RESPOSTA_INVALIDA", "{}|RESPOSTA_INVALIDA",
            "{\"data\":{\"success\":false}}|REJEITADO_PELO_PROVEDOR",
            "{\"data\":{\"successCount\":0,\"failureCount\":1}}|REJEITADO_PELO_PROVEDOR"
    })
    void trataRespostaInvalidaOuRecusaEmHttp200(String resposta, Erro erro) {
        servidor.expect(requestTo(URL)).andRespond(withSuccess(resposta, MediaType.APPLICATION_JSON));

        assertThat(provedor(CHAVE, "").send(SMS).erro()).isEqualTo(erro);
        servidor.verify();
    }

    @Test
    void diferenciaFalhaDeConexaoDeTimeout() {
        servidor.expect(requestTo(URL)).andRespond(withException(new ConnectException()));
        servidor.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException()));
        var provedor = provedor(CHAVE, "");

        assertThat(provedor.send(SMS).erro()).isEqualTo(Erro.FALHA_DE_CONEXAO);
        assertThat(provedor.send(SMS).erro()).isEqualTo(Erro.TIMEOUT);
        servidor.verify();
    }

    @Test
    void semChaveNaoFazRequisicao() {
        servidor.expect(never(), requestTo(URL));

        assertThat(provedor("", "").send(SMS).erro()).isEqualTo(Erro.CREDENCIAIS_AUSENTES);
        servidor.verify();
    }

    private TextBeeSmsProvider provedor(String chave, String dispositivo) {
        return new TextBeeSmsProvider(builder.build(), new ObjectMapper(), chave, dispositivo);
    }
}
