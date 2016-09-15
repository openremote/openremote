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

import com.google.gwt.user.client.ui.InsertPanel;
import com.google.gwt.user.client.ui.Label;
import elemental.json.Json;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.util.TextUtil;
import org.openremote.manager.shared.Runnable;
import org.openremote.manager.shared.attribute.Attribute;
import org.openremote.manager.shared.attribute.AttributeType;
import org.openremote.manager.shared.attribute.Attributes;
import org.openremote.manager.shared.attribute.MetadataElement;

import java.util.*;
import java.util.logging.Logger;

public class AttributesEditor<S extends AttributesEditor.Style> {

    private static final Logger LOG = Logger.getLogger(AttributesEditor.class.getName());

    public interface Container<S> {
        FormView getFormView();

        S getStyle();

        InsertPanel getPanel();

        void showConfirmation(String title, String text, Runnable onConfirm);

        void showConfirmation(String title, String text, Runnable onConfirm, Runnable onCancel);
    }

    public interface Style {

        String attributeStringEditor();

        String attributeIntegerEditor();

        String attributeFloatEditor();

        String attributeBooleanEditor();
    }

    final protected Environment environment;
    final protected Container<S> container;
    final protected Attributes attributes;
    final protected Map<Attribute, FormGroup> formGroups = new LinkedHashMap<>();

