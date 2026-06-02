package org.openremote.model.telematics.session;

import org.openremote.model.telematics.protocol.DeviceProtocol;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Manages active device sessions across all protocols.
 * <p>
 * The session manager:
 * <ul>
 *   <li>Creates and tracks sessions for connected devices</li>
 *   <li>Provides lookup by device ID</li>
 *   <li>Handles session cleanup (timeouts, disconnects)</li>
 *   <li>Publishes session lifecycle events</li>
 * </ul>
 * <p>
 * This is typically a singleton service that all protocol handlers share.
 */
public interface DeviceSessionManager {

    /**
     * Get or create a session for a device.
     * <p>
     * If a session already exists, it is returned. Otherwise, a new session
     * is created with the given protocol.
     *
     * @param deviceId The device identifier (IMEI)
     * @param protocol The protocol for this device
     * @param realm    The OpenRemote realm
     * @return The session (existing or new)
     */
    DeviceSession getOrCreate(String deviceId, DeviceProtocol protocol, String realm);

    /**
     * Get an existing session by device ID.
     *
     * @param deviceId The device identifier
     * @return The session if it exists
     */
    Optional<DeviceSession> get(String deviceId);

    /**
     * Get a session by the associated asset ID.
     *
     * @param assetId The asset ID
     * @return The session if found
     */
    Optional<DeviceSession> getByAssetId(String assetId);

    /**
     * Remove a session.
     *
     * @param deviceId The device identifier
     * @return The removed session, if it existed
     */
    Optional<DeviceSession> remove(String deviceId);

    /**
     * Stream all active sessions.
     *
     * @return Stream of all sessions
     */
    Stream<DeviceSession> all();

    /**
     * Stream sessions for a specific realm.
     *
     * @param realm The realm to filter by
     * @return Stream of sessions in that realm
     */
    Stream<DeviceSession> byRealm(String realm);

    /**
     * Stream sessions using a specific protocol.
     *
     * @param protocolId The protocol ID to filter by
     * @return Stream of sessions using that protocol
     */
    Stream<DeviceSession> byProtocol(String protocolId);

    /**
     * Get the count of active sessions.
     *
     * @return The number of active sessions
     */
    int getActiveSessionCount();

    /**
     * Clean up timed-out sessions.
     *
     * @param timeoutSeconds Sessions idle longer than this are removed
     * @return The number of sessions removed
     */
    int cleanupTimedOut(long timeoutSeconds);

    /**
     * Listener for session lifecycle events.
     */
    interface SessionListener {
        /**
         * Called when a new session is created.
         */
        void onSessionCreated(DeviceSession session);

        /**
         * Called when a session receives a message.
         */
        void onSessionMessage(DeviceSession session);

        /**
         * Called when a session is removed.
         */
        void onSessionRemoved(DeviceSession session);
    }

    /**
     * Register a session lifecycle listener.
     */
    void addListener(SessionListener listener);

    /**
     * Remove a session lifecycle listener.
     */
    void removeListener(SessionListener listener);
}
