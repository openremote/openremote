/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.shared.ngsi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ContextRegistrationV1Response {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String duration;
    @JsonInclude
    protected String registrationId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected StatusCode errorCode;

    public ContextRegistrationV1Response(@JsonProperty("duration")String duration, @JsonProperty("registrationId")String registrationId, @JsonProperty("errorCode")StatusCode errorCode) {
        this.duration = duration;
        this.errorCode = errorCode;
        this.registrationId = registrationId;
    }

    public String getDuration() {
        return duration;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public StatusCode getErrorCode() {
        return errorCode;
    }
}
