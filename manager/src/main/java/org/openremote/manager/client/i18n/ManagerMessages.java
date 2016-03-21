package org.openremote.manager.client.i18n;

import com.google.gwt.i18n.client.LocalizableResource;
import com.google.gwt.i18n.client.Messages;

@LocalizableResource.DefaultLocale("en")
public interface ManagerMessages extends Messages {

    String fieldBlank();

    String serverUnavailable();

    String serverError(int code);

    String loginFailed();

    String signedInAs(String username);
}
