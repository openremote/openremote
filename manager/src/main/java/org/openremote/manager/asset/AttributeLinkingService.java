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
package org.openremote.manager.asset;

import org.openremote.manager.agent.AgentService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeLink;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeInfo;
import org.openremote.model.protocol.ProtocolUtil;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.util.Pair;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaItemType;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * This service generates new {@link AttributeEvent}s for any {@link Attribute} that contains an
 * {@link MetaItemType#ATTRIBUTE_LINKS} meta item when the {@link Attribute} is updated.
 * <p>
 * See {@link AttributeLink} for capabilities.
 */
public class AttributeLinkingService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AttributeLinkingService.class.getName());
    protected AssetProcessingService assetProcessingService;
    protected AssetStorageService assetStorageService;
    protected AgentService agentService;
    protected GatewayService gatewayService;

    @Override
    public void init(Container container) throws Exception {
        assetProcessingService = container.getService(AssetProcessingService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        agentService = container.getService(AgentService.class);
        gatewayService = container.getService(GatewayService.class);
        ClientEventService clientEventService = container.getService(ClientEventService.class);
        clientEventService.addInternalSubscription(AttributeEvent.class, null, this::onAttributeEvent);
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    public void onAttributeEvent(AttributeEvent event) {
        if (getClass().getName().equals(event.getSource())) {
            LOG.finest("Attribute update came from this service so ignoring to avoid infinite loops: ref=" + event.getRef());
            return;
        }

        event.getMetaValue(MetaItemType.ATTRIBUTE_LINKS)
            .ifPresent(attributeLinks ->
                Arrays.stream(attributeLinks).forEach(attributeLink ->
                    processLinkedAttributeUpdate(event, attributeLink)));
    }

    protected void sendAttributeEvent(AttributeEvent attributeEvent) {
        LOG.finest("Sending attribute event for linked attribute: " + attributeEvent);
        assetProcessingService.sendAttributeEvent(attributeEvent, getClass().getName());
    }

    protected void processLinkedAttributeUpdate(AttributeInfo attributeInfo, AttributeLink attributeLink) {
        if (attributeLink == null) {
            return;
        }

        LOG.finest("Processing attribute links for updated attribute ref=" + attributeInfo.getRef());

        // Convert the value as required
        Pair<Boolean, Object> sendConvertedValue = convertValueForLinkedAttribute(
            assetStorageService,
            attributeInfo,
            attributeLink
        );

        if (sendConvertedValue.key) {
            LOG.finest("Value converter matched ignore value");
            return;
        }

        final Object[] value = {sendConvertedValue.value};

        // Get the attribute and try and coerce the value into the correct type
        getAttribute(assetStorageService, attributeInfo.getRealm(), attributeLink.getAttributeRef()).ifPresent(attr -> {

            if (value[0] != null) {

                // Do basic value conversion
                if (!attr.getTypeClass().isAssignableFrom(value[0].getClass())) {
                    Object val = ValueUtil.convert(value[0], attr.getTypeClass());

                    if (val == null) {
                        LOG.warning("Failed to convert value: " + value[0].getClass() + " -> " + attr.getTypeClass());
                        LOG.warning("Cannot send linked attribute update");
                        return;
                    }
                    value[0] = val;
                }
            }

            sendAttributeEvent(new AttributeEvent(attributeLink.getAttributeRef(), value[0]));
        });
    }

    protected Pair<Boolean, Object> convertValueForLinkedAttribute(AssetStorageService assetStorageService,
                                                                   AttributeInfo attributeInfo,
                                                                   AttributeLink attributeLink) throws AssetProcessingException {

        Object originalValue = attributeInfo.getValue().orElse(null);

        // Filter the value first
        if (attributeLink.getFilters() != null) {
            originalValue = ValueUtil.applyValueFilters(originalValue, attributeLink.getFilters());
        }
        Object finalOriginalValue = originalValue;

        // Apply converter
        return attributeLink.getConverter()
            .map(
                converter -> {
                    Pair<Boolean, Object> converterValue = ProtocolUtil.applyValueConverter(finalOriginalValue, converter);

                    if (converterValue.key) {
                        return converterValue;
                    }

                    // Do special value conversion
                    return getSpecialConverter(converterValue.value).map(
                        specialConverter -> doSpecialConversion(
                            assetStorageService,
                            attributeInfo,
                            specialConverter,
                            attributeLink.getAttributeRef()
                        )
                    ).orElse(converterValue);
                })
            .orElse(new Pair<>(false, originalValue));
    }

    protected static Optional<AttributeLink.ConverterType> getSpecialConverter(Object value) {
        if (value != null && ValueUtil.isString(value.getClass())) {
            return AttributeLink.ConverterType.fromValue(ValueUtil.getValue(value, String.class).orElse(""));
        }
        return Optional.empty();
    }

    protected static Pair<Boolean, Object> doSpecialConversion(AssetStorageService assetStorageService,
                                                              AttributeInfo attributeInfo,
                                                              AttributeLink.ConverterType converter,
                                                              AttributeRef linkedAttributeRef) throws RuntimeException {
        switch (converter) {
            case TOGGLE -> {
                // Look up current value of the linked attribute within the same database session
                try {
                    Attribute<?> currentAttribute = getAttribute(assetStorageService, attributeInfo.getRealm(), linkedAttributeRef).orElseThrow(
                        () -> new RuntimeException("Cannot toggle value as attribute cannot be found: " + linkedAttributeRef));
                    if (!ValueUtil.isBoolean(currentAttribute.getTypeClass())) {
                        throw new RuntimeException(
                            "Cannot toggle value as attribute is not of type BOOLEAN: " + linkedAttributeRef);
                    }
                    return new Pair<>(false, !(Boolean) currentAttribute.getValue(Boolean.class).orElse(false));
                } catch (NoSuchElementException e) {
                    LOG.fine("The attribute doesn't exist so ignoring toggle value request: " + linkedAttributeRef);
                    return new Pair<>(true, null);
                }
            }
            case INCREMENT, DECREMENT -> {
                // Look up current value of the linked attribute within the same database session
                try {
                    Attribute<?> currentAttribute = getAttribute(assetStorageService, attributeInfo.getRealm(), linkedAttributeRef)
                        .orElseThrow(() ->
                            new RuntimeException("Cannot toggle value as attribute cannot be found: " + linkedAttributeRef));
                    if (!ValueUtil.isNumber(currentAttribute.getTypeClass())) {
                        throw new RuntimeException(
                            "Cannot increment/decrement value as attribute is not of type NUMBER: " + linkedAttributeRef
                        );
                    }
                    int change = converter == AttributeLink.ConverterType.INCREMENT ? +1 : -1;
                    return new Pair<>(false, (ValueUtil.getValueCoerced(currentAttribute.getValue().orElse(null), Double.class).orElse(0D) + change));
                } catch (NoSuchElementException e) {
                    LOG.fine("The attribute doesn't exist so ignoring increment/decrement value request: " + linkedAttributeRef);
                    return new Pair<>(true, null);
                }
            }
            default -> throw new RuntimeException("Converter is not supported ref=" + linkedAttributeRef + ": " + converter);
        }
    }

    protected static Optional<Attribute<?>> getAttribute(AssetStorageService assetStorageService,
                                                         String realm,
                                                         AttributeRef attributeRef) {
        // Get the full asset as shared em
        Asset<?> asset = assetStorageService.find(
            new AssetQuery()
                .realm(new RealmPredicate(realm))
                .ids(attributeRef.getId())
        );

        Attribute<?> attribute = asset != null ? asset.getAttributes().get(attributeRef.getName()).orElse(null) : null;

        if (attribute == null) {
            LOG.warning("Attribute or asset could not be found: " + attributeRef);
        }

        return Optional.ofNullable(attribute);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}
