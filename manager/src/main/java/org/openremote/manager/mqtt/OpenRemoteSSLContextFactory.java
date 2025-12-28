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
package org.openremote.manager.mqtt;

import org.apache.activemq.artemis.spi.core.remoting.ssl.SSLContextConfig;
import org.apache.activemq.artemis.spi.core.remoting.ssl.SSLContextFactory;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.manager.persistence.ManagerPersistenceService;
import org.openremote.model.Container;

import javax.net.ssl.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Custom SSLContextFactory for OpenRemote that dynamically loads certificates from /storage/certs.
 * This allows the MQTT broker to use certificates managed by HAProxy, which are shared via a Docker volume.
 * Certificates are reloaded periodically to support certificate renewal without restarts.
 * <p>
 * Note: This factory is instantiated by ActiveMQ Artemis via ServiceLoader, so we use a static
 * registry to access the Container instance set by MQTTBrokerService.
 */
public class OpenRemoteSSLContextFactory implements SSLContextFactory {

    private static final Logger LOG = Logger.getLogger(OpenRemoteSSLContextFactory.class.getName());
    private static final int HIGH_PRIORITY = 100;
    private static final long CERT_RELOAD_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes

    // Static registry for Container instance (set by MQTTBrokerService)
    private static volatile Container container;

