package org.openremote.agent.protocol.tradfri.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.agent.protocol.tradfri.util.ApiCode;

/**
 * The class that contains the payload for a request to authenticate to the IKEA TRÅDFRI gateway
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticateRequest {

    /**
     * The identity of the client to register to the IKEA TRÅDFRI gateway
     */
    @JsonProperty(ApiCode.IDENTITY)
    private String identity;

    /**
     * Construct the AuthenticateRequest class

     */
    public AuthenticateRequest(){
    }

    /**
     * Get the identity of the client to register to the IKEA TRÅDFRI gateway
     * @return The identity of the client to register to the IKEA TRÅDFRI gateway
     */
    public String getIdentity() {
        return this.identity;
    }

    /**
     * Set the identity of the client to register to the IKEA TRÅDFRI gateway
     * @param identity The new identity of the client to register to the IKEA TRÅDFRI gateway
     */
    public void setIdentity(String identity) {
        this.identity = identity;
    }
}
