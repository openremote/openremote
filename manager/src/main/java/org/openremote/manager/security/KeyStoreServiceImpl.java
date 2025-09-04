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

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.keycloak.representations.idm.RealmRepresentation;
import org.openremote.agent.protocol.mqtt.CustomKeyManagerFactory;
import org.openremote.agent.protocol.mqtt.CustomX509TrustManagerFactory;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.security.KeyStoreService;
import org.openremote.model.security.Realm;
import org.openremote.model.security.RealmResource;
import org.openremote.model.security.User;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

    public KeyStore mtlsKeyStore;

    private KeyPair caKeyPair;
    private X509Certificate caCert;

    private String keyStorePassword;

    @Override
    public int getPriority() {
        return ManagerWebService.PRIORITY + 10;
    }

    @Override
    public void init(Container container) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
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

        this.mtlsKeyStore = createKeyStore(persistenceService.resolvePath(Paths.get("keystores").resolve("mtls_keystore.p12")));

        initCA();
//        this.mtlsKeyStore = KeyStore.getInstance(, "".toCharArray());

        KeycloakIdentityProvider idp = (KeycloakIdentityProvider) identityService.identityProvider;

        List<RealmRepresentation> realms = new ArrayList<>();
//        idp.getRealms(rr -> {
//
//            rr.findAll().forEach(realm -> {
//                realms.add(realm);
////                realm.getUsers().forEach(user -> {
////                    LOG.info("Realm: " + realm.getDisplayName() + ", User: " + user.getUsername() + ", Roles: " + Arrays.toString(user.getRealmRoles().toArray()));
////                });
//            });
//            return null;
//        });

        Map<RealmRepresentation, List<User>> usersByRealm = realms.stream()
                .collect(Collectors.toMap(
                        realm -> realm, // or use your actual Realm object if available
                        realm -> Arrays.asList(identityService.getIdentityProvider()
                                .queryUsers(new UserQuery().realm(new RealmPredicate(realm.getRealm()))))
                ));
        usersByRealm.forEach((realm, users) -> (users).forEach(user -> {
            LOG.info("Realm: " + user.getRealm() + ", User: " + user.getUsername() + ", Roles: " + user.getAttributes().toString());
            try {
                createUserKeyStoreEntryWithCA(user, realm);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));

        Arrays.stream(identityService.identityProvider.getRealms()).sequential().forEach(realm -> {

        });
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

    private void initCA() throws Exception {
        String caAlias = "rootCA";

        if (mtlsKeyStore.containsAlias(caAlias)) {
            Key caPrivateKey = mtlsKeyStore.getKey(caAlias, getKeyStorePassword());
            java.security.cert.Certificate certificate = mtlsKeyStore.getCertificate(caAlias);
            this.caKeyPair = new KeyPair(certificate.getPublicKey(), (PrivateKey) caPrivateKey);
            this.caCert = (X509Certificate) certificate;
        } else {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
            keyGen.initialize(4096);
            caKeyPair = keyGen.generateKeyPair();

            caCert = generateCACertificate("CN=OpenRemote Root CA", caKeyPair);

            mtlsKeyStore.setKeyEntry(caAlias, caKeyPair.getPrivate(), getKeyStorePassword(), new java.security.cert.Certificate[]{caCert});
            storeMltsKeystore(mtlsKeyStore);
        }
    }

    private void storeMltsKeystore(KeyStore keystore) {
        try {
            keystore.store(new FileOutputStream(persistenceService.resolvePath(Paths.get("keystores").resolve("mtls_keystore.p12")).toFile()), getKeyStorePassword());
            this.mtlsKeyStore = keystore;
        } catch (Exception e) {
            getLogger().severe("Couldn't store MTLS KeyStore to Storage! " + e.getMessage());
        }
    }

    private X509Certificate generateCACertificate(String dn, KeyPair keyPair) throws Exception {
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);
        Date endDate = new Date(now + (30L * 365 * 24 * 60 * 60 * 1000)); // 10 years validity

        X500Name issuer = new X500Name(dn);

        BigInteger serial = BigInteger.valueOf(now);

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer, serial, startDate, endDate, issuer, keyPair.getPublic());

        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider("BC");
        ContentSigner signer = signerBuilder.build(keyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(signer);

        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);
    }
    private X509Certificate generateUserCertificate(String dn, KeyPair userKeyPair) throws Exception {
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);
        Date endDate = new Date(now + (10L * 365L * 24 * 60 * 60 * 1000)); // 10 year validity

        BigInteger serial = BigInteger.valueOf(now);

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                new X500Name(caCert.getSubjectX500Principal().getName()), // Issuer
                serial,
                startDate,
                endDate,
                new X500Name(dn), // Subject
                userKeyPair.getPublic()
        );

        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider("BC");
        ContentSigner signer = signerBuilder.build(caKeyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(signer);

        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);
    }

    private void createUserKeyStoreEntryWithCA(User user, RealmRepresentation realm) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
        keyGen.initialize(2048);
        KeyPair userKeyPair = keyGen.generateKeyPair();

        String dn = "CN=" + user.getId() + ", OU=" + realm.getId();
        X509Certificate userCert = generateUserCertificate(dn, userKeyPair);

        mtlsKeyStore.setKeyEntry(
                user.getUsername() + "_" + realm.getRealm(),
                userKeyPair.getPrivate(),
                getKeyStorePassword(),
                new java.security.cert.Certificate[]{userCert, caCert} // certificate chain
        );

        storeMltsKeystore(mtlsKeyStore);
    }
}
