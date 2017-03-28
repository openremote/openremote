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
package org.openremote.manager.client.assets.attributes;

import com.google.gwt.user.client.ui.InsertPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.event.ShowFailureEvent;
import org.openremote.manager.client.event.ShowInfoEvent;
import org.openremote.manager.client.event.ShowSuccessEvent;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.util.CollectionsUtil;
import org.openremote.manager.client.util.TextUtil;
import org.openremote.manager.client.widget.*;
import org.openremote.model.AttributeType;
import org.openremote.model.Consumer;
import org.openremote.model.MetaItem;
import org.openremote.model.asset.AbstractAssetAttribute;
import org.openremote.model.asset.AbstractAssetAttributes;
import org.openremote.model.asset.AssetMeta;

import java.util.Comparator;
import java.util.LinkedHashMap;

public abstract class AttributesView<
    C extends AttributesView.Container<S>,
    S extends AttributesView.Style,
    ATTRIBUTES extends AbstractAssetAttributes<?, A>,
    A extends AbstractAssetAttribute
    > {

    public interface Container<S extends AttributesView.Style> {
        S getStyle();

        InsertPanel getPanel();

        ManagerMessages getMessages();
    }

    public interface Style {

        String stringEditor();

        String integerEditor();

        String decimalEditor();

        String booleanEditor();
    }

    final protected Environment environment;
    final protected C container;
    final protected ATTRIBUTES attributes;
    final protected LinkedHashMap<A, FormGroup> attributeGroups = new LinkedHashMap<>();

    public AttributesView(Environment environment, C container, ATTRIBUTES attributes) {
        this.environment = environment;
        this.container = container;
        this.attributes = attributes;
    }

    public ATTRIBUTES getAttributes() {
        return attributes;
    }

    public void clear() {
        while (container.getPanel().getWidgetCount() > 0) {
            container.getPanel().remove(0);
        }
        attributeGroups.clear();
    }

    public void build() {
        clear();
        createAttributeGroups();
    }

    public void setOpaque(boolean opaque) {
        for (FormGroup formGroup : attributeGroups.values()) {
            formGroup.setOpaque(opaque);
        }
    }

    public void close() {
        // Subclasses can manage lifecycle
    }

    /* ####################################################################### */

    protected void addAttributeActions(A attribute,
                                       FormGroup formGroup,
                                       FormField formField,
                                       FormGroupActions formGroupActions,
                                       IsWidget editor) {
    }

    protected void addAttributeExtensions(A attribute,
                                          FormGroup formGroup) {
    }

    /* ####################################################################### */

    protected void createAttributeGroups() {
        for (A attribute : attributes.get()) {
            FormGroup formGroup = createAttributeGroup(attribute);
            if (formGroup != null) {
                attributeGroups.put(attribute, formGroup);
            }
        }
        // Sort form groups by label text ascending
        CollectionsUtil.sortMap(attributeGroups, Comparator.comparing(a -> a.getFormLabel().getText()));

        for (FormGroup attributeGroup : attributeGroups.values()) {
            container.getPanel().add(attributeGroup);
        }
    }

    protected FormGroup createAttributeGroup(A attribute) {

        FormGroup formGroup = new FormGroup();

        FormLabel formLabel = createAttributeLabel(attribute);
        formGroup.addFormLabel(formLabel);

        String description = getAttributeDescription(attribute);
        if (description != null) {
            formGroup.addInfolabel(new Label(description));
        }

        FormGroupActions formGroupActions = new FormGroupActions();

        FormField formField = new FormField();

        IsWidget editor = createEditor(attribute, formGroup);

        addAttributeActions(attribute, formGroup, formField, formGroupActions, editor);

        if (editor == null)
            editor = createUnsupportedEditor(attribute);
        formField.add(editor);

        formGroup.addFormField(formField);
        formGroup.addFormGroupActions(formGroupActions);

        addAttributeExtensions(attribute, formGroup);

        return formGroup;
    }

    protected FormLabel createAttributeLabel(A attribute) {
        String label = getAttributeLabel(attribute);
        FormLabel formLabel = new FormLabel(TextUtil.ellipsize(label, 30));
        formLabel.addStyleName("larger");
        return formLabel;
    }

    protected String getAttributeLabel(A attribute) {
        String label = attribute.getName();
        MetaItem labelItem = attribute.firstMetaItem(AssetMeta.LABEL);
        if (labelItem != null) {
            label = labelItem.getValueAsString();
        }
        return label;
    }

    protected String getAttributeDescription(A attribute) {
        MetaItem description = attribute.firstMetaItem(AssetMeta.DESCRIPTION);
        if (description != null) {
            return description.getValueAsString();
        }
        return null;
    }

    protected IsWidget createEditor(A attribute, FormGroup formGroup) {
        IsWidget editor;
        S style = container.getStyle();
        MetaItem defaultValueItem = attribute.firstMetaItem(AssetMeta.DEFAULT);
        Consumer<String> errorConsumer = msg -> {
            formGroup.setError(true);
            showValidationError(msg);
        };

        if (attribute.getType().equals(AttributeType.STRING)) {
            String currentValue = attribute.getValueAsString();
            String defaultValue = defaultValueItem != null ? defaultValueItem.getValueAsString() : null;
            Consumer<String> updateConsumer = isDefaultReadOnly(attribute) ? null : value -> {
                formGroup.setError(false);
                attribute.setValueAsString(value);
            };
            editor = createStringEditor(style, currentValue, defaultValue, updateConsumer);
        } else if (attribute.getType().equals(AttributeType.INTEGER)) {
            Integer currentValue = attribute.getValueAsInteger();
            Integer defaultValue = defaultValueItem != null ? defaultValueItem.getValueAsInteger() : null;
            Consumer<Integer> updateConsumer = isDefaultReadOnly(attribute) ? null : value -> {
                formGroup.setError(false);
                attribute.setValueAsInteger(value);
            };
            editor = createIntegerEditor(style, currentValue, defaultValue, updateConsumer, errorConsumer);
        } else if (attribute.getType().equals(AttributeType.DECIMAL)) {
            Double currentValue = attribute.getValueAsDecimal();
            Double defaultValue = defaultValueItem != null ? defaultValueItem.getValueAsDecimal() : null;
            Consumer<Double> updateConsumer = isDefaultReadOnly(attribute) ? null : value -> {
                formGroup.setError(false);
                attribute.setValueAsDecimal(value);
            };
            editor = createDecimalEditor(style, currentValue, defaultValue, updateConsumer, errorConsumer);
        } else if (attribute.getType().equals(AttributeType.BOOLEAN)) {
            Boolean currentValue = attribute.getValueAsBoolean();
            Boolean defaultValue = defaultValueItem != null ? defaultValueItem.getValueAsBoolean() : null;
            Consumer<Boolean> updateConsumer = isDefaultReadOnly(attribute) ? null : value -> {
                formGroup.setError(false);
                attribute.setValueAsBoolean(value);
            };
            editor = createBooleanEditor(style, currentValue, defaultValue, updateConsumer);
        } else {
            return null;
        }
        return editor;
    }

    protected IsWidget createUnsupportedEditor(A attribute) {
        FormField unsupportedField = new FormField();
        unsupportedField.add(new FormOutputText(
            environment.getMessages().unsupportedAttributeType(attribute.getType().getValue())
        ));
        return unsupportedField;
    }

    protected FormInputText createStringEditor(S style,
                                               String currentValue,
                                               String defaultValue,
                                               Consumer<String> updateConsumer) {
        FormInputText input = createFormInputText(style.stringEditor());

        if (currentValue != null) {
            input.setValue(currentValue);
        } else if ((defaultValue) != null) {
            input.setValue(defaultValue);
        }

        if (updateConsumer != null) {
            input.addValueChangeHandler(
                event -> updateConsumer.accept(
                    event.getValue() != null && event.getValue().length() > 0
                        ? event.getValue()
                        : null
                )
            );
        } else {
            input.setReadOnly(true);
        }
        return input;
    }

    protected FormInputNumber createIntegerEditor(S style,
                                                  Integer currentValue,
                                                  Integer defaultValue,
                                                  Consumer<Integer> updateConsumer,
                                                  Consumer<String> errorConsumer) {
        FormInputNumber input = createFormInputNumber(style.integerEditor());

        if (currentValue != null) {
            input.setValue(currentValue.toString());
        } else if (defaultValue != null) {
            input.setValue(defaultValue.toString());
        }

        if (updateConsumer != null) {
            input.addValueChangeHandler(event -> {
                try {
                    updateConsumer.accept(
                        event.getValue() != null && event.getValue().length() > 0
                            ? Integer.valueOf(event.getValue())
                            : null
                    );
                } catch (NumberFormatException ex) {
                    errorConsumer.accept(environment.getMessages().enterOnlyNumbers());
                }
            });
        } else {
            input.setReadOnly(true);
        }
        return input;
    }

    protected FormInputText createDecimalEditor(S style,
                                                Double currentValue,
                                                Double defaultValue,
                                                Consumer<Double> updateConsumer,
                                                Consumer<String> errorConsumer) {
        FormInputText input = createFormInputText(style.decimalEditor());

        if (currentValue != null) {
            input.setValue(currentValue.toString());
        } else if (defaultValue != null) {
            input.setValue(defaultValue.toString());
        }

        if (updateConsumer != null) {
            input.addValueChangeHandler(event -> {
                try {
                    updateConsumer.accept(
                        event.getValue() != null && event.getValue().length() > 0
                            ? Double.valueOf(event.getValue())
                            : null
                    );
                } catch (NumberFormatException ex) {
                    errorConsumer.accept(environment.getMessages().enterOnlyDecimals());
                }
            });
        } else {
            input.setReadOnly(true);
        }
        return input;
    }

    protected FormCheckBox createBooleanEditor(S style,
                                               Boolean currentValue,
                                               Boolean defaultValue,
                                               Consumer<Boolean> updateConsumer) {
        FormCheckBox input = createFormInputCheckBox(style.booleanEditor());

        Boolean value = null;
        if (currentValue != null) {
            value = currentValue;
        } else if (defaultValue != null) {
            value = defaultValue;
        }

        input.setValue(value);

        if (updateConsumer != null) {
            input.addValueChangeHandler(event -> updateConsumer.accept(input.getValue()));
        } else {
            input.setEnabled(false);
        }
        return input;
    }

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

    protected boolean isDefaultReadOnly(A attribute) {
        return false;
    }

    protected void removeAttribute(A attribute) {
        attributes.remove(attribute.getName());
        int attributeGroupIndex = container.getPanel().getWidgetIndex(attributeGroups.get(attribute));
        container.getPanel().remove(attributeGroupIndex);
        attributeGroups.remove(attribute);
    }

    protected void showInfo(String text) {
        environment.getEventBus().dispatch(new ShowInfoEvent(text));
    }

    protected void showSuccess(String text) {
        environment.getEventBus().dispatch(new ShowSuccessEvent(text));
    }

    protected void showValidationError(String error) {
        environment.getEventBus().dispatch(new ShowFailureEvent(error, 3000));
    }

}
