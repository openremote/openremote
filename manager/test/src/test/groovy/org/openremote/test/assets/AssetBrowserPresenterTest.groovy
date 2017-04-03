package org.openremote.test.assets

import com.google.gwt.junit.GWTMockUtilities
import com.google.gwt.user.client.ui.Widget
import com.google.gwt.view.client.HasData
import org.openremote.manager.client.Environment
import org.openremote.manager.client.admin.TenantArrayMapper
import org.openremote.manager.client.assets.AssetArrayMapper
import org.openremote.manager.client.assets.AssetMapper
import org.openremote.manager.client.assets.browser.*
import org.openremote.manager.client.i18n.ManagerMessages
import org.openremote.manager.client.service.RequestServiceImpl
import org.openremote.manager.client.service.SecurityService
import org.openremote.manager.client.style.WidgetStyle
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.shared.asset.AssetResource
import org.openremote.manager.shared.http.EntityReader
import org.openremote.manager.shared.security.Tenant
import org.openremote.manager.shared.security.TenantResource
import org.openremote.manager.shared.validation.ConstraintViolationReport
import org.openremote.model.Consumer
import org.openremote.model.Runnable
import org.openremote.model.asset.Asset
import org.openremote.model.event.shared.EventService
import org.openremote.test.ClientObjectMapper
import org.openremote.test.GwtClientTrait
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*

class AssetBrowserPresenterTest extends Specification implements ManagerContainerTrait, GwtClientTrait {

    def "Browse assets"() {

        given: "The server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)

        and: "An authenticated user and client security service"
        def realm = MASTER_REALM;
        def accessToken = authenticate(
                container,
                realm,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), SETUP_KEYCLOAK_ADMIN_PASSWORD, SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT)
        ).token
        def securityService = Stub(SecurityService) {
            getRealm() >> realm
            getToken() >> accessToken
            updateToken(_, _, _) >> { int minValiditySeconds, Consumer<Boolean> successFn, Runnable errorFn ->
                successFn.accept(true) // The token is always valid (this assumes the test doesn't run very long)
            };
            hasResourceRoleOrIsSuperUser(_, _) >> { String role, String resource ->
                return true; // TODO: Should use the parsed token
            }
            isSuperUser() >> true
        }

        and: "A client request service and target"
        def constraintViolationReader = new ClientObjectMapper(container.JSON, ConstraintViolationReport.class) as EntityReader<ConstraintViolationReport>
        def requestService = new RequestServiceImpl(securityService, constraintViolationReader)
        def clientTarget = getClientTarget(createClient(container).build(), serverUri(serverPort), realm)

        and: "the server resources to call from client"
        def assetResource = Stub(AssetResource) {
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }
        def tenantResource = Stub(TenantResource) {
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }

        and: "the data mappers for client/server calls"
        def assetMapper = new ClientObjectMapper(container.JSON, Asset.class) as AssetMapper
        def assetArrayMapper = new ClientObjectMapper(container.JSON, Asset[].class) as AssetArrayMapper
        def tenantArrayMapper = new ClientObjectMapper(container.JSON, Tenant[].class) as TenantArrayMapper

        and: "The expected result"
        def conditions = new PollingConditions(initialDelay: 2, timeout: 10)
        def collectedClientEvents = []

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
        def eventBus = createEventBus(collectedClientEvents)
        def placeController = createPlaceController(securityService, eventBus)
        def environment = Environment.create(
                securityService,
                requestService,
                Mock(EventService),
                placeController,
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
                assetArrayMapper,
                tenantResource,
                tenantArrayMapper
        )

        when: "the asset tree is created"
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
            assert rowData[0].tenant.id == keycloakDemoSetup.masterTenant.id
            assert rowData[1] instanceof TenantTreeNode
            assert rowData[1].label == "Customer A"
            assert rowData[1].tenant.id == keycloakDemoSetup.customerATenant.id
            assert rowData[2] instanceof TenantTreeNode
            assert rowData[2].label == "Customer B"
            assert rowData[2].tenant.id == keycloakDemoSetup.customerBTenant.id
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

        cleanup: "The server should be stopped"
        stopContainer(container);
    }
}
