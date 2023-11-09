package org.openremote.model.persistence;

import jakarta.persistence.AttributeConverter;

import java.time.Instant;

public class InstantEpochConverter implements AttributeConverter<Instant, Long> {
    @Override
    public Long convertToDatabaseColumn(Instant attribute) {
        return attribute != null ? attribute.toEpochMilli() : 0;
    }

    @Override
    public Instant convertToEntityAttribute(Long dbData) {
        return dbData != null ? Instant.ofEpochMilli(dbData) : null;
    }
}
