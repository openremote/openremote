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
package org.openremote.agent3.protocol.trigger;

import org.openremote.agent3.protocol.AbstractProtocol;
import org.openremote.container.Container;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.openremote.agent3.protocol.trigger.TriggerConfiguration.*;
import static org.openremote.model.Constants.ASSET_META_NAMESPACE;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;

/**
 * This protocol can be used to trigger {@link AttributeEvent} using a trigger type:
 * <p>
 * A trigger is defined by creating a {@link TriggerConfiguration} attribute on an
 * agent. A trigger consists of:
 * <ul>
 * <li>{@link TriggerType}</li>
 * <li>Trigger Value: {@link TriggerType#TIME} = CRON EXPRESSION or {@link TriggerType#ATTRIBUTE} = {@link AttributeRef}</li>
 * <li>Trigger Action {@link AttributeState} that should be sent when triggered</li>
 * </ul>
 * {@link AssetAttribute}s can link to time triggers to read/write the trigger time and/or enable/disable
 * the trigger.
 * <p>
 * The {@link org.openremote.model.asset.AssetMeta#PROTOCOL_PROPERTY} meta should be used on linked attributes to
 * indicate what property of the trigger the attribute should read/write. The content of the meta should be a string
 * that is specific to the trigger type handler. The only globally recognised value is "ENABLED"
 * which reads/writes the trigger's {@link org.openremote.model.asset.AssetMeta#ENABLED} meta item and is handled by
 * the {@link AbstractProtocol} super class.
 */
public class TriggerProtocol extends AbstractProtocol {

    private static final Logger LOG = Logger.getLogger(TriggerProtocol.class.getName());

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":trigger";
    public static final String META_TRIGGER_VALUE = ASSET_META_NAMESPACE + ":triggerValue";
    public static final String META_TRIGGER_TYPE = ASSET_META_NAMESPACE + ":triggerType";
    public static final String META_TRIGGER_ACTION = ASSET_META_NAMESPACE + ":triggerAction";
    protected final Map<AttributeRef, AssetAttribute> protocolConfigMap = new HashMap<>();
    protected final Map<AttributeRef, AbstractTriggerHandler> attributeTriggerHandlerMap = new HashMap<>();

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    public void start(Container container) throws Exception {
        super.start(container);

        for (TriggerType triggerType : TriggerType.values()) {
            AbstractTriggerHandler handler = triggerType.getHandler();
            LOG.info("Starting trigger handler: " + handler.getName());
            handler.start(container);
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        for (TriggerType triggerType : TriggerType.values()) {
            AbstractTriggerHandler handler = triggerType.getHandler();
            LOG.info("Stopping trigger handler: " + handler.getName());
            handler.stop(container);
        }

        super.stop(container);
    }

    @Override
    public void init(Container container) throws Exception {
        super.init(container);

        // Push static protocol reference so trigger handlers can call doTriggerAction
        AbstractTriggerHandler.protocol = this;

        for (TriggerType triggerType : TriggerType.values()) {
            AbstractTriggerHandler handler = triggerType.getHandler();
            LOG.info("Initialising trigger handler: " + handler.getName());
            handler.init(container);
        }
    }

    @Override
    protected void doLinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        // The protocol configuration is actually a Trigger Configuration
        AssetAttribute initialisedProtocolConfiguration;

        synchronized (protocolConfigMap) {
            initialisedProtocolConfiguration = protocolConfigMap.computeIfAbsent(protocolConfiguration.getReferenceOrThrow(), protocolConfigRef ->
            {
                // Verify that this is a valid Trigger Configuration
                if (!isValidTriggerConfiguration(protocolConfiguration)) {
                    LOG.warning("Trigger Configuration is not valid so it will be ignored: " + protocolConfigRef);
                    updateDeploymentStatus(protocolConfigRef, DeploymentStatus.ERROR);
                    return null;
                }

                // Register the trigger with its handler
                TriggerType type = getTriggerType(protocolConfiguration).get();
                AbstractTriggerHandler handler = getTriggerTypeHandler(protocolConfiguration).get();
                Value value = getTriggerValue(protocolConfiguration).get();
                handler.registerTrigger(protocolConfigRef, value, protocolConfiguration.isEnabled());
                updateDeploymentStatus(protocolConfigRef, protocolConfiguration.isEnabled() ? DeploymentStatus.LINKED_ENABLED : DeploymentStatus.LINKED_DISABLED);

                return protocolConfiguration;
            });
        }
    }

