package org.openremote.test.admin

import com.google.gwt.junit.GWTMockUtilities
import com.google.gwt.place.shared.WithTokenizers
import com.google.gwt.user.client.ui.AcceptsOneWidget
import com.google.gwt.user.client.ui.Widget
import org.openremote.app.client.Environment
import org.openremote.app.client.ManagerActivityMapper
import org.openremote.app.client.ManagerHistoryMapper
import org.openremote.app.client.admin.*
import org.openremote.app.client.admin.navigation.AdminNavigation
import org.openremote.app.client.admin.navigation.AdminNavigationPresenter
import org.openremote.app.client.admin.users.AbstractAdminUsersPlace
import org.openremote.app.client.admin.users.AdminUsers
import org.openremote.app.client.admin.users.AdminUsersActivity
import org.openremote.app.client.admin.users.AdminUsersPlace
import org.openremote.app.client.admin.users.edit.AdminUserEdit
import org.openremote.app.client.admin.users.edit.AdminUserEditActivity
import org.openremote.app.client.admin.users.edit.AdminUserEditPlace
import org.openremote.app.client.event.GoToPlaceEvent
import org.openremote.app.client.event.ShowSuccessEvent
import org.openremote.app.client.event.WillGoToPlaceEvent
import org.openremote.app.client.i18n.ManagerMessages
import org.openremote.app.client.event.EventService
import org.openremote.app.client.style.WidgetStyle
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.setup.AbstractKeycloakSetup
import org.openremote.manager.setup.SetupService
import org.openremote.model.event.Event
import org.openremote.model.event.bus.EventListener
import org.openremote.model.notification.NotificationResource
import org.openremote.model.security.*
import org.openremote.test.ClientObjectMapper
import org.openremote.test.GwtClientTrait
import org.openremote.test.ManagerContainerTrait
import org.openremote.test.TestOpenRemoteApp
import spock.lang.Specification
import spock.util.concurrent.BlockingVariables
import spock.util.concurrent.PollingConditions

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*

class AdminUsersActivityTest extends Specification implements ManagerContainerTrait, GwtClientTrait {

