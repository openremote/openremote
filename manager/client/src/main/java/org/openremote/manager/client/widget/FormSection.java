/*
 * Copyright 2017, OpenRemote Inc.
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

import com.google.gwt.user.client.ui.FlowPanel;

/**
 * Represents a {@link FormSectionLabel} and one or more {@link com.google.gwt.user.client.ui.Widget}s to be displayed
 * inside a {@link FormGroup}.
 */
public class FormSection extends FlowPanel {
    protected FormSectionLabel label = new FormSectionLabel();

    public FormSection(String label) {
        addLabel(label);
    }

    public void addLabel(String label) {
        this.label.setText(label);
        add(this.label);
    }

    public void setDisabled(boolean disabled) {
        removeStyleName("opaque");
        if (disabled) {
            addStyleName("opaque");
        }
    }
}
