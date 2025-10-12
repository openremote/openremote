/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.protocol.openweathermap;

import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.openremote.model.asset.agent.AgentLink;

public class OpenWeatherMapAgentLink extends AgentLink<OpenWeatherMapAgentLink> {

    @JsonPropertyDescription("The OpenWeatherMap weather field to use as a data source for the attribute")
    protected OpenWeatherMapField field;

    // For Hydrators
    protected OpenWeatherMapAgentLink() {
    }

    public OpenWeatherMapAgentLink(String id) {
        super(id);
    }

    public Optional<OpenWeatherMapField> getField() {
        return Optional.ofNullable(field);
    }

    public OpenWeatherMapAgentLink setField(OpenWeatherMapField field) {
        this.field = field;
        return this;
    }

}