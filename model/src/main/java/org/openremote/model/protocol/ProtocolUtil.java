/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.protocol;

import org.openremote.model.Constants;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeLink;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.attribute.AttributeInfo;
import org.openremote.model.query.filter.ValuePredicate;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.TsIgnore;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.ValueFilter;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static org.openremote.model.Constants.DYNAMIC_VALUE_PLACEHOLDER;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.model.util.ValueUtil.NULL_LITERAL;
import static org.openremote.model.util.ValueUtil.applyValueFilters;

@TsIgnore
public final class ProtocolUtil {

    protected static Logger LOG = SyslogCategory.getLogger(PROTOCOL, ProtocolUtil.class);
    
    protected ProtocolUtil() {
    }

    /**
     * Will perform standard value processing for outbound values (Linked Attribute -> Protocol); the
     * containsDynamicPlaceholder flag is required so that the entire write value string is not
     * searched on every single write request (for performance reasons), instead this should be recorded when the
     * attribute is first linked.
     */
    public static Pair<Boolean, Object> doOutboundValueProcessing(String assetId, AttributeInfo attribute, AgentLink<?> agentLink, Object value, boolean containsDynamicPlaceholder) {

        String writeValue = agentLink.getWriteValue().orElse(null);

        Pair<Boolean, Object> ignoreAndConvertedValue;

        // value conversion
        Object finalValue = value;
        ignoreAndConvertedValue = agentLink.getWriteValueConverter().map(converter -> {
            LOG.finest("Applying attribute write value converter to attribute: assetId=" + assetId + ", attribute=" + attribute.getName());
            return applyValueConverter(finalValue, converter);
        }).orElse(new Pair<>(false, finalValue));

        if (ignoreAndConvertedValue.key) {
            return ignoreAndConvertedValue;
        }

        value = ignoreAndConvertedValue.value;

        // dynamic value insertion

        boolean hasWriteValue = !TextUtil.isNullOrEmpty(writeValue);

        if (hasWriteValue) {
            if (containsDynamicPlaceholder) {
                String valueStr = value == null ? NULL_LITERAL : ValueUtil.convert(value, String.class);
                writeValue = writeValue.replaceAll(Constants.DYNAMIC_VALUE_PLACEHOLDER_REGEXP, valueStr);
            }

            value = writeValue;
        }

        return new Pair<>(false, value);
    }

    public static boolean hasDynamicWriteValue(AgentLink<?> agentLink) {
        return agentLink.getWriteValue().map(str -> str.contains(DYNAMIC_VALUE_PLACEHOLDER)).orElse(false);
    }

    /**
     * Will perform standard value processing for inbound values (Protocol -> Linked Attribute); returning the processed
     * value and a flag indicating whether the inbound value should be ignored (i.e. drop the inbound message).
     */
    public static Pair<Boolean, Object> doInboundValueProcessing(String assetId, Attribute<?> attribute, AgentLink<?> agentLink, Object value) {

        Pair<Boolean, Object> ignoreAndConvertedValue;
        final AtomicReference<Object> valRef = new AtomicReference<>(value);

        // value filtering
        agentLink.getValueFilters().ifPresent(valueFilters -> {
            LOG.finest("Applying attribute value filters to attribute: assetId=" + assetId + ", attribute=" + attribute.getName());
            Object o = ValueUtil.applyValueFilters(value, valueFilters);
            if (o == null) {
                LOG.info("Value filters generated a null value for attribute: assetId=" + assetId + ", attribute=" + attribute.getName());
            }
            valRef.set(o);
        });

        // value conversion
        ignoreAndConvertedValue = agentLink.getValueConverter().map(converter -> {
            LOG.finest("Applying attribute value converter to attribute: assetId=" + assetId + ", attribute=" + attribute.getName());
            return applyValueConverter(valRef.get(), converter);
        }).orElse(new Pair<>(false, valRef.get()));

        if (ignoreAndConvertedValue.key) {
            return ignoreAndConvertedValue;
        }

        valRef.set(ignoreAndConvertedValue.value);

        if (valRef.get() == null) {
            return new Pair<>(false, null);
        }

        // built in value conversion
        Class<?> toType = attribute.getTypeClass();
        Class<?> fromType = valRef.get().getClass();

        if (toType != fromType) {
            LOG.finest("Applying built in attribute value conversion: " + fromType + " -> " + toType);
            valRef.set(ValueUtil.getValueCoerced(valRef.get(), toType).orElse(null));

            if (valRef.get() == null) {
                LOG.warning("Failed to convert value: " + fromType + " -> " + toType);
                LOG.warning("Cannot send linked attribute update: assetId=" + assetId + ", attribute=" + attribute.getName());
                return new Pair<>(true, null);
            }
        }

        return new Pair<>(false, valRef.get());
    }

    @SuppressWarnings("unchecked")
    public static Pair<Boolean, Object> applyValueConverter(Object value, Map<String, Object> converter) {

        if (converter == null) {
            return new Pair<>(false, value);
        }

        String converterKey = ValueUtil.getValueCoerced(value, String.class).map(str -> str.toUpperCase(Locale.ROOT)).orElse(NULL_LITERAL);

        return Optional.ofNullable(converter.get(converterKey))
            .map(converterValue -> {
                if (converterValue instanceof String convertValueStr) {
                    if ("@IGNORE".equalsIgnoreCase(convertValueStr)) {
                        return new Pair<>(true, null);
                    }

                    if ("@NULL".equalsIgnoreCase(convertValueStr)) {
                        return new Pair<>(false, null);
                    }
                }

                return new Pair<>(false, converterValue);
            })
            .orElse((Pair<Boolean, Object>) Optional.ofNullable(converter.get("*"))
                .map(converterValue -> {
                    if (converterValue instanceof String converterValueStr) {
                        if (AttributeLink.ConverterType.NEGATE.getValue().equals(converterValueStr)) {
                            if (ValueUtil.isNumber(value.getClass())) {
                                return new Pair<>(false, (ValueUtil.getValueCoerced(value, Double.class).orElse(0D) * -1));
                            }
                            if (ValueUtil.isBoolean(value.getClass())) {
                                return new Pair<>(false, !(ValueUtil.getValueCoerced(value, Boolean.class).orElse(false)));
                            }
                        }
                    }
                    return new Pair<>(false, converterValue);
                })
                .orElse(new Pair<>(true, value)));
    }

    public static Consumer<String> createGenericAttributeMessageConsumer(String assetId, Attribute<?> attribute, AgentLink<?> agentLink, Supplier<Long> currentMillisSupplier, Consumer<AttributeState> stateConsumer) {

        ValueFilter[] matchFilters = agentLink.getMessageMatchFilters().orElse(null);
        ValuePredicate matchPredicate = agentLink.getMessageMatchPredicate().orElse(null);

        if (matchPredicate == null) {
            return null;
        }

        return message -> {
            if (!TextUtil.isNullOrEmpty(message)) {
                Object messageFiltered = applyValueFilters(message, matchFilters);
                if (messageFiltered != null) {
                    if (matchPredicate.asPredicate(currentMillisSupplier).test(messageFiltered)) {
                        LOG.finest("Inbound message meets attribute matching meta so writing state to state consumer for attribute: asssetId=" + assetId + ", attribute=" + attribute.getName());
                        stateConsumer.accept(new AttributeState(assetId, attribute.getName(), message));
                    }
                }
            }
        };
    }
}
