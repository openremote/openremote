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

import com.google.gwt.user.client.ui.*;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.app.dialog.JsonEditor;
import org.openremote.manager.client.event.ShowFailureEvent;
import org.openremote.manager.client.event.ShowInfoEvent;
import org.openremote.manager.client.event.ShowSuccessEvent;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.util.CollectionsUtil;
import org.openremote.manager.client.widget.*;
import org.openremote.model.AbstractValueHolder;
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

import static org.openremote.manager.client.widget.ValueEditors.*;

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

        String regularAttribute();

        String highlightAttribute();
    }

    final protected Environment environment;
    final protected C container;
    final protected List<AssetAttribute> attributes;
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
                                       FormGroupActions formGroupActions) {
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

        if (attributeGroups.size() == 0) {
            Label emptyLabel = new Label(environment.getMessages().noAttributes());
            emptyLabel.addStyleName(environment.getWidgetStyle().FormListEmptyMessage());
            container.getPanel().add(emptyLabel);
        } else {
            for (FormGroup attributeGroup : attributeGroups.values()) {
                container.getPanel().add(attributeGroup);
            }
        }
    }

    protected FormGroup createAttributeGroup(AssetAttribute attribute) {
        FormGroup formGroup = new FormGroup();

        formGroup.addStyleName("flex-none");
        formGroup.addStyleName(environment.getWidgetStyle().FormListItem());

        if (attribute.hasAgentLink() || attribute.isProtocolConfiguration()) {
            formGroup.addStyleName(container.getStyle().highlightAttribute());
            formGroup.addStyleName(environment.getWidgetStyle().HighlightBorder());
        } else {
            formGroup.addStyleName(container.getStyle().regularAttribute());
            formGroup.addStyleName(environment.getWidgetStyle().RegularBorder());
        }

        FormLabel formLabel = createAttributeLabel(attribute);
        if (attribute.isExecutable()) {
            formLabel.setIcon("cog");
        } else if (attribute.isProtocolConfiguration()) {
            formLabel.setIcon("gears");
        } else {
            formLabel.setIcon(attribute.getType().map(AttributeType::getIcon).orElse(AttributeType.DEFAULT_ICON));
        }
        formGroup.addFormLabel(formLabel);

        StringBuilder infoText = new StringBuilder();
        if (attribute.isExecutable()) {
            infoText.append(environment.getMessages().executable());
        } else if (attribute.isProtocolConfiguration()) {
            infoText.append(environment.getMessages().protocolConfiguration());
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
        formField.add(createAttributeValueEditor(attribute, formGroup));

        addAttributeActions(attribute, formGroup, formField, formGroupActions);

        formGroup.addFormField(formField);
        formGroup.addFormGroupActions(formGroupActions);

        addAttributeExtensions(attribute, formGroup);

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

    protected IsWidget createAttributeValueEditor(AssetAttribute attribute, FormGroup formGroup) {
        Optional<MetaItem> defaultValueItem = attribute.getMetaItem(AssetMeta.DEFAULT);
        S style = container.getStyle();

        AttributeType attributeType = attribute.getType().orElse(null);
        if (attributeType == null)
            return null;

        boolean readOnly = isEditorReadOnly(attribute);
        boolean showTimestamp = isShowTimestamp(attribute);

        Consumer<List<ValidationFailure>> validationResultConsumer = failures -> {
            for (ValidationFailure failure : failures) {
                showValidationError(attribute.getLabelOrName().orElse(null), failure);
            }
        };

        IsWidget editor;
        if (attributeType == AttributeType.STRING || attributeType.getValueType() == ValueType.STRING) {
            String currentValue = attribute.getValue().map(Object::toString).orElse(null);
            String defaultValue = defaultValueItem.flatMap(AbstractValueHolder::getValueAsString).orElse(null);
            editor = createStringEditor(
                attribute, currentValue, defaultValue, readOnly, style.stringEditor(), formGroup, showTimestamp, validationResultConsumer
            );
        } else if (attributeType == AttributeType.NUMBER || attributeType.getValueType() == ValueType.NUMBER) {
            String currentValue = attribute.getValue().map(Object::toString).orElse(null);
            String defaultValue = defaultValueItem.flatMap(AbstractValueHolder::getValueAsString).orElse(null);
            editor = createNumberEditor(
                attribute, currentValue, defaultValue, readOnly, style.numberEditor(), formGroup, showTimestamp, validationResultConsumer
            );
        } else if (attributeType == AttributeType.BOOLEAN || attributeType.getValueType() == ValueType.BOOLEAN) {
            Boolean currentValue = attribute.getValueAsBoolean().orElse(null);
            Boolean defaultValue = defaultValueItem.flatMap(AbstractValueHolder::getValueAsBoolean).orElse(null);
            editor = createBooleanEditor(
                attribute, currentValue, defaultValue, readOnly, style.booleanEditor(), formGroup, showTimestamp, validationResultConsumer
            );
        } else if (attributeType == AttributeType.OBJECT || attributeType.getValueType() == ValueType.OBJECT) {
            ObjectValue currentValue = attribute.getValue().flatMap(Values::getObject).orElse(null);
            Supplier<Value> resetSupplier = () -> attribute.getValue().orElse(null);
            String label = environment.getMessages().jsonObject();
            String title = !readOnly
                ? environment.getMessages().edit() + " " + environment.getMessages().jsonObject()
                : environment.getMessages().jsonObject();

            editor = createObjectEditor(
                attribute, currentValue, resetSupplier, readOnly, label, title, container.getJsonEditor(), formGroup, showTimestamp, validationResultConsumer
            );
        } else if (attributeType == AttributeType.ARRAY || attributeType.getValueType() == ValueType.ARRAY) {
            ArrayValue currentValue = attribute.getValue().flatMap(Values::getArray).orElse(null);
            Supplier<Value> resetSupplier = () -> attribute.getValue().orElse(null);
            String label = environment.getMessages().jsonArray();
            String title = !readOnly
                ? environment.getMessages().edit() + " " + environment.getMessages().jsonArray()
                : environment.getMessages().jsonArray();
            editor = createArrayEditor(
                attribute, currentValue, resetSupplier, readOnly, label, title, container.getJsonEditor(), formGroup, showTimestamp, validationResultConsumer
            );
        } else {
            editor = new FormOutputText(
                environment.getMessages().unsupportedAttributeType(
                    attribute.getType().map(AttributeType::name).orElse(null)
                )
            );
        }
        return editor;
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

        int attributeGroupIndex = container.getPanel().getWidgetIndex(attributeGroups.get(attribute));
        container.getPanel().remove(attributeGroupIndex);
        attributeGroups.remove(attribute);

        if (attributeGroups.size() == 0) {
            Label emptyLabel = new Label(environment.getMessages().noAttributes());
            emptyLabel.addStyleName(environment.getWidgetStyle().FormListEmptyMessage());
            container.getPanel().add(emptyLabel);
        }
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

    public void showValidationError(String fieldLabel, ValidationFailure validationFailure) {
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
