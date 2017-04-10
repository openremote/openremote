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

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import elemental.client.Browser;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.widget.*;
import org.openremote.model.Attribute;
import org.openremote.model.AttributeEvent;
import org.openremote.model.ReadAttributesEvent;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetAttributes;
import org.openremote.model.event.bus.EventRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AttributesBrowser
    extends AttributesView<AttributesBrowser.Container, AttributesBrowser.Style, AssetAttributes, AssetAttribute> {

    private static final Logger LOG = Logger.getLogger(AttributesBrowser.class.getName());

    public interface Container extends AttributesView.Container<AttributesBrowser.Style> {

    }

    public interface Style extends AttributesView.Style {

    }

    final protected Asset asset;
    final protected List<FormButton> readButtons = new ArrayList<>();

    protected FormGroup liveUpdatesGroup;
    protected EventRegistration<AttributeEvent> eventRegistration;

    public AttributesBrowser(Environment environment, Container container, Asset asset) {
        super(environment, container, new AssetAttributes(asset));
        this.asset = asset;

        eventRegistration = environment.getEventBus().register(
            AttributeEvent.class,
            event -> {
                for (AssetAttribute assetAttribute : attributeGroups.keySet()) {
                    if (assetAttribute.getReference().equals(event.getAttributeRef())) {
                        assetAttribute.setValueUnchecked(event.getValue(), event.getTimestamp());
                        rebuildEditor(assetAttribute);
                        break;
                    }
                }
            }
        );
    }

    @Override
    protected void createAttributeGroups() {
        if (getAttributes().size() > 0) {
            liveUpdatesGroup = createLiveUpdatesGroup();
            container.getPanel().add(liveUpdatesGroup);
        }
        super.createAttributeGroups();
    }

    @Override
    public void setOpaque(boolean opaque) {
        super.setOpaque(opaque);
        liveUpdatesGroup.setOpaque(opaque);
    }

    @Override
    public void close() {
        super.close();

        if (eventRegistration != null) {
            environment.getEventBus().remove(eventRegistration);
            eventRegistration = null;
        }

        subscribeToLiveUpdates(false);
    }

    @Override
    protected void addAttributeActions(AssetAttribute attribute,
                                       FormGroup formGroup,
                                       FormField formField,
                                       FormGroupActions formGroupActions,
                                       IsWidget editor) {

        // Allow direct read/write of attribute if we understand attribute type (have an editor)
        if (editor != null) {
            FormButton writeValueButton = new FormButton();
            writeValueButton.setEnabled(!attribute.isReadOnly());
            writeValueButton.setText(container.getMessages().write());
            writeValueButton.setPrimary(true);
            writeValueButton.setIcon("cloud-upload");
            writeValueButton.addClickHandler(clickEvent -> writeAttributeValue(attribute));
            formGroupActions.add(writeValueButton);

            FormButton readValueButton = new FormButton();
            readValueButton.setText(container.getMessages().read());
            readValueButton.setIcon("cloud-download");
            readValueButton.addClickHandler(clickEvent -> readAttributeValue(attribute));
            readButtons.add(readValueButton);
            formGroupActions.add(readValueButton);
        }
    }

    @Override
    protected void addAttributeExtensions(AssetAttribute attribute,
                                          FormGroup formGroup) {
        formGroup.addExtension(createDatapointBrowser(attribute));
    }

    /* ####################################################################### */

    protected void readAllAttributeValues() {
        environment.getEventService().dispatch(
            new ReadAttributesEvent(
                asset.getId(),
                attributeGroups.keySet().stream().map(Attribute::getName).collect(Collectors.toList())
            )
        );
    }

    protected void readAttributeValue(AssetAttribute attribute) {
        environment.getEventService().dispatch(
            new ReadAttributesEvent(attribute.getAssetId(), attribute.getName())
        );
    }

    protected FormGroup createLiveUpdatesGroup() {
        FormGroup formGroup = new FormGroup();

        FormLabel formLabel = new FormLabel("Show live updates");
        formGroup.addFormLabel(formLabel);

        FormField formField = new FormField();
        formGroup.addFormField(formField);

        FormCheckBox liveUpdatesCheckBox = new FormCheckBox();
        liveUpdatesCheckBox.addValueChangeHandler(event -> subscribeToLiveUpdates(event.getValue()));
        formField.add(liveUpdatesCheckBox);

        if (attributes.size() > 0) {
            FormGroupActions formGroupActions = new FormGroupActions();

            FormButton readAllButton = new FormButton();
            readAllButton.setText("Read and refresh all attributes");
            readAllButton.addClickHandler(event -> readAllAttributeValues());
            formGroupActions.add(readAllButton);

            readButtons.add(readAllButton);

            formGroup.addFormGroupActions(formGroupActions);
        }

        return formGroup;
    }

    protected void subscribeToLiveUpdates(boolean enable) {
        for (FormButton readButton : readButtons) {
            readButton.setEnabled(!enable);
        }

        if (enable) {
            environment.getEventService().subscribe(
                AttributeEvent.class,
                new AttributeEvent.EntityIdFilter(asset.getId())
            );
        } else {
            environment.getEventService().unsubscribe(
                AttributeEvent.class
            );
        }
    }

    protected void rebuildEditor(AssetAttribute attribute) {
        FormGroup formGroup = attributeGroups.get(attribute);
        if (formGroup == null)
            return;
        FormField formField = formGroup.getFormField();
        if (formField.getWidgetCount() > 0) {
            formField.getWidget(0).removeFromParent();
        }
        final AttributeEditor editor = createEditor(attribute, formGroup);
        if (editor == null) {
            formField.add(createUnsupportedEditor(attribute));
            return;
        }

        formField.add(editor);

        // "Blink" the editor so users know there is a new value
        editor.asWidget().addStyleName(environment.getWidgetStyle().HighlightBackground());
        Browser.getWindow().setTimeout(() -> {
            editor.asWidget().removeStyleName(environment.getWidgetStyle().HighlightBackground());
        }, 250);
    }

    protected Widget createDatapointBrowser(AssetAttribute attribute) {
        // TODO
        return new Image("/static/img/Example%20Chart%20Big.png");
    }

    /* ####################################################################### */

    abstract protected void writeAttributeValue(AssetAttribute attribute);

}
