package br.com.hubdeservicos.sms.service;

import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.configuracao.repository.ConfiguracaoSistemaRepository;
import br.com.hubdeservicos.sms.dto.SmsSendRequest;
import br.com.hubdeservicos.sms.dto.SmsSendResult;
import br.com.hubdeservicos.sms.dto.SmsSendResult.Erro;
import br.com.hubdeservicos.sms.provider.SmsProvider;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;

import static br.com.hubdeservicos.configuracao.entity.ConfiguracaoSistemaEntity.SMS_PROVEDOR_ATIVO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SmsServiceTest {

    private final ConfiguracaoSistemaRepository configuracoes = mock(ConfiguracaoSistemaRepository.class);
    private final SmsProvider textBee = mock(SmsProvider.class);
    private final SmsSendRequest pedido = new SmsSendRequest("+12025550123", "Mensagem simulada");

    @Test
    void delegaTextBeePeloContratoSemConhecerImplementacao() {
        when(configuracoes.buscarValor(SMS_PROVEDOR_ATIVO, StatusCadastro.ATIVO)).thenReturn(Optional.of("TEXTBEE"));
        when(textBee.getProviderCode()).thenReturn("TEXTBEE");
        var esperado = SmsSendResult.aceito("TEXTBEE", "lote");
        when(textBee.send(pedido)).thenReturn(esperado);

        assertThat(servico(textBee).send(pedido)).isEqualTo(esperado);
        verify(textBee).send(pedido);
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
        when(configuracoes.buscarValor(SMS_PROVEDOR_ATIVO, StatusCadastro.ATIVO)).thenReturn(Optional.of("TEXTBEE"));
        when(textBee.getProviderCode()).thenReturn("TEXTBEE");
        when(textBee.send(pedido)).thenThrow(new IllegalStateException("Detalhe interno"));

        assertThat(servico(textBee).send(pedido))
                .isEqualTo(SmsSendResult.falha("TEXTBEE", Erro.FALHA_DO_PROVEDOR));
    }

    private SmsService servico(SmsProvider... provedores) {
        return new SmsService(configuracoes, new SmsProviderResolver(List.of(provedores)));
    }
}
