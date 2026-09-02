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
package org.openremote.model.datapoint.query;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class AssetDatapointQueryLocalDateTimeDeserializerJackson2
    extends StdDeserializer<LocalDateTime> {

  public AssetDatapointQueryLocalDateTimeDeserializerJackson2() {
    super(LocalDateTime.class);
  }

  @Override
  public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    if (p.currentToken() == JsonToken.VALUE_NULL) {
      return null;
    }
    if (p.currentToken() != JsonToken.VALUE_STRING) {
      return (LocalDateTime) ctxt.handleUnexpectedToken(LocalDateTime.class, p);
    }

    try {
      return AssetDatapointQueryLocalDateTimeDeserializer.parse(p.getText());
    } catch (DateTimeParseException ex) {
      throw InvalidFormatException.from(
          p,
          AssetDatapointQueryLocalDateTimeDeserializer.EXPECTED_FORMAT_MESSAGE,
          p.getText(),
          LocalDateTime.class);
    }
  }
}
