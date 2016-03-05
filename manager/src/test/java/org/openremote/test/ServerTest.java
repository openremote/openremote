package org.openremote.test;

import org.openremote.container.Container;
import org.openremote.manager.server.SampleDataService;

import java.net.Inet4Address;
import java.net.ServerSocket;

public abstract class ServerTest extends IntegrationTest {

    protected int ephemeralPort;
    protected Container container;

    @Override
    public void setUp() {
        super.setUp();

        this.ephemeralPort = findEphemeralPort();

        container = new Container(new SampleDataService());
    }

    @Override
    public void tearDown() {
        if (container != null)
            container.stop();
        super.tearDown();
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
}
