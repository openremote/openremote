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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
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


        if (keyStorePath.isPresent()) {
            this.keyStore = KeyStore.getInstance(keyStorePath.get().toFile(), keyStorePassword.toCharArray());
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
}
