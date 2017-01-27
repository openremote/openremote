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

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InsertPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import elemental.json.Json;
import elemental.json.JsonValue;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.util.TextUtil;
import org.openremote.manager.shared.event.ui.ShowInfoEvent;
import org.openremote.model.*;
import org.openremote.model.Runnable;
import org.openremote.model.asset.AssetAttributeMeta;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.manager.shared.util.Util.sortMap;
import static org.openremote.model.asset.AssetAttributeMeta.getFirstMetadataItemValue;

public class AttributesEditor<S extends AttributesEditor.Style> {

    private static final Logger LOG = Logger.getLogger(AttributesEditor.class.getName());

    public interface Container<S extends AttributesEditor.Style> {
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

        FormLabel formLabel = createFormLabel(attribute);
        formGroup.addFormLabel(formLabel);

        FormField formField = new FormField();
        formGroup.addFormField(formField);
        formField.add(createEditor(attribute));

        MetadataItem description = AssetAttributeMeta.getFirstMetadataItem(attribute, AssetAttributeMeta.DESCRIPTION);
        if (description != null) {
            formGroup.addInfolabel(new Label(description.getValue().asString()));
        }

        FormGroupActions formGroupActions = new FormGroupActions();

        FormButton deleteButton = new FormButton();
        deleteButton.setText(container.getMessages().deleteAttribute());
        deleteButton.setIcon("remove");
        deleteButton.addClickHandler(clickEvent -> {
            environment.getEventBus().dispatch(new ShowInfoEvent("TODO: Not implemented"));
        });
        formGroupActions.add(deleteButton);

        formGroup.addFormGroupActions(formGroupActions);

        addAttributeMetaEditor(attribute, formGroup);

        return formGroup;
    }

    protected FormLabel createFormLabel(Attribute attribute) {
        FormLabel formLabel = new FormLabel();
        String label = attribute.getName();
        MetadataItem labelItem = AssetAttributeMeta.getFirstMetadataItem(attribute, AssetAttributeMeta.LABEL);
        if (labelItem != null) {
            label = labelItem.getValue().asString();
        }
        formLabel.setText(TextUtil.ellipsize(label, 30));
        formLabel.addStyleName("larger");

        return formLabel;
    }

    protected void addAttributeMetaEditor(Attribute attribute, FormGroup formGroup) {
        MetaEditor metaEditor = new MetaEditor(attribute);
        formGroup.addExtension(metaEditor);
    }

    /* ####################################################################### */

    protected IsWidget createEditor(Attribute attribute) {
        IsWidget editor;
        S style = container.getStyle();
        Consumer<JsonValue> updateConsumer = isDefaultReadOnly(attribute) ? null : attribute::setValue;
        JsonValue currentValue = attribute.getValue();
        JsonValue defaultValue = getFirstMetadataItemValue(attribute, AssetAttributeMeta.DEFAULT);

        if (attribute.getType().equals(AttributeType.STRING)) {
            editor = createStringEditor(style, currentValue, defaultValue, updateConsumer);
        } else if (attribute.getType().equals(AttributeType.INTEGER)) {
            editor = createIntegerEditor(style, currentValue, defaultValue, updateConsumer);
        } else if (attribute.getType().equals(AttributeType.DECIMAL)) {
            editor = createDecimalEditor(style, currentValue, defaultValue, updateConsumer);
        } else if (attribute.getType().equals(AttributeType.BOOLEAN)) {
            editor = createBooleanEditor(style, currentValue, defaultValue, updateConsumer);
        } else {
            FormField unsupportedField = new FormField();
            unsupportedField.add(new FormOutputText(
                environment.getMessages().unsupportedAttributeType(attribute.getType().getValue())
            ));
            editor = unsupportedField;
        }
        return editor;
    }

    protected IsWidget createEditor(MetadataItem item) {
        IsWidget editor;

        S style = container.getStyle();
        JsonValue currentValue = item.getValue();
        // For some metadata items we know if we can edit them or not... custom items are always editable
        Boolean isEditable = AssetAttributeMeta.isEditable(item.getName());
        Consumer<JsonValue> updateConsumer = isEditable != null && isEditable ? item::setValue : null;

        // TODO: We should support more JSON types, and have special editors for well-known meta items
        switch (currentValue.getType()) {
            case STRING:
                editor = createStringEditor(style, currentValue, null, updateConsumer);
                break;
            case NUMBER:
                editor = createIntegerEditor(style, currentValue, null, updateConsumer);
                break;
            case BOOLEAN:
                editor = createBooleanEditor(style, currentValue, null, updateConsumer);
                break;
            default:
                FormField unsupportedField = new FormField();
                unsupportedField.add(new FormOutputText(
                    environment.getMessages().unsupportedMetaItemType(currentValue.getType().name())
                ));
                editor = unsupportedField;
        }
        return editor;
    }

