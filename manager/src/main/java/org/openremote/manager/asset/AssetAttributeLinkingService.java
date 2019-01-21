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

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.manager.asset.AssetProcessingException.Reason;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.attribute.*;
import org.openremote.model.attribute.AttributeEvent.Source;
import org.openremote.model.util.Pair;
import org.openremote.model.value.*;

import javax.persistence.EntityManager;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Logger;

import static org.openremote.model.query.AssetQuery.Include;
import static org.openremote.model.query.AssetQuery.Select;
import static org.openremote.model.attribute.AttributeEvent.Source.ATTRIBUTE_LINKING_SERVICE;
import static org.openremote.model.attribute.MetaItem.isMetaNameEqualTo;

/**
 * This service processes asset updates on attributes that have one or more {@link AssetMeta#ATTRIBUTE_LINK} meta items.
 * <p>
 * If such an event occurs then the event is 'forwarded' to the linked attribute; an attribute can contain multiple
 * {@link AssetMeta#ATTRIBUTE_LINK} meta items; optionally the value forwarded to the linked attribute can be modified
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
 * Example {@link AssetMeta#ATTRIBUTE_LINK} meta items:
 * <blockquote><pre>{@code
 * [
 * "name": "urn:openremote:asset:meta:attributeLink",
 * "value": {
 * "attributeRef": ["0oI7Gf_kTh6WyRJFUTr8Lg", "light1"],
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
 * "attributeRef": ["0oI7Gf_kTh6WyRJFUTr8Lg", "light2"],
 * "converter": {
 * "0": true,
 * "1": false
 * "NULL": "@IGNORE"
 * }
 * }
 * ]
 * }</pre></blockquote>
 */
// TODO: Improve AssetAttributeLinkingService so that outbound events are synchronsied with inbound
public class AssetAttributeLinkingService implements ContainerService, AssetUpdateProcessor {

    private static final Logger LOG = Logger.getLogger(AssetAttributeLinkingService.class.getName());
    protected AssetProcessingService assetProcessingService;
    protected AssetStorageService assetStorageService;

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        assetProcessingService = container.getService(AssetProcessingService.class);
        assetStorageService = container.getService(AssetStorageService.class);
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    @Override
    public boolean processAssetUpdate(EntityManager em,
                                      Asset asset,
                                      AssetAttribute attribute,
                                      Source source) throws AssetProcessingException {
        if (source == ATTRIBUTE_LINKING_SERVICE) {
            LOG.fine("Attribute update came from this service so ignoring to avoid infinite loops: " + attribute);
            return false;
        }

        attribute.getMetaStream()
            .filter(isMetaNameEqualTo(AssetMeta.ATTRIBUTE_LINK))
            .forEach(metaItem -> processLinkedAttributeUpdate(em, metaItem, attribute.getState().orElse(null)));

        return false;
    }

    protected void sendAttributeEvent(AttributeEvent attributeEvent) {
        LOG.fine("Sending attribute event for linked attribute: " + attributeEvent);
        assetProcessingService.sendAttributeEvent(attributeEvent, ATTRIBUTE_LINKING_SERVICE);
    }

    protected void processLinkedAttributeUpdate(EntityManager em, MetaItem metaItem, AttributeState attributeState) {
        if (attributeState == null)
            return;
        LOG.fine("Processing attribute state for linked attribute");
        Optional<AttributeLink> attributeLink = metaItem.getValue().flatMap(AttributeLink::fromValue);

        if (!attributeLink.isPresent()) {
            throw new AssetProcessingException(Reason.INVALID_ATTRIBUTE_LINK);
        }

        // Convert the value as required
        Pair<Boolean, Value> sendConvertedValue = convertValueForLinkedAttribute(
            em,
            assetStorageService,
            attributeState.getValue().orElse(null),
            attributeLink.get()
        );

        if (!sendConvertedValue.key) {
            // Do not forward the attribute state (conversion resulted in NULL or IGNORE)
            return;
        }

        sendAttributeEvent(new AttributeEvent(attributeLink.get().getAttributeRef(), sendConvertedValue.value));
    }

