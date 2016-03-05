package org.openremote.test;

public abstract class ServerTest extends IntegrationTest {
/*
    protected int ephemeralPort;

    @Override
    public void setUp(TestContext context) {
        super.setUp(context);

        this.ephemeralPort = findEphemeralPort();

        DeploymentOptions options = new DeploymentOptions().setConfig(
            new JsonObject().put(NETWORK_WEBSERVER_PORT, ephemeralPort)
        );

        vertx.deployVerticle(
            ServerVerticle.class.getName(),
            options,
            context.asyncAssertSuccess()
        );
    }

    protected Integer findEphemeralPort() {
        try {
            ServerSocket socket = new ServerSocket(0, 0, Inet4Address.getLocalHost());
            int port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
*/
}
