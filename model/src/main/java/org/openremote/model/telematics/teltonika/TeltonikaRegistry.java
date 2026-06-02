package org.openremote.model.telematics.teltonika;

import org.openremote.model.asset.Asset;
import org.openremote.model.telematics.parameter.DynamicParameter;
import org.openremote.model.telematics.parameter.ParseableValueDescriptor;
import org.openremote.model.telematics.parameter.TelematicsParameterRegistry;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Parameter registry for Teltonika devices.
 * <p>
 * Provides access to all known Teltonika parameters from {@link TeltonikaParameters}
 * and creates dynamic parameters for unknown IDs.
 */
public class TeltonikaRegistry implements TelematicsParameterRegistry<TeltonikaParameter<?>> {

    private static final Logger LOG = Logger.getLogger(TeltonikaRegistry.class.getName());
    private static final TeltonikaRegistry INSTANCE = new TeltonikaRegistry();

    private final Map<String, TeltonikaParameter<?>> parametersById;
    private final Map<String, TeltonikaParameter<?>> parametersByFullName;
    private final Map<String, DynamicParameter<?>> dynamicParameters = new ConcurrentHashMap<>();

    private TeltonikaRegistry() {
        Map<String, TeltonikaParameter<?>> byId = new HashMap<>();
        Map<String, TeltonikaParameter<?>> byFullName = new HashMap<>();

        for (Field field : TeltonikaParameters.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) &&
                Modifier.isFinal(field.getModifiers()) &&
                TeltonikaParameter.class.isAssignableFrom(field.getType())) {
                try {
                    TeltonikaParameter<?> param = (TeltonikaParameter<?>) field.get(null);
                    if (param != null) {
                        byId.put(param.getId(), param);
                        byFullName.put(param.getName(), param);
                    }
                } catch (IllegalAccessException e) {
                    LOG.warning("Cannot access field " + field.getName() + ": " + e.getMessage());
                }
            }
        }

        this.parametersById = Collections.unmodifiableMap(byId);
        this.parametersByFullName = Collections.unmodifiableMap(byFullName);

        LOG.info("TeltonikaRegistry: " + parametersById.size() + " parameters");
    }

    public static TeltonikaRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public String getVendorPrefix() {
        return TeltonikaParameter.VENDOR_PREFIX;
    }

    @Override
    public Optional<TeltonikaParameter<?>> getById(String id) {
        return Optional.ofNullable(parametersById.get(id));
    }

    @Override
    public Optional<TeltonikaParameter<?>> getByFullName(String fullName) {
        return Optional.ofNullable(parametersByFullName.get(fullName));
    }

    public Optional<TeltonikaParameter<?>> getByName(String name) {
        return getByFullName(name);
    }

    @Override
    public ParseableValueDescriptor<?> getOrCreateDynamic(String id, int byteLength) {
        TeltonikaParameter<?> known = parametersById.get(id);
        if (known != null) {
            return known;
        }

        return dynamicParameters.computeIfAbsent(id, k -> {
            LOG.fine("Dynamic parameter: " + id + " (length=" + byteLength + ")");
            return (DynamicParameter<?>) DynamicParameter.fromByteLength(getVendorPrefix(), id, byteLength);
        });
    }

    @Override
    public Stream<TeltonikaParameter<?>> all() {
        return parametersById.values().stream();
    }

    @Override
    public int size() {
        return parametersById.size();
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<AttributeDescriptor<T>> findMatchingAttributeDescriptor(
            Class<? extends Asset<?>> assetClass,
            ValueDescriptor<T> valueDescriptor) {

        return Arrays.stream(assetClass.getFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()) &&
                        AttributeDescriptor.class.isAssignableFrom(field.getType()))
                .map(field -> {
                    try {
                        return (AttributeDescriptor<?>) field.get(null);
                    } catch (IllegalAccessException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(attrDesc -> attrDesc.getType().getName().equals(valueDescriptor.getName()))
                .findFirst()
                .map(attrDesc -> (AttributeDescriptor<T>) attrDesc);
    }

    public int getDynamicParameterCount() {
        return dynamicParameters.size();
    }
}
