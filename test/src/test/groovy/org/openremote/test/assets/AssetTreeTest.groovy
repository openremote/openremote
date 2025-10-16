package org.openremote.test.assets

import org.openremote.manager.setup.SetupService
import org.openremote.manager.event.ClientEventService
import org.openremote.model.asset.AssetResource
import org.openremote.model.asset.AssetTreeEvent
import org.openremote.model.asset.ReadAssetTreeEvent
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.RealmPredicate
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*

class AssetTreeTest extends Specification implements ManagerContainerTrait {

    def "Get asset tree data via resource"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

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
        assert assetTree != null

        and: "the asset tree should contain 10 assets"
        assert assetTree.getAssets().size() == 10

        and: "has more should be true, since there are more than 10 assets"
        assert assetTree.hasMore() == true

        when: "the next part of the asset tree is requested with a limit of 10 and offset of 10"
        assetTree = assetResource.queryAssetTree(null,
                new AssetQuery()
                        .realm(new RealmPredicate(managerTestSetup.realmBuildingName))
                        .limit(10)
                        .offset(10)
        )

        then: "the asset tree should be returned"
        assert assetTree != null

        and: "the asset tree should contain assets"
        assert assetTree.getAssets().size() > 0

        and: "has more should be false, since there are no more assets to fetch"
        assert assetTree.hasMore() == false

        when: "we get the asset tree to test the hasChildren flag"
        assetTree = assetResource.queryAssetTree(null,
                new AssetQuery()
                        .realm(new RealmPredicate(managerTestSetup.realmBuildingName))
        )

        then: "the asset tree should be returned"
        assert assetTree != null
        assert assetTree.getAssets().size() > 0

        and: "the asset tree should contain assets"
        assert assetTree.getAssets().size() > 0

        and: "the hasChildren flag should be set correctly"        
        def livingRoom1 = assetTree.getAssets().find { asset -> 
            asset.id == managerTestSetup.apartment1LivingroomId
        }
        if (livingRoom1) {
            assert !livingRoom1.hasChildren()
        }

        def smartBuilding = assetTree.getAssets().find { asset -> 
            asset.id == managerTestSetup.smartBuildingId
        }
        if (smartBuilding) {
            assert smartBuilding.hasChildren()
        }
    }

    def "Get asset tree data via events"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def clientEventService = container.getService(ClientEventService.class)
        def conditions = new PollingConditions(delay: 0.2, timeout: 5)


        when: "we subscribe to asset tree events"
        List<SharedEvent> events = new CopyOnWriteArrayList<>()
        Consumer<SharedEvent> eventConsumer = { assetTreeEvent ->
            events.add(assetTreeEvent)
        }
        clientEventService.addSubscription(AssetTreeEvent.class, null, eventConsumer)

        then: "the subscription should be in place"
        assert events.size() == 0

        when: "we send a read asset tree event"
        def readAssetTreeEvent = new ReadAssetTreeEvent(new AssetQuery()
            .realm(new RealmPredicate(managerTestSetup.realmBuildingName))
            .limit(3))
        
        // Set the response consumer
        readAssetTreeEvent.setResponseConsumer({ event -> 
            clientEventService.publishEvent(event)
        })

        // Publish the read asset tree event
        clientEventService.publishEvent(readAssetTreeEvent)

        def assetTree = null

        then: "the asset tree event should be received"
        conditions.eventually {
            assert events.size() == 1
            assert events.get(0) instanceof AssetTreeEvent
            def assetTreeEvent = events.get(0) as AssetTreeEvent
            assetTree = assetTreeEvent.getAssetTree()
        }

        and: "the asset tree should contain 3 assets"
        assert assetTree.getAssets().size() == 3

        and: "the hasMore flag should be true since there are more assets to fetch"
        assert assetTree.hasMore() == true

        events.clear() // Clear the events list

        when: "we send a read asset tree event with a limit of 3 and offset of 3"
        readAssetTreeEvent = new ReadAssetTreeEvent(new AssetQuery()
            .realm(new RealmPredicate(managerTestSetup.realmBuildingName))
            .limit(3)
            .offset(3))
        
        // Set the response consumer
        readAssetTreeEvent.setResponseConsumer({ event -> 
            clientEventService.publishEvent(event)
        })

        // Publish the read asset tree event
        clientEventService.publishEvent(readAssetTreeEvent)

        def assetTree2 = null

        then: "the asset tree event should be received"
        conditions.eventually {
            assert events.size() == 1
            assert events.get(0) instanceof AssetTreeEvent
            def assetTreeEvent = events.get(0) as AssetTreeEvent
            assetTree2 = assetTreeEvent.getAssetTree()
        }

        and: "the asset tree should contain 3 assets"
        assert assetTree2.getAssets().size() == 3

        and: "the hasMore flag should be true since there are more assets to fetch"
        assert assetTree2.hasMore() == true

        events.clear() // Clear the events list

        when: "we send a read asset tree event to test the hasChildren flag"
        readAssetTreeEvent = new ReadAssetTreeEvent(new AssetQuery()
            .realm(new RealmPredicate(managerTestSetup.realmBuildingName)))
        
        // Set the response consumer
        readAssetTreeEvent.setResponseConsumer({ event -> 
            clientEventService.publishEvent(event)
        })

        // Publish the read asset tree event
        clientEventService.publishEvent(readAssetTreeEvent)

        def assetTree3 = null

        then: "the asset tree event should be received with correct hasChildren flags"
        conditions.eventually {
            assert events.size() == 1
            assert events.get(0) instanceof AssetTreeEvent
            def assetTreeEvent = events.get(0) as AssetTreeEvent
            assetTree3 = assetTreeEvent.getAssetTree()
            assert assetTree3.getAssets().size() > 0

            and: "the asset tree should contain assets"
            assert assetTree3.getAssets().size() > 0

            and: "the hasMore flag should be false since we queried without a limit"
            assert assetTree3.hasMore() == false
            

            and: "the hasChildren flag should be set correctly"
            // Check hasChildren flag for a room asset (should not have children)
            def livingRoom1 = assetTree3.getAssets().find { asset -> 
                asset.id == managerTestSetup.apartment1LivingroomId
            }
            if (livingRoom1) {
                assert !livingRoom1.hasChildren()
            }

            // Check hasChildren flag for a building asset (should have children)
            def smartBuilding = assetTree3.getAssets().find { asset -> 
                asset.id == managerTestSetup.smartBuildingId
            }
            if (smartBuilding) {
                assert smartBuilding.hasChildren()
            }
        }
    }
}
