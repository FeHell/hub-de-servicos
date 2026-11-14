package br.com.hubdeservicos.provider;

import br.com.hubdeservicos.dto.sms.SmsSendRequest;
import br.com.hubdeservicos.dto.sms.SmsSendResult;

public interface SmsProvider {

    String getProviderCode();

    String getDisplayName();

    SmsSendResult send(SmsSendRequest request);
}
