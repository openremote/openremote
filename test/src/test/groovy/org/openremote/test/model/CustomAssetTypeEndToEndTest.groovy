package org.openremote.test.model

import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.asset.CustomAssetTypeStorageService
import org.openremote.model.Constants
import org.openremote.model.asset.CustomAssetTypeAttributeDefinition
import org.openremote.model.asset.CustomAssetTypeDefinition
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.MetaItem
import org.openremote.model.attribute.MetaMap
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

class CustomAssetTypeEndToEndTest extends Specification implements ManagerContainerTrait {

    def "persisted custom asset types are loaded after manager restart"() {
        given:
        String typeName = "RestartLoaded${UUID.randomUUID().toString().replace("-", "")}Asset"
        def config = defaultConfig()
        config[PersistenceService.OR_SETUP_RUN_ON_RESTART] = "false"
        def container = startContainer(config, defaultServices())
        def customAssetTypeStorageService = container.getService(CustomAssetTypeStorageService.class)
        def assetStorageService = container.getService(AssetStorageService.class)

        and:
        customAssetTypeStorageService.persist(validDefinition(typeName))
        def asset = new ThingAsset("Restart loaded custom asset").setRealm(Constants.MASTER_REALM)
        asset.type = typeName
        asset.addOrReplaceAttributes(new Attribute<>("temperature", ValueType.NUMBER, 24.0d))
        def saved = assetStorageService.merge(asset)

        expect:
        ValueUtil.getAssetInfo(typeName).isPresent()

        when:
        def restartConfig = new LinkedHashMap<>(config)
        restartConfig["CUSTOM_ASSET_TYPE_RESTART_MARKER"] = typeName
        container = startContainer(restartConfig, defaultServices())
        customAssetTypeStorageService = container.getService(CustomAssetTypeStorageService.class)
        assetStorageService = container.getService(AssetStorageService.class)
        def assetTypeInfo = ValueUtil.getAssetInfo(typeName).orElse(null)
        def found = assetStorageService.find(saved.id)

        then:
        customAssetTypeStorageService.find(typeName) != null
        assetTypeInfo != null
        assetTypeInfo.assetDescriptor.dynamic
        assetTypeInfo.attributeDescriptors.temperature.type.is(ValueType.NUMBER)
        assetTypeInfo.attributeDescriptors.temperature.meta.get(MetaItemType.LABEL).flatMap { it.value }.orElse(null) == "Temperature"
        found.type == typeName
        found.getAttribute("temperature").map { it.type }.orElse(null).is(ValueType.NUMBER)
        found.getAttribute("temperature").flatMap { it.getValue(Double) }.orElse(null) == 24.0d
    }

    private static CustomAssetTypeDefinition validDefinition(String name) {
        new CustomAssetTypeDefinition(
                name,
                name,
                "water-boiler",
                "#cc5500",
                null,
                true,
                [
                        new CustomAssetTypeAttributeDefinition(
                                "temperature",
                                "number",
                                false,
                                null,
                                null,
                                null,
                                null,
                                new MetaMap([
                                        new MetaItem<>(MetaItemType.LABEL, "Temperature")
                                ]),
                                10
                        )
                ]
        )
    }
}
