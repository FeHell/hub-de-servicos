package br.com.hubdeservicos.provider.infobip;

import br.com.hubdeservicos.dto.sms.SmsSendRequest;
import br.com.hubdeservicos.dto.sms.SmsSendResult.Erro;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class InfobipConfiguracaoTest {
    private final ApplicationContextRunner ambiente = new ApplicationContextRunner()
            .withInitializer(contexto -> contexto.getEnvironment().getPropertySources().replace(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                            Map.of("INFOBIP_BASE_URL", "https://conta-teste.api.infobip.com",
                                    "INFOBIP_API_KEY", "chave-ficticia"))))
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withPropertyValues("spring.config.location=optional:classpath:/application-local.yml")
            .withBean(ObjectMapper.class)
            .withUserConfiguration(InfobipSmsProvider.class);

    @Test
    void leVariaveisDoAmbienteERemetenteDoYamlLocal() {
        ambiente.run(contexto -> {
            assertThat(contexto).hasNotFailed();
            assertThat(contexto.getEnvironment().getProperty("hub.sms.infobip.remetente-padrao")).isEqualTo("InfoSMS");
            // Um telefone inválido para o adaptador confirma que as validações de configuração passaram,
            // sem gerar tráfego externo.
            assertThat(contexto.getBean(InfobipSmsProvider.class).send(new SmsSendRequest("34", "Teste")).erro())
                    .isEqualTo(Erro.DESTINATARIO_INVALIDO);
        });
    }

    @Test
    void exigeRemetenteExplicitoForaDoLocal() {
        ambiente.withPropertyValues("spring.config.location=optional:classpath:/infobip-sem-perfil.yml")
                .run(contexto -> assertThat(contexto.getBean(InfobipSmsProvider.class)
                        .send(new SmsSendRequest("+5534999999999", "Teste")).erro())
                        .isEqualTo(Erro.REMETENTE_AUSENTE));
    }
}
