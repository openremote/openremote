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
package org.openremote.container.json

import com.fasterxml.jackson.databind.ObjectMapper
import org.openremote.model.asset.Asset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.ValueType
import spock.lang.Specification

/**
 * Reproduces what RESTEasy's Jackson 2 provider (the only JSON {@link
 * jakarta.ws.rs.ext.MessageBodyReader}/ {@link jakarta.ws.rs.ext.MessageBodyWriter} on the
 * classpath) does when the manager's own REST API (e.g. {@code POST /api/asset}) round trips an
 * {@link Asset}.
 */
class AssetJackson2Test extends Specification {

  static final ObjectMapper JSON = Jackson2Config.configureObjectMapper(new ObjectMapper())

  def setupSpec() {
    if (ValueUtil.getValueDescriptor("text").isEmpty()) {
      ValueUtil.initialise(null)
    }
  }

  /**
   * Verifies that {@link org.openremote.container.json.Jackson2Config}'s {@link
   * org.openremote.model.jackson.AssetDeserializerJackson2} sets the asset type info context so
   * {@link org.openremote.model.jackson.AttributeDeserializerJackson2} can resolve a value type
   * from the asset descriptor even when the JSON omits the {@code "type"} field entirely. Without
   * {@code AssetDeserializerJackson2} the attribute type stays {@code null}.
   */
  def "resolves type from asset descriptor when type field is absent"() {
    given: "'location' has no 'type' field - type must come from Asset.LOCATION descriptor via context"
    def json = '''
        {"name":"Thing","realm":"smartcity","type":"ThingAsset","attributes":{
          "location":{"name":"location","value":{"type":"Point","coordinates":[0.0,0.0]}}}}'''

    when:
    def asset = JSON.readValue(json, Asset)

    then:
    asset.class == ThingAsset
    // Type must be resolved from the Asset.LOCATION descriptor, not from a 'type' field
    asset.getAttributes().get("location").map { it.type }.orElse(null) == ValueType.GEO_JSON_POINT
  }
}
