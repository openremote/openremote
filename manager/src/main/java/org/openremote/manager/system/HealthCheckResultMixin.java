/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.manager.system;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import org.apache.camel.health.HealthCheck;

@JsonInclude(JsonInclude.Include.NON_NULL)
public interface HealthCheckResultMixin {

  class HealthCheckSerializer extends StdScalarSerializer<HealthCheck> {
    public HealthCheckSerializer() {
      super(HealthCheck.class, false);
    }

    @Override
    public void serialize(HealthCheck value, JsonGenerator g, SerializerProvider provider)
        throws IOException {
      g.writeString(value.getClass().getSimpleName());
    }
  }

  @JsonSerialize(using = HealthCheckSerializer.class)
  @JsonProperty("type")
  HealthCheck getCheck();

  @JsonProperty
  HealthCheck.State getState();

  @JsonProperty
  Optional<String> getMessage();

  @JsonProperty
  Optional<Throwable> getError();

  @JsonProperty
  Map<String, Object> getDetails();
}