    def "List all, create, update, delete user"() {

        given: "The server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainerNoDemoAssets(defaultConfig(serverPort), defaultServices())
        def identityService = container.getService(ManagerIdentityService.class)
        def keycloakProvider = container.getService(SetupService.class).getTaskOfType(AbstractKeycloakSetup.class).keycloakProvider

        and: "expected results"
        def conditions = new PollingConditions(timeout: 10)
        def blockingResult = new BlockingVariables(10)
        def resultEvents = []
        def resultTenants = []
        def resultUsers = []
        def resultSelectedRealm = null

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

        and: "the server resources to call from client"
        def clientTarget = getClientApiTarget(serverUri(serverPort), realm)
        def tenantResource = Stub(TenantResource) {
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }
        def userResource = Stub(UserResource) {
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }
        def notificationResource = Stub(NotificationResource) {
            _(*_) >> { callResourceProxy(container.JSON, clientTarget, getDelegate()) }
        }

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
                "TestMessagePasswordUpdated"
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
        def placeController = createPlaceController(eventBus)
        def environment = Environment.create(
                testApp,
                Mock(EventService),
                placeController,
                placeHistoryMapper,
                eventBus,
                managerMessages,
                new WidgetStyle()
        )

        and: "The views and activities to test"
        def adminViewWidget = Mock(Widget)
        def adminView = Mock(AdminView) {
            asWidget() >> {
                adminViewWidget
            }
        }

        def adminNavigationView = Mock(AdminNavigation)
        def adminNavigationPresenter = new AdminNavigationPresenter(environment, adminNavigationView)

        def adminUsersView = Mock(AdminUsers) {
            setTenants(_, _) >> { Tenant[] tenants, String selectedRealm ->
                resultTenants = tenants
                resultSelectedRealm = selectedRealm
            }
            setUsers(_) >> {
                resultUsers = it[0]
            }
        }
        def tenantArrayMapper = new ClientObjectMapper(container.JSON, Tenant[].class) as TenantArrayMapper
        def userArrayMapper = new ClientObjectMapper(container.JSON, User[].class) as UserArrayMapper
        AdminUsersActivity adminUsersActivity

        def adminUserView = Mock(AdminUserEdit)
        def userMapper = new ClientObjectMapper(container.JSON, User.class) as UserMapper
        def credentialMapper = new ClientObjectMapper(container.JSON, Credential.class) as CredentialMapper
        def roleArrayMapper = new ClientObjectMapper(container.JSON, Role[].class) as RoleArrayMapper
        AdminUserEditActivity adminUserActivity

        and: "An activity management configuration"
        def activityDisplay = Mock(AcceptsOneWidget)
        def activityMapper = new ManagerActivityMapper(
                testApp,
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
                {},
                {},
                {
                    adminUsersActivity = new AdminUsersActivity(
                            environment, adminView, adminNavigationPresenter, adminUsersView, tenantResource, tenantArrayMapper, userResource, userArrayMapper
                    )
                    return adminUsersActivity
                },
                {
                    adminUserActivity = new AdminUserEditActivity(
                            environment, adminView, adminNavigationPresenter, adminUserView, userResource, userMapper, credentialMapper, roleArrayMapper, notificationResource
                    )
                    return adminUserActivity
                },
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
        conditions.eventually {
            assert resultTenants.length == 3
            assert resultSelectedRealm == null
        }

        when: "The master tenant is selected"
        resultEvents = []
        resultTenants = []
        resultUsers = []
        resultSelectedRealm = null
        adminUsersActivity != null
        adminUsersActivity.onTenantSelected(MASTER_REALM)

        then: "The activity should be stopped"
        1 * adminUsersView.setPresenter(null)
        1 * adminView.setContent(null)

        and: "The admin users activity should be shown with the selected realm"
        resultEvents[0] instanceof WillGoToPlaceEvent
        resultEvents[0].place instanceof AdminUsersPlace
        resultEvents[1] instanceof GoToPlaceEvent
        resultEvents[1].place instanceof AdminUsersPlace
        resultEvents[1].place.realm == MASTER_REALM

        and: "The view should have received the tenants, selected realm, and users"
        conditions.eventually {
            assert resultTenants.length == 3
            assert resultSelectedRealm == MASTER_REALM
            assert resultUsers.length == 2
        }

        when: "The user clicks Create User"
        def place = new AdminUserEditPlace(resultEvents[1].place)
        resultEvents = []
        resultTenants = []
        resultUsers = []
        resultSelectedRealm = null
        placeController.goTo(place)

        then: "The activity should be stopped"
        1 * adminUsersView.setPresenter(null)
        1 * adminView.setContent(null)

        and: "The admin user activity should be shown with the selected realm"
        conditions.eventually {
            assert resultEvents[0] instanceof WillGoToPlaceEvent
            assert resultEvents[0].place instanceof AdminUserEditPlace
            assert resultEvents[1] instanceof GoToPlaceEvent
            assert resultEvents[1].place instanceof AdminUserEditPlace
            assert resultEvents[1].place.realm == MASTER_REALM
        }

        and: "The admin navigation view should have the right place set"
        1 * adminNavigationView.onPlaceChange(_ as AdminUserEditPlace)

        and: "The activity display should be set to admin view"
        1 * activityDisplay.setWidget(adminViewWidget)

        and: "The admin view should have content set"
        1 * adminView.setContent(adminUserView)

        and: "The admin user view should have the right activity set as presenter"
        1 * adminUserView.setPresenter(_ as AdminUserEditActivity)

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
        1 * adminUserView.setEditMode(false)
        1 * adminUserView.setUsername(null)
        1 * adminUserView.setFirstName(null)
        1 * adminUserView.setLastName(null)
        1 * adminUserView.setEmail(null)
        1 * adminUserView.setUserEnabled(null)
        1 * adminUserView.enableCreate(true)

        when: "The user clicks the Create button"
        resultEvents = []
        resultTenants = []
        resultUsers = []
        resultSelectedRealm = null
        adminUserActivity != null
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
        resultEvents = []
        resultTenants = []
        resultUsers = []
        resultSelectedRealm = null
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
        conditions.eventually {
            assert resultEvents[0] instanceof ShowSuccessEvent
            assert resultEvents[0].text == "TestMessageUserCreated:testuser"
        }

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
        conditions.eventually {
            assert resultEvents[1] instanceof WillGoToPlaceEvent
            assert resultEvents[1].place instanceof AdminUsersPlace
            assert resultEvents[2] instanceof GoToPlaceEvent
            assert resultEvents[2].place instanceof AdminUsersPlace
            assert resultEvents[2].place.realm == MASTER_REALM
        }

        and: "The view should have received the users"
        conditions.eventually {
            assert resultUsers.length == 3
        }

        when: "The new user is selected"
        resultEvents = []
        resultTenants = []
        resultSelectedRealm = null
        def selectedUser
        resultUsers.each {
            if (it.username == "testuser") {
                selectedUser = it
                return
            }
        }
        selectedUser != null
        adminUsersActivity.onUserSelected(selectedUser)

        then: "The admin user activity should be shown"
        conditions.eventually {
            assert resultEvents[0] instanceof WillGoToPlaceEvent
            assert resultEvents[0].place instanceof AbstractAdminUsersPlace
            assert resultEvents[1] instanceof GoToPlaceEvent
            assert resultEvents[1].place instanceof AbstractAdminUsersPlace
            assert resultEvents[1].place.realm == MASTER_REALM
            assert resultEvents[1].place.userId == selectedUser.id
        }

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
        1 * adminUserView.setEditMode(false)
        1 * adminUserView.setEditMode(true)

        and: "The user should be loaded"
        1 * adminUserView.enableRoles(true)
        1 * adminUserView.setUsername("testuser")
        1 * adminUserView.setFirstName("Foo")
        1 * adminUserView.setLastName("Bar")
        1 * adminUserView.setEmail("valid@email.address")
        1 * adminUserView.setUserEnabled(true)

        then: "The roles should be added in the right order"
        1 * adminUserView.addRole(!null, "RoleLabel:read", true, false) >> {
            blockingResult.readRoleId = it[0]
        }

        then: "The roles should be added in the right order"
        1 * adminUserView.addRole(!null, "RoleLabel:read-assets", false, false) >> {
            blockingResult.readAssetsRoleId = it[0]
        }

        then: "The roles should be added in the right order"
        1 * adminUserView.addRole(!null, "RoleLabel:read-map", false, false) >> {
            blockingResult.readMapRoleId = it[0]
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

        when: "The user selects a composite role"
        adminUserActivity.onRoleAssigned(blockingResult.readRoleId, true)

        then: "The other roles must be assigned"
        1 * adminUserView.toggleRoleAssigned(blockingResult.readAssetsRoleId, true)
        1 * adminUserView.toggleRoleAssigned(blockingResult.readMapRoleId, true)

        when: "The user deselects a composite role"
        adminUserActivity.onRoleAssigned(blockingResult.readRoleId, false)

        then: "The other roles must be unassigned"
        1 * adminUserView.toggleRoleAssigned(blockingResult.readAssetsRoleId, false)
        1 * adminUserView.toggleRoleAssigned(blockingResult.readMapRoleId, false)

        when: "The user selects a composite role and then deselects one of its component roles"
        adminUserActivity.onRoleAssigned(blockingResult.readRoleId, true)
        adminUserActivity.onRoleAssigned(blockingResult.readMapRoleId, false)

        then: "The composite role must be unassigned"
        1 * adminUserView.toggleRoleAssigned(blockingResult.readRoleId, false)

        when: "The user clicks the Update button"
        resultEvents = []
        resultTenants = []
        resultUsers = []
        resultSelectedRealm = null
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
        resultEvents = []
        resultTenants = []
        resultUsers = []
        resultSelectedRealm = null
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
        resultEvents = []
        resultTenants = []
        resultUsers = []
        resultSelectedRealm = null
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
        resultEvents = []
        resultTenants = []
        resultUsers = []
        resultSelectedRealm = null
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
        conditions.eventually {
            assert resultEvents[0] instanceof ShowSuccessEvent
            assert resultEvents[0].text == "TestMessageUserDeleted:testuser"
        }

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
        conditions.eventually {
            assert resultEvents[1] instanceof WillGoToPlaceEvent
            assert resultEvents[1].place instanceof AdminUsersPlace
            assert resultEvents[2] instanceof GoToPlaceEvent
            assert resultEvents[2].place instanceof AdminUsersPlace
            assert resultEvents[2].place.realm == MASTER_REALM
        }

        and: "The view should have received the tenants and users"
        conditions.eventually {
            assert resultTenants.length == 3
            assert resultSelectedRealm == MASTER_REALM
            assert resultUsers.length == 2
        }

        cleanup: "The server should be stopped"
        stopContainer(container)
    }
}
