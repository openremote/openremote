/*
 *
 *  * Copyright 2026, OpenRemote Inc.
 *  *
 *  * See the CONTRIBUTORS.txt file in the distribution for a
 *  * full listing of individual contributors.
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as
 *  * published by the Free Software Foundation, either version 3 of the
 *  * License, or (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.openremote.manager.gateway;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.openremote.model.gateway.GatewayTunnelInfo;
import org.openremote.model.syslog.SyslogCategory;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.GATEWAY;

/**
 * This is a {@link GatewayTunnelFactory} implementation using the Apache MINA SSHD library
 */
public class MINAGatewayTunnelFactory implements GatewayTunnelFactory {

    private static final Logger LOG = SyslogCategory.getLogger(GATEWAY, MINAGatewayTunnelFactory.class.getName());
    private static final Duration RECONNECT_DELAY = Duration.ofSeconds(5);
    protected File tunnelKeyFile;
    protected String localhostRewrite;
    protected SshClient client;
    protected ExecutorService executor;
    protected ScheduledExecutorService scheduledExecutor;

   /**
    * This represents the state of an individual remote forward session
    */
   private class TunnelSessionManager {
      private final String sshHost;
      private final int sshPort;
      private final GatewayTunnelInfo tunnelInfo;
      private final CompletableFuture<Void> initialFuture;
      private final AtomicBoolean isRunning = new AtomicBoolean(false);
      private final AtomicReference<ClientSession> currentSession = new AtomicReference<>();

      public TunnelSessionManager(String sshHost, int sshPort, GatewayTunnelInfo tunnelInfo, CompletableFuture<Void> initialFuture) {
         this.sshHost = sshHost;
         this.sshPort = sshPort;
         this.tunnelInfo = tunnelInfo;
         this.initialFuture = initialFuture;
      }

      public void start() {
         isRunning.set(true);
         attemptConnection();
      }

      void stop() {
         if (isRunning.compareAndSet(true, false)) {
            LOG.fine("Stopping session: " + this);
            ClientSession session = currentSession.getAndSet(null);
            if (session != null) {
               try {
                  session.close();
               } catch (IOException e) {
                  LOG.fine("Error closing session during tunnel stop msg=" + e.getMessage() + ": " + this);
               }
            }
         }
      }

      private void attemptConnection() {
         if (!isRunning.get() || !client.isOpen()) return;

         LOG.fine("Initiating connection attempt: " + this);

         try {
            // Arbitrary username being used it is irrelevant
            ConnectFuture future = client.connect("root", sshHost, sshPort);

            future.addListener(f -> {
               if (f.isConnected()) {
                  performAuth(f.getSession());
               } else {
                  handleFailure(f.getException());
               }
            });
         } catch (Throwable t) {
            handleFailure(t);
         }
      }

      private void performAuth(ClientSession session) {
         if (!isRunning.get()) {
            closeSessionQuietly(session);
            return;
         }

         LOG.fine("Initiating auth: " + this);

         try {
            AuthFuture authFuture = session.auth();
            authFuture.addListener(f -> {
               if (f.isSuccess()) {
                  // CRITICAL: We are currently on the I/O thread
                  scheduledExecutor.submit(() -> setupForwarding(session));
               } else {
                  closeSessionQuietly(session);
                  handleFailure(f.getException());
               }
            });
         } catch (Throwable t) {
            closeSessionQuietly(session);
            handleFailure(t);
         }
      }

      /**
       * This method blocks waiting for the server to confirm the port forwarding.
       * It MUST NOT be called from an I/O thread.
       */
      private void setupForwarding(ClientSession session) {
         // Re-check state since we switched threads
         if (!isRunning.get()) {
            closeSessionQuietly(session);
            return;
         }

         LOG.fine("Initiating forwarding: " + this);

         try {
            String bindAddress = tunnelInfo.getType() ==  GatewayTunnelInfo.Type.TCP ? "" : tunnelInfo.getId();
            int rPort = tunnelInfo.getType() == GatewayTunnelInfo.Type.HTTPS ? 443 : tunnelInfo.getType() == GatewayTunnelInfo.Type.HTTP ? 80 : tunnelInfo.getAssignedPort();
            String target = localhostRewrite != null && "localhost".equals(tunnelInfo.getTarget()) ? localhostRewrite : tunnelInfo.getTarget();
            SshdSocketAddress remoteAddress = new SshdSocketAddress(bindAddress, rPort);
            SshdSocketAddress localAddress = new SshdSocketAddress(target, tunnelInfo.getTargetPort());

            // This call blocks!
            session.startRemotePortForwarding(remoteAddress, localAddress);
            handleSuccess(session);
         } catch (Exception e) {
            closeSessionQuietly(session);
            handleFailure(e);
         }
      }

