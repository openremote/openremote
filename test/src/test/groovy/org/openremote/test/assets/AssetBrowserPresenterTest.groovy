package org.openremote.test.assets

import com.google.gwt.junit.GWTMockUtilities
import com.google.gwt.place.shared.WithTokenizers
import com.google.gwt.user.client.ui.Widget
import com.google.gwt.view.client.HasData
import org.openremote.app.client.Environment
import org.openremote.app.client.ManagerHistoryMapper
import org.openremote.app.client.admin.TenantArrayMapper
import org.openremote.app.client.assets.AssetArrayMapper
import org.openremote.app.client.assets.AssetMapper
import org.openremote.app.client.assets.AssetQueryMapper
import org.openremote.app.client.assets.browser.*
import org.openremote.app.client.event.SubscriptionFailureEvent
import org.openremote.app.client.i18n.ManagerMessages
import org.openremote.app.client.style.WidgetStyle
import org.openremote.manager.asset.AssetStorageService

import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.setup.AbstractKeycloakSetup
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.*
import org.openremote.model.event.shared.SharedEvent
import org.openremote.model.event.shared.TenantFilter
import org.openremote.model.query.AssetQuery
import org.openremote.model.security.Tenant
import org.openremote.model.security.TenantResource
import org.openremote.test.*
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.event.ClientEventService.WEBSOCKET_EVENTS
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*

class AssetBrowserPresenterTest extends Specification implements ManagerContainerTrait, GwtClientTrait {

