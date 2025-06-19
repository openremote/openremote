package org.openremote.test.security

import org.keycloak.admin.client.resource.IdentityProviderResource
import org.keycloak.admin.client.resource.IdentityProvidersResource
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.security.ManagerKeycloakIdentityProvider
import org.openremote.model.security.ClientRole
import org.openremote.model.security.Realm
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

class IdentityProviderTest extends Specification implements ManagerContainerTrait {

    @Shared
    static ManagerKeycloakIdentityProvider identityProvider

    def setupSpec() {
        def container = startContainer(defaultConfig(), defaultServices())
        identityProvider = container.getService(ManagerIdentityService.class).identityProvider as ManagerKeycloakIdentityProvider
    }

    def "Realm operations"() {
        def realmName = "testrealm"
        def realmDisplayName = "Test Realm"
        def realmEnabled = true

        when: "a realm does not yet exist in the identity provider"
        !identityProvider.realmExists(realmName)

        and: "the realm is created"
        def realm = new Realm(null, realmName, realmDisplayName, realmEnabled)
        identityProvider.createRealm(realm)
        realm = identityProvider.getRealm(realmName)

        then: "the realm exists in the identity provider"
        identityProvider.realmExists(realmName)

        and: "the realm has the correct properties"
        realm.name == realmName
        realm.displayName == realmDisplayName
        realm.enabled == realmEnabled

        when: "the realm is updated"
        realmDisplayName = "Updated Test Realm"
        realm.displayName = realmDisplayName
        identityProvider.updateRealm(realm)

        then: "the realm still exists in the identity provider"
        identityProvider.realmExists(realmName)

        when: "the realm is retrieved"
        realm = identityProvider.getRealm(realmName)

        then: "the realm has the correct properties"
        realm.name == realmName
        realm.displayName == realmDisplayName
        realm.enabled == realmEnabled

        when: "the realm is deleted"
        identityProvider.deleteRealm(realmName)

        then: "the realm no longer exists in the identity provider"
        !identityProvider.realmExists(realmName)

        and: "the realm cannot be retrieved"
        identityProvider.getRealm(realmName) == null
    }

    def "Configure Keycloak chaining"() {
        def realmName = "idprealm"
        def realmDisplayName = "IdP Realm"
        def realmEnabled = true
        def idpAlias = "keycloak-oidc"

        when: "a realm does not yet exist in the identity provider"
        !identityProvider.realmExists(realmName)

        and: "the realm is created"
        def realm = new Realm(null, realmName, realmDisplayName, realmEnabled)
        identityProvider.createRealm(realm)

        then: "the realm exists in the identity provider"
        identityProvider.realmExists(realmName)

        when: "an identity provider is created in the realm"
        Map<String, String> idpConfig = new HashMap<>();
        idpConfig.put("clientId", "test-client");
        idpConfig.put("clientSecret", "test-client-secret");
        idpConfig.put("clientAuthMethod", "client_secret_basic");
        idpConfig.put("syncMode", "FORCE");
        idpConfig.put("filteredByClaim", "true");
        idpConfig.put("claimFilterName", "claimName");
        idpConfig.put("claimFilterValue", "claimValue");

        identityProvider.createUpdateIdentityProvider(realmName, idpAlias, "oidc", idpConfig);

        then: "the identity provider is created in the realm"
        identityProvider.getRealms { realmsResource ->
            IdentityProvidersResource identityProvidersResource = realmsResource.realm(realmName).identityProviders();
            assert identityProvidersResource.findAll().stream().anyMatch(ipr -> idpAlias == ipr.getAlias())
            return null
        } == null

        and: "the identity provider configuration matches"
        identityProvider.getRealms { realmsResource ->
            IdentityProvidersResource identityProvidersResource = realmsResource.realm(realmName).identityProviders();
            Map<String, String> storedConfig = identityProvidersResource.findAll().stream().filter(ipr -> idpAlias == ipr.getAlias()).findFirst().get().getConfig()

            assert idpConfig.size() == storedConfig.size()
            idpConfig.each { key, value ->
                if (key != "clientSecret") {
                    // clientSecret is not returned by Keycloak for security reasons
                    assert storedConfig.get(key) == value
                }
            }

            return null
        } == null

        when: "identity provider role mappers are created for all roles"
        ClientRole.ALL_ROLES.forEach(role -> {
            String mapperName = role.replaceAll(":", " ")

            Map<String, String> mapperConfig = new HashMap<>();
            mapperConfig.put("syncMode", "FORCE")
            mapperConfig.put("claim", "cloudRoles")
            mapperConfig.put("claim.value", role)
            mapperConfig.put("role", "openremote." + role)

            identityProvider.createUpdateIdentityProviderMapper(realmName, idpAlias, mapperName, "oidc-role-idp-mapper", mapperConfig)
        });

        then: "the identity provider role mappers are created"
        identityProvider.getRealms { realmsResource ->
            IdentityProviderResource idpResource = realmsResource.realm(realmName).identityProviders().get(idpAlias)
            List<IdentityProviderMapperRepresentation> mappers = idpResource.getMappers()

            assert mappers.size() == ClientRole.ALL_ROLES.size()

            mappers.each { mapper ->
                Map<String, String> storedConfig = mapper.getConfig()
                String role = storedConfig.get("claim.value")

                assert ClientRole.ALL_ROLES.contains(role)
                assert storedConfig.get("syncMode") == "FORCE"
                assert storedConfig.get("claim") == "cloudRoles"
                assert storedConfig.get("role") == "openremote." + role
            }

            return null
        } == null

        when: "the browser authentication flow is configured to redirect to the identity provider"
        identityProvider.addAuthenticationExecutionConfig(realmName, "browser", "identity-provider-redirector", idpAlias, Map.of("defaultProvider", idpAlias));

        then: "the authentication flow is updated"
        identityProvider.getRealms { realmsResource ->
            def flows = realmsResource.realm(realmName).flows().getFlows()
            def browserFlow = flows.stream().filter { afr -> afr.getAlias() == "browser" }.findFirst().get()
            def iprExecutions = browserFlow.getAuthenticationExecutions().stream().filter(execution -> execution.getAuthenticator() == "identity-provider-redirector").toList()
            assert iprExecutions.size() == 1
            assert iprExecutions.get(0).getAuthenticatorConfig() == idpAlias
            return null
        } == null
    }

}
