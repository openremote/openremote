package org.openremote.model.telematics.protocol;

import io.netty.buffer.ByteBuf;
import org.openremote.model.telematics.core.DeviceMessage;
import org.openremote.model.telematics.parameter.TelematicsParameterRegistry;

import java.util.List;
import java.util.Optional;

/**
 * Protocol handler for a specific telematics device manufacturer/protocol.
 * <p>
 * This interface abstracts the protocol-specific logic for:
 * <ul>
 *   <li>Decoding binary/JSON data into {@link DeviceMessage} (a list of Attributes)</li>
 *   <li>Encoding commands to send back to devices</li>
 *   <li>Protocol handshake and acknowledgment</li>
 * </ul>
 * <p>
 * Each manufacturer (Teltonika, Queclink, etc.) implements this interface
 * to handle their specific protocol format.
 * <p>
 * The output is always a list of {@link DeviceMessage}, where each message
 * contains standard OpenRemote {@link org.openremote.model.attribute.Attribute} objects.
 */
public interface DeviceProtocol {

    /**
     * The unique identifier for this protocol (e.g., "teltonika", "queclink").
     */
    String getProtocolId();

    /**
     * The vendor/manufacturer name for display purposes.
     */
    String getVendorName();

    /**
     * Get the parameter registry for this protocol.
     */
    TelematicsParameterRegistry<?> getParameterRegistry();

    /**
     * Determines if this protocol can handle the given data.
     * <p>
     * Used for protocol auto-detection. Should examine the data without consuming it.
     *
     * @param data    The raw data to examine
     * @param context Context about the message source
     * @return true if this protocol can handle the data
     */
    boolean canHandle(ByteBuf data, MessageContext context);

    /**
     * Decode binary data into DeviceMessages.
     * <p>
     * A single packet may contain multiple records, so this returns a list.
     * Each DeviceMessage contains:
     * <ul>
     *   <li>Device ID (IMEI)</li>
     *   <li>Protocol name</li>
     *   <li>List of Attributes (location, speed, IO elements, etc.)</li>
     * </ul>
     *
     * @param data    The data to decode
     * @param context Context about the message source (may include cached device ID)
     * @return List of decoded messages (may be empty but never null)
     * @throws ProtocolDecodeException If the data is malformed
     */
    List<DeviceMessage> decode(ByteBuf data, MessageContext context) throws ProtocolDecodeException;

    /**
     * Encode a command to send to a device.
     *
     * @param command The command to encode
     * @param context Context about the target device
     * @return The encoded command as bytes, or empty if command not supported
     */
    Optional<ByteBuf> encodeCommand(DeviceCommand command, MessageContext context) throws ProtocolEncodeException;

    /**
     * Get the acknowledgment to send after receiving data.
     *
     * @param messageCount The number of messages/records received
     * @param context      Context about the connection
     * @return The acknowledgment bytes, or empty if no ack is needed
     */
    Optional<ByteBuf> getAcknowledgment(int messageCount, MessageContext context);

    /**
     * Parse device identification from an initial handshake.
     * <p>
     * Some protocols (like Teltonika TCP) send an IMEI before data.
     *
     * @param data    The identification data
     * @param context Context to update with device ID
     * @return true if identification was successful
     */
    default boolean handleIdentification(ByteBuf data, MessageContext context) {
        return false;
    }

    /**
     * Get the response to send after identification.
     *
     * @param accepted Whether the device is accepted
     * @param context  The message context
     * @return The response bytes, or empty if no response needed
     */
    default Optional<ByteBuf> getIdentificationResponse(boolean accepted, MessageContext context) {
        return Optional.empty();
    }
}
