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
package org.openremote.model.attribute;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.UserAsset;
import org.openremote.model.rules.AssetState;
import org.openremote.model.rules.TemporaryFact;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.Optional;
import java.util.function.Function;

import static org.openremote.model.Constants.ASSET_META_NAMESPACE;
import static org.openremote.model.attribute.MetaItem.MetaItemFailureReason.META_ITEM_VALUE_MISMATCH;
import static org.openremote.model.attribute.MetaItemDescriptor.Access.ACCESS_PRIVATE;
import static org.openremote.model.util.TextUtil.REGEXP_PATTERN_DOUBLE;
import static org.openremote.model.util.TextUtil.REGEXP_PATTERN_INTEGER_POSITIVE_NON_ZERO;

/**
 * This describes generic {@link MetaItem}s that can be used on any {@link Attribute}.
 */
public enum MetaItemType implements MetaItemDescriptor {

    /**
     * Marks an attribute of an agent asset as a {@link org.openremote.model.asset.agent.ProtocolConfiguration}. The
     * attribute value is a protocol URN.
     */
    PROTOCOL_CONFIGURATION(
        ASSET_META_NAMESPACE + ":protocolConfiguration",
        ACCESS_PRIVATE,
        ValueType.BOOLEAN,
        null,
        null,
        Values.create(true),
        true),

    /**
     * Links the attribute to an agent's {@link org.openremote.model.asset.agent.ProtocolConfiguration}, connecting it
     * to a sensor and/or actuator.
     */
    AGENT_LINK(
        ASSET_META_NAMESPACE + ":agentLink",
        ACCESS_PRIVATE,
        ValueType.ARRAY,
        null,
        null,
        null,
        false,
        false,
        null,
        null,
        null,
        value ->
            Optional.ofNullable(AttributeRef.isAttributeRef(value)
                                    ? null
                                    : new ValidationFailure(META_ITEM_VALUE_MISMATCH,
                                                            AttributeRef.class.getSimpleName()))
    ),

    /**
     * Links the attribute to another attribute, so an attribute event on the attribute triggers the same attribute
     * event on the linked attribute.
     */
    ATTRIBUTE_LINK(
        ASSET_META_NAMESPACE + ":attributeLink",
        ACCESS_PRIVATE,
        ValueType.OBJECT,
        null,
        null,
        null,
        false,
        false,
        null,
        null,
        null,
        value ->
            Optional.ofNullable(AttributeLink.isAttributeLink(value)
                                    ? null
                                    : new ValidationFailure(META_ITEM_VALUE_MISMATCH,
                                                            AttributeLink.class.getSimpleName()))
    ),

    /**
     * A human-friendly string that can be displayed in UI instead of the raw attribute name.
     */
    LABEL(
        ASSET_META_NAMESPACE + ":label",
        new Access(true, true, true),
        ValueType.STRING,
        null,
        null,
        null,
        false),

    /**
     * If there is a dashboard, some kind of attribute overview, should this attribute be shown.
     */
    SHOW_ON_DASHBOARD(
        ASSET_META_NAMESPACE + ":showOnDashboard",
        new Access(true, false, true),
        ValueType.BOOLEAN,
        null,
        null,
        Values.create(true),
        true),

    /**
     * Format string that can be used to render the attribute value, see https://github.com/alexei/sprintf.js.
     */
    FORMAT(
        ASSET_META_NAMESPACE + ":format",
        new Access(true, false, true),
        ValueType.STRING,
        null,
        null,
        null,
        false),

    /**
     * A human-friendly string describing the purpose of an attribute, useful when rendering editors.
     */
    DESCRIPTION(
        ASSET_META_NAMESPACE + ":description",
        new Access(true, true, true),
        ValueType.STRING,
        null,
        null,
        null,
        false),

    /**
     * Points to semantic description of the attribute, typically a URI.
     */
    ABOUT(
        ASSET_META_NAMESPACE + ":about",
        new Access(true, true, true),
        ValueType.STRING,
        null,
        null,
        null,
        false),

