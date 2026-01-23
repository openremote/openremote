package org.openremote.manager.gateway;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.hosts.HostConfigEntryResolver;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.loader.KeyPairResourceLoader;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.openremote.model.gateway.GatewayTunnelInfo;
import org.openremote.model.gateway.GatewayTunnelStartRequestEvent;
import org.openremote.model.syslog.SyslogCategory;

import java.io.File;
import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.GATEWAY;

public class MINAGatewayTunnelFactory implements GatewayTunnelFactory {

    private static final Logger LOG = SyslogCategory.getLogger(GATEWAY, MINAGatewayTunnelFactory.class.getName());
    protected File tunnelKeyFile;
    protected String localhostRewrite;
    protected SshClient client;
    protected Map<GatewayTunnelInfo, ClientSession> sessionMap = new ConcurrentHashMap<>();

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
    public void startTunnel(GatewayTunnelStartRequestEvent startRequestEvent) throws Exception {
        ClientSession session = null;

        try {
            LOG.fine("Connecting to " + startRequestEvent.getSshHostname() + ":" + startRequestEvent.getSshPort());
            ConnectFuture connectFuture = client.connect("root", startRequestEvent.getSshHostname(), startRequestEvent.getSshPort()).verify();
            LOG.fine("Connected");

            session = connectFuture.getSession();

            session.addSessionListener(new SessionListener() {
                @Override
                public void sessionClosed(Session session) {
                    // This is called in all closure situations
                }

                @Override
                public void sessionException(Session session, Throwable t) {
                    LOG.info("Session error: " + t.getMessage());
                    reconnect(startRequestEvent);
                }
            });

            session.auth().verify();
            sessionMap.put(startRequestEvent.getInfo(), session);
        } catch (Exception e) {
            LOG.warning("Failed to connect to gateway: " + startRequestEvent.getSshHostname() + ":" + startRequestEvent.getSshPort() + " msg=" + e.getMessage());
            if (session != null) {
                session.close(true);
            }
            throw e;
        }
    }

    protected void reconnect(GatewayTunnelStartRequestEvent startRequestEvent) {
        // Check session is still in the session map (i.e. is still wanted)
    }

    @Override
    public void stopTunnel(GatewayTunnelInfo tunnelInfo) throws Exception {
        ClientSession session = sessionMap.remove(tunnelInfo);
        if (session != null) {
            session.close(true);
        }
    }

    @Override
    public void stopAllInRealm(String realm) {
        sessionMap.entrySet().removeIf(entry -> {
            boolean matches = entry.getKey().getRealm().equalsIgnoreCase(realm);
            if (matches) {
                try {
                    entry.getValue().close(true);
                } catch (Exception ignored) {
                }
            }
            return matches;
        });
    }

    @Override
    public void stopAll() {
        try {
            sessionMap.values()
                    .forEach(session -> {
                        try {
                            session.close(true);
                        } catch (Exception ignored) {
                        }
                    });
        } finally {
            sessionMap.clear();
        }
    }
}
