//package org.openremote.model.telematics;
//
//import io.netty.buffer.ByteBuf;
//import org.openremote.model.asset.Asset;
//
///**
// * Interface defining a telematics protocol implementation.
// * <p>
// * Each vendor (e.g., Teltonika, Queclink) should implement this interface
// * to provide their specific protocol handling, including:
// * - Message parsing from binary or JSON formats
// * - Parameter registry for device attributes
// * - Asset type creation
// *
// * @param <M> The Message type used by this protocol
// * @param <A> The Asset type used by this protocol
// * @param <D> The ParsingValueDescriptor type used by this protocol's registry
// */
//public interface TelematicsProtocol<M extends Message, A extends TrackerAsset, D extends ParsingValueDescriptor<?>> {
//
//    /**
//     * Get the unique vendor identifier (e.g., "teltonika", "queclink").
//     *
//     * @return The vendor identifier string
//     */
//    String getVendorId();
//
//    /**
//     * Get a human-readable vendor name.
//     *
//     * @return The vendor display name
//     */
//    String getVendorName();
//
//    /**
//     * Get the protocol name or identifier.
//     *
//     * @return The protocol name
//     */
//    String getProtocolName();
//
//    /**
//     * Get the asset class used by this protocol.
//     *
//     * @return The asset class
//     */
//    Class<A> getAssetType();
//
//    /**
//     * Get the parameter registry for this protocol.
//     *
//     * @return The parameter registry
//     */
//    ParameterRegistry<D> getParameterRegistry();
//
//    /**
//     * Parse a binary message from the given ByteBuf.
//     *
//     * @param data The binary data to parse
//     * @return The parsed message, or null if parsing fails
//     */
//    M parseBinaryMessage(ByteBuf data);
//
//    /**
//     * Parse a JSON message from the given string.
//     *
//     * @param json The JSON string to parse
//     * @return The parsed message, or null if parsing fails
//     */
//    M parseJsonMessage(String json);
//
//    /**
//     * Create a new asset instance for this protocol.
//     *
//     * @return A new asset instance
//     */
//    A createAsset();
//
//    /**
//     * Check if this protocol supports MQTT communication.
//     *
//     * @return true if MQTT is supported
//     */
//    default boolean supportsMqtt() {
//        return false;
//    }
//
//    /**
//     * Check if this protocol supports TCP communication.
//     *
//     * @return true if TCP is supported
//     */
//    default boolean supportsTcp() {
//        return false;
//    }
//
//    /**
//     * Check if this protocol supports UDP communication.
//     *
//     * @return true if UDP is supported
//     */
//    default boolean supportsUdp() {
//        return false;
//    }
//}