    def "Browse assets as superuser"() {

        given: "The server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def identityService = container.getService(ManagerIdentityService.class)
        def keycloakProvider = container.getService(SetupService.class).getTaskOfType(AbstractKeycloakSetup.class).keycloakProvider
        def assetStorageService = container.getService(AssetStorageService.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)

        and: "an authenticated user"
        def realm = MASTER_REALM
        def accessToken = {
            authenticate(
                    container,
                    realm,
                    KEYCLOAK_CLIENT_ID,
                    MASTER_REALM_ADMIN_USER,
                    getString(container.getConfig(), SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT)
            ).token
        }

        and: "a test client app"
        def testApp = new TestOpenRemoteApp(
                keycloakProvider.getKeycloakDeployment(realm, KEYCLOAK_CLIENT_ID),
                identityService.getIdentityProvider().getTenant(realm),
                accessToken
        )

        and: "a client websocket connection and attached event bus and service"
        List<SharedEvent> collectedSharedEvents = []
        def eventBus = createEventBus(collectedSharedEvents)
        def clientEventService = new ClientEventService(eventBus, container.JSON)
        connect(createWebsocketClient(), clientEventService.endpoint, serverUri(serverPort), WEBSOCKET_EVENTS, realm, accessToken.call())

        and: "the server resources to call from client"
        def clientTarget = getClientApiTarget(serverUri(serverPort), realm)
        def assetResource = Stub(AssetResource) {
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }
        def tenantResource = Stub(TenantResource) {
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }

        and: "the data mappers for client/server calls"
        def assetMapper = new ClientObjectMapper(container.JSON, Asset.class) as AssetMapper
        def assetQueryMapper = new ClientObjectMapper(container.JSON, AssetQuery.class) as AssetQueryMapper
        def assetArrayMapper = new ClientObjectMapper(container.JSON, Asset[].class) as AssetArrayMapper
        def tenantArrayMapper = new ClientObjectMapper(container.JSON, Tenant[].class) as TenantArrayMapper

        and: "The expected result"
        def conditions = new PollingConditions(initialDelay: 1, timeout: 10)

        and: "The fake client MVP environment"
        GWTMockUtilities.disarm()
        def managerMessages = Mock(ManagerMessages) {
            loadingAssets() >> {
                "TestMessageLoadingAssets"
            }
            requestFailed(_) >> {
                "TestMessageRequestFailed:" + it[0]
            }
        }
        def placeController = createPlaceController(eventBus)
        def placeHistoryMapper = createPlaceHistoryMapper(ManagerHistoryMapper.getAnnotation(WithTokenizers.class))
        def environment = Environment.create(
                testApp,
                clientEventService,
                placeController,
                placeHistoryMapper,
                eventBus,
                managerMessages,
                new WidgetStyle()
        )

        and: "The view and presenter to test"
        def assetBrowserWidget = Mock(Widget)
        def assetBrowser = Mock(AssetBrowser) {
            asWidget() >> {
                assetBrowserWidget
            }
        }
        def rootTreeNode = new RootTreeNode()
        List<BrowserTreeNode> treeDisplayRowData = null
        HasData<BrowserTreeNode> treeDisplay = Mock(HasData)
        def assetBrowserPresenter = new AssetBrowserPresenter(
                environment,
                assetBrowser,
                assetResource,
                assetMapper,
                assetQueryMapper,
                assetArrayMapper,
                tenantResource,
                tenantArrayMapper
        )

        when: "the view is attached"
        assetBrowserPresenter.onViewAttached()

        and: "the asset tree is created"
        assetBrowserPresenter.loadNodeChildren(rootTreeNode, treeDisplay)

        then: "the loading message should be displayed"
        1 * treeDisplay.setRowData(*_) >> { arguments ->
            assert arguments[0] == 0
            def rowData = arguments[1]
            treeDisplayRowData = rowData
            assert rowData.size() == 1
            assert rowData[0] instanceof LabelTreeNode
            assert rowData[0].label == "TestMessageLoadingAssets"
        }
        1 * treeDisplay.setRowCount(1, true)

        then: "the tenants should be displayed"
        1 * treeDisplay.setRowData(*_) >> { arguments ->
            assert arguments[0] == 0
            def rowData = arguments[1]
            treeDisplayRowData = rowData
            assert rowData.size() == 3
            assert rowData[0] instanceof TenantTreeNode
            assert rowData[0].label == "Master"
            assert rowData[0].icon == "group"
            assert rowData[0].id == keycloakDemoSetup.masterTenant.realm
            assert rowData[1] instanceof TenantTreeNode
            assert rowData[1].label == "Building"
            assert rowData[1].id == keycloakDemoSetup.tenantBuilding.realm
            assert rowData[2] instanceof TenantTreeNode
            assert rowData[2].label == "Smart City"
            assert rowData[2].id == keycloakDemoSetup.tenantCity.realm
        }
        1 * treeDisplay.setRowCount(3, true)

        when: "a tenant tree node is expanded"
        assetBrowserPresenter.loadNodeChildren(treeDisplayRowData[0], treeDisplay)

        then: "the assets should be displayed"
        1 * treeDisplay.setRowData(*_) >> { arguments ->
            assert arguments[0] == 0
            def rowData = arguments[1]
            treeDisplayRowData = rowData
            assert rowData.size() == 1
            assert rowData[0] instanceof AssetTreeNode
            assert rowData[0].label == "Smart Office"
            assert rowData[0].asset.id == managerDemoSetup.smartOfficeId
        }
        1 * treeDisplay.setRowCount(1, true)

        when: "an asset is modified in the database"
        def asset = assetStorageService.find(managerDemoSetup.smartOfficeId)
        asset.setName("Testname123")
        assetStorageService.merge(asset)

        then: "a tree modified event should be received from the server"
        conditions.eventually {
            assert collectedSharedEvents.size() == 1
            assert collectedSharedEvents[0] instanceof AssetTreeModifiedEvent
            assert collectedSharedEvents[0].realm == keycloakDemoSetup.masterTenant.realm
            assert collectedSharedEvents[0].assetId == managerDemoSetup.smartOfficeId
        }

        then: "the asset tree should be refreshed"
        1 * assetBrowser.refresh(managerDemoSetup.smartOfficeId)

        when: "an asset is created in the database"
        collectedSharedEvents.clear()
        asset = new Asset("My Test Asset", AssetType.THING, asset)
        asset = assetStorageService.merge(asset)

        then: "a tree modified event should be received from the server"
        conditions.eventually {
            assert collectedSharedEvents.size() == 2
            // The inserted new asset
            assert collectedSharedEvents.any {
                (it instanceof AssetTreeModifiedEvent
                        && it.realm == keycloakDemoSetup.masterTenant.realm
                        && it.assetId == asset.id
                        && !it.newAssetChildren)
            }
            // The parent which has a new child asset
            assert collectedSharedEvents.any {
                (it instanceof AssetTreeModifiedEvent
                        && it.realm == keycloakDemoSetup.masterTenant.realm
                        && it.assetId == managerDemoSetup.smartOfficeId
                        && it.newAssetChildren)
            }
        }

        then: "the asset tree should be refreshed"
        1 * assetBrowser.refresh(asset.id, managerDemoSetup.smartOfficeId)

        cleanup: "the client should be stopped"
        if (clientEventService != null) clientEventService.close()

        and: "the server should be stopped"
        stopContainer(container)
    }

