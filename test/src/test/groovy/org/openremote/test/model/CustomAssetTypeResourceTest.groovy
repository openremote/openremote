package org.openremote.test.model

import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.asset.CustomAssetTypeStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.model.Constants
import org.openremote.model.asset.CustomAssetTypeAttributeDefinition
import org.openremote.model.asset.CustomAssetTypeDefinition
import org.openremote.model.asset.CustomAssetTypeResource
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.util.MapAccess
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.ValueType
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER

class CustomAssetTypeResourceTest extends Specification implements ManagerContainerTrait {

    @Shared
    static CustomAssetTypeResource superAdminResource

    @Shared
    static CustomAssetTypeResource regularUserResource

    @Shared
    static CustomAssetTypeStorageService customAssetTypeStorageService

    @Shared
    static AssetStorageService assetStorageService

    @Shared
    static KeycloakTestSetup keycloakTestSetup

    def setupSpec() {
        def container = startContainer(defaultConfig(), defaultServices())
        keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        customAssetTypeStorageService = container.getService(CustomAssetTypeStorageService.class)
        assetStorageService = container.getService(AssetStorageService.class)

        def superAdminAccessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                MapAccess.getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        )

        def regularUserAccessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                "testuser1",
                "testuser1"
        )

        superAdminResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, superAdminAccessToken).proxy(CustomAssetTypeResource.class)
        regularUserResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, regularUserAccessToken).proxy(CustomAssetTypeResource.class)
    }

    def "super admin can manage custom asset type definitions"() {
        given:
        def typeName = "ApiManagedBoilerAsset"

        when:
        def created = superAdminResource.create(null, validDefinition(typeName))

        then:
        created.name == typeName
        ValueUtil.getAssetInfo(typeName).isPresent()

        when:
        def listed = superAdminResource.getAll(null)
        def found = superAdminResource.get(null, typeName)

        then:
        listed*.name.contains(typeName)
        found.name == typeName
        superAdminResource.getUsage(null, typeName) == 0

        when:
        found.attributes = found.attributes + [
                attribute("pressure", "number").setOptional(true)
        ]
        def updated = superAdminResource.update(null, typeName, found)

        then:
        updated.attributes*.name.contains("pressure")
        ValueUtil.getAssetInfo(typeName).get().attributeDescriptors.containsKey("pressure")

        when:
        superAdminResource.delete(null, typeName)

        then:
        customAssetTypeStorageService.find(typeName) == null
        ValueUtil.getAssetInfo(typeName).isEmpty()
    }

    def "validate checks definitions without persisting them"() {
        given:
        def typeName = "ApiValidationOnlyAsset"

        when:
        superAdminResource.validate(null, typeName, validDefinition(typeName))

        then:
        customAssetTypeStorageService.find(typeName) == null
        ValueUtil.getAssetInfo(typeName).isEmpty()
    }

    def "usage reports assets whose stored type exactly matches the custom type"() {
        given:
        def typeName = "ApiUsageBoilerAsset"
        superAdminResource.create(null, validDefinition(typeName))
        def asset = new ThingAsset("API usage boiler").setRealm(Constants.MASTER_REALM)
        asset.type = typeName
        asset.addOrReplaceAttributes(new Attribute<>("temperature", ValueType.NUMBER, 21.5d))
        assetStorageService.merge(asset)

        expect:
        superAdminResource.getUsage(null, typeName) == 1
    }

    def "non-super-admin users cannot access custom asset type management"() {
        when:
        regularUserResource.getAll(null)

        then:
        def ex = thrown(WebApplicationException)
        assertStatus(ex, Response.Status.FORBIDDEN)

        when:
        regularUserResource.create(null, validDefinition("ApiForbiddenAsset"))

        then:
        ex = thrown(WebApplicationException)
        assertStatus(ex, Response.Status.FORBIDDEN)
    }

    def "invalid create returns bad request and does not mutate state"() {
        given:
        def typeName = "ApiInvalidAsset"
        def definition = validDefinition(typeName)
        definition.attributes = [
                attribute("query", "assetQuery")
        ]

        when:
        superAdminResource.create(null, definition)

        then:
        def ex = thrown(WebApplicationException)
        assertStatus(ex, Response.Status.BAD_REQUEST)
        customAssetTypeStorageService.find(typeName) == null
        ValueUtil.getAssetInfo(typeName).isEmpty()
    }

    def "duplicate create returns conflict"() {
        given:
        def typeName = "ApiDuplicateAsset"
        superAdminResource.create(null, validDefinition(typeName))

        when:
        superAdminResource.create(null, validDefinition(typeName))

        then:
        def ex = thrown(WebApplicationException)
        assertStatus(ex, Response.Status.CONFLICT)
    }

    private static CustomAssetTypeDefinition validDefinition(String name) {
        new CustomAssetTypeDefinition(
                name,
                name,
                "cube-outline",
                null,
                null,
                true,
                [
                        attribute("temperature", "number")
                ]
        )
    }

    private static CustomAssetTypeAttributeDefinition attribute(String name, String type) {
        new CustomAssetTypeAttributeDefinition(
                name,
                type,
                false,
                null,
                null,
                null,
                null,
                null,
                null
        )
    }

    private static void assertStatus(WebApplicationException exception, Response.Status status) {
        exception.response.withCloseable { response ->
            assert response.status == status.statusCode
            true
        }
    }
}
