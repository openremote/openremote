package org.openremote.container.json;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class JsonUtil {

    public static <T> T convert(ObjectMapper objectMapper, Class<T> targetType, Object object) {
        Map<String, Object> props = objectMapper.convertValue(object, Map.class);
        return objectMapper.convertValue(props, targetType);
    }

}