    /**
     * Marks the attribute as read-only for non-superuser clients. South-bound {@link AttributeEvent}s by regular or
     * restricted users are ignored. North-bound {@link AttributeEvent}s made by protocols and rules engine are
     * possible.
     */
    READ_ONLY(
        ASSET_META_NAMESPACE + ":readOnly",
        new Access(true, false, true),
        ValueType.BOOLEAN,
        null,
        null,
        Values.create(true),
        true),

    /**
     * Marks the attribute as readable by restricted clients and therefore users who are linked to the asset, see {@link
     * UserAsset}.
     */
    ACCESS_RESTRICTED_READ(
        ASSET_META_NAMESPACE + ":accessRestrictedRead",
        ACCESS_PRIVATE,
        ValueType.BOOLEAN,
        null,
        null,
        Values.create(true),
        true),

    /**
     * Marks the attribute as writable by restricted clients and therefore users who are linked to the asset, see {@link
     * UserAsset}.
     */
    ACCESS_RESTRICTED_WRITE(
        ASSET_META_NAMESPACE + ":accessRestrictedWrite",
        ACCESS_PRIVATE,
        ValueType.BOOLEAN,
        null,
        null,
        Values.create(true),
        true),

    /**
     * Marks the attribute as readable by unauthenticated public clients.
     */
    ACCESS_PUBLIC_READ(
        ASSET_META_NAMESPACE + ":accessPublicRead",
        ACCESS_PRIVATE,
        ValueType.BOOLEAN,
        null,
        null,
        Values.create(true),
        true),

    /**
     * Marks the attribute as writable by unauthenticated public clients.
     */
    ACCESS_PUBLIC_WRITE(
        ASSET_META_NAMESPACE + ":accessPublicWrite",
        ACCESS_PRIVATE,
        ValueType.BOOLEAN,
        null,
        null,
        Values.create(true),
        true),

//    /**
//     * RT: This wasn't used anywhere and not really sure when it would be useful so removing for now.
//     */
//    /**
//     * Default value that might be used when editing an attribute.
//     */
    //DEFAULT(ASSET_META_NAMESPACE + ":default", new Access(false, false, false), ValueType.STRING),

    /**
     * Minimum range constraint for numeric attribute values.
     */
    RANGE_MIN(
        ASSET_META_NAMESPACE + ":rangeMin",
        new Access(true, false, true),
        ValueType.NUMBER,
        REGEXP_PATTERN_DOUBLE,
        PatternFailure.DOUBLE.name(),
        null,
        false),

    /**
     * Maximum range constraint for numeric attribute values.
     */
    RANGE_MAX(
        ASSET_META_NAMESPACE + ":rangeMax",
        new Access(true, false, true),
        ValueType.NUMBER,
        REGEXP_PATTERN_DOUBLE,
        PatternFailure.DOUBLE.name(),
        null,
        false),

    /**
     * Step increment/decrement constraint for numeric attribute values.
     */
    STEP(
        ASSET_META_NAMESPACE + ":step",
        new Access(true, false, true),
        ValueType.NUMBER,
        REGEXP_PATTERN_DOUBLE,
        PatternFailure.DOUBLE.name(),
        null,
        false),

    /**
     * Regex (Java syntax) constraint for string attribute values.
     */
    PATTERN(
        ASSET_META_NAMESPACE + ":pattern",
        new Access(true, false, true),
        ValueType.STRING,
        null,
        null,
        null,
        false),

    /**
     * Should attribute values be stored in time series database
     */
    STORE_DATA_POINTS(
        ASSET_META_NAMESPACE + ":storeDataPoints",
        new Access(true, false, true),
        ValueType.BOOLEAN,
        null,
        null,
        Values.create(true),
        true),

    /**
     * How long to store attribute values in time series database (data older than this will be automatically purged)
     */
    DATA_POINTS_MAX_AGE_DAYS(
            ASSET_META_NAMESPACE + ":dataPointsMaxAgeDays",
            new Access(true, false, true),
            ValueType.NUMBER,
            REGEXP_PATTERN_INTEGER_POSITIVE_NON_ZERO,
            PatternFailure.INTEGER_POSITIVE_NON_ZERO.name(),
            null,
            false,
            false,
            Values.create(1d),
            null,
            null,
            value -> value == null ? Optional.empty() : Values.getIntegerCoerced(value)
                    .isPresent() ? Optional.empty() : Optional.of(new ValidationFailure(META_ITEM_VALUE_MISMATCH, "Integer"))),

