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
package org.openremote.agent.protocol;

import org.apache.commons.codec.binary.BinaryCodec;
import org.apache.commons.codec.binary.Hex;
import org.openremote.container.web.OAuthGrant;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.asset.agent.ProtocolDescriptor;
import org.openremote.model.attribute.*;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.attribute.MetaItemDescriptor.Access.ACCESS_PRIVATE;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.*;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.model.util.TextUtil.REGEXP_PATTERN_STRING_NON_EMPTY;
import static org.openremote.model.value.Values.NULL_LITERAL;

/**
 * A protocol is a thread-safe singleton {@link ContainerService} that connects devices and
 * services to the context broker. Each protocol implementation must have a unique
 * resource name (RfC 2141 URN) in a VM.
 * <h3>Configuring protocol instances</h3>
 * A protocol implementation must support multiple protocol configurations and therefore
 * support multiple logical instances. A protocol 'instance' can be defined by an
 * attribute on an {@link Asset} that has a type of {@link AssetType#AGENT}; the
 * attribute must conform to {@link ProtocolConfiguration}.
 * <p>
 * When a protocol configuration is loaded/created for a protocol then the
 * {@link #linkProtocolConfiguration} method will be called.
 * The protocol should check the {@link AssetAttribute#isEnabled} status of the protocol
 * configuration to determine whether or not the logical instance should be running or stopped.
 * <p>
 * The protocol is responsible for calling the provided consumer whenever the status of the
 * logical instance changes (e.g. if the configuration is not valid then the protocol should
 * call the consumer with a value of {@link ConnectionStatus#ERROR} and it should provide
 * sensible logging to allow fault finding).
 * <h3>Connecting attributes to actuators and sensors</h3>
 * {@link AssetAttribute}s of {@link Asset}s can be linked to a protocol configuration
 * instance by creating an {@link MetaItemType#AGENT_LINK} {@link MetaItem} on an attribute.
 * Besides the {@link MetaItemType#AGENT_LINK}, other protocol-specific meta items may also be
 * required when an asset attribute is linked to a protocol configuration instance.
 * Attributes linked to a protocol configuration instance will get passed to the protocol via
 * a call to {@link #linkAttributes}.
 * <p>
 * The protocol handles read and write of linked attributes:
 * <p>
 * If the actual state of the device (or service) changes, the linked protocol writes the new
 * state into the attribute value and notifies the context broker of the change. A protocol updates
 * a linked attributes' value by sending  an {@link AttributeEvent} messages on the
 * {@link #SENSOR_QUEUE}, including the source protocol name in header {@link #SENSOR_QUEUE_SOURCE_PROTOCOL}.
 * <p>
 * If the user writes a new value into the linked attribute, the protocol translates this value
 * change into a device (or service) action. Write operations on attributes linked to a protocol
 * configuration can be consumed by the protocol on the {@link #ACTUATOR_TOPIC} where the message
 * body will be an {@link AttributeEvent}. Each message also contains the target protocol name in
 * header {@link #ACTUATOR_TOPIC_TARGET_PROTOCOL}.
 * <p>
 * To simplify protocol development some common protocol behaviour is recommended:
 * <h1>Inbound value conversion (Protocol -> Linked Attribute)</h1>
 * <p>
 * Standard value filtering and/or conversion should be performed in the following order, this is encapsulated in
 * {@link #doInboundValueProcessing}:
 * <ol>
 * <li>Configurable value filtering which allows the value produced by the protocol to be filtered through any
 * number of {@link ValueFilter}s before being written to the linked attribute
 * (see {@link #META_ATTRIBUTE_VALUE_FILTERS})</li>
 * <li>Configurable value conversion which allows the value produced by the protocol to be converted in a configurable
 * way before being written to the linked attribute (see {@link #META_ATTRIBUTE_VALUE_CONVERTER})</li>
 * <li>Automatic basic value conversion should be performed when the {@link ValueType} of the value produced by the
 * protocol and any configured value conversion does not match the linked attributes underlying {@link ValueType}; this
 * basic conversion should use the {@link Values#convertToValue} method</li>
 * </ol>
 * <h1>Outbound value conversion (Linked Attribute -> Protocol)</h1>
 * Standard value conversion should be performed in the following order, this is encapsulated in
 * {@link #doOutboundValueProcessing}:
 * <ol>
 * <li>Configurable value conversion which allows the value sent from the linked attribute to be converted in a
 * configurable way before being sent to the protocol for processing (see {@link #META_ATTRIBUTE_WRITE_VALUE_CONVERTER})
 * <li>Configurable dynamic value insertion (replacement of {@link #DYNAMIC_VALUE_PLACEHOLDER} strings within a
 * pre-defined JSON string with the value sent from the linked attribute (this allows for attribute values to be inserted
 * into a larger payload before processing by the protocol; it also allows the written value to be fixed or statically
 * converted.
 * </ol>
 * When sending the converted value onto the actual protocol implementation for processing the original
 * {@link AttributeEvent} as well as the converted value should be made available.
 * <p>
 * NOTE: That {@link #linkProtocolConfiguration} will always be called
 * before {@link #linkAttributes} and {@link #unlinkAttributes} will always be called before
 * {@link #unlinkProtocolConfiguration}.
 * <p>
 * The following summarises the method calls protocols should expect:
 * <p>
 * Protocol configuration (logical instance) is created/loaded:
 * <ol>
 * <li>{@link #linkProtocolConfiguration}</li>
 * <li>{@link #linkAttributes}</li>
 * </ol>
 * <p>
 * Protocol configuration (logical instance) is modified:
 * <ol>
 * <li>{@link #unlinkAttributes}</li>
 * <li>{@link #unlinkProtocolConfiguration}</li>
 * <li>{@link #linkProtocolConfiguration}</li>
 * <li>{@link #linkAttributes}</li>
 * </ol>
 * <p>
 * Protocol configuration (logical instance) is removed:
 * <ol>
 * <li>{@link #unlinkAttributes}</li>
 * <li>{@link #unlinkProtocolConfiguration}</li>
 * </ol>
 * <p>
 * Attribute linked to protocol configuration is created/loaded:
 * <ol>
 * <li>{@link #linkAttributes}</li>
 * </ol>
 * <p>
 * Attribute linked to protocol configuration is modified:
 * <ol>
 * <li>{@link #unlinkAttributes}</li>
 * <li>{@link #linkAttributes}</li>
 * </ol>
 * <p>
 * Attribute link to protocol configuration is removed:
 * <ol>
 * <li>{@link #unlinkAttributes}</li>
 * </ol>
 */
