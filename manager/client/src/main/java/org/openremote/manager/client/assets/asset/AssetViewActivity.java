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
package org.openremote.manager.client.assets.asset;

import com.google.inject.Provider;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.app.dialog.JsonEditor;
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.assets.attributes.AbstractAttributeViewExtension;
import org.openremote.manager.client.assets.attributes.AttributeView;
import org.openremote.manager.client.assets.attributes.AttributeViewImpl;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.datapoint.DatapointBrowser;
import org.openremote.manager.client.datapoint.NumberDatapointArrayMapper;
import org.openremote.manager.client.interop.value.ObjectValueMapper;
import org.openremote.manager.client.simulator.Simulator;
import org.openremote.manager.client.widget.FormButton;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.datapoint.AssetDatapointResource;
import org.openremote.manager.shared.map.MapResource;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.Constants;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.ReadAssetAttributesEvent;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeExecuteStatus;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.Datapoint;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.NumberDatapoint;
import org.openremote.model.simulator.SimulatorState;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class AssetViewActivity
    extends AbstractAssetActivity<AssetView.Presenter, AssetView, AssetViewPlace>
    implements AssetView.Presenter {

    protected final static String READ_BUTTON_CLASS = "or-internal-read-button";
    final AssetResource assetResource;
    final AssetMapper assetMapper;
    final AssetDatapointResource assetDatapointResource;
    final NumberDatapointArrayMapper numberDatapointArrayMapper;
    final protected List<AttributeRef> activeSimulators = new ArrayList<>();
    protected static boolean liveUpdates;

    @Inject
    public AssetViewActivity(Environment environment,
                             Tenant currentTenant,
                             AssetBrowser.Presenter assetBrowserPresenter,
                             Provider<JsonEditor> jsonEditorProvider,
                             AssetView view,
                             AssetResource assetResource,
                             AssetMapper assetMapper,
                             AssetDatapointResource assetDatapointResource,
                             NumberDatapointArrayMapper numberDatapointArrayMapper,
                             MapResource mapResource,
                             ObjectValueMapper objectValueMapper) {
        super(environment, currentTenant, assetBrowserPresenter, jsonEditorProvider, objectValueMapper, mapResource, false);
        this.presenter = this;
        this.view = view;
        this.assetResource = assetResource;
        this.assetMapper = assetMapper;
        this.assetDatapointResource = assetDatapointResource;
        this.numberDatapointArrayMapper = numberDatapointArrayMapper;
    }

    @Override
    public void onStop() {
        subscribeLiveUpdates(false);
        super.onStop();
    }

    @Override
    public void start() {
        if (asset == null) {
            // Something went wrong loading the asset
            return;
        }

        if (liveUpdates) {
            subscribeLiveUpdates(true);
        }

        registrations.add(environment.getEventBus().register(
            AttributeEvent.class,
            this::onAttributeEvent
        ));


        writeAssetToView();
        writeAttributesToView();
        loadParent();
    }

    @Override
    public void centerMap() {
        view.flyTo(asset.getCoordinates());
    }

    @Override
    public void enableLiveUpdates(boolean enable) {
        liveUpdates = enable;

        for (AttributeView attributeView : attributeViews) {
            if (attributeView instanceof AttributeViewImpl) {
                ((AttributeViewImpl)attributeView).getActionButtons().forEach(button -> {
                    if (button.getStyleName().contains(READ_BUTTON_CLASS)) {
                        (button).setEnabled(!enable);
                    }
                });
            }
        }

        if (enable) {
            // Poll all values once so we have some state
            readAllAttributeValues();
        }

        subscribeLiveUpdates(true);
    }

    protected void subscribeLiveUpdates(boolean subscribe) {
        if (subscribe) {
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

    @Override
    public void refresh() {
        for (AttributeView attributeView : attributeViews) {
            attributeView.onAttributeChanged(System.currentTimeMillis());
        }
    }

    @Override
    protected void onAttributeModified(AssetAttribute attribute) {
        // Called when a view has modified the attribute so we need to do validation
        validateAttribute(true, attribute, result -> processValidationResults(Collections.singletonList(result)));
    }

    protected void onAttributeEvent(AttributeEvent attributeEvent) {
        for (AttributeView attributeView : attributeViews) {
            AssetAttribute assetAttribute = attributeView.getAttribute();
            Optional<AttributeRef> assetAttributeRef = assetAttribute.getReference();

            if (assetAttributeRef.map(ref -> ref.equals(attributeEvent.getAttributeRef())).orElse(false)) {
                assetAttribute.setValue(attributeEvent.getValue().orElse(null), attributeEvent.getTimestamp());
                attributeView.onAttributeChanged(System.currentTimeMillis());
                break;
            }
        }
    }

    @Override
    public void writeAssetToView() {
        super.writeAssetToView();
        view.setIconAndType(asset.getWellKnownType().getIcon(), asset.getType());
    }

    protected List<FormButton> createAttributeActions(AssetAttribute attribute, AttributeViewImpl view) {
        List<FormButton> actionButtons = new ArrayList<>();

        if (attribute.isExecutable()) {
            // A command is executed by writing a special value
            FormButton startButton = new FormButton();
            startButton.setEnabled(!attribute.isReadOnly());
            startButton.setText(environment.getMessages().start());
            startButton.setPrimary(true);
            startButton.setIcon("play-circle");
            startButton.addClickHandler(clickEvent -> {
                attribute.setValue(AttributeExecuteStatus.REQUEST_START.asValue());
                writeAttributeValue(attribute);
            });
            actionButtons.add(startButton);

            FormButton repeatButton = new FormButton();
            repeatButton.setEnabled(!attribute.isReadOnly());
            repeatButton.setText(environment.getMessages().repeat());
            repeatButton.setPrimary(true);
            repeatButton.setIcon("repeat");
            repeatButton.addClickHandler(clickEvent -> {
                attribute.setValue(AttributeExecuteStatus.REQUEST_REPEATING.asValue());
                writeAttributeValue(attribute);
            });
            actionButtons.add(repeatButton);

            FormButton cancelButton = new FormButton();
            cancelButton.setEnabled(!attribute.isReadOnly());
            cancelButton.setText(environment.getMessages().cancel());
            cancelButton.setPrimary(true);
            cancelButton.setIcon("stop-circle");
            cancelButton.addClickHandler(clickEvent -> {
                attribute.setValue(AttributeExecuteStatus.REQUEST_CANCEL.asValue());
                writeAttributeValue(attribute);
            });
            actionButtons.add(cancelButton);

            FormButton readStatusButton = new FormButton();
            readStatusButton.setText(environment.getMessages().getStatus());
            readStatusButton.setIcon("cloud-download");
            readStatusButton.addStyleName(READ_BUTTON_CLASS);
            readStatusButton.setEnabled(!liveUpdates);
            readStatusButton.addClickHandler(clickEvent -> readAttributeValue(attribute));
            actionButtons.add(readStatusButton);

        } else {
            // Default read/write actions
            FormButton writeValueButton = new FormButton();
            writeValueButton.setEnabled(!attribute.isReadOnly());
            writeValueButton.setText(environment.getMessages().write());
            writeValueButton.setPrimary(true);
            writeValueButton.setIcon("cloud-upload");
            writeValueButton.addClickHandler(clickEvent -> writeAttributeValue(attribute));
            actionButtons.add(writeValueButton);

            FormButton readValueButton = new FormButton();
            readValueButton.addStyleName(READ_BUTTON_CLASS);
            readValueButton.setText(environment.getMessages().read());
            readValueButton.setIcon("cloud-download");
            readValueButton.setEnabled(!liveUpdates);
            readValueButton.addClickHandler(clickEvent -> readAttributeValue(attribute));
            actionButtons.add(readValueButton);
        }

        return actionButtons;
    }

    protected List<AbstractAttributeViewExtension> createAttributeExtensions(AssetAttribute attribute, AttributeViewImpl view) {
        List<AbstractAttributeViewExtension> viewExtensions = new ArrayList<>();

        if (Datapoint.isDatapointsCapable(attribute) && attribute.isStoreDatapoints()) {
            viewExtensions.add(
                createDatapointBrowser(attribute, view)
            );
        }

        if (environment.getSecurityService().isSuperUser() &&
            ProtocolConfiguration.isProtocolConfiguration(attribute) &&
            ProtocolConfiguration.getProtocolName(attribute)
                .map(name -> name.equals(Constants.PROTOCOL_NAMESPACE + ":simulator"))
                .orElse(false)) {
            viewExtensions.add(
                createSimulator(attribute, view)
            );
        }

        return viewExtensions;
    }

    protected void readAllAttributeValues() {
        environment.getEventService().dispatch(
            new ReadAssetAttributesEvent(asset.getId())
        );
    }

    protected void readAttributeValue(AssetAttribute attribute) {
        attribute.getReference().ifPresent(attributeRef ->
            environment.getEventService().dispatch(
                new ReadAssetAttributesEvent(attributeRef.getEntityId(), attributeRef.getAttributeName())
            )
        );
    }

    /*###########################################################################################*/
    /*####                             EXTENSIONS BELOW                                      ####*/
    /*###########################################################################################*/

    protected DatapointBrowser createDatapointBrowser(AssetAttribute attribute, AttributeViewImpl view) {
        return new DatapointBrowser(environment, this.view.getStyle(), view, attribute, 675, 200) {
            @SuppressWarnings("ConstantConditions")
            @Override
            protected void queryDatapoints(DatapointInterval interval,
                                           long timestamp,
                                           Consumer<NumberDatapoint[]> consumer) {
                queryDataPoints(attribute.getName().get(), interval, timestamp, consumer);
            }
        };
    }

    protected void queryDataPoints(String attributeName, DatapointInterval interval, long timestamp, Consumer<NumberDatapoint[]> consumer) {
        if (!isNullOrEmpty(attributeName)) {
            environment.getRequestService().execute(
                numberDatapointArrayMapper,
                requestParams -> assetDatapointResource.getNumberDatapoints(
                    requestParams, this.asset.getId(), attributeName, interval, timestamp
                ),
                200,
                consumer,
                ex -> handleRequestException(ex, environment)
            );
        }
    }

    protected Simulator createSimulator(AssetAttribute attribute, AttributeViewImpl view) {
        AttributeRef protocolConfigurationRef = attribute.getReferenceOrThrow();

        return new Simulator(
            environment,
            this.view.getStyle(),
            view,
            attribute,
            protocolConfigurationRef,
            () -> {
                activeSimulators.add(protocolConfigurationRef);
                updateSimulatorSubscription();
            },
            () -> {
                activeSimulators.remove(protocolConfigurationRef);
                updateSimulatorSubscription();
            }
        );
    }

    protected void updateSimulatorSubscription() {
        if (activeSimulators.size() > 0) {
            environment.getEventService().subscribe(
                SimulatorState.class, new SimulatorState.ConfigurationFilter(
                    activeSimulators.toArray(new AttributeRef[activeSimulators.size()])
                )
            );
        } else {
            environment.getEventService().unsubscribe(SimulatorState.class);
        }
    }


}
