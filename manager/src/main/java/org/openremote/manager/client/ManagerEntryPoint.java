package org.openremote.manager.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.RootLayoutPanel;

public class ManagerEntryPoint implements com.google.gwt.core.client.EntryPoint {

    protected final ManagerGinjector injector = GWT.create(ManagerGinjector.class);

    @Override
    public void onModuleLoad() {

        RootLayoutPanel.get().add(injector.getMainPresenter().getView());

        injector.getPlaceHistoryHandler().handleCurrentHistory();
    }
}
