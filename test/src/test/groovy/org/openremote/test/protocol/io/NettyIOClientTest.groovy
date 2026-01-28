package org.openremote.test.protocol.io


import org.apache.http.client.utils.URIBuilder
import org.openremote.agent.protocol.io.AbstractNettyIOClient
import org.openremote.manager.gateway.GatewayIOClient
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.auth.OAuthClientCredentialsGrant
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class NettyIOClientTest extends Specification implements ManagerContainerTrait {

    def "Concurrency test for reconnect logic"() {

        given: "A NettyIOClient that pauses before scheduling connection"
        // Speed up the retry interval for the test
        def oldDelay = AbstractNettyIOClient.RECONNECT_DELAY_INITIAL_MILLIS
        AbstractNettyIOClient.RECONNECT_DELAY_INITIAL_MILLIS = 10 
        
        def pauseLatch = new CountDownLatch(1)
        def startChannelCount = new AtomicInteger(0)
        
        def client = new GatewayIOClient(
                new URIBuilder("ws://127.0.0.1:$serverPort/websocket/events?Realm=random").build(),
                null,
                new OAuthClientCredentialsGrant("http://127.0.0.1:$serverPort/auth/realms/random/protocol/openid-connect/token",
                        'abcd',
                        'abcd',
                        null).setBasicAuthHeader(true)
        ) {
            @Override
            protected CompletableFuture<Void> startChannel() {
                startChannelCount.incrementAndGet()
                // Simulate failure because resources (bootstrap) are destroyed by disconnect()
                if (this.bootstrap == null) {
                    throw new NullPointerException("Bootstrap is null")
                }
                // Return a dummy future (won't be reached if exception thrown above)
                def future = CompletableFuture.failedFuture(new Exception("Fail"))
                return future
            }

            @Override
            protected void scheduleDoConnect(long initialDelay) {
                // Pause here to simulate concurrency issue during connect/reconnect.
                // We pause here after connect() has set status to CONNECTING and initialized bootstrap,
                // but BEFORE the retry future is created and assigned to 'connectRetry'.
                try {
                    pauseLatch.await()
                } catch (InterruptedException e) {
                    e.printStackTrace()
                }
                super.scheduleDoConnect(initialDelay)
            }
        }
        
        // Use a dedicated executor for the retry logic
        client.executorService = Executors.newCachedThreadPool()

        when: "Connect is called in a separate thread"
        Thread.start {
            client.connect()
        }
        
        // Wait until the client enters the connecting state (and hits the pauseLatch)
        new PollingConditions().eventually {
            assert client.connectionStatus == ConnectionStatus.CONNECTING
        }
        // Small sleep to ensure we are waiting on the latch inside scheduleDoConnect
        Thread.sleep(100)

        and: "Disconnect is called immediately by the main thread"
        // This runs while the other thread is paused. 
        // Crucially, 'connectRetry' is still null in the client, so disconnect() fails to cancel any future.
        // It destroys the bootstrap and sets status to DISCONNECTED.
        client.disconnect()

        then: "Client status is DISCONNECTED"
        client.connectionStatus == ConnectionStatus.DISCONNECTED

        when: "The paused connect thread is allowed to proceed"
        pauseLatch.countDown()

        then: "No attempt should be made to actually start the channel"
        new PollingConditions(initialDelay: 1, timeout: 5, delay: 2).eventually {
            assert startChannelCount.get() == 0
        }
        
        cleanup:
        AbstractNettyIOClient.RECONNECT_DELAY_INITIAL_MILLIS = oldDelay
        client.executorService.shutdownNow()
    }
}