public interface Protocol extends ContainerService {

    Logger LOG = SyslogCategory.getLogger(PROTOCOL, Protocol.class);
    String ACTUATOR_TOPIC_TARGET_PROTOCOL = "Protocol";
    String SENSOR_QUEUE_SOURCE_PROTOCOL = "Protocol";

    /**
     * Can be used by protocols that support it to indicate that string values should be converted to/from bytes from/to
     * HEX string representation (e.g. 34FD87)
     */
    MetaItemDescriptor META_PROTOCOL_CONVERT_HEX = metaItemFixedBoolean(PROTOCOL_NAMESPACE + ":convertHex", ACCESS_PRIVATE, false);

    /**
     * Can be used by protocols that support it to indicate that string values should be converted to/from bytes from/to
     * binary string representation (e.g. 1001010111)
     */
    MetaItemDescriptor META_PROTOCOL_CONVERT_BINARY = metaItemFixedBoolean(PROTOCOL_NAMESPACE + ":convertBinary", ACCESS_PRIVATE, false);

    /**
     * Charset to use when converting byte[] to a string (should default to UTF8 if not specified); values must be string
     * that matches a charset as defined in {@link java.nio.charset.Charset}
     */
    MetaItemDescriptor META_PROTOCOL_CHARSET = metaItemString(
            PROTOCOL_NAMESPACE + ":charset",
            ACCESS_PRIVATE,
            false,
            Charset.availableCharsets().keySet().toArray(new String[0])
    );