    public AttributesEditor(Environment environment, Container<S> container, Attributes attributes) {
        this.environment = environment;
        this.container = container;
        this.attributes = attributes;

        buildAttributeFormGroups();
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void buildAttributeFormGroups() {
        if (attributes == null) {
            formGroups.clear();
            return;
        }
        List<Attribute> attributeList = Arrays.asList(attributes.get());
        Collections.sort(attributeList, (o1, o2) -> o1.getName().compareTo(o2.getName()));
        for (Attribute attribute : attributeList) {
            LOG.fine("Building form group for attribute: " + attribute.getName());
            FormGroup formGroup = createFormGroup(attribute);
            if (formGroup != null) {
                formGroups.put(attribute, formGroup);
            }
        }
    }

    /* TODO Finish attribute add/remove, metadata editing

    public void buildActionsFormGroup() {
        FormGroup formGroup = new FormGroup();
        FormButton addButton = new FormButton();
        FormInputText newAttributeNameInput  =new FormInputText();
        addButton.addClickHandler(event -> {
            if (newAttributeNameInput.getValue() != null) {
                String attributeName = newAttributeNameInput.getValue();
                if (!attributes.hasAttribute(attributeName)) {
                    Attribute newAttribute = new Attribute(attributeName, AttributeType.STRING);
                    attributes.add(newAttribute);
                    FormGroup newAttributeFormGroup = createFormGroup(newAttribute);
                    formGroups.add(newAttributeFormGroup);
                    container.add(newAttributeFormGroup);
                }
            }
        });
    }
    */

    public void render() {
        for (FormGroup formGroup : formGroups.values()) {
            container.getPanel().add(formGroup);
        }
    }

    public void refresh() {
        formGroups.clear();
        buildAttributeFormGroups();
        clearContainer();
        render();
    }

    public void setOpaque(boolean opaque) {
        for (FormGroup formGroup : formGroups.values()) {
            formGroup.setOpaque(opaque);
        }
    }

    /* ####################################################################### */

    protected void clearContainer() {
        while (container.getPanel().getWidgetCount() > 0) {
            container.getPanel().remove(0);
        }
    }

    protected FormGroup createFormGroup(Attribute attribute) {

        FormGroup formGroup = new FormGroup();

        FormLabel formLabel = buildFormLabel(attribute);
        formGroup.addFormLabel(formLabel);

        FormField attributeField = new FormField();
        formGroup.addFormField(attributeField);

        if (attribute.getType().equals(AttributeType.STRING)) {
            formGroup.getFormField().add(createStringEditor(container.getStyle(), attribute, isDefaultReadOnly(attribute)));
        } else if (attribute.getType().equals(AttributeType.INTEGER)) {
            formGroup.getFormField().add(createIntegerEditor(container.getStyle(), attribute, isDefaultReadOnly(attribute)));
        } else if (attribute.getType().equals(AttributeType.FLOAT)) {
            formGroup.getFormField().add(createFloatEditor(container.getStyle(), attribute, isDefaultReadOnly(attribute)));
        } else if (attribute.getType().equals(AttributeType.BOOLEAN)) {
            formGroup.getFormField().add(createBooleanEditor(container.getStyle(), attribute, isDefaultReadOnly(attribute)));
        } else {
            formGroup.getFormField().add(
                new Label(environment.getMessages().unsupportedAttributeType(attribute.getType().getValue()))
            );
        }

        String description = getDescription(attribute);
        if (description != null) {
            formGroup.addInfolabel(new Label(description));
        }

        return formGroup;
    }

    protected FormLabel buildFormLabel(Attribute attribute) {
        FormLabel formLabel = new FormLabel();
        String label = attribute.getName();
        if (attribute.hasMetadataElement("label")) {
            MetadataElement metadataLabel = attribute.getMetadata().getElement("label");
            if (metadataLabel.getType().equals(AttributeType.STRING.getValue())) {
                label = metadataLabel.getValue().asString();
            }
        }
        formLabel.setText(TextUtil.ellipsize(label, 30));
        formLabel.addStyleName("larger");

        return formLabel;
    }

    /* ####################################################################### */

    protected FormInputText createStringEditor(S style, Attribute attribute, boolean readOnly) {
        FormInputText input = createFormInputText(style.attributeStringEditor());

        MetadataElement defaultValue;
        if (attribute.getValue() != null) {
            input.setValue(attribute.getValue().asString());
        } else if ((defaultValue = getDefaultValue(attribute)) != null) {
            input.setValue(defaultValue.getValue().asString());
        }

        if (readOnly) {
            input.setReadOnly(true);
        } else {
            input.addValueChangeHandler(
                event -> attribute.setValue(
                    Json.create(event.getValue())
                )
            );
        }
        return input;
    }

    protected FormInputNumber createIntegerEditor(S style, Attribute attribute, boolean readOnly) {
        FormInputNumber input = createFormInputNumber(style.attributeIntegerEditor());

        MetadataElement defaultValue;
        if (attribute.getValue() != null) {
            input.setValue(attribute.getValue().asString());
        } else if ((defaultValue = getDefaultValue(attribute)) != null) {
            input.setValue(defaultValue.getValue().asString());
        }

        if (readOnly) {
            input.setReadOnly(true);
        } else {
            input.addValueChangeHandler(event -> {
                try {
                    Integer value = Integer.valueOf(event.getValue());
                    attribute.setValue(Json.create(value));
                } catch (NumberFormatException ex) {
                    // TODO Do nothing? can we hook this up into validation?
                }
            });
        }
        return input;
    }

    protected FormInputText createFloatEditor(S style, Attribute attribute, boolean readOnly) {
        FormInputText input = createFormInputText(style.attributeFloatEditor());

        MetadataElement defaultValue;
        if (attribute.getValue() != null) {
            input.setValue(attribute.getValue().asString());
        } else if ((defaultValue = getDefaultValue(attribute)) != null) {
            input.setValue(defaultValue.getValue().asString());
        }

        if (readOnly) {
            input.setReadOnly(true);
        } else {
            input.addValueChangeHandler(event -> {
                try {
                    Double value = Double.valueOf(event.getValue());
                    attribute.setValue(Json.create(value));
                } catch (NumberFormatException ex) {
                    // TODO Do nothing? can we hook this up into validation?
                }
            });
        }
        return input;
    }

    protected FormCheckBox createBooleanEditor(S style, Attribute attribute, boolean readOnly) {
        FormCheckBox input = createFormInputCheckBox(style.attributeBooleanEditor());

        Boolean value = null;
        MetadataElement defaultValue;
        if (attribute.getValueAsBoolean() != null) {
            value = attribute.getValueAsBoolean();
        } else if ((defaultValue = getDefaultValue(attribute)) != null) {
            defaultValue.getValue().asBoolean();
        }

        input.setValue(value);

        if (readOnly) {
            input.setEnabled(false);
        } else {
            input.addValueChangeHandler(event -> {
                attribute.setValue(Json.create(input.getValue()));
            });
        }
        return input;
    }

    /* ####################################################################### */

    protected FormInputText createFormInputText(String formFieldStyleName) {
        FormInputText input = new FormInputText();
        input.addStyleName(formFieldStyleName);
        return input;
    }

    protected FormInputNumber createFormInputNumber(String formFieldStyleName) {
        FormInputNumber input = new FormInputNumber();
        input.addStyleName(formFieldStyleName);
        return input;
    }

    protected FormCheckBox createFormInputCheckBox(String formFieldStyleName) {
        FormCheckBox input = new FormCheckBox();
        input.addStyleName(formFieldStyleName);
        return input;
    }

    /* ####################################################################### */

    protected String getDescription(Attribute attribute) {
        if (attribute.hasMetadataElement("description")) {
            MetadataElement metadataDescription = attribute.getMetadata().getElement("description");
            if (metadataDescription.getType().equals(AttributeType.STRING.getValue())) {
                return metadataDescription.getValue().asString();
            }
        }
        return null;
    }

    protected MetadataElement getDefaultValue(Attribute attribute) {
        if (attribute.hasMetadataElement("defaultValue")) {
            return attribute.getMetadata().getElement("defaultValue");
        }
        return null;
    }

    protected boolean isDefaultReadOnly(Attribute attribute) {
        return false;
    }
}
