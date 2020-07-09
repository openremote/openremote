package org.openremote.agent.protocol.tradfri.util;

/**
 * The class that contains the credentials used to authenticate to the IKEA TRÅDFRI gateway
 */
public class Credentials {

    /**
     * The identity that can be used to authenticate to the IKEA TRÅDFRI gateway
     */
    private String identity;

    /**
     * The key that can be used to authenticate to the IKEA TRÅDFRI gateway
     */
    private String key;

    /**
     * Construct the Credentials class
     * @param identity The identity that can be used to authenticate to the IKEA TRÅDFRI gateway
     * @param key The key that can be used to authenticate to the IKEA TRÅDFRI gateway
     */
    public Credentials(String identity, String key) {
        this.identity = identity;
        this.key = key;
    }

    /**
     * Construct the Credentials class
     */
    public Credentials() {
    }

    /**
     * Get the identity that can be used to authenticate to the IKEA TRÅDFRI gateway
     * @return The identity that can be used to authenticate to the IKEA TRÅDFRI gateway
     */
    public String getIdentity() {
        return this.identity;
    }

    /**
     * Get the key that can be used to authenticate to the IKEA TRÅDFRI gateway
     * @return The key that can be used to authenticate to the IKEA TRÅDFRI gateway
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Set the identity that can be used to authenticate to the IKEA TRÅDFRI gateway
     * @param identity The new identity that can be used to authenticate to the IKEA TRÅDFRI gateway
     */
    public void setIdentity(String identity) {
        this.identity = identity;
    }

    /**
     * Set the key that can be used to authenticate to the IKEA TRÅDFRI gateway
     * @param key The new key that can be used to authenticate to the IKEA TRÅDFRI gateway
     */
    public void setKey(String key) {
        this.key = key;
    }
}
