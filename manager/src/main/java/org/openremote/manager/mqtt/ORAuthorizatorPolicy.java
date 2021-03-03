package org.openremote.manager.mqtt;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;

import io.moquette.broker.security.IAuthorizatorPolicy;
import io.moquette.broker.subscriptions.Topic;

public class ORAuthorizatorPolicy implements IAuthorizatorPolicy {

    final protected Collection<IAuthorizatorPolicy> authorizatorPolicies = new CopyOnWriteArraySet<>();

    @Override
    public boolean canRead(Topic topic, String username, String clientId) {
        return authorizatorPolicies.stream()
                .anyMatch(authorizer -> authorizer.canRead(topic, username, clientId));
    }

    @Override
    public boolean canWrite(Topic topic, String username, String clientId) {
        return authorizatorPolicies.stream()
                .anyMatch(authorizer -> authorizer.canWrite(topic, username, clientId));
    }

    public void addAuthorizatorPolicy(IAuthorizatorPolicy authorizer) {
        this.authorizatorPolicies.add(authorizer);
    }   
}
