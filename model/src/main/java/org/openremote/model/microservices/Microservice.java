/*
 * Copyright 2023, OpenRemote Inc.
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
 * Represents an external service's/microservice's metadata and status
 * information
 */
public class Microservice {

    /**
     * The label of the service, e.g. "Energy Service"
     */
    @JsonProperty("label")
    protected String label;

    /**
     * The unique identifier of the service, e.g. "energy-service"
     */
    @JsonProperty("serviceId")
    protected String serviceId;

    /**
     * The URL of the service's exposed Web UI, e.g.
     * "https://demo.openremote.app/services/energy-service/ui"
     */
    @JsonProperty("webUiUrl")
    protected String webUiUrl;

    /**
     * Indicates whether the service uses multi-tenancy for its configuration UI
     */
    @JsonProperty("multiTenancy")
    protected Boolean multiTenancy;

    /**
     * The status of the service, e.g. "AVAILABLE"
     */
    @JsonProperty("status")
    protected MicroserviceStatus status;

    public Microservice(String label, String serviceId, String webUiUrl,
                        MicroserviceStatus status, Boolean multiTenancy) {
        this.label = label;
        this.serviceId = serviceId;
        this.webUiUrl = webUiUrl;
        this.status = status;
        this.multiTenancy = multiTenancy;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getWebUiUrl() {
        return webUiUrl;
    }

    public void setWebUiUrl(String webUiUrl) {
        this.webUiUrl = webUiUrl;
    }

    public MicroserviceStatus getStatus() {
        return status;
    }

    public void setStatus(MicroserviceStatus status) {
        this.status = status;
    }

    public Boolean getMultiTenancy() {
        return multiTenancy;
    }

    public void setMultiTenancy(Boolean multiTenancy) {
        this.multiTenancy = multiTenancy;
    }

    @Override
    public String toString() {
        return "Microservice{" +
                "label='" + label + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", webUiUrl='" + webUiUrl + '\'' +
                ", status='" + status + '\'' +
                ", multiTenancy='" + multiTenancy + '\'' +
                '}';
    }
}
