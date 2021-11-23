package org.openremote.test.assets

import org.keycloak.adapters.rotation.AdapterTokenVerifier
import org.openremote.container.security.keycloak.AccessTokenAuthContext
import org.openremote.container.timer.TimerService
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.setup.SetupService
import org.openremote.test.setup.KeycloakTestSetup
import org.openremote.test.setup.ManagerTestSetup
import org.openremote.model.asset.AssetResource
import org.openremote.model.asset.UserAssetLink
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import javax.ws.rs.WebApplicationException

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*

class AssetUserLinkingTest extends Specification implements ManagerContainerTrait {

    def "Link assets and users as superuser"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def timerService = container.getService(TimerService.class)
        def identityService = container.getService(ManagerIdentityService.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "an authenticated admin user"
        def accessTokenString = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the asset resource"
        def assetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessTokenString).proxy(AssetResource.class)
        /* ############################################## READ ####################################### */

        and: "user access tokens"
        def testUserAccessToken = authenticate(container, MASTER_REALM, KEYCLOAK_CLIENT_ID, "testuser1", "testuser1").token
        def accessToken = AdapterTokenVerifier.verifyToken(testUserAccessToken, keycloakTestSetup.getKeycloakProvider().getKeycloakDeployment(MASTER_REALM, KEYCLOAK_CLIENT_ID))
        def testUser1Token = new AccessTokenAuthContext(MASTER_REALM, accessToken)

        testUserAccessToken = authenticate(container, keycloakTestSetup.tenantBuilding.realm, KEYCLOAK_CLIENT_ID, "testuser2", "testuser2").token
        accessToken = AdapterTokenVerifier.verifyToken(testUserAccessToken, keycloakTestSetup.getKeycloakProvider().getKeycloakDeployment(keycloakTestSetup.tenantBuilding.realm, KEYCLOAK_CLIENT_ID))
        def testUser2Token = new AccessTokenAuthContext(keycloakTestSetup.tenantBuilding.realm, accessToken)

        testUserAccessToken = authenticate(container, keycloakTestSetup.tenantBuilding.realm, KEYCLOAK_CLIENT_ID, "testuser3", "testuser3").token
        accessToken = AdapterTokenVerifier.verifyToken(testUserAccessToken, keycloakTestSetup.getKeycloakProvider().getKeycloakDeployment(keycloakTestSetup.tenantBuilding.realm, KEYCLOAK_CLIENT_ID))
        def testUser3Token = new AccessTokenAuthContext(keycloakTestSetup.tenantBuilding.realm, accessToken)

        expect: "some users to be restricted"
        !identityService.getIdentityProvider().isRestrictedUser(testUser1Token)
        !identityService.getIdentityProvider().isRestrictedUser(testUser2Token)
        identityService.getIdentityProvider().isRestrictedUser(testUser3Token)

        when: "all user assets are retrieved of a realm"
        def userAssetLinks = assetResource.getUserAssetLinks(null, keycloakTestSetup.tenantBuilding.realm, null, null)

        then: "result should match"
        userAssetLinks.length == 9
        userAssetLinks.any {
            it.id.realm == keycloakTestSetup.tenantBuilding.realm &&
                    it.id.userId == keycloakTestSetup.testuser3Id &&
                    it.id.assetId == managerTestSetup.apartment1Id &&
                    it.assetName == "Apartment 1" &&
                    it.parentAssetName == "Smart building" &&
                    it.userFullName == "testuser3 (DemoA3 DemoLast)"
        }
        userAssetLinks.any {
            it.id.realm == keycloakTestSetup.tenantBuilding.realm &&
                    it.id.userId == keycloakTestSetup.testuser3Id &&
                    it.id.assetId == managerTestSetup.apartment1LivingroomId &&
                    it.assetName == "Living Room 1" &&
                    it.parentAssetName == "Apartment 1" &&
                    it.userFullName == "testuser3 (DemoA3 DemoLast)"
        }
        userAssetLinks.any {
            it.id.realm == keycloakTestSetup.tenantBuilding.realm &&
                    it.id.userId == keycloakTestSetup.testuser3Id &&
                    it.id.assetId == managerTestSetup.apartment1KitchenId &&
                    it.assetName == "Kitchen 1" &&
                    it.parentAssetName == "Apartment 1" &&
                    it.userFullName == "testuser3 (DemoA3 DemoLast)"
        }
        userAssetLinks.any {
            it.id.realm == keycloakTestSetup.tenantBuilding.realm &&
                    it.id.userId == keycloakTestSetup.testuser3Id &&
                    it.id.assetId == managerTestSetup.apartment1HallwayId &&
                    it.assetName == "Hallway 1" &&
                    it.parentAssetName == "Apartment 1" &&
                    it.userFullName == "testuser3 (DemoA3 DemoLast)"
        }
        userAssetLinks.any {
            it.id.realm == keycloakTestSetup.tenantBuilding.realm &&
                    it.id.userId == keycloakTestSetup.testuser3Id &&
                    it.id.assetId == managerTestSetup.apartment1Bedroom1Id &&
                    it.assetName == "Bedroom 1" &&
                    it.parentAssetName == "Apartment 1" &&
                    it.userFullName == "testuser3 (DemoA3 DemoLast)"
        }
        userAssetLinks.any {
            it.id.realm == keycloakTestSetup.tenantBuilding.realm &&
                    it.id.userId == keycloakTestSetup.testuser3Id &&
                    it.id.assetId == managerTestSetup.apartment1BathroomId &&
                    it.assetName == "Bathroom 1" &&
                    it.parentAssetName == "Apartment 1" &&
                    it.userFullName == "testuser3 (DemoA3 DemoLast)"
        }

