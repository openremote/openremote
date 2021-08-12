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
import org.openremote.model.attribute.AttributeWriteFailure;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.*;
import org.openremote.model.attribute.AttributeEvent.Source;
import org.openremote.model.protocol.ProtocolUtil;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.util.Pair;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaItemType;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Logger;

import static org.openremote.model.attribute.AttributeEvent.Source.ATTRIBUTE_LINKING_SERVICE;

/**
 * This service processes asset updates on attributes that have one or more {@link MetaItemType#ATTRIBUTE_LINKS} meta items.
 * <p>
 * If such an event occurs then the event is 'forwarded' to the linked attribute; an attribute can contain multiple
 * {@link MetaItemType#ATTRIBUTE_LINKS} meta items; optionally the value forwarded to the linked attribute can be modified
 * by configuring the converter property in the meta item's value:
 * <p>
 * By default the exact value of the attribute is forwarded unless a key exists in the converter JSON Object that
 * matches the value as a string (note matches are case sensitive so booleans should be lower case i.e. true or false);
 * in that case the value of the key is forwarded instead. There are several special conversions available by using
 * the value of a {@link AttributeLink.ConverterType} as the value. This allows for example a button press to toggle a boolean
 * attribute or for a particular value to be ignored.
 * <p>
 * To convert null values the converter key of "NULL" can be used.
 * <p>
 * Example {@link MetaItemType#ATTRIBUTE_LINKS} meta items:
 * <blockquote><pre>{@code
 * [
 * "name": "urn:openremote:asset:meta:attributeLink",
 * "value": {
 * "ref": ["0oI7Gf_kTh6WyRJFUTr8Lg", "light1"],
 * "converter": {
 * "PRESSED": "@TOGGLE",
 * "LONG_PRESSED": "@IGNORE",
 * "RELEASED": "@IGNORE"
 * }
 * }
 * ],
 * [
 * "name": "urn:openremote:asset:meta:attributeLink",
 * "value": {
 * "ref": ["0oI7Gf_kTh6WyRJFUTr8Lg", "light2"],
 * "converter": {
 * "0": true,
 * "1": false
 * "NULL": "@IGNORE"
 * }
 * }
 * ]
 * }</pre></blockquote>
 */
// TODO: Improve AttributeLinkingService so that outbound events are synchronsied with inbound
public class AttributeLinkingService implements ContainerService, AssetUpdateProcessor {

