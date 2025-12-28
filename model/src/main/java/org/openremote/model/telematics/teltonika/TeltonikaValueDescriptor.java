package org.openremote.model.telematics.teltonika;

import io.netty.buffer.ByteBuf;
import org.openremote.model.telematics.ParsingValueDescriptor;
import org.openremote.model.value.ValueConstraint;

import java.util.Optional;
import java.util.function.Function;

public class TeltonikaValueDescriptor<T> extends ParsingValueDescriptor<T> {

    public static final String VENDOR_PREFIX = "teltonika";

    protected int avlId;
    protected String avlType;
    protected Long min;
    protected Long max;
    protected Long multiplier;
    protected String units;
    protected String description;
    protected String hwSupport;
    protected String parameterGroup;

    public TeltonikaValueDescriptor(String name, Class<T> avlType, ValueConstraint... constraints) {
        super(VENDOR_PREFIX, name, avlType, -1, avlType::cast, constraints);
    }

    public TeltonikaValueDescriptor(String name, Class<T> avlType, Function<ByteBuf, T> parser, ValueConstraint... constraints) {
        super(VENDOR_PREFIX, name, avlType, -1, parser, constraints);
    }

    public TeltonikaValueDescriptor(String name, Class<T> avlType, int length, Function<ByteBuf, T> parser, ValueConstraint... constraints) {
        super(VENDOR_PREFIX, name, avlType, length, parser, constraints);
    }

    public TeltonikaValueDescriptor(Class<T> type, int length, Function<ByteBuf, T> parser, int avlId, String name, String avlType, Long min, Long max, Long multiplier, String units, String description, String hwSupport, String parameterGroup, ValueConstraint... constraints) {
        super(VENDOR_PREFIX, String.valueOf(avlId), type, length, parser, constraints);
        this.avlId = avlId;
        this.avlType = avlType;
        this.min = min;
        this.max = max;
        this.multiplier = multiplier;
        this.units = units;
        this.description = description;
        this.hwSupport = hwSupport;
        this.parameterGroup = parameterGroup;
    }

    public int getAvlId() {
        return avlId;
    }

    public String getAvlType() {
        return avlType;
    }

    public void setAvlType(String type) {
        this.avlType = type;
    }

    public Optional<Long> getMin() {
        return Optional.ofNullable(min);
    }

    public void setMin(Long min) {
        this.min = min;
    }

    public Optional<Long> getMax() {
        return Optional.ofNullable(max);
    }

    public void setMax(Long max) {
        this.max = max;
    }

    public Optional<Long> getMultiplier() {
        return Optional.ofNullable(multiplier);
    }

    public void setMultiplier(Long multiplier) {
        this.multiplier = multiplier;
    }

    public Optional<String> getAvlUnits() {
        return Optional.ofNullable(units);
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Optional<String> getHwSupport() {
        return Optional.ofNullable(hwSupport);
    }

    public void setHwSupport(String hwSupport) {
        this.hwSupport = hwSupport;
    }

    public Optional<String> getParameterGroup() {
        return Optional.ofNullable(parameterGroup);
    }

    public void setParameterGroup(String parameterGroup) {
        this.parameterGroup = parameterGroup;
    }
}
