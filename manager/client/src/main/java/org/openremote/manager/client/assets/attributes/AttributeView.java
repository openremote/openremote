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
package org.openremote.manager.client.assets.attributes;

import com.google.gwt.user.client.ui.IsWidget;
import org.openremote.model.ValidationFailure;
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeValidationResult;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;

import java.util.function.Consumer;

/**
 * The presenter will deal with validating the {@link AssetAttribute}s and will notify the individual
 * {@link AttributeView}s when the validation state changes for an attribute.
 */
public interface AttributeView extends IsWidget {

    @FunctionalInterface
    interface ValidationErrorConsumer {
        void accept(String attributeName, String metaItemName, ValidationFailure validationFailure);
    }

    @FunctionalInterface
    interface ValueEditorSupplier {
        IsWidget createValueEditor(ValueHolder valueHolder, ValueType valueType, AttributeView.Style style, AttributeView parentView, Consumer<Value> onValueModified);
    }

    interface Style {

        String stringEditor();

        String numberEditor();

        String booleanEditor();

        String regularAttribute();

        String highlightAttribute();

        String highlightAttributeError();

        String metaItemNameEditor();

        String metaItemValueEditor();

        String agentLinkEditor();
    }

    /**
     * Get the attribute associated with this view.
     */
    AssetAttribute getAttribute();

    /**
     * This callback should be used by the view to notify the presenter that the attribute has been modified. Could be
     * a value change or meta item create, update or delete.
     */
    void setAttributeModifiedCallback(Consumer<AssetAttribute> attributeModifiedCallback);

    /**
     * This {@link ValueEditorSupplier} should be used to get a basic value editor/viewer for a {@link ValueHolder}.
     */
    void setValueEditorSupplier(ValueEditorSupplier valueEditorSupplier);

    /**
     * This should be used by the view to display failure messages to the user in a standard way.
     */
    void setValidationErrorConsumer(ValidationErrorConsumer validationErrorConsumer);

    /**
     * Called when the validation state of the attribute changes. {@link AttributeValidationResult#isValid()} will be
     * true when the attribute is valid.
     */
    void onValidationStateChange(AttributeValidationResult validationResult);

    /**
     * Called by the presenter when the attribute has changed outside of the system (i.e. it has changed on the server).
     */
    void onAttributeChanged(long timestamp);

    /**
     * Called by the presenter to indicate that some operation that affects the attribute linked to the view is pending
     * or has finished.
     */
    void setBusy(boolean busy);

    /**
     * Set the view to a disabled state
     */
    void setDisabled(boolean disabled);

    /**
     * Indicates if current activity is edit mode or view mode.
     */
    void setEditMode(boolean editMode);

    /**
     * Highlight the attribute as having an error.
     */
    void setHighlightError(boolean highlightError);
}