    /**
     * Max length of messages received by a {@link Protocol}; what this actually means will be protocol specific
     * i.e. for {@link String} protocols it could be the number of characters but for {@link Byte} protocols it could be
     * the number of bytes. This is typically used for {@link org.openremote.agent.protocol.io.IoClient} based
     * {@link Protocol}s.
     */
    MetaItemDescriptor META_PROTOCOL_MAX_LENGTH = metaItemInteger(
        PROTOCOL_NAMESPACE + ":maxLength",
        ACCESS_PRIVATE,
        false,
        0,
        Integer.MAX_VALUE
    );

    /**
     * Defines a delimiter for messages received by a {@link Protocol}. Multiples of this {@link MetaItem} can be used
     * to add multiple delimiters (the first matched delimiter should be used to generate the shortest possible match(.
     * This is typically used for {@link org.openremote.agent.protocol.io.IoClient} based {@link Protocol}s.
     */
    MetaItemDescriptor META_PROTOCOL_DELIMITER = new MetaItemDescriptorImpl(
        PROTOCOL_NAMESPACE + ":delimiter",
        ValueType.STRING,
        ACCESS_PRIVATE,
        false,
        null,
        null,
        null,
        null,
        false,
        null,
        null,
        null,
        false
    );

    /**
     * For protocols that use {@link #META_PROTOCOL_DELIMITER}, this indicates whether or not the matched delimiter
     * should be stripped from the message.
     */
    MetaItemDescriptor META_PROTOCOL_STRIP_DELIMITER = metaItemFixedBoolean(
        PROTOCOL_NAMESPACE + ":stripDelimiter",
        ACCESS_PRIVATE,
        false
    );

    /**
     * OAuth grant ({@link OAuthGrant} stored as {@link ObjectValue})
     */
    MetaItemDescriptor META_PROTOCOL_OAUTH_GRANT = metaItemObject(
        PROTOCOL_NAMESPACE + ":oAuthGrant",
        ACCESS_PRIVATE,
        false,
        null);


    /**
     * Basic authentication username (string)
     */
    MetaItemDescriptor META_PROTOCOL_USERNAME = metaItemString(
        PROTOCOL_NAMESPACE + ":username",
        ACCESS_PRIVATE,
        false,
        REGEXP_PATTERN_STRING_NON_EMPTY,
        PatternFailure.STRING_EMPTY);

    /**
     * Basic authentication password (string)
     */
    MetaItemDescriptor META_PROTOCOL_PASSWORD = metaItemString(
        PROTOCOL_NAMESPACE + ":password",
        ACCESS_PRIVATE,
        false,
        true,
        REGEXP_PATTERN_STRING_NON_EMPTY,
        PatternFailure.STRING_EMPTY);


    /**
     * TCP/IP network host name/IP address
     */
    MetaItemDescriptor META_PROTOCOL_HOST =  metaItemString(
        PROTOCOL_NAMESPACE + ":host",
        ACCESS_PRIVATE,
        true,
        TextUtil.REGEXP_PATTERN_STRING_NON_EMPTY_NO_WHITESPACE,
        PatternFailure.STRING_EMPTY_OR_CONTAINS_WHITESPACE);

    /**
     * TCP/IP network port number
     */
    MetaItemDescriptor META_PROTOCOL_PORT = metaItemInteger(
        PROTOCOL_NAMESPACE + ":port",
        ACCESS_PRIVATE,
        true,
        1,
        65536);

    /**
     * Serial port name/address
     */
    MetaItemDescriptor META_PROTOCOL_SERIAL_PORT = metaItemString(
        PROTOCOL_NAMESPACE + ":serialPort",
        ACCESS_PRIVATE,
        true,
        REGEXP_PATTERN_STRING_NON_EMPTY,
        PatternFailure.STRING_EMPTY);

    MetaItemDescriptor META_PROTOCOL_SERIAL_BAUDRATE = metaItemInteger(
        PROTOCOL_NAMESPACE + ":baudrate",
        ACCESS_PRIVATE,
        true,
        1,
        Integer.MAX_VALUE);

    /**
     * Defines {@link ValueFilter}s to apply to an incoming value before it is written to a protocol linked attribute;
     * this is particularly useful for generic protocols. The {@link MetaItem} value should be an {@link ArrayValue} of
     * {@link ObjectValue}s where each {@link ObjectValue} represents a serialised {@link ValueFilter}. The message
     * should pass through the filters in array order.
     */
    MetaItemDescriptor META_ATTRIBUTE_VALUE_FILTERS = metaItemArray(
        PROTOCOL_NAMESPACE + ":valueFilters",
        ACCESS_PRIVATE,
        false,
        null);

