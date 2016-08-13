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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasWidgets;

public class FormGroup extends FlowPanel implements HasWidgets {

    protected FormLabel formLabel;
    protected FormField formField;

    public FormGroup() {
        getElement().addClassName("layout horizontal center or-FormGroup theme-FormGroup");
    }

    @UiChild(tagname = "label", limit = 1)
    public void addFormLabel(FormLabel formLabel) {
        if (this.formLabel != null)
            throw new IllegalStateException("Form label already set");
        this.formLabel = formLabel;
        add(formLabel);
    }

    @UiChild(tagname = "field", limit = 1)
    public void addFormField(FormField formField) {
        if (this.formField != null)
            throw new IllegalStateException("Form field already set");
        this.formField = formField;
        add(formField);

        if (this.formLabel != null) {
            formField.setFormFieldId(this.formLabel.getFormFieldId());
        }

    }

    public Style getStyle() {
        return getElement().getStyle();
    }

    public void setError(boolean error) {
        getElement().removeClassName("error");
        if (error) {
            getElement().addClassName("error");
        }
    }

    public void setAlignStart(boolean alignStart) {
        if (alignStart) {
            getElement().removeClassName("center");
            getElement().addClassName("start");
        } else {
            getElement().removeClassName("start");
            getElement().addClassName("center");
        }
    }
}
