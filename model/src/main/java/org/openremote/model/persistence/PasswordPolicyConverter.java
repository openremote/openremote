package org.openremote.model.persistence;

import jakarta.persistence.AttributeConverter;

import java.util.Arrays;
import java.util.List;

public class PasswordPolicyConverter implements AttributeConverter<List<String>, String> {


    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        return attribute != null ? String.join(" and ", attribute) : null;
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        return dbData != null ? Arrays.stream(dbData.split(" and ")).toList() : null;
    }

}
