package org.openremote.model.attribute;

import org.openremote.model.value.Value;

import java.util.Optional;

public enum AttributeDescriptorImpl implements AttributeDescriptor {

    LOCATION("location", AttributeType.LOCATION);

    final protected String name;
    final protected AttributeType attributeType;

    AttributeDescriptorImpl(String name, AttributeType attributeType) {
        this.name = name;
        this.attributeType = attributeType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AttributeType getType() {
        return attributeType;
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
}
