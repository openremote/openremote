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
package org.openremote.manager.server.asset;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.AssetQuery;
import org.openremote.model.asset.AssetState;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.util.Pair;
import org.openremote.model.value.*;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.openremote.model.attribute.MetaItem.isMetaNameEqualTo;

/**
 * This service listens for attribute events on attributes that have one or more {@link AssetMeta#ATTRIBUTE_LINK} meta
 * items.
 * <p>
 * If such an event occurs then the event is 'forwarded' to the linked attribute; an attribute can contain multiple
 * {@link AssetMeta#ATTRIBUTE_LINK} meta items; optionally the value forwarded to the linked attribute can be modified
 * by configuring the converter property in the meta item's value:
 * <p>
 * By default the exact value of the attribute is forwarded unless a key exists in the converter JSON Object that
 * matches the value as a string (note matches are case sensitive so booleans should be lower case i.e. true or false);
 * in that case the value of the key is forwarded instead. There are several special conversions available by using
 * the value of a {@link Converter} as the value. This allows for example a button press to toggle a boolean
 * attribute or for a particular value to be ignored.
 * <p>
 * To convert null values the converter key of "NULL" can be used.
 * <p>
 * <b>
 * NOTE: State converters (e.g. {@link Converter#TOGGLE}, {@link Converter#INCREMENT}, {@link Converter#DECREMENT}) are
 * not synchronised so if the initiating attribute (the one with the {@link AssetMeta#ATTRIBUTE_LINK} meta items)
 * generates multiple attribute events in a short period of time then the value pushed to the linked attribute may not
 * be correct.
 * </b>
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
public class AssetAttributeLinkingService implements ContainerService, Consumer<AssetState> {

    public enum Converter {

        /**
         * Ignore this value (do not send to the linked attribute)
         */
        IGNORE("@IGNORE"),

        /**
         * Send a null vale to the linked attribute
         */
        NULL("@NULL"),

        /**
         * Toggle the value of the linked attribute; the linked attribute's type must be
         * {@link org.openremote.model.attribute.AttributeType#BOOLEAN}
         */
        TOGGLE("@TOGGLE"),

        /**
         * Increment the value of the linked attribute; the linked attribute's type must be
         * {@link org.openremote.model.attribute.AttributeType#NUMBER}
         */
        INCREMENT("@INCREMENT"),

        /**
         * Decrement the value of the linked attribute; the linked attribute's type must be
         * {@link org.openremote.model.attribute.AttributeType#NUMBER}
         */
        DECREMENT("@DECREMENT");

        private String value;
        // Prevents cloning of values each time fromString is called
        private static final Converter[] copyOfValues = values();

        Converter(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Optional<Converter> fromString(String value) {
            for (Converter converter : copyOfValues) {
                if (converter.getValue().equalsIgnoreCase(value))
                    return Optional.of(converter);
            }
            return Optional.empty();
        }
    }

    private static final Logger LOG = Logger.getLogger(AssetAttributeLinkingService.class.getName());
    protected AssetProcessingService assetProcessingService;
    protected AssetStorageService assetStorageService;

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
    public void accept(AssetState assetState) {
        assetState
            .getAttribute()
            .getMetaStream()
            .filter(isMetaNameEqualTo(AssetMeta.ATTRIBUTE_LINK))
            .forEach(metaItem -> processLinkedAttributeUpdate(metaItem, assetState));
    }

    protected void sendAttributeEvent(AttributeEvent attributeEvent) {
        LOG.fine("Sending attribute event for linked attribute: " + attributeEvent);
        assetProcessingService.sendAttributeEvent(attributeEvent);
    }

    protected void processLinkedAttributeUpdate(MetaItem attributeLink, AssetState assetState) {
        LOG.fine("Processing attribute event for linked attribute");
        Optional<Pair<AttributeRef, Optional<ObjectValue>>> linkAndConverter = getAttributeLinkAndConverter(attributeLink);

        if (!linkAndConverter.isPresent()) {
            LOG.info("Invalid attribute link on: " + assetState.getAttributeRef());
            return;
        }

        // Convert the value as required
        Pair<Boolean, Value> sendConvertedValue = convertValueForLinkedAttribute(
            assetState.getValue(),
            linkAndConverter.get().key,
            linkAndConverter.get().value,
            assetStorageService
        );

        if (!sendConvertedValue.key) {
            // Do not forward the event (either marked as ignored or there was a problem with the value conversion)
            return;
        }

        sendAttributeEvent(new AttributeEvent(linkAndConverter.get().key, sendConvertedValue.value));
    }

    protected static Optional<Pair<AttributeRef, Optional<ObjectValue>>> getAttributeLinkAndConverter(MetaItem metaItem) {
        return metaItem
            .getValueAsObject()
            .map(obj -> {
                Optional<AttributeRef> ref = AttributeRef.fromValue(obj.get("attributeRef").orElse(null));
                Optional<ObjectValue> converter = obj.getObject("converter");

                return ref
                    .map(attributeRef -> new Pair<>(attributeRef, converter))
                    .orElse(null);
            });
    }

    protected static Pair<Boolean, Value> convertValueForLinkedAttribute(Value originalValue, AttributeRef linkedAttributeRef, Optional<ObjectValue> converter, AssetStorageService assetStorageService) {
        return converter
            .map(
                conv -> {
                    String converterKey = originalValue == null ? "NULL": originalValue.toString();
                    Optional<Value> converterValue = conv.get(converterKey);
                    // Convert the value
                    return converterValue
                        .map(value ->
                            getSpecialConverter(value)
                                .map(c -> doSpecialConversion(c, linkedAttributeRef, assetStorageService))
                                .orElse(new Pair<>(true, value))) // use the converter value as the new value
                        .orElseGet(() -> new Pair<>(true, originalValue)); // use the original value
                })
            .orElse(new Pair<>(true, originalValue)); // use the original value
    }

    protected static Optional<Converter> getSpecialConverter(Value value) {
        if (value.getType() == ValueType.STRING) {
            return Converter.fromString(value.toString());
        }
        return Optional.empty();
    }

    protected static Pair<Boolean, Value> doSpecialConversion(Converter converter, AttributeRef linkedAttributeRef, AssetStorageService assetStorageService) {
        switch (converter) {
            case IGNORE:
                LOG.fine("Converter set to ignore value so not forwarding to linked attribute");
                return new Pair<>(false, null);
            case NULL:
                return new Pair<>(true, null);
            case TOGGLE:
                // Look up current value of the linked attribute
                try {
                    Value currentValue = getCurrentValue(linkedAttributeRef, assetStorageService);
                    if (currentValue == null || currentValue.getType() != ValueType.BOOLEAN) {
                        LOG.fine("Cannot toggle attribute value as attribute is not of type BOOLEAN: " + linkedAttributeRef);
                        return new Pair<>(false, null);
                    }
                    return new Pair<>(true, Values.create(!((BooleanValue)currentValue).getBoolean()));
                } catch (NoSuchElementException e) {
                    LOG.fine("The attribute doesn't exist so ignoring toggle value request: " + linkedAttributeRef);
                    return new Pair<>(false, null);
                }
            case INCREMENT:
            case DECREMENT:
                // Look up current value of the linked attribute
                try {
                    Value currentValue = getCurrentValue(linkedAttributeRef, assetStorageService);
                    if (currentValue == null || currentValue.getType() != ValueType.NUMBER) {
                        LOG.fine("Cannot increment/decrement attribute value as attribute is not of type NUMBER: " + linkedAttributeRef);
                        return new Pair<>(false, null);
                    }
                    int change = converter == Converter.INCREMENT ? +1 : -1;
                    return new Pair<>(true, Values.create(((NumberValue)currentValue).getNumber() + change));
                } catch (NoSuchElementException e) {
                    LOG.fine("The attribute doesn't exist so ignoring increment/decrement value request: " + linkedAttributeRef);
                    return new Pair<>(false, null);
                }
            default:
                throw new UnsupportedOperationException("Converter is not supported: " + converter);
        }
    }

    protected static Value getCurrentValue(AttributeRef attributeRef, AssetStorageService assetStorageService) throws NoSuchElementException {
        ServerAsset asset = assetStorageService.find(
            new AssetQuery()
                .id(attributeRef.getEntityId())
                .select(new AssetQuery.Select(attributeRef.getAttributeName()).loadComplete(true))
        );

        Optional<AssetAttribute> attribute = Optional.empty();
        if (asset == null || !(attribute = asset.getAttribute(attributeRef.getAttributeName())).isPresent()) {
            throw new NoSuchElementException("Attribute or asset could not be found: " + attributeRef);
        }

        return attribute.get().getValue().orElse(null);
    }
}
