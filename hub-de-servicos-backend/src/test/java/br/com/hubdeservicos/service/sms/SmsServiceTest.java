package br.com.hubdeservicos.service.sms;

import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.repository.configuracao.ConfiguracaoSistemaRepository;
import br.com.hubdeservicos.dto.sms.SmsSendRequestBean;
import br.com.hubdeservicos.dto.sms.SmsSendResultBean;
import br.com.hubdeservicos.dto.sms.SmsSendResultBean.Erro;
import br.com.hubdeservicos.provider.SmsProvider;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;

import static br.com.hubdeservicos.entity.configuracao.ConfiguracaoSistemaEntity.SMS_PROVEDOR_ATIVO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SmsServiceTest {

    private final ConfiguracaoSistemaRepository configuracoes = mock(ConfiguracaoSistemaRepository.class);
    private final SmsProvider infobip = mock(SmsProvider.class);
    private final SmsSendRequestBean pedido;
    {
        SmsSendRequestBean smsSendRequestPreparado = new SmsSendRequestBean();
        smsSendRequestPreparado.setDestinatario("+12025550123");
        smsSendRequestPreparado.setMensagem("Mensagem simulada");
        pedido = smsSendRequestPreparado;
    }

    @Test
    void delegaInfobipPeloContratoSemConhecerImplementacao() {
        when(configuracoes.buscarValor(SMS_PROVEDOR_ATIVO, StatusCadastro.ATIVO)).thenReturn(Optional.of("INFOBIP"));
        when(infobip.getProviderCode()).thenReturn("INFOBIP");
        var esperado = SmsSendResultBean.aceito("INFOBIP", "lote");
        when(infobip.send(pedido)).thenReturn(esperado);

        assertThat(servico(infobip).send(pedido)).isEqualTo(esperado);
        verify(infobip).send(pedido);
    }

    @Test
    void trataAusenciaDeConfiguracaoDeImplementacaoEDeBanco() {
        var servico = servico();
        var dataRetrievalFailureExceptionPreparado = new DataRetrievalFailureException("Detalhe interno");
        when(configuracoes.buscarValor(SMS_PROVEDOR_ATIVO, StatusCadastro.ATIVO))
                .thenReturn(Optional.empty(), Optional.of("SEM_IMPLEMENTACAO"))
                .thenThrow(dataRetrievalFailureExceptionPreparado);

        assertThat(servico.send(pedido).getErro()).isEqualTo(Erro.CONFIGURACAO_INDISPONIVEL);
        assertThat(servico.send(pedido).getErro()).isEqualTo(Erro.PROVEDOR_INDISPONIVEL);
        assertThat(servico.send(pedido).getErro()).isEqualTo(Erro.CONFIGURACAO_INDISPONIVEL);
    }

    @Test
    void validaPedidoAntesDeConsultarConfiguracao() {
        SmsSendRequestBean smsSendRequestPreparado2 = new SmsSendRequestBean();
        smsSendRequestPreparado2.setDestinatario("");
        smsSendRequestPreparado2.setMensagem("Mensagem");
        assertThat(servico().send(smsSendRequestPreparado2).getErro()).isEqualTo(Erro.SOLICITACAO_INVALIDA);
        verifyNoInteractions(configuracoes);
    }

    @Test
    void falhaInesperadaDoProvedorNaoVazaDetalhe() {
        when(configuracoes.buscarValor(SMS_PROVEDOR_ATIVO, StatusCadastro.ATIVO)).thenReturn(Optional.of("INFOBIP"));
        when(infobip.getProviderCode()).thenReturn("INFOBIP");
        var illegalStateExceptionPreparado = new IllegalStateException("Detalhe interno");
        when(infobip.send(pedido)).thenThrow(illegalStateExceptionPreparado);

        assertThat(servico(infobip).send(pedido))
                .isEqualTo(SmsSendResultBean.falha("INFOBIP", Erro.FALHA_DO_PROVEDOR));
    }

    private SmsService servico(SmsProvider... provedores) {
        var argumentoPreparado = configuracoes;
        var smsProviderResolverPreparado = new SmsProviderResolver(List.of(provedores));
        var argumentoPreparado2 = smsProviderResolverPreparado;
        var smsServicePreparado2 = new SmsService(argumentoPreparado, argumentoPreparado2);
        var smsServicePreparado = smsServicePreparado2;
        return smsServicePreparado;
    }
}
