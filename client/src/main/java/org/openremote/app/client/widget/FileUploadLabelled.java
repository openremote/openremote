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
package org.openremote.app.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.FormElement;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;

/**
 * This creates a customisable File upload widget (need to use a label wrapper otherwise the invisible
 * file input control is not clickable (not too sure why it has to be a label probably something in the
 * specs about this).
 */
public class FileUploadLabelled extends ComplexPanel {

    protected FileUpload fileUpload;
    protected FormInlineLabel label;
    Element wrapper;
    FormElement parentForm;

    public FileUploadLabelled() {
        wrapper = Document.get().createElement(LabelElement.TAG);
        setElement(wrapper);
        fileUpload = new FileUpload();
        add(fileUpload, wrapper);
        label = new FormInlineLabel();
        add(label, wrapper);
    }

    @Override
    protected void onAttach() {
        super.onAttach();

        parentForm = getParentForm();
        if (parentForm == null) {
            // wrap the input in a form to allow resetting the input
            FormPanel form = new FormPanel();
            add(form, wrapper);
            parentForm = form.getElement().cast();
            fileUpload.removeFromParent();
            form.add(fileUpload);
        }
    }

    public FileUpload getFileUpload() {
        return fileUpload;
    }

    public FormInlineLabel getLabel() {
        return label;
    }

    public void setIcon(String icon) {
        label.setIcon(icon);
    }

    public void setText(String text) {
        label.setText(text);
    }

    public void setClass(String clss) {
        wrapper.setClassName(clss);
    }

    public void clearInput() {
        if (parentForm != null) {
            parentForm.reset();
        } else {
            // This probably won't work due to browser protection but is a backup option
            fileUpload.getElement().setAttribute("value", "");
        }
    }

    public void setDisabled(boolean disabled) {
        if (disabled) {
            getElement().setAttribute("disabled", "");
        } else {
            getElement().removeAttribute("disabled");
        }
    }

    protected FormElement getParentForm() {
        Element parent = wrapper.getParentElement();
        while (parent != null && !parent.hasTagName("form")) {
            parent = parent.getParentElement();
        }
        return (FormElement)parent;
    }
}
