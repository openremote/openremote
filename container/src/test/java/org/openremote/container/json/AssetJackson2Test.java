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
package org.openremote.container.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.impl.ThingAsset;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.ValueType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reproduces what RESTEasy's Jackson 2 provider (the only JSON {@link jakarta.ws.rs.ext.MessageBodyReader}/
 * {@link jakarta.ws.rs.ext.MessageBodyWriter} on the classpath) does when the manager's own REST API
 * (e.g. {@code POST /api/asset}) round trips an {@link Asset}.
 */
class AssetJackson2Test {

    static final ObjectMapper JSON = Jackson2Config.configureObjectMapper(new ObjectMapper());

    @BeforeAll
    static void setup() {
        if (ValueUtil.getValueDescriptor("text").isEmpty()) {
            ValueUtil.initialise(null);
        }
    }

    /**
     * Verifies that {@link org.openremote.container.json.Jackson2Config}'s
     * {@link org.openremote.model.jackson.AssetDeserializerJackson2} sets the asset type info context
     * so {@link org.openremote.model.jackson.AttributeDeserializerJackson2} can resolve a value type
     * from the asset descriptor even when the JSON omits the {@code "type"} field entirely.
     * Without {@code AssetDeserializerJackson2} the attribute type stays {@code null}.
     */
    @Test
    void resolveTypeFromAssetDescriptorWhenTypeFieldAbsent() throws Exception {
        // "location" has no "type" field — type must come from Asset.LOCATION descriptor via context
        String json = """
            {"name":"Thing","realm":"smartcity","type":"ThingAsset","attributes":{
              "location":{"name":"location","value":{"type":"Point","coordinates":[0.0,0.0]}}}}""";

        Asset<?> asset = JSON.readValue(json, Asset.class);
        assertEquals(ThingAsset.class, asset.getClass());
        assertEquals(ValueType.GEO_JSON_POINT,
            asset.getAttributes().get("location").map(a -> a.getType()).orElse(null),
            "Type must be resolved from Asset.LOCATION descriptor, not from a 'type' field");
    }
}
