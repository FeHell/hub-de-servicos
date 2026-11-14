package br.com.hubdeservicos.service.sms;

import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.repository.configuracao.ConfiguracaoSistemaRepository;
import br.com.hubdeservicos.dto.sms.SmsSendRequest;
import br.com.hubdeservicos.dto.sms.SmsSendResult;
import br.com.hubdeservicos.dto.sms.SmsSendResult.Erro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import static br.com.hubdeservicos.entity.configuracao.ConfiguracaoSistemaEntity.SMS_PROVEDOR_ATIVO;

@Service
public class SmsService {
    private static final Logger LOG = LoggerFactory.getLogger(SmsService.class);

    private final ConfiguracaoSistemaRepository configuracoes;
    private final SmsProviderResolver provedorResolver;

    public SmsService(ConfiguracaoSistemaRepository configuracoes, SmsProviderResolver provedorResolver) {
        this.configuracoes = configuracoes;
        this.provedorResolver = provedorResolver;
    }

    public SmsSendResult send(SmsSendRequest request) {
        if (request == null || request.destinatario() == null || request.destinatario().isBlank()
                || request.mensagem() == null || request.mensagem().isBlank()) {
            return SmsSendResult.falha(null, Erro.SOLICITACAO_INVALIDA);
        }

        String codigo;
        try {
            codigo = configuracoes.buscarValor(SMS_PROVEDOR_ATIVO, StatusCadastro.ATIVO).orElse(null);
        } catch (DataAccessException erro) {
            LOG.warn("SMS: falha ao consultar SMS_PROVEDOR_ATIVO no banco");
            return SmsSendResult.falha(null, Erro.CONFIGURACAO_INDISPONIVEL);
        }
        if (codigo == null) {
            LOG.warn("SMS: configuração ativa SMS_PROVEDOR_ATIVO ausente");
            return SmsSendResult.falha(null, Erro.CONFIGURACAO_INDISPONIVEL);
        }
        var provedor = provedorResolver.localizar(codigo);
        if (provedor.isEmpty()) {
            LOG.warn("SMS: SMS_PROVEDOR_ATIVO não corresponde a um provedor registrado");
            return SmsSendResult.falha(codigo, Erro.PROVEDOR_INDISPONIVEL);
        }
        try {
            return provedor.get().send(request);
        } catch (RuntimeException erro) {
            LOG.warn("SMS: adaptador lançou uma falha inesperada");
            return SmsSendResult.falha(codigo, Erro.FALHA_DO_PROVEDOR);
        }
    }
}
