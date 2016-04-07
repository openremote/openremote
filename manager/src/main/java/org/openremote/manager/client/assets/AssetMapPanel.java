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
package org.openremote.manager.client.assets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.PopupPanel;
import org.openremote.manager.client.widget.AppPanel;

public class AssetMapPanel implements AppPanel {

    interface UI extends UiBinder<PopupPanel, AssetMapPanel> {
    }

    final PopupPanel popupPanel;

    private UI ui = GWT.create(UI.class);

    public AssetMapPanel() {
        this.popupPanel = ui.createAndBindUi(this);
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                popupPanel.center();
                hide();
            }
        });
        popupPanel.hide();
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
    public void hide() {
        popupPanel.hide();
    }
}