    @Override
    protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        synchronized (protocolConfigMap) {
            protocolConfigMap.remove(protocolConfiguration.getReferenceOrThrow());
        }
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, AssetAttribute protocolConfiguration) {
        LOG.fine("Request to send to actuator");

        // Send to actuator means that the trigger wants to be updated
        // if the attributes' META_TRIGGER_PROPERTY value is TRIGGER_PROPERTY_ENABLED
        // then handle the request here, otherwise delegate to the trigger handler
        AssetAttribute attribute = getLinkedAttribute(event.getAttributeRef());

        TriggerConfiguration.getTriggerProperty(attribute)
            .ifPresent(propertyName -> {
                synchronized (attributeTriggerHandlerMap) {
                    AbstractTriggerHandler handler = attributeTriggerHandlerMap.get(event.getAttributeRef());
                    if (handler != null) {
                        LOG.info("Passing attribute write request to trigger handler '" + handler.getName() + "'");
                        handler.processAttributeWrite(attribute, protocolConfiguration, propertyName, event);
                    }
                }
            });
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        // Wire up the attribute
        TriggerConfiguration.getTriggerTypeHandler(protocolConfiguration)
            .ifPresent(handler -> registerAttributeWithTriggerHandler(attribute, protocolConfiguration, handler));
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AbstractTriggerHandler oldHandler;
        AttributeRef attributeRef = attribute.getReferenceOrThrow();

        synchronized (attributeTriggerHandlerMap) {
            oldHandler = attributeTriggerHandlerMap.get(attributeRef);

            if (oldHandler != null) {
                unRegisterAttributeWithTriggerHandler(attributeRef, protocolConfiguration.getReferenceOrThrow(), oldHandler);
            }
        }
    }

    protected void registerAttributeWithTriggerHandler(AssetAttribute attribute, AssetAttribute triggerConfiguration, AbstractTriggerHandler handler) {
        LOG.fine("Request to register attribute with trigger handler");

        TriggerConfiguration.getTriggerProperty(attribute)
            .ifPresent(propertyName -> {
                AttributeRef attributeRef = attribute.getReferenceOrThrow();
                LOG.info("Registering attribute '" + attributeRef + "' on trigger handler '" + handler.getName() + "'");
                synchronized (attributeTriggerHandlerMap) {
                    attributeTriggerHandlerMap.put(attribute.getReferenceOrThrow(), handler);
                }
                handler.registerAttribute(attributeRef, triggerConfiguration.getReferenceOrThrow(), propertyName);
            });
    }

    protected void unRegisterAttributeWithTriggerHandler(AttributeRef attributeRef, AttributeRef triggerRef, AbstractTriggerHandler handler) {
        LOG.fine("Request to unregister attribute with trigger handler");

        // Check if attribute is linked to a trigger handler
        synchronized (attributeTriggerHandlerMap) {
            if (attributeTriggerHandlerMap.remove(attributeRef) != null) {
                LOG.info("Un-registering attribute '" + attributeRef + "' on trigger handler '" + handler.getName() + "'");
                handler.unregisterAttribute(attributeRef, triggerRef);
            }
        }
    }

    /**
     * Updates the value of an attribute linked to a trigger handler
     */
    void updateAttribute(AttributeState state) {
        updateLinkedAttribute(state);
    }

    /**
     * Sends the trigger's attribute state into the processing chain
     */
    void doTriggerAction(AttributeRef triggerRef) {
        LOG.info("Trigger handler requested trigger action to be executed");

        synchronized (protocolConfigMap) {
            Optional.ofNullable(protocolConfigMap.get(triggerRef))
                .ifPresent(triggerConfiguration ->
                    getTriggerAction(triggerConfiguration)
                        .ifPresent(this::sendAttributeEvent)
                );
        }
    }

    /**
     * Update the trigger's {@link #META_TRIGGER_VALUE}
     */
    void updateTriggerValue(AttributeState state) {
        LOG.fine("Updating the trigger value: " + state);

        updateLinkedProtocolConfiguration(
            getLinkedProtocolConfiguration(state.getAttributeRef()),
            META_TRIGGER_VALUE,
            new MetaItem(META_TRIGGER_VALUE, state.getCurrentValue().orElse(null))
        );
    }
}
