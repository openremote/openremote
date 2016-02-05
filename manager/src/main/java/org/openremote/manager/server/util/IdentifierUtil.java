package org.openremote.manager.server.util;

import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.UUID;

/**
 * Utility functions for identifier value generation.
 */
public class IdentifierUtil {

    /**
     * A random 128 bit UUID in URL-safe 22 characters.
     */
    public static String generateGlobalUniqueId() {
        String encoded = Base64.getUrlEncoder().encodeToString(IdentifierUtil.toByteArray(UUID.randomUUID()));
        while (encoded.charAt(encoded.length() - 1) == '=')
            encoded = encoded.substring(0, encoded.length() - 1);
        return encoded;
    }

    /**
     * Generates a unique identifier that is the same every time this method is invoked on the same machine with
     * the same argument. This is a short (27 characters) URL-safe string.
     * <p>
     * This method combines the first non-loopback network interface's MAC address with given salt, then produces a
     * SHA-1 hash. In other words, every time you call this method with the same salt on the same
     * machine, you get the same identifier. If you use the same salt on a different machine, a different identifier
     * will be generated.
     * </p>
     *
     * @param salt An arbitrary string, longer values improve uniqueness.
     * @return A global unique identifier, stable for the current system and salt, with 48 bit + [salt bytes] uniqueness.
     */
    public static String generateSystemUniqueId(String salt) {
        return getEncodedHash(
            IdentifierUtil.getFirstNetworkInterfaceHardwareAddress(),
            salt.getBytes(Charset.forName("UTF-8"))
        );
    }

    /**
     * Generates a URL-safe, 27 character unpadded Base64 encoded SHA-1 hash of the given bytes.
     */
    public static String getEncodedHash(byte[]... bytes) {
        if (bytes == null)
            throw new IllegalArgumentException("Can't encode/hash null bytes");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            for (byte[] byteArray : bytes) {
                digest.update(byteArray);
            }
            byte[] hash = digest.digest();
            digest.reset();
            String encoded = Base64.getUrlEncoder().encodeToString(hash);
            while (encoded.charAt(encoded.length() - 1) == '=')
                encoded = encoded.substring(0, encoded.length() - 1);
            return encoded;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Gets the first non-loopback, "up" network interface's MAC address or throws an exception.
     */
    public static byte[] getFirstNetworkInterfaceHardwareAddress() {
        try {
            Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(interfaceEnumeration)) {
                if (!iface.isLoopback() && iface.isUp() && iface.getHardwareAddress() != null) {
                    return iface.getHardwareAddress();
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Could not discover first network interface hardware address", ex);
        }
        throw new RuntimeException("Could not discover first network interface hardware address");
    }

    /**
     * Returns the 16 bytes of the given UUID.
     */
    public static byte[] toByteArray(UUID uuid) {
        byte[] byteArray = new byte[(Long.SIZE / Byte.SIZE) * 2];
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        LongBuffer longBuffer = buffer.asLongBuffer();
        longBuffer.put(new long[]{uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()});
        return byteArray;
    }

}
