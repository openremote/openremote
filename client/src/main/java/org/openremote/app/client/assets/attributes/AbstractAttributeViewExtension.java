/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.app.client.assets.attributes;

import org.openremote.app.client.Environment;
import org.openremote.app.client.widget.FormSection;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.interop.Consumer;

public abstract class AbstractAttributeViewExtension extends FormSection implements AttributeView {

    protected Environment environment;
    protected AssetAttribute attribute;
    protected Consumer<AssetAttribute> attributeModifiedCallback;
    protected ValueEditorSupplier valueEditorSupplier;
    protected boolean editMode;
    protected AttributeView.Style style;
    protected AttributeView parentView;
    protected ValidationErrorConsumer validationErrorConsumer;

    public AbstractAttributeViewExtension(Environment environment, AttributeView.Style style, AttributeView parentView, AssetAttribute attribute, String label) {
        super(label);
        this.environment = environment;
        this.style = style;
        this.parentView = parentView;
        this.attribute = attribute;
    }

    @Override
    public AssetAttribute getAttribute() {
        return attribute;
    }

    @Override
    public void setAttributeModifiedCallback(Consumer<AssetAttribute> attributeModifiedCallback) {
        this.attributeModifiedCallback = attributeModifiedCallback;
    }

    @Override
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    protected void notifyAttributeModified() {
        if (attributeModifiedCallback != null) {
            attributeModifiedCallback.accept(attribute);
        }
    }

    @Override
    public void setValueEditorSupplier(ValueEditorSupplier valueEditorSupplier) {
        this.valueEditorSupplier = valueEditorSupplier;
    }

    @Override
    public void setValidationErrorConsumer(ValidationErrorConsumer validationErrorConsumer) {
        this.validationErrorConsumer = validationErrorConsumer;
    }

    @Override
    public void setDisabled(boolean disabled) {
        super.setDisabled(disabled);
    }

    @Override
    public void setStatus(ConnectionStatus connectionStatus) {
        // TODO Ability for extensions to display some error highlight/visual marker
    }

    public void showValidationError(String attributeName, String metaItemName, ValidationFailure validationFailure) {
        if (validationErrorConsumer != null) {
            validationErrorConsumer.accept(attributeName, metaItemName, validationFailure);
        }
    }
}
