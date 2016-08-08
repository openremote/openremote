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

import java.util.List;

public class ContextRegistrationV1 {
    @JsonProperty(value = "contextRegistrations")
    protected List<EntityAttributeRegistrationV1> registrations;
    @JsonInclude
    protected String duration;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String registrationId;

    public ContextRegistrationV1(List<EntityAttributeRegistrationV1> registrations, String duration) {
        this.registrations = registrations;
        this.duration = duration;
    }

    public ContextRegistrationV1(@JsonProperty("registrations") List<EntityAttributeRegistrationV1> registrations, @JsonProperty("duration") String duration, @JsonProperty("registrationId") String registrationId) {
        this.registrations = registrations;
        this.duration = duration;
        this.registrationId = registrationId;
    }

    public List<EntityAttributeRegistrationV1> getRegistrations() {
        return registrations;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    public void setRegistrations(List<EntityAttributeRegistrationV1> registrations) {
        this.registrations = registrations;
    }
}
