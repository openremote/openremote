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
package org.openremote.manager.client.flows;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import org.openremote.manager.client.util.Timeout;

import javax.inject.Inject;
import java.util.logging.Logger;

public class FlowsViewImpl implements FlowsView {

    private static final Logger LOG = Logger.getLogger(FlowsViewImpl.class.getName());

    interface UI extends UiBinder<HTMLPanel, FlowsViewImpl> {
    }

    private UI ui = GWT.create(UI.class);
    private HTMLPanel root;

    Presenter presenter;

    @UiField
    IFrameElement frame;

    @Inject
    public FlowsViewImpl() {
        root = ui.createAndBindUi(this);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        // TODO ouch
        Timeout.debounce("setiframefocus", () -> {
            frame.focus();
        }, 1000);
    }

    @Override
    public Widget asWidget() {
        return root;
    }
}