    protected FormInputText createStringEditor(S style,
                                               JsonValue currentValue,
                                               JsonValue defaultValue,
                                               Consumer<JsonValue> updateConsumer) {
        FormInputText input = createFormInputText(style.attributeStringEditor());

        if (currentValue != null) {
            input.setValue(currentValue.asString());
        } else if ((defaultValue) != null) {
            input.setValue(defaultValue.asString());
        }

        if (updateConsumer != null) {
            input.addValueChangeHandler(
                event -> updateConsumer.accept(
                    Json.create(event.getValue())
                )
            );
        } else {
            input.setReadOnly(true);
        }
        return input;
    }

    protected FormInputNumber createIntegerEditor(S style,
                                                  JsonValue currentValue,
                                                  JsonValue defaultValue,
                                                  Consumer<JsonValue> updateConsumer) {
        FormInputNumber input = createFormInputNumber(style.attributeIntegerEditor());

        if (currentValue != null) {
            input.setValue(currentValue.asString());
        } else if (defaultValue != null) {
            input.setValue(defaultValue.asString());
        }

        if (updateConsumer != null) {
            input.addValueChangeHandler(event -> {
                try {
                    Integer value = Integer.valueOf(event.getValue());
                    updateConsumer.accept(Json.create(value));
                } catch (NumberFormatException ex) {
                    // TODO Do nothing? can we hook this up into validation?
                }
            });
        } else {
            input.setReadOnly(true);
        }
        return input;
    }

    protected FormInputText createDecimalEditor(S style,
                                                JsonValue currentValue,
                                                JsonValue defaultValue,
                                                Consumer<JsonValue> updateConsumer) {
        FormInputText input = createFormInputText(style.attributeDecimalEditor());

        if (currentValue != null) {
            input.setValue(currentValue.asString());
        } else if (defaultValue != null) {
            input.setValue(defaultValue.asString());
        }

        if (updateConsumer != null) {
            input.addValueChangeHandler(event -> {
                try {
                    Double value = Double.valueOf(event.getValue());
                    updateConsumer.accept(Json.create(value));
                } catch (NumberFormatException ex) {
                    // TODO Do nothing? can we hook this up into validation?
                }
            });
        } else {
            input.setReadOnly(true);
        }
        return input;
    }

    protected FormCheckBox createBooleanEditor(S style,
                                               JsonValue currentValue,
                                               JsonValue defaultValue,
                                               Consumer<JsonValue> updateConsumer) {
        FormCheckBox input = createFormInputCheckBox(style.attributeBooleanEditor());

        Boolean value = null;
        if (currentValue != null) {
            value = currentValue.asBoolean();
        } else if (defaultValue != null) {
            defaultValue.asBoolean();
        }

        input.setValue(value);

        if (updateConsumer != null) {
            input.addValueChangeHandler(event -> updateConsumer.accept(Json.create(input.getValue())));
        } else {
            input.setEnabled(false);
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

    /* ####################################################################### */

    public class MetaEditor extends FlowPanel {

        final protected Attribute attribute;

        public MetaEditor(Attribute attribute) {
            this.attribute = attribute;

            if (attribute.hasMetadata()) {
                for (MetadataItem item : attribute.getMetadata().all()) {
                    FormGroup formGroup = new FormGroup();

                    FormLabel label = new FormLabel();
                    label.addStyleName("largest");
                    label.setText(item.getName());
                    formGroup.addFormLabel(label);

                    FormField formField = new FormField();
                    formField.add(createEditor(item));
                    formGroup.addFormField(formField);

                    FormGroupActions formGroupActions = new FormGroupActions();

                    FormButton deleteButton = new FormButton();
                    deleteButton.setText(container.getMessages().deleteItem());
                    deleteButton.setIcon("remove");
                    deleteButton.addClickHandler(clickEvent -> {
                        environment.getEventBus().dispatch(new ShowInfoEvent("TODO: Not implemented"));
                    });
                    formGroupActions.add(deleteButton);

                    formGroup.addFormGroupActions(formGroupActions);

                    add(formGroup);
                }
            }
        }
    }
}
