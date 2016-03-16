package org.openremote.manager.client.app;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.place.shared.Place;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.inject.Inject;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Navbar;
import org.gwtbootstrap3.client.ui.NavbarLink;
import org.gwtbootstrap3.client.ui.NavbarText;
import org.gwtbootstrap3.client.ui.html.Span;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.assets.AssetsPlace;
import org.openremote.manager.client.map.MapPlace;

/**
 * Created by Richard on 24/02/2016.
 */
public class HeaderViewImpl extends Composite implements HeaderView {
    interface UI extends UiBinder<Navbar, HeaderViewImpl> {
    }

    private static UI ui = GWT.create(UI.class);
    private Presenter presenter;
    private ManagerMessages messages;

    @UiField
    AnchorListItem btnMap;

    @UiField
    AnchorListItem btnAssets;

    @UiField
    Span lblSignedIn;

    @UiField
    NavbarText signinText;

    @UiField
    NavbarLink btnLogout;

    @UiHandler("btnLogout")
    void logoutClicked(ClickEvent e) {
        presenter.doLogout();
    }

    @UiHandler("btnMap")
    void defaultLayoutClicked(ClickEvent e) {
        presenter.goTo(new MapPlace());
    }

    @UiHandler("btnAssets")
    void vmdLayoutClicked(ClickEvent e) {
        presenter.goTo(new AssetsPlace());
    }

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
        btnMap.setActive(place instanceof MapPlace);
        btnAssets.setActive(place instanceof AssetsPlace);
    }

    @Override
    public void setUsername(String username) {
        if (username != null && username.length() > 0) {
            lblSignedIn.setText(messages.signedInAs(username));
            signinText.setVisible(true);
        } else {
            signinText.setVisible(false);
        }
    }
}