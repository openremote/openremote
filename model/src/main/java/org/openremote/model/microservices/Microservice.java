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
 * Holds comprehensive details about a microservice/external service
 * This object is used by the {@link MicroserviceResource} to register,
 * deregister services
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
     * The IP address of the service, e.g. "192.168.1.100"
     */
    @JsonProperty("ipAddress")
    protected String ipAddress;

    /**
     * The port of the service, e.g. "8080"
     */
    @JsonProperty("port")
    protected Integer port;

    /**
     * The URL of the service's homepage which provides the user interface, e.g.
     * "https://openremote.app/services/energy-service/ui"
     */
    @JsonProperty("homepageUrl")
    protected String homepageUrl;

    /**
     * The status of the service, e.g. "AVAILABLE"
     */
    @JsonProperty("status")
    protected MicroserviceStatus status;

    public Microservice(String label, String serviceId, String homepageUrl, MicroserviceStatus status, String ipAddress,
            Integer port) {
        this.label = label;
        this.serviceId = serviceId;
        this.homepageUrl = homepageUrl;
        this.ipAddress = ipAddress;
        this.port = port;
        this.status = status;
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

    public String getHomepageUrl() {
        return homepageUrl;
    }

    public void setHomepageUrl(String homepageUrl) {
        this.homepageUrl = homepageUrl;
    }

    public MicroserviceStatus getStatus() {
        return status;
    }

    public void setStatus(MicroserviceStatus status) {
        this.status = status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "Microservice{" +
                "label='" + label + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", port='" + port + '\'' +
                ", homepageUrl='" + homepageUrl + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
