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
package org.openremote.app.client.assets.asset;

import com.google.gwt.http.client.URL;
import org.openremote.app.client.Environment;
import org.openremote.app.client.assets.AgentStatusEventMapper;
import org.openremote.app.client.assets.AssetMapper;
import org.openremote.app.client.assets.attributes.AbstractAttributeViewExtension;
import org.openremote.app.client.assets.attributes.AttributeView;
import org.openremote.app.client.assets.attributes.AttributeViewImpl;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.datapoint.DatapointBrowser;
import org.openremote.app.client.datapoint.NumberDatapointArrayMapper;
import org.openremote.app.client.interop.value.ObjectValueMapper;
import org.openremote.app.client.simulator.Simulator;
import org.openremote.app.client.widget.FormButton;
import org.openremote.model.Constants;
import org.openremote.model.asset.*;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.AgentResource;
import org.openremote.model.asset.agent.AgentStatusEvent;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeExecuteStatus;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.AssetDatapointResource;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.interop.Consumer;
import org.openremote.model.map.MapResource;
import org.openremote.model.simulator.SimulatorState;
import org.openremote.model.value.Values;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class AssetViewActivity
    extends AbstractAssetActivity<AssetView.Presenter, AssetView, AssetViewPlace>
    implements AssetView.Presenter {

    protected final static String READ_BUTTON_CLASS = "or-internal-read-button";
    final AssetResource assetResource;
    final AssetMapper assetMapper;
    final AgentResource agentResource;
    final AgentStatusEventMapper agentStatusEventMapper;
    final AssetDatapointResource assetDatapointResource;
    final NumberDatapointArrayMapper numberDatapointArrayMapper;
    final protected List<AttributeRef> activeSimulators = new ArrayList<>();
    protected static boolean liveUpdates;

    @Inject
    public AssetViewActivity(Environment environment,
                             AssetBrowser.Presenter assetBrowserPresenter,
                             AssetView view,
                             AssetResource assetResource,
                             AssetMapper assetMapper,
                             AgentResource agentResource,
                             AgentStatusEventMapper agentStatusEventMapper,
                             AssetDatapointResource assetDatapointResource,
                             NumberDatapointArrayMapper numberDatapointArrayMapper,
                             MapResource mapResource,
                             ObjectValueMapper objectValueMapper) {
        super(environment, assetBrowserPresenter, objectValueMapper, mapResource, false);
        this.agentStatusEventMapper = agentStatusEventMapper;
        this.presenter = this;
        this.view = view;
        this.assetResource = assetResource;
        this.assetMapper = assetMapper;
        this.agentResource = agentResource;
        this.assetDatapointResource = assetDatapointResource;
        this.numberDatapointArrayMapper = numberDatapointArrayMapper;
    }

    @Override
    public void onStop() {
        subscribeLiveUpdates(false);
        if (isAgentOrHasAgentLinks()) {
            subscribeAgentStatus(false);
        }
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

        // If this is an agent or an asset with attributes linked to an agent, start polling all agents status
        if (isAgentOrHasAgentLinks()) {
            subscribeAgentStatus(true);
        }

        registrations.add(environment.getEventBus().register(
            AttributeEvent.class,
            this::onAttributeEvent
        ));

        registrations.add(environment.getEventBus().register(
            AssetEvent.class,
            this::onAssetEvent
        ));

        writeAssetToView();
        writeAttributesToView();
        loadParent();

        registrations.add(environment.getEventBus().register(
            AgentStatusEvent.class,
            this::onAgentStatusEvent
        ));

        // Fetch initial agent status
        if (asset.getWellKnownType() == AssetType.AGENT) {
            fetchAgentStatus(assetId);
        } else {
            asset.getAttributesStream()
                .map(AgentLink::getAgentLink)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(AttributeRef::getEntityId)
                .distinct()
                .forEach(this::fetchAgentStatus);
        }
    }

    protected boolean isAgentOrHasAgentLinks() {
        return asset != null && (
            asset.getWellKnownType() == AssetType.AGENT
                || asset.getAttributesStream().anyMatch(attribute -> AgentLink.getAgentLink(attribute).isPresent())
        );
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
                ((AttributeViewImpl) attributeView).getActionButtons().forEach(button -> {
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

        subscribeLiveUpdates(enable);
    }

    protected void subscribeLiveUpdates(boolean subscribe) {
        if (subscribe) {
            environment.getEventService().subscribe(
                AttributeEvent.class,
                new AssetFilter<AttributeEvent>().setAssetIds(asset.getId())
            );
        } else {
            environment.getEventService().unsubscribe(
                AttributeEvent.class
            );
        }
    }

    protected void subscribeAgentStatus(boolean subscribe) {
        if (subscribe) {
            environment.getEventService().subscribe(AgentStatusEvent.class, new TenantFilter<>(asset.getRealm()));
        } else {
            environment.getEventService().unsubscribe(AgentStatusEvent.class);
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
                attributeView.onAttributeChanged(attributeEvent.getTimestamp());
                break;
            }
        }
    }

    protected void onAssetEvent(AssetEvent assetEvent) {
        for (AttributeView attributeView : attributeViews) {
            attributeView.getAttribute().getReference().map(AttributeRef::getAttributeName)
                .flatMap(attributeName -> assetEvent.getAsset().getAttribute(attributeName))
                .ifPresent(assetAttribute ->
                    attributeView.getAttribute().setValue(
                        assetAttribute.getValue().orElse(null),
                        assetAttribute.getValueTimestamp().orElse(0L)));
        }
    }

    protected void onAgentStatusEvent(AgentStatusEvent event) {
        for (AttributeView attributeView : attributeViews) {
            AssetAttribute assetAttribute = attributeView.getAttribute();
            Optional<AttributeRef> assetAttributeRef = assetAttribute.getReference();

            if (asset.getWellKnownType() == AssetType.AGENT) {
                if (assetAttributeRef.map(ref -> ref.equals(event.getProtocolConfiguration())).orElse(false)) {
                    attributeView.setStatus(event.getConnectionStatus());
                }
            } else {
                AgentLink.getAgentLink(assetAttribute)
                    .filter(agentLink -> agentLink.equals(event.getProtocolConfiguration()))
                    .ifPresent(agentLink -> attributeView.setStatus(event.getConnectionStatus()));
            }

        }
    }

    protected void fetchAgentStatus(String agentId) {
        environment.getApp().getRequests().sendAndReturn(
            agentStatusEventMapper,
            requestParams -> agentResource.getAgentStatus(requestParams, agentId),
            200,
            agentStatuses -> {
                for (AgentStatusEvent event : agentStatuses) {
                    onAgentStatusEvent(event);
                }
            }
        );
    }

    @Override
    public void writeAssetToView() {
        super.writeAssetToView();
        AssetType assetType = asset.getWellKnownType();

        view.setIconAndType(assetType != null ? assetType.getIcon() : "cube", asset.getType());

        // Build the link manually, shorter result than AssetQueryMapper, and we must hardcode the path anyway
        String query = Values.createObject()
            .put("select", Values.createObject().put("include", Values.create("ALL")))
            .put("id", Values.create(asset.getId()))
            .toJson();
        view.setAccessPublicReadAnchor(
            "/" + asset.getRealm() + "/asset/public/query?q=" + URL.encodeQueryString(query)
        );
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

        if (attribute.isStoreDatapoints()) {
            viewExtensions.add(
                createDatapointBrowser(attribute, view)
            );
        }

        if (environment.getApp().getSecurity().isSuperUser() &&
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
            new ReadAssetEvent(asset.getId())
        );
    }

    protected void readAttributeValue(AssetAttribute attribute) {
        attribute.getReference().ifPresent(attributeRef ->
            environment.getEventService().dispatch(
                new ReadAssetAttributeEvent(attributeRef)
            )
        );
    }

    /*###########################################################################################*/
    /*####                             EXTENSIONS BELOW                                      ####*/
    /*###########################################################################################*/

    protected DatapointBrowser createDatapointBrowser(AssetAttribute attribute, AttributeViewImpl view) {
        return new DatapointBrowser(environment,
            this.view.getStyle(),
            view,
            attribute,
            675,
            200,
            DatapointInterval.HOUR,
            attribute.getValueTimestamp().orElse(System.currentTimeMillis()),
            attribute.getValueTimestamp().orElse(System.currentTimeMillis()) + 1000 * 60 * 60 * 24//add a day
        ) {
            @Override
            protected void queryDatapoints(DatapointInterval interval,
                                           long fromTimestamp,
                                           long toTimestamp,
                                           Consumer<ValueDatapoint[]> consumer) {
                attribute.getName().ifPresent(attributeName ->
                    queryDataPoints(attributeName, interval, fromTimestamp, toTimestamp, consumer)
                );
            }
        };
    }

    protected void queryDataPoints(String attributeName, DatapointInterval interval, long fromTimestamp, long toTimestamp, Consumer<ValueDatapoint[]> consumer) {
        if (!isNullOrEmpty(attributeName)) {
            environment.getApp().getRequests().sendAndReturn(
                numberDatapointArrayMapper,
                requestParams -> assetDatapointResource.getDatapoints(
                    requestParams, this.asset.getId(), attributeName, interval, fromTimestamp, toTimestamp
                ),
                200,
                consumer
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
