package br.com.hubdeservicos.dto.sms;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(name = "SmsSendResult")
public final class SmsSendResultBean {
    private boolean aceito;

    private String idExterno;

    private String codigoProvedor;

    private Erro erro;

    public SmsSendResultBean() {
    }

    public boolean getAceito() {
        return aceito;
    }

    public void setAceito(boolean aceito) {
        this.aceito = aceito;
    }

    public String getIdExterno() {
        return idExterno;
    }

    public void setIdExterno(String idExterno) {
        this.idExterno = idExterno;
    }

    public String getCodigoProvedor() {
        return codigoProvedor;
    }

    public void setCodigoProvedor(String codigoProvedor) {
        this.codigoProvedor = codigoProvedor;
    }

    public Erro getErro() {
        return erro;
    }

    public void setErro(Erro erro) {
        this.erro = erro;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof SmsSendResultBean outro)) {
            return false;
        }
        return Objects.equals(aceito, outro.aceito) && Objects.equals(idExterno, outro.idExterno)
                && Objects.equals(codigoProvedor, outro.codigoProvedor) && Objects.equals(erro, outro.erro);
    }

    @Override
    public int hashCode() {
        int resultado = 0;
        resultado = 31 * resultado + Objects.hashCode(aceito);
        resultado = 31 * resultado + Objects.hashCode(idExterno);
        resultado = 31 * resultado + Objects.hashCode(codigoProvedor);
        resultado = 31 * resultado + Objects.hashCode(erro);
        return resultado;
    }

    public enum Erro {
        SOLICITACAO_INVALIDA, CONFIGURACAO_INDISPONIVEL, PROVEDOR_INDISPONIVEL, CREDENCIAIS_AUSENTES, URL_BASE_AUSENTE,
        URL_BASE_INVALIDA, REMETENTE_AUSENTE, REMETENTE_NAO_PERMITIDO, DESTINATARIO_NAO_AUTORIZADO,
        DESTINATARIO_INVALIDO, AUTENTICACAO_RECUSADA_PELO_PROVEDOR, ACESSO_NEGADO_PELO_PROVEDOR, LIMITE_DO_PROVEDOR,
        REJEITADO_PELO_PROVEDOR, FALHA_DO_PROVEDOR, FALHA_DE_CONEXAO, TIMEOUT, RESPOSTA_INVALIDA
    }

    public static SmsSendResultBean aceito(String provedor, String idExterno) {
        SmsSendResultBean smsSendResultPreparado = new SmsSendResultBean();
        smsSendResultPreparado.setAceito(true);
        smsSendResultPreparado.setIdExterno(idExterno);
        smsSendResultPreparado.setCodigoProvedor(provedor);
        smsSendResultPreparado.setErro(null);
        return smsSendResultPreparado;
    }

    public static SmsSendResultBean falha(String provedor, Erro erro) {
        SmsSendResultBean smsSendResultPreparado2 = new SmsSendResultBean();
        smsSendResultPreparado2.setAceito(false);
        smsSendResultPreparado2.setIdExterno(null);
        smsSendResultPreparado2.setCodigoProvedor(provedor);
        smsSendResultPreparado2.setErro(erro);
        return smsSendResultPreparado2;
    }

    @Override
    public String toString() {
        return "SmsSendResult[aceito=" + aceito + ", erro=" + erro + "]";
    }
}
