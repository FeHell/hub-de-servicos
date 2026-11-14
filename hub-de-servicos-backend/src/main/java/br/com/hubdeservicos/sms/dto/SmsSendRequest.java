package br.com.hubdeservicos.sms.dto;

public record SmsSendRequest(String destinatario, String mensagem) {

    @Override
    public String toString() {
        return "SmsSendRequest";
    }
}
