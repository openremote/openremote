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

import com.google.gwt.user.client.ui.UIObject;
import java.util.function.Consumer;

public interface AppPanel {

    enum Position {
        /**
         * Centered in viewport, no margins.
         */
        CENTER,
        /**
         * Relative to target with optional margins.
         */
        TARGET_AUTO,
        /**
         * Relative to bottom right of target with optional margins.
         */
        TARGET_BOTTOM_RIGHT,
        /**
         * Relative to top left of target with optional margins.
         */
        TARGET_TOP_LEFT
    }

    void setTarget(UIObject target);

    void setPosition(Position position);

    void setMarginTop(int marginTop);

    void setMarginRight(int marginRight);

    void setMarginBottom(int marginBottom);

    void setMarginLeft(int marginLeft);

    void setAutoHide(boolean autoHide);

    void setAutoHideOnHistoryEvents(boolean autoHide);

    void setModal(boolean modal);

    void setOpenCloseConsumer(Consumer<Boolean> openCloseConsumer);

    boolean isOpen();

    void open();

    void close();

    void toggle();
}