    def "Browse assets as testuser1"() {

        given: "The server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def identityService = container.getService(ManagerIdentityService.class)
        def keycloakProvider = container.getService(SetupService.class).getTaskOfType(AbstractKeycloakSetup.class).keycloakProvider
        def assetStorageService = container.getService(AssetStorageService.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)

        and: "an authenticated user"
        def realm = MASTER_REALM
        def accessToken = {
            authenticate(
                    container,
                    realm,
                    KEYCLOAK_CLIENT_ID,
                    "testuser1",
                    "testuser1"
            ).token
        }

        and: "a test client app"
        def testApp = new TestOpenRemoteApp(
                keycloakProvider.getKeycloakDeployment(realm, KEYCLOAK_CLIENT_ID),
                identityService.getIdentityProvider().getTenant(realm),
                accessToken
        )

        and: "a client websocket connection and attached event bus and service"
        List<SharedEvent> collectedSharedEvents = []
        def eventBus = createEventBus(collectedSharedEvents)
        def clientEventService = new ClientEventService(eventBus, container.JSON)
        connect(createWebsocketClient(), clientEventService.endpoint, serverUri(serverPort), WEBSOCKET_EVENTS, realm, accessToken.call())

        and: "the server resources to call from client"
        def clientTarget = getClientApiTarget(serverUri(serverPort), realm)
        def assetResource = Stub(AssetResource) {
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }
        def tenantResource = Stub(TenantResource) {
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }

        and: "the data mappers for client/server calls"
        def assetMapper = new ClientObjectMapper(container.JSON, Asset.class) as AssetMapper
        def assetQueryMapper = new ClientObjectMapper(container.JSON, AssetQuery.class) as AssetQueryMapper
        def assetArrayMapper = new ClientObjectMapper(container.JSON, Asset[].class) as AssetArrayMapper
        def tenantArrayMapper = new ClientObjectMapper(container.JSON, Tenant[].class) as TenantArrayMapper

        and: "The expected result"
        def conditions = new PollingConditions(initialDelay: 1, timeout: 10)

        and: "The fake client MVP environment"
        GWTMockUtilities.disarm()
        def managerMessages = Mock(ManagerMessages) {
            loadingAssets() >> {
                "TestMessageLoadingAssets"
            }
            requestFailed(_) >> {
                "TestMessageRequestFailed:" + it[0]
            }
        }
        def placeController = createPlaceController(eventBus)
        def placeHistoryMapper = createPlaceHistoryMapper(ManagerHistoryMapper.getAnnotation(WithTokenizers.class))
        def environment = Environment.create(
                testApp,
                clientEventService,
                placeController,
                placeHistoryMapper,
                eventBus,
                managerMessages,
                new WidgetStyle()
        )

        and: "The view and presenter to test"
        def assetBrowserWidget = Mock(Widget)
        def assetBrowser = Mock(AssetBrowser) {
            asWidget() >> {
                assetBrowserWidget
            }
        }
        def rootTreeNode = new RootTreeNode()
        List<BrowserTreeNode> treeDisplayRowData = null
        HasData<BrowserTreeNode> treeDisplay = Mock(HasData)
        def assetBrowserPresenter = new AssetBrowserPresenter(
                environment,
                assetBrowser,
                assetResource,
                assetMapper,
                assetQueryMapper,
                assetArrayMapper,
                tenantResource,
                tenantArrayMapper
        )

        when: "the view is attached"
        assetBrowserPresenter.onViewAttached()

        and: "the asset tree is created"
        assetBrowserPresenter.loadNodeChildren(rootTreeNode, treeDisplay)

        then: "the loading message should be displayed"
        1 * treeDisplay.setRowData(*_) >> { arguments ->
            assert arguments[0] == 0
            def rowData = arguments[1]
            treeDisplayRowData = rowData
            assert rowData.size() == 1
            assert rowData[0] instanceof LabelTreeNode
            assert rowData[0].label == "TestMessageLoadingAssets"
        }
        1 * treeDisplay.setRowCount(1, true)

        then: "the root assets of the realm should be displayed"
        1 * treeDisplay.setRowData(*_) >> { arguments ->
            assert arguments[0] == 0
            def rowData = arguments[1]
            treeDisplayRowData = rowData
            assert rowData.size() == 1
            assert rowData[0] instanceof AssetTreeNode
            assert rowData[0].label == "Smart Office"
            assert rowData[0].icon == "office-building"
            assert rowData[0].asset.id == managerDemoSetup.smartOfficeId
        }
        1 * treeDisplay.setRowCount(1, true)

        when: "an asset tree node is expanded"
        assetBrowserPresenter.loadNodeChildren(treeDisplayRowData[0], treeDisplay)

        then: "the assets should be displayed"
        1 * treeDisplay.setRowData(*_) >> { arguments ->
            assert arguments[0] == 0
            def rowData = arguments[1]
            treeDisplayRowData = rowData
            assert rowData.size() == 1
            assert rowData[0] instanceof AssetTreeNode
            assert rowData[0].label == "Ground Floor"
            assert rowData[0].icon == "stairs"
            assert rowData[0].asset.id == managerDemoSetup.groundFloorId
        }
        1 * treeDisplay.setRowCount(1, true)

        when: "an asset is modified in the database"
        def asset = assetStorageService.find(managerDemoSetup.groundFloorId)
        asset.setName("Testname123")
        assetStorageService.merge(asset)

        then: "a tree modified event should be received from the server"
        conditions.eventually {
            assert collectedSharedEvents.size() == 1
            assert collectedSharedEvents[0] instanceof AssetTreeModifiedEvent
            assert collectedSharedEvents[0].realm == keycloakDemoSetup.masterTenant.realm
            assert collectedSharedEvents[0].assetId == managerDemoSetup.groundFloorId
        }

        then: "the asset tree should be refreshed"
        1 * assetBrowser.refresh(managerDemoSetup.groundFloorId)

        when: "the client tries to subscribe to events in a foreign realm"
        collectedSharedEvents.clear()
        clientEventService.subscribe(
                AssetTreeModifiedEvent.class,
                new TenantFilter(keycloakDemoSetup.tenantBuilding.realm)
        )

        then: "the server should return a failure"
        conditions.eventually {
            assert collectedSharedEvents.size() == 1
            assert collectedSharedEvents[0] instanceof SubscriptionFailureEvent
            assert collectedSharedEvents[0].failedEventType == AssetTreeModifiedEvent.getEventType(AssetTreeModifiedEvent.class)
        }

        cleanup: "the client should be stopped"
        if (clientEventService != null) clientEventService.close()

        and: "the server should be stopped"
        stopContainer(container)
    }

