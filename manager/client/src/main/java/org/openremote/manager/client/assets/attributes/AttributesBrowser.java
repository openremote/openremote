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

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import elemental.client.Browser;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.datapoint.DatapointBrowser;
import org.openremote.manager.client.util.CollectionsUtil;
import org.openremote.manager.client.widget.*;
import org.openremote.model.AttributeEvent;
import org.openremote.model.AttributeExecuteStatus;
import org.openremote.model.AttributeType;
import org.openremote.model.ReadAttributesEvent;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.NumberDatapoint;
import org.openremote.model.event.bus.EventRegistration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

public abstract class AttributesBrowser
    extends AttributesView<AttributesBrowser.Container, AttributesBrowser.Style> {

    public interface Container extends AttributesView.Container<AttributesBrowser.Style> {

    }

    public interface Style extends AttributesView.Style {

    }

    final protected Asset asset;
    final protected List<FormButton> readButtons = new ArrayList<>();

    protected FormGroup liveUpdatesGroup;
    protected EventRegistration<AttributeEvent> eventRegistration;

    public AttributesBrowser(Environment environment, Container container, Asset asset) {
        super(environment, container, asset.getAttributeList());
        this.asset = asset;

        eventRegistration = environment.getEventBus().register(
            AttributeEvent.class,
            event -> {
                for (AssetAttribute assetAttribute : attributeGroups.keySet()) {
                    if (assetAttribute.getReference().equals(event.getAttributeRef())) {
                        assetAttribute.setValueUnchecked(event.getValue(), event.getTimestamp());
                        refreshAttribute(assetAttribute);
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
    protected void sortAttributes(LinkedHashMap<AssetAttribute, FormGroup> attributeGroups) {
        // Executable commands first, then sort form groups by label text ascending
        CollectionsUtil.sortMap(attributeGroups, (o1, o2) -> {
                if (o1.getKey().isExecutable() && !o2.getKey().isExecutable()) {
                    return -1;
                } else if (!o1.getKey().isExecutable() && o2.getKey().isExecutable()) {
                    return 1;
                } else {
                    return o1.getValue().getFormLabel().getText().compareTo(o2.getValue().getFormLabel().getText());
                }
            }
        );
    }

    @Override
    protected boolean isEditorReadOnly(AssetAttribute attribute) {
        return attribute.isExecutable() || super.isEditorReadOnly(attribute);
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

        // Allow read/write of attribute if we understand attribute type (have an editor)
        if (editor != null) {

            if (attribute.isExecutable()) {
                // A command is executed by writing a special value
                FormButton startButton = new FormButton();
                startButton.setEnabled(!attribute.isReadOnly());
                startButton.setText(container.getMessages().start());
                startButton.setPrimary(true);
                startButton.setIcon("play-circle");
                startButton.addClickHandler(clickEvent -> {
                    attribute.setValue(AttributeExecuteStatus.REQUEST_START.asJsonValue());
                    writeAttributeValue(attribute);
                });
                formGroupActions.add(startButton);

                FormButton repeatButton = new FormButton();
                repeatButton.setEnabled(!attribute.isReadOnly());
                repeatButton.setText(container.getMessages().repeat());
                repeatButton.setPrimary(true);
                repeatButton.setIcon("repeat");
                repeatButton.addClickHandler(clickEvent -> {
                    attribute.setValue(AttributeExecuteStatus.REQUEST_REPEATING.asJsonValue());
                    writeAttributeValue(attribute);
                });
                formGroupActions.add(repeatButton);

                FormButton cancelButton = new FormButton();
                cancelButton.setEnabled(!attribute.isReadOnly());
                cancelButton.setText(container.getMessages().cancel());
                cancelButton.setPrimary(true);
                cancelButton.setIcon("stop-circle");
                cancelButton.addClickHandler(clickEvent -> {
                    attribute.setValue(AttributeExecuteStatus.REQUEST_CANCEL.asJsonValue());
                    writeAttributeValue(attribute);
                });
                formGroupActions.add(cancelButton);

                FormButton readStatusButton = new FormButton();
                readStatusButton.setText(container.getMessages().getStatus());
                readStatusButton.setIcon("cloud-download");
                readStatusButton.addClickHandler(clickEvent -> readAttributeValue(attribute));
                readButtons.add(readStatusButton);
                formGroupActions.add(readStatusButton);

            } else {
                // Default read/write actions
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
    }

    @Override
    protected void addAttributeExtensions(AssetAttribute attribute,
                                          FormGroup formGroup) {
        Widget datapointBrowser = createDatapointBrowser(attribute);
        if (datapointBrowser != null) {
            FormSectionLabel sectionLabel = new FormSectionLabel(environment.getMessages().historicalData());
            formGroup.addExtension(sectionLabel);
            formGroup.addExtension(datapointBrowser);
        } else {
            formGroup.showDisabledExtensionToggle();
        }
    }

    /* ####################################################################### */

    protected FormGroup createLiveUpdatesGroup() {
        FormGroup formGroup = new FormGroup();

        FormLabel formLabel = new FormLabel(environment.getMessages().showLiveUpdates());
        formGroup.addFormLabel(formLabel);

        FormField formField = new FormField();
        formGroup.addFormField(formField);

        FormCheckBox liveUpdatesCheckBox = new FormCheckBox();
        liveUpdatesCheckBox.addValueChangeHandler(event -> subscribeToLiveUpdates(event.getValue()));
        formField.add(liveUpdatesCheckBox);

        if (attributes.size() > 0) {
            FormGroupActions formGroupActions = new FormGroupActions();

            FormButton readAllButton = new FormButton();
            readAllButton.setText(environment.getMessages().refreshAllAttributes());
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

            // Poll all values once so we have some state
            readAllAttributeValues();

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

    protected void readAllAttributeValues() {
        environment.getEventService().dispatch(
            new ReadAttributesEvent(asset.getId())
        );
    }

    protected void readAttributeValue(AssetAttribute attribute) {
        attribute.getAssetId().ifPresent(assetId ->
            environment.getEventService().dispatch(
                new ReadAttributesEvent(assetId, attribute.getName())
            )
        );
    }

    protected void writeAttributeValue(AssetAttribute attribute) {
        environment.getEventService().dispatch(
            new AttributeEvent(attribute.getState())
        );
        if (attribute.isExecutable()) {
            showSuccess(container.getMessages().commandRequestSent(attribute.getName()));
        } else {
            showSuccess(container.getMessages().attributeWriteSent(attribute.getName()));
        }
    }

    protected void refreshAttribute(AssetAttribute attribute) {

        // Rebuild editor
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

        // "Blink" the editor so users know there might be a new value
        editor.asWidget().addStyleName(environment.getWidgetStyle().HighlightBackground());
        Browser.getWindow().setTimeout(() -> editor
            .asWidget()
            .removeStyleName(environment.getWidgetStyle().HighlightBackground()), 250);

        // Refresh charts, jump to current time so the new value is visible
        FormGroup attributeFormGroup = attributeGroups.get(attribute);
        if (attributeFormGroup != null) {
            attributeFormGroup.forExtension(widget -> {
                if (widget instanceof DatapointBrowser) {
                    DatapointBrowser datapointBrowser = (DatapointBrowser) widget;
                    datapointBrowser.refresh(System.currentTimeMillis());
                }
            });
        }
    }

    protected DatapointBrowser createDatapointBrowser(AssetAttribute attribute) {
        if (!attribute.isStoreDatapoints())
            return null;
        if (!attribute.getType().isPresent()
            || !(attribute.getType().get() == AttributeType.DECIMAL || attribute.getType().get() == AttributeType.INTEGER))
            return null;
        return new DatapointBrowser(environment.getMessages(), 580, 220) {
            @Override
            protected void queryDatapoints(DatapointInterval interval,
                                           long timestamp,
                                           Consumer<NumberDatapoint[]> consumer) {
                getNumberDatapoints(attribute, interval, timestamp, consumer);
            }
        };
    }

    /* ####################################################################### */

    abstract protected void getNumberDatapoints(AssetAttribute assetAttribute,
                                                DatapointInterval interval,
                                                long timestamp,
                                                Consumer<NumberDatapoint[]> consumer);
}
