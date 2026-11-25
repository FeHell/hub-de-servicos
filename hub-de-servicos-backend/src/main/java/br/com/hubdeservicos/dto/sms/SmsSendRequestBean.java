package br.com.hubdeservicos.dto.sms;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(name = "SmsSendRequest")
public final class SmsSendRequestBean {
    private String destinatario;

    private String mensagem;

    public SmsSendRequestBean() {
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof SmsSendRequestBean outro)) {
            return false;
        }
        return Objects.equals(destinatario, outro.destinatario) && Objects.equals(mensagem, outro.mensagem);
    }

    @Override
    public int hashCode() {
        int resultado = 0;
        resultado = 31 * resultado + Objects.hashCode(destinatario);
        resultado = 31 * resultado + Objects.hashCode(mensagem);
        return resultado;
    }

    @Override
    public String toString() {
        return "SmsSendRequest";
    }
}
