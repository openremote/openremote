/*
 * Copyright 2021, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.agent.protocol.bluetooth.mesh.utils;

import javax.validation.constraints.NotNull;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Logger;

import org.openremote.agent.protocol.bluetooth.mesh.NodeKey;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class MeshParserUtils {

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, MeshParserUtils.class);

    private static final String TAG = MeshParserUtils.class.getSimpleName();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
    private static final String PATTERN_KEY = "[0-9a-fA-F]{32}";
    private static final String PATTERN_UUID_HEX = "[0-9a-fA-F]{32}";

    private static final int PROHIBITED_DEFAULT_TTL_STATE_MIN = 0x01;
    private static final int PROHIBITED_DEFAULT_TTL_STATE_MID = 0x80;
    private static final int PROHIBITED_DEFAULT_TTL_STATE_MAX = 0xFF;
    public static final int USE_DEFAULT_TTL = 0xFF;

    private static final int MIN_TTL = 0x00;
    private static final int MAX_TTL = 0x7F;
    private static final int PROHIBITED_PUBLISH_TTL_MIN = 0x80;
    private static final int PROHIBITED_PUBLISH_TTL_MAX = 0xFE;

    private static final int IV_ADDRESS_MIN = 0;
    private static final int IV_ADDRESS_MAX = 4096;
    private static final int UNICAST_ADDRESS_MIN = 0;
    private static final char[] HEX_ARRAY = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final byte[] ALPHANUMERIC = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
        'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

    private static final int[] M_NUMERIC_MAX = {0, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};

    public static final int RESOLUTION_100_MS = 0b00;
    public static final int RESOLUTION_1_S = 0b01;
    public static final int RESOLUTION_10_S = 0b10;
    public static final int RESOLUTION_10_M = 0b11;

    public static final int GENERIC_ON_OFF_5_MS = 5;

    public static String bytesToHex(final byte[] bytes, final boolean add0x) {
        if (bytes == null)
            return "";
        return bytesToHex(bytes, 0, bytes.length, add0x);
    }

    public static String bytesToHex(final byte[] bytes, final int start, final int length, final boolean add0x) {
        if (bytes == null || bytes.length <= start || length <= 0)
            return "";

        final int maxLength = Math.min(length, bytes.length - start);
        final char[] hexChars = new char[maxLength * 2];
        for (int j = 0; j < maxLength; j++) {
            final int v = bytes[start + j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        if (!add0x)
            return new String(hexChars);
        return "0x" + new String(hexChars);
    }

    public static byte[] toByteArray(String hexString) {
        int len = hexString.length();
        byte[] bytes = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                + Character.digit(hexString.charAt(i + 1), 16));
        }
        return bytes;
    }

    private static boolean isValidKeyIndex(final Integer value) {
        return value == null || value != (value & 0xFFF);
    }

    /**
     * Checks if the IV Index is valid.
     *
     * <p>
     * The IV Index is a 32-bit value that is a shared network resource
     * (i.e., all nodes in a mesh network share the same value of the IV Index
     * and use it for all subnets they belong to). The IV Index starts at 0x00000000
     * </p>
     *
     * @param ivIndex IV Index value
     */
    private static boolean isValidIvIndex(@NotNull final Integer ivIndex) {
        Objects.requireNonNull(ivIndex);
        return ivIndex == 0 || ivIndex > 0;
    }

    public static byte parseUpdateFlags(final int keyRefreshFlag, final int ivUpdateFlag) {
        byte flags = 0;
        if (keyRefreshFlag == 1) {
            flags |= 0b01;
        } else {
            flags &= ~0b01;
        }

        if (ivUpdateFlag == 1) {
            flags |= 0b10;
        } else {
            flags &= ~0b01;
        }
        return flags;
    }

    public static int getBitValue(final int value, final int position) {
        return (value >> position) & 1;
    }

    public static byte[] addKeyIndexPadding(final Integer keyIndex) {
        return ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort((short) (keyIndex & 0x0FFF)).array();
    }

    public static int removeKeyIndexPadding(final byte[] keyIndex) {
        return keyIndex[0] & 0x0F | keyIndex[1];
    }

    /**
     * Validates the network key input
     *
     * @param networkKey Network Key input
     * @return true if the Network Key is a valid value
     * @throws IllegalArgumentException in case of an invalid was entered as an input and the message containing the error
     */
    public static boolean validateNetworkKeyInput(@NotNull final String networkKey) throws IllegalArgumentException {

        if (TextUtil.isNullOrEmpty(networkKey)) {
            throw new IllegalArgumentException("Network key cannot be empty!");
        } else if (!networkKey.matches(PATTERN_KEY)) {
            throw new IllegalArgumentException("Network key must be 16 bytes long!");
        }

        return true;
    }

    /**
     * Validates the Key Index input
     *
     * @param input   Key Index input
     * @return true if the Key Index is a valid value
     * @throws IllegalArgumentException in case of an invalid was entered as an input and the message containing the error
     */
    public static boolean validateKeyIndexInput(final String input) throws IllegalArgumentException {

        if (TextUtil.isNullOrEmpty(input)) {
            throw new IllegalArgumentException("Key index cannot be empty!");
        }

        final int keyIndex;

        try {
            keyIndex = Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Key index must be 0 to 4095!");
        }
        if (isValidKeyIndex(keyIndex)) {
            throw new IllegalArgumentException("Key index must be 0 to 4095!");
        }

        return true;
    }

    /**
     * Validates the IV Index input
     *
     * @param input   IV Index input
     * @return true if the the value is valid
     * @throws IllegalArgumentException in case of an invalid was entered as an input and the message containing the error
     */
    public static boolean validateIvIndexInput(final String input) throws IllegalArgumentException {

        if (TextUtil.isNullOrEmpty(input)) {
            throw new IllegalArgumentException("IV Index cannot be empty!");
        }

        final int ivIndex;
        try {
            ivIndex = Integer.parseInt(input, 16);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("IV Index must be 0 to 4294967295!");
        }

        if (!isValidIvIndex(ivIndex)) {
            throw new IllegalArgumentException("IV Index must be 0 to 4294967295!");
        }

        return true;
    }

    /**
     * Validates the IV Index input
     *
     * @param ivIndex IV Index input
     * @return true if the the value is valid
     * @throws IllegalArgumentException in case of an invalid was entered as an input and the message containing the error
     */
    public static boolean validateIvIndexInput(final Integer ivIndex) throws IllegalArgumentException {
        if (ivIndex == null) {
            throw new IllegalArgumentException("IV Index cannot be empty!");
        }

        if (!isValidIvIndex(ivIndex)) {
            throw new IllegalArgumentException("IV Index must be 0 to 4294967295!");
        }

        return true;
    }

    /**
     * Validates the key input
     *
     * @param key key
     * @return true if the Key is a valid value
     * @throws IllegalArgumentException in case of an invalid was entered as an input and the message containing the error
     */
    public static boolean validateKeyInput(@NotNull final String key) throws IllegalArgumentException {
        if (TextUtil.isNullOrEmpty(key)) {
            throw new IllegalArgumentException("Key cannot be empty!");
        } else if (!key.matches(PATTERN_KEY)) {
            throw new IllegalArgumentException("key must be a 32-character hexadecimal string!");
        }

        return true;
    }

    public static boolean isValidSequenceNumber(final int sequenceNumber) {

        boolean flag = sequenceNumber == (sequenceNumber & 0xFFFFFF);

        if (sequenceNumber == 0xFFFFFF) {
            flag = false;
        }
        return flag;
    }

    public static byte[] getSequenceNumberBytes(int sequenceNumber) {
        if (MeshParserUtils.isValidSequenceNumber(sequenceNumber)) {
            return new byte[]{(byte) ((sequenceNumber >> 16) & 0xFF), (byte) ((sequenceNumber >> 8) & 0xFF), (byte) (sequenceNumber & 0xFF)};
        }
        return null;
    }

    public static int convert24BitsToInt(@NotNull final byte[] byteArray) {
        Objects.requireNonNull(byteArray);
        if (byteArray.length != 3)
            throw new IllegalArgumentException("Invalid length, byte array must be 3-bytes long.");
        return (((byteArray[0] & 0xFF) << 16) | ((byteArray[1] & 0xFF) << 8) | (byteArray[2] & 0xFF));
    }

    public static int getSequenceNumberFromPDU(final byte[] pdu) {
        return convert24BitsToInt(new byte[]{pdu[3], pdu[4], pdu[5]}); // get sequence number array from pdu
    }

    public static int calculateSeqZero(final byte[] sequenceNumber) {
        return ((sequenceNumber[1] & 0x1F) << 8) | (sequenceNumber[2] & 0xFF); // 13 least significant bits
    }

    public static byte[] getSrcAddress(final byte[] pdu) {
        return ByteBuffer.allocate(2).put(pdu, 6, 2).array(); // get src address from pdu
    }

    public static byte[] getDstAddress(final byte[] pdu) {
        return ByteBuffer.allocate(2).put(pdu, 8, 2).array(); // get mDst address from pdu
    }

    /*
    private static int getSegmentedMessageLength(final SparseArray<byte[]> segmentedMessageMap) {
        int length = 0;
        for (int i = 0; i < segmentedMessageMap.size(); i++) {
            length += segmentedMessageMap.get(i).length;
        }
        return length;
    }
    */
    private static int getSegmentedMessageLength(final Map<Integer, byte[]> segmentedMessages) {
        int length = 0;
        for (byte[] curSegment : segmentedMessages.values()) {
            length += curSegment.length;
        }
        return length;
    }

    /*
    public static byte[] concatenateSegmentedMessages(final SparseArray<byte[]> segmentedMessages) {
        final int length = getSegmentedMessageLength(segmentedMessages);
        final ByteBuffer completeBuffer = ByteBuffer.allocate(length);
        completeBuffer.order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < segmentedMessages.size(); i++) {
            completeBuffer.put(segmentedMessages.get(i));
        }
        return completeBuffer.array();
    }
    */

    public static byte[] concatenateSegmentedMessages(final Map<Integer, byte[]> segmentedMessages) {
        final int length = getSegmentedMessageLength(segmentedMessages);
        final ByteBuffer completeBuffer = ByteBuffer.allocate(length);
        completeBuffer.order(ByteOrder.BIG_ENDIAN);
        //for (int i = 0; i < segmentedMessages.size(); i++) {
        //    completeBuffer.put(segmentedMessages.get(i));
        //}
        TreeMap<Integer, byte[]> sortedMap = new TreeMap<>(segmentedMessages);
        for (Integer index : sortedMap.keySet()) {
            completeBuffer.put(sortedMap.get(index));
        }
        return completeBuffer.array();
    }

    /**
     * Returns the opcode within the access payload
     *
     * @param accessPayload payload
     * @return array of opcodes
     */
    public static int getOpCode(final byte[] accessPayload, final int opCodeCount) {
        switch (opCodeCount) {
            case 1:
                return accessPayload[0];
            case 2:
                return unsignedBytesToInt(accessPayload[1], accessPayload[0]);
            default:
                return unsignedByteToInt(accessPayload[1]) << 8
                    | unsignedByteToInt(accessPayload[0]) << 16
                    | unsignedByteToInt(accessPayload[2]);
        }
    }

    /**
     * Returns an array of opcodes.
     * If the MSB = 0 then the length is 1
     * If the MSB = 1 then the length is 2
     * If the MSB = 2 then the length is 3
     *
     * @param opCode operation code
     */
    public static byte[] getOpCode(final int opCode) {
        if (opCode < 0x80) {
            return new byte[]{(byte) (opCode & 0xFF)};
        } else if (opCode < 0x4000 || (opCode & 0xFFFC00) == 0x8000) {
            return new byte[]{(byte) (0x80 | ((opCode >> 8) & 0x3F)), (byte) (opCode & 0xFF)};
        } else {
            return new byte[]{(byte) (0xC0 | ((opCode >> 16) & 0x3F)),
                (byte) ((opCode >> 8) & 0xFF),
                (byte) (opCode & 0xFF)};
        }
    }

    /**
     * Returns the length of the opcode.
     * If the MSB = 0 then the length is 1
     * If the MSB = 1 then the length is 2
     * If the MSB = 2 then the length is 3
     *
     * @param mostSignificantOpcodeByte operation code
     * @return length of opcodes
     */
    public static int getOpCodeLength(final int mostSignificantOpcodeByte) {
        if (mostSignificantOpcodeByte < 0x80) {
            return 1;
        } else if (/*mostSignificantOpcodeByte >= 0x80 &&*/ mostSignificantOpcodeByte <= 0xBF) {
            return 2;
        } else {
            return 3;
        }
    }

    /**
     * Returns the vendor opcode packed with company identifier
     * If the MSB = 0 then the length is 1
     * If the MSB = 1 then the length is 2
     * If the MSB = 2 then the length is 3
     *
     * @param opCode operation code
     * @return length of opcodes
     */
    public static byte[] createVendorOpCode(final int opCode, final int companyIdentifier) {
        final byte[] opCodes = new byte[3];
        opCodes[0] = (byte) (opCode | 0xC0);
        opCodes[1] = (byte) (companyIdentifier & 0xFF);
        opCodes[2] = (byte) ((companyIdentifier >> 8) & 0xFF);
        return opCodes;
    }

    /**
     * Checks if the opcode is valid
     *
     * @param opCode opCode of mesh message
     * @return if the opcode is valid
     */
    public static boolean isValidOpcode(final int opCode) throws IllegalArgumentException {
        if (opCode != (opCode & 0xFFFFFF))
            throw new IllegalArgumentException("Invalid opcode, opcode must be 1-3 octets");

        return true;
    }

    /**
     * Checks if the parameters are within the valid range
     *
     * @param parameters opCode of mesh message
     * @return if the opcode is valid
     */
    public static boolean isValidParameters(final byte[] parameters) throws IllegalArgumentException {
        if (parameters != null && parameters.length > 379)
            throw new IllegalArgumentException("Invalid parameters, parameters must be 0-379 octets");

        return true;
    }

    /**
     * Checks if the Publication ttl value is within the allowed range where the range is 0x00 - 0x7F.
     *
     * @param ttl ttl
     * @return true if valid and false otherwise
     */
    public static boolean isValidTtl(final int ttl) {
        return ttl >= MIN_TTL && ttl <= MAX_TTL;
    }

    /**
     * Checks if the ttl value is within the allowed range.
     *
     * <p>
     * 0x00, 0x02 - 0x7F are the Default TTL states.
     * 0x01, 0x80 and 0xFF are Prohibited.
     * </p>
     *
     * @param ttl ttl
     * @return true if valid and false otherwise
     */
    public static boolean isValidDefaultTtl(final int ttl) {
        return (ttl == MIN_TTL || (ttl >= 0x02 && ttl <= MAX_TTL));
    }

    /**
     * Checks if the default publish ttl is used for publication
     *
     * @param publishTtl publish ttl
     * @return true if valid and false otherwise
     */
    public static boolean isDefaultPublishTtl(final int publishTtl) {
        return publishTtl == USE_DEFAULT_TTL;
    }

    /**
     * Checks if the retransmit count is within the allowed range
     *
     * @param retransmitCount publish ttl
     * @return true if valid and false otherwise
     */
    public static boolean validateRetransmitCount(final int retransmitCount) {
        return retransmitCount == (retransmitCount & 0b111);
    }

    /**
     * Checks if the publish retransmit interval steps is within the allowed range
     *
     * @param intervalSteps publish ttl
     * @return true if valid and false otherwise
     */
    public static boolean validatePublishRetransmitIntervalSteps(final int intervalSteps) {
        return intervalSteps == (intervalSteps & 0b11111);
    }

    /**
     * Returns the remaining time as a string
     *
     * @param remainingTime remaining time that for the transition to finish
     * @return remaining time as string.
     */
    public static String getRemainingTime(final int remainingTime) {
        final int stepResolution = remainingTime >> 6;
        final int numberOfSteps = remainingTime & 0x3F;
        switch (stepResolution) {
            case RESOLUTION_100_MS:
                return (numberOfSteps * 100) + " milliseconds";
            case RESOLUTION_1_S:
                return numberOfSteps + " seconds";
            case RESOLUTION_10_S:
                return (numberOfSteps * 10) + " seconds";
            case RESOLUTION_10_M:
                return (numberOfSteps * 10) + " minutes";
            default:
                return "Unknown";
        }
    }

    /**
     * Returns the remaining time as a string
     *
     * @return remaining time as string.
     */
    public static String getRemainingTransitionTime(final int stepResolution, final int numberOfSteps) {
        switch (stepResolution) {
            case RESOLUTION_100_MS:
                return (numberOfSteps * 100) + " ms";
            case RESOLUTION_1_S:
                return numberOfSteps + " s";
            case RESOLUTION_10_S:
                return (numberOfSteps * 10) + " s";
            case RESOLUTION_10_M:
                return (numberOfSteps * 10) + " min.";
            default:
                return "Unknown";
        }
    }

    /**
     * Returns the remaining time in milliseconds
     *
     * @param resolution time resolution
     * @param steps      number of steps
     * @return time in milliseconds
     */
    public static int getRemainingTime(final int resolution, final int steps) {
        switch (resolution) {
            case RESOLUTION_100_MS:
                return (steps * 100);
            case RESOLUTION_1_S:
                return steps * 1000;
            case RESOLUTION_10_S:
                return (steps * 10) * 1000;
            case RESOLUTION_10_M:
                return (steps * 10) * 1000 * 60;
        }
        return 0;
    }

    public static int getValue(final byte[] bytes) {
        if (bytes == null || bytes.length != 2)
            return 0;
        return unsignedToSigned(unsignedBytesToInt(bytes[0], bytes[1]), 16);
    }

    /**
     * Convert a signed byte to an unsigned int.
     */
    public static int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    /**
     * Convert signed bytes to a 16-bit unsigned int.
     */
    public static int unsignedBytesToInt(byte b0, byte b1) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8));
    }

    public static int bytesToInt(@NotNull byte[] b) {
        Objects.requireNonNull(b);
        return b.length == 4 ? ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getInt() : ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getShort();
    }

    public static int hexToInt(String hex) {
        return MeshParserUtils.bytesToInt(MeshParserUtils.toByteArray(hex));
    }

    public static byte[] intToBytes(int i) {
        return ByteBuffer.allocate(4).putInt(i).array();
    }

    /**
     * Convert an unsigned integer value to a two's-complement encoded signed value.
     */
    private static int unsignedToSigned(int unsigned, int size) {
        if ((unsigned & (1 << size - 1)) != 0) {
            unsigned = -1 * ((1 << size - 1) - (unsigned & ((1 << size - 1) - 1)));
        }
        return unsigned;
    }

    /**
     * Returns if the model id is a vendor model
     *
     * @param modelId model identifier
     */
    public static boolean isVendorModel(final int modelId) {
        return modelId < Short.MIN_VALUE || modelId > Short.MAX_VALUE;
    }

    public static int getCompanyIdentifier(final int modelId) {
        if (modelId >= Short.MIN_VALUE && modelId <= Short.MAX_VALUE) {
            throw new IllegalArgumentException("Not a valid vendor model ID");
        }
        final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(modelId);
        return buffer.getShort(0);
    }

    /**
     * Generates an oob count to be entered on the device when blink,beep,vibrate,push or twist action is selected
     *
     * <p>
     * Based on mesh profile specification v1.0.1 section 5.4.2.4 page 254
     * </p>
     *
     * @param oobSize oob size
     */
    static byte[] generateOOBCount(final int oobSize) {
        final Random random = new Random();
        final int bound = (int) Math.pow(10, oobSize) - 1;
        final byte randomByte = (byte) (random.nextInt(bound) + 1);
        LOG.info("Random OOB count: " + randomByte);
        return new byte[]{randomByte};
    }

    /**
     * Generates a random number to be entered on the device
     *
     * <p>
     * Based on mesh profile specification v1.0.1 section 5.4.2.4 page 254
     * </p>
     *
     * @param oobSize oob size
     */
    static byte[] generateOOBNumeric(final int oobSize) {
        final Random random = new Random();
        final int value = random.nextInt((int) Math.pow(10, oobSize));
        LOG.info("Random OOB numeric: " + value);
        return intToBytes(value);
    }

    /**
     * Generates a random alphanumeric code to be entered on the device
     *
     * <p>
     * Based on mesh profile specification v1.0.1 section 5.4.2.4 page 254
     * </p>
     *
     * @param oobSize oob size
     */
    static byte[] generateOOBAlphaNumeric(final int oobSize) {
        final Random random = new Random();
        final byte[] value = new byte[oobSize];
        for (int i = 0; i < oobSize; i++) {
            final int index = random.nextInt(ALPHANUMERIC.length);
            value[i] = ALPHANUMERIC[index];
        }
        LOG.info("Random OOB alpha numeric: " + new String(value));

        return value;
    }

    /**
     * Returns UUID as a hex
     *
     * @param uuid UUID
     */
    public static String uuidToHex(@NotNull final UUID uuid) {
        Objects.requireNonNull(uuid);
        return uuid.toString().replace("-", "").toUpperCase(Locale.US);
    }

    /**
     * Returns UUID as a hex
     *
     * @param uuid UUID
     */
    public static String uuidToHex(@NotNull final String uuid) {
        Objects.requireNonNull(uuid);
        return uuid.replace("-", "").toUpperCase(Locale.US);
    }

    /**
     * Returns UUID in bytes
     *
     * @param uuid UUID
     */
    public static byte[] uuidToBytes(@NotNull final UUID uuid) {
        Objects.requireNonNull(uuid);
        return toByteArray(uuid.toString().replace("-", ""));
    }

    /**
     * Formats a hex string without dashes to a uuid string
     *
     * @param uuidHex Hex string
     */
    public static String formatUuid(@NotNull final String uuidHex) {
        Objects.requireNonNull(uuidHex);
        if (isUuidPattern(uuidHex)) {
            return new StringBuffer(uuidHex)
                .insert(8, "-")
                .insert(13, "-")
                .insert(18, "-")
                .insert(23, "-")
                .toString()
                .toUpperCase(Locale.US);
        }
        return uuidHex;
    }

    public static boolean isUuidPattern(@NotNull final String uuidHex) {
        Objects.requireNonNull(uuidHex);
        return uuidHex.matches(PATTERN_UUID_HEX);
    }

    /**
     * Returns a type4 UUID from a uuid string without dashes
     *
     * @param uuidHex Hex string
     */
    public static UUID getUuid(@NotNull final String uuidHex) {
        Objects.requireNonNull(uuidHex);
        if (uuidHex.matches(PATTERN_UUID_HEX))
            return UUID.fromString(new StringBuffer(uuidHex)
                .insert(8, "-")
                .insert(4, "-")
                .insert(4, "-")
                .insert(4, "-")
                .toString());

        return null;
    }

    /**
     * Formats the timestamp
     *
     * @param timestamp timestamp
     */
    public static String formatTimeStamp(final long timestamp) {
        return SDF.format(new Date(timestamp));
    }

    public static boolean isNodeKeyExists(@NotNull final List<NodeKey> keys, final int index) {
        Objects.requireNonNull(keys);
        for (NodeKey key : keys) {
            if (key.getIndex() == index) {
                return true;
            }
        }
        return false;
    }

    public static NodeKey getNodeKey(@NotNull final List<NodeKey> keys, final int index) {
        Objects.requireNonNull(keys);
        for (NodeKey key : keys) {
            if (key.getIndex() == index) {
                return key;
            }
        }
        return null;
    }

    /**
     * Validates heart beat count log.
     *
     * @param countLog Heartbeat publication count.
     * @return true if valid.
     * @throws IllegalArgumentException if the countLog value is not a value from 0 to 17 or 255
     */
    public static boolean isValidHeartbeatCountLog(final int countLog) {
        if ((countLog >= 0x00 && countLog < 0x12) || countLog == 0xFF)
            return true;
        throw new IllegalArgumentException("Count log must be 0 to 17 or 255!");
    }

    /**
     * Validates heart beat publication ttl.
     *
     * @param ttl Heartbeat publication ttl.
     * @return true if valid or false otherwise.
     */
    public static boolean isValidHeartbeatPublicationTtl(final int ttl) {
        return ttl <= 0x7F;
    }

}