package org.openremote.manager.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;

public class ManagerEntryPoint implements com.google.gwt.core.client.EntryPoint {

    protected final ManagerGinjector injector = GWT.create(ManagerGinjector.class);

    @Override
    public void onModuleLoad() {
        //RootLayoutPanel.get().add(GWT.create(TestLayout.class));
        injector.getAppController().start();
    }
}
