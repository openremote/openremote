/*
 * Copyright 2026, OpenRemote Inc.
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

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import org.openremote.model.asset.Asset;
import org.openremote.model.util.ValueUtil;

import java.io.IOException;

/**
 * Jackson 2 wrapper deserializer for {@link Asset} subclasses. Sets the asset type info
 * in the deserialization context before delegating to the standard POJO deserializer, so
 * that {@link AttributeDeserializerJackson2} can resolve attribute types from the asset model.
 */
public class AssetDeserializerJackson2 extends StdDeserializer<Asset<?>>
    implements ContextualDeserializer, ResolvableDeserializer {

    // Must match Asset.AssetDeserializer.ASSET_TYPE_INFO_ATTRIBUTE (avoid loading Jackson3 class here)
    static final String ASSET_TYPE_INFO_ATTRIBUTE = "assetTypeInfo";

    protected final JsonDeserializer<?> defaultDeserializer;
    protected final Class<?> clazz;

    public AssetDeserializerJackson2(JsonDeserializer<?> defaultDeserializer, Class<?> clazz) {
        super(clazz);
        this.defaultDeserializer = defaultDeserializer;
        this.clazz = clazz;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Asset<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ctxt.setAttribute(ASSET_TYPE_INFO_ATTRIBUTE, ValueUtil.getAssetInfo(clazz.getSimpleName()).orElse(null));
        return (Asset<?>) defaultDeserializer.deserialize(p, ctxt);
    }

    @Override
    public void resolve(DeserializationContext ctxt) throws JsonMappingException {
        if (defaultDeserializer instanceof ResolvableDeserializer rd) {
            rd.resolve(ctxt);
        }
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        if (!(defaultDeserializer instanceof ContextualDeserializer cd)) {
            return this;
        }
        JsonDeserializer<?> contextual = cd.createContextual(ctxt, property);
        if (contextual == defaultDeserializer) {
            return this;
        }
        return new AssetDeserializerJackson2(contextual, clazz);
    }

    @Override
    public JsonDeserializer<?> getDelegatee() {
        return defaultDeserializer;
    }
}
