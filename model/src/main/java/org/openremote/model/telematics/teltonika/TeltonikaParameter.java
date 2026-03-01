package org.openremote.model.telematics.teltonika;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Transient;
import org.openremote.model.telematics.parameter.ParameterParser;
import org.openremote.model.telematics.parameter.ParseableValueDescriptor;
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueFormat;

import java.util.Optional;

/**
 * Teltonika-specific ParseableValueDescriptor.
 * <p>
 * Extends ParseableValueDescriptor with Teltonika AVL metadata:
 * <ul>
 *   <li>AVL ID (numeric identifier in the binary protocol)</li>
 *   <li>Parameter group (e.g., "Permanent I/O Elements")</li>
 *   <li>Hardware support (which device models support this)</li>
 *   <li>Min/Max values and multiplier from specification</li>
 * </ul>
 *
 * @param <T> The type of the parameter value
 */
public class TeltonikaParameter<T> extends ParseableValueDescriptor<T> {

    public static final String VENDOR_PREFIX = "teltonika";

    private final String id;
    private final String displayName;
    private final String description;
    @Transient
    @JsonIgnore
    private final transient String units;
    private final String parameterGroup;
    private final String hwSupport;
    private final Long minValue;
    private final Long maxValue;
    private final Double multiplier;

    private TeltonikaParameter(Builder<T> builder) {
        super(VENDOR_PREFIX + "_" + builder.id, builder.type, builder.byteLength, builder.parser, builder.constraints);
        if (builder.valueFormat != null) {
            this.format = builder.valueFormat;
        }
        if (builder.valueUnits != null) {
            super.units = builder.valueUnits;
        }
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.units = builder.units;
        this.parameterGroup = builder.parameterGroup;
        this.hwSupport = builder.hwSupport;
        this.minValue = builder.minValue;
        this.maxValue = builder.maxValue;
        this.multiplier = builder.multiplier;
    }

    /**
     * The AVL ID (e.g., "239", "sp", "alt").
     */
    public String getId() {
        return id;
    }

    public Optional<String> getDisplayName() {
        return Optional.ofNullable(displayName);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<String> getUnit() {
        return Optional.ofNullable(units);
    }

    public Optional<String> getParameterGroup() {
        return Optional.ofNullable(parameterGroup);
    }

    public Optional<String> getHwSupport() {
        return Optional.ofNullable(hwSupport);
    }

    public Optional<Long> getMinValue() {
        return Optional.ofNullable(minValue);
    }

    public Optional<Long> getMaxValue() {
        return Optional.ofNullable(maxValue);
    }

    public Optional<Double> getMultiplier() {
        return Optional.ofNullable(multiplier);
    }

    @Override
    public String toString() {
        return "TeltonikaParameter{" +
                "id='" + id + '\'' +
                ", name='" + getName() + '\'' +
                ", type=" + getType().getSimpleName() +
                ", byteLength=" + getByteLength() +
                '}';
    }

    /**
     * Creates a new builder.
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Creates a builder initialized from an existing {@link ValueDescriptor} and parser.
     * Useful when reusing predefined {@link org.openremote.model.value.ValueType} descriptors
     * (constraints/format/units) while adding protocol parsing details.
     */
    public static <T> Builder<T> builder(ValueDescriptor<T> valueDescriptor, ParameterParser<T> parser) {
        return TeltonikaParameter.<T>builder()
                .valueDescriptor(valueDescriptor)
                .parser(parser);
    }

    /**
     * Creates a builder initialized from an existing {@link ValueDescriptor}, fixed byte length and parser.
     */
    public static <T> Builder<T> builder(ValueDescriptor<T> valueDescriptor, int byteLength, ParameterParser<T> parser) {
        return TeltonikaParameter.<T>builder(valueDescriptor, parser)
                .byteLength(byteLength);
    }

    public static class Builder<T> {
        private String id;
        private Class<T> type;
        private int byteLength = -1;
        private ParameterParser<T> parser;
        private ValueConstraint[] constraints = new ValueConstraint[0];
        private ValueFormat valueFormat;
        private String[] valueUnits;
        private String displayName;
        private String description;
        private String units;
        private String parameterGroup;
        private String hwSupport;
        private Long minValue;
        private Long maxValue;
        private Double multiplier;

        public Builder<T> id(String id) {
            this.id = id;
            return this;
        }

        public Builder<T> id(int avlId) {
            this.id = String.valueOf(avlId);
            return this;
        }

        public Builder<T> type(Class<T> type) {
            this.type = type;
            return this;
        }

        /**
         * Reuse an existing value descriptor (type/constraints/format/units) and keep only parser/byte-length vendor-specific.
         */
        public Builder<T> valueDescriptor(ValueDescriptor<T> valueDescriptor) {
            this.type = valueDescriptor.getType();
            ValueConstraint[] descriptorConstraints = valueDescriptor.getConstraints();
            this.constraints = descriptorConstraints != null ? descriptorConstraints : new ValueConstraint[0];
            this.valueFormat = valueDescriptor.getFormat();
            this.valueUnits = valueDescriptor.getUnits();
            if (this.units == null && valueDescriptor.getUnits() != null && valueDescriptor.getUnits().length > 0) {
                this.units = valueDescriptor.getUnits()[0];
            }
            return this;
        }

        public Builder<T> byteLength(int byteLength) {
            this.byteLength = byteLength;
            return this;
        }

        public Builder<T> parser(ParameterParser<T> parser) {
            this.parser = parser;
            return this;
        }

        public Builder<T> constraints(ValueConstraint... constraints) {
            this.constraints = constraints;
            return this;
        }

        public Builder<T> displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder<T> description(String description) {
            this.description = description;
            return this;
        }

        public Builder<T> units(String units) {
            this.units = units;
            this.valueUnits = units != null ? new String[] {units} : null;
            return this;
        }

        public Builder<T> parameterGroup(String parameterGroup) {
            this.parameterGroup = parameterGroup;
            return this;
        }

        public Builder<T> hwSupport(String hwSupport) {
            this.hwSupport = hwSupport;
            return this;
        }

        public Builder<T> minValue(Long minValue) {
            this.minValue = minValue;
            return this;
        }

        public Builder<T> maxValue(Long maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        public Builder<T> multiplier(Double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public TeltonikaParameter<T> build() {
            return new TeltonikaParameter<>(this);
        }
    }
}
