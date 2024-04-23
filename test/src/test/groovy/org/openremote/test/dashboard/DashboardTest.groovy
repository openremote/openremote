package org.openremote.test.dashboard

import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.AssetResource
import org.openremote.model.asset.UserAssetLink
import org.openremote.model.asset.impl.LightAsset
import org.openremote.model.attribute.MetaItem
import org.openremote.model.dashboard.*
import org.openremote.model.query.DashboardQuery
import org.openremote.model.query.filter.RealmPredicate
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.MetaItemType
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.model.Constants.*

class DashboardTest extends Specification implements ManagerContainerTrait {

    def "Test dashboard creation and access rights"() {

        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "all users are logged in"
        def adminUserAccessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token
        def privateUser1AccessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                "testuser1",
                "testuser1"
        ).token

        and: "the dashboard resource"
        def serverUri = serverUri(serverPort)
        def adminUserDashboardResource = getClientApiTarget(serverUri, MASTER_REALM, adminUserAccessToken).proxy(DashboardResource.class)
        def privateUser1DashboardResource = getClientApiTarget(serverUri, MASTER_REALM, privateUser1AccessToken).proxy(DashboardResource.class)
        def publicUserDashboardResource = getClientApiTarget(serverUri, MASTER_REALM).proxy(DashboardResource.class)
        def adminUserAssetResource = getClientApiTarget(serverUri, MASTER_REALM, adminUserAccessToken).proxy(AssetResource.class)

        /* ----------------------- */

        // Test to verify dashboards can be CREATED and FETCHED by different users.

        when: "a dashboard is created in the authenticated realm"
        DashboardScreenPreset[] screenPresets = Arrays.asList(new DashboardScreenPreset("preset1", DashboardScalingPreset.KEEP_LAYOUT)).toArray() as DashboardScreenPreset[]
        Dashboard temp1 = new Dashboard(MASTER_REALM, "dashboard1", screenPresets)
        adminUserDashboardResource.create(null, temp1)
        Dashboard temp2 = new Dashboard(MASTER_REALM, "dashboard2", screenPresets).setOwnerId(keycloakTestSetup.testuser1Id)
        privateUser1DashboardResource.create(null, temp2)

        then: "requesting the dashboard should return the same object"
        def adminDashboards = adminUserDashboardResource.getAllRealmDashboards(null, MASTER_REALM)
        def adminDashboard = adminDashboards[0]
        def privateUser1Dashboard = adminDashboards[1]
        assert adminDashboards.length == 2
        def userDashboards = privateUser1DashboardResource.getAllRealmDashboards(null, MASTER_REALM)
        assert userDashboards.length == 2
        assert adminDashboard.id == userDashboards[0].id
        assert privateUser1Dashboard.id == userDashboards[1].id

        /* ----------------------- */

        // Test to verify user can query by displayName

        when: "admin user tries to query dashboard by display name"
        def displayNameQuery = new DashboardQuery().names("dashboard1")
        def displayNameDashboards = adminUserDashboardResource.query(null, displayNameQuery)

        then: "the correct dashboard should be returned"
        assert displayNameDashboards.length == 1
        assert displayNameDashboards[0].displayName == "dashboard1"

        /* ----------------------- */

        // Test to verify "EDIT ACCESS" logic

        when: "dashboard1 is made 'edit private' by its original owner"
        privateUser1Dashboard.setEditAccess(DashboardAccess.PRIVATE)
        assert privateUser1DashboardResource.update(null, privateUser1Dashboard) != null

        and: "dashboard1 is actually private"
        def dashboards = privateUser1DashboardResource.getAllRealmDashboards(null, MASTER_REALM)
        assert dashboards.length == 2
        assert dashboards.find((d) -> d.id == privateUser1Dashboard.id).editAccess == DashboardAccess.PRIVATE

        and: "the first dashboard cannot be edited by a different user, even if its admin user"
        privateUser1Dashboard.setDisplayName("another displayname")
        adminUserDashboardResource.update(null, privateUser1Dashboard)

        then: "it should throw exception"
        thrown NotFoundException

        /* ----------------------- */

        // Test to verify "DELETE" logic

        when: "admin user tries to delete a dashboard he doesn't own"
        adminUserDashboardResource.delete(null, MASTER_REALM, privateUser1Dashboard.id)

        then: "it should throw exception"
        thrown NotFoundException

        /* ----------------------- */

        // Test to verify public/unauthenticated user cannot fetch dashboards by default

        when: "dashboards are requested by a public user"
        def publicDashboards1 = publicUserDashboardResource.getAllRealmDashboards(null, MASTER_REALM)

        then: "it should return an empty array"
        assert publicDashboards1.length == 0

        /* ----------------------- */

        // Test to verify public dashboards CAN be fetched by public/unauthenticated users

        when: "dashboard1 is made 'view public' by its original owner"
        privateUser1Dashboard.setViewAccess(DashboardAccess.PUBLIC)
        assert privateUser1DashboardResource.update(null, privateUser1Dashboard) != null

