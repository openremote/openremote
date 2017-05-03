/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.server.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class JsonUtil {

    protected JsonUtil() {}

    /**
     * For some reason the GWT compiler no longer likes this being in the {@link org.openremote.model.util.JsonUtil}
     * class; no idea why as it used to compile fine with this in there???
     */
    // TODO: Figure out why this won't compile when located in the model JsonUtil class and then remove this class
    @SuppressWarnings("unchecked")
    public static <T> T convert(ObjectMapper objectMapper, Class<T> targetType, Object object) {
        Map<String, Object> props = objectMapper.convertValue(object, Map.class);
        return objectMapper.convertValue(props, targetType);
    }
}
