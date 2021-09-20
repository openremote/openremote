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
package org.openremote.model.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.impl.UnknownAsset;
import org.openremote.model.util.ValueUtil;

import java.io.IOException;

/**
 * Resolves asset type strings using {@link org.openremote.model.util.ValueUtil}
 */
public class AssetTypeIdResolver extends TypeIdResolverBase {
    @SuppressWarnings("unchecked")
    @Override
    public String idFromValue(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Asset)) {
            throw new IllegalArgumentException("Type must be an asset type");
        }
        return ((Asset<?>)value).getType();
    }

    @SuppressWarnings("unchecked")
    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        return idFromValue(value);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
        AssetDescriptor<?> descriptor = ValueUtil.getAssetDescriptor(id).orElse(UnknownAsset.DESCRIPTOR);
        return context.constructType(descriptor.getType());
    }
}
