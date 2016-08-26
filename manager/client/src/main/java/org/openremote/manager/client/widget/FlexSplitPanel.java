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

import com.google.gwt.dom.client.Style;
import com.google.gwt.uibinder.client.UiChild;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;

public class FlexSplitPanel extends Composite {

    final protected FlowPanel mainPanel = new FlowPanel();
    final protected FlowPanel firstPanel = new FlowPanel();
    final protected FlowPanel secondPanel = new FlowPanel();
    final protected FocusPanel resizeHandle = new FocusPanel();
    final protected Label resizeHandleKnob = new Label();

    protected boolean vertical;
    protected int snapDistance = 40;
    protected int firstPanelMinWidth = 0;
    protected int firstPanelMinHeight = 0;
    protected int secondPanelMinWidth = 0;
    protected int secondPanelMinHeight = 0;

    protected Runnable onResize;
    protected boolean isResizing = false;
    protected int resizeStart = 0;

    public FlexSplitPanel() {
        mainPanel.setStyleName(
            "flex layout " + (isVertical() ? "vertical" : "horizontal")
        );

        mainPanel.add(firstPanel);
        mainPanel.add(resizeHandle);
        mainPanel.add(secondPanel);

        firstPanel.setStyleName("flex layout vertical");
        firstPanel.getElement().getStyle().setOverflow(Style.Overflow.AUTO);
        secondPanel.setStyleName("flex layout vertical");
        secondPanel.getElement().getStyle().setOverflow(Style.Overflow.AUTO);

        resizeHandle.setStyleName("layout horizontal center-center");
        resizeHandle.getElement().getStyle().setCursor(Style.Cursor.POINTER);
        resizeHandle.getElement().getStyle().setProperty("flex", "0 0 24px");

        resizeHandle.addMouseDownHandler(event -> {
            isResizing = true;
            resizeStart(event.getClientX() - getAbsoluteLeft(), event.getClientY() - getAbsoluteTop());
            DOM.setCapture(resizeHandle.getElement());
            event.preventDefault();
        });
        resizeHandle.addMouseMoveHandler(event -> {
            if (isResizing) {
                resizeMove(event.getClientX() - getAbsoluteLeft(), event.getClientY() - getAbsoluteTop());
                event.preventDefault();
            }
        });
        resizeHandle.addMouseUpHandler(event -> {
            if (isResizing) {
                isResizing = false;
                DOM.releaseCapture(resizeHandle.getElement());
                resizeStop();
            }
        });

        resizeHandleKnob.getElement().getStyle().setDisplay(Style.Display.INLINE_BLOCK);
        resizeHandleKnob.getElement().getStyle().setProperty("fontSize", "44px");
        if (isVertical()) {
            resizeHandleKnob.getElement().getStyle().setLineHeight(24, Style.Unit.PX);
            resizeHandleKnob.getElement().setInnerHTML("&#183;&#183;&#183;");
        } else {
            resizeHandleKnob.getElement().getStyle().setLineHeight(15, Style.Unit.PX);
            resizeHandleKnob.getElement().setInnerHTML("&#183;<br/>&#183;<br/>&#183;<br/>");
        }
        resizeHandle.add(resizeHandleKnob);

        initWidget(mainPanel);
    }

    public FlowPanel getFirstPanel() {
        return firstPanel;
    }

    @UiChild(tagname = "first")
    public void addFirstPanelWidget(Widget widget) {
        getFirstPanel().add(widget);
    }

    @UiChild(tagname = "second")
    public void addSecondPanelWidget(Widget widget) {
        getSecondPanel().add(widget);
    }