    /**
     * Defines a value converter map to allow for basic value type conversion; the incoming value will be converted
     * to JSON and if this string matches a key in the converter then the value of that key will be pushed through to
     * the attribute. An example use case is an API that returns "ACTIVE"/"DISABLED" strings but you want to connect
     * this to a {@link AttributeValueType#BOOLEAN} attribute.
     */
    MetaItemDescriptor META_ATTRIBUTE_VALUE_CONVERTER = metaItemObject(
        PROTOCOL_NAMESPACE + ":valueConverter",
        ACCESS_PRIVATE,
        false,
        null);

    /**
     * Similar to {@link #META_ATTRIBUTE_VALUE_CONVERTER} but will applied to outgoing values allowing for the opposite
     * conversion.
     */
    MetaItemDescriptor META_ATTRIBUTE_WRITE_VALUE_CONVERTER = metaItemObject(
        PROTOCOL_NAMESPACE + ":writeValueConverter",
        ACCESS_PRIVATE,
        false,
        null);

    /**
     * JSON string to be used for attribute writes and can contain {@link #DYNAMIC_VALUE_PLACEHOLDER}s; this allows the
     * written value to be injected into a bigger JSON payload or to even hardcode the value sent to the protocol
     * (i.e. ignore the written value). If this {@link MetaItem} is not defined then the written value is passed through
     * to the protocol as is. The resulting JSON string (after any dynamic value insertion) is then parsed using
     * {@link Values#parse} so it is important the value of this {@link MetaItem} is a valid JSON string so literal
     * strings must be quoted (e.g. '"string value"' not 'string value' otherwise parsing will fail).
     * <p>
     * A value of 'null' will produce a literal null.
     */
    MetaItemDescriptor META_ATTRIBUTE_WRITE_VALUE = metaItemAny(
            PROTOCOL_NAMESPACE + ":writeValue",
            ACCESS_PRIVATE,
            false,
            null,
            TextUtil.REGEXP_PATTERN_STRING_NON_EMPTY,
            PatternFailure.STRING_EMPTY.name());

    /**
     * Polling frequency in milliseconds for {@link Attribute}s whose value should be polled; can be set on the
     * {@link ProtocolConfiguration} or the {@link Attribute} (the latter takes precedence).
     */
    MetaItemDescriptor META_ATTRIBUTE_POLLING_MILLIS = metaItemInteger(
            PROTOCOL_NAMESPACE + ":pollingMillis",
            ACCESS_PRIVATE,
            false,
            1000,
            null);

    /**
     * The predicate to use on incoming messages to determine if the message is intended for the {@link Attribute} that
     * has this {@link MetaItem}; it is particularly useful for pub-sub based {@link Protocol}s.
     */
    MetaItemDescriptor META_ATTRIBUTE_MATCH_PREDICATE = metaItemObject(
        PROTOCOL_NAMESPACE + ":matchPredicate",
        ACCESS_PRIVATE,
        false,
        new StringPredicate(AssetQuery.Match.EXACT, false, "").toModelValue());

    /**
     * {@link ValueFilter}s to apply to incoming messages prior to comparison with the
     * {@link Protocol#META_ATTRIBUTE_MATCH_PREDICATE}, if the predicate matches then the original un-filtered
     * message is intended for this linked {@link Attribute} and the message should be written to the {@link Attribute}
     * where the actual {@link Value} written can be filtered using the {@link Protocol#META_ATTRIBUTE_VALUE_FILTERS}.
     * <p>
     * The {@link Value} of this {@link MetaItem} must be an {@link ArrayValue} of {@link ObjectValue}s where each
     * {@link ObjectValue} represents a serialised {@link ValueFilter}. The message will pass through the filters in
     * array order and the resulting final {@link Value} should be written to the {@link Attribute}
     */
    MetaItemDescriptor META_ATTRIBUTE_MATCH_FILTERS = metaItemArray(
        PROTOCOL_NAMESPACE + ":matchFilters",
        ACCESS_PRIVATE,
        false,
        null);

