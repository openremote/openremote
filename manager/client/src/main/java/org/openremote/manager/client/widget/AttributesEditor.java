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
import org.openremote.Runnable;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.util.TextUtil;
import org.openremote.model.Attribute;
import org.openremote.model.AttributeType;
import org.openremote.model.Attributes;
import org.openremote.model.MetadataItem;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.model.asset.AssetAttributeMeta.*;
import static org.openremote.manager.shared.util.Util.sortMap;

public class AttributesEditor<S extends AttributesEditor.Style> {

    private static final Logger LOG = Logger.getLogger(AttributesEditor.class.getName());

    public interface Container<S> {
        FormView getFormView();

        S getStyle();

        InsertPanel getPanel();

        void showConfirmation(String title, String text, Runnable onConfirm);

        void showConfirmation(String title, String text, Runnable onConfirm, Runnable onCancel);

        ManagerMessages getMessages();
    }

    public interface Style {

        String attributeStringEditor();

        String attributeIntegerEditor();

        String attributeDecimalEditor();

        String attributeBooleanEditor();
    }

    final protected Environment environment;
    final protected Container<S> container;
    final protected Attributes attributes;
    final protected LinkedHashMap<Attribute, FormGroup> formGroups = new LinkedHashMap<>();

    public AttributesEditor(Environment environment, Container<S> container, Attributes attributes) {
        this.environment = environment;
        this.container = container;
        this.attributes = attributes;
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
        for (Attribute attribute : attributeList) {
            LOG.fine("Building form group for attribute: " + attribute.getName());
            FormGroup formGroup = createFormGroup(attribute);
            if (formGroup != null) {
                formGroups.put(attribute, formGroup);
            }
        }
        // Sort form groups by label text ascending
        sortMap(formGroups, (a, b) -> a.getFormLabel().getText().compareTo(b.getFormLabel().getText()));
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

    public void build() {
        formGroups.clear();
        buildAttributeFormGroups();
        for (FormGroup formGroup : formGroups.values()) {
            container.getPanel().add(formGroup);
        }
    }

    public void clearBuild() {
        while (container.getPanel().getWidgetCount() > 0) {
            container.getPanel().remove(0);
        }
        build();
    }

    public void setOpaque(boolean opaque) {
        for (FormGroup formGroup : formGroups.values()) {
            formGroup.setOpaque(opaque);
        }
    }

    public void close() {
        // Subclasses can manage lifecycle
    }

    /* ####################################################################### */

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
        } else if (attribute.getType().equals(AttributeType.DECIMAL)) {
            formGroup.getFormField().add(createDecimalEditor(container.getStyle(), attribute, isDefaultReadOnly(attribute)));
        } else if (attribute.getType().equals(AttributeType.BOOLEAN)) {
            formGroup.getFormField().add(createBooleanEditor(container.getStyle(), attribute, isDefaultReadOnly(attribute)));
        } else {
            formGroup.getFormField().add(
                new Label(environment.getMessages().unsupportedAttributeType(attribute.getType().getValue()))
            );
        }

        MetadataItem description = getFirstMetadataItem(attribute, DESCRIPTION);
        if (description != null) {
            formGroup.addInfolabel(new Label(description.getValue().asString()));
        }

        // TODO Finish actions/metadata editing
        FormGroupActions formGroupActions = new FormGroupActions();

        FormButton deleteButton = new FormButton();
        deleteButton.setText(container.getMessages().deleteAttribute());
        deleteButton.setIcon("remove");
        formGroupActions.add(deleteButton);

        FormButton editMetaButton = new FormButton();
        editMetaButton.setText(container.getMessages().editAttributeMeta());
        editMetaButton.setIcon("edit");
        formGroupActions.add(editMetaButton);

        formGroup.add(formGroupActions);

        return formGroup;
    }

    protected FormLabel buildFormLabel(Attribute attribute) {
        FormLabel formLabel = new FormLabel();
        String label = attribute.getName();
        MetadataItem labelItem = getFirstMetadataItem(attribute, LABEL);
        if (labelItem != null) {
            label = labelItem.getValue().asString();
        }
        formLabel.setText(TextUtil.ellipsize(label, 30));
        formLabel.addStyleName("larger");

        return formLabel;
    }

    /* ####################################################################### */

    protected FormInputText createStringEditor(S style, Attribute attribute, boolean readOnly) {
        FormInputText input = createFormInputText(style.attributeStringEditor());

        MetadataItem defaultValue;
        if (attribute.getValue() != null) {
            input.setValue(attribute.getValue().asString());
        } else if ((defaultValue = getFirstMetadataItem(attribute, DEFAULT)) != null) {
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

        MetadataItem defaultValue;
        if (attribute.getValue() != null) {
            input.setValue(attribute.getValue().asString());
        } else if ((defaultValue = getFirstMetadataItem(attribute, DEFAULT)) != null) {
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

    protected FormInputText createDecimalEditor(S style, Attribute attribute, boolean readOnly) {
        FormInputText input = createFormInputText(style.attributeDecimalEditor());

        MetadataItem defaultValue;
        if (attribute.getValue() != null) {
            input.setValue(attribute.getValue().asString());
        } else if ((defaultValue = getFirstMetadataItem(attribute, DEFAULT)) != null) {
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
        MetadataItem defaultValue;
        if (attribute.getValueAsBoolean() != null) {
            value = attribute.getValueAsBoolean();
        } else if ((defaultValue = getFirstMetadataItem(attribute, DEFAULT)) != null) {
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

    protected boolean isDefaultReadOnly(Attribute attribute) {
        return false;
    }
}