    private static final Logger LOG = Logger.getLogger(AttributeLinkingService.class.getName());
    protected AssetProcessingService assetProcessingService;
    protected AssetStorageService assetStorageService;
    protected AgentService agentService;

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        assetProcessingService = container.getService(AssetProcessingService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        agentService = container.getService(AgentService.class);
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    @Override
    public boolean processAssetUpdate(EntityManager em,
                                      Asset<?> asset,
                                      Attribute<?> attribute,
                                      Source source) throws AssetProcessingException {
        if (source == ATTRIBUTE_LINKING_SERVICE) {
            LOG.finest("Attribute update came from this service so ignoring to avoid infinite loops: " + attribute);
            return false;
        }

        attribute.getMetaValue(MetaItemType.ATTRIBUTE_LINKS)
            .ifPresent(attributeLinks ->
                Arrays.stream(attributeLinks).forEach(attributeLink ->
                    processLinkedAttributeUpdate(em, attributeLink, new AttributeState(asset.getId(), attribute))));

        return false;
    }

    protected void sendAttributeEvent(AttributeEvent attributeEvent) {
        LOG.finer("Sending attribute event for linked attribute: " + attributeEvent);
        assetProcessingService.sendAttributeEvent(attributeEvent, ATTRIBUTE_LINKING_SERVICE);
    }

    protected void processLinkedAttributeUpdate(EntityManager em, AttributeLink attributeLink, AttributeState attributeState) {
        if (attributeState == null)
            return;
        LOG.finer("Processing attribute state for linked attribute");

        if (attributeLink == null) {
            throw new AssetProcessingException(AttributeWriteFailure.INVALID_ATTRIBUTE_LINK);
        }

        // Convert the value as required
        Pair<Boolean, Object> sendConvertedValue = convertValueForLinkedAttribute(
            em,
            assetStorageService,
            attributeState.getValue().orElse(null),
            attributeLink
        );

        if (sendConvertedValue.key) {
            LOG.finer("Value converter matched ignore value");
            return;
        }

        final Object[] value = {sendConvertedValue.value};

        // Get the attribute and try and coerce the value into the correct type
        getAttribute(em, assetStorageService, attributeLink.getAttributeRef()).ifPresent(attribute -> {

            if (value[0] != null) {

                // Do basic value conversion
                if (!attribute.getType().getType().isAssignableFrom(value[0].getClass())) {
                    Object val = ValueUtil.convert(value[0], attribute.getType().getType());

                    if (val == null) {
                        LOG.warning("Failed to convert value: " + value[0].getClass() + " -> " + attribute.getType().getType());
                        LOG.warning("Cannot send linked attribute update");
                        return;
                    }
                    value[0] = val;
                }
            }

            sendAttributeEvent(new AttributeEvent(attributeLink.getAttributeRef(), value[0]));

        });
    }

    protected Pair<Boolean, Object> convertValueForLinkedAttribute(EntityManager em,
                                                                         AssetStorageService assetStorageService,
                                                                         Object originalValue,
                                                                         AttributeLink attributeLink) throws AssetProcessingException {

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
                            em,
                            assetStorageService,
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

    protected static Pair<Boolean, Object> doSpecialConversion(EntityManager em,
                                                              AssetStorageService assetStorageService,
                                                              AttributeLink.ConverterType converter,
                                                              AttributeRef linkedAttributeRef) throws AssetProcessingException {
        switch (converter) {
            case TOGGLE:
                // Look up current value of the linked attribute within the same database session
                try {
                    Attribute<?> currentAttribute = getAttribute(em, assetStorageService, linkedAttributeRef).orElseThrow(
                        () -> new AssetProcessingException(
                            AttributeWriteFailure.ATTRIBUTE_NOT_FOUND,
                            "cannot toggle value as attribute cannot be found: " + linkedAttributeRef
                        )
                    );
                    if (!ValueUtil.isBoolean(currentAttribute.getType().getType())) {
                        throw new AssetProcessingException(
                            AttributeWriteFailure.LINKED_ATTRIBUTE_CONVERSION_FAILURE,
                            "cannot toggle value as attribute is not of type BOOLEAN: " + linkedAttributeRef
                        );
                    }
                    return new Pair<>(false, !(Boolean)currentAttribute.getValueAs(Boolean.class).orElse(false));
                } catch (NoSuchElementException e) {
                    LOG.fine("The attribute doesn't exist so ignoring toggle value request: " + linkedAttributeRef);
                    return new Pair<>(true, null);
                }
            case INCREMENT:
            case DECREMENT:
                // Look up current value of the linked attribute within the same database session
                try {
                    Attribute<?> currentAttribute = getAttribute(em, assetStorageService, linkedAttributeRef).orElseThrow(
                        () -> new AssetProcessingException(
                            AttributeWriteFailure.ATTRIBUTE_NOT_FOUND,
                            "cannot toggle value as attribute cannot be found: " + linkedAttributeRef
                        )
                    );
                    if (!ValueUtil.isNumber(currentAttribute.getType().getType())) {
                        throw new AssetProcessingException(
                            AttributeWriteFailure.LINKED_ATTRIBUTE_CONVERSION_FAILURE,
                            "cannot increment/decrement value as attribute is not of type NUMBER: " + linkedAttributeRef
                        );
                    }
                    int change = converter == AttributeLink.ConverterType.INCREMENT ? +1 : -1;
                    return new Pair<>(false, (ValueUtil.getValueCoerced(currentAttribute.getValue().orElse(null), Double.class).orElse(0D) + change));
                } catch (NoSuchElementException e) {
                    LOG.fine("The attribute doesn't exist so ignoring increment/decrement value request: " + linkedAttributeRef);
                    return new Pair<>(true, null);
                }
            default:
                throw new AssetProcessingException(
                    AttributeWriteFailure.LINKED_ATTRIBUTE_CONVERSION_FAILURE,
                    "converter is not supported: " + converter
                );
        }
    }

    protected static Optional<Attribute<?>> getAttribute(EntityManager em,
                                                 AssetStorageService assetStorageService,
                                                 AttributeRef attributeRef) {
        // Get the full asset as shared em
        Asset<?> asset = assetStorageService.find(
            em,
            new AssetQuery()
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
