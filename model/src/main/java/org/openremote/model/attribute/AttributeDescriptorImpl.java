package org.openremote.model.attribute;

import java.util.Optional;

public enum AttributeDescriptorImpl implements AttributeDescriptor {

    LOCATION("location", AttributeType.LOCATION),

    CONSOLE_PROVIDER_GEOFENCE("providerGeofence", AttributeType.OBJECT);

    final protected String name;
    final protected AttributeType attributeType;
    final protected MetaItem[] defaultMetaItems;

    AttributeDescriptorImpl(String name, AttributeType attributeType, MetaItem... defaultMetaItems) {
        this.name = name;
        this.attributeType = attributeType;
        this.defaultMetaItems = defaultMetaItems;
    }

    public static Optional<AttributeDescriptor> getByValue(String name) {
        if (name == null)
            return Optional.empty();

        for (AttributeDescriptor descriptor : values()) {
            if (name.equals(descriptor.getName()))
                return Optional.of(descriptor);
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AttributeType getType() {
        return attributeType;
    }

    @Override
    public MetaItem[] getDefaultMetaItems() {
        return defaultMetaItems;
    }
}
