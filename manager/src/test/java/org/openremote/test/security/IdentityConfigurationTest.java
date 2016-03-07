package org.openremote.test.security;

import org.junit.Ignore;
import org.openremote.test.ClientTest;

import java.util.logging.Logger;

@Ignore
public class IdentityConfigurationTest extends ClientTest {

    private static final Logger LOG = Logger.getLogger(IdentityConfigurationTest.class.getName());

        /*
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
            */

}