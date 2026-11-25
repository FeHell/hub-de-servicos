package br.com.hubdeservicos.provider.infobip;

import br.com.hubdeservicos.dto.sms.SmsSendRequestBean;
import br.com.hubdeservicos.dto.sms.SmsSendResultBean.Erro;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.hubdeservicos.service.configuracao.ConfiguracoesExternasService;
import br.com.hubdeservicos.service.configuracao.ConfiguracoesExternasService.InfobipVO;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InfobipConfiguracaoTest {
    private final ApplicationContextRunner ambiente;

    {
        var applicationContextRunnerPreparado = new ApplicationContextRunner();
        var configDataApplicationContextInitializerPreparado = new ConfigDataApplicationContextInitializer();
        ambiente = applicationContextRunnerPreparado.withInitializer(contexto -> {
            var systemEnvironmentPropertySourcePreparado = new SystemEnvironmentPropertySource(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, Map.of("INFOBIP_BASE_URL",
                            "https://conta-teste.api.infobip.com", "INFOBIP_API_KEY", "chave-ficticia"));
            contexto.getEnvironment().getPropertySources().replace(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    systemEnvironmentPropertySourcePreparado);
        }).withInitializer(configDataApplicationContextInitializerPreparado)
                .withPropertyValues("spring.config.location=optional:classpath:/application-local.yml")
                .withBean(ObjectMapper.class).withBean(ConfiguracoesExternasService.class, () -> {
                    var configuracoes = mock(ConfiguracoesExternasService.class);
                    var infobipPreparado = new InfobipVO("", "", false);
                    when(configuracoes.lerInfobip()).thenReturn(infobipPreparado);
                    return configuracoes;
                }).withUserConfiguration(InfobipSmsProvider.class);
    }

    @Test
    void preservaRemetenteLocalEIgnoraVariaveisLegadasDaInfobip() {
        ambiente.run(contexto -> {
            assertThat(contexto).hasNotFailed();
            assertThat(contexto.getEnvironment().getProperty("hub.sms.infobip.remetente-padrao")).isEqualTo("InfoSMS");
            SmsSendRequestBean smsSendRequestPreparado = new SmsSendRequestBean();
            smsSendRequestPreparado.setDestinatario("34");
            smsSendRequestPreparado.setMensagem("Teste");
            assertThat(contexto.getBean(InfobipSmsProvider.class).send(smsSendRequestPreparado).getErro())
                    .isEqualTo(Erro.CREDENCIAIS_AUSENTES);
            var infobipPreparado2 = new InfobipVO("https://conta-teste.api.infobip.com", "chave-ficticia-do-banco",
                    true);
            when(contexto.getBean(ConfiguracoesExternasService.class).lerInfobip()).thenReturn(infobipPreparado2);
            SmsSendRequestBean smsSendRequestPreparado2 = new SmsSendRequestBean();
            smsSendRequestPreparado2.setDestinatario("34");
            smsSendRequestPreparado2.setMensagem("Teste");
            assertThat(contexto.getBean(InfobipSmsProvider.class).send(smsSendRequestPreparado2).getErro())
                    .isEqualTo(Erro.DESTINATARIO_INVALIDO);
        });
    }

    @Test
    void exigeRemetenteExplicitoForaDoLocal() {
        ambiente.withPropertyValues("spring.config.location=optional:classpath:/infobip-sem-perfil.yml")
                .run(contexto -> {
                    var infobipPreparado3 = new InfobipVO("https://conta-teste.api.infobip.com",
                            "chave-ficticia-do-banco", true);
                    when(contexto.getBean(ConfiguracoesExternasService.class).lerInfobip())
                            .thenReturn(infobipPreparado3);
                    SmsSendRequestBean smsSendRequestPreparado3 = new SmsSendRequestBean();
                    smsSendRequestPreparado3.setDestinatario("+5534999999999");
                    smsSendRequestPreparado3.setMensagem("Teste");
                    assertThat(contexto.getBean(InfobipSmsProvider.class).send(smsSendRequestPreparado3).getErro())
                            .isEqualTo(Erro.REMETENTE_AUSENTE);
                });
    }
}
