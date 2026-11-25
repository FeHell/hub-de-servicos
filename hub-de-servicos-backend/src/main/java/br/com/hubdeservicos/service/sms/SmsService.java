package br.com.hubdeservicos.service.sms;

import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.repository.configuracao.ConfiguracaoSistemaRepository;
import br.com.hubdeservicos.dto.sms.SmsSendRequestBean;
import br.com.hubdeservicos.dto.sms.SmsSendResultBean;
import br.com.hubdeservicos.dto.sms.SmsSendResultBean.Erro;

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

    public SmsSendResultBean send(SmsSendRequestBean request) {
        if (request == null || request.getDestinatario() == null || request.getDestinatario().isBlank()
                || request.getMensagem() == null || request.getMensagem().isBlank()) {
            return SmsSendResultBean.falha(null, Erro.SOLICITACAO_INVALIDA);
        }

        String codigo;
        try {
            codigo = configuracoes.buscarValor(SMS_PROVEDOR_ATIVO, StatusCadastro.ATIVO).orElse(null);
        } catch (DataAccessException erro) {
            LOG.warn("SMS: falha ao consultar SMS_PROVEDOR_ATIVO no banco");
            return SmsSendResultBean.falha(null, Erro.CONFIGURACAO_INDISPONIVEL);
        }
        if (codigo == null) {
            LOG.warn("SMS: configuração ativa SMS_PROVEDOR_ATIVO ausente");
            return SmsSendResultBean.falha(null, Erro.CONFIGURACAO_INDISPONIVEL);
        }
        var provedor = provedorResolver.localizar(codigo);
        if (provedor.isEmpty()) {
            LOG.warn("SMS: SMS_PROVEDOR_ATIVO não corresponde a um provedor registrado");
            return SmsSendResultBean.falha(codigo, Erro.PROVEDOR_INDISPONIVEL);
        }
        try {
            return provedor.get().send(request);
        } catch (RuntimeException erro) {
            LOG.warn("SMS: adaptador lançou uma falha inesperada");
            return SmsSendResultBean.falha(codigo, Erro.FALHA_DO_PROVEDOR);
        }
    }
}
