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
package org.openremote.model.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Represents a service's metadata and status information
 */
public class ServiceDescriptor {

    /**
     * The label of the service, e.g. "Energy Service"
     */
    @JsonProperty("label")
    protected String label;

    /**
     * The unique identifier of the service, e.g. "energy-service"
     */
    @JsonProperty("name")
    protected String name;

    /**
     * The URL of the service, e.g.
     * "https://test.openremote.app/services/energy-service"
     */
    @JsonProperty("url")
    protected String url;

    /**
     * The status of the service, e.g. "AVAILABLE"
     */
    @JsonProperty("status")
    protected ServiceStatus status;

    /**
     * Indicates whether the service supports multi-tenancy
     */
    @JsonProperty("multiTenancy")
    protected Boolean multiTenancy;

    /**
     * The client remote address, e.g. "192.168.1.100" (used internally)
     */
    @JsonIgnore
    protected String clientRemoteAddress;

    public ServiceDescriptor(String label, String name, String url, ServiceStatus status, Boolean multiTenancy) {
        this.label = label;
        this.name = name;
        this.url = url;
        this.status = status;
        this.multiTenancy = multiTenancy;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public String getClientRemoteAddress() {
        return clientRemoteAddress;
    }

    public void setClientRemoteAddress(String clientRemoteAddress) {
        this.clientRemoteAddress = clientRemoteAddress;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceStatus status) {
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
        return "ServiceDescriptor{" +
                "label='" + label + '\'' +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", status='" + status + '\'' +
                ", multiTenancy='" + multiTenancy + '\'' +
                '}';
    }
}
