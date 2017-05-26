package org.openremote.test.admin

import com.google.gwt.junit.GWTMockUtilities
import com.google.gwt.place.shared.WithTokenizers
import com.google.gwt.user.client.ui.AcceptsOneWidget
import com.google.gwt.user.client.ui.Widget
import org.openremote.manager.client.Environment
import org.openremote.manager.client.ManagerActivityMapper
import org.openremote.manager.client.ManagerHistoryMapper
import org.openremote.manager.client.TenantMapper
import org.openremote.manager.client.admin.AdminView
import org.openremote.manager.client.admin.TenantArrayMapper
import org.openremote.manager.client.admin.navigation.AdminNavigation
import org.openremote.manager.client.admin.navigation.AdminNavigationPresenter
import org.openremote.manager.client.admin.tenant.*
import org.openremote.manager.client.event.GoToPlaceEvent
import org.openremote.manager.client.event.ShowFailureEvent
import org.openremote.manager.client.event.ShowSuccessEvent
import org.openremote.manager.client.event.WillGoToPlaceEvent
import org.openremote.manager.client.i18n.ManagerMessages
import org.openremote.manager.client.service.RequestServiceImpl
import org.openremote.manager.client.style.WidgetStyle
import org.openremote.manager.server.security.ManagerIdentityService
import org.openremote.manager.shared.http.EntityReader
import org.openremote.manager.shared.security.Tenant
import org.openremote.manager.shared.security.TenantResource
import org.openremote.manager.shared.validation.ConstraintViolationReport
import org.openremote.model.event.Event
import org.openremote.model.event.bus.EventListener
import org.openremote.manager.client.service.EventService
import org.openremote.test.ClientObjectMapper
import org.openremote.test.ClientSecurityService
import org.openremote.test.GwtClientTrait
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD
import static org.openremote.manager.server.setup.AbstractKeycloakSetup.SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*

class AdminTenantsActivityTest extends Specification implements ManagerContainerTrait, GwtClientTrait {

