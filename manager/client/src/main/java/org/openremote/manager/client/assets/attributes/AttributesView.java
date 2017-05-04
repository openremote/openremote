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

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InsertPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.event.ShowFailureEvent;
import org.openremote.manager.client.event.ShowInfoEvent;
import org.openremote.manager.client.event.ShowSuccessEvent;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.util.CollectionsUtil;
import org.openremote.manager.client.widget.*;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.Constants;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.Values;

import java.util.*;
import java.util.function.Consumer;

public abstract class AttributesView<
    C extends AttributesView.Container<S>,
    S extends AttributesView.Style
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

    public interface AttributeEditor extends IsWidget {
    }

    public static class TimestampLabel extends FlowPanel {

        static protected DateTimeFormat dateFormat = DateTimeFormat.getFormat(Constants.DEFAULT_DATE_FORMAT);
        static protected DateTimeFormat timeFormat = DateTimeFormat.getFormat(Constants.DEFAULT_TIME_FORMAT);

        public TimestampLabel(Long timestamp) {
            addStyleName("flex");
            getElement().getStyle().setFontSize(9, com.google.gwt.dom.client.Style.Unit.PX);
            getElement().getStyle().setTextAlign(com.google.gwt.dom.client.Style.TextAlign.RIGHT);
            if (timestamp != null) {
                setTimestamp(timestamp);
            }
        }

        public void setTimestamp(long timestamp) {
            clear();
            if (timestamp > 0) {
                add(new FormOutputText(dateFormat.format(new Date(timestamp))));
                add(new FormOutputText(timeFormat.format(new Date(timestamp))));
            }
        }
    }


    final protected Environment environment;
    final protected C container;
    final protected List<AssetAttribute> attributes;
    final protected Map<String, AttributeEditor> editors = new HashMap<>();
    final protected LinkedHashMap<AssetAttribute, FormGroup> attributeGroups = new LinkedHashMap<>();

    public AttributesView(Environment environment, C container, List<AssetAttribute> attributes) {
        this.environment = environment;
        this.container = container;
        this.attributes = attributes;
    }

    public List<AssetAttribute> getAttributes() {
        return attributes;
    }

    public void clear() {
        while (container.getPanel().getWidgetCount() > 0) {
            container.getPanel().remove(0);
        }
        editors.clear();
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

    protected void addAttributeActions(AssetAttribute attribute,
                                       FormGroup formGroup,
                                       FormField formField,
                                       FormGroupActions formGroupActions,
                                       IsWidget editor) {
    }

    protected void addAttributeExtensions(AssetAttribute attribute,
                                          FormGroup formGroup) {
    }

    /* ####################################################################### */

    protected void createAttributeGroups() {
        for (AssetAttribute attribute : attributes) {
            FormGroup formGroup = createAttributeGroup(attribute);
            if (formGroup != null) {
                attributeGroups.put(attribute, formGroup);
            }
        }

        sortAttributes(attributeGroups);

        for (FormGroup attributeGroup : attributeGroups.values()) {
            container.getPanel().add(attributeGroup);
        }
    }

    protected FormGroup createAttributeGroup(AssetAttribute attribute) {

        FormGroup formGroup = new FormGroup();

        FormLabel formLabel = createAttributeLabel(attribute);
        formGroup.addFormLabel(formLabel);

        Optional<String> description = getAttributeDescription(attribute);
        description.ifPresent(s -> formGroup.addInfolabel(new Label(s)));

        FormGroupActions formGroupActions = new FormGroupActions();

        FormField formField = new FormField();

        AttributeEditor attributeEditor = createEditor(attribute, formGroup);

        addAttributeActions(attribute, formGroup, formField, formGroupActions, attributeEditor);

        if (attributeEditor == null)
            attributeEditor = createUnsupportedEditor(attribute);

        formField.add(attributeEditor);

        formGroup.addFormField(formField);
        formGroup.addFormGroupActions(formGroupActions);

        addAttributeExtensions(attribute, formGroup);

        editors.put(attribute.getName().orElse(null), attributeEditor);
        return formGroup;
    }

    protected FormLabel createAttributeLabel(AssetAttribute attribute) {
        FormLabel formLabel =  new FormLabel(TextUtil.ellipsize(getAttributeLabel(attribute), 100));
        formLabel.addStyleName("larger");
        return formLabel;
    }

    protected String getAttributeLabel(AssetAttribute attribute) {
        return attribute
            .getLabel()
            .orElse(attribute.getName().orElse(""));
    }

    protected Optional<String> getAttributeDescription(AssetAttribute attribute) {
        return attribute.getDescription();
    }

    protected AttributeEditor createEditor(AssetAttribute attribute, FormGroup formGroup) {
        Optional<MetaItem> defaultValueItem = attribute.getMetaItem(AssetMeta.DEFAULT);
        S style = container.getStyle();

        AttributeEditor attributeEditor;
        AttributeType attributeType = attribute.getType().orElse(AttributeType.STRING);
        if (attributeType == AttributeType.STRING) {
            attributeEditor = createStringEditor(attribute, defaultValueItem, style, formGroup);
        } else if (attributeType == AttributeType.INTEGER) {
            attributeEditor = createIntegerEditor(attribute, defaultValueItem, style, formGroup);
        } else if (attributeType == AttributeType.DECIMAL) {
            attributeEditor = createDecimalEditor(attribute, defaultValueItem, style, formGroup);
        } else if (attributeType == AttributeType.BOOLEAN) {
            attributeEditor = createBooleanEditor(attribute, defaultValueItem, style, formGroup);
        } else {
            return null;
        }
        return attributeEditor;
    }

    protected AttributeEditor createUnsupportedEditor(AssetAttribute attribute) {
        FormField unsupportedField = new FormField();
        unsupportedField.add(new FormOutputText(
            environment.getMessages().unsupportedAttributeType(
                attribute.getType().map(AttributeType::getDisplayName).orElse(null)
            )
        ));
        return () -> unsupportedField;
    }

    protected AttributeEditor createStringEditor(AssetAttribute attribute, Optional<MetaItem> defaultValueItem, S style, FormGroup formGroup) {
        String currentValue = attribute.getValue().map(Object::toString).orElse(null);
        Optional<String> defaultValue = defaultValueItem.flatMap(AbstractValueHolder::getValueAsString);

        Consumer<String> updateConsumer = isEditorReadOnly(attribute) ? null : value -> {
            formGroup.setError(false);
            attribute.setValue(Values.create(value));
        };

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center");
        FormInputText inputText = createStringEditorWidget(style, currentValue, defaultValue, updateConsumer);
        panel.add(inputText);
        if (isShowTimestamp(attribute)) {
            TimestampLabel timestampLabel = new TimestampLabel(attribute.getValueTimestamp());
            panel.add(timestampLabel);
        }

        return () -> panel;
    }

    protected FormInputText createStringEditorWidget(S style,
                                                     String currentValue,
                                                     Optional<String> defaultValue,
                                                     Consumer<String> updateConsumer) {
        FormInputText input = createFormInputText(style.stringEditor());

        if (currentValue != null) {
            input.setValue(currentValue);
        } else defaultValue.ifPresent(input::setValue);

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

    protected AttributeEditor createIntegerEditor(AssetAttribute attribute, Optional<MetaItem> defaultValueItem, S style, FormGroup formGroup) {
        String currentValue = attribute.getValue().map(Object::toString).orElse(null);
        Optional<String> defaultValue = defaultValueItem.flatMap(AbstractValueHolder::getValueAsString);
        Consumer<String> updateConsumer = isEditorReadOnly(attribute) ? null : value -> {
            Integer intValue = Integer.valueOf(value);
            formGroup.setError(false);
            attribute.setValue(Values.create(intValue));
        };

        Consumer<String> errorConsumer = msg -> {
            formGroup.setError(true);
            showValidationError(msg);
        };

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center");
        FormInputNumber inputNumber = createIntegerEditorWidget(style, currentValue, defaultValue, updateConsumer, errorConsumer);
        panel.add(inputNumber);
        if (isShowTimestamp(attribute)) {
            TimestampLabel timestampLabel = new TimestampLabel(attribute.getValueTimestamp());
            panel.add(timestampLabel);
        }

        return () -> panel;
    }

    protected FormInputNumber createIntegerEditorWidget(S style,
                                                        String currentValue,
                                                        Optional<String> defaultValue,
                                                        Consumer<String> updateConsumer,
                                                        Consumer<String> errorConsumer) {
        FormInputNumber input = createFormInputNumber(style.integerEditor());

        if (currentValue != null) {
            input.setValue(currentValue);
        } else defaultValue.ifPresent(input::setValue);

        if (updateConsumer != null) {
            input.addValueChangeHandler(event -> {
                try {
                    updateConsumer.accept(
                        event.getValue() != null && event.getValue().length() > 0
                            ? event.getValue()
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

    protected AttributeEditor createDecimalEditor(AssetAttribute attribute, Optional<MetaItem> defaultValueItem, S style, FormGroup formGroup) {
        String currentValue = attribute.getValue().map(Object::toString).orElse(null);
        Optional<String> defaultValue = defaultValueItem.flatMap(AbstractValueHolder::getValueAsString);
        Consumer<String> updateConsumer = isEditorReadOnly(attribute) ? null : value -> {
            Double decimalValue = Double.valueOf(value);
            formGroup.setError(false);
            attribute.setValue(Values.create(decimalValue));
        };

        Consumer<String> errorConsumer = msg -> {
            formGroup.setError(true);
            showValidationError(msg);
        };

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center");
        FormInputText inputText = createDecimalEditorWidget(style, currentValue, defaultValue, updateConsumer, errorConsumer);
        panel.add(inputText);
        if (isShowTimestamp(attribute)) {
            TimestampLabel timestampLabel = new TimestampLabel(attribute.getValueTimestamp());
            panel.add(timestampLabel);
        }

        return () -> panel;
    }

    protected FormInputText createDecimalEditorWidget(S style,
                                                      String currentValue,
                                                      Optional<String> defaultValue,
                                                      Consumer<String> updateConsumer,
                                                      Consumer<String> errorConsumer) {
        FormInputText input = createFormInputText(style.decimalEditor());

        if (currentValue != null) {
            input.setValue(currentValue);
        } else defaultValue.ifPresent(input::setValue);

        if (updateConsumer != null) {
            input.addValueChangeHandler(event -> {
                try {
                    updateConsumer.accept(
                        event.getValue() != null && event.getValue().length() > 0
                            ? event.getValue()
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

    protected AttributeEditor createBooleanEditor(AssetAttribute attribute, Optional<MetaItem> defaultValueItem, S style, FormGroup formGroup) {
        // TODO: Should a boolean attribute with no value default to false?
        boolean currentValue = attribute.getValueAsBoolean().orElse(false);
        Optional<Boolean> defaultValue = defaultValueItem.flatMap(AbstractValueHolder::getValueAsBoolean);
        Consumer<Boolean> updateConsumer = isEditorReadOnly(attribute) ? null : value -> {
            formGroup.setError(false);
            attribute.setValue(Values.create(value));
        };

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center");
        FormCheckBox checkBox = createBooleanEditorWidget(style, currentValue, defaultValue, updateConsumer);
        panel.add(checkBox);
        if (isShowTimestamp(attribute)) {
            TimestampLabel timestampLabel = new TimestampLabel(attribute.getValueTimestamp());
            panel.add(timestampLabel);
        }

        return () -> panel;
    }

    protected FormCheckBox createBooleanEditorWidget(S style,
                                                     Boolean currentValue,
                                                     Optional<Boolean> defaultValue,
                                                     Consumer<Boolean> updateConsumer) {
        FormCheckBox input = createFormInputCheckBox(style.booleanEditor());

        Boolean value = null;
        if (currentValue != null) {
            value = currentValue;
        } else if (defaultValue.isPresent()) {
            value = defaultValue.get();
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

    protected boolean isEditorReadOnly(AssetAttribute attribute) {
        return attribute.isReadOnly();
    }

    protected boolean isShowTimestamp(AssetAttribute attribute) {
        return true;
    }

    protected void sortAttributes(LinkedHashMap<AssetAttribute, FormGroup> attributeGroups) {
        // Sort form groups by label text ascending
        CollectionsUtil.sortMap(attributeGroups, Comparator.comparing(
            entry -> entry.getValue().getFormLabel().getText())
        );
    }

    protected void removeAttribute(AssetAttribute attribute) {
        attributes.remove(attribute);

        attribute
            .getName()
            .map(editors::remove);

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
