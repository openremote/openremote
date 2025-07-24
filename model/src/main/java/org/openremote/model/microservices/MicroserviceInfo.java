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