    /**
     * Should attribute writes be processed by the rules engines as {@link AssetState} facts, with a lifecycle that
     * reflects the state of the asset attribute. Each attribute will have one fact at all times in rules memory. These
     * state facts are kept in sync with asset changes: When the attribute is updated, the fact will be updated
     * (replaced). If you want evaluate the change history of an attribute, you typically need to combine this with
     * {@link #RULE_EVENT}.
     */
    RULE_STATE(
        ASSET_META_NAMESPACE + ":ruleState",
        new Access(true, false, true),
        ValueType.BOOLEAN,
        null,
        null,
        Values.create(true),
        true),

    /**
     * Should attribute writes be processed by the rules engines as temporary facts. When an attribute is updated, the
     * change will be inserted as a new {@link AssetState} temporary fact in rules engines. These facts expire
     * automatically after a defined time, see {@link #RULE_EVENT_EXPIRES}. If you want to match (multiple) {@link
     * AssetState}s for the same attribute over time, to evaluate the change history of an attribute, add this meta
     * item.
     */
    RULE_EVENT(
        ASSET_META_NAMESPACE + ":ruleEvent",
        new Access(true, false, true),
        ValueType.BOOLEAN,
        null,
        null,
        Values.create(true),
        true),

    /**
     * Set maximum lifetime of {@link AssetState} temporary facts in rules, for example "1h30m5s". The rules engine will
     * remove temporary {@link AssetState} facts if they are older than this value (using event source/value timestamp,
     * not event processing time).
     * <p>
     * The default expiration for asset events can be configured with environment variable
     * <code>RULE_EVENT_EXPIRES</code>.
     * <p>
     * Also see {@link TemporaryFact#GUARANTEED_MIN_EXPIRATION_MILLIS}.
     */
    RULE_EVENT_EXPIRES(
        ASSET_META_NAMESPACE + ":ruleEventExpires",
        new Access(true, false, true),
        ValueType.STRING,
        "^([+-])?((\\d+)[Dd])?\\s*((\\d+)[Hh])?\\s*((\\d+)[Mm])?\\s*((\\d+)[Ss])?\\s*((\\d+)([Mm][Ss])?)?$",
        // See TimeUtil
        PatternFailure.DAYS_HOURS_MINS_SECONDS.name(),
        null,
        false),

    /**
     * Disabled flag to be used by asset attributes that could require this functionality (e.g. {@link
     * org.openremote.model.asset.agent.ProtocolConfiguration})
     */
    DISABLED(
        ASSET_META_NAMESPACE + ":disabled",
        new Access(true, false, true),
        ValueType.BOOLEAN,
        null,
        null,
        Values.create(true),
        true),

    /**
     * Marks an attribute as being executable so it then supports values of type {@link AttributeExecuteStatus}.
     */
    EXECUTABLE(
        ASSET_META_NAMESPACE + ":executable",
        new Access(true, false, true),
        ValueType.BOOLEAN,
        null,
        null,
        Values.create(true),
        true),

    /**
     * Indicates the discreet values allowed for this attribute, only makes sense for attributes that
     * support a limited number of values otherwise can be <code>null</code>.
     */
    ALLOWED_VALUES(
        ASSET_META_NAMESPACE + ":allowedValues",
        new Access(true, false, true),
        ValueType.ARRAY,
        null,
        null,
        null,
        false),

    /**
     * Indicates the unit type the this attribute represents.
     * For e.g. if the attribute represents currency and it's in euro's then the unit type would be EUR.
     * For e.g. if the attribute represents distance and it's in kilometers then the unit type would be KM.
     */
    UNIT_TYPE(
        ASSET_META_NAMESPACE + ":unitType",
        new Access(true, false, true),
        ValueType.STRING,
        null,
        null,
        null,
        false
    ),

