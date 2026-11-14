package br.com.hubdeservicos.dto.sms;

public record SmsSendRequest(String destinatario, String mensagem) {

    @Override
    public String toString() {
        return "SmsSendRequest";
    }
}
