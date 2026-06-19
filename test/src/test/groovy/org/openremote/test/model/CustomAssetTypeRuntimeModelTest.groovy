package org.openremote.test.model

import org.openremote.manager.asset.AssetModelService
import org.openremote.manager.asset.CustomAssetTypeStorageService
import org.openremote.model.asset.Asset
import org.openremote.model.asset.CustomAssetTypeAttributeDefinition
import org.openremote.model.asset.CustomAssetTypeDefinition
import org.openremote.model.attribute.MetaItem
import org.openremote.model.attribute.MetaMap
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

class CustomAssetTypeRuntimeModelTest extends Specification implements ManagerContainerTrait {

    @Shared
    static CustomAssetTypeStorageService customAssetTypeStorageService

    @Shared
    static AssetModelService assetModelService

    def setupSpec() {
        startContainer(defaultConfig(), defaultServices())
        customAssetTypeStorageService = container.getService(CustomAssetTypeStorageService.class)
        assetModelService = container.getService(AssetModelService.class)
    }

    def "enabled custom asset type is exposed in the local runtime model after create"() {
        given:
        def typeName = "RuntimeModelBoilerAsset"

        when:
        customAssetTypeStorageService.persist(validDefinition(typeName))
        def assetTypeInfo = ValueUtil.getAssetInfo(typeName).orElse(null)

        then:
        assetTypeInfo != null
        assetModelService.getAssetInfo(null, typeName).is(assetTypeInfo)
        assetTypeInfo.assetDescriptor.dynamic
        assetTypeInfo.assetDescriptor.type == null
        assetTypeInfo.attributeDescriptors.temperature.type.is(ValueType.NUMBER)
        assetTypeInfo.attributeDescriptors.temperature.meta.get(MetaItemType.LABEL).flatMap { it.value }.orElse(null) == "Temperature"
        assetTypeInfo.attributeDescriptors.containsKey(Asset.LOCATION.name)
    }

    def "disabled custom asset types are hidden and update refreshes the local runtime model"() {
        given:
        def typeName = "RuntimeModelToggleAsset"
        def definition = validDefinition(typeName).setEnabled(false)

        when:
        customAssetTypeStorageService.persist(definition)

        then:
        ValueUtil.getAssetInfo(typeName).isEmpty()

        when:
        def stored = customAssetTypeStorageService.find(typeName)
        stored.setEnabled(true)
        customAssetTypeStorageService.merge(stored)

        then:
        ValueUtil.getAssetInfo(typeName).isPresent()

        when:
        stored = customAssetTypeStorageService.find(typeName)
        stored.setEnabled(false)
        customAssetTypeStorageService.merge(stored)

        then:
        ValueUtil.getAssetInfo(typeName).isEmpty()
    }

    def "delete refreshes the local runtime model"() {
        given:
        def typeName = "RuntimeModelDeleteAsset"
        customAssetTypeStorageService.persist(validDefinition(typeName))

        expect:
        ValueUtil.getAssetInfo(typeName).isPresent()

        when:
        customAssetTypeStorageService.delete(typeName)

        then:
        ValueUtil.getAssetInfo(typeName).isEmpty()
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
