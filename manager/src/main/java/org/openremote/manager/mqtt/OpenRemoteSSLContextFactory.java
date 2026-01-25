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
import org.openremote.manager.persistence.ManagerPersistenceService;
import org.openremote.model.Container;

import javax.net.ssl.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Custom SSLContextFactory for OpenRemote that dynamically loads certificates for the MQTT broker.
 * <p>
 * Certificate loading follows this priority order:
 * <ol>
 *   <li>Storage directory ({@code /storage/certs}) - for HAProxy-managed certificates shared via Docker volume</li>
 *   <li>Configured keystore file paths - traditional JKS/PKCS12 keystores from Artemis config</li>
 *   <li>Classpath resource - bundled self-signed certificate for development/testing</li>
 * </ol>
 * <p>
 * Certificates are reloaded periodically to support certificate renewal without restarts.
 * <p>
 * Note: This factory is instantiated by ActiveMQ Artemis via ServiceLoader, so we use a static
 * registry to access the Container instance set by MQTTBrokerService.
 */
public class OpenRemoteSSLContextFactory implements SSLContextFactory {

    private static final Logger LOG = Logger.getLogger(OpenRemoteSSLContextFactory.class.getName());
    private static final int HIGH_PRIORITY = 100;
    private static final long CERT_RELOAD_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes
    private static final Pattern NUMBERED_CERT_PATTERN = Pattern.compile("^(\\d+)-.*");

    /**
     * Classpath resource path for the fallback self-signed certificate.
     * Located relative to this class in the resources directory.
     */
    private static final String FALLBACK_CERT_RESOURCE = "01-selfsigned";

    // Static registry for Container instance (set by MQTTBrokerService)
    private static volatile Container container;

    private volatile SSLContext cachedSSLContext;
    private final Map<String, Long> fileModificationTimes = new ConcurrentHashMap<>();
    private ScheduledFuture<?> reloadFuture;

    public OpenRemoteSSLContextFactory() {
        // Schedule periodic certificate reloading
        reloadFuture = container.getScheduledExecutor().schedule(this::reloadCertificatesIfNeeded, CERT_RELOAD_INTERVAL_MS, TimeUnit.MILLISECONDS);
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
        if (container == null) {
            return null;
        }
        ManagerPersistenceService persistenceService = container.getService(ManagerPersistenceService.class);
        if (persistenceService == null || persistenceService.getStorageDir() == null) {
            return null;
        }
        return persistenceService.getStorageDir().resolve("proxy").resolve("certs");
    }

    @Override
    public SSLContext getSSLContext(SSLContextConfig config, Map<String, Object> additionalOpts) throws Exception {
        // Return cached context if certificates haven't changed
        if (cachedSSLContext != null && !certificatesHaveChanged()) {
            return cachedSSLContext;
        }

        // Priority 1: Try to load from /storage/certs (HAProxy)
        SSLContext sslContext = tryLoadFromHaproxy(config);

        // Priority 2: Fall back to configured keystore file paths
        if (sslContext == null) {
            sslContext = tryLoadFromConfiguredFiles(config);
        }

        // Priority 3: Fall back to bundled classpath resource
        if (sslContext == null) {
            sslContext = tryLoadFromClasspathResource(config);
        }

        if (sslContext == null) {
            throw new SSLException("Failed to create SSLContext: no certificates found in storage, classpath, or configuration");
        }

        cachedSSLContext = sslContext;
        return sslContext;
    }

    private SSLContext tryLoadFromHaproxy(SSLContextConfig config) {
        Path certsDir = getCertsDir();
        if (certsDir == null) {
            LOG.log(Level.FINE, "Container not initialized, skipping storage certificate loading");
            return null;
        }

        try {
            if (!Files.exists(certsDir) || !Files.isDirectory(certsDir)) {
                LOG.log(Level.FINE, "Certificate directory {0} does not exist", certsDir);
                return null;
            }

            // HAProxy stores certificates as numbered files like "00-cert", "01-selfsigned"
            // Find the first file numerically (e.g., 00-cert comes before 01-selfsigned)
            Path combinedPemFile = findFirstNumberedCertFile(certsDir);

            if (combinedPemFile == null) {
                LOG.log(Level.FINE, "No numbered certificate files found in {0}", certsDir);
                return null;
            }

            LOG.log(Level.INFO, "Loading certificate from storage: " + combinedPemFile.getFileName());
            SSLContext ctx;
            try (BufferedReader reader = Files.newBufferedReader(combinedPemFile)) {
                ctx = createSSLContextFromPEM(reader, combinedPemFile.toString(), config);
            }
            updateFileModificationTimes(combinedPemFile);
            return ctx;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to load certificates from " + certsDir, e);
            return null;
        }
    }

