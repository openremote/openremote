package org.openremote.manager.client.i18n;

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.LocalizableResource;

/**
 * Created by Richard on 18/02/2016.
 */
@LocalizableResource.DefaultLocale("en")
public interface ManagerConstants extends Constants {

    String appTitle();

    String loginTitle();

    String username();

    String password();

    String overviewMap();

    String assetManager();

    String logout();
}
