package org.openremote.manager.mqtt;

import io.moquette.broker.security.IAuthorizatorPolicy;
import io.moquette.broker.subscriptions.Topic;
import org.openremote.container.web.ClientRequestInfo;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.asset.Asset;
import org.openremote.model.query.AssetQuery;

import java.util.logging.Logger;

public class KeycloakAuthorizatorPolicy implements IAuthorizatorPolicy {

    private static final Logger LOG = Logger.getLogger(KeycloakAuthorizatorPolicy.class.getName());

    protected final ManagerKeycloakIdentityProvider identityProvider;
    protected final AssetStorageService assetStorageService;

    public KeycloakAuthorizatorPolicy(ManagerKeycloakIdentityProvider identityProvider, AssetStorageService assetStorageService) {
        this.identityProvider = identityProvider;
        this.assetStorageService = assetStorageService;
    }

    @Override
    public boolean canWrite(Topic topic, String realm, String clientId) {
        return false;
    }

    @Override
    public boolean canRead(Topic topic, String realm, String clientId) {
        if (topic.isEmpty() || topic.getTokens().size() > 2) {
            LOG.info("Topic may not be empty and should have the following format: asset/{assetId}");
            return false;
        }

        if (!topic.headToken().toString().equals("asset")) {
            LOG.info("Topic should have the following format: asset/{assetId}");
            return false;
        }

        String assetId = topic.getTokens().get(1).toString();
        Asset asset = assetStorageService.find(assetId, true, AssetQuery.Access.PROTECTED);
        if (asset == null) {
            LOG.info("Asset not found");
            return false;
        }

        return true;
    }

    private ClientRequestInfo getClientRequestInfo() {
        String accessToken = identityProvider.getAdminAccessToken(null);
        return new ClientRequestInfo(null, accessToken);
    }
}
