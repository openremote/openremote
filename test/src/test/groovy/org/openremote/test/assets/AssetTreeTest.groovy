package org.openremote.test.assets

import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.WebApplicationException
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetResource
import org.openremote.model.asset.impl.BuildingAsset
import org.openremote.model.asset.impl.RoomAsset
import org.openremote.model.attribute.*
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.ParentPredicate
import org.openremote.model.query.filter.RealmPredicate
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*
import static org.openremote.model.value.MetaItemType.*
import static org.openremote.model.value.ValueType.BOOLEAN
import static org.openremote.model.value.ValueType.NUMBER

class AssetTreeTest extends Specification implements ManagerContainerTrait {

    def "Query asset tree tests"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def conditions = new PollingConditions(delay: 0.2, timeout: 5)

        and: "an authenticated user exists"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the asset resource is available"
        def assetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)

        when: "the asset tree is requested with a limit of 10 and offset of 0"
        def assetTree = assetResource.queryAssetTree(null,
                new AssetQuery()
                        .realm(new RealmPredicate(managerTestSetup.realmBuildingName))
                        .limit(10)
                        .offset(0)
        )

        then: "the asset tree should be returned"
        assetTree.getAssets().size() == 10
        assetTree.getLimit() == 10
        assetTree.getOffset() == 0

        and: "has more should be true, since there are more than 10 assets"
        assetTree.hasMore() == true

        when: "when the next part of the asset tree is requested with a limit of 10 and offset of 10"
        assetTree = assetResource.queryAssetTree(null,
                new AssetQuery()
                        .realm(new RealmPredicate(managerTestSetup.realmBuildingName))
                        .limit(10)
                        .offset(10)
        )

        then: "the asset tree should be returned"
        assetTree.getAssets().size() > 0
        assetTree.getLimit() == 10
        assetTree.getOffset() == 10

        and: "has more should be false, since there are no assets outside the limit bounds"
        assetTree.hasMore() == false

        // TODO has children related tests
    }
}