    private SSLContext tryLoadFromClasspathResource(SSLContextConfig config) {
        try (InputStream is = getClass().getResourceAsStream(FALLBACK_CERT_RESOURCE)) {
            if (is == null) {
                LOG.log(Level.FINE, "Classpath resource {0} not found", FALLBACK_CERT_RESOURCE);
                return null;
            }

            LOG.log(Level.INFO, "Loading certificate from classpath resource: {0}", FALLBACK_CERT_RESOURCE);

            SSLContext ctx;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                ctx = createSSLContextFromPEM(reader, "classpath resource", config);
            }

            LOG.log(Level.INFO, "SSLContext created successfully from classpath resource");
            return ctx;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to load certificate from classpath resource: " + FALLBACK_CERT_RESOURCE, e);
            return null;
        }
    }

    private SSLContext tryLoadFromConfiguredFiles(SSLContextConfig config) {
        if (config.getKeystorePath() == null || config.getKeystorePath().isEmpty()) {
            LOG.log(Level.FINE, "No keystore path configured");
            return null;
        }

        try {
            LOG.log(Level.INFO, "Loading certificate from configured keystore: {0}", config.getKeystorePath());
            return createSSLContextFromFiles(config);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to load certificates from configured keystore: " + config.getKeystorePath(), e);
            return null;
        }
    }

    private Path findFirstNumberedCertFile(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> NUMBERED_CERT_PATTERN.matcher(path.getFileName().toString()).matches())
                    .min(Comparator.comparing(Path::getFileName))
                    .orElse(null);
        }
    }

    private SSLContext createSSLContextFromPEM(Reader reader, String source, SSLContextConfig config) throws Exception {
        ensureBouncyCastleProvider();

        ParsedPem parsed = parsePemContent(reader);

        if (parsed.privateKey == null) {
            throw new SSLException("No private key found in " + source);
        }
        if (parsed.certificateChain.isEmpty()) {
            throw new SSLException("No certificate found in " + source);
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
        return sslContext;
    }

    private void ensureBouncyCastleProvider() {
        if (Security.getProvider("BC") == null) {
            throw new SecurityException("BouncyCastle provider not found. Ensure BC is registered before using this factory.");
        }
    }

    private static final class ParsedPem {
        final PrivateKey privateKey;
        final List<X509Certificate> certificateChain;

        ParsedPem(PrivateKey privateKey, List<X509Certificate> certificateChain) {
            this.privateKey = privateKey;
            this.certificateChain = certificateChain;
        }
    }

    private ParsedPem parsePemContent(Reader reader) throws Exception {
        PrivateKey privateKey = null;
        List<X509Certificate> certs = new ArrayList<>();

        JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter().setProvider("BC");
        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter().setProvider("BC");

        try (PEMParser parser = new PEMParser(reader)) {
            Object obj;
            while ((obj = parser.readObject()) != null) {
                switch (obj) {
                    case PrivateKeyInfo pki -> privateKey = keyConverter.getPrivateKey(pki);
                    case PEMKeyPair kp -> privateKey = keyConverter.getPrivateKey(kp.getPrivateKeyInfo());
                    case X509CertificateHolder holder -> certs.add(certConverter.getCertificate(holder));
                    default -> {}
                }
            }
        }

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
        Path certsDir = getCertsDir();
        if (certsDir == null || !Files.exists(certsDir)) {
            return false;
        }

        try {
            Path combinedPemFile = findFirstNumberedCertFile(certsDir);
            return combinedPemFile != null && hasFileChanged(combinedPemFile);
        } catch (IOException e) {
            LOG.log(Level.FINE, "Error checking certificate file changes", e);
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
        fileModificationTimes.clear();
    }

    @Override
    public int getPriority() {
        return HIGH_PRIORITY;
    }

    /**
     * Shuts down the certificate reload scheduler and clears all cached state.
     * Should be called when the MQTT broker is stopping.
     */
    public void shutdown() {
        LOG.log(Level.INFO, "Shutting down OpenRemoteSSLContextFactory");
        reloadFuture.cancel(true);
        clearSSLContexts();
    }

    /**
     * Clears the Container reference. Should be called when the MQTT broker is stopping.
     */
    public static void clearContainer() {
        container = null;
    }
}
