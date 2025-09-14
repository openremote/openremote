/*
 * Copyright 2024, OpenRemote Inc.
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

package org.openremote.manager.security;

import org.openremote.agent.protocol.mqtt.CustomKeyManagerFactory;
import org.openremote.agent.protocol.mqtt.CustomX509TrustManagerFactory;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.model.Container;
import org.openremote.model.security.KeyStoreService;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Optional;

import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD;
import static org.openremote.container.util.MapAccess.getString;

/**
 * <p>
 * This service is used for retrieving, creating, or editing KeyStore (and TrustStore) files.
 * </p><p>
 * Currently the KeyStores are stored in the Storage directory of OpenRemote, which is usually volumed and persisted.
 * </p><p>
 * Each realm is allocated 2 KeyStores; A Client KeyStore, which is used for storing key-pairs that are used by
 * clients (be that as an Agent or as a plain client), and a TrustStore, which contains trusted certificates, usually SSL
 * certificates. To ensure both security and extensibility, both the default and predefined TrustStores are used to find
 * the correct certificates.
 * </p>
 */
public class KeyStoreServiceImpl implements KeyStoreService {

    protected PersistenceService persistenceService;
    protected ManagerIdentityService identityService;

    private KeyStore keyStore = null;
    private KeyStore trustStore = null;

    private static final String OR_SSL_CLIENT_KEYSTORE_FILE = "OR_SSL_CLIENT_KEYSTORE_FILE";
    private static final String OR_SSL_CLIENT_TRUSTSTORE_FILE = "OR_SSL_CLIENT_TRUSTSTORE_FILE";

    private static final String OR_SSL_CLIENT_KEYSTORE_PASSWORD = "OR_SSL_CLIENT_KEYSTORE_PASSWORD";
    private static final String OR_SSL_CLIENT_TRUSTSTORE_PASSWORD = "OR_SSL_CLIENT_TRUSTSTORE_PASSWORD";

    private static final String OR_KEYSTORE_PASSWORD = "OR_KEYSTORE_PASSWORD";

    protected Path keyStorePath;
    protected Path trustStorePath;

    private String keyStorePassword;

    @Override
    public int getPriority() {
        return ManagerIdentityService.PRIORITY + 10;
    }

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        identityService = container.getService(ManagerIdentityService.class);