    def "List all, create, update, delete tenant"() {

        given: "The server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainerNoDemoAssets(defaultConfig(serverPort), defaultServices())
        def identityService = container.getService(ManagerIdentityService.class)

        and: "expected results"
        def conditions = new PollingConditions(timeout: 10)
        def resultEvents = []
        def resultTenants = []
        def resultCreateTenantHistoryToken = null

        and: "An authenticated user and client security service"
        def realm = MASTER_REALM;
        def accessToken = {
            authenticate(
                    container,
                    realm,
                    KEYCLOAK_CLIENT_ID,
                    MASTER_REALM_ADMIN_USER,
                    getString(container.getConfig(), SETUP_KEYCLOAK_ADMIN_PASSWORD, SETUP_KEYCLOAK_ADMIN_PASSWORD_DEFAULT)
            ).token
        }
        def securityService = new ClientSecurityService(identityService.getKeycloakDeployment(realm, KEYCLOAK_CLIENT_ID), accessToken)

        and: "A client request service and target"
        def constraintViolationReader = new ClientObjectMapper(container.JSON, ConstraintViolationReport.class) as EntityReader<ConstraintViolationReport>
        def requestService = new RequestServiceImpl(securityService, constraintViolationReader)
        def clientTarget = getClientTarget(serverUri(serverPort), realm)

        and: "The fake client MVP environment"
        GWTMockUtilities.disarm()
        def managerMessages = Mock(ManagerMessages) {
            tenantCreated(_) >> {
                "TestMessageTenantCreated:" + it[0]
            }
            tenantUpdated(_) >> {
                "TestMessageTenantUpdated:" + it[0]
            }
            tenantDeleted(_) >> {
                "TestMessageTenantDeleted:" + it[0]
            }
            requestFailed(_) >> {
                "TestMessageRequestFailed:" + it[0]
            }
            conflictRequest() >> {
                "TestMessageConflictRequest"
            }
        }
        def eventBus = createEventBus()
        eventBus.register(null, new EventListener<Event>() {
            @Override
            void on(Event event) {
                resultEvents << event
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

        def tenantResource = Stub(TenantResource) {
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }

        def adminTenantsView = Mock(AdminTenants) {
            setTenants(_) >> {
                resultTenants = it[0];
            }
            setCreateTenantHistoryToken(_) >> {
                resultCreateTenantHistoryToken = it[0]
            }
        }
        def tenantArrayMapper = new ClientObjectMapper(container.JSON, Tenant[].class) as TenantArrayMapper
        AdminTenantsActivity adminTenantsActivity

        def adminTenantView = Mock(AdminTenant)
        def tenantMapper = new ClientObjectMapper(container.JSON, Tenant.class) as TenantMapper
        AdminTenantActivity adminTenantActivity

        and: "An activity management configuration"
        def environment = Environment.create(
                securityService,
                requestService,
                Mock(EventService),
                placeController,
                placeHistoryMapper,
                eventBus,
                managerMessages,
                new WidgetStyle()
        )
        def activityDisplay = Mock(AcceptsOneWidget)
        def activityMapper = new ManagerActivityMapper(
                securityService,
                eventBus,
                managerMessages,
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {},
                {
                    adminTenantsActivity = new AdminTenantsActivity(
                            environment, adminView, adminNavigationPresenter, adminTenantsView, tenantResource, tenantArrayMapper
                    )
                    return adminTenantsActivity
                },
                {
                    adminTenantActivity = new AdminTenantActivity(
                            environment, adminView, adminNavigationPresenter, adminTenantView, tenantResource, tenantMapper
                    )
                    return adminTenantActivity
                },
                {},
                {},
                {},
        )
        startActivityManager(activityDisplay, activityMapper, eventBus)

        when: "Navigating to a default place"
        def placeHistoryHandler = createPlaceHistoryHandler(placeController, placeHistoryMapper, new AdminTenantsPlace())
        placeHistoryHandler.handleCurrentHistory()

        then: "The admin navigation view should have the right place set"
        1 * adminNavigationView.onPlaceChange(_ as AdminTenantsPlace)

        and: "The activity display should be set to admin view"
        1 * activityDisplay.setWidget(adminViewWidget)

        and: "The admin view should have content set"
        1 * adminView.setContent(adminTenantsView)

        and: "The admin tenants view should have the right activity set as presenter"
        1 * adminTenantsView.setPresenter(_ as AdminTenantsActivity)

        and: "The view should have received the tenants"
        conditions.eventually {
            assert resultTenants.length == 4
        }

        when: "The user clicks Create Tenant"
        placeController.goTo(placeHistoryMapper.getPlace(resultCreateTenantHistoryToken))

        then: "The activity should be stopped"
        1 * adminTenantsView.setPresenter(null)
        1 * adminView.setContent(null)

        and: "The admin navigation view should have the right place set"
        1 * adminNavigationView.onPlaceChange(_ as AdminTenantPlace)

        and: "The activity display should be set to admin view"
        1 * activityDisplay.setWidget(adminViewWidget)

        and: "The admin view should have content set"
        1 * adminView.setContent(adminTenantView)

        and: "The admin tenant view should have the right activity set as presenter"
        1 * adminTenantView.setPresenter(_ as AdminTenantActivity)

        and: "The admin tenant form should be cleared"
        1 * adminTenantView.clearFormMessages()
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
        resultEvents = []
        adminTenantActivity != null
        adminTenantActivity.create()

        then: "The activity reads the tenant form"
        1 * adminTenantView.setFormBusy(true)
        1 * adminTenantView.clearFormMessages()
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
        resultEvents = []
        adminTenantActivity.create()

        then: "The activity reads the tenant form"
        1 * adminTenantView.setFormBusy(true)
        1 * adminTenantView.clearFormMessages()
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
        conditions.eventually {
            assert resultEvents[0] instanceof ShowSuccessEvent
            assert resultEvents[0].text == "TestMessageTenantCreated:Test Name"
        }

        and: "The form should be cleared the activity stopped"
        1 * adminTenantView.setFormBusy(false)
        1 * adminTenantView.setPresenter(null)
        1 * adminTenantView.clearFormMessages();
        1 * adminTenantView.setTenantDisplayNameError(false)
        1 * adminTenantView.setTenantRealmError(false)
        1 * adminTenantView.setTenantEnabledError(false)

        and: "The admin tenants activity should be shown"
        conditions.eventually {
            assert resultEvents[1] instanceof WillGoToPlaceEvent
            assert resultEvents[1].place instanceof AdminTenantsPlace
            assert resultEvents[2] instanceof GoToPlaceEvent
            assert resultEvents[2].place instanceof AdminTenantsPlace
        }

        and: "The view should have received the tenants"
        conditions.eventually {
            assert resultTenants.length == 5
        }

        when: "The new tenant is selected"
        resultEvents = []
        def selectedTenant
        resultTenants.each {
            if (it.realm == "testrealm") {
                selectedTenant = it
                return
            }
        }
        adminTenantsActivity.onTenantSelected(selectedTenant)

        then: "The admin tenant activity should be shown"
        conditions.eventually {
            assert resultEvents[0] instanceof WillGoToPlaceEvent
            assert resultEvents[0].place instanceof AdminTenantPlace
            assert resultEvents[1] instanceof GoToPlaceEvent
            assert resultEvents[1].place instanceof AdminTenantPlace
            assert resultEvents[1].place.realm == "testrealm"
        }

        and: "The admin tenant form should be cleared"
        1 * adminTenantView.clearFormMessages()
        1 * adminTenantView.setTenantDisplayNameError(false)
        1 * adminTenantView.setTenantRealmError(false)
        1 * adminTenantView.setTenantEnabledError(false)
        1 * adminTenantView.enableCreate(false)
        1 * adminTenantView.enableUpdate(false)
        1 * adminTenantView.enableDelete(false)

        and: "The tenant should be loaded"
        1 * adminTenantView.setTenantDisplayName("Test Name")
        1 * adminTenantView.setTenantRealm("testrealm")
        1 * adminTenantView.setTenantEnabled(true)
        1 * adminTenantView.setFormBusy(false)
        1 * adminTenantView.enableCreate(false)
        1 * adminTenantView.enableUpdate(true)
        1 * adminTenantView.enableDelete(true)

        when: "The user clicks the Update button"
        resultEvents = []
        adminTenantActivity.update()

        then: "The activity reads the tenant form"
        1 * adminTenantView.setFormBusy(true)
        1 * adminTenantView.clearFormMessages()
        1 * adminTenantView.setTenantDisplayNameError(false)
        1 * adminTenantView.setTenantRealmError(false)
        1 * adminTenantView.setTenantEnabledError(false)
        1 * adminTenantView.getTenantDisplayName() >> {
            "Test Name2"
        }
        1 * adminTenantView.getTenantRealm() >> {
            return "master" // Note: This should be a conflict
        }
        1 * adminTenantView.getTenantEnabled() >> {
            return true;
        }

        and: "The conflict toast should be shown"
        conditions.eventually {
            assert resultEvents[0] instanceof ShowFailureEvent
            assert resultEvents[0].text == "TestMessageRequestFailed:TestMessageConflictRequest"
        }

        when: "The user clicks the Update button"
        resultEvents = []
        adminTenantActivity.update()

        then: "The activity reads the tenant form"
        1 * adminTenantView.setFormBusy(true)
        1 * adminTenantView.clearFormMessages()
        1 * adminTenantView.setTenantDisplayNameError(false)
        1 * adminTenantView.setTenantRealmError(false)
        1 * adminTenantView.setTenantEnabledError(false)
        1 * adminTenantView.getTenantDisplayName() >> {
            "Test Name2"
        }
        1 * adminTenantView.getTenantRealm() >> {
            return "testrealm2"
        }
        1 * adminTenantView.getTenantEnabled() >> {
            return true;
        }

        and: "The form success should be shown"
        1 * adminTenantView.setFormBusy(false)
        1 * adminTenantView.addFormMessageSuccess("TestMessageTenantUpdated:Test Name2")

        when: "The user clicks the Delete button"
        resultEvents = []
        adminTenantActivity.delete()

        then: "The activity shows a confirmation dialog"
        1 * adminTenantView.showConfirmation(_, _, !null) >> {
            it[2].run()
        }

        and: "The activity clears the tenant form messages"
        1 * adminTenantView.setFormBusy(true)
        1 * adminTenantView.clearFormMessages()
        1 * adminTenantView.setTenantDisplayNameError(false)
        1 * adminTenantView.setTenantRealmError(false)
        1 * adminTenantView.setTenantEnabledError(false)

        and: "The success toast should be shown"
        conditions.eventually {
            assert resultEvents[0] instanceof ShowSuccessEvent
            assert resultEvents[0].text == "TestMessageTenantDeleted:Test Name2"
        }

        and: "The form should be cleared the activity stopped"
        1 * adminTenantView.setFormBusy(false)
        1 * adminTenantView.setPresenter(null)
        1 * adminTenantView.clearFormMessages()
        1 * adminTenantView.setTenantDisplayNameError(false)
        1 * adminTenantView.setTenantRealmError(false)
        1 * adminTenantView.setTenantEnabledError(false)

        and: "The admin tenants activity should be shown"
        conditions.eventually {
            assert resultEvents[1] instanceof WillGoToPlaceEvent
            assert resultEvents[1].place instanceof AdminTenantsPlace
            assert resultEvents[2] instanceof GoToPlaceEvent
            assert resultEvents[2].place instanceof AdminTenantsPlace
        }

        and: "The view should have received the tenants"
        conditions.eventually {
            resultTenants.length == 4
        }

        cleanup: "The server should be stopped"
        stopContainer(container);
    }
}
