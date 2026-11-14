package br.com.hubdeservicos.service.sms;

import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.repository.configuracao.ConfiguracaoSistemaRepository;
import br.com.hubdeservicos.dto.sms.SmsSendRequest;
import br.com.hubdeservicos.dto.sms.SmsSendResult;
import br.com.hubdeservicos.dto.sms.SmsSendResult.Erro;
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
    private final SmsSendRequest pedido = new SmsSendRequest("+12025550123", "Mensagem simulada");

    @Test
    void delegaInfobipPeloContratoSemConhecerImplementacao() {
        when(configuracoes.buscarValor(SMS_PROVEDOR_ATIVO, StatusCadastro.ATIVO)).thenReturn(Optional.of("INFOBIP"));
        when(infobip.getProviderCode()).thenReturn("INFOBIP");
        var esperado = SmsSendResult.aceito("INFOBIP", "lote");
        when(infobip.send(pedido)).thenReturn(esperado);

        assertThat(servico(infobip).send(pedido)).isEqualTo(esperado);
        verify(infobip).send(pedido);
    }

    @Test
    void trataAusenciaDeConfiguracaoDeImplementacaoEDeBanco() {
        var servico = servico();
        when(configuracoes.buscarValor(SMS_PROVEDOR_ATIVO, StatusCadastro.ATIVO))
                .thenReturn(Optional.empty(), Optional.of("SEM_IMPLEMENTACAO"))
                .thenThrow(new DataRetrievalFailureException("Detalhe interno"));

        assertThat(servico.send(pedido).erro()).isEqualTo(Erro.CONFIGURACAO_INDISPONIVEL);
        assertThat(servico.send(pedido).erro()).isEqualTo(Erro.PROVEDOR_INDISPONIVEL);
        assertThat(servico.send(pedido).erro()).isEqualTo(Erro.CONFIGURACAO_INDISPONIVEL);
    }

    @Test
    void validaPedidoAntesDeConsultarConfiguracao() {
        assertThat(servico().send(new SmsSendRequest("", "Mensagem")).erro()).isEqualTo(Erro.SOLICITACAO_INVALIDA);
        verifyNoInteractions(configuracoes);
    }

    @Test
    void falhaInesperadaDoProvedorNaoVazaDetalhe() {
        when(configuracoes.buscarValor(SMS_PROVEDOR_ATIVO, StatusCadastro.ATIVO)).thenReturn(Optional.of("INFOBIP"));
        when(infobip.getProviderCode()).thenReturn("INFOBIP");
        when(infobip.send(pedido)).thenThrow(new IllegalStateException("Detalhe interno"));

        assertThat(servico(infobip).send(pedido))
                .isEqualTo(SmsSendResult.falha("INFOBIP", Erro.FALHA_DO_PROVEDOR));
    }

    private SmsService servico(SmsProvider... provedores) {
        return new SmsService(configuracoes, new SmsProviderResolver(List.of(provedores)));
    }
}