    def "Browse assets as testuser3"() {

        given: "The server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def identityService = container.getService(ManagerIdentityService.class)
        def keycloakProvider = container.getService(SetupService.class).getTaskOfType(AbstractKeycloakSetup.class).keycloakProvider
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)

        and: "an authenticated user"
        def realm = keycloakDemoSetup.tenantBuilding.realm
        def accessToken = {
            authenticate(
                    container,
                    realm,
                    KEYCLOAK_CLIENT_ID,
                    "testuser3",
                    "testuser3"
            ).token
        }

        and: "a test client app"
        def testApp = new TestOpenRemoteApp(
                keycloakProvider.getKeycloakDeployment(realm, KEYCLOAK_CLIENT_ID),
                identityService.getIdentityProvider().getTenant(realm),
                accessToken
        )

        and: "a client websocket connection and attached event bus and service"
        List<SharedEvent> collectedSharedEvents = []
        def eventBus = createEventBus(collectedSharedEvents)
        def clientEventService = new ClientEventService(eventBus, container.JSON)
        connect(createWebsocketClient(), clientEventService.endpoint, serverUri(serverPort), WEBSOCKET_EVENTS, realm, accessToken.call())

        and: "the server resources to call from client"
        def clientTarget = getClientApiTarget(serverUri(serverPort), realm)
        def assetResource = Stub(AssetResource) {
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }
        def tenantResource = Stub(TenantResource) {
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }

