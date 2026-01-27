/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.jackson;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.impl.ThingAsset;
import org.openremote.model.util.ValueUtil;

/** Resolves asset type strings using {@link org.openremote.model.util.ValueUtil} */
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
    return ((Asset<?>) value).getType();
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

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public JavaType typeFromId(DatabindContext context, String id) throws IOException {
    Class<?> assetClass =
        ValueUtil.getAssetDescriptor(id)
            .map(AssetDescriptor::getType)
            .orElse((Class) ThingAsset.class);
    return context.constructType(assetClass);
  }
}
