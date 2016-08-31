package org.openremote.test.admin

import com.google.gwt.junit.GWTMockUtilities
import com.google.gwt.place.shared.WithTokenizers
import com.google.gwt.user.client.ui.AcceptsOneWidget
import com.google.gwt.user.client.ui.Widget
import org.openremote.manager.client.ManagerActivityMapper
import org.openremote.manager.client.ManagerHistoryMapper
import org.openremote.manager.client.admin.*
import org.openremote.manager.client.admin.navigation.AdminNavigation
import org.openremote.manager.client.admin.navigation.AdminNavigationPresenter
import org.openremote.manager.client.admin.users.*
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
import org.openremote.manager.shared.security.*
import org.openremote.manager.shared.validation.ConstraintViolationReport
import org.openremote.test.ClientObjectMapper
import org.openremote.test.ClientTrait
import org.openremote.test.ContainerTrait
import spock.lang.Specification
import spock.util.concurrent.BlockingVariables

import static org.openremote.manager.shared.Constants.MANAGER_CLIENT_ID
import static org.openremote.manager.shared.Constants.MASTER_REALM

class AdminUsersActivityTest extends Specification implements ContainerTrait, ClientTrait {

    def "List all, create, update, delete user"() {

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

        and: "The expected result"
        def result = new BlockingVariables(10)
        result.appEvents = []

        and: "The fake client MVP environment"
        GWTMockUtilities.disarm()
        def managerMessages = Mock(ManagerMessages) {
            userCreated(_) >> {
                "TestMessageUserCreated:" + it[0]
            }
            userUpdated(_) >> {
                "TestMessageUserUpdated:" + it[0]
            }
            userDeleted(_) >> {
                "TestMessageUserDeleted:" + it[0]
            }
            roleLabel(_) >> {
                "RoleLabel:" + it[0]
            }
            passwordsMustMatch() >> {
                "TestMessagePasswordsMustMatch"
            }
            passwordUpdated() >> {
                "TestMessagePasswordUpdated" }

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

        def tenantResource = Stub(TenantResource) {
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }
        def userResource = Stub(UserResource) {
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }

        def adminUsersView = Mock(AdminUsers) {
            setTenants(_, _) >> { Tenant[] tenants, String selectedRealm ->
                result.tenants = tenants
                result.selectedRealm = selectedRealm
            }
            setUsers(_) >> {
                result.users = it[0]
            }
        }
        def tenantArrayMapper = new ClientObjectMapper(container.JSON, Tenant[].class) as TenantArrayMapper
        def userArrayMapper = new ClientObjectMapper(container.JSON, User[].class) as UserArrayMapper
        AdminUsersActivity adminUsersActivity

        def adminUserView = Mock(AdminUser)
        def userMapper = new ClientObjectMapper(container.JSON, User.class) as UserMapper
        def credentialMapper = new ClientObjectMapper(container.JSON, Credential.class) as CredentialMapper
        def roleArrayMapper = new ClientObjectMapper(container.JSON, Role[].class) as RoleArrayMapper
        AdminUserActivity adminUserActivity

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
                {},
                {},
                {},
                {
                    adminUsersActivity = new AdminUsersActivity(adminView, adminNavigationPresenter, adminUsersView, eventBus,
                    managerMessages, placeController, requestService, tenantResource, tenantArrayMapper, userResource, userArrayMapper)
                    return adminUsersActivity
                },
                {
                    adminUserActivity = new AdminUserActivity(adminView, adminNavigationPresenter, adminUserView, managerMessages,
                    placeController, eventBus, securityService, requestService, userResource, userMapper, credentialMapper, roleArrayMapper)
                    return adminUserActivity
                },
                {},
                {},
                {}
        )
        startActivityManager(activityDisplay, activityMapper, eventBus)

        when: "Navigating to a default place"
        def placeHistoryHandler = createPlaceHistoryHandler(placeController, placeHistoryMapper, new AdminUsersPlace())
        placeHistoryHandler.handleCurrentHistory()

        then: "The admin navigation view should have the right place set"
        1 * adminNavigationView.onPlaceChange(_ as AdminUsersPlace)

        and: "The activity display should be set to admin view"
        1 * activityDisplay.setWidget(adminViewWidget)

        and: "The admin view should have content set"
        1 * adminView.setContent(adminUsersView)

        and: "The admin users view should have the right activity set as presenter"
        1 * adminUsersView.setPresenter(_ as AdminUsersActivity)

        and: "The view should have received the tenants and selected realm"
        result.tenants.length == 1
        result.tenants[0].realm == MASTER_REALM
        result.tenants[0].enabled
        result.selectedRealm == null

        when: "The master tenant is selected"
        adminUsersActivity != null
        result.appEvents.clear()
        adminUsersActivity.onTenantSelected(MASTER_REALM)

        then: "The activity should be stopped"
        1 * adminUsersView.setPresenter(null)
        1 * adminView.setContent(null)

        and: "The admin users activity should be shown with the selected realm"
        result.appEvents[0] instanceof WillGoToPlaceEvent
        result.appEvents[0].place instanceof AdminUsersPlace
        result.appEvents[1] instanceof GoToPlaceEvent
        result.appEvents[1].place instanceof AdminUsersPlace
        result.appEvents[1].place.realm == MASTER_REALM

        and: "The view should have received the tenants, selected realm, and users"
        result.tenants.length == 1
        result.tenants[0].realm == MASTER_REALM
        result.tenants[0].enabled
        result.selectedRealm == MASTER_REALM
        result.users.length == 2

        when: "The user clicks Create User"
        result.appEvents.clear()
        adminUsersActivity.createUser()

        then: "The activity should be stopped"
        1 * adminUsersView.setPresenter(null)
        1 * adminView.setContent(null)

        and: "The admin user activity should be shown with the selected realm"
        result.appEvents[0] instanceof WillGoToPlaceEvent
        result.appEvents[0].place instanceof AdminUserPlace
        result.appEvents[1] instanceof GoToPlaceEvent
        result.appEvents[1].place instanceof AdminUserPlace
        result.appEvents[1].place.realm == MASTER_REALM

        and: "The admin navigation view should have the right place set"
        1 * adminNavigationView.onPlaceChange(_ as AdminUserPlace)

        and: "The activity display should be set to admin view"
        1 * activityDisplay.setWidget(adminViewWidget)

        and: "The admin view should have content set"
        1 * adminView.setContent(adminUserView)

        and: "The admin user view should have the right activity set as presenter"
        1 * adminUserView.setPresenter(_ as AdminUserActivity)

        and: "The admin user form should be cleared"
        1 * adminUserView.clearRoles()
        1 * adminUserView.clearFormMessages()
        1 * adminUserView.setUsernameError(false)
        1 * adminUserView.setFirstNameError(false)
        1 * adminUserView.setLastNameError(false)
        1 * adminUserView.setEmailError(false)
        1 * adminUserView.setUserEnabledError(false)
        1 * adminUserView.setPasswordError(false)
        1 * adminUserView.enableCreate(false)
        1 * adminUserView.enableUpdate(false)
        1 * adminUserView.enableDelete(false)
        1 * adminUserView.enableResetPassword(false)
        1 * adminUserView.enableRoles(false)
        1 * adminUserView.setUsernameEditEnabled(false)
        1 * adminUserView.setUsername(null)
        1 * adminUserView.setFirstName(null)
        1 * adminUserView.setLastName(null)
        1 * adminUserView.setEmail(null)
        1 * adminUserView.setUserEnabled(null)
        1 * adminUserView.enableCreate(true)
        1 * adminUserView.setUsernameEditEnabled(true)

        when: "The user clicks the Create button"
        adminUserActivity != null
        result.appEvents.clear()
        adminUserActivity.create()

        then: "The activity reads the form"
        1 * adminUserView.setFormBusy(true)
        1 * adminUserView.clearFormMessages()
        1 * adminUserView.setUsernameError(false)
        1 * adminUserView.setFirstNameError(false)
        1 * adminUserView.setLastNameError(false)
        1 * adminUserView.setEmailError(false)
        1 * adminUserView.setUserEnabledError(false)
        1 * adminUserView.setPasswordError(false)
        1 * adminUserView.getUsername() >> {
            "testuser"
        }
        1 * adminUserView.getFirstName() >> {
            return "Foo"
        }
        1 * adminUserView.getLastName() >> {
            return "Bar"
        }
        1 * adminUserView.getEmail() >> {
            return "invalidemail" // This will cause a constraint violation
        }
        1 * adminUserView.getUserEnabled() >> {
            return true
        }

        and: "The form errors should be shown"
        1 * adminUserView.addFormMessageError("Please provide a valid email address.")
        1 * adminUserView.setEmailError(true)
        1 * adminUserView.setFormBusy(false)

        when: "The user clicks the Create button"
        result.appEvents.clear()
        adminUserActivity.create()

        then: "The activity reads the form"
        1 * adminUserView.setFormBusy(true)
        1 * adminUserView.clearFormMessages()
        1 * adminUserView.setUsernameError(false)
        1 * adminUserView.setFirstNameError(false)
        1 * adminUserView.setLastNameError(false)
        1 * adminUserView.setEmailError(false)
        1 * adminUserView.setUserEnabledError(false)
        1 * adminUserView.setPasswordError(false)
        1 * adminUserView.getUsername() >> {
            "testuser"
        }
        1 * adminUserView.getFirstName() >> {
            return "Foo"
        }
        1 * adminUserView.getLastName() >> {
            return "Bar"
        }
        1 * adminUserView.getEmail() >> {
            return "valid@email.address"
        }
        1 * adminUserView.getUserEnabled() >> {
            return true
        }

        and: "The success toast should be shown"
        result.appEvents[0] instanceof ShowInfoEvent
        result.appEvents[0].text == "TestMessageUserCreated:testuser"

        and: "The form should be cleared, the activity stopped"
        1 * adminUserView.setFormBusy(false)
        1 * adminUserView.setPresenter(null)
        1 * adminUserView.clearRoles()
        1 * adminUserView.clearFormMessages()
        1 * adminUserView.setUsernameError(false)
        1 * adminUserView.setFirstNameError(false)
        1 * adminUserView.setLastNameError(false)
        1 * adminUserView.setEmailError(false)
        1 * adminUserView.setUserEnabledError(false)
        1 * adminUserView.setPasswordError(false)

        and: "The admin users activity should be shown"
        result.appEvents[1] instanceof WillGoToPlaceEvent
        result.appEvents[1].place instanceof AdminUsersPlace
        result.appEvents[2] instanceof GoToPlaceEvent
        result.appEvents[2].place instanceof AdminUsersPlace
        result.appEvents[2].place.realm == MASTER_REALM

        and: "The view should have received the users"
        result.users.length == 3

        when: "The new user is selected"
        result.appEvents.clear()
        def selectedUser
        result.users.each {
            if (it.username == "testuser") {
                selectedUser = it
                return
            }
        }
        selectedUser != null
        adminUsersActivity.onUserSelected(selectedUser)

        then: "The admin user activity should be shown"
        result.appEvents[0] instanceof WillGoToPlaceEvent
        result.appEvents[0].place instanceof AdminUserPlace
        result.appEvents[1] instanceof GoToPlaceEvent
        result.appEvents[1].place instanceof AdminUserPlace
        result.appEvents[1].place.realm == MASTER_REALM
        result.appEvents[1].place.userId == selectedUser.id

        and: "The admin user form should be cleared"
        1 * adminUserView.clearRoles()
        1 * adminUserView.clearFormMessages()
        1 * adminUserView.setUsernameError(false)
        1 * adminUserView.setFirstNameError(false)
        1 * adminUserView.setLastNameError(false)
        1 * adminUserView.setEmailError(false)
        1 * adminUserView.setUserEnabledError(false)
        1 * adminUserView.setPasswordError(false)
        1 * adminUserView.enableCreate(false)
        1 * adminUserView.enableUpdate(false)
        1 * adminUserView.enableDelete(false)
        1 * adminUserView.enableResetPassword(false)
        1 * adminUserView.enableRoles(false)
        1 * adminUserView.setUsernameEditEnabled(false)

        and: "The user should be loaded"
        1 * adminUserView.enableRoles(true)
        1 * adminUserView.setUsername("testuser")
        1 * adminUserView.setFirstName("Foo")
        1 * adminUserView.setLastName("Bar")
        1 * adminUserView.setEmail("valid@email.address")
        1 * adminUserView.setUserEnabled(true)

        then: "The roles should be added in the right order"
        1 * adminUserView.addRole(!null, "RoleLabel:read", true, false) >> {
            result.readRoleId = it[0]
        }

        then: "The roles should be added in the right order"
        1 * adminUserView.addRole(!null, "RoleLabel:read-assets", false, false) >> {
            result.readAssetsRoleId = it[0]
        }

        then: "The roles should be added in the right order"
        1 * adminUserView.addRole(!null, "RoleLabel:read-map", false, false) >> {
            result.readMapRoleId = it[0]
        }

        then: "The roles should be added in the right order"
        1 * adminUserView.addRole(!null, "RoleLabel:write", true, false)

        then: "The roles should be added in the right order"
        1 * adminUserView.addRole(!null, "RoleLabel:write-assets", false, false)

        and: "The form should be enabled"
        1 * adminUserView.setFormBusy(false)
        1 * adminUserView.enableCreate(false)
        1 * adminUserView.enableUpdate(true)
        1 * adminUserView.enableDelete(true)
        1 * adminUserView.enableResetPassword(true)
        1 * adminUserView.setUsernameEditEnabled(false)

        when: "The user selects a composite role"
        adminUserActivity.onRoleAssigned(result.readRoleId, true)

        then: "The other roles must be assigned"
        1 * adminUserView.toggleRoleAssigned(result.readAssetsRoleId, true)
        1 * adminUserView.toggleRoleAssigned(result.readMapRoleId, true)

        when: "The user deselects a composite role"
        adminUserActivity.onRoleAssigned(result.readRoleId, false)

        then: "The other roles must be unassigned"
        1 * adminUserView.toggleRoleAssigned(result.readAssetsRoleId, false)
        1 * adminUserView.toggleRoleAssigned(result.readMapRoleId, false)

        when: "The user selects a composite role and then deselects one of its component roles"
        adminUserActivity.onRoleAssigned(result.readRoleId, true)
        adminUserActivity.onRoleAssigned(result.readMapRoleId, false)

        then: "The composite role must be unassigned"
        1 * adminUserView.toggleRoleAssigned(result.readRoleId, false)

        when: "The user clicks the Update button"
        result.appEvents.clear()
        adminUserActivity.update()

        then: "The activity reads the tenant form"
        1 * adminUserView.getUsername() >> {
            "testuser"
        }
        1 * adminUserView.getFirstName() >> {
            return "Foo"
        }
        1 * adminUserView.getLastName() >> {
            return "Bar"
        }
        1 * adminUserView.getEmail() >> {
            return "valid@email.address"
        }
        1 * adminUserView.getUserEnabled() >> {
            return true
        }

        and: "The form success should be shown"
        1 * adminUserView.addFormMessageSuccess("TestMessageUserUpdated:testuser")

        when: "The user clicks the Update button"
        result.appEvents.clear()
        adminUserActivity.update()

        then: "The activity reads the tenant form (with invalid passwords)"
        1 * adminUserView.getUsername() >> {
            "testuser"
        }
        1 * adminUserView.getFirstName() >> {
            return "Foo"
        }
        1 * adminUserView.getLastName() >> {
            return "Bar"
        }
        1 * adminUserView.getEmail() >> {
            return "valid@email.address"
        }
        1 * adminUserView.getUserEnabled() >> {
            return true
        }
        1 * adminUserView.getPassword() >> {
            return "secret123"
        }
        1 * adminUserView.getPasswordControl() >> {
            return "secret456"
        }

        and: "The form errors should be shown"
        1 * adminUserView.addFormMessageError("TestMessagePasswordsMustMatch")
        1 * adminUserView.setPasswordError(true)
        1 * adminUserView.addFormMessageSuccess("TestMessageUserUpdated:testuser")

        when: "The user clicks the Update button"
        result.appEvents.clear()
        adminUserActivity.update()

        then: "The activity reads the tenant form (with matching passwords)"
        1 * adminUserView.getUsername() >> {
            "testuser"
        }
        1 * adminUserView.getFirstName() >> {
            return "Foo"
        }
        1 * adminUserView.getLastName() >> {
            return "Bar"
        }
        1 * adminUserView.getEmail() >> {
            return "valid@email.address"
        }
        1 * adminUserView.getUserEnabled() >> {
            return true
        }
        1 * adminUserView.getPassword() >> {
            return "secret123"
        }
        1 * adminUserView.getPasswordControl() >> {
            return "secret123"
        }

        and: "The form success should be shown"
        1 * adminUserView.addFormMessageSuccess("TestMessagePasswordUpdated")
        1 * adminUserView.addFormMessageSuccess("TestMessageUserUpdated:testuser")


        when: "The user clicks the Delete button"
        result.appEvents.clear()
        adminUserActivity.delete()

        then: "The activity shows a confirmation dialog"
        1 * adminUserView.showConfirmation(_, _, !null) >> {
            it[2].run()
        }

        and: "The activity clears the form messages"
        1 * adminUserView.setFormBusy(true)
        1 * adminUserView.clearFormMessages()
        1 * adminUserView.setUsernameError(false)
        1 * adminUserView.setFirstNameError(false)
        1 * adminUserView.setLastNameError(false)
        1 * adminUserView.setEmailError(false)
        1 * adminUserView.setUserEnabledError(false)
        1 * adminUserView.setPasswordError(false)

        and: "The success toast should be shown"
        result.appEvents[0] instanceof ShowInfoEvent
        result.appEvents[0].text == "TestMessageUserDeleted:testuser"

        and: "The form should be cleared, the activity stopped"
        1 * adminUserView.setFormBusy(false)
        1 * adminUserView.setPresenter(null)
        1 * adminUserView.clearRoles()
        1 * adminUserView.clearFormMessages()
        1 * adminUserView.setUsernameError(false)
        1 * adminUserView.setFirstNameError(false)
        1 * adminUserView.setLastNameError(false)
        1 * adminUserView.setEmailError(false)
        1 * adminUserView.setUserEnabledError(false)
        1 * adminUserView.setPasswordError(false)

        and: "The admin tenants activity should be shown"
        result.appEvents[1] instanceof WillGoToPlaceEvent
        result.appEvents[1].place instanceof AdminUsersPlace
        result.appEvents[2] instanceof GoToPlaceEvent
        result.appEvents[2].place instanceof AdminUsersPlace
        result.appEvents[2].place.realm == MASTER_REALM

        and: "The view should have received the tenants and users"
        result.tenants.length == 1
        result.tenants[0].realm == MASTER_REALM
        result.tenants[0].enabled
        result.selectedRealm == MASTER_REALM
        result.users.length == 2

        and: "The server should be stopped"
        stopContainer(container);
    }
}
