package org.openremote.test.admin

import com.google.gwt.junit.GWTMockUtilities
import com.google.gwt.place.shared.WithTokenizers
import com.google.gwt.user.client.ui.AcceptsOneWidget
import com.google.gwt.user.client.ui.Widget
import org.openremote.manager.client.ManagerActivityMapper
import org.openremote.manager.client.ManagerHistoryMapper
import org.openremote.manager.client.admin.AdminView
import org.openremote.manager.client.admin.TenantArrayMapper
import org.openremote.manager.client.admin.TenantMapper
import org.openremote.manager.client.admin.navigation.AdminNavigation
import org.openremote.manager.client.admin.navigation.AdminNavigationPresenter
import org.openremote.manager.client.admin.tenant.*
import org.openremote.manager.client.event.GoToPlaceEvent
import org.openremote.manager.client.event.WillGoToPlaceEvent
import org.openremote.manager.client.event.bus.EventListener
import org.openremote.manager.client.i18n.ManagerMessages
import org.openremote.manager.client.service.RequestServiceImpl
import org.openremote.manager.client.service.SecurityService
import org.openremote.manager.shared.Consumer
import org.openremote.manager.shared.Runnable
import org.openremote.manager.shared.event.Event
import org.openremote.manager.shared.event.ui.ShowInfoEvent
import org.openremote.manager.shared.http.EntityReader
import org.openremote.manager.shared.security.Tenant
import org.openremote.manager.shared.security.TenantResource
import org.openremote.manager.shared.validation.ConstraintViolationReport
import org.openremote.test.ClientObjectMapper
import org.openremote.test.ClientTrait
import org.openremote.test.ContainerTrait
import spock.lang.Specification
import spock.util.concurrent.BlockingVariables

import static org.openremote.manager.shared.Constants.MANAGER_CLIENT_ID
import static org.openremote.manager.shared.Constants.MASTER_REALM

class AdminTenantsActivityTest extends Specification implements ContainerTrait, ClientTrait {

