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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A microservice in the context of OpenRemote is an external service that is
 * managed by the microservice registry.
 * 
 * It is used to register, deregister and manage the lifecycle of the service.
 * 
 * The microservice registry is used to keep track of the microservices and
 * their
 * instances.
 */
public class Microservice {

    /**
     * The unique identifier of the service, e.g. "energy-service"
     */
    @JsonProperty("serviceId")
    protected String serviceId;

    /**
     * Unique identifier of the instance, in scope of the serviceId
     */
    @JsonProperty(value = "instanceId")
    protected String instanceId;

    /**
     * The label of the service, e.g. "Energy Service" for display purposes
     */
    @JsonProperty("label")
    protected String label;

    /**
     * The URL of the service's configuration page/UI, e.g.
     * "https://openremote.app/services/energy-service/ui"
     */
    @JsonProperty("homepageUrl")
    protected String homepageUrl;

    /**
     * The active status of the service, e.g. "AVAILABLE"
     */
    @JsonProperty("status")
    protected MicroserviceStatus status;

    /**
     * The lease info of the service, contains timestamps for lease expiration,
     * registration and renewal. Used internally by the microservice registry.
     */
    @JsonIgnore
    protected MicroserviceLeaseInfo leaseInfo;

    public Microservice() {
    }

    public Microservice(String label, String serviceId, String homepageUrl, MicroserviceStatus status) {
        this.label = label;
        this.serviceId = serviceId;
        this.homepageUrl = homepageUrl;
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

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
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

    public MicroserviceLeaseInfo getLeaseInfo() {
        return leaseInfo;
    }

    public void setLeaseInfo(MicroserviceLeaseInfo leaseInfo) {
        this.leaseInfo = leaseInfo;
    }

    @Override
    public String toString() {
        return "Microservice{" +
                "label='" + label + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", homepageUrl='" + homepageUrl + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
