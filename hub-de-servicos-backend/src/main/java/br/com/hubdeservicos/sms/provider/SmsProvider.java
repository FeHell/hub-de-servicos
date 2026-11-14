package br.com.hubdeservicos.sms.provider;

import br.com.hubdeservicos.sms.dto.SmsSendRequest;
import br.com.hubdeservicos.sms.dto.SmsSendResult;

public interface SmsProvider {

    String getProviderCode();

    String getDisplayName();

    SmsSendResult send(SmsSendRequest request);
}
