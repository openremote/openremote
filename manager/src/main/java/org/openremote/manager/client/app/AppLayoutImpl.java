/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.client.app;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import org.openremote.manager.client.assets.AssetsPlace;
import org.openremote.manager.client.i18n.ManagerConstants;

import javax.inject.Inject;

public class AppLayoutImpl extends Composite implements AppLayout {

    interface UI extends UiBinder<HeaderPanel, AppLayoutImpl> {
    }

    private UI ui = GWT.create(UI.class);
    private HeaderPanel appLayout;

    @UiField
    SimpleLayoutPanel headerPanel;

    @UiField
    SimpleLayoutPanel contentPanel;

    @UiField
    SimpleLayoutPanel leftSidePanel;

    @UiField
    SplitLayoutPanel bodyPanel;

    @UiField(provided = true)
    ManagerConstants constants;

    Presenter presenter;

    @Inject
    public AppLayoutImpl(ManagerConstants constants) {
        this.constants = constants;
        appLayout = ui.createAndBindUi(this);
        initWidget(appLayout);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public AcceptsOneWidget getMainContentPanel() {
        return contentPanel;
    }

    @Override
    public AcceptsOneWidget getLeftSidePanel() {
        return leftSidePanel;
    }

    @Override
    public AcceptsOneWidget getHeaderPanel() {
        return headerPanel;
    }

    @Override
    public void updateLayout(Place place) {
        bodyPanel.setWidgetHidden(leftSidePanel, true);
        bodyPanel.setWidgetHidden(contentPanel, false);

        if (place instanceof AssetsPlace) {
            bodyPanel.setWidgetHidden(leftSidePanel, false);
        }
    }
}
