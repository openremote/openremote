package org.openremote.test;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Ignore;
import org.junit.Test;
import org.openremote.manager.server.identity.ClientInstall;

import java.util.logging.Logger;

import static com.hubrick.vertx.rest.MediaType.APPLICATION_JSON_VALUE;
import static io.vertx.core.http.HttpHeaders.ACCEPT;
import static org.openremote.manager.server.util.UrlUtil.url;

@Ignore
public class IdentityConfigurationTest extends ManagerClientTest {

    private static final Logger LOG = Logger.getLogger(IdentityConfigurationTest.class.getName());

    @Test
    public void checkInstall(TestContext tc) {
        Async async = tc.async();
        client.get(
            url("api", "identity", "install", "or-manager").toString(),
            ClientInstall.class,
            response -> {
                LOG.info("### GOT RESPONSE: " + response.getBody());
                tc.assertEquals("or-manager", response.getBody().getClientId());
                tc.assertNotNull(response.getBody().getPublicKey());
                async.complete();
            }
        ).exceptionHandler(tc::fail)
            .putHeader(ACCEPT, APPLICATION_JSON_VALUE)
            .end();
    }

}