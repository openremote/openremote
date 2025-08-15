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
 * Represents a microservice or external service managed by the OpenRemote microservice registry.
 * 
 * This class encapsulates the essential information needed to register, manage, and track
 * the lifecycle of external services within the OpenRemote system. The registry maintains
 * information about service instances, their status, and lease management.
 */
public class Microservice {

    /**
     * The unique identifier of the service (e.g., "energy-service").
     * 
     * This ID is used to group related service instances and must be consistent
     * across all instances of the same service type.
     */
    @JsonProperty("serviceId")
    protected String serviceId;

    /**
     * Unique identifier for this specific instance within the scope of the serviceId.
     * 
     * Each running instance of a service must have a unique instanceId to distinguish
     * it from other instances of the same service type.
     */
    @JsonProperty(value = "instanceId")
    protected String instanceId;

    /**
     * Human-readable display name for the service (e.g., "Energy Service").
     * 
     * This label is used in user interfaces to provide a friendly name
     * for the service.
     */
    @JsonProperty("label")
    protected String label;

    /**
     * URL to the service's configuration page or user interface.
     * 
     * This URL provides access to the service's web interface for configuration
     * and management purposes (e.g., "https://openremote.app/services/energy-service/ui").
     */
    @JsonProperty("homepageUrl")
    protected String homepageUrl;

    /**
     * Current operational status of the service instance.
     * 
     * Indicates whether the service is available, unavailable, or in another state.
     * The status is managed by the microservice registry based on lease expiration.
     */
    @JsonProperty("status")
    protected MicroserviceStatus status;

    /**
     * The realm identifier where this service is registered.
     * 
     * Services are typically scoped to a specific realm. When set to MASTER_REALM
     * and registered by a super admin service user, the service becomes available
     * across all realms. This is used to indicate that the service is a global service.
     */
    @JsonProperty("realm")
    protected String realm;

    /**
     * Internal lease management information for the service registration.
     * 
     * Contains timestamps for lease expiration, registration, and renewal.
     * This information is used internally by the microservice registry to manage
     * service lifecycle and availability.
     */
    @JsonIgnore
    protected MicroserviceLeaseInfo leaseInfo;

    /**
     * Indicates whether this service is globally accessible across all realms.
     * 
     * Global services are available to all realms and typically use super admin
     * service users with system-wide access permissions.
     * 
     * This is set automatically by the microservice registry when the service is
     * registered by a super admin service user and set to MASTER_REALM.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    protected boolean isGlobal = false;

    public Microservice() {
    }

    public Microservice(String label, String serviceId, String homepageUrl, MicroserviceStatus status, String realm) {
        this.label = label;
        this.serviceId = serviceId;
        this.homepageUrl = homepageUrl;
        this.status = status;
        this.realm = realm;
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

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
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

    public boolean getIsGlobal() {
        return isGlobal;
    }

    public void setIsGlobal(boolean isGlobal) {
        this.isGlobal = isGlobal;
    }

    @Override
    public String toString() {
        return "Microservice{" +
                "label='" + label + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", homepageUrl='" + homepageUrl + '\'' +
                ", status='" + status + '\'' +
                ", realm='" + realm + '\'' +
                ", isGlobal='" + isGlobal + '\'' +
                '}';
    }
}
