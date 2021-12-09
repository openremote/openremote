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

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.AESLightEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.modes.CCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.openremote.agent.protocol.bluetooth.mesh.SecureNetworkBeacon;
import org.openremote.model.syslog.SyslogCategory;

import javax.validation.constraints.NotNull;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;


public class SecureUtils {

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, SecureUtils.class);

    /**
     * Used to calculate the confirmation key
     */
    public static final byte[] PRCK = "prck".getBytes(Charset.forName("US-ASCII"));

    /**
     * Used to calculate the session key
     */
    public static final byte[] PRSK = "prsk".getBytes(Charset.forName("US-ASCII"));
    /**
     * Used to calculate the session nonce
     */
    public static final byte[] PRSN = "prsn".getBytes(Charset.forName("US-ASCII"));
    /**
     * Used to calculate the device key
     */
    public static final byte[] PRDK = "prdk".getBytes(Charset.forName("US-ASCII"));

    /**
     * K2 Master input
     */
    public static final byte[] K2_MASTER_INPUT = {0x00};
    /**
     * Salt input for K2
     */
    public static final byte[] SMK2 = "smk2".getBytes(Charset.forName("US-ASCII"));
    /**
     * Salt input for K3
     */
    public static final byte[] SMK3 = "smk3".getBytes(Charset.forName("US-ASCII"));
    /**
     * Input for K3 data
     */
    public static final byte[] SMK3_DATA = "id64".getBytes(Charset.forName("US-ASCII"));
    /**
     * Salt input for K4
     */
    public static final byte[] SMK4 = "smk4".getBytes(Charset.forName("US-ASCII"));
    /**
     * Input for K4 data
     */
    public static final byte[] SMK4_DATA = "id6".getBytes(Charset.forName("US-ASCII"));
    /**
     * Output mask for K4
     */
    public static final int ENC_K4_OUTPUT_MASK = 0x3f;
    //For S1, the key is constant
    protected static final byte[] SALT_KEY = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    //Padding for the random nonce
    protected static final byte[] NONCE_PADDING = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final String TAG = SecureUtils.class.getSimpleName();
    /**
     * Salt input for identity key
     */
    private static final byte[] NKIK = "nkik".getBytes(Charset.forName("US-ASCII"));

    /**
     * Salt input for beacon key
     */
    private static final byte[] NKBK = "nkbk".getBytes(Charset.forName("US-ASCII"));
    /**
     * Salt input for identity key
     */
    private static final byte[] ID128 = "id128".getBytes(Charset.forName("US-ASCII"));
    //Padding for the random nonce
    private static final byte[] HASH_PADDING = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final int HASH_LENGTH = 8;
    public static int NRF_MESH_KEY_SIZE = 16;

    public static byte[] generateRandomNumber() {
        final SecureRandom random = new SecureRandom();
        final byte[] randomBytes = new byte[16];
        random.nextBytes(randomBytes);

        return randomBytes;
    }

    public static String generateRandomNetworkKey() {
        final byte[] networkKey = generateRandomNumber();
        return MeshParserUtils.bytesToHex(networkKey, false);
    }

    public static String generateRandomApplicationKey() {
        return MeshParserUtils.bytesToHex(generateRandomNumber(), false);
    }

    public static byte[] calculateSalt(final byte[] data) {
        return calculateCMAC(data, SALT_KEY);
    }

    public static byte[] calculateCMAC(final byte[] data, final byte[] key) {
        final byte[] cmac = new byte[16];

        CipherParameters cipherParameters = new KeyParameter(key);
        BlockCipher blockCipher = new AESEngine();
        CMac mac = new CMac(blockCipher);

        mac.init(cipherParameters);
        mac.update(data, 0, data.length);
        mac.doFinal(cmac, 0);
        return cmac;
    }

    public static byte[] encryptCCM(@NotNull final byte[] data,
                                    @NotNull final byte[] key,
                                    @NotNull final byte[] nonce,
                                    final int micSize) {
        Objects.requireNonNull(data);
        Objects.requireNonNull(key);
        Objects.requireNonNull(nonce);

        final byte[] ccm = new byte[data.length + micSize];

        final CCMBlockCipher ccmBlockCipher = new CCMBlockCipher(new AESEngine());
        final AEADParameters aeadParameters = new AEADParameters(new KeyParameter(key), micSize * 8, nonce);
        ccmBlockCipher.init(true, aeadParameters);
        ccmBlockCipher.processBytes(data, 0, data.length, ccm, data.length);
        try {
            ccmBlockCipher.doFinal(ccm, 0);
            return ccm;
        } catch (InvalidCipherTextException e) {
            LOG.log(Level.SEVERE, "Error while encrypting: " + e.getMessage(), e);
            return null;
        }
    }

    public static byte[] encryptCCM(@NotNull final byte[] data,
                                    @NotNull final byte[] key,
                                    @NotNull final byte[] nonce,
                                    @NotNull final byte[] additionalData,
                                    final int micSize) {
        Objects.requireNonNull(data);
        Objects.requireNonNull(key);
        Objects.requireNonNull(nonce);
        Objects.requireNonNull(additionalData);

        final byte[] ccm = new byte[data.length + micSize];

        final CCMBlockCipher ccmBlockCipher = new CCMBlockCipher(new AESEngine());
        final AEADParameters aeadParameters = new AEADParameters(new KeyParameter(key), micSize * 8, nonce, additionalData);
        ccmBlockCipher.init(true, aeadParameters);
        ccmBlockCipher.processBytes(data, 0, data.length, ccm, data.length);
        try {
            ccmBlockCipher.doFinal(ccm, 0);
            return ccm;
        } catch (InvalidCipherTextException e) {
            LOG.log(Level.SEVERE, "Error while encrypting: " + e.getMessage(), e);
            return null;
        }
    }

    public static byte[] decryptCCM(@NotNull final byte[] data,
                                    @NotNull final byte[] key,
                                    @NotNull final byte[] nonce,
                                    final int micSize) throws InvalidCipherTextException {
        Objects.requireNonNull(data);
        Objects.requireNonNull(key);
        Objects.requireNonNull(nonce);

        final byte[] ccm = new byte[data.length - micSize];

        final CCMBlockCipher ccmBlockCipher = new CCMBlockCipher(new AESEngine());
        final AEADParameters aeadParameters = new AEADParameters(new KeyParameter(key), micSize * 8, nonce);
        ccmBlockCipher.init(false, aeadParameters);
        ccmBlockCipher.processBytes(data, 0, data.length, ccm, 0);
        ccmBlockCipher.doFinal(ccm, 0);
        return ccm;
    }

    public static byte[] decryptCCM(@NotNull final byte[] data,
                                    @NotNull final byte[] key,
                                    @NotNull final byte[] nonce,
                                    @NotNull final byte[] additionalData,
                                    final int micSize) throws InvalidCipherTextException {
        Objects.requireNonNull(data);
        Objects.requireNonNull(key);
        Objects.requireNonNull(nonce);
        Objects.requireNonNull(additionalData);

        final byte[] ccm = new byte[data.length - micSize];

        final CCMBlockCipher ccmBlockCipher = new CCMBlockCipher(new AESEngine());
        final AEADParameters aeadParameters = new AEADParameters(new KeyParameter(key), micSize * 8, nonce, additionalData);
        ccmBlockCipher.init(false, aeadParameters);
        ccmBlockCipher.processBytes(data, 0, data.length, ccm, 0);
        ccmBlockCipher.doFinal(ccm, 0);
        return ccm;
    }

    public static byte[] calculateK1(final byte[] ecdh, final byte[] confirmationSalt, final byte[] text) {
        return calculateCMAC(text, calculateCMAC(ecdh, confirmationSalt));
    }

    /**
     * Calculate k2
     *
     * @param data network key
     * @param p    master input
     */
    public static K2Output calculateK2(final byte[] data, final byte[] p) {
        if (data == null || p == null)
            return null;

        final byte[] salt = calculateSalt(SMK2);
        final byte[] t = calculateCMAC(data, salt);

        final byte[] t0 = {};
        final ByteBuffer inputBufferT0 = ByteBuffer.allocate(t0.length + p.length + 1);
        inputBufferT0.put(t0);
        inputBufferT0.put(p);
        inputBufferT0.put((byte) 0x01);
        final byte[] t1 = calculateCMAC(inputBufferT0.array(), t);
        final byte nid = (byte) (t1[15] & 0x7F);

        final ByteBuffer inputBufferT1 = ByteBuffer.allocate(t1.length + p.length + 1);
        inputBufferT1.put(t1);
        inputBufferT1.put(p);
        inputBufferT1.put((byte) 0x02);
        final byte[] encryptionKey = calculateCMAC(inputBufferT1.array(), t);

        final ByteBuffer inputBufferT2 = ByteBuffer.allocate(encryptionKey.length + p.length + 1);
        inputBufferT2.put(encryptionKey);
        inputBufferT2.put(p);
        inputBufferT2.put((byte) 0x03);
        final byte[] privacyKey = calculateCMAC(inputBufferT2.array(), t);

        return new K2Output(nid, encryptionKey, privacyKey);
    }

    /**
     * Calculate k3
     *
     * @param n network key
     */
    public static byte[] calculateK3(final byte[] n) {
        if (n == null)
            return null;

        final byte[] salt = calculateSalt(SMK3);

        final byte[] t = calculateCMAC(n, salt);

        ByteBuffer buffer = ByteBuffer.allocate(SMK3_DATA.length + 1);
        buffer.put(SMK3_DATA);
        buffer.put((byte) 0x01);
        final byte[] cmacInput = buffer.array();

        final byte[] result = calculateCMAC(cmacInput, t);

        //Only the least significant 8 bytes are returned
        final byte[] networkId = new byte[8];
        final int srcOffset = result.length - networkId.length;

        System.arraycopy(result, srcOffset, networkId, 0, networkId.length);
        return networkId;
    }

    /**
     * Calculate k4
     *
     * @param n key
     */
    public static byte calculateK4(final byte[] n) {
        if (n == null || n.length != 16)
            throw new IllegalArgumentException("Key cannot be empty and must be 16-bytes long.");

        byte[] salt = calculateSalt(SMK4);

        final byte[] t = calculateCMAC(n, salt);

        ByteBuffer buffer = ByteBuffer.allocate(SMK4_DATA.length + 1);
        buffer.put(SMK4_DATA);
        buffer.put((byte) 0x01);
        final byte[] cmacInput = buffer.array();

        final byte[] result = calculateCMAC(cmacInput, t);

        //Only the least siginificant 6 bytes are returned
        return (byte) ((result[15]) & 0x3F);
    }

    /**
     * Calculates the identity key
     *
     * @param n network key
     * @return hash value
     */
    public static byte[] calculateIdentityKey(final byte[] n) {
        if (n == null)
            return null;
        final byte[] salt = calculateSalt(NKIK);
        ByteBuffer buffer = ByteBuffer.allocate(ID128.length + 1);
        buffer.put(ID128);
        buffer.put((byte) 0x01);
        final byte[] p = buffer.array();
        return calculateK1(n, salt, p);
    }

    /**
     * Calculates the beacon key
     *
     * @param n network key
     * @return hash value
     */
    public static byte[] calculateBeaconKey(final byte[] n) {
        final byte[] salt = calculateSalt(NKBK);
        ByteBuffer buffer = ByteBuffer.allocate(ID128.length + 1);
        buffer.put(ID128);
        buffer.put((byte) 0x01);
        final byte[] p = buffer.array();
        return calculateK1(n, salt, p);
    }

    /**
     * Calculates the authentication value of secure network beacon
     *
     * @param n         network key
     * @param flags     flags
     * @param networkId network id of the network
     * @param ivIndex   ivindex of the network
     */
    public static byte[] calculateAuthValueSecureNetBeacon(final byte[] n,
                                                           final int flags,
                                                           final byte[] networkId,
                                                           final int ivIndex) {
        final int inputLength = 1 + networkId.length + 4;
        final ByteBuffer pBuffer = ByteBuffer.allocate(inputLength);
        pBuffer.put((byte) flags);
        pBuffer.put(networkId);
        pBuffer.putInt(ivIndex);
        final byte[] beaconKey = calculateBeaconKey(n);
        return calculateCMAC(pBuffer.array(), beaconKey);
    }

    /**
     * Creates the secure network beacon
     *
     * @param n         network key
     * @param flags     network flags, this represents the current state of hte network if key refresh/iv update is ongoing or complete
     * @param networkId unique id of the network
     * @param ivIndex   iv index of the network
     */
    public static SecureNetworkBeacon createSecureNetworkBeacon(final byte[] n,
                                                                final int flags,
                                                                final byte[] networkId,
                                                                final int ivIndex) {
        final byte[] authentication = calculateAuthValueSecureNetBeacon(n, flags, networkId, ivIndex);

        final int inputLength = 1 + networkId.length + 4;
        final ByteBuffer pBuffer = ByteBuffer.allocate(inputLength);
        pBuffer.put((byte) flags);
        pBuffer.put(networkId);
        pBuffer.putInt(ivIndex);
        final ByteBuffer secNetBeaconBuffer = ByteBuffer.allocate(1 + inputLength + 8);
        secNetBeaconBuffer.put((byte) 0x01);
        secNetBeaconBuffer.put(pBuffer.array());
        secNetBeaconBuffer.put(authentication, 0, 8);
        return new SecureNetworkBeacon(secNetBeaconBuffer.array());
    }

    /**
     * Calculates the secure network beacon
     *
     * @param n         network key
     * @param flags     network flags, this represents the current state of hte network if key refresh/iv update is ongoing or complete
     * @param networkId unique id of the network
     * @param ivIndex   iv index of the network
     */
    public static byte[] calculateSecureNetworkBeacon(final byte[] n,
                                                      final int beaconType,
                                                      final int flags,
                                                      final byte[] networkId,
                                                      final int ivIndex) {
        final byte[] authentication = calculateAuthValueSecureNetBeacon(n, flags, networkId, ivIndex);

        final int inputLength = 1 + networkId.length + 4;
        final ByteBuffer pBuffer = ByteBuffer.allocate(inputLength);
        pBuffer.put((byte) flags);
        pBuffer.put(networkId);
        pBuffer.putInt(ivIndex);
        final ByteBuffer secNetBeaconBuffer = ByteBuffer.allocate(1 + inputLength + 8);
        secNetBeaconBuffer.put((byte) beaconType);
        secNetBeaconBuffer.put(pBuffer.array());
        secNetBeaconBuffer.put(authentication, 0, 8);
        return secNetBeaconBuffer.array();
    }

    /**
     * Calculates hash value for advertising with node id
     *
     * @param identityKey resolving identity key
     * @param random      64-bit random value
     * @param src         unicast address of the node
     * @return hash value
     */
    public static byte[] calculateHash(final byte[] identityKey, final byte[] random, final byte[] src) {
        final int length = HASH_PADDING.length + random.length + src.length;
        final ByteBuffer bufferHashInput = ByteBuffer.allocate(length).order(ByteOrder.BIG_ENDIAN);
        bufferHashInput.put(HASH_PADDING);
        bufferHashInput.put(random);
        bufferHashInput.put(src);
        final byte[] hashInput = bufferHashInput.array();
        final byte[] hash = SecureUtils.encryptWithAES(hashInput, identityKey);

        final ByteBuffer buffer = ByteBuffer.allocate(HASH_LENGTH).order(ByteOrder.BIG_ENDIAN);
        buffer.put(hash, 8, HASH_LENGTH);

        return buffer.array();
    }

    public static byte[] encryptWithAES(final byte[] data, final byte[] key) {
        final byte[] encrypted = new byte[data.length];
        final CipherParameters cipherParameters = new KeyParameter(key);
        final AESLightEngine engine = new AESLightEngine();
        engine.init(true, cipherParameters);
        engine.processBlock(data, 0, encrypted, 0);

        return encrypted;
    }

    public static int getNetMicLength(final int ctl) {
        if (ctl == 0) {
            return 4; //length;
        } else {
            return 8; //length
        }
    }

    /**
     * Gets the transport MIC length based on the aszmic value
     *
     * @param aszmic application size message integrity check
     */
    public static int getTransMicLength(final int aszmic) {
        if (aszmic == 0) {
            return 4; //length;
        } else {
            return 8; //length
        }
    }

    public static class K2Output {
        private byte nid;
        private byte[] encryptionKey;
        private byte[] privacyKey;

        private K2Output(final byte nid, final byte[] encryptionKey, final byte[] privacyKey) {
            this.nid = nid;
            this.encryptionKey = encryptionKey;
            this.privacyKey = privacyKey;
        }

        public byte getNid() {
            return nid;
        }

        public byte[] getEncryptionKey() {
            return encryptionKey;
        }

        public byte[] getPrivacyKey() {
            return privacyKey;
        }
    }
}
