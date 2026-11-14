package br.com.hubdeservicos.dto.sms;

public record SmsSendResult(boolean aceito, String idExterno, String codigoProvedor, Erro erro) {

    public enum Erro {
        SOLICITACAO_INVALIDA,
        CONFIGURACAO_INDISPONIVEL,
        PROVEDOR_INDISPONIVEL,
        CREDENCIAIS_AUSENTES,
        ACESSO_NEGADO_PELO_PROVEDOR,
        LIMITE_DO_PROVEDOR,
        REJEITADO_PELO_PROVEDOR,
        FALHA_DO_PROVEDOR,
        FALHA_DE_CONEXAO,
        TIMEOUT,
        RESPOSTA_INVALIDA
    }

    public static SmsSendResult aceito(String provedor, String idExterno) {
        return new SmsSendResult(true, idExterno, provedor, null);
    }

    public static SmsSendResult falha(String provedor, Erro erro) {
        return new SmsSendResult(false, null, provedor, erro);
    }

    @Override
    public String toString() {
        return "SmsSendResult[aceito=" + aceito + ", erro=" + erro + "]";
    }
}
