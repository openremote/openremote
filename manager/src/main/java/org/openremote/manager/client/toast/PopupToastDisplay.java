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
package org.openremote.manager.client.toast;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.openremote.manager.client.ThemeStyle;
import org.openremote.manager.client.util.Point;
import org.openremote.manager.client.util.Rectangle;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PopupToastDisplay implements ToastDisplay {

    public static final int MARGIN_BOTTOM_PIXEL = 10;
    public static final int MARGIN_RIGHT_PIXEL = 25;

    final protected ThemeStyle themeStyle;
    final protected Map<Toast, ToastPopupPanel> toastPanels = new HashMap<>();

    @Inject
    public PopupToastDisplay(ThemeStyle themeStyle) {
        this.themeStyle = themeStyle;
    }

    @Override
    public void show(final Toast toast) {
        final ToastPopupPanel panel = new ToastPopupPanel(toast);

        panel.setPopupPositionAndShow((offsetWidth, offsetHeight) -> {
            int originLeft = Window.getScrollLeft() + Window.getClientWidth() - offsetWidth - MARGIN_RIGHT_PIXEL;
            int originTop = Window.getScrollTop() + Window.getClientHeight() - offsetHeight - MARGIN_BOTTOM_PIXEL;
            setRelativePosition(
                toastPanels.values(),
                panel,
                originLeft, originTop, originTop,
                offsetHeight, offsetWidth
            );
        });

        toastPanels.put(toast, panel);
    }

    @Override
    public void remove(Toast toast) {
        if (toastPanels.containsKey(toast)) {
            toastPanels.get(toast).hide();
            toastPanels.remove(toast);
        }
    }

    class ToastPopupPanel extends PopupPanel {

        final FlowPanel content = new FlowPanel();
        final Toast toast;

        ToastPopupPanel(final Toast toast) {
            super(false, false);
            this.toast = toast;

            setGlassEnabled(false);
            getElement().getStyle().setZIndex(1000);
            setWidget(content);

            boolean isInfo = toast.getType() == Toast.Type.INFO;

            content.setStyleName("layout horizontal center");
            content.addStyleName("or-Toast");
            content.addStyleName(themeStyle.Toast());
            content.addStyleName(isInfo ? themeStyle.ToastInfo() : themeStyle.ToastFailure());

            Label icon = new Label();
            icon.setStyleName("or-MessagesIcon theme-MessagesIcon fa fa-" + (isInfo ? "check" : "warning"));
            content.add(icon);

            Label text = new Label(toast.getText());
            content.add(text);
        }

    }

    protected void setRelativePosition(Collection<ToastPopupPanel> panels,
                                       ToastPopupPanel panel,
                                       int desiredLeft, int desiredTop, int originTop,
                                       int offsetHeight, int offsetWidth) {

        for (PopupPanel existingPanel : panels) {

            Rectangle existingRec =
                new Rectangle(
                    new Point(existingPanel.getPopupLeft(), existingPanel.getPopupTop()),
                    existingPanel.getOffsetWidth(), existingPanel.getOffsetHeight()
                );

            Rectangle newRec =
                new Rectangle(
                    new Point(desiredLeft, desiredTop),
                    offsetWidth, offsetHeight
                );

            // Detect collision with existing panel in grid
            if (existingRec.isOverlapping(newRec)) {

                // Calculate new grid position
                int newTop = desiredTop - offsetHeight - MARGIN_BOTTOM_PIXEL;
                if (newTop < 0) {
                    desiredTop = originTop;
                    desiredLeft = desiredLeft - offsetWidth - MARGIN_RIGHT_PIXEL;
                } else {
                    desiredTop = newTop;
                }
                // Recursive processing until a free slot in the grid is found
                setRelativePosition(
                    panels,
                    panel,
                    desiredLeft, desiredTop, originTop,
                    offsetHeight, offsetWidth
                );
                return;
            }
        }
        panel.setPopupPosition(desiredLeft, desiredTop);
    }
}
