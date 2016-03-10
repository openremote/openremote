package org.openremote.manager.client;

import com.google.gwt.core.client.GWT;

public class ManagerEntryPoint implements com.google.gwt.core.client.EntryPoint {

    protected final ManagerGinjector injector = GWT.create(ManagerGinjector.class);

    @Override
    public void onModuleLoad() {

        injector.getAppController().start();
    }
}
