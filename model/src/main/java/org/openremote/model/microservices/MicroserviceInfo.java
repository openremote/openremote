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
package org.openremote.model.microservices;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A view model representation of a {@link Microservice}
 * Excluding any internal details such as IP address, port, etc.
 */
public class MicroserviceInfo {

    @JsonProperty("serviceId")
    private final String serviceId;

    @JsonProperty("label")
    private final String label;

    @JsonProperty("homepageUrl")
    private final String homepageUrl;

    @JsonProperty("status")
    private final MicroserviceStatus status;

    @JsonProperty("multiTenancy")
    private final Boolean multiTenancy;

    public MicroserviceInfo(String serviceId, String label, String homepageUrl, MicroserviceStatus status, Boolean multiTenancy) {
        this.serviceId = serviceId;
        this.label = label;
        this.homepageUrl = homepageUrl;
        this.status = status;
        this.multiTenancy = multiTenancy;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getLabel() {
        return label;
    }

    public String getHomepageUrl() {
        return homepageUrl;
    }

    public MicroserviceStatus getStatus() {
        return status;
    }

    public Boolean getMultiTenancy() {
        return multiTenancy;
    }

    public static MicroserviceInfo fromMicroservice(Microservice microservice) {
        return new MicroserviceInfo(
                microservice.getServiceId(),
                microservice.getLabel(),
                microservice.getHomepageUrl(),
                microservice.getStatus(),
                microservice.getMultiTenancy()
        );
    }

    @Override
    public String toString() {
        return "MicroserviceInfo{" +
                "serviceId='" + serviceId + '\'' +
                ", label='" + label + '\'' +
                ", homepageUrl='" + homepageUrl + '\'' +
                ", status=" + status +
                ", multiTenancy=" + multiTenancy +
                '}';
    }
}
