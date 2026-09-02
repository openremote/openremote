/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.container.persistence;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import org.openremote.model.util.ValueUtil;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** A {@link FormatMapper} that uses our own {@link ObjectMapper}. */
@SuppressWarnings("unchecked")
public class JsonFormatMapper implements FormatMapper {

  @Override
  public <T> T fromString(
      CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
    if (javaType.getJavaType() == String.class) {
      return (T) charSequence.toString();
    }
    ObjectMapper mapper = ValueUtil.JSON;
    try {
      return mapper.readValue(
          charSequence.toString(), mapper.constructType(javaType.getJavaType()));
    } catch (JacksonException e) {
      throw new IllegalArgumentException(
          "Could not deserialize string to java type: " + javaType, e);
    }
  }

  @Override
  public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
    if (javaType.getJavaType() == String.class) {
      return (String) value;
    }
    ObjectMapper mapper = ValueUtil.JSON;
    try {
      Type targetType = javaType.getJavaType();
      Class<?> rawClass = mapper.constructType(targetType).getRawClass();
      if (value != null
          && (rawClass.isInterface() || Modifier.isAbstract(rawClass.getModifiers()))) {
        return mapper.writeValueAsString(value);
      }
      return mapper.writerFor(mapper.constructType(targetType)).writeValueAsString(value);
    } catch (JacksonException e) {
      throw new IllegalArgumentException("Could not serialize object of java type: " + javaType, e);
    }
  }
}
