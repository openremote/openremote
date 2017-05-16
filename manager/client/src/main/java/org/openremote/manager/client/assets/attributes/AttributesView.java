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
import org.openremote.manager.client.app.dialog.JsonEditor;
import org.openremote.manager.client.event.ShowFailureEvent;
import org.openremote.manager.client.event.ShowInfoEvent;
import org.openremote.manager.client.event.ShowSuccessEvent;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.util.CollectionsUtil;
import org.openremote.manager.client.widget.*;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.Constants;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

public abstract class AttributesView<
    C extends AttributesView.Container<S>,
    S extends AttributesView.Style
    > {

    private static final Logger LOG = Logger.getLogger(AttributesView.class.getName());

    public interface Container<S extends AttributesView.Style> {
        S getStyle();

        InsertPanel getPanel();

        JsonEditor getJsonEditor();

        ManagerMessages getMessages();
    }

    public interface Style {

        String stringEditor();

        String numberEditor();

        String booleanEditor();
    }

    public interface AttributeEditor extends IsWidget {
    }

    public class ValueUpdate<T> {

        final String fieldLabel;
        final FormGroup formGroup;
        final AbstractValueHolder valueHolder;
        final Consumer<Boolean> resultConsumer;
        final T rawValue;

        public ValueUpdate(String fieldLabel, FormGroup formGroup, AbstractValueHolder valueHolder, Consumer<Boolean> resultConsumer, T rawValue) {
            this.fieldLabel = fieldLabel;
            this.formGroup = formGroup;
            this.valueHolder = valueHolder;
            this.resultConsumer = resultConsumer;
            this.rawValue = rawValue;
        }
    }

    public abstract class ValueUpdater<T> implements Consumer<ValueUpdate<T>> {

        @Override
        public void accept(ValueUpdate<T> valueUpdate) {
            valueUpdate.formGroup.setError(false);
            List<ValidationFailure> failures = new ArrayList<>();
            try {
                valueUpdate.valueHolder.setValue(
                    valueUpdate.rawValue != null ? createValue(valueUpdate.rawValue) : null
                );
                failures.addAll(valueUpdate.valueHolder.getValidationFailures());
            } catch (IllegalArgumentException ex) {
                failures.add(ex::getMessage);
            }
            if (!failures.isEmpty()) {
                valueUpdate.formGroup.setError(true);
                for (ValidationFailure failure : failures) {
                    showValidationError(valueUpdate.fieldLabel, failure);
                }
            }
            if (valueUpdate.resultConsumer != null) {
                valueUpdate.resultConsumer.accept(failures.isEmpty());
            }
        }

        abstract Value createValue(T rawValue) throws IllegalArgumentException;
    }

    public enum ConversionValidationFailure implements ValidationFailure {
        NOT_A_VALID_NUMBER
    }

    public final ValueUpdater<String> STRING_UPDATER = new ValueUpdater<String>() {
        @Override
        Value createValue(String rawValue) throws IllegalArgumentException {
            return Values.create(rawValue);
        }
    };

    public final ValueUpdater<String> DOUBLE_UPDATER = new ValueUpdater<String>() {
        @Override
        Value createValue(String rawValue) throws IllegalArgumentException {
            try {
                return Values.create(Double.valueOf(rawValue));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(ConversionValidationFailure.NOT_A_VALID_NUMBER.name());
            }
        }
    };

    public final ValueUpdater<Boolean> BOOLEAN_UPDATER = new ValueUpdater<Boolean>() {
        @Override
        Value createValue(Boolean rawValue) throws IllegalArgumentException {
            return Values.create(rawValue);
        }
    };

    public final ValueUpdater<Value> VALUE_UPDATER = new ValueUpdater<Value>() {
        @Override
        Value createValue(Value rawValue) throws IllegalArgumentException {
            return rawValue;
        }
    };

    public static class TimestampLabel extends FlowPanel {

        static protected DateTimeFormat dateFormat = DateTimeFormat.getFormat(Constants.DEFAULT_DATE_FORMAT);
        static protected DateTimeFormat timeFormat = DateTimeFormat.getFormat(Constants.DEFAULT_TIME_FORMAT);

        public TimestampLabel(Long timestamp) {
            addStyleName("flex layout vertical end or-FormInfoLabel");
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

    public boolean validateAttributes() {
        boolean isValid = true;
        for (AssetAttribute attribute : attributes) {
            if (!validateAttribute(attribute, false))
                isValid = false;
        }
        if (!isValid) {
            showValidationError(environment.getMessages().invalidAssetAttributes());
        }
        return isValid;
    }

    public boolean validateAttribute(AssetAttribute attribute, boolean showValidationFailureMessage) {
        // If there is already an error displayed, don't do other validation
        FormGroup formGroup = attributeGroups.get(attribute);
        if (formGroup.isError()) {
            return false;
        }

        List<ValidationFailure> failures = attribute.getValidationFailures();
        if (!failures.isEmpty()) {
            LOG.fine("Invalid " + attribute + ": " + failures);
            attributeGroups.get(attribute).setError(true);
            if (showValidationFailureMessage) {
                for (ValidationFailure failure : failures) {
                    showValidationError(attribute.getLabelOrName().orElse(null), failure);
                }
            }
            return false;
        }
        return true;
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

        formGroup.addFormLabel(createAttributeLabel(attribute));

        StringBuilder infoText = new StringBuilder();
        if (attribute.isExecutable()) {
            infoText.append(environment.getMessages().executable());
        } else if (attribute.getType().isPresent()) {
            infoText.append(environment.getMessages().attributeType(attribute.getType().get().name()));
        }
        getAttributeDescription(attribute)
            .ifPresent(description -> {
                if (infoText.length() > 0)
                    infoText.append(" - ");
                infoText.append(description);
            });
        if (infoText.length() > 0) {
            formGroup.addInfolabel(new Label(infoText.toString()));
        }

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
        FormLabel formLabel = new FormLabel(TextUtil.ellipsize(getAttributeLabel(attribute), 100));
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

        AttributeType attributeType = attribute.getType().orElse(null);
        if (attributeType == null)
            return null;

        AttributeEditor attributeEditor;
        if (attributeType == AttributeType.STRING || attributeType.getValueType() == ValueType.STRING) {
            attributeEditor = createStringEditor(attribute, defaultValueItem, style, formGroup);
        } else if (attributeType == AttributeType.NUMBER) {
            attributeEditor = createNumberEditor(attribute, defaultValueItem, style, formGroup);
        } else if (attributeType == AttributeType.BOOLEAN || attributeType.getValueType() == ValueType.BOOLEAN) {
            attributeEditor = createBooleanEditor(attribute, defaultValueItem, style, formGroup);
        } else if (attributeType.getValueType() == ValueType.ARRAY) {
            attributeEditor = createArrayEditor(attribute, style, formGroup);
        } else {
            return null;
        }
        return attributeEditor;
    }

    protected AttributeEditor createUnsupportedEditor(AssetAttribute attribute) {
        FormField unsupportedField = new FormField();
        unsupportedField.add(new FormOutputText(
            environment.getMessages().unsupportedAttributeType(
                attribute.getType().map(AttributeType::name).orElse(null)
            )
        ));
        return () -> unsupportedField;
    }

    protected AttributeEditor createStringEditor(AssetAttribute attribute, Optional<MetaItem> defaultValueItem, S style, FormGroup formGroup) {
        String currentValue = attribute.getValue().map(Object::toString).orElse(null);
        Optional<String> defaultValue = defaultValueItem.flatMap(AbstractValueHolder::getValueAsString);

        Consumer<String> updateConsumer = !isEditorReadOnly(attribute)
            ? rawValue -> STRING_UPDATER.accept(new ValueUpdate<>(attribute.getLabelOrName().orElse(null), formGroup, attribute, null, rawValue))
            : null;

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
                    event.getValue() == null || event.getValue().length() == 0 ? null : event.getValue()
                )
            );
        } else {
            input.setReadOnly(true);
        }
        return input;
    }

    protected AttributeEditor createNumberEditor(AssetAttribute attribute, Optional<MetaItem> defaultValueItem, S style, FormGroup formGroup) {
        String currentValue = attribute.getValue().map(Object::toString).orElse(null);
        Optional<String> defaultValue = defaultValueItem.flatMap(AbstractValueHolder::getValueAsString);

        Consumer<String> updateConsumer = !isEditorReadOnly(attribute)
            ? rawValue -> DOUBLE_UPDATER.accept(new ValueUpdate<>(attribute.getLabelOrName().orElse(null), formGroup, attribute, null, rawValue))
            : null;

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center");
        FormInputText inputText = createNumberEditorWidget(style, currentValue, defaultValue, updateConsumer);
        panel.add(inputText);
        if (isShowTimestamp(attribute)) {
            TimestampLabel timestampLabel = new TimestampLabel(attribute.getValueTimestamp());
            panel.add(timestampLabel);
        }

        return () -> panel;
    }

    protected FormInputText createNumberEditorWidget(S style,
                                                     String currentValue,
                                                     Optional<String> defaultValue,
                                                     Consumer<String> updateConsumer) {
        FormInputText input = createFormInputText(style.numberEditor());

        if (currentValue != null) {
            input.setValue(currentValue);
        } else defaultValue.ifPresent(input::setValue);

        if (updateConsumer != null) {
            input.addValueChangeHandler(event -> {
                updateConsumer.accept(
                    event.getValue() == null || event.getValue().length() == 0 ? null : event.getValue()
                );
            });
        } else {
            input.setReadOnly(true);
        }
        return input;
    }

    protected AttributeEditor createBooleanEditor(AssetAttribute attribute, Optional<MetaItem> defaultValueItem, S style, FormGroup formGroup) {
        boolean currentValue = attribute.getValueAsBoolean().orElse(false); // An empty boolean attribute value is false
        Optional<Boolean> defaultValue = defaultValueItem.flatMap(AbstractValueHolder::getValueAsBoolean);

        Consumer<Boolean> updateConsumer = !isEditorReadOnly(attribute)
            ? rawValue -> BOOLEAN_UPDATER.accept(new ValueUpdate<>(attribute.getLabelOrName().orElse(null), formGroup, attribute, null, rawValue))
            : null;

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center");
        FormCheckBox checkBox = createBooleanEditorWidget(style, currentValue, defaultValue, updateConsumer);
        FlowPanel checkBoxWrapper = new FlowPanel();
        checkBoxWrapper.setStyleName("flex layout horizontal center");
        checkBoxWrapper.add(checkBox);
        panel.add(checkBoxWrapper);
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

    protected AttributeEditor createArrayEditor(AssetAttribute attribute, S style, FormGroup formGroup) {
        Optional<ArrayValue> currentValue = attribute.getValue().flatMap(Values::getArray);

        Consumer<Value> updateConsumer = !isEditorReadOnly(attribute)
            ? rawValue -> VALUE_UPDATER.accept(new ValueUpdate<>(attribute.getLabelOrName().orElse(null), formGroup, attribute, null, rawValue))
            : null;

        Supplier<Value> resetSupplier = () -> attribute.getValue().orElse(null);

        String title = updateConsumer != null
            ? environment.getMessages().edit() + " " + environment.getMessages().jsonArray()
            : environment.getMessages().jsonArray();

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center");
        FormButton editButton = createJsonEditorWidget(style, title, currentValue.orElse(null), updateConsumer, resetSupplier);
        FlowPanel wrapper = new FlowPanel();
        wrapper.setStyleName("flex layout horizontal center");
        wrapper.add(editButton);
        panel.add(wrapper);
        if (isShowTimestamp(attribute)) {
            TimestampLabel timestampLabel = new TimestampLabel(attribute.getValueTimestamp());
            panel.add(timestampLabel);
        }

        return () -> panel;
    }

    protected AttributeEditor createObjectEditor(AssetAttribute attribute, S style, FormGroup formGroup) {
        Optional<ObjectValue> currentValue = attribute.getValue().flatMap(Values::getObject);

        Consumer<Value> updateConsumer = !isEditorReadOnly(attribute)
            ? rawValue -> VALUE_UPDATER.accept(new ValueUpdate<>(attribute.getLabelOrName().orElse(null), formGroup, attribute, null, rawValue))
            : null;

        Supplier<Value> resetSupplier = () -> attribute.getValue().orElse(null);

        String title = updateConsumer != null
            ? environment.getMessages().edit() + " " + environment.getMessages().jsonObject()
            : environment.getMessages().jsonObject();

        FlowPanel panel = new FlowPanel();
        panel.setStyleName("flex layout horizontal center");
        FormButton editButton = createJsonEditorWidget(style, title, currentValue.orElse(null), updateConsumer, resetSupplier);
        FlowPanel wrapper = new FlowPanel();
        wrapper.setStyleName("flex layout horizontal center");
        wrapper.add(editButton);
        panel.add(wrapper);
        if (isShowTimestamp(attribute)) {
            TimestampLabel timestampLabel = new TimestampLabel(attribute.getValueTimestamp());
            panel.add(timestampLabel);
        }

        return () -> panel;
    }

    protected FormButton createJsonEditorWidget(S style,
                                                String title,
                                                Value currentValue,
                                                Consumer<Value> updateConsumer,
                                                Supplier<Value> resetSupplier) {
        JsonEditor jsonEditor = container.getJsonEditor();
        jsonEditor.setTitle(title);

        if (currentValue != null) {
            jsonEditor.setValue(currentValue);
        }

        FormButton button = new FormButton();
        button.setIcon("file-text-o");
        button.setText(environment.getMessages().jsonArray());
        button.addClickHandler(event -> jsonEditor.show());

        jsonEditor.setOnReset(() -> jsonEditor.setValue(resetSupplier.get()));

        if (updateConsumer != null) {
            jsonEditor.setOnApply(updateConsumer);
        }
        return button;
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
        environment.getEventBus().dispatch(new ShowFailureEvent(error, 5000));
    }

    protected void showValidationError(String fieldLabel, ValidationFailure validationFailure) {
        StringBuilder error = new StringBuilder();
        if (fieldLabel != null)
            error.append(environment.getMessages().validationFailedFor(fieldLabel));
        else
            error.append(environment.getMessages().validationFailed());
        if (validationFailure != null)
            error.append(": ").append(environment.getMessages().validationFailure(validationFailure.name()));
        showValidationError(error.toString());
    }
}
