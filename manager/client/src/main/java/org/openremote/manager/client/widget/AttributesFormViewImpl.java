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
import com.google.gwt.user.client.ui.TextBox;
import elemental.json.Json;
import org.openremote.manager.shared.attribute.Attribute;
import org.openremote.manager.shared.attribute.AttributeType;
import org.openremote.manager.shared.attribute.Attributes;
import org.openremote.manager.shared.attribute.MetadataElement;

import java.util.ArrayList;
import java.util.List;

public class AttributesFormViewImpl extends FormViewImpl {

    public FormGroup[] createAttributeFormGroups(String formFieldStyleName, Attributes attributes) {
        List<FormGroup> list = new ArrayList<>();
        for (Attribute attribute : attributes.get()) {

            FormGroup formGroup = createAttributeFormGroup(attribute);

            if (attribute.getType().equals(AttributeType.STRING)) {
                formGroup.getFormField().add(createStringEditor(formFieldStyleName, attribute));
            } else if (attribute.getType().equals(AttributeType.INTEGER)) {
                formGroup.getFormField().add(createIntegerEditor(formFieldStyleName, attribute));
            } else {
                formGroup.getFormField().add(
                    new Label(managerMessages.unsupportedAttributeType(attribute.getType().getName()))
                );
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
            if (metadataLabel.getType().equals(AttributeType.STRING.getName())) {
                label = metadataLabel.getValue().asString();
            }
        }
        formLabel.setText(label);
        formGroup.addFormLabel(formLabel);

        FormField attributeField = new FormField();
        formGroup.addFormField(attributeField);

        return formGroup;
    }

    protected TextBox createTextBoxEditor(String formFieldStyleName, Attribute attribute) {
        TextBox textBox = new TextBox();

        textBox.addStyleName(formFieldStyleName);
        textBox.addStyleName(widgetStyle.FormControl());
        textBox.addStyleName(themeStyle.FormControl());
        textBox.addStyleName(widgetStyle.FormInputText());
        textBox.addStyleName(themeStyle.FormInputText());

        if (attribute.getMetadata().hasElement("description")) {
            MetadataElement metadataDescription = attribute.getMetadata().getElement("description");
            if (metadataDescription.getType().equals(AttributeType.STRING.getName())) {
                textBox.setTitle(metadataDescription.getValue().asString());
            }
        }

        if (attribute.getValue() != null) {
            textBox.setValue(attribute.getValue().asString());
        }
        return textBox;
    }

    protected TextBox createStringEditor(String formFieldStyleName, Attribute attribute) {
        TextBox textBox = createTextBoxEditor(formFieldStyleName, attribute);

        textBox.addValueChangeHandler(
            event -> attribute.setValue(
                Json.create(event.getValue())
            )
        );

        return textBox;
    }

    protected TextBox createIntegerEditor(String formFieldStyleName, Attribute attribute) {
        TextBox textBox = createTextBoxEditor(formFieldStyleName, attribute);

        textBox.addValueChangeHandler(event -> {
            try {
                Integer value = Integer.valueOf(event.getValue());
                attribute.setValue(Json.create(value));
            } catch (NumberFormatException ex) {
                // TODO Do nothing? can we hook this up into validation?
            }
        });

        return textBox;
    }
}
