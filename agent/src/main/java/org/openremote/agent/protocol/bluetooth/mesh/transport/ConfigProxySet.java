package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * To be used as a wrapper class to create a ConfigProxySet message.
 */
public class ConfigProxySet extends ConfigMessage {

    public static final int PROXY_FEATURE_DISABLED = 0x00;
    public static final int PROXY_FEATURE_ENABLED = 0x01;
    private static final String TAG = ConfigProxySet.class.getSimpleName();
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_GATT_PROXY_SET;
    private final int proxyState;

    /**
     * Constructs ConfigNodeReset message.
     *
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigProxySet(/* @ProxyState */ final int proxyState) throws IllegalArgumentException {
        if (proxyState != PROXY_FEATURE_DISABLED && proxyState != PROXY_FEATURE_ENABLED)
            throw new IllegalArgumentException("Invalid proxy state value.");
        this.proxyState = proxyState;
        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    void assembleMessageParameters() {
        mParameters = new byte[]{(byte) proxyState};
    }

    // @Retention(RetentionPolicy.SOURCE)
    // @IntDef({PROXY_FEATURE_DISABLED, PROXY_FEATURE_ENABLED})
    // public @interface ProxyState {
    // }
}

