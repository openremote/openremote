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
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * A form group has a main and optional extension panel that can be expanded/collapsed on demand.
 * <p>
 * The main panel renders a {@link FormLabel}, optional secondary info label, a {@link FormField}, and {@link FormGroupActions} in a block.
 * <p>
 * The extension panel renders any widget in a collapsible block after the main panel.
 */
public class FormGroup extends FlowPanel implements HasWidgets {

    private static final Logger LOG = Logger.getLogger(FormGroup.class.getName());

    protected FlowPanel mainPanel = new FlowPanel();

    protected FlowPanel labelPanel = new FlowPanel();
    protected FormLabel formLabel;
    protected Label infoLabel;

    protected FormField formField;

    protected FormGroupActions formGroupActions;

    protected FormButton toggleExtensionButton = new FormButton();

    protected FlowPanel extensionPanel = new FlowPanel();

    protected boolean expanded;

    protected boolean errorInField;
    protected boolean errorInExtension;

    public FormGroup() {
        setStyleName("layout vertical or-FormGroup");

        mainPanel.setStyleName("layout horizontal center");
        add(mainPanel);

        labelPanel.setStyleName("layout vertical");
        mainPanel.add(labelPanel);

        toggleExtensionButton.addStyleName("or-FormGroupExtensionButton");
        toggleExtensionButton.setVisible(false);
        toggleExtensionButton.setIcon("caret-down");
        toggleExtensionButton.addClickHandler(clickEvent -> {
            setExpanded(!isExpanded());
        });
        mainPanel.add(toggleExtensionButton);

        extensionPanel.setStyleName("layout vertical or-FormGroupExtension");
    }

    @UiChild(tagname = "label", limit = 1)
    public void addFormLabel(FormLabel formLabel) {
        if (this.formLabel != null)
            throw new IllegalStateException("Form label already set");
        this.formLabel = formLabel;
        labelPanel.add(formLabel);
    }

    @UiChild(tagname = "field", limit = 1)
    public void addFormField(FormField formField) {
        if (this.formField != null)
            throw new IllegalStateException("Form field already set");
        this.formField = formField;
        mainPanel.insert(formField,
            mainPanel.getWidgetIndex(formGroupActions) > 0
                ? mainPanel.getWidgetIndex(formGroupActions)
                : mainPanel.getWidgetIndex(toggleExtensionButton)
        );

        if (this.formLabel != null) {
            formField.setFormFieldId(this.formLabel.getFormFieldId());
        }
    }

    @UiChild(tagname = "actions", limit = 1)
    public void addFormGroupActions(FormGroupActions formGroupActions) {
        this.formGroupActions = formGroupActions;
        mainPanel.insert(formGroupActions, mainPanel.getWidgetIndex(toggleExtensionButton));
    }

    @UiChild(tagname = "info", limit = 1)
    public void addInfolabel(Label infoLabel) {
        if (this.infoLabel != null)
            throw new IllegalStateException("Form info label already set");
        this.infoLabel = infoLabel;
        infoLabel.setStyleName("or-FormInfoLabel");
        labelPanel.add(infoLabel);
    }

    @UiChild(tagname = "expansion")
    public void addExtension(Widget widget) {
        toggleExtensionButton.setVisible(true);
        toggleExtensionButton.setEnabled(true);
        extensionPanel.add(widget);
    }

    public void showDisabledExtensionToggle() {
        toggleExtensionButton.setVisible(true);
        toggleExtensionButton.setEnabled(false);
    }

    public int getExtensionWidgetIndex(Widget widget) {
        return extensionPanel.getWidgetIndex(widget);
    }

    public void insertExtension(Widget widget, int beforeIndex) {
        toggleExtensionButton.setVisible(true);
        toggleExtensionButton.setEnabled(true);
        extensionPanel.insert(widget, beforeIndex);
    }

    public boolean removeExtension(Widget widget) {
        boolean removed = extensionPanel.remove(widget);
        if (removed && extensionPanel.getWidgetCount() == 0)
            toggleExtensionButton.setVisible(false);
        return removed;
    }

    public void forExtension(Consumer<Widget> extension) {
        for (int i = 0; i < extensionPanel.getWidgetCount(); i++) {
            extension.accept(extensionPanel.getWidget(i));
        }
    }

    public FormLabel getFormLabel() {
        return formLabel;
    }

    public FormField getFormField() {
        return formField;
    }

    public Label getInfoLabel() {
        return infoLabel;
    }

    public Style getStyle() {
        return getElement().getStyle();
    }

    public void setError(boolean errorInField) {
        this.errorInField = errorInField;
        if (getFormLabel() != null) {
            getFormLabel().setError(errorInField || errorInExtension);
        }
        if (getFormField() != null) {
            getFormField().setError(errorInField);
        }
    }

    public void setErrorInExtension(boolean errorInExtension) {
        this.errorInExtension = errorInExtension;
        if (getFormLabel() != null) {
            getFormLabel().setError(errorInField || errorInExtension);
        }
        toggleExtensionButton.removeStyleName("error");
        if (errorInExtension) {
            toggleExtensionButton.addStyleName("error");
        }
    }

    public boolean isError() {
        return errorInField || errorInExtension;
    }

    public void setAlignStart(boolean alignStart) {
        if (alignStart) {
            mainPanel.removeStyleName("center");
            mainPanel.addStyleName("start");
        } else {
            mainPanel.removeStyleName("start");
            mainPanel.addStyleName("center");
        }
    }

    public void setOpaque(boolean opaque) {
        removeStyleName("opaque");
        if (opaque) {
            addStyleName("opaque");
        }
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        if (expanded) {
            add(extensionPanel);
            addStyleName("expanded");
            toggleExtensionButton.setIcon("caret-up");
            toggleExtensionButton.addStyleName("or-FormButtonDown");
            toggleExtensionButton.removeStyleName("or-FormButton");
        } else {
            remove(extensionPanel);
            removeStyleName("expanded");
            toggleExtensionButton.setIcon("caret-down");
            toggleExtensionButton.removeStyleName("or-FormButtonDown");
            toggleExtensionButton.addStyleName("or-FormButton");
        }
    }

    public boolean isExpanded() {
        return expanded;
    }
}
