package org.openremote.agent.protocol.bluetooth.mesh;

/**
 * Internal provisioning callbacks
 */
public interface InternalProvisioningCallbacks {

    /**
     * Generates confirmation inputs based on the public key xy components of provisioner and provisionee
     *
     * @param provisionerPublicKeyXY provisioner public key xy
     * @param provisioneePublicKeyXY provisionee publick key xy
     */
    byte[] generateConfirmationInputs(final byte[] provisionerPublicKeyXY, final byte[] provisioneePublicKeyXY);

}