        then: "when a public user fetches all dashboards, it should only return that dashboard"
        def publicDashboards2 = publicUserDashboardResource.getAllRealmDashboards(null, MASTER_REALM)
        assert publicDashboards2.length == 1
        assert publicDashboards2[0].viewAccess == DashboardAccess.PUBLIC
        assert publicDashboards2[0].editAccess == DashboardAccess.PRIVATE

        /* ----------------------- */

        // Test to verify private dashboards cannot be fetched by public/unauthenticated users, even if ID is specified

        when: "when a public user specifically fetches a private dashboard"
        publicUserDashboardResource.get(null, MASTER_REALM, adminDashboard.id)

        then: "it should throw exception"
        thrown ForbiddenException

        /* ------------------------- */

        // Test to check whether dashboards are queried when no assets are accessible

        when: "a new asset is created in the building realm"
        def lightAsset = new LightAsset("admin light").setRealm(keycloakTestSetup.realmBuilding.name)
        lightAsset.getAttribute("brightness").ifPresent {
            it -> it.addMeta(new MetaItem<>(MetaItemType.ACCESS_RESTRICTED_READ))
        }
        def adminLightAsset = adminUserAssetResource.create(null, lightAsset)

        and: "a dashboard is created in the building realm using that asset"
        def gridItem = new DashboardGridItem().setX(0).setY(0).setW(1).setH(1)
        def widgetConfigStr = ('{"attributeRefs":[{"id":"assetId","name":"brightness"}]}').replace("assetId", adminLightAsset.id)
        def widget = new DashboardWidget()
                .setDisplayName("Light widget")
                .setWidgetTypeId("test")
                .setGridItem(gridItem)
                .setWidgetConfig(ValueUtil.parse(widgetConfigStr, Object.class).orElse(null))
        def template = new DashboardTemplate(screenPresets)
                .setWidgets(new DashboardWidget[]{ widget })
        def buildingDashboard = new Dashboard()
                .setRealm(keycloakTestSetup.realmBuilding.name)
                .setDisplayName("Dashboard with assets")
                .setTemplate(template)

        def newDashboard = adminUserDashboardResource.create(null, buildingDashboard)

        and: "a restricted user authenticates for the building realm"
        def restrictedUser1AccessToken = authenticate(
                container,
                keycloakTestSetup.realmBuilding.name,
                KEYCLOAK_CLIENT_ID,
                "testuser3",
                "testuser3"
        ).token
        def restrictedUserDashboardResource = getClientApiTarget(serverUri, keycloakTestSetup.realmBuilding.name, restrictedUser1AccessToken).proxy(DashboardResource.class)

        and: "a restricted user tries to query a dashboard without access to the assets shown"
        def restrictedDashboards = restrictedUserDashboardResource.getAllRealmDashboards(null, keycloakTestSetup.realmBuilding.name)

        then: "it should only return that dashboard"
        assert restrictedDashboards.length == 0

        when: "the user gets access to the asset"
        def assetStorageService = container.getService(AssetStorageService.class)
        assetStorageService.storeUserAssetLinks(Arrays.asList(
                new UserAssetLink(keycloakTestSetup.realmBuilding.getName(), keycloakTestSetup.testuser3Id, adminLightAsset.getId())
        ))

        then: "the user is able to request the dashboard"
        def restrictedDashboards2 = restrictedUserDashboardResource.getAllRealmDashboards(null, keycloakTestSetup.realmBuilding.name)
        assert restrictedDashboards2.length == 1
        assert restrictedDashboards2[0].displayName == "Dashboard with assets"


        /* ------------------------- */

        // Test for cross realm requests
        when: "the admin superuser in master realm tries requesting a dashboard in the building realm"
        def dashboardOtherRealmQuery = new DashboardQuery().realm(new RealmPredicate(keycloakTestSetup.realmBuilding.name))
        def dashboardsInOtherRealm = adminUserDashboardResource.query(null, dashboardOtherRealmQuery)

        then: "it returns the building realm dashboard"
        assert dashboardsInOtherRealm.length == 1
        assert dashboardsInOtherRealm[0].displayName == "Dashboard with assets"

        when: "the private user in master realm tries requesting a dashboard in the building realm"
        def sameDashboardOtherRealmQuery = new DashboardQuery().realm(new RealmPredicate(keycloakTestSetup.realmBuilding.name))
        def sameDashboardsInOtherRealm = privateUser1DashboardResource.query(null, sameDashboardOtherRealmQuery)

        then: "no dashboard should be returned"
        assert sameDashboardsInOtherRealm.length == 0



        /* ------------------------- */

        // Test to delete the dashboard

        when: "the admin user deletes the new dashboard"
        adminUserDashboardResource.delete(null, keycloakTestSetup.realmBuilding.getName(), newDashboard.getId())

        and: "tries to get the dashboard again"
        adminUserDashboardResource.get(null, keycloakTestSetup.realmBuilding.getName(), newDashboard.getId())

        then: "it cannot be found, since it got deleted"
        thrown NotFoundException

        when: "the restricted user tries to fetch the deleted dashboard"
        restrictedUserDashboardResource.get(null, keycloakTestSetup.realmBuilding.getName(), newDashboard.getId())

        then: "it cannot be found as well, since it got deleted"
        thrown NotFoundException
    }
}
