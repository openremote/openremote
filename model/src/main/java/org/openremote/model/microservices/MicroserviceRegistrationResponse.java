package org.openremote.model.microservices;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MicroserviceRegistrationResponse {

    @JsonProperty("serviceId")
    private String serviceId;

    @JsonProperty("instanceId")
    private String instanceId;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public MicroserviceRegistrationResponse(String serviceId, String instanceId) {
        this.serviceId = serviceId;
        this.instanceId = instanceId;
    }

    @Override
    public String toString() {
        return "MicroserviceRegistrationResponse [instanceId=" + instanceId + "]";
    }
}