    /**
     * Could possible have predicted datapoints
     */
    HAS_PREDICTED_DATA_POINTS(
        ASSET_META_NAMESPACE + ":hasPredictedDatapoints",
        new Access(true, false, true),
        ValueType.BOOLEAN,
        null,
        null,
        Values.create(true),
        true);

    final protected String urn;
    final protected Access access;
    final protected ValueType valueType;
    final protected Value initialValue;
    protected final Value allowedMin;
    protected final Value allowedMax;
    protected final Value[] allowedValues;
    final protected boolean valueFixed;
    final protected boolean isSecret;
    final protected Integer maxPerAttribute = 1; // All asset meta so far are single instance
    final protected boolean required = false; // All asset meta so far are not mandatory
    final protected String pattern;
    final protected String patternFailureMessage;
    @JsonIgnore
    final protected Function<Value, Optional<ValidationFailure>> validator;

    MetaItemType(String urn, Access access, ValueType valueType, String pattern, String patternFailureMessage, Value initialValue, boolean valueFixed) {
        this(urn, access, valueType, pattern, patternFailureMessage, initialValue, valueFixed, false, null, null, null, null);
    }

    MetaItemType(String urn, Access access, ValueType valueType, String pattern, String patternFailureMessage, Value initialValue, boolean valueFixed, boolean isSecret) {
        this(urn, access, valueType, pattern, patternFailureMessage, initialValue, valueFixed, isSecret, null, null, null, null);
    }

    MetaItemType(String urn,
                 Access access,
                 ValueType valueType,
                 String pattern,
                 String patternFailureMessage,
                 Value initialValue,
                 boolean valueFixed,
                 boolean isSecret,
                 Value allowedMin,
                 Value allowedMax,
                 Value[] allowedValues,
                 Function<Value, Optional<ValidationFailure>> validator) {

        if (initialValue != null && initialValue.getType() != valueType) {
            throw new IllegalStateException("Initial asset meta value must be of the same type as the asset meta");
        }
        this.validator = validator;
        this.urn = urn;
        this.access = access;
        this.valueType = valueType;
        this.initialValue = initialValue;
        this.valueFixed = valueFixed;
        this.pattern = pattern;
        this.patternFailureMessage = patternFailureMessage;
        this.allowedMin = allowedMin;
        this.allowedMax = allowedMax;
        this.allowedValues = allowedValues;
        this.isSecret = isSecret;
    }


    @Override
    public String getUrn() {
        return urn;
    }

    @Override
    public Access getAccess() {
        return access;
    }

    @Override
    public ValueType getValueType() {
        return valueType;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public String getPatternFailureMessage() {
        return patternFailureMessage;
    }

    @Override
    public Integer getMaxPerAttribute() {
        return maxPerAttribute;
    }

    @Override
    public Optional<Function<Value, Optional<ValidationFailure>>> getValidator() {
        return Optional.ofNullable(validator);
    }

    @Override
    public boolean isSecret() {
        return isSecret;
    }

    @Override
    public Value getInitialValue() {
        return initialValue;
    }

    @Override
    public boolean isValueFixed() {
        return valueFixed;
    }

    @Override
    public Value getAllowedMin() {
        return allowedMin;
    }

    @Override
    public Value getAllowedMax() {
        return allowedMax;
    }

    @Override
    public Value[] getAllowedValues() {
        return allowedValues;
    }

    public MetaItemDescriptor withInitialValue(String initialValue) {
        return withInitialValue(Values.create(initialValue));
    }

    public MetaItemDescriptor withInitialValue(int initialValue) {
        return withInitialValue(Values.create(initialValue));
    }

    public MetaItemDescriptor withInitialValue(boolean initialValue) {
        return withInitialValue(Values.create(initialValue));
    }

    public MetaItemDescriptor withInitialValue(double initialValue) {
        return withInitialValue(Values.create(initialValue));
    }

    public MetaItemDescriptor withInitialValue(Value initialValue) {
        return new MetaItemDescriptorImpl(this, initialValue);
    }
}
