package org.openremote.manager.client.flows;

import com.google.gwt.user.client.ui.IsWidget;

public interface FlowsView extends IsWidget {

    interface Presenter {
    }

    void setPresenter(Presenter presenter);
}
