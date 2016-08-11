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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import elemental.json.JsonObject;

public class RegistrationRequestV2 {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected EntityAttributeListV2 subject;
    @JsonProperty(value = "callback")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String providerUri;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected JsonObject metadata;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String duration;
    @JsonIgnore
    protected String registrationId;

    public RegistrationRequestV2(EntityAttributeListV2 subject, String providerUri, JsonObject metadata, String duration) {
        this.subject = subject;
        this.providerUri = providerUri;
        this.metadata = metadata;
        this.duration = duration;
    }

    public RegistrationRequestV2(String duration) {
        this.duration = duration;
    }

    public EntityAttributeListV2 getSubject() {
        return subject;
    }

    public String getProviderUri() {
        return providerUri;
    }

    public JsonObject getMetadata() {
        return metadata;
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
}