    private volatile SSLContext cachedSSLContext;
    private final Map<String, Long> fileModificationTimes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "OpenRemote-SSLContext-Reloader");
        t.setDaemon(true);
        return t;
    });

    public OpenRemoteSSLContextFactory() {
        // Schedule periodic certificate reloading
        scheduler.scheduleWithFixedDelay(this::reloadCertificatesIfNeeded,
                CERT_RELOAD_INTERVAL_MS,
                CERT_RELOAD_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
        LOG.log(Level.INFO, "Initialized OpenRemote SSLContextFactory with certificate auto-reload");
    }

    /**
     * Register the Container instance. Must be called by MQTTBrokerService before
     * the MQTT broker starts.
     */
    public static void setContainer(Container container) {
        OpenRemoteSSLContextFactory.container = container;
        LOG.log(Level.INFO, "Container registered with OpenRemoteSSLContextFactory");
    }

    private Path getCertsDir() {
        return container.getService(ManagerPersistenceService.class).getStorageDir().resolve("certs");
    }

    @Override
    public SSLContext getSSLContext(SSLContextConfig config, Map<String, Object> additionalOpts) throws Exception {
        // If we have a cached context and certificates haven't changed, return it
        if (cachedSSLContext != null && !certificatesHaveChanged()) {
            return cachedSSLContext;
        }

        // Try to load from /storage/certs first
        SSLContext sslContext = tryLoadFromStorageCerts(config);

        // Fall back to default file-based loading if /storage/certs doesn't exist or fails
        if (sslContext == null) {
            sslContext = createSSLContextFromFiles(config);
        }

        cachedSSLContext = sslContext;
        return sslContext;
    }

    private SSLContext tryLoadFromStorageCerts(SSLContextConfig config) {
        try {
            Path certsDir = getCertsDir();
            if (!Files.exists(certsDir) || !Files.isDirectory(certsDir)) {
                LOG.log(Level.FINE, "Certificate directory " + certsDir + " does not exist, using fallback");
                return null;
            }

            // HAProxy stores certificates as numbered files like "00-cert", "01-selfsigned"
            // Find the first file numerically (e.g., 00-cert comes before 01-selfsigned)
            Path combinedPemFile = findFirstNumberedCertFile(certsDir);

            if (combinedPemFile != null) {
                // HAProxy combined PEM format (private key + cert in one file)
                LOG.log(Level.INFO, "Loading combined certificate from " + certsDir + "/" + combinedPemFile.getFileName());
                return createSSLContextFromCombinedPEM(combinedPemFile, config);
            }

            LOG.log(Level.FINE, "Certificate or key file not found in " +  certsDir + ".");
            return null;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to load certificates from " + getCertsDir() + ", falling back to file paths", e);
            return null;
        }
    }

    private Path findFirstNumberedCertFile(Path directory) throws IOException {
        // Pattern matches files like "00-cert", "01-selfsigned", "02-example.com"
        Pattern pattern = Pattern.compile("^(\\d+)-.*");

        List<Path> numberedFiles;
        try (Stream<Path> stream = Files.list(directory)) {
            numberedFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> pattern.matcher(path.getFileName().toString()).matches())
                    .sorted() // sorts `Path`s lexicographically (00-selfsigned, 01-test, 02-domain, etc.)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        if (numberedFiles.isEmpty()) {
            return null;
        }

        return numberedFiles.getFirst();
    }

    private SSLContext createSSLContextFromCombinedPEM(Path pemFile, SSLContextConfig config) throws Exception {
        // Ensure BC provider exists (optional but recommended if you call setProvider("BC"))
        if (Security.getProvider("BC") == null) {
            throw new SecurityException("No BC provider found");
        }

        ParsedPem parsed = readPemBundle(pemFile);

        if (parsed.privateKey == null) {
            throw new Exception("No private key found in " + pemFile);
        }
        if (parsed.certificateChain.isEmpty()) {
            throw new Exception("No certificate found in " + pemFile);
        }

        char[] password = config.getKeystorePassword() != null
                ? config.getKeystorePassword().toCharArray()
                : new char[0];

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setKeyEntry(
                "mqtt-server",
                parsed.privateKey,
                password,
                parsed.certificateChain.toArray(new java.security.cert.Certificate[0])
        );

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);

        TrustManager[] trustManagers = buildTrustManagers(config, parsed.certificateChain);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), trustManagers, new SecureRandom());

        updateFileModificationTimes(pemFile);
        LOG.log(Level.INFO, "SSLContext created successfully from combined PEM file");
        return sslContext;
    }

    private static final class ParsedPem {
        final PrivateKey privateKey;
        final List<X509Certificate> certificateChain;

        ParsedPem(PrivateKey privateKey, List<X509Certificate> certificateChain) {
            this.privateKey = privateKey;
            this.certificateChain = certificateChain;
        }
    }

    private ParsedPem readPemBundle(Path pemFile) throws Exception {
        PrivateKey privateKey = null;
        List<X509Certificate> certs = new ArrayList<>();

        JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter().setProvider("BC");
        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter().setProvider("BC");

        try (BufferedReader reader = Files.newBufferedReader(pemFile);
             PEMParser parser = new PEMParser(reader)) {

            Object obj;
            while ((obj = parser.readObject()) != null) {
                if (obj instanceof PrivateKeyInfo pki) {
                    privateKey = keyConverter.getPrivateKey(pki);
                } else if (obj instanceof PEMKeyPair kp) {
                    privateKey = keyConverter.getPrivateKey(kp.getPrivateKeyInfo());
                } else if (obj instanceof X509CertificateHolder holder) {
                    certs.add(certConverter.getCertificate(holder));
                }
            }
        }

        // If the file has multiple certs, they’ll be in file order. Usually leaf first, then intermediates.
        return new ParsedPem(privateKey, certs);
    }

    private TrustManager[] buildTrustManagers(SSLContextConfig config, List<X509Certificate> certChain) throws Exception {
        if (config.isTrustAll()) {
            return new TrustManager[]{createTrustAllManager()};
        }

        if (certChain.size() > 1) {
            // Use intermediates/root(s) as trust anchors
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            for (int i = 1; i < certChain.size(); i++) {
                trustStore.setCertificateEntry("ca-" + i, certChain.get(i));
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            return tmf.getTrustManagers();
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null); // system default CAs
        return tmf.getTrustManagers();
    }

    private SSLContext createSSLContextFromFiles(SSLContextConfig config) throws Exception {
        if (config.getKeystorePath() == null || config.getKeystorePath().isEmpty()) {
            throw new Exception("No keystore path configured and no certificates found in " + config.getKeystorePath());
        }

        LOG.log(Level.INFO, "Creating SSLContext from configured file paths: " + config.getKeystorePath());

        // Initialize KeyManager
        KeyManager[] keyManagers = createKeyManagers(
            config.getKeystorePath(),
            config.getKeystorePassword(),
            config.getKeystoreType() != null ? config.getKeystoreType() : "PKCS12"
        );

        // Initialize TrustManager
        TrustManager[] trustManagers;
        if (config.isTrustAll()) {
            trustManagers = new TrustManager[]{createTrustAllManager()};
        } else if (config.getTruststorePath() != null && !config.getTruststorePath().isEmpty()) {
            trustManagers = createTrustManagers(
                config.getTruststorePath(),
                config.getTruststorePassword(),
                config.getTruststoreType() != null ? config.getTruststoreType() : "PKCS12"
            );
        } else {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            trustManagers = tmf.getTrustManagers();
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, new SecureRandom());

        LOG.log(Level.INFO, "SSLContext created successfully from file paths");
        return sslContext;
    }

    private KeyManager[] createKeyManagers(String keystorePath, String keystorePassword, String keystoreType) throws Exception {
        KeyStore keyStore = loadKeyStore(keystorePath, keystorePassword, keystoreType);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keystorePassword != null ? keystorePassword.toCharArray() : null);
        return kmf.getKeyManagers();
    }

    private TrustManager[] createTrustManagers(String truststorePath, String truststorePassword, String truststoreType) throws Exception {
        KeyStore trustStore = loadKeyStore(truststorePath, truststorePassword, truststoreType);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        return tmf.getTrustManagers();
    }

    private KeyStore loadKeyStore(String path, String password, String type) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(type);
        try (InputStream is = new FileInputStream(path)) {
            keyStore.load(is, password != null ? password.toCharArray() : null);
        }
        return keyStore;
    }

    private Path findFile(Path directory, String... possibleNames) throws IOException {
        for (String name : possibleNames) {
            Path file = directory.resolve(name);
            if (Files.exists(file) && Files.isRegularFile(file)) {
                return file;
            }
        }
        return null;
    }

    private void reloadCertificatesIfNeeded() {
        try {
            if (certificatesHaveChanged()) {
                LOG.log(Level.INFO, "Certificate files have changed, clearing SSL context cache");
                clearSSLContexts();
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error checking for certificate changes", e);
        }
    }

    private boolean certificatesHaveChanged() {
        try {
            Path certsDir = getCertsDir();
            if (!Files.exists(certsDir)) {
                return false;
            }

            // Check for HAProxy numbered combined cert files first
            Path combinedPemFile = findFirstNumberedCertFile(certsDir);
            if (combinedPemFile != null) {
                return hasFileChanged(combinedPemFile);
            }

            throw new Exception();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasFileChanged(Path file) throws IOException {
        if (file == null) {
            return false;
        }

        String path = file.toString();
        long currentModTime = Files.getLastModifiedTime(file).toMillis();
        Long previousModTime = fileModificationTimes.get(path);

        return previousModTime == null || currentModTime != previousModTime;
    }

    private void updateFileModificationTimes(Path... files) throws IOException {
        for (Path file : files) {
            if (file != null && Files.exists(file)) {
                fileModificationTimes.put(file.toString(), Files.getLastModifiedTime(file).toMillis());
            }
        }
    }

    private TrustManager createTrustAllManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                // Trust all clients
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                // Trust all servers
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

    @Override
    public void clearSSLContexts() {
        LOG.log(Level.INFO, "Clearing SSL context cache");
        cachedSSLContext = null;
    }

    @Override
    public int getPriority() {
        return HIGH_PRIORITY;
    }
}
