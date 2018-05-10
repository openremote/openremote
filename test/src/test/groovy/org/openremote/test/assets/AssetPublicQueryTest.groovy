package org.openremote.test.assets

import org.openremote.container.Container
import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.asset.AssetStorageService

import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetResource
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.asset.AssetQuery
import org.openremote.model.attribute.MetaItem
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.asset.BaseAssetQuery.*
import static org.openremote.model.asset.AssetMeta.ACCESS_PUBLIC_READ
import static org.openremote.model.asset.AssetMeta.LABEL
import static org.openremote.model.asset.AssetType.THING
import static org.openremote.model.attribute.AttributeValueType.NUMBER

class AssetPublicQueryTest extends Specification implements ManagerContainerTrait {

    @Shared
    static Container container
    @Shared
    static ManagerDemoSetup managerDemoSetup
    @Shared
    static KeycloakDemoSetup keycloakDemoSetup
    @Shared
    static AssetStorageService assetStorageService
    @Shared
    static PersistenceService persistenceService
    @Shared
    static AssetResource assetResource

    @Shared
    static String somePublicAssetId

    def setupSpec() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        container = startContainer(defaultConfig(serverPort), defaultServices())
        managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        assetStorageService = container.getService(AssetStorageService.class)
        persistenceService = container.getService(PersistenceService.class)
        assetResource = getClientTarget(serverUri(serverPort), MASTER_REALM).proxy(AssetResource.class)

        Asset somePublicAsset = new Asset("Some Public Asset", THING)
        somePublicAsset.setParentId(managerDemoSetup.smartOfficeId)
        somePublicAsset.setAccessPublicRead(true)
        somePublicAsset.setAttributes(
                new AssetAttribute("somePrivateAttribute", NUMBER, Values.create(123)).addMeta(
                        new MetaItem(LABEL, Values.create("Some Private Attribute"))
                ),
                new AssetAttribute("somePublicAttribute", NUMBER, Values.create(456)).addMeta(
                        new MetaItem(LABEL, Values.create("Some Public Attribute")),
                        new MetaItem(ACCESS_PUBLIC_READ, Values.create(true))
                ),
        )

        somePublicAsset = assetStorageService.merge(somePublicAsset)
        somePublicAssetId = somePublicAsset.getId()
    }

    def cleanupSpec() {
        given: "the server should be stopped"
        stopContainer(container)
    }

    def "Query public assets"() {

        when: "a query for all public assets is executed"
        def assets = assetResource.queryPublicAssets(null,
                new AssetQuery()
                        .select(new Select(Include.ALL, Access.PUBLIC_READ))
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.id))
        )

        then: "the result should match"
        assets.size() == 1
        assets[0].id == somePublicAssetId
        assets[0].name == "Some Public Asset"
        assets[0].wellKnownType == THING
        assets[0].parentId == managerDemoSetup.smartOfficeId
        assets[0].realmId == keycloakDemoSetup.masterTenant.id
        assets[0].tenantRealm == keycloakDemoSetup.masterTenant.realm
        assets[0].tenantDisplayName == keycloakDemoSetup.masterTenant.displayName
        !assets[0].getAttribute("somePrivateAttribute").isPresent()
        assets[0].getAttribute("somePublicAttribute").get().getValue().get() == Values.create(456)
        assets[0].getAttribute("somePublicAttribute").get().getMeta().size() == 1
        assets[0].getAttribute("somePublicAttribute").get().getMetaItem(LABEL).get().getValueAsString().get() == "Some Public Attribute"
   }
}
