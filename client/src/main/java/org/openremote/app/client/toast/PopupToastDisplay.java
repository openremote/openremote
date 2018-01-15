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
package org.openremote.app.client.toast;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import jsinterop.annotations.JsType;
import org.openremote.app.client.style.WidgetStyle;
import org.openremote.app.client.widget.MessagesIcon;
import org.openremote.app.client.widget.PopupPanel;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@JsType
public class PopupToastDisplay implements ToastDisplay {

    protected class Point {

        final protected int x;
        final protected int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public Point multiply(double by) {
            return new Point(
                this.x != 0 ? (int) (this.x * by) : 0,
                this.y != 0 ? (int) (this.y * by) : 0
            );
        }

        public Point divide(double by) {
            return new Point(
                this.x != 0 ? (int) (this.x / by) : 0,
                this.y != 0 ? (int) (this.y / by) : 0
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Point point = (Point) o;

            if (x != point.x) return false;
            return y == point.y;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            return result;
        }

        @Override
        public String toString() {
            return "Point{" +
                "x=" + x +
                ", y=" + y +
                '}';
        }
    }

    protected class Rectangle {

        protected Point position;
        protected int width;
        protected int height;

        public Rectangle(Point position, int width, int height) {
            this.position = position;
            this.width = width;
            this.height = height;
        }

        public void reset() {
            position = new Point(0, 0);
            width = 0;
            height = 0;
        }

        public Point getPosition() {
            return position;
        }

        public void setPosition(Point position) {
            this.position = position;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public Rectangle intersection(Rectangle that) {
            int tx1 = this.position.getX();
            int ty1 = this.position.getY();
            int rx1 = that.position.getX();
            int ry1 = that.position.getY();
            long tx2 = tx1;
            tx2 += this.width;
            long ty2 = ty1;
            ty2 += this.height;
            long rx2 = rx1;
            rx2 += that.width;
            long ry2 = ry1;
            ry2 += that.height;
            if (tx1 < rx1) tx1 = rx1;
            if (ty1 < ry1) ty1 = ry1;
            if (tx2 > rx2) tx2 = rx2;
            if (ty2 > ry2) ty2 = ry2;
            tx2 -= tx1;
            ty2 -= ty1;

            // tx2,ty2 will never overflow (they will never be
            // larger than the smallest of the two source w,h)
            // they might underflow, though...
            if (tx2 < Integer.MIN_VALUE) tx2 = Integer.MIN_VALUE;
            if (ty2 < Integer.MIN_VALUE) ty2 = Integer.MIN_VALUE;
            return new Rectangle(new Point(tx1, ty1), (int) tx2, (int) ty2);
        }

        public boolean isOverlapping(Rectangle that) {
            Rectangle intersection = this.intersection(that);
            return (intersection.getWidth() > 0 && intersection.getHeight() > 0);
        }

        @Override
        public String toString() {
            return "Rectangle{" +
                "position=" + position +
                ", width=" + width +
                ", height=" + height +
                '}';
        }
    }

    public static final int MARGIN_BOTTOM_PIXEL = 10;
    public static final int MARGIN_RIGHT_PIXEL = 25;

    final protected WidgetStyle widgetStyle;
    final protected Map<Toast, ToastPopupPanel> toastPanels = new HashMap<>();

    @Inject
    public PopupToastDisplay(WidgetStyle widgetStyle) {
        this.widgetStyle = widgetStyle;
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

            content.setStyleName("layout horizontal center");
            content.addStyleName(widgetStyle.Toast());

            switch (toast.getType()) {
                case INFO:
                    content.addStyleName(widgetStyle.ToastInfo());
                    content.add(new MessagesIcon("info-circle"));
                    break;
                case SUCCESS:
                    content.addStyleName(widgetStyle.ToastSuccess());
                    content.add(new MessagesIcon("check"));
                    break;
                default:
                    content.addStyleName(widgetStyle.ToastFailure());
                    content.add(new MessagesIcon("warning"));
                    break;
            }

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
