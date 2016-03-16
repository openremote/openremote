package org.openremote.manager.client.assets;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.IsWidget;

public interface AssetListView extends IsWidget {

    interface Presenter {
        void goTo(Place place);

        void getHelloText();
    }

    void setPresenter(Presenter presenter);
}