    protected static Pair<Boolean, Value> convertValueForLinkedAttribute(EntityManager em,
                                                                         AssetStorageService assetStorageService,
                                                                         Value originalValue,
                                                                         AttributeLink attributeLink) throws AssetProcessingException {
        return attributeLink.getConverter()
            .map(
                converter -> {
                    String converterKey = originalValue == null ? "NULL" : originalValue.toString().toUpperCase(Locale.ROOT);
                    Optional<Value> converterValue = converter.get(converterKey);
                    // Convert the value
                    return converterValue
                        .map(value ->
                            getSpecialConverter(value)
                                .map(c -> doSpecialConversion(em, assetStorageService, c, attributeLink.getAttributeRef()))
                                .orElse(new Pair<>(true, value))) // use the converter value as the new value
                        .orElseGet(() -> new Pair<>(true, originalValue)); // use the original value
                })
            .orElse(new Pair<>(true, originalValue)); // use the original value
    }

    protected static Optional<AttributeLink.ConverterType> getSpecialConverter(Value value) {
        if (value.getType() == ValueType.STRING) {
            return AttributeLink.ConverterType.fromValue(value.toString());
        }
        return Optional.empty();
    }

    protected static Pair<Boolean, Value> doSpecialConversion(EntityManager em,
                                                              AssetStorageService assetStorageService,
                                                              AttributeLink.ConverterType converter,
                                                              AttributeRef linkedAttributeRef) throws AssetProcessingException {
        switch (converter) {
            case IGNORE:
                LOG.fine("Converter set to ignore value so not forwarding to linked attribute");
                return new Pair<>(false, null);
            case NULL:
                return new Pair<>(true, null);
            case TOGGLE:
                // Look up current value of the linked attribute within the same database session
                try {
                    Value currentValue = getCurrentValue(em, assetStorageService, linkedAttributeRef);
                    if (currentValue == null || currentValue.getType() != ValueType.BOOLEAN) {
                        throw new AssetProcessingException(
                            Reason.LINKED_ATTRIBUTE_CONVERSION_FAILURE,
                            "cannot toggle value as attribute is not of type BOOLEAN: " + linkedAttributeRef
                        );
                    }
                    return new Pair<>(true, Values.create(!((BooleanValue) currentValue).getBoolean()));
                } catch (NoSuchElementException e) {
                    LOG.fine("The attribute doesn't exist so ignoring toggle value request: " + linkedAttributeRef);
                    return new Pair<>(false, null);
                }
            case INCREMENT:
            case DECREMENT:
                // Look up current value of the linked attribute within the same database session
                try {
                    Value currentValue = getCurrentValue(em, assetStorageService, linkedAttributeRef);
                    if (currentValue == null || currentValue.getType() != ValueType.NUMBER) {
                        throw new AssetProcessingException(
                            Reason.LINKED_ATTRIBUTE_CONVERSION_FAILURE,
                            "cannot increment/decrement value as attribute is not of type NUMBER: " + linkedAttributeRef
                        );
                    }
                    int change = converter == AttributeLink.ConverterType.INCREMENT ? +1 : -1;
                    return new Pair<>(true, Values.create(((NumberValue) currentValue).getNumber() + change));
                } catch (NoSuchElementException e) {
                    LOG.fine("The attribute doesn't exist so ignoring increment/decrement value request: " + linkedAttributeRef);
                    return new Pair<>(false, null);
                }
            default:
                throw new AssetProcessingException(
                    Reason.LINKED_ATTRIBUTE_CONVERSION_FAILURE,
                    "converter is not supported: " + converter
                );
        }
    }

    protected static Value getCurrentValue(EntityManager em,
                                           AssetStorageService assetStorageService,
                                           AttributeRef attributeRef) throws NoSuchElementException {
        Asset asset = assetStorageService.find(
            em,
            new AssetQuery()
                .id(attributeRef.getEntityId())
                .select(new Select(Include.ALL, false, attributeRef.getAttributeName()))
        );

        Optional<AssetAttribute> attribute;
        if (asset == null || !(attribute = asset.getAttribute(attributeRef.getAttributeName())).isPresent()) {
            throw new NoSuchElementException("Attribute or asset could not be found: " + attributeRef);
        }

        return attribute.get().getValue().orElse(null);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}
