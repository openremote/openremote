package org.openremote.manager.mqtt;

import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager5;
import org.apache.activemq.artemis.spi.core.security.jaas.NoCacheLoginException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.openremote.container.Container;
import org.openremote.manager.security.RemotingConnectionPrincipal;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.openremote.container.web.WebTargetBuilder.createClient;

/**
 * An {@link ActiveMQSecurityManager5} implementation that authenticates the user by either retrieving an access token
 * on behalf of the service user or validating the supplied access token
 */
public class ActiveMQSecurityManager2 implements ActiveMQSecurityManager5 {

    protected static final int CONNECTION_POOL_SIZE = 10;
    protected static final long CONNECTION_TIMEOUT_MILLIS = 10000;
    protected final ExecutorService executorService;
    protected final AtomicReference<ResteasyClient> resteasyClient = new AtomicReference<>();

    public ActiveMQSecurityManager2(ExecutorService executorService) {
        this.executorService = executorService;
    }

    protected ResteasyClient getResteasyClient() {
        synchronized (resteasyClient) {
            if (resteasyClient.get() == null) {
                resteasyClient.set(
                    createClient(Container.EXECUTOR,
                        CONNECTION_POOL_SIZE,
                        CONNECTION_TIMEOUT_MILLIS,
                        (resteasyClientBuilder ->
                            // As OAuth will hit the same endpoint a lot we want the full pool to be used
                            resteasyClientBuilder.maxPooledPerRoute(CONNECTION_POOL_SIZE))));
            }
            return resteasyClient.get();
        }
    }

    @Override
    public Subject authenticate(String user, String password, RemotingConnection remotingConnection, String securityDomain) throws NoCacheLoginException {

        if (remotingConnection.getSubject() != null) {
            return remotingConnection.getSubject();
        }

        // TODO: Add support for bearer token authentication https://github.com/openremote/openremote/issues/2534
        // Login user
        String realm = null;
        if (user != null) {
            String[] realmAndUsername = user.split(":");
            if (realmAndUsername.length == 2) {
                realm = realmAndUsername[0];
                user = realmAndUsername[1];
            }
        }


        if (subject != null) {
            // Set subject here so any code that calls this method behaves like a normal ActiveMQ SecurityStoreImpl::authenticate call
            remotingConnection.setSubject(subject);
            subject.getPrincipals().add(new RemotingConnectionPrincipal(remotingConnection));
        }

        // Store the subject in the connection
        try {
            return remotingConnection.getSubject() != null ? remotingConnection.getSubject() : getAuthenticatedSubject(user, password, remotingConnection, securityDomain);
        } catch (LoginException e) {
            return null;
        }
        return null;
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
