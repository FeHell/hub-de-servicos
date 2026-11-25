package br.com.hubdeservicos.provider.infobip;

import br.com.hubdeservicos.repository.configuracao.ConfiguracaoSistemaRepository;
import br.com.hubdeservicos.service.configuracao.ConfiguracoesExternasService;
import br.com.hubdeservicos.service.sms.SmsProviderResolver;
import br.com.hubdeservicos.service.sms.SmsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.web.client.RestClient;

public final class InfobipTransporteControlado {
    private InfobipTransporteControlado() {
    }

    public static SmsService sms(RestClient cliente, ObjectMapper json, ConfiguracoesExternasService externas,
            ConfiguracaoSistemaRepository repositorio) {
        var infobip = new InfobipSmsProvider(cliente, json, externas, "InfoSMS");
        var argumentoPreparado = repositorio;
        var smsProviderResolverPreparado = new SmsProviderResolver(List.of(infobip));
        var argumentoPreparado2 = smsProviderResolverPreparado;
        var smsServicePreparado = new SmsService(argumentoPreparado, argumentoPreparado2);
        return smsServicePreparado;
    }
}
