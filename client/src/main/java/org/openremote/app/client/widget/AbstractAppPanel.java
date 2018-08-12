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
package org.openremote.app.client.widget;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.UIObject;
import java.util.function.Consumer;

public abstract class AbstractAppPanel implements AppPanel {

    final protected PopupPanel popupPanel;

    protected UIObject target;
    protected Position position = Position.CENTER;
    protected int marginTop;
    protected int marginRight;
    protected int marginBottom;
    protected int marginLeft;
    protected Consumer<Boolean> openCloseConsumer;

    protected HandlerRegistration windowHandlerRegistration;

    public AbstractAppPanel(UiBinder<PopupPanel, AbstractAppPanel> binder) {
        this.popupPanel = binder.createAndBindUi(this);

        popupPanel.getElement().getStyle().setOverflow(Style.Overflow.AUTO);

        popupPanel.setGlassStyleName("or-PopupPanelGlass");

        popupPanel.addAttachHandler(event -> {
            if (event.isAttached()) {
                windowHandlerRegistration = Window.addResizeHandler(e -> {
                    if (isOpen()) {
                        open();
                    }
                });
                if (openCloseConsumer != null) {
                    openCloseConsumer.accept(true);
                }
            } else if (windowHandlerRegistration != null) {
                windowHandlerRegistration.removeHandler();
                windowHandlerRegistration = null;
            }
        });

        popupPanel.addCloseHandler(event -> {
            if (openCloseConsumer != null) {
                openCloseConsumer.accept(false);
            }
            if (target != null) {
                popupPanel.removeAutoHidePartner(target.getElement());
            }
        });

    }

    public PopupPanel getPopupPanel() {
        return popupPanel;
    }

    @Override
    public void setTarget(UIObject target) {
        if (this.target != null) {
            popupPanel.removeAutoHidePartner(this.target.getElement());
        }
        this.target = target;
    }

    @Override
    public void setPosition(Position position) {
        this.position = position;
    }

    @Override
    public void setMarginTop(int marginTop) {
        this.marginTop = marginTop;
    }

    @Override
    public void setMarginRight(int marginRight) {
        this.marginRight = marginRight;
    }

    @Override
    public void setMarginBottom(int marginBottom) {
        this.marginBottom = marginBottom;
    }

    @Override
    public void setMarginLeft(int marginLeft) {
        this.marginLeft = marginLeft;
    }

    @Override
    public void setAutoHide(boolean autoHide) {
        popupPanel.setAutoHideEnabled(autoHide);
    }

    @Override
    public void setAutoHideOnHistoryEvents(boolean autoHide) {
        popupPanel.setAutoHideOnHistoryEventsEnabled(autoHide);
    }

    @Override
    public void setModal(boolean modal) {
        getPopupPanel().setGlassEnabled(modal);
    }

    @Override
    public void setOpenCloseConsumer(Consumer<Boolean> openCloseConsumer) {
        this.openCloseConsumer = openCloseConsumer;
    }


    @Override
    public boolean isOpen() {
        return popupPanel.isShowing();
    }

    @Override
    public void open() {
        if (position == Position.CENTER) {
            popupPanel.center();
        } else if (target != null) {

            popupPanel.removeAutoHidePartner(target.getElement());
            popupPanel.addAutoHidePartner(target.getElement());

            if (position == Position.TARGET_AUTO) {
                popupPanel.showRelativeTo(target);
            } else if (position == Position.TARGET_BOTTOM_RIGHT) {
                getPopupPanel().setPopupPositionAndShow((offsetWidth, offsetHeight) -> {
                    int bottom = target.getAbsoluteTop() + target.getOffsetHeight();
                    int right = target.getAbsoluteLeft() + target.getOffsetWidth();
                    int top = bottom - offsetHeight - marginBottom;
                    int left = right - offsetWidth - marginRight;
                    getPopupPanel().setPopupPosition(left, top);
                });
            } else if (position == Position.TARGET_TOP_LEFT) {
                getPopupPanel().setPopupPositionAndShow((offsetWidth, offsetHeight) -> {
                        int top = target.getAbsoluteTop() + marginTop;
                        int left = target.getAbsoluteLeft() + marginLeft;
                        getPopupPanel().setPopupPosition(left, top);
                    }
                );
            }
        }
    }

    @Override
    public void toggle() {
        if (isOpen()) {
            close();
        } else {
            open();
        }
    }

    @Override
    public void close() {
        popupPanel.hide();
    }
}
