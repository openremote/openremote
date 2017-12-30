/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.client.apps;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.components.client.style.WidgetStyle;
import org.openremote.manager.client.util.Timeout;
import org.openremote.manager.client.widget.Hyperlink;
import org.openremote.manager.shared.apps.ConsoleApp;

import javax.inject.Inject;

public class AppsViewImpl extends Composite implements AppsView {

    interface UI extends UiBinder<HTMLPanel, AppsViewImpl> {
    }

    interface Style extends CssResource {

        String nav();

        String navItem();
    }

    @UiField
    ManagerMessages managerMessages;
    @UiField
    WidgetStyle widgetStyle;

    @UiField
    Style style;

    @UiField
    HTMLPanel appsListPanel;

    @UiField
    IFrameElement frame;

    @UiField
    HTMLPanel placeholder;

    Presenter presenter;

    @Inject
    public AppsViewImpl() {
        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
        if (presenter == null) {
            appsListPanel.clear();
            frame.setSrc("about:blank");
            frame.getStyle().setDisplay(com.google.gwt.dom.client.Style.Display.NONE);
        }
    }

    @Override
    public void setApps(ConsoleApp[] apps) {
        appsListPanel.clear();

        if (apps == null || apps.length == 0) {
            LabelItem item = new LabelItem(null, managerMessages.noAppsFound());
            appsListPanel.add(item);
            return;
        }

        for (ConsoleApp app : apps) {
            LinkItem item = new LinkItem(app.getTenant().getRealm(), app.getTenant().getDisplayName());
            appsListPanel.add(item);
        }
    }

    @Override
    public void openAppUrl(String realm, String appUrl) {

        // Select the right item in the list of apps
        appsListPanel.forEach(item -> {
            item.removeStyleName("active");
            if (item instanceof LinkItem) {
                LinkItem linkItem = (LinkItem) item;
                if (linkItem.realm.equals(realm)) {
                    item.addStyleName("active");
                }
            }
        });

        // Show the app in an iframe, try to give it focus
        placeholder.setVisible(false);
        frame.getStyle().clearDisplay();
        frame.setSrc(appUrl);
        Timeout.debounce("setiframefocus", () -> {
            frame.focus();
        }, 500);
    }

    class LabelItem extends Label {

        final String realm;

        public LabelItem(String realm, String text) {
            super(text);
            this.realm = realm;
            addStyleName(widgetStyle.SecondaryNavItem());
        }
    }

    class LinkItem extends Hyperlink {

        final String realm;

        public LinkItem(String realm, String text) {
            this.realm = realm;
            setIcon("connectdevelop");
            addStyleName(style.navItem());
            addStyleName(widgetStyle.SecondaryNavItem());
            setText(text);
            setSimpleClickHandler(() -> presenter.onAppSelected(realm));
        }
    }
}
