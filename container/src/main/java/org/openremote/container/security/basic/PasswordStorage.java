/**
 * Copyright (c) 2016, Taylor Hornby
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openremote.container.security.basic;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.DAYS;

/**
 * https://github.com/defuse/password-hashing
 * <p>
 * TODO: On ARM32 this is very slow, even with only 500 iterations it takes ~2 seconds on an RPI3 to hash a password, so we cache the result
 * TODO: Remove the cache if ARM64 is fast enough and increase iterations
 */
public class PasswordStorage {

    private static final Logger LOG = Logger.getLogger(PasswordStorage.class.getName());

    static class CacheKey {
        final public char[] password;
        final public byte[] salt;
        final public int iterations;
        final public int bytes;

        public CacheKey(char[] password, byte[] salt, int iterations, int bytes) {
            this.password = password;
            this.salt = salt;
            this.iterations = iterations;
            this.bytes = bytes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey cacheKey = (CacheKey) o;

            if (iterations != cacheKey.iterations) return false;
            if (bytes != cacheKey.bytes) return false;
            if (!Arrays.equals(password, cacheKey.password)) return false;
            return Arrays.equals(salt, cacheKey.salt);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(password);
            result = 31 * result + Arrays.hashCode(salt);
            result = 31 * result + iterations;
            result = 31 * result + bytes;
            return result;
        }
    }

    static final protected LoadingCache<CacheKey, byte[]> hashedPasswordCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(7, DAYS)
        .build(new CacheLoader<CacheKey, byte[]>() {
            @Override
            public byte[] load(CacheKey key) throws Exception {
                try {
                    LOG.fine("Cache miss, hashing password...") ;
                    PBEKeySpec spec = new PBEKeySpec(key.password, key.salt, key.iterations, key.bytes * 8);
                    SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
                    return skf.generateSecret(spec).getEncoded();
                } catch (NoSuchAlgorithmException ex) {
                    throw new CannotPerformOperationException(
                        "Hash algorithm not supported.",
                        ex
                    );
                } catch (InvalidKeySpecException ex) {
                    throw new CannotPerformOperationException(
                        "Invalid key spec.",
                        ex
                    );
                }
            }
        });

    @SuppressWarnings("serial")
    static public class InvalidHashException extends RuntimeException {
        public InvalidHashException(String message) {
            super(message);
        }

        public InvalidHashException(String message, Throwable source) {
            super(message, source);
        }
    }

    @SuppressWarnings("serial")
    static public class CannotPerformOperationException extends RuntimeException {
        public CannotPerformOperationException(String message) {
            super(message);
        }

        public CannotPerformOperationException(String message, Throwable source) {
            super(message, source);
        }
    }

    public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";

    // These constants may be changed without breaking existing hashes.
    public static final int SALT_BYTE_SIZE = 24;
    public static final int HASH_BYTE_SIZE = 18;
    public static final int PBKDF2_ITERATIONS = 500; // TODO Try to increase this to >1000 on ARM64 if fast enough

    // These constants define the encoding and may not be changed.
    public static final int HASH_SECTIONS = 5;
    public static final int HASH_ALGORITHM_INDEX = 0;
    public static final int ITERATION_INDEX = 1;
    public static final int HASH_SIZE_INDEX = 2;
    public static final int SALT_INDEX = 3;
    public static final int PBKDF2_INDEX = 4;

    public static String createHash(String password)
        throws CannotPerformOperationException {
        return createHash(password.toCharArray());
    }

    public static String createHash(char[] password)
        throws CannotPerformOperationException {
        // Generate a random salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_BYTE_SIZE];
        random.nextBytes(salt);

        // Hash the password
        byte[] hash = pbkdf2(password, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE);
        int hashSize = hash.length;

        // format: algorithm:iterations:hashSize:salt:hash
        String parts = "sha1:" +
            PBKDF2_ITERATIONS +
            ":" + hashSize +
            ":" +
            toBase64(salt) +
            ":" +
            toBase64(hash);
        return parts;
    }

    public static boolean verifyPassword(String password, String correctHash)
        throws CannotPerformOperationException, InvalidHashException {
        return verifyPassword(password.toCharArray(), correctHash);
    }

    public static boolean verifyPassword(char[] password, String correctHash)
        throws CannotPerformOperationException, InvalidHashException {
        // Decode the hash into its parameters
        String[] params = correctHash.split(":");
        if (params.length != HASH_SECTIONS) {
            throw new InvalidHashException(
                "Fields are missing from the password hash."
            );
        }

        // Currently, Java only supports SHA1.
        if (!params[HASH_ALGORITHM_INDEX].equals("sha1")) {
            throw new CannotPerformOperationException(
                "Unsupported hash type."
            );
        }

        int iterations = 0;
        try {
            iterations = Integer.parseInt(params[ITERATION_INDEX]);
        } catch (NumberFormatException ex) {
            throw new InvalidHashException(
                "Could not parse the iteration count as an integer.",
                ex
            );
        }

        if (iterations < 1) {
            throw new InvalidHashException(
                "Invalid number of iterations. Must be >= 1."
            );
        }


        byte[] salt = null;
        try {
            salt = fromBase64(params[SALT_INDEX]);
        } catch (IllegalArgumentException ex) {
            throw new InvalidHashException(
                "Base64 decoding of salt failed.",
                ex
            );
        }

        byte[] hash = null;
        try {
            hash = fromBase64(params[PBKDF2_INDEX]);
        } catch (IllegalArgumentException ex) {
            throw new InvalidHashException(
                "Base64 decoding of pbkdf2 output failed.",
                ex
            );
        }


        int storedHashSize = 0;
        try {
            storedHashSize = Integer.parseInt(params[HASH_SIZE_INDEX]);
        } catch (NumberFormatException ex) {
            throw new InvalidHashException(
                "Could not parse the hash size as an integer.",
                ex
            );
        }

        if (storedHashSize != hash.length) {
            throw new InvalidHashException(
                "Hash length doesn't match stored hash length."
            );
        }

        // Compute the hash of the provided password, using the same salt, 
        // iteration count, and hash length
        byte[] testHash = pbkdf2(password, salt, iterations, hash.length);
        // Compare the hashes in constant time. The password is correct if
        // both hashes match.
        return slowEquals(hash, testHash);
    }

    private static boolean slowEquals(byte[] a, byte[] b) {
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++)
            diff |= a[i] ^ b[i];
        return diff == 0;
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bytes) {
        try {
            return hashedPasswordCache.get(
                new CacheKey(password, salt, iterations, bytes)
            );
        } catch (ExecutionException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }

    private static byte[] fromBase64(String hex) {
        return Base64.getDecoder().decode(hex);
    }

    private static String toBase64(byte[] array) {
        return Base64.getEncoder().encodeToString(array);
    }

}