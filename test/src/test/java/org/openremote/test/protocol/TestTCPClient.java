package org.openremote.test.protocol;

import org.openremote.agent.protocol.tcp.TCPIOClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class TestTCPClient extends TCPIOClient<String> {

    public int connectAttempts;

    public TestTCPClient(String host, int port) {
        super(host, port);
    }

    @Override
    protected CompletableFuture<Void> doConnect() {
        connectAttempts++;
        return super.doConnect();
    }
}
