package org.openremote.manager.gateway;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionListener;
import org.openremote.model.gateway.GatewayTunnelInfo;
import org.openremote.model.gateway.GatewayTunnelStartRequestEvent;

import java.io.File;

public class MINAGatewayTunnelFactory implements GatewayTunnelFactory {

    protected File tunnelKeyFile;
    protected String localhostRewrite;
    protected SshClient client;

    public MINAGatewayTunnelFactory(File tunnelKeyFile, String localhostRewrite) {
        this.tunnelKeyFile = tunnelKeyFile;
        this.localhostRewrite = localhostRewrite;
        client = SshClient.setUpDefaultClient();

        FileKeyPairProvider keyProvider = new FileKeyPairProvider(tunnelKeyFile.toPath());
        client.setKeyIdentityProvider(keyProvider);
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
            session = client.connect(null, startRequestEvent.getSshHostname(), startRequestEvent.getSshPort()).verify().getSession();
            session.auth().verify();

            session.addSessionListener(new SessionListener() {
                @Override
                public void sessionClosed(Session session) {
                    System.out.println("Session closed. Triggering reconnection...");
                    reconnect(); // Your custom reconnection logic
                }

                @Override
                public void sessionException(Session session, Throwable t) {
                    System.err.println("Session error: " + t.getMessage());
                }
            });

        } catch (Exception e) {
            if (session != null) {
                session.close(true);
            }
        }
    }

    @Override
    public void stopTunnel(GatewayTunnelInfo tunnelInfo) throws Exception {

    }

    @Override
    public void stopAllInRealm(String realm) {

    }

    @Override
    public void stopAll() {

    }
}
