package org.openremote.manager.mqtt;

import jakarta.security.enterprise.AuthenticationException;
import jakarta.ws.rs.client.Client;
import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager5;
import org.apache.activemq.artemis.spi.core.security.jaas.NoCacheLoginException;
import org.openremote.container.security.IdentityService;
import org.openremote.container.security.TokenPrincipal;
import org.openremote.manager.security.RemotingConnectionPrincipal;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Set;
import java.util.concurrent.*;

import static org.openremote.container.web.WebTargetBuilder.createClient;

/**
 * An {@link ActiveMQSecurityManager5} implementation that authenticates the user by either retrieving an access token
 * on behalf of the service user or validating the supplied access token
 */
public class ActiveMQSecurityManager2 implements ActiveMQSecurityManager5 {

    protected static final int CONNECTION_POOL_SIZE = 50;
    protected static final long CONNECTION_TIMEOUT_MILLIS = 10000;
    protected final ExecutorService executorService;
    protected final IdentityService identityService;
    protected final Client client;

    public ActiveMQSecurityManager2(ExecutorService executorService, IdentityService identityService) {
        this.executorService = executorService;
        this.identityService = identityService;
        client = createClient(executorService, CONNECTION_POOL_SIZE, CONNECTION_TIMEOUT_MILLIS, null);
    }

    @Override
    public Subject authenticate(String user, String password, RemotingConnection remotingConnection, String securityDomain) throws NoCacheLoginException {

        if (remotingConnection.getSubject() != null) {
            return remotingConnection.getSubject();
        }

        try {
            // TODO: Add support for bearer token authentication https://github.com/openremote/openremote/issues/2534
            // Login service user
            String realm = null;
            if (user != null) {
                int delimIndex = user.indexOf(':');
                if (delimIndex > 0) {
                    realm = user.substring(0, delimIndex);
                    user = user.substring(delimIndex + 1);
                }
            }

            if (realm == null) {
                throw new IllegalArgumentException("Invalid user format: " + user);
            }

            String bearerToken = identityService.getBearerToken(realm, user, password).get(
                CONNECTION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            TokenPrincipal tokenPrincipal = identityService.verify(realm, bearerToken);
            Principal connectionPrincipal = new RemotingConnectionPrincipal(remotingConnection);
            Subject subject = new Subject(true, Set.of(tokenPrincipal, connectionPrincipal), Set.of(), Set.of());

            // Set subject here so any code that calls this method behaves like a normal ActiveMQ SecurityStoreImpl::authenticate call
            remotingConnection.setSubject(subject);

            return subject;
        } catch (InterruptedException | TimeoutException | ExecutionException | AuthenticationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean authorize(Subject subject, Set<Role> roles, CheckType checkType, String address) {

        return false;
    }

    @Override
    public boolean validateUser(String user, String password) {
        throw new UnsupportedOperationException("Invoke validateUser(String, String, RemotingConnection, String) instead");
    }

    @Override
    public boolean validateUserAndRole(String user, String password, Set<Role> roles, CheckType checkType) {
        throw new UnsupportedOperationException("Invoke validateUserAndRole(String, String, Set<Role>, CheckType, String, RemotingConnection, String) instead");
    }
}