    public FlowPanel getSecondPanel() {
        return secondPanel;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    public boolean isVertical() {
        return vertical;
    }

    public int getSnapDistance() {
        return snapDistance;
    }

    public void setSnapDistance(int snapDistance) {
        this.snapDistance = snapDistance;
    }

    public void setHandleStyle(String handleStyle) {
        resizeHandle.addStyleName(handleStyle);
    }

    public void setFirstPanelWidth(int pixels) {
        adjustFirstPanelWidth(pixels);
    }

    public void setSecondPanelWidth(int pixels) {
        adjustSecondPanelWidth(pixels);
    }

    public void setFirstPanelHeight(int pixels) {
        adjustFirstPanelHeight(pixels);
    }

    public void setSecondPanelHeight(int pixels) {
        adjustSecondPanelHeight(pixels);
    }

    public void setFirstPanelMinWidth(int firstPanelMinWidth) {
        if (firstPanelMinWidth < 0)
            return;
        this.firstPanelMinWidth = firstPanelMinWidth;
    }

    public void setFirstPanelMinHeight(int firstPanelMinHeight) {
        if (firstPanelMinHeight < 0)
            return;
        this.firstPanelMinHeight = firstPanelMinHeight;
    }

    public void setSecondPanelMinWidth(int secondPanelMinWidth) {
        if (secondPanelMinWidth < 0)
            return;
        this.secondPanelMinWidth = secondPanelMinWidth;
    }

    public void setSecondPanelMinHeight(int secondPanelMinHeight) {
        if (secondPanelMinHeight < 0)
            return;
        this.secondPanelMinHeight = secondPanelMinHeight;
    }

    public Runnable getOnResize() {
        return onResize;
    }

    public void setOnResize(Runnable onResize) {
        this.onResize = onResize;
    }

    protected void resizeStart(int x, int y) {
        resizeStart = isVertical() ? y : x;
    }

    protected void resizeMove(int x, int y) {
        if (isVertical()) {
            int delta = y - resizeStart;
            resizeStart = y;
            int firstPanelHeight = firstPanel.getElement().getOffsetHeight() + delta;
            int secondPanelHeight = secondPanel.getElement().getOffsetHeight() - delta;
            if (firstPanelHeight < firstPanelMinHeight || secondPanelHeight < secondPanelMinHeight)
                return;
            if (firstPanelHeight <= getSnapDistance() && delta < 0)
                firstPanelHeight = 0;
            if (secondPanelHeight <= getSnapDistance() && delta > 0) {
                adjustSecondPanelHeight(0);
            } else {
                adjustFirstPanelHeight(firstPanelHeight);
            }
        } else {
            int delta = x - resizeStart;
            resizeStart = x;
            int firstPanelWidth = firstPanel.getElement().getOffsetWidth() + delta;
            int secondPanelWidth = secondPanel.getElement().getOffsetWidth() - delta;
            if (firstPanelWidth < firstPanelMinWidth || secondPanelWidth < secondPanelMinWidth)
                return;
            if (firstPanelWidth <= getSnapDistance() && delta < 0)
                firstPanelWidth = 0;
            if (secondPanelWidth <= getSnapDistance() && delta > 0) {
                adjustSecondPanelWidth(0);
            } else {
                adjustFirstPanelWidth(firstPanelWidth);
            }
        }
        if (onResize != null)
            onResize.run();
    }

    protected void resizeStop() {
        resizeStart = 0;
    }

    protected void adjustFirstPanelHeight(int heightPixels) {
        firstPanel.getElement().getStyle().setProperty("minHeight", heightPixels + "px");
        firstPanel.getElement().getStyle().setProperty("maxHeight", heightPixels + "px");
        secondPanel.getElement().getStyle().clearProperty("minHeight");
        secondPanel.getElement().getStyle().clearProperty("maxHeight");
    }

    protected void adjustSecondPanelHeight(int heightPixels) {
        firstPanel.getElement().getStyle().clearProperty("minHeight");
        firstPanel.getElement().getStyle().clearProperty("maxHeight");
        secondPanel.getElement().getStyle().setProperty("minHeight", heightPixels  + "px");
        secondPanel.getElement().getStyle().setProperty("maxHeight", heightPixels + "px");
    }

    protected void adjustFirstPanelWidth(int widthPixels) {
        firstPanel.getElement().getStyle().setProperty("minWidth", widthPixels + "px");
        firstPanel.getElement().getStyle().setProperty("maxWidth", widthPixels + "px");
        secondPanel.getElement().getStyle().clearProperty("minWidth");
        secondPanel.getElement().getStyle().clearProperty("maxWidth");
    }

    protected void adjustSecondPanelWidth(int widthPixels) {
        firstPanel.getElement().getStyle().clearProperty("minWidth");
        firstPanel.getElement().getStyle().clearProperty("maxWidth");
        secondPanel.getElement().getStyle().setProperty("minWidth", widthPixels  + "px");
        secondPanel.getElement().getStyle().setProperty("maxWidth", widthPixels+ "px");
    }

}
