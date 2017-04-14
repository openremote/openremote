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
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.UIObject;

import java.util.logging.Logger;

public abstract class AbstractAppPanel implements AppPanel {

    private static final Logger LOG = Logger.getLogger(AbstractAppPanel.class.getName());

    final protected PopupPanel popupPanel;

    protected UIObject target;
    protected int marginTop;
    protected int marginRight;
    protected int marginBottom;
    protected int marginLeft;
    protected UIObject bottomRightTarget;
    protected UIObject topLeftTarget;

    protected HandlerRegistration windowHandlerRegistration;

    public AbstractAppPanel(UiBinder<PopupPanel, AbstractAppPanel> binder) {
        this.popupPanel = binder.createAndBindUi(this);

        popupPanel.getElement().getStyle().setOverflow(Style.Overflow.AUTO);

        popupPanel.setAutoHideOnHistoryEventsEnabled(true);

        popupPanel.setGlassStyleName("or-PopupPanelGlass");

        popupPanel.addAttachHandler(event -> {
            if (event.isAttached()) {
                windowHandlerRegistration = Window.addResizeHandler(e -> resize());
            } else if (windowHandlerRegistration != null) {
                windowHandlerRegistration.removeHandler();
                windowHandlerRegistration = null;
            }
        });

        popupPanel.addCloseHandler(event -> {
            if (target != null) {
                popupPanel.removeAutoHidePartner(target.getElement());
                target = null;
            }
        });

    }

    public PopupPanel getPopupPanel() {
        return popupPanel;
    }

    @Override
    public void setAutoHide(boolean autoHide) {
        popupPanel.setAutoHideEnabled(autoHide);
    }

    @Override
    public void setModal(boolean modal) {
        getPopupPanel().setGlassEnabled(modal);
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

    public void showBottomRightOf(UIObject bottomRightTarget, int marginRight, int marginBottom) {
        this.bottomRightTarget = bottomRightTarget;
        this.marginRight = marginRight;
        this.marginBottom = marginBottom;
        getPopupPanel().setPopupPositionAndShow((offsetWidth, offsetHeight) -> {
                int bottom = bottomRightTarget.getAbsoluteTop() + bottomRightTarget.getOffsetHeight();
                int right = bottomRightTarget.getAbsoluteLeft() + bottomRightTarget.getOffsetWidth();
                int top = bottom - offsetHeight - marginBottom;
                int left = right - offsetWidth - marginRight;
                getPopupPanel().setPopupPosition(left, top);
            }
        );
    }

    public void showTopLeftOf(UIObject topLeftTarget, int marginTop, int marginLeft) {
        this.topLeftTarget = topLeftTarget;
        this.marginTop = marginTop;
        this.marginLeft = marginLeft;
        getPopupPanel().setPopupPositionAndShow((offsetWidth, offsetHeight) -> {
                int top = topLeftTarget.getAbsoluteTop() + marginTop;
                int left = topLeftTarget.getAbsoluteLeft() + marginLeft;
                getPopupPanel().setPopupPosition(left, top);
            }
        );
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

    @Override
    public void showCenter() {
        popupPanel.center();
    }

    @Override
    public void resize() {
        if (isShowing()) {
            if (target != null) {
                popupPanel.showRelativeTo(target);
            } else if (bottomRightTarget != null) {
                showBottomRightOf(bottomRightTarget, marginRight, marginBottom);
            } else if (topLeftTarget != null) {
                showTopLeftOf(topLeftTarget, marginTop, marginLeft);
            } else {
                showCenter();
            }
        }
    }
}
