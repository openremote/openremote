package org.openremote.manager.gateway;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.openremote.model.gateway.GatewayTunnelInfo;
import org.openremote.model.syslog.SyslogCategory;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.GATEWAY;

public class MINAGatewayTunnelFactory implements GatewayTunnelFactory {

    private static final Logger LOG = SyslogCategory.getLogger(GATEWAY, MINAGatewayTunnelFactory.class.getName());
    protected File tunnelKeyFile;
    protected String localhostRewrite;
    protected SshClient client;

    public MINAGatewayTunnelFactory(File tunnelKeyFile, String localhostRewrite) {
        this.tunnelKeyFile = tunnelKeyFile;
        this.localhostRewrite = localhostRewrite;
        client = SshClient.setUpDefaultClient();

        // Load the key
        FileKeyPairProvider keyProvider = new FileKeyPairProvider(tunnelKeyFile.toPath());
        Iterable<KeyPair> keys = keyProvider.loadKeys(null);
        client.setKeyIdentityProvider(KeyIdentityProvider.wrapKeyPairs(keys));

        // Disable reading ~/.ssh/known_hosts
        client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);

        // Disable reading ~/.ssh/config
        // This prevents the client from inheriting aliases or settings from the user's config
        client.setHostConfigEntryResolver(HostConfigEntryResolver.EMPTY);
    }

    @Override
    public void start() {
        client.start();
    }

    @Override
    public void stop() {
        client.stop();
    }

    @Override
    public GatewayTunnelSession createSession(String hostname, int port, GatewayTunnelInfo gatewayTunnelInfo, Consumer<Throwable> closedCallback) {
        AtomicReference<CompletableFuture<Void>> activeAttemptRef = new AtomicReference<>();;
        AtomicReference<ClientSession> sessionRef = new AtomicReference<>();
        AtomicReference<Throwable> failureCause = new AtomicReference<>();
        AtomicReference<GatewayTunnelSession> tunnelSession = new AtomicReference<>();
        // Flags if the user explicitly called disconnect()
        AtomicBoolean isManualDisconnect = new AtomicBoolean(false);

        LOG.fine("Creating session: hostname=" + hostname + ", port=" + port + ", tunnelInfo=" + gatewayTunnelInfo);

        // This allows the caller to disconnect this specific session later
        Supplier<CompletableFuture<Void>> disconnectSupplier = () -> {
            CompletableFuture<Void> closeResult = new CompletableFuture<>();
            ClientSession currentSession = sessionRef.get();

            // We are intentionally closing this session
            isManualDisconnect.set(true);

            if (currentSession != null) {
                currentSession.close(false).addListener(future -> {
                    if (future.isClosed()) {
                        closeResult.complete(null);
                    } else {
                        String msg = "Failed to close session cleanly: " + currentSession;
                        LOG.warning(msg);
                        closeResult.completeExceptionally(new IOException(msg));
                    }
                });
            } else {
                // If session was never established, cancel the connection attempt
                CompletableFuture<Void> activeAttempt = activeAttemptRef.get();
                if (activeAttempt != null && !activeAttempt.isDone()) {
                    activeAttempt.cancel(true);
                }
                closeResult.complete(null);
            }
            return closeResult;
        };

        // Reconnection
        Consumer<CompletableFuture<Void>> startConnectionAttempt = (futureToComplete) -> {
            // Reset flags for the new attempt
            isManualDisconnect.set(false);
            failureCause.set(null);

            try {
                ConnectFuture connectFuture = client.connect("root", hostname, port);

                connectFuture.addListener(connFuture -> {
                    if (connFuture.isConnected()) {
                        ClientSession session = connFuture.getSession();
                        sessionRef.set(session); // Store in atomic ref for the disconnect supplier to see

                        session.addSessionListener(new SessionListener() {
                            @Override
                            public void sessionException(Session s, Throwable t) {
                                failureCause.compareAndSet(null, t);
                                LOG.warning("Session exception caught: " + t.getMessage());
                            }

                            @Override
                            public void sessionClosed(Session s) {
                                if (isManualDisconnect.get()) {
                                    LOG.fine("Session closed by user: " + gatewayTunnelInfo);
                                    tunnelSession.get().onClose(null);
                                } else {
                                    Throwable failure = failureCause.get() != null ? failureCause.get() : new IOException("Session closed unexpectedly");
                                    LOG.info("Session closed unexpectedly '" + failure.getMessage() + "' : " + gatewayTunnelInfo);
                                    tunnelSession.get().onClose(failure);
                                }
                            }
                        });

                        // Start Authentication
                        try {
                            session.auth().addListener(authFuture -> {
                                if (authFuture.isSuccess()) {
                                    LOG.fine("Session connected and authenticated: " + gatewayTunnelInfo);
                                    futureToComplete.complete(null);
                                } else {
                                    Throwable failure = authFuture.getException();
                                    String msg = "Authentication failed '" + (failure != null ? failure.getMessage() : "Unknown error") + "': " + gatewayTunnelInfo;
                                    session.close(true);
                                    LOG.warning(msg);
                                    futureToComplete.completeExceptionally(new IOException(msg, failure));
                                }
                            });
                        } catch (IOException e) {
                            session.close(true);
                            futureToComplete.completeExceptionally(e);
                        }
                    } else {
                        // Connection failed (Network level)
                        Throwable t = connFuture.getException();
                        String msg = "Connection failed '" + (t != null ? t.getMessage() : "Unknown error") + "': " + gatewayTunnelInfo;
                        LOG.warning(msg);
                        futureToComplete.completeExceptionally(new IOException(msg, t));
                    }
                });
            } catch (Exception e) {
                LOG.warning("Connection failed '" + e.getMessage() + "': " + gatewayTunnelInfo);
                futureToComplete.completeExceptionally(e);
            }
        };

        // Define reconnect
        Supplier<CompletableFuture<Void>> reconnectSupplier = () -> {
            CompletableFuture<Void> active = activeAttemptRef.get();
            if (active != null && !active.isDone()) {
                LOG.fine("Reconnect requested but connection attempt already in progress. Returning existing future: " + gatewayTunnelInfo);
                return active;
            }

            LOG.info("Initiating reconnect for: " + gatewayTunnelInfo);

            // First, ensure any existing session is closed cleanly
            return disconnectSupplier.get().thenCompose(ignored -> {
                // Then start a fresh connection attempt
                CompletableFuture<Void> newAttempt = new CompletableFuture<>();
                activeAttemptRef.set(newAttempt);
                startConnectionAttempt.accept(newAttempt);
                return newAttempt;
            });
        };

        CompletableFuture<Void> initialFuture = new CompletableFuture<>();
        activeAttemptRef.set(initialFuture);
        startConnectionAttempt.accept(initialFuture);

        tunnelSession.set(new GatewayTunnelSession(
                initialFuture,
                gatewayTunnelInfo,
                disconnectSupplier,
                reconnectSupplier,
                closedCallback
        ));

        return tunnelSession.get();
    }
}
