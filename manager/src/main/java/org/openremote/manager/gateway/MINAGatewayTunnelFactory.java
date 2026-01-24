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
import org.openremote.model.gateway.GatewayTunnelStartRequestEvent;
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
    public GatewayTunnelSession createSession(GatewayTunnelStartRequestEvent startRequestEvent, Consumer<Throwable> closedCallback) {
       CompletableFuture<Void> connectionResultFuture = new CompletableFuture<>();
       AtomicReference<ClientSession> sessionRef = new AtomicReference<>();
       AtomicReference<Throwable> failureCause = new AtomicReference<>();
       // Flags if the user explicitly called disconnect()
       AtomicBoolean isManualDisconnect = new AtomicBoolean(false);

       LOG.fine("Creating session: " + startRequestEvent);

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
             connectionResultFuture.cancel(true);
             closeResult.complete(null);
          }
          return closeResult;
       };

       // Initiate the Async Connection
       try {
          ConnectFuture connectFuture = client.connect("root", startRequestEvent.getSshHostname(), startRequestEvent.getSshPort());

          connectFuture.addListener(connFuture -> {
             if (connFuture.isConnected()) {
                ClientSession session = connFuture.getSession();
                sessionRef.set(session); // Store in atomic ref for the disconnect supplier to see

                // Add session listeners (reconnection logic, etc.)
                session.addSessionListener(new SessionListener() {
                   @Override
                   public void sessionException(Session s, Throwable t) {
                      // Capture the error (only the first one matters usually)
                      failureCause.compareAndSet(null, t);

                      // Optional: Log it here, but wait for sessionClosed to trigger logic
                      LOG.warning("Session exception caught: " + t.getMessage());
                   }

                   @Override
                   public void sessionClosed(Session s) {
                      // This method is ALWAYS called when session dies (error or manual)

                      if (isManualDisconnect.get()) {
                         // Case A: We asked for it -> Clean exit
                         LOG.fine("Session closed by user: " + startRequestEvent);
                         closedCallback.accept(null);
                      } else {
                         Throwable failure = failureCause.get() != null ? failureCause.get() : new IOException("Session closed unexpectedly");
                         LOG.info("Session closed unexpectedly '" + failure.getMessage() + "' : " + startRequestEvent);
                         closedCallback.accept(failure);
                      }
                   }
                });

                // Start Authentication
                try {
                   session.auth().addListener(authFuture -> {
                      if (authFuture.isSuccess()) {
                         LOG.fine("Session connected and authenticated: " + startRequestEvent);
                         connectionResultFuture.complete(null);
                      } else {
                         Throwable failure = authFuture.getException();
                         String msg = failure != null ? failure.getMessage() : "Unknown error";
                         msg = "Authentication failed '" + msg + "': " + startRequestEvent;
                         session.close(true);
                         LOG.warning(msg);
                         connectionResultFuture.completeExceptionally(new IOException(msg, failure));
                      }
                   });
                } catch (IOException e) {
                   session.close(true);
                   connectionResultFuture.completeExceptionally(e);
                }
             } else {
                // Connection failed (Network level)
                Throwable t = connFuture.getException();
                String msg = t != null ? t.getMessage() : "Unknown error";
                msg = "Connection failed '" + msg + "': " + startRequestEvent;
                LOG.warning(msg);
                connectionResultFuture.completeExceptionally(new IOException(msg, t));
             }
          });
       } catch (Exception e) {
          LOG.warning("Connection failed '" + e.getMessage() + "': " + startRequestEvent);
          connectionResultFuture.completeExceptionally(e);
       }

       return new GatewayTunnelSession(
          connectionResultFuture,
          startRequestEvent.getInfo(),
          disconnectSupplier
       );
    }
}