        when: "all user assets are retrieved of a realm and user"
        userAssetLinks = assetResource.getUserAssetLinks(null, keycloakTestSetup.tenantBuilding.realm, keycloakTestSetup.testuser3Id, null)

        then: "result should match"
        userAssetLinks.length == 6

        when: "the realm and user don't match"
        assetResource.getUserAssetLinks(null, keycloakTestSetup.tenantCity.realm, keycloakTestSetup.testuser3Id, null)

        then: "an error response should be returned"
        WebApplicationException ex = thrown()
        ex.response.status == 400

        when: "the realm doesn't exist"
        assetResource.getUserAssetLinks(null, "doesnotexist", keycloakTestSetup.testuser3Id, null)

        then: "an error response should be returned"
        ex = thrown()
        ex.response.status == 400

        when: "all user assets are retrieved of a realm and user"
        userAssetLinks = assetResource.getUserAssetLinks(null, keycloakTestSetup.tenantBuilding.realm, null, managerTestSetup.apartment1Id)

        then: "result should match"
        userAssetLinks.length == 1
        userAssetLinks.any {
            it.id.realm == keycloakTestSetup.tenantBuilding.realm &&
                    it.id.userId == keycloakTestSetup.testuser3Id &&
                    it.id.assetId == managerTestSetup.apartment1Id &&
                    it.assetName == "Apartment 1" &&
                    it.parentAssetName == "Smart building" &&
                    it.userFullName == "testuser3 (DemoA3 DemoLast)"
        }

        when: "all user assets are retrieved of a realm and user and asset"
        userAssetLinks = assetResource.getUserAssetLinks(null, keycloakTestSetup.tenantBuilding.realm, keycloakTestSetup.testuser3Id, managerTestSetup.apartment1Id)

        then: "result should match"
        userAssetLinks.length == 1
        userAssetLinks.any {
            it.id.realm == keycloakTestSetup.tenantBuilding.realm &&
                    it.id.userId == keycloakTestSetup.testuser3Id &&
                    it.id.assetId == managerTestSetup.apartment1Id &&
                    it.assetName == "Apartment 1" &&
                    it.parentAssetName == "Smart building" &&
                    it.userFullName == "testuser3 (DemoA3 DemoLast)"
        }

        when: "all user assets are retrieved of a realm and user and asset"
        userAssetLinks = assetResource.getUserAssetLinks(null, keycloakTestSetup.tenantBuilding.realm, keycloakTestSetup.testuser2Id, managerTestSetup.apartment1Id)

        then: "result should match"
        userAssetLinks.length == 0

        /* ############################################## WRITE ####################################### */

        when: "an asset is linked to a user"
        UserAssetLink userAssetLink = new UserAssetLink(keycloakTestSetup.tenantBuilding.realm, keycloakTestSetup.testuser2Id, managerTestSetup.apartment2Id)
        assetResource.createUserAssetLinks(null, [userAssetLink])
        userAssetLinks = assetResource.getUserAssetLinks(null, keycloakTestSetup.tenantBuilding.realm, keycloakTestSetup.testuser2Id, null)

        then: "result should match"
        userAssetLinks.length == 1
        userAssetLinks.any {
            it.id.realm == keycloakTestSetup.tenantBuilding.realm &&
                    it.id.userId == keycloakTestSetup.testuser2Id &&
                    it.id.assetId == managerTestSetup.apartment2Id &&
                    it.assetName == "Apartment 2" &&
                    it.parentAssetName == "Smart building" &&
                    it.userFullName == "testuser2 (DemoA2 DemoLast)" &&
                    it.createdOn.time <= timerService.currentTimeMillis
        }

        when: "an asset link is deleted"
        assetResource.deleteUserAssetLink(null, keycloakTestSetup.tenantBuilding.realm, keycloakTestSetup.testuser2Id, managerTestSetup.apartment2Id)
        userAssetLinks = assetResource.getUserAssetLinks(null, keycloakTestSetup.tenantBuilding.realm, keycloakTestSetup.testuser2Id, null)

        then: "result should match"
        userAssetLinks.length == 0
    }
}
