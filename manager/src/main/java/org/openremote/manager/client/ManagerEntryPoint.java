package org.openremote.manager.client;

import com.google.gwt.core.client.GWT;
import org.fusesource.restygwt.client.Defaults;
import org.openremote.manager.client.auth.BearerAuthorizationDispatcher;

public class ManagerEntryPoint implements com.google.gwt.core.client.EntryPoint {

    protected final ManagerGinjector injector = GWT.create(ManagerGinjector.class);

    @Override
    public void onModuleLoad() {
        // One-time configuration
        Defaults.setDispatcher(new BearerAuthorizationDispatcher());

        injector.getMainController().start();
    }
}