    def "List tenants and create a tenant"() {

        given: "The server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())

        and: "An authenticated user and client security service"
        def realm = MASTER_REALM;
        def accessToken = authenticate(container, realm, MANAGER_CLIENT_ID, "admin", "admin").token
        def securityService = Stub(SecurityService) {
            getRealm() >> realm
            getToken() >> accessToken
            updateToken(_, _, _) >> { int minValiditySeconds, Consumer<Boolean> successFn, Runnable errorFn ->
                successFn.accept(true) // The token is always valid (this assumes the test doesn't run very long)
            };
            hasResourceRoleOrIsAdmin(_, _) >> { String role, String resource ->
                return true; // TODO: Should use the parsed token
            }
        }

        and: "A client request service and target"
        def constraintViolationReader = new ClientObjectMapper(container.JSON, ConstraintViolationReport.class) as EntityReader<ConstraintViolationReport>
        def requestService = new RequestServiceImpl(securityService, constraintViolationReader)
        def clientTarget = getClientTarget(createClient(container).build(), serverUri(serverPort), realm)

        and: "The expected asynchronous result"
        def result = new BlockingVariables(5)
        result.appEvents = []

        and: "The fake client MVP environment"
        GWTMockUtilities.disarm()
        def managerMessages = Mock(ManagerMessages) {
            _(*_) >> { "TestMessage" }
        }
        def eventBus = createEventBus()
        eventBus.register(null, new EventListener<Event>() {
            @Override
            void on(Event event) {
                result.appEvents += event
            }
        })
        def placeHistoryMapper = createPlaceHistoryMapper(ManagerHistoryMapper.getAnnotation(WithTokenizers.class))
        def placeController = createPlaceController(securityService, eventBus)

        and: "The views and activities to test"
        def adminViewWidget = Mock(Widget)
        def adminView = Mock(AdminView) {
            asWidget() >> {
                adminViewWidget
            }
        }

        def adminNavigationView = Mock(AdminNavigation)
        def adminNavigationPresenter = new AdminNavigationPresenter(adminNavigationView, placeHistoryMapper)
        def adminTenantsView = Mock(AdminTenants) {
            setTenants(_) >> {
                result.tenants = it[0];
            }
        }
        def tenantResource = Stub(TenantResource) {
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }

        def tenantArrayMapper = new ClientObjectMapper(container.JSON, Tenant[].class) as TenantArrayMapper
        def adminTenantsActivity = new AdminTenantsActivity(
                adminView, adminNavigationPresenter, adminTenantsView, managerMessages,
                placeController, requestService, tenantResource, tenantArrayMapper
        )

        def adminTenantView = Mock(AdminTenant) {

        }

        def tenantMapper = new ClientObjectMapper(container.JSON, Tenant.class) as TenantMapper
        def adminTenantActivity = new AdminTenantActivity(
                adminView, adminNavigationPresenter, adminTenantView, managerMessages,
                placeController, eventBus, securityService, requestService, tenantResource, tenantMapper
        )

        and: "An activity management configuration"
        def activityDisplay = Mock(AcceptsOneWidget)
        def activityMapper = new ManagerActivityMapper(
                securityService,
                eventBus,
                managerMessages,
                {},
                {},
                {},
                {},
                { return adminTenantsActivity },
                { return adminTenantActivity },
                {},
                {},
                {}
        )
        startActivityManager(activityDisplay, activityMapper, eventBus)

        when: "Navigating to the default place"
        def placeHistoryHandler = createPlaceHistoryHandler(placeController, placeHistoryMapper, new AdminTenantsPlace())
        placeHistoryHandler.handleCurrentHistory()

        then: "The admin navigation view should have the right place set"
        1 * adminNavigationView.onPlaceChange(_ as AdminTenantsPlace)

        and: "The activity display should be set to admin view"
        1 * activityDisplay.setWidget(adminViewWidget)

        and: "The admin view should have content set"
        1 * adminView.setContent(adminTenantsView)

        and: "The admin tenants view should have the right activity set as presenter"
        1 * adminTenantsView.setPresenter(adminTenantsActivity)

        and: "The view should have received the tenants"
        result.tenants.length == 1
        result.tenants[0].realm == MASTER_REALM
        result.tenants[0].enabled

        when: "The user clicks Create Tenant"
        adminTenantsActivity.createTenant()

        then: "The activity should be stopped"
        1 * adminTenantsView.setPresenter(null)
        1 * adminView.setContent(null)

        and: "The admin navigation view should have the right place set"
        1 * adminNavigationView.onPlaceChange(_ as AdminTenantPlace)

        and : "The activity display should be set to admin view"
        1 * activityDisplay.setWidget(adminViewWidget)

        and: "The admin view should have content set"
        1 * adminView.setContent(adminTenantView)

        and: "The admin tenant view should have the right activity set as presenter"
        1 * adminTenantView.setPresenter(adminTenantActivity)

        and: "The admin tenant form should be cleared"
        1 * adminTenantView.clearFormMessagesSuccess()
        1 * adminTenantView.clearFormMessagesError()
        1 * adminTenantView.setTenantDisplayNameError(false)
        1 * adminTenantView.setTenantRealmError(false)
        1 * adminTenantView.setTenantEnabledError(false)
        1 * adminTenantView.enableCreate(false)
        1 * adminTenantView.enableUpdate(false)
        1 * adminTenantView.enableDelete(false)
        1 * adminTenantView.setTenantDisplayName(null)
        1 * adminTenantView.setTenantRealm(null)
        1 * adminTenantView.setTenantEnabled(null)
        1 * adminTenantView.enableCreate(true)

        when: "The user clicks the Create button"
        result.appEvents.clear()
        adminTenantActivity.create()

        then: "The activity reads the tenant form"
        1 * adminTenantView.setFormBusy(true)
        1 * adminTenantView.clearFormMessagesSuccess()
        1 * adminTenantView.clearFormMessagesError()
        1 * adminTenantView.setTenantDisplayNameError(false)
        1 * adminTenantView.setTenantRealmError(false)
        1 * adminTenantView.setTenantEnabledError(false)
        1 * adminTenantView.getTenantDisplayName() >> {
            "Test Name"
        }
        1 * adminTenantView.getTenantRealm() >> {
            return null; // This will cause a constraint violation
        }
        1 * adminTenantView.getTenantEnabled() >> {
            return true;
        }

        and: "The form errors should be shown"
        1 * adminTenantView.addFormMessageError("Tenant realm can not be empty.")
        1 * adminTenantView.setTenantRealmError(true)
        1 * adminTenantView.setFormBusy(false)

        when: "The user clicks the Create button"
        result.appEvents.clear()
        adminTenantActivity.create()

        then: "The activity reads the tenant form"
        1 * adminTenantView.setFormBusy(true)
        1 * adminTenantView.clearFormMessagesSuccess()
        1 * adminTenantView.clearFormMessagesError()
        1 * adminTenantView.setTenantDisplayNameError(false)
        1 * adminTenantView.setTenantRealmError(false)
        1 * adminTenantView.setTenantEnabledError(false)
        1 * adminTenantView.getTenantDisplayName() >> {
            "Test Name"
        }
        1 * adminTenantView.getTenantRealm() >> {
            return "testrealm"
        }
        1 * adminTenantView.getTenantEnabled() >> {
            return true;
        }

        and: "The success toast should be shown"
        result.appEvents[0] instanceof ShowInfoEvent
        result.appEvents[0].text == "TestMessage"

        and: "The form should be cleared the activity stopped"
        1 * adminTenantView.setFormBusy(false)
        1 * adminTenantView.setPresenter(null)
        1 * adminTenantView.clearFormMessagesSuccess()
        1 * adminTenantView.clearFormMessagesError()
        1 * adminTenantView.setTenantDisplayNameError(false)
        1 * adminTenantView.setTenantRealmError(false)
        1 * adminTenantView.setTenantEnabledError(false)

        and: "The admin tenants activity should be shown"
        result.appEvents[1] instanceof WillGoToPlaceEvent
        result.appEvents[1].place instanceof AdminTenantsPlace
        result.appEvents[2] instanceof GoToPlaceEvent
        result.appEvents[2].place instanceof AdminTenantsPlace

        then : "The view should have received the tenants"
        result.tenants.length == 2

        and: "The server should be stopped"
        stopContainer(container);
    }
}
