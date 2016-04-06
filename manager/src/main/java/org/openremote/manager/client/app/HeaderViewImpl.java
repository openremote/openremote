package org.openremote.manager.client.app;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.place.shared.Place;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.inject.Inject;
import gwt.material.design.client.ui.MaterialButton;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialLink;
import org.openremote.manager.client.assets.AssetsPlace;
import org.openremote.manager.client.flows.FlowsPlace;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.map.MapPlace;

/**
 * Created by Richard on 24/02/2016.
 */
public class HeaderViewImpl extends Composite implements HeaderView {
    interface UI extends UiBinder<HTMLPanel, HeaderViewImpl> {
    }

    private static UI ui = GWT.create(UI.class);
    private Presenter presenter;
    private ManagerMessages messages;
    @UiField
    MaterialButton itemMap;
    @UiField
    MaterialButton itemFlows;
    @UiField
    MaterialButton itemAssets;
    @UiField
    MaterialLabel signinText;
    @UiField
    MaterialButton logoutButton;

    @Inject
    public HeaderViewImpl(ManagerMessages messages) {
        this.messages = messages;
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }


    @Override
    public void onPlaceChange(Place place) {
        if (place instanceof MapPlace)
            itemMap.addStyleName("active");
        else
            itemMap.removeStyleName("active");

        if (place instanceof AssetsPlace)
            itemAssets.addStyleName("active");
        else
            itemAssets.removeStyleName("active");

        if (place instanceof FlowsPlace)
            itemFlows.addStyleName("active");
        else
            itemFlows.removeStyleName("active");
    }

    @Override
    public void setUsername(String username) {
        if (username != null && username.length() > 0) {
            signinText.setText(messages.signedInAs(username));
            signinText.setVisible(true);
        } else {
            signinText.setVisible(false);
        }
    }

    @UiHandler("logoutButton")
    void logoutClicked(ClickEvent e) {
        presenter.doLogout();
    }

    @UiHandler("itemMap")
    void itemMapClicked(ClickEvent e) {
        presenter.goTo(new MapPlace());
    }

    @UiHandler("itemAssets")
    void itemAssetsClicked(ClickEvent e) {
        presenter.goTo(new AssetsPlace());
    }

    @UiHandler("itemFlows")
    void itemFlowsClicked(ClickEvent e) {
        presenter.goTo(new FlowsPlace());
    }

}