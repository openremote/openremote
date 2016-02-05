package org.openremote.manager.shared.util;

import javax.persistence.AttributeConverter;

public class StringArrayConverter implements AttributeConverter<String[], String> {

    @Override
    public String convertToDatabaseColumn(String[] attribute) {
        if (attribute == null || attribute.length == 0)
            return null;
        StringBuilder sb = new StringBuilder();
        for (String s : attribute) {
            sb.append(s).append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length()-1);
        }
        return sb.toString();
    }

    @Override
    public String[] convertToEntityAttribute(String data) {
        if (data == null)
            return new String[0];
        return data.split(",");
    }
}