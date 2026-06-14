package org.openremote.test.assets

import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.AssetAttributeConfigurationDocument
import org.openremote.model.asset.AssetAttributeConfigurationEntry
import org.openremote.model.asset.AssetAttributeConfigurationExportRequest
import org.openremote.model.asset.AssetAttributeConfigurationImportRequest
import org.openremote.model.asset.AssetResource
import org.openremote.model.asset.impl.RoomAsset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.MetaItem
import org.openremote.model.attribute.MetaMap
import org.openremote.model.value.ValueType
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.manager.security.ManagerIdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER
import static org.openremote.model.util.MapAccess.getString
import static org.openremote.model.value.MetaItemType.LABEL
import static org.openremote.model.value.MetaItemType.READ_ONLY
import static org.openremote.model.value.MetaItemType.STORE_DATA_POINTS

class AssetAttributeConfigurationResourceTest extends Specification implements ManagerContainerTrait {

    def "Export endpoint returns persisted attribute configuration document"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "an authenticated admin asset resource"
        def assetResource = adminAssetResource(container)

        and: "an asset with configured and unconfigured attributes"
        def asset = new ThingAsset("Export endpoint asset")
            .setRealm(keycloakTestSetup.realmMaster.name)
            .addOrReplaceAttributes(
                new Attribute<>("temperature", ValueType.NUMBER, 21d)
                    .addOrReplaceMeta(
                        new MetaItem<>(READ_ONLY, true),
                        new MetaItem<>(LABEL, "Current temperature")
                    ),
                new Attribute<>("humidity", ValueType.NUMBER, 45d)
                    .addOrReplaceMeta(new MetaItem<>(STORE_DATA_POINTS, true)),
                new Attribute<>("notes", ValueType.TEXT, "No exported meta")
            )
        asset = assetResource.create(null, asset)

        when: "the selected attribute configuration is exported"
        def document = assetResource.exportAttributeConfiguration(
            null,
            asset.id,
            new AssetAttributeConfigurationExportRequest(["temperature", "notes"])
        )

        then: "only selected configured attributes are exported"
        document.version == AssetAttributeConfigurationDocument.CURRENT_VERSION
        document.assetType == ThingAsset.DESCRIPTOR.name
        document.attributes.keySet() == ["temperature"] as Set
        document.attributes.temperature.type == "number"
        document.attributes.temperature.meta.getValue(READ_ONLY).orElse(false)
        document.attributes.temperature.meta.getValue(LABEL).orElse(null) == "Current temperature"
    }

    def "Import preview endpoint returns patched draft attributes without persisting"() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)

        and: "an authenticated admin asset resource"
        def assetResource = adminAssetResource(container)

        and: "a persisted target asset"
        def asset = new ThingAsset("Import preview endpoint asset")
            .setRealm(keycloakTestSetup.realmMaster.name)
            .addOrReplaceAttributes(
                new Attribute<>("temperature", ValueType.NUMBER, 18d)
                    .addOrReplaceMeta(
                        new MetaItem<>(READ_ONLY, false),
                        new MetaItem<>(LABEL, "Persisted label")
                    ),
                new Attribute<>("humidity", ValueType.NUMBER, 40d)
                    .addOrReplaceMeta(new MetaItem<>(LABEL, "Persisted humidity label")),
                new Attribute<>("mode", ValueType.TEXT, "auto")
                    .addOrReplaceMeta(new MetaItem<>(LABEL, "Persisted mode label"))
            )
        asset = assetResource.create(null, asset)

        and: "the modify view draft has an unsaved metadata change"
        def draftAsset = assetResource.get(null, asset.id)
        draftAsset.getAttribute("humidity").get().setMeta(new MetaMap([
            new MetaItem<>(LABEL, "Unsaved humidity label")
        ]))

        and: "an import document contains compatible, missing, and type-mismatched attributes"
        def document = new AssetAttributeConfigurationDocument(
            AssetAttributeConfigurationDocument.CURRENT_VERSION,
            RoomAsset.DESCRIPTOR.name,
            [
                temperature: new AssetAttributeConfigurationEntry("number", new MetaMap([
                    new MetaItem<>(READ_ONLY, true)
                ])),
                missing    : new AssetAttributeConfigurationEntry("text", new MetaMap([
                    new MetaItem<>(LABEL, "Missing")
                ])),
                mode       : new AssetAttributeConfigurationEntry("boolean", new MetaMap([
                    new MetaItem<>(LABEL, "Wrong type")
                ]))
            ]
        )

        when: "the import is previewed"
        def preview = assetResource.previewAttributeConfigurationImport(
            null,
            asset.id,
            new AssetAttributeConfigurationImportRequest(draftAsset, document)
        )

        then: "the response contains the confirmation report"
        preview.assetTypeMismatch.expected == ThingAsset.DESCRIPTOR.name
        preview.assetTypeMismatch.actual == RoomAsset.DESCRIPTOR.name
        preview.importableAttributes*.name == ["temperature"]
        preview.missingAttributes*.name == ["missing"]
        preview.typeMismatches*.name == ["mode"]

        and: "the patched attributes replace compatible metadata and preserve unrelated draft metadata"
        preview.patchedAttributes.get("temperature").get().meta.getValue(READ_ONLY).orElse(false)
        !preview.patchedAttributes.get("temperature").get().meta.has(LABEL)
        preview.patchedAttributes.get("humidity").get().meta.getValue(LABEL).orElse(null) == "Unsaved humidity label"
        preview.patchedAttributes.get("mode").get().meta.getValue(LABEL).orElse(null) == "Persisted mode label"

        and: "the preview did not persist the patch"
        def storedAsset = assetResource.get(null, asset.id)
        storedAsset.getAttribute("temperature").get().meta.getValue(LABEL).orElse(null) == "Persisted label"
        !storedAsset.getAttribute("temperature").get().meta.getValue(READ_ONLY).orElse(false)
        storedAsset.getAttribute("humidity").get().meta.getValue(LABEL).orElse(null) == "Persisted humidity label"
    }

    protected AssetResource adminAssetResource(container) {
        def accessToken = authenticate(
            container,
            MASTER_REALM,
            KEYCLOAK_CLIENT_ID,
            MASTER_REALM_ADMIN_USER,
            getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        )

        getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)
    }
}
