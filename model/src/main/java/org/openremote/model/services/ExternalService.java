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
package org.openremote.model.services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Represents an external service managed by the OpenRemote
 * external service registry.
 * <p>
 * This class encapsulates service metadata and the required information needed
 * to register, manage,
 * and track the lifecycle of external services within the OpenRemote system.
 */
public class ExternalService {

    /**
     * The unique identifier of the service (e.g., "energy-service").
     * <p>
     * This ID is used to group related service instances and must be consistent
     * across all instances of the same service type.
     */
    @Size(min = 3, max = 255, message = "{ExternalService.serviceId.Size}")
    @NotEmpty(message = "{ExternalService.serviceId.NotEmpty}")
    @JsonProperty("serviceId")
    protected String serviceId;

    /**
     * Identifier for this specific instance within the scope of the
     * serviceId.
     * <p>
     * This instanceId is an incremental integer assigned by the external service
     * registry when the service is registered.
     */
    @JsonProperty(value = "instanceId")
    protected int instanceId;

    /**
     * The version of the service.
     * <p>
     * Can be optionally set, to determine the version of the service, and interpret
     * which version of the service is running and registered.
     */
    @Size(min = 3, max = 255, message = "{ExternalService.version.Size}")
    @JsonProperty("version")
    protected String version;

    /**
     * The realm identifier where this service is registered.
     * <p>
     * Services are typically scoped to a specific realm. When
     * registered by a super admin service user, the realm must be MASTER, the service then becomes available
     * across all realms if the isGlobal flag is also set to true.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    protected String realm;

    /**
     * The icon to display for the service.
     * <p>
     * This icon is used in user interfaces to provide a visual representation
     * of the service. The default icon is "puzzle" which is a generic service icon.
     * <p>
     * Should be a valid icon name from the Material Design Icons library.
     */
    @JsonProperty("icon")
    protected String icon = "puzzle";

    /**
     * Human-readable display name for the service (e.g., "Energy Service").
     * <p>
     * This label is used in user interfaces to provide a friendly name
     * for the service.
     */
    @NotEmpty(message = "{ExternalService.label.NotEmpty}")
    @Size(min = 1, max = 255, message = "{ExternalService.label.Size}")
    @JsonProperty("label")
    protected String label;

    /**
     * URL to the service's configuration page or user interface.
     * <p>
     * This URL provides access to the service's web interface for configuration
     * and management purposes (e.g.,
     * "https://openremote.app/services/energy-service/ui").
     *
     */
    @Size(min = 3, max = 512, message = "{ExternalService.homepageUrl.Size}")
    @NotEmpty(message = "{ExternalService.homepageUrl.NotEmpty}")
    @JsonProperty("homepageUrl")
    protected String homepageUrl;

    /**
     * Current operational status of the service instance.
     * <p>
     * Indicates whether the service is available, unavailable, or in another state.
     * The status is managed by the external service registry based on lease expiration.
     */
    @NotNull(message = "{ExternalService.status.NotNull}")
    @JsonProperty("status")
    protected ExternalServiceStatus status;

    /**
     * Internal lease management information for the service registration.
     * <p>
     * Contains timestamps for lease expiration, registration, and renewal.
     * This information is used internally by the external service registry to manage
     * service lifecycle and availability.
     */
    @JsonIgnore
    protected ExternalServiceLeaseInfo leaseInfo;

    /**
     * Indicates whether this service is globally accessible across all realms.
     * <p>
     * Global services are available to all realms and typically use super admin
     * service users with system-wide access permissions.
     * <p>
     * This is set automatically by the external service registry when the service is
     * registered by a super admin service user and set to MASTER_REALM.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    protected boolean isGlobal = false;

    public ExternalService() {
    }

    // Minimal constructor
    public ExternalService(String serviceId, String realm, String label, String homepageUrl, ExternalServiceStatus status) {
        this.serviceId = serviceId;
        this.realm = realm;
        this.label = label;
        this.homepageUrl = homepageUrl;
        this.status = status;
    }

    public ExternalService(String serviceId, String label, String homepageUrl, ExternalServiceStatus status) {
        this.serviceId = serviceId;
        this.label = label;
        this.homepageUrl = homepageUrl;
        this.status = status;
    }

    // Getters and setters in property order
    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public int getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(int instanceId) {
        this.instanceId = instanceId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getHomepageUrl() {
        return homepageUrl;
    }

    public void setHomepageUrl(String homepageUrl) {
        this.homepageUrl = homepageUrl;
    }

    public ExternalServiceStatus getStatus() {
        return status;
    }

    public void setStatus(ExternalServiceStatus status) {
        this.status = status;
    }

    public ExternalServiceLeaseInfo getLeaseInfo() {
        return leaseInfo;
    }

    public void setLeaseInfo(ExternalServiceLeaseInfo leaseInfo) {
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
        return "ExternalService{" +
                "serviceId='" + serviceId + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", version='" + version + '\'' +
                ", realm='" + realm + '\'' +
                ", icon='" + icon + '\'' +
                ", label='" + label + '\'' +
                ", homepageUrl='" + homepageUrl + '\'' +
                ", status='" + status + '\'' +
                ", isGlobal='" + isGlobal + '\'' +
                '}';
    }
}
