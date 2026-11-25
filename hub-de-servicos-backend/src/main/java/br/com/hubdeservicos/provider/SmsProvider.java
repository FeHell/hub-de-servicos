package br.com.hubdeservicos.provider;

import br.com.hubdeservicos.dto.sms.SmsSendRequestBean;
import br.com.hubdeservicos.dto.sms.SmsSendResultBean;

public interface SmsProvider {

    String getProviderCode();

    String getDisplayName();

    SmsSendResultBean send(SmsSendRequestBean request);
}
