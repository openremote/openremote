package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;

/**
 * To be used as a wrapper class for when creating the ConfigRelayStatus message.
 */
public final class ConfigProxyStatus extends ConfigStatusMessage {

    private static final String TAG = ConfigProxyStatus.class.getSimpleName();
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_GATT_PROXY_STATUS;
    private int mProxyState;

    /**
     * Constructs a ConfigRelayStatus message.
     *
     * @param message Access message received
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigProxyStatus(final AccessMessage message) {
        super(message);
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    final void parseStatusParameters() {
        final byte[] payload = ((AccessMessage) mMessage).getAccessPdu();
        mProxyState = payload[2];
    }


    /**
     * Returns the current ConfigProxySet.ProxyState of the node
     */
    /* @ConfigProxySet.ProxyState */
    public int getProxyState() {
        return mProxyState;
    }

    /**
     * Returns true if the proxy feature is currently enabled on the node and false otherwise
     */
    public boolean isProxyFeatureEnabled() {
        return mProxyState == ConfigProxySet.PROXY_FEATURE_ENABLED;
    }
}

