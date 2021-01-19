package org.openremote.test.assets


import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakTestSetup
import org.openremote.manager.setup.builtin.ManagerTestSetup
import org.openremote.model.asset.Asset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.asset.AssetResource
import org.openremote.model.attribute.MetaItem
import org.openremote.model.query.AssetQuery
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.value.ValueType.*
import static org.openremote.model.value.MetaItemType.*

class AssetPublicQueryTest extends Specification implements ManagerContainerTrait {

    @Shared
    static ManagerTestSetup managerTestSetup
    @Shared
    static KeycloakTestSetup keycloakTestSetup
    @Shared
    static AssetStorageService assetStorageService
    @Shared
    static PersistenceService persistenceService
    @Shared
    static AssetResource assetResource
    @Shared
    static List<Asset> returnedAssets

    def setupSpec() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        assetStorageService = container.getService(AssetStorageService.class)
        persistenceService = container.getService(PersistenceService.class)
        assetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM).proxy(AssetResource.class)
        returnedAssets = new ArrayList<>()
        returnedAssets = new ArrayList<>()

        for (int i = 0; i < 10; i++) {
            Asset somePublicAsset = new ThingAsset("Some Public Asset")
                .setParentId(managerTestSetup.smartOfficeId)
                .setRealm(MASTER_REALM)
                .setAccessPublicRead(true)
            somePublicAsset.addOrReplaceAttributes(
                    new Attribute<>("somePrivateAttribute", NUMBER, 123D).addMeta(
                            new MetaItem<>(LABEL, "Some Private Attribute")
                    ),
                    new Attribute<>("somePublicAttribute", NUMBER, 456D).addMeta(
                            new MetaItem<>(LABEL, "Some Public Attribute"),
                            new MetaItem<>(ACCESS_PUBLIC_READ, true)
                    ),
            )

            returnedAssets.add(assetStorageService.merge(somePublicAsset))
        }
    }

    def "Query public assets"() {

        expect: "10 public assets to have been created"
        assert returnedAssets.size() == 10
        assert returnedAssets.count { it.id != null } == 10

        when: "a query for a specific public asset is executed"
        def query = new AssetQuery()
                .ids(returnedAssets.get(0).id)
        def assets = assetResource.queryPublicAssets(null, query)

        then: "the result should match"
        assets.size() == 1
        assets[0].id == returnedAssets.get(0).id
        assets[0].name == "Some Public Asset"
        assets[0].type == ThingAsset.DESCRIPTOR.name
        assets[0].parentId == managerTestSetup.smartOfficeId
        assets[0].realm == keycloakTestSetup.masterTenant.realm
        !assets[0].getAttribute("somePrivateAttribute").isPresent()
        assets[0].getAttribute("somePublicAttribute").get().getValue().get() == 456
        assets[0].getAttribute("somePublicAttribute").get().getMeta().size() >= 1
        assets[0].getAttribute("somePublicAttribute").get().getMetaItem(LABEL).get().getValue().get() == "Some Public Attribute"

        when: "a GET query for a specific public asset is executed"
        def queryJson = "{\"select\":{\"include\":\"ALL\"},\"id\":\"${returnedAssets.get(0).id}\"}"
        queryJson = Values.JSON.writeValueAsString(query)
        assets = assetResource.getPublicAssets(null, queryJson)

        then: "the result should match"
        assets.size() == 1
        assets[0].id == returnedAssets.get(0).id
        assets[0].name == "Some Public Asset"
        assets[0].type == ThingAsset.DESCRIPTOR.name
        assets[0].parentId == managerTestSetup.smartOfficeId
        assets[0].realm == keycloakTestSetup.masterTenant.realm
        !assets[0].getAttribute("somePrivateAttribute").isPresent()
        assets[0].getAttribute("somePublicAttribute").get().getValue().get() == 456
        assets[0].getAttribute("somePublicAttribute").get().getMeta().size() >= 1
        assets[0].getAttribute("somePublicAttribute").get().getMetaItem(LABEL).get().getValue().get() == "Some Public Attribute"
    }
}
