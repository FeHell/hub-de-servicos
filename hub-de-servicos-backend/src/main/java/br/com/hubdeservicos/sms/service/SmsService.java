package br.com.hubdeservicos.sms.service;

import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.configuracao.repository.ConfiguracaoSistemaRepository;
import br.com.hubdeservicos.sms.dto.SmsSendRequest;
import br.com.hubdeservicos.sms.dto.SmsSendResult;
import br.com.hubdeservicos.sms.dto.SmsSendResult.Erro;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import static br.com.hubdeservicos.configuracao.entity.ConfiguracaoSistemaEntity.SMS_PROVEDOR_ATIVO;

@Service
public class SmsService {

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
            return SmsSendResult.falha(null, Erro.CONFIGURACAO_INDISPONIVEL);
        }
        if (codigo == null) {
            return SmsSendResult.falha(null, Erro.CONFIGURACAO_INDISPONIVEL);
        }
        var provedor = provedorResolver.localizar(codigo);
        if (provedor.isEmpty()) {
            return SmsSendResult.falha(codigo, Erro.PROVEDOR_INDISPONIVEL);
        }
        try {
            return provedor.get().send(request);
        } catch (RuntimeException erro) {
            return SmsSendResult.falha(codigo, Erro.FALHA_DO_PROVEDOR);
        }
    }
}
