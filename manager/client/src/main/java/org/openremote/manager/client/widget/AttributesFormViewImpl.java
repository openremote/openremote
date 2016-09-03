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

import com.google.gwt.user.client.ui.Label;
import com.google.inject.Provider;
import elemental.json.Json;
import org.openremote.manager.client.app.dialog.ConfirmationDialog;
import org.openremote.manager.client.util.TextUtil;
import org.openremote.manager.shared.asset.AssetAttributeType;
import org.openremote.manager.shared.attribute.Attribute;
import org.openremote.manager.shared.attribute.AttributeType;
import org.openremote.manager.shared.attribute.Attributes;
import org.openremote.manager.shared.attribute.MetadataElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class AttributesFormViewImpl extends FormViewImpl {

    private static final Logger LOG = Logger.getLogger(AttributesFormViewImpl.class.getName());

    public AttributesFormViewImpl(Provider<ConfirmationDialog> confirmationDialogProvider) {
        super(confirmationDialogProvider);
    }

    public FormGroup[] createAttributeFormGroups(AttributesFormStyle style, Attributes attributes) {
        if (attributes == null)
            return new FormGroup[0];
        List<FormGroup> list = new ArrayList<>();
        List<Attribute> attributeList = Arrays.asList(attributes.get());
        Collections.sort(attributeList, (o1, o2) -> o1.getName().compareTo(o2.getName()));
        for (Attribute attribute : attributeList) {

            FormGroup formGroup = createAttributeFormGroup(attribute);

            if (attribute.getType().equals(AttributeType.STRING)) {
                formGroup.getFormField().add(createStringEditor(style, attribute));
            } else if (attribute.getType().equals(AttributeType.INTEGER)) {
                formGroup.getFormField().add(createIntegerEditor(style, attribute));
            } else if (attribute.getType().equals(AttributeType.FLOAT)) {
                formGroup.getFormField().add(createFloatEditor(style, attribute));
            } else if (attribute.getType().equals(AttributeType.BOOLEAN)) {
                formGroup.getFormField().add(createBooleanEditor(style, attribute));
            } else {
                formGroup.getFormField().add(
                    new Label(managerMessages.unsupportedAttributeType(attribute.getType().getValue()))
                );
            }

            String description = getDescription(attribute);
            if (description != null) {
                formGroup.addInfolabel(new Label(description));
            }

            list.add(formGroup);
        }
        return list.toArray(new FormGroup[list.size()]);
    }

    public FormGroup createAttributeFormGroup(Attribute attribute) {
        FormGroup formGroup = new FormGroup();

        FormLabel formLabel = new FormLabel();
        String label = attribute.getName();
        if (attribute.getMetadata().hasElement("label")) {
            MetadataElement metadataLabel = attribute.getMetadata().getElement("label");
            if (metadataLabel.getType().equals(AttributeType.STRING.getValue())) {
                label = metadataLabel.getValue().asString();
            }
        }
        formLabel.setText(TextUtil.ellipsize(label, 30));
        formLabel.addStyleName("larger");

        if (AssetAttributeType.isSensor(attribute)) {
            formLabel.setIcon("sign-out");
        } else if (AssetAttributeType.isActuator(attribute)) {
            formLabel.setIcon("sign-in");
        }

        formGroup.addFormLabel(formLabel);

        FormField attributeField = new FormField();
        formGroup.addFormField(attributeField);

        return formGroup;
    }

    /* ####################################################################### */

    protected FormInputText createStringEditor(AttributesFormStyle style, Attribute attribute) {
        FormInputText input = createFormInputText(style.attributeStringEditor());

        if (attribute.getValue() != null) {
            input.setValue(attribute.getValue().asString());
        }

        if (AssetAttributeType.isSensor(attribute)) {
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

    protected FormInputNumber createIntegerEditor(AttributesFormStyle style, Attribute attribute) {
        FormInputNumber input = createFormInputNumber(style.attributeIntegerEditor());

        if (attribute.getValue() != null) {
            input.setValue(attribute.getValue().asString());
        }

        if (AssetAttributeType.isSensor(attribute)) {
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

    protected FormInputText createFloatEditor(AttributesFormStyle style, Attribute attribute) {
        FormInputText input = createFormInputText(style.attributeFloatEditor());

        if (attribute.getValue() != null) {
            input.setValue(attribute.getValue().asString());
        }

        if (AssetAttributeType.isSensor(attribute)) {
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

    protected FormCheckBox createBooleanEditor(AttributesFormStyle style, Attribute attribute) {
        FormCheckBox input = createFormInputCheckBox(style.attributeBooleanEditor());

        if (attribute.getValue() != null) {
            boolean value = attribute.getValue().asBoolean();
            input.setValue(value);
        }

        if (AssetAttributeType.isSensor(attribute)) {
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
        if (attribute.getMetadata().hasElement("description")) {
            MetadataElement metadataDescription = attribute.getMetadata().getElement("description");
            if (metadataDescription.getType().equals(AttributeType.STRING.getValue())) {
                return metadataDescription.getValue().asString();
            }
        }
        return null;
    }
}
