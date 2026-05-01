package org.openremote.model.telematics.session;

import org.openremote.model.telematics.core.DeviceMessage;
import org.openremote.model.telematics.protocol.DeviceCommand;
import org.openremote.model.telematics.protocol.DeviceProtocol;

import java.time.Instant;
import java.util.Optional;

/**
 * Represents an active session with a telematics device.
 * <p>
 * A session tracks:
 * <ul>
 *   <li>Device identification (IMEI)</li>
 *   <li>Connection state and timing</li>
 *   <li>The protocol being used</li>
 * </ul>
 * <p>
 * Sessions are created when a device first connects and persist
 * until explicitly removed or timed out.
 */
public interface DeviceSession {

    /**
     * Connection states for a device session.
     */
    enum State {
        CONNECTING,
        CONNECTED,
        IDLE,
        DISCONNECTED
    }

    /**
     * The device identifier (typically IMEI).
     */
    String getDeviceId();

    /**
     * The OpenRemote asset ID associated with this device.
     */
    Optional<String> getAssetId();

    /**
     * Set the asset ID for this session.
     */
    void setAssetId(String assetId);

    /**
     * The protocol handling this device's communication.
     */
    DeviceProtocol getProtocol();

    /**
     * Current connection state.
     */
    State getState();

    /**
     * Time of the last received message.
     */
    Instant getLastSeen();

    /**
     * Time when this session was created.
     */
    Instant getCreatedAt();

    /**
     * The OpenRemote realm this device belongs to.
     */
    String getRealm();

    /**
     * Called when a message is received from the device.
     * Updates last seen time.
     *
     * @param message The decoded message (list of Attributes)
     */
    void onMessage(DeviceMessage message);

    /**
     * Called when the device connects.
     */
    void onConnect();

    /**
     * Called when the device disconnects.
     */
    void onDisconnect();

    /**
     * Queue a command to send to the device.
     *
     * @param command The command to send
     * @return true if the command was queued successfully
     */
    boolean queueCommand(DeviceCommand command);

    /**
     * Get the number of messages received in this session.
     */
    long getMessageCount();

    /**
     * Check if this session has timed out.
     *
     * @param timeoutSeconds The timeout threshold in seconds
     * @return true if the session has timed out
     */
    default boolean isTimedOut(long timeoutSeconds) {
        return getLastSeen().plusSeconds(timeoutSeconds).isBefore(Instant.now());
    }
}
