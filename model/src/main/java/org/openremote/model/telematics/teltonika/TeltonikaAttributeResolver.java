package org.openremote.model.telematics.teltonika;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.telematics.parameter.ParseableValueDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Resolves protocol parameters to typed OpenRemote attributes.
 *
 * Supports both known Teltonika parameters and unknown values. Unknown values are still mapped
 * to deterministic attribute names using {@code teltonika_<id>}.
 */
public class TeltonikaAttributeResolver {

    private static final Logger LOG = Logger.getLogger(TeltonikaAttributeResolver.class.getName());
    private static final String VENDOR_PREFIX = TeltonikaParameter.VENDOR_PREFIX + "_";

    private final TeltonikaRegistry parameterRegistry;

    public TeltonikaAttributeResolver() {
        this(TeltonikaRegistry.getInstance());
    }

    public TeltonikaAttributeResolver(TeltonikaRegistry parameterRegistry) {
        this.parameterRegistry = parameterRegistry;
    }

    public Attribute<?> resolveBinaryIo(int parameterId, ByteBuf value, long timestamp) {
        return resolveBinaryIo(String.valueOf(parameterId), value, timestamp);
    }

    public Attribute<?> resolveBinaryIo(String parameterId, ByteBuf value, long timestamp) {
        int normalizedLength = value.readableBytes();
        Optional<TeltonikaParameter<?>> descriptor = findDescriptor(parameterId);

        if (descriptor.isPresent()) {
            TeltonikaParameter<?> resolved = descriptor.get();
            if (resolved.hasFixedLength() && resolved.getByteLength() != normalizedLength) {
                LOG.warning("Teltonika parameter length mismatch for ID " + parameterId +
                        ": descriptor expects " + resolved.getByteLength() +
                        " bytes but payload has " + normalizedLength + " bytes. Treating as raw value.");
                return createRawAttribute(parameterId, value, normalizedLength, timestamp);
            }
            return parseKnownBinary(parameterId, descriptor.get(), value, timestamp);
        }

        return createRawAttribute(parameterId, value, normalizedLength, timestamp);
    }

    public Attribute<?> resolveJson(String parameterId, Object rawValue, long timestamp) {
        Optional<TeltonikaParameter<?>> descriptor = findDescriptor(parameterId);
        if (descriptor.isPresent()) {
            return parseKnownJson(parameterId, descriptor.get(), rawValue, timestamp);
        }
        return createRawJsonAttribute(parameterId, rawValue, timestamp);
    }

    private Attribute<?> parseKnownBinary(String parameterId, TeltonikaParameter<?> descriptor, ByteBuf value, long timestamp) {
        ByteBuf copy = Unpooled.buffer(value.readableBytes());
        value.getBytes(value.readerIndex(), copy, value.readableBytes());
        try {
            Object parsedValue = descriptor.hasFixedLength() ? descriptor.parse(copy) : descriptor.parse(copy, copy.readableBytes());
            return createDescriptorAttribute(descriptor, parsedValue, timestamp)
                    .orElseGet(() -> createFallbackTypedAttribute(normalizeAttributeName(parameterId), parsedValue, timestamp));
        } catch (Exception e) {
            String rawHex = ByteBufUtil.hexDump(value, value.readerIndex(), value.readableBytes()).toUpperCase();
            throw new IllegalStateException(
                    "Failed parsing Teltonika binary parameter id='" + parameterId +
                            "' descriptor='" + descriptor.getName() +
                            "' byteLength=" + value.readableBytes() +
                            " rawHex='" + rawHex + "'",
                    e
            );
        } finally {
            copy.release();
        }
    }

