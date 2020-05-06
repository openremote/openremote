package org.openremote.agent.protocol.tradfri.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.agent.protocol.tradfri.util.ApiCode;

/**
 * The class that contains the payload for a request to authenticate to the IKEA TRÅDFRI gateway
 * @author Stijn Groenen
 * @version 1.0.0
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
     * @since 1.0.0
     */
    public AuthenticateRequest(){
    }

    /**
     * Get the identity of the client to register to the IKEA TRÅDFRI gateway
     * @return The identity of the client to register to the IKEA TRÅDFRI gateway
     * @since 1.0.0
     */
    public String getIdentity() {
        return this.identity;
    }

    /**
     * Set the identity of the client to register to the IKEA TRÅDFRI gateway
     * @param identity The new identity of the client to register to the IKEA TRÅDFRI gateway
     * @since 1.0.0
     */
    public void setIdentity(String identity) {
        this.identity = identity;
    }
}
