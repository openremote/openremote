/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.container.persistence;

import com.vladmihalcea.hibernate.type.util.ObjectMapperWrapper;
import org.hibernate.internal.util.SerializationHelper;
import org.openremote.model.util.ValueUtil;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public class CustomObjectMapperJsonSerializer extends com.vladmihalcea.hibernate.type.util.ObjectMapperJsonSerializer {

    // super class field is private
    protected static ObjectMapperWrapper objectMapperWrapper = new ObjectMapperWrapper(ValueUtil.JSON);

    public CustomObjectMapperJsonSerializer() {
        super(objectMapperWrapper);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T clone(T object) {

        boolean useStandardSerialisation = false;

        // Bug in default implementation means it cannot cope with subclasses of parameterized types that don't have type params
        if (object instanceof Collection && object.getClass().getTypeParameters().length == 0) {
            Object firstElement = ValueUtil.findFirstNonNullElement((Collection<?>) object);

            if (firstElement != null && !(firstElement instanceof Serializable)) {
                return objectMapperWrapper.fromBytes(objectMapperWrapper.toBytes(object), (Class<T>)object.getClass());
            } else {
                useStandardSerialisation = true;
            }
        }

        if (object instanceof Map && object.getClass().getTypeParameters().length == 0) {
            Map.Entry<?,?> firstEntry = ValueUtil.findFirstNonNullEntry((Map<?,?>) object);
            if (firstEntry != null) {
                Object key = firstEntry.getKey();
                Object value = firstEntry.getValue();
                if (!(key instanceof Serializable) || !(value instanceof Serializable)) {
                    return objectMapperWrapper.fromBytes(objectMapperWrapper.toBytes(object), (Class<T>)object.getClass());
                } else {
                    useStandardSerialisation = true;
                }
            }
        }

        if (useStandardSerialisation) {
            return object instanceof Serializable ?
                (T) SerializationHelper.clone((Serializable) object) :
                objectMapperWrapper.fromBytes(objectMapperWrapper.toBytes(object), (Class<T>) object.getClass());
        }

        return super.clone(object);
    }
}