        and: "the data mappers for client/server calls"
        def assetMapper = new ClientObjectMapper(container.JSON, Asset.class) as AssetMapper
        def assetQueryMapper = new ClientObjectMapper(container.JSON, AssetQuery.class) as AssetQueryMapper
        def assetArrayMapper = new ClientObjectMapper(container.JSON, Asset[].class) as AssetArrayMapper
        def tenantArrayMapper = new ClientObjectMapper(container.JSON, Tenant[].class) as TenantArrayMapper

        and: "The expected result"
        def conditions = new PollingConditions(initialDelay: 1, timeout: 10)

        and: "The fake client MVP environment"
        GWTMockUtilities.disarm()
        def managerMessages = Mock(ManagerMessages) {
            loadingAssets() >> {
                "TestMessageLoadingAssets"
            }
            requestFailed(_) >> {
                "TestMessageRequestFailed:" + it[0]
            }
        }
        def placeController = createPlaceController(eventBus)
        def placeHistoryMapper = createPlaceHistoryMapper(ManagerHistoryMapper.getAnnotation(WithTokenizers.class))
        def environment = Environment.create(
                testApp,
                clientEventService,
                placeController,
                placeHistoryMapper,
                eventBus,
                managerMessages,
                new WidgetStyle()
        )

        and: "The view and presenter to test"
        def assetBrowserWidget = Mock(Widget)
        def assetBrowser = Mock(AssetBrowser) {
            asWidget() >> {
                assetBrowserWidget
            }
        }
        def assetBrowserPresenter = new AssetBrowserPresenter(
                environment,
                assetBrowser,
                assetResource,
                assetMapper,
                assetQueryMapper,
                assetArrayMapper,
                tenantResource,
                tenantArrayMapper
        )

        when: "the view is attached"
        assetBrowserPresenter.onViewAttached()

        // TODO Need to render asset list for restricted users without trees and subscribing to tree mods
        then: "the server should return a subscription failure"
        conditions.eventually {
            assert collectedSharedEvents.size() == 1
            assert collectedSharedEvents[0] instanceof SubscriptionFailureEvent
            assert collectedSharedEvents[0].failedEventType == AssetTreeModifiedEvent.getEventType(AssetTreeModifiedEvent.class)
        }

        cleanup: "the client should be stopped"
        if (clientEventService != null) clientEventService.close()

        and: "the server should be stopped"
        stopContainer(container)
    }
}
