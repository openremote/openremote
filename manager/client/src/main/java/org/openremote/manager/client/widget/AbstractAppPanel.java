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
package org.openremote.manager.client.widget;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.UIObject;

public abstract class AbstractAppPanel implements AppPanel {

    final protected PopupPanel popupPanel;

    protected UIObject target;

    public AbstractAppPanel(UiBinder<PopupPanel, AbstractAppPanel> binder, boolean autoHide) {
        this.popupPanel = binder.createAndBindUi(this);
        popupPanel.setAutoHideOnHistoryEventsEnabled(true);
        popupPanel.setAutoHideEnabled(autoHide);

        popupPanel.addCloseHandler(event -> {
            if (target != null) {
                popupPanel.removeAutoHidePartner(target.getElement());
                target = null;
            }
        });

        Window.addResizeHandler(event -> {
            if (isShowing() && target != null) {
                popupPanel.showRelativeTo(target);
            }
        });
    }

    @Override
    public boolean isShowing() {
        return popupPanel.isShowing();
    }

    @Override
    public void show() {
        popupPanel.show();
    }

    @Override
    public void showRelativeTo(UIObject target) {
        popupPanel.showRelativeTo(target);
        popupPanel.addAutoHidePartner(target.getElement());
        this.target = target;
    }

    @Override
    public void toggle() {
        if (isShowing()) {
            hide();
        } else {
            show();
        }
    }

    @Override
    public void toggleRelativeTo(UIObject target) {
        if (isShowing()) {
            hide();
        } else {
            showRelativeTo(target);
        }
    }

    @Override
    public void hide() {
        popupPanel.hide();
    }

}