        // Stop using OR_ADMIN_PASSWORD, if OR_KEYSTORE_PASSWORD is unset, use no password instead.
        keyStorePassword = getString(container.getConfig(), OR_KEYSTORE_PASSWORD, "");
    }

    @Override
    public void start(Container container) throws Exception {

        String keyStoreEnv = getString(container.getConfig(), OR_SSL_CLIENT_KEYSTORE_FILE, null);
        String trustStoreEnv = getString(container.getConfig(), OR_SSL_CLIENT_TRUSTSTORE_FILE, null);

        Optional<Path> keyStorePath = keyStoreEnv != null ? Optional.of(Paths.get(keyStoreEnv)) : Optional.empty();
        Optional<Path> trustStorePath = trustStoreEnv != null ? Optional.of(Paths.get(trustStoreEnv)) : Optional.empty();

        String keyStorePassword = getString(container.getConfig(), OR_SSL_CLIENT_KEYSTORE_PASSWORD, String.valueOf(getKeyStorePassword()));
        String trustStorePassword = getString(container.getConfig(), OR_SSL_CLIENT_TRUSTSTORE_PASSWORD, String.valueOf(getKeyStorePassword()));

        char[] usedKeystorePassword = getKeyStorePassword();

        if (keyStorePath.isPresent()) {
            this.keyStore = KeyStore.getInstance(keyStorePath.get().toFile(), keyStorePassword.toCharArray());
            this.keyStorePath = keyStorePath.get();
        } else {
            Path defaultKeyStorePath = persistenceService.resolvePath(Paths.get("keystores").resolve("client_keystore.p12"));
            if (new File(defaultKeyStorePath.toUri()).exists()) {
                this.keyStorePath = defaultKeyStorePath;
                try{
                    this.keyStore = KeyStore.getInstance(new File(defaultKeyStorePath.toUri()), getKeyStorePassword());
                }
                catch(Exception exception){
                    if (exception instanceof IOException e) {
                        //If the truststore's password is incorrect, try using OR_ADMIN_PASSWORD, as the first version of
                        // KeystoreService used that as the fallback password if OR_KEYSTORE_PASSWORD was unset.
                        if (e.getCause() instanceof UnrecoverableKeyException) {
                            String adminPassword = getString(container.getConfig(), OR_ADMIN_PASSWORD, "secret");
                            this.keyStore = KeyStore.getInstance(defaultKeyStorePath.toFile(), adminPassword.toCharArray());
                            if (this.keyStore != null) {
                                this.keyStorePassword = adminPassword;
                                usedKeystorePassword = adminPassword.toCharArray();
                                LOG.log(Level.INFO, "Loaded KeyStore from " + defaultKeyStorePath.toAbsolutePath() +
                                        " using OR_ADMIN_PASSWORD as fallback. Make sure to set OR_KEYSTORE_PASSWORD " +
                                        "to OR_ADMIN_PASSWORD's value to get rid of this message.");
                            } else {
                                LOG.log(Level.WARNING, "Failed to load KeyStore from " + defaultKeyStorePath.toAbsolutePath() + ": " + e.getMessage(), e);
                            }
                        }
                    } else {
                        LOG.log(Level.WARNING, "Failed to load KeyStore from " + defaultKeyStorePath.toAbsolutePath() + ": " + exception.getMessage(), exception);
                    }
                }
            } else {
                this.keyStore = createKeyStore(defaultKeyStorePath);
            }

            this.keyStorePath = defaultKeyStorePath;
        }

        if (trustStorePath.isPresent()) {
            this.trustStore = KeyStore.getInstance(trustStorePath.get().toFile(), trustStorePassword.toCharArray());
        } else {
            Path defaultTrustStorePath = persistenceService.resolvePath(Paths.get("keystores").resolve("client_truststore.p12"));
            if (new File(defaultTrustStorePath.toUri()).exists()) {
                this.trustStorePath = defaultTrustStorePath;
                try{
                    this.trustStore = KeyStore.getInstance(new File(defaultTrustStorePath.toUri()), getKeyStorePassword());
                }
                catch(Exception exception){
                    if (exception instanceof IOException e) {
                        //If the truststore's password is incorrect, try using OR_ADMIN_PASSWORD, as the first version of
                        // KeystoreService used that as the fallback password if OR_KEYSTORE_PASSWORD was unset.
                        if (e.getCause() instanceof UnrecoverableKeyException) {
                            String adminPassword = getString(container.getConfig(), OR_ADMIN_PASSWORD, "secret");
                            this.trustStore = KeyStore.getInstance(defaultTrustStorePath.toFile(), adminPassword.toCharArray());
                            if (this.trustStore != null) {
                                trustStorePassword = adminPassword;
                                LOG.log(Level.INFO, "Loaded TrustStore from " + defaultTrustStorePath.toAbsolutePath() +
                                        " using OR_ADMIN_PASSWORD as fallback. Make sure to set OR_KEYSTORE_PASSWORD " +
                                        "to OR_ADMIN_PASSWORD's value to get rid of this message.");
                            } else {
                                LOG.log(Level.WARNING, "Failed to load TrustStore from " + defaultTrustStorePath.toAbsolutePath() + ": " + e.getMessage(), e);
                            }
                        }
                    } else {
                        LOG.log(Level.WARNING, "Failed to load TrustStore from " + defaultTrustStorePath.toAbsolutePath() + ": " + exception.getMessage(), exception);
                    }
                }
            } else {
                this.trustStore = createKeyStore(defaultTrustStorePath);
            }
            this.trustStorePath = defaultTrustStorePath;
        }

        String keyStorePasswordEnv = getString(container.getConfig(), OR_KEYSTORE_PASSWORD, "");


        // We know that the used password to unlock the keystore is in usedKeystorePassword, but the keypairs inside of the keystore could use different passwords.
        // Below are potential passwords that we are going to try to unlock the keypairs with, so that we can then migrate them to the current default password.
        List<char[]> potentialPasswords = new ArrayList<char[]>();
        potentialPasswords.add(getString(container.getConfig(), OR_ADMIN_PASSWORD, "").toCharArray());
        potentialPasswords.add(keyStorePasswordEnv.toCharArray());
        potentialPasswords.add("secret".toCharArray());
        potentialPasswords.add(getKeyStorePassword());

        // Migrate any key entry passwords from "secret" (which was the default before #2018) to the current default password
        boolean changed = rewriteKeystoreWithUniformPasswords(
                this.keyStore,
                potentialPasswords,
                keyStorePasswordEnv.toCharArray()
        );

        // Optionally log a post-reload verification for one known alias (fresh instance — no cache):
        if (changed) {
            try {
                this.keyStore.getKey("master.testalias", "secret".toCharArray());
                LOG.warning("Post-rewrite check: old entry password still works for master.testalias (unexpected).");
            } catch (UnrecoverableKeyException expected) {
                LOG.info("Post-rewrite check: old entry password rejected for master.testalias.");
            }
            // Should succeed with new password:
            this.keyStore.getKey("master.testalias", safeChars(keyStorePasswordEnv.toCharArray()));
        }
    }

    private KeyStore getKeyStore() {
        try {
            return KeyStore.getInstance(this.keyStorePath.toAbsolutePath().toFile(), getKeyStorePassword());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private KeyStore getTrustStore() {
        try {
            return KeyStore.getInstance(this.trustStorePath.toAbsolutePath().toFile(), getKeyStorePassword());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void storeKeyStore(KeyStore keystore) {
        try {
            this.keyStore = keystore;
            keystore.store(new FileOutputStream(this.keyStorePath.toFile()), getKeyStorePassword());
        } catch (Exception saveException) {
            getLogger().severe("Couldn't store KeyStore to Storage! " + saveException.getMessage());
        }
    }

    private void storeKeyStore(KeyStore ks, char[] newStorePwd) throws IOException, GeneralSecurityException {
        Path tmp = Files.createTempFile(this.keyStorePath.getParent(), "keystore-rewrite", ".p12");
        try (OutputStream os = new FileOutputStream(tmp.toFile())) {
            ks.store(os, safeChars(newStorePwd));
        }
        try {
            java.nio.file.Files.move(tmp, this.keyStorePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            // Fallback if fs does not support ATOMIC_MOVE
            java.nio.file.Files.move(tmp, this.keyStorePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void storeTrustStore(KeyStore truststore) {
        try {
            this.trustStore = truststore;
            truststore.store(new FileOutputStream(this.trustStorePath.toFile()), getKeyStorePassword());
        } catch (Exception saveException) {
            getLogger().severe("Couldn't store TrustStore to Storage! " + saveException.getMessage());
        }
    }

    private char[] getKeyStorePassword() {
        return this.keyStorePassword.toCharArray();
    }

    @Override
    public KeyManagerFactory getKeyManagerFactory(String alias) throws Exception {
        KeyManagerFactory keyManagerFactory = new CustomKeyManagerFactory(alias);
        try {
            this.keyStore = null;
            this.keyStore = getKeyStore();
            keyManagerFactory.init(this.keyStore, getKeyStorePassword());
        } catch (Exception e) {
            throw new Exception("Could not retrieve KeyManagerFactory: " + e.getMessage());
        }

        return keyManagerFactory;
    }

    @Override
    public TrustManagerFactory getTrustManagerFactory() throws Exception {

        CustomX509TrustManagerFactory tmf = new CustomX509TrustManagerFactory(this.trustStore, (KeyStore) null);
        try {
            tmf.init((KeyStore) null);
        } catch (Exception e) {
            throw new Exception("Could not retrieve KeyManagerFactory: " + e.getMessage());
        }

        return tmf;
    }

    protected KeyStore createKeyStore(Path path) throws Exception {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(null, getKeyStorePassword());
        File keyStoreFile = path.toAbsolutePath().toFile();
        if (keyStoreFile.getParentFile() != null && !keyStoreFile.getParentFile().exists()) {
            keyStoreFile.getParentFile().mkdirs();
        }
        keyStoreFile.createNewFile();

        // Save the newly created KeyStore
        try (OutputStream os = new FileOutputStream(keyStoreFile)) {
            keystore.store(os, getKeyStorePassword());
        } catch (Exception saveException) {
            getLogger().severe("Couldn't store KeyStore to Storage! " + saveException.getMessage());
            throw saveException;
        }
        return keystore;
    }

    @Override
    public void stop(Container container) throws Exception {

    }

    private Logger getLogger() {
        return LOG;
    }

    private static char[] safeChars(char[] pwd) {
        return (pwd == null || pwd.length == 0) ? new char[0] : pwd;
    }

    /** Rewrites the keystore into a fresh PKCS12 with:
     *  - per-entry password = newEntryPwd
     *  - store password = newStorePwd
     *  Candidate oldEntryPwds are tried to recover keys.
     *  Cert entries are copied as-is.
     */
    private boolean rewriteKeystoreWithUniformPasswords(
            KeyStore source,
            List<char[]> oldEntryPwds,
            char[] newPwd
    ) throws Exception {

        KeyStore target = KeyStore.getInstance("PKCS12");
        // initialize empty keystore with the intended STORE password
        target.load(null, safeChars(newPwd));

        boolean changed = false;

        Enumeration<String> aliases = source.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();

            if (source.isCertificateEntry(alias)) {
                target.setCertificateEntry(alias, source.getCertificate(alias));
                continue;
            }

            if (source.isKeyEntry(alias)) {
                // First try with the intended NEW entry password: if it already works, just copy
                try {
                    Key k = source.getKey(alias, safeChars(newPwd));
                    java.security.cert.Certificate[] chain = source.getCertificateChain(alias);
                    target.setEntry(alias, new KeyStore.PrivateKeyEntry((PrivateKey) k, chain),
                            new KeyStore.PasswordProtection(safeChars(newPwd)));
                    continue;
                } catch (UnrecoverableKeyException ignore) {}

                // Otherwise try the list of legacy passwords
                boolean recovered = false;
                for (char[] oldPwd : oldEntryPwds) {
                    try {
                        Key k = source.getKey(alias, safeChars(oldPwd));
                        java.security.cert.Certificate[] chain = source.getCertificateChain(alias);
                        target.setEntry(alias, new KeyStore.PrivateKeyEntry((PrivateKey) k, chain),
                                new KeyStore.PasswordProtection(safeChars(newPwd)));
                        changed = true;
                        recovered = true;
                        break;
                    } catch (UnrecoverableKeyException ignore) {
                        // try next
                    }
                }

                if (!recovered) {
                    // if we can't recover this key with any candidate, do NOT drop it silently
                    throw new UnrecoverableKeyException("Could not unlock key alias \"" + alias + "\" with any provided password");
                }
            }
        }

        // Atomically replace original file
        Path tmp = Files.createTempFile(this.keyStorePath.getParent(), "keystore-rewrite-", ".p12");
        try (OutputStream os = new FileOutputStream(tmp.toFile())) {
            target.store(os, safeChars(newPwd));
        }
        try {
            Files.move(tmp, this.keyStorePath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            Files.move(tmp, this.keyStorePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        // reload to avoid PKCS12 key caching on the old object
        this.keyStorePassword = new String(newPwd);
        this.keyStore = KeyStore.getInstance(this.keyStorePath.toFile(), safeChars(newPwd));

        return changed;
    }
}