      private void handleSuccess(ClientSession session) {
         currentSession.set(session);

         if (!initialFuture.isDone()) {
            LOG.info("Remote port forwarding started: " + this);
         } else {
            LOG.fine("Remote port forwarding reconnected: " + this);
         }
         initialFuture.complete(null);

         session.addCloseFutureListener(f -> {
            LOG.fine("Remote port forwarding closed: " + this);
            if (isRunning.get()) {
               handleFailure(null);
            }
         });
      }

      private void handleFailure(Throwable t) {
         boolean wasInitialFailure = initialFuture.completeExceptionally(
            t != null ? t : new RuntimeException("Connection failed")
         );

         if (wasInitialFailure) {
            // If initial connection attempt failed, we stop completely.
            LOG.info("Initial connect has failed so aborting: " + this);
            isRunning.set(false);
         } else if (isRunning.get()) {
            // Future was already done (meaning previous success), so this is a drop. Retry.
            LOG.info("Connection lost so scheduling reconnect" + RECONNECT_DELAY + ": " + this);
            scheduleReconnect();
         }
      }

      private void scheduleReconnect() {
         if (!isRunning.get() || !client.isOpen()) return;
         scheduledExecutor.schedule(this::attemptConnection, RECONNECT_DELAY.toMillis(), TimeUnit.MILLISECONDS);
      }

      private void closeSessionQuietly(ClientSession session) {
         try { session.close(); } catch (IOException ignored) {}
      }

      @Override
      public String toString() {
         return "sshHost='" + sshHost + '\'' +
            ", sshPort=" + sshPort +
            ", isRunning=" + isRunning +
            ", tunnelInfo=" + tunnelInfo +
            '}';
      }
   }

    public MINAGatewayTunnelFactory(ExecutorService executor, ScheduledExecutorService scheduledExecutor, File tunnelKeyFile, String localhostRewrite) {
        this.executor = executor;
        this.scheduledExecutor = scheduledExecutor;
        this.tunnelKeyFile = tunnelKeyFile;
        this.localhostRewrite = localhostRewrite;
        client = SshClient.setUpDefaultClient();

       CoreModuleProperties.HEARTBEAT_INTERVAL.set(client, Duration.ofSeconds(10));
       CoreModuleProperties.SOCKET_KEEPALIVE.set(client, true);

       // Handle keepalive@sish requests from the server
//       client.setGlobalRequestHandlers(Collections.singletonList((connectionService, request, wantReply, buffer) -> {
//          if ("keepalive@sish".equals(request)) {
//             return RequestHandler.Result.ReplySuccess;
//          }
//          return RequestHandler.Result.Unsupported;
//       }));

        // Load the key
        FileKeyPairProvider keyProvider = new FileKeyPairProvider(tunnelKeyFile.toPath());
        Iterable<KeyPair> keys = keyProvider.loadKeys(null);
        client.setKeyIdentityProvider(KeyIdentityProvider.wrapKeyPairs(keys));

        // Disable reading ~/.ssh/known_hosts
        client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);

        // Disable reading ~/.ssh/config
        // This prevents the client from inheriting aliases or settings from the user's config
        client.setHostConfigEntryResolver(HostConfigEntryResolver.EMPTY);

        // Enable remote forwarding
        client.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);
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
    public GatewayTunnelSession createSession(String hostname, int port, GatewayTunnelInfo tunnelInfo, Consumer<Throwable> closedCallback) {
       LOG.fine("Creating session: hostname=" + hostname + ", port=" + port + ", tunnelInfo=" + tunnelInfo);
       CompletableFuture<Void> connectFuture = new CompletableFuture<>();
       TunnelSessionManager manager = new TunnelSessionManager(hostname, port, tunnelInfo, connectFuture);
       Runnable disconnectRunnable = manager::stop;
       manager.start();
       return new GatewayTunnelSession(connectFuture, tunnelInfo, disconnectRunnable);
    }
}