    // TODO: Some of these options should be configurable depending on expected load etc.
    // Message topic for communicating from asset/thing to protocol layer (asset attribute changed, trigger actuator)
    String ACTUATOR_TOPIC = "seda://ActuatorTopic?multipleConsumers=true&concurrentConsumers=1&waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&limitConcurrentConsumers=false&size=1000";

    // Message queue for communicating from protocol to asset/thing layer (sensor changed, trigger asset attribute update)
    String SENSOR_QUEUE = "seda://SensorQueue?waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=false&size=25000";

    String DYNAMIC_VALUE_PLACEHOLDER = "{$value}";

    String DYNAMIC_VALUE_PLACEHOLDER_REGEXP = "\"?\\{\\$value}\"?";

    //Specify to use time. First # delimiter used to specify format. Second # delimiter used to add or subtract millis
    String DYNAMIC_TIME_PLACEHOLDER_REGEXP = "\"?\\{\\$time#?([a-z,A-Z,\\-,\\s,:]*)#?(-?\\d*)?}\"?";

    /**
     * Get the name for this protocol
     */
    String getProtocolName();

    /**
     * Get the display friendly name for this protocol
     */
    String getProtocolDisplayName();

    /**
     * Get the version number for this protocol
     */
    String getVersion();

    /**
     * Links attributes to their protocolConfiguration; the protocolConfiguration would have
     * been linked before this call. If an attribute is not valid for this protocol or the protocol
     * Configuration then it is up to the protocol whether to put the entire protocolConfiguration
     * into an {@link ConnectionStatus#ERROR} state or to continue running (appropriate logging should
     * always be used).
     * <p>
     * Attributes are linked to a protocol configuration via an {@link MetaItemType#AGENT_LINK} meta item.
     */
    void linkAttributes(Collection<AssetAttribute> attributes, AssetAttribute protocolConfiguration);

    /**
     * Un-links attributes from their protocolConfiguration; called whenever an attribute is modified or removed
     * or when the linked protocolConfiguration is modified or removed.
     */
    void unlinkAttributes(Collection<AssetAttribute> attributes, AssetAttribute protocolConfiguration) throws Exception;