    private Attribute<?> parseKnownJson(String parameterId, TeltonikaParameter<?> descriptor, Object rawValue, long timestamp) {
        try {
            ByteBuf binary = convertJsonValueToBinary(rawValue, descriptor);
            try {
                Object parsedValue = descriptor.hasFixedLength() ? descriptor.parse(binary) : descriptor.parse(binary, binary.readableBytes());
                return createDescriptorAttribute(descriptor, parsedValue, timestamp)
                        .orElseGet(() -> createFallbackTypedAttribute(normalizeAttributeName(parameterId), parsedValue, timestamp));
            } finally {
                binary.release();
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed parsing Teltonika JSON parameter id='" + parameterId +
                            "' descriptor='" + descriptor.getName() +
                            "' rawValue='" + rawValue + "'",
                    e
            );
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<Attribute<?>> createDescriptorAttribute(TeltonikaParameter<?> descriptor, Object value, long timestamp) {
        Optional<AttributeDescriptor<Object>> attributeDescriptor = (Optional<AttributeDescriptor<Object>>) (Optional<?>)
                parameterRegistry.findMatchingAttributeDescriptor(TeltonikaTrackerAsset.class, descriptor);

        if (attributeDescriptor.isPresent()) {
            return Optional.of(new Attribute<>(attributeDescriptor.get(), value, timestamp));
        }

        return Optional.empty();
    }

    private Optional<TeltonikaParameter<?>> findDescriptor(String rawId) {
        String id = stripPrefix(rawId);
        return parameterRegistry.getById(id)
                .or(() -> parameterRegistry.getByFullName(normalizeAttributeName(id)))
                .or(() -> parameterRegistry.getByFullName(rawId));
    }

    private String stripPrefix(String rawId) {
        if (rawId == null) {
            return "unknown";
        }
        return rawId.startsWith(VENDOR_PREFIX) ? rawId.substring(VENDOR_PREFIX.length()) : rawId;
    }

    private String normalizeAttributeName(String id) {
        String normalizedId = stripPrefix(id);
        return VENDOR_PREFIX + normalizedId;
    }

    private Attribute<?> createRawAttribute(String parameterId, ByteBuf value, int length, long timestamp) {
        LOG.warning("Unknown Teltonika parameter ID " + parameterId + " with length " + length + ". Storing as raw hex.");
        String name = normalizeAttributeName(parameterId);
        int readerIndex = value.readerIndex();

        if (length == 1) {
            return new Attribute<>(name, ValueType.INTEGER, (int) value.getUnsignedByte(readerIndex), timestamp);
        }
        if (length == 2) {
            return new Attribute<>(name, ValueType.INTEGER, value.getUnsignedShort(readerIndex), timestamp);
        }
        if (length == 4) {
            return new Attribute<>(name, ValueType.LONG, value.getUnsignedInt(readerIndex), timestamp);
        }
        if (length == 8) {
            return new Attribute<>(name, ValueType.LONG, value.getLong(readerIndex), timestamp);
        }

        String hexValue = ByteBufUtil.hexDump(value, readerIndex, length).toUpperCase();
        return new Attribute<>(name, ValueType.TEXT, hexValue, timestamp);
    }

    private Attribute<?> createRawJsonAttribute(String parameterId, Object rawValue, long timestamp) {
        String name = normalizeAttributeName(parameterId);
        Object normalized = rawValue;
        if (normalized instanceof Number number) {
            if (number.doubleValue() == number.longValue()) {
                long v = number.longValue();
                if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) {
                    return new Attribute<>(name, ValueType.INTEGER, (int) v, timestamp);
                }
                return new Attribute<>(name, ValueType.LONG, v, timestamp);
            }
            return new Attribute<>(name, ValueType.NUMBER, number.doubleValue(), timestamp);
        }
        if (normalized instanceof Boolean bool) {
            return new Attribute<>(name, ValueType.BOOLEAN, bool, timestamp);
        }
        return new Attribute<>(name, ValueType.TEXT, normalized != null ? normalized.toString() : null, timestamp);
    }

    private Attribute<?> createFallbackTypedAttribute(String name, Object value, long timestamp) {
        if (value instanceof Integer) {
            return new Attribute<>(name, ValueType.INTEGER, (Integer) value, timestamp);
        }
        if (value instanceof Long) {
            return new Attribute<>(name, ValueType.LONG, (Long) value, timestamp);
        }
        if (value instanceof Double) {
            return new Attribute<>(name, ValueType.NUMBER, (Double) value, timestamp);
        }
        if (value instanceof Float floatValue) {
            return new Attribute<>(name, ValueType.NUMBER, floatValue.doubleValue(), timestamp);
        }
        if (value instanceof Boolean) {
            return new Attribute<>(name, ValueType.BOOLEAN, (Boolean) value, timestamp);
        }
        return new Attribute<>(name, ValueType.TEXT, value != null ? value.toString() : null, timestamp);
    }

    private ByteBuf convertJsonValueToBinary(Object value, ParseableValueDescriptor<?> valueDescriptor) {
        if (value == null) {
            return Unpooled.buffer(0);
        }

        int length = valueDescriptor.getByteLength();
        if (length <= 0) {
            byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
            return Unpooled.wrappedBuffer(bytes);
        }

        Class<?> expectedType = valueDescriptor.getType();
        ByteBuf buffer = Unpooled.buffer(length);

        try {
            switch (length) {
                case 1 -> {
                    if (expectedType == Boolean.class) {
                        boolean boolVal = (value instanceof Boolean b && b)
                                || (value instanceof Number n && n.intValue() == 1)
                                || "true".equalsIgnoreCase(value.toString())
                                || "1".equals(value.toString());
                        buffer.writeByte(boolVal ? 1 : 0);
                    } else if (value instanceof Number number) {
                        buffer.writeByte(number.intValue());
                    } else {
                        buffer.writeByte(Integer.parseInt(value.toString()));
                    }
                }
                case 2 -> {
                    if (value instanceof Number number) {
                        buffer.writeShort(number.shortValue());
                    } else {
                        buffer.writeShort(Integer.parseInt(value.toString()));
                    }
                }
                case 4 -> {
                    if (value instanceof Number number) {
                        buffer.writeInt(number.intValue());
                    } else {
                        buffer.writeInt(Integer.parseInt(value.toString()));
                    }
                }
                case 8 -> {
                    if (value instanceof Number number) {
                        buffer.writeLong(number.longValue());
                    } else {
                        buffer.writeLong(Long.parseLong(value.toString()));
                    }
                }
                default -> {
                    byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
                    int bytesToWrite = Math.min(bytes.length, length);
                    buffer.writeBytes(bytes, 0, bytesToWrite);
                    for (int i = bytesToWrite; i < length; i++) {
                        buffer.writeByte(0);
                    }
                }
            }
            return buffer;
        } catch (Exception e) {
            buffer.release();
            throw new IllegalArgumentException(
                    "Cannot convert JSON value '" + value + "' to binary for descriptor '" + valueDescriptor.getName() +
                            "' with byteLength=" + length,
                    e
            );
        }
    }
}
