package br.com.hubdeservicos.provider.infobip;

import br.com.hubdeservicos.dto.sms.SmsSendRequest;
import br.com.hubdeservicos.dto.sms.SmsSendResult.Erro;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class InfobipSmsProviderTest {
    private static final String ORIGEM = "https://conta-teste.api.infobip.com";
    private static final SmsSendRequest SMS = new SmsSendRequest("+5534999999999", "Mensagem de teste");
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();

    @Test
    void enviaFormatoOficialEInterpretaAceitacao() {
        servidor.expect(requestTo(ORIGEM + "/sms/3/messages")).andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "App chave-ficticia"))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"messages":[{"sender":"InfoSMS","destinations":[{"to":"5534999999999"}],
                        "content":{"text":"Mensagem de teste"}}]}
                        """, true))
                .andRespond(withSuccess("""
                        {"messages":[{"messageId":"msg-teste","status":{"groupName":"PENDING","id":26}}]}
                        """, MediaType.APPLICATION_JSON));
        var resultado = provedor(ORIGEM + "/", "chave-ficticia", "InfoSMS").send(SMS);
        assertThat(resultado.aceito()).isTrue();
        assertThat(resultado.idExterno()).isEqualTo("msg-teste");
        servidor.verify();
    }

    @ParameterizedTest
    @CsvSource({"400,REJEITADO_PELO_PROVEDOR", "401,AUTENTICACAO_RECUSADA_PELO_PROVEDOR",
            "403,ACESSO_NEGADO_PELO_PROVEDOR", "429,LIMITE_DO_PROVEDOR", "500,FALHA_DO_PROVEDOR"})
    void trataStatusHttp(int status, Erro erro) {
        servidor.expect(requestTo(ORIGEM + "/sms/3/messages"))
                .andRespond(withStatus(HttpStatus.valueOf(status)));
        assertThat(provedor(ORIGEM, "chave-ficticia", "InfoSMS").send(SMS).erro()).isEqualTo(erro);
        servidor.verify();
    }

    @ParameterizedTest
    @CsvSource({"REJECTED,REJEITADO_PELO_PROVEDOR", "UNDELIVERABLE,REJEITADO_PELO_PROVEDOR",
            "EXPIRED,REJEITADO_PELO_PROVEDOR", "DESCONHECIDO,RESPOSTA_INVALIDA"})
    void consideraEstadoDaMensagemMesmoQuandoHttpAceita(String grupo, Erro erro) {
        servidor.expect(requestTo(ORIGEM + "/sms/3/messages")).andRespond(withSuccess("""
                {"messages":[{"messageId":"msg-teste","status":{"groupName":"%s"}}]}
                """.formatted(grupo), MediaType.APPLICATION_JSON));
        assertThat(provedor(ORIGEM, "chave-ficticia", "InfoSMS").send(SMS).erro()).isEqualTo(erro);
        servidor.verify();
    }

    @Test
    void bloqueiaEnvioComConfiguracaoIncompleta() {
        assertThat(provedor(ORIGEM, "", "InfoSMS").send(SMS).erro()).isEqualTo(Erro.CREDENCIAIS_AUSENTES);
        assertThat(provedor("", "chave-ficticia", "InfoSMS").send(SMS).erro())
                .isEqualTo(Erro.URL_BASE_AUSENTE);
        assertThat(provedor("http://example.test", "chave-ficticia", "InfoSMS").send(SMS).erro())
                .isEqualTo(Erro.URL_BASE_INVALIDA);
        assertThat(provedor(ORIGEM + "/sms/3/messages", "chave-ficticia", "InfoSMS").send(SMS).erro())
                .isEqualTo(Erro.URL_BASE_INVALIDA);
        assertThat(provedor(ORIGEM, "chave-ficticia", "").send(SMS).erro())
                .isEqualTo(Erro.REMETENTE_AUSENTE);
        assertThat(provedor(ORIGEM, "chave-ficticia", "InfoSMS")
                .send(new SmsSendRequest("(34) 99999-9999", "Mensagem de teste")).erro())
                .isEqualTo(Erro.DESTINATARIO_INVALIDO);
        servidor.verify();
    }

    @ParameterizedTest
    @CsvSource({"11,REMETENTE_NAO_PERMITIDO", "13,REMETENTE_NAO_PERMITIDO",
            "18,DESTINATARIO_NAO_AUTORIZADO", "52,DESTINATARIO_INVALIDO"})
    void distingueMotivoDaRejeicaoSemRepassarTextoDaApi(int id, Erro erro) {
        servidor.expect(requestTo(ORIGEM + "/sms/3/messages")).andRespond(withSuccess("""
                {"messages":[{"messageId":"msg-teste","status":{"groupName":"REJECTED","id":%s,
                "description":"texto que não deve chegar ao usuário"}}]}
                """.formatted(id), MediaType.APPLICATION_JSON));
        var resultado = provedor(ORIGEM, "chave-ficticia", "InfoSMS").send(SMS);
        assertThat(resultado.aceito()).isFalse();
        assertThat(resultado.erro()).isEqualTo(erro);
        assertThat(resultado.idExterno()).isNull();
        servidor.verify();
    }

    @Test
    void falhaDeConexaoNaoReenvia() {
        servidor.expect(requestTo(ORIGEM + "/sms/3/messages"))
                .andRespond(withException(new java.net.ConnectException()));
        assertThat(provedor(ORIGEM, "chave-ficticia", "InfoSMS").send(SMS).erro())
                .isEqualTo(Erro.FALHA_DE_CONEXAO);
        servidor.verify();
    }

    private InfobipSmsProvider provedor(String origem, String chave, String remetente) {
        return new InfobipSmsProvider(builder.build(), new ObjectMapper(), origem, chave, remetente);
    }
}