    /**
     * Links the protocol configuration to the protocol; the protocol is responsible for calling the statusConsumer
     * to indicate the runtime state of the protocol configuration. If the {@link ConnectionStatus} is set to
     * {@link ConnectionStatus#ERROR} or {@link ConnectionStatus#ERROR_CONFIGURATION} or this method throws an exception
     * then attribute linking will be skipped for this {@link ProtocolConfiguration}.
     */
    void linkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration, Consumer<ConnectionStatus> statusConsumer)
        throws Exception;

    /**
     * Un-links the protocol configuration from the protocol; called whenever a protocolConfiguration is modified
     * or removed.
     */
    void unlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration);

    /**
     * Get a {@link ProtocolDescriptor} for this protocol.
     */
    ProtocolDescriptor getProtocolDescriptor();

    /**
     * Create an empty {@link ProtocolConfiguration} attribute that contains the required meta items needed
     * by the protocol. The purpose of this is to populate the UI when adding a new protocol configuration
     * for this protocol.
     */
    AssetAttribute getProtocolConfigurationTemplate();

    /**
     * Validate the supplied {@link ProtocolConfiguration} attribute against this protocol (should indicate that the
     * protocol configuration is well formed but not necessarily that it connects to a working system).
     */
    AttributeValidationResult validateProtocolConfiguration(AssetAttribute protocolConfiguration);

    /**
     * Extract the {@link ValueFilter}s from the specified {@link Attribute}
     */
    static Optional<ValueFilter[]> getLinkedAttributeValueFilters(Attribute attribute) {
        if (attribute == null) {
            return Optional.empty();
        }

        Optional<ArrayValue> arrayValueOptional = attribute.getMetaItem(Protocol.META_ATTRIBUTE_VALUE_FILTERS)
            .flatMap(AbstractValueHolder::getValueAsArray);

        if (!arrayValueOptional.isPresent()) {
            return Optional.empty();
        }

        try {
            String json = arrayValueOptional.get().toJson();
            return Optional.of(Container.JSON.readValue(json, ValueFilter[].class));
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
        }

        return Optional.empty();
    }

    static Optional<OAuthGrant> getOAuthGrant(AssetAttribute attribute) throws IllegalArgumentException {
        return !attribute.hasMetaItem(META_PROTOCOL_OAUTH_GRANT)
            ? Optional.empty()
            : Optional.of(attribute.getMetaItem(META_PROTOCOL_OAUTH_GRANT)
            .flatMap(AbstractValueHolder::getValueAsObject)
            .map(objValue -> {
                String json = objValue.toJson();
                try {
                    return Container.JSON.readValue(json, OAuthGrant.class);
                } catch (IOException e) {
                    throw new IllegalArgumentException("OAuth Grant meta item is not valid", e);
                }
            })
            .orElseThrow(() -> new IllegalArgumentException("OAuth grant meta item must be an ObjectValue")));
    }

    static String bytesToHexString(byte[] bytes) {
        return Hex.encodeHexString(bytes);
    }

    static byte[] bytesFromHexString(String hex) {
        try {
            return Hex.decodeHex(hex.toCharArray());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to convert hex string to bytes", e);
            return new byte[0];
        }
    }

    static String bytesToBinaryString(byte[] bytes) {
        return BinaryCodec.toAsciiString(bytes);
    }

    static byte[] bytesFromBinaryString(String binary) {
        try {
            return BinaryCodec.fromAscii(binary.toCharArray());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to convert hex string to bytes", e);
            return new byte[0];
        }
    }

    /**
     * Will perform recommended value processing for outbound values (Linked Attribute -> Protocol); the
     * containsDynamicPlaceholder flag is required so that the entire {@link #META_ATTRIBUTE_WRITE_VALUE} payload is
     * not searched on every single write request (for performance reasons), instead this should be recorded when the
     * attribute is first linked.
     */
    static Pair<Boolean, Value> doOutboundValueProcessing(AssetAttribute attribute, Value value, boolean containsDynamicPlaceholder) {

        String writeValue = Values.getMetaItemValueOrThrow(attribute, META_ATTRIBUTE_WRITE_VALUE, false, true)
            .map(Object::toString).orElse(null);

        if (attribute.isExecutable()) {
            AttributeExecuteStatus status = Values.getString(value)
                .flatMap(AttributeExecuteStatus::fromString)
                .orElse(null);

            if (status == AttributeExecuteStatus.REQUEST_START && writeValue != null) {
                try {
                    value = Values.parse(writeValue).orElse(null);
                } catch (Exception e) {
                    value = null;
                    LOG.log(Level.INFO, "Failed to pass attribute write payload generated by META_ATTRIBUTE_WRITE_VALUE", e);
                }
                return new Pair<>(false, value);
            }
        }

        // value conversion
        ObjectValue converter = Values.getMetaItemValueOrThrow(
            attribute,
            META_ATTRIBUTE_WRITE_VALUE_CONVERTER,
            false,
            false)
            .flatMap(Values::getObject)
            .orElse(null);

        if (converter != null) {
            LOG.fine("Applying attribute value converter to attribute write: " + attribute.getReferenceOrThrow());

            Pair<Boolean, Value> converterResult = applyValueConverter(value, converter);

            if (converterResult.key) {
                return converterResult;
            }

            value = converterResult.value;
        }

        // dynamic value insertion
        boolean hasWriteValue = attribute.hasMetaItem(META_ATTRIBUTE_WRITE_VALUE);

        if (hasWriteValue) {
            if (writeValue == null) {
                LOG.fine("META_ATTRIBUTE_WRITE_VALUE contains null so sending null to protocol for attribute write on: " + attribute.getReferenceOrThrow());
                return new Pair<>(false, null);
            }

            if (containsDynamicPlaceholder) {
                String valueStr = value == null ? NULL_LITERAL : value.toString();
                writeValue = writeValue.replaceAll(DYNAMIC_VALUE_PLACEHOLDER_REGEXP, valueStr);
            }

            try {
                value = Values.parse(writeValue).orElse(null);
            } catch (Exception e) {
                LOG.log(Level.INFO, "Failed to pass attribute write payload generated by META_ATTRIBUTE_WRITE_VALUE", e);
            }
        }

        return new Pair<>(false, value);
    }

    static Pair<Boolean, Value> doInboundValueProcessing(AssetAttribute attribute, Value value, ProtocolAssetService assetService) {

        // filtering
        ValueFilter[] filters = Protocol.getLinkedAttributeValueFilters(attribute).orElse(null);
        if (filters != null) {
            value = assetService.applyValueFilters(value, filters);
        }

        // value conversion
        ObjectValue converter = Values.getMetaItemValueOrThrow(
            attribute,
            META_ATTRIBUTE_VALUE_CONVERTER,
            false,
            false)
            .flatMap(Values::getObject)
            .orElse(null);

        if (converter != null) {
            LOG.fine("Applying attribute value converter to attribute: " + attribute.getReferenceOrThrow());

            Pair<Boolean, Value> convertedValue = applyValueConverter(value, converter);

            if (convertedValue.key) {
                return convertedValue;
            }

            value = convertedValue.value;
        }

        // built in value conversion
        Optional<ValueType> attributeValueType = attribute.getType().map(AttributeValueDescriptor::getValueType);

        if (value != null && attributeValueType.isPresent()) {
            if (attributeValueType.get() != value.getType()) {
                LOG.fine("Trying to convert value: " + value.getType() + " -> " + attributeValueType.get());
                Optional<Value> convertedValue = Values.convertToValue(value, attributeValueType.get());

                if (!convertedValue.isPresent()) {
                    LOG.warning("Failed to convert value: " + value.getType() + " -> " + attributeValueType.get());
                    LOG.warning("Cannot send linked attribute update");
                    return new Pair<>(true, null);
                }

                value = convertedValue.get();
            }
        }

        return new Pair<>(false, value);
    }

    static Pair<Boolean, Value> applyValueConverter(Value value, ObjectValue converter) {

        if (converter == null) {
            return new Pair<>(false, value);
        }

        String converterKey = value == null ? NULL_LITERAL.toUpperCase() : value.toString().toUpperCase(Locale.ROOT);
        return converter.get(converterKey)
            .map(v -> {
                if (v.getType() == ValueType.STRING) {
                    String valStr = v.toString();
                    if ("@IGNORE".equalsIgnoreCase(valStr)) {
                        return new Pair<>(true, (Value)null);
                    }

                    if ("@NULL".equalsIgnoreCase(valStr)) {
                        return new Pair<>(false, (Value)null);
                    }
                }

                return new Pair<>(false, v);
            })
            .orElse(new Pair<>(true, value));
    }

    static Consumer<String> createGenericAttributeMessageConsumer(AssetAttribute attribute, ProtocolAssetService assetService, Consumer<AttributeState> stateConsumer) {

        ValueFilter[] matchFilters = Values.getMetaItemValueOrThrow(
            attribute,
            META_ATTRIBUTE_MATCH_FILTERS,
            false,
            true)
            .map(Value::toJson)
            .map(json -> {
                try {
                    return Container.JSON.readValue(json, ValueFilter[].class);
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Failed to deserialize ValueFilter[]", e);
                    return null;
                }
            }).orElse(null);

        StringPredicate matchPredicate = Values.getMetaItemValueOrThrow(
            attribute,
            META_ATTRIBUTE_MATCH_PREDICATE,
            false,
            true)
            .map(Value::toJson)
            .map(s -> {
                try {
                    return Container.JSON.readValue(s, StringPredicate.class);
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Failed to deserialise StringPredicate", e);
                    return null;
                }
            })
            .orElse(null);

        if (matchPredicate == null) {
            return null;
        }

        AttributeRef attributeRef = attribute.getReferenceOrThrow();

        return message -> {
            if (!TextUtil.isNullOrEmpty(message)) {
                StringValue stringValue = Values.create(message);
                Value val = assetService.applyValueFilters(stringValue, matchFilters);
                if (val != null) {
                    if (StringPredicate.asPredicate(matchPredicate).test(message)) {
                        LOG.finest("Message matches attribute so writing state to state consumer for attribute: " + attributeRef);
                        stateConsumer.accept(new AttributeState(attributeRef, stringValue));
                    }
                }
            }
        };
    }
}
