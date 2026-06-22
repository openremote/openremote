package org.openremote.test.model

import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.asset.CustomAssetTypeStorageService
import org.openremote.model.Constants
import org.openremote.model.asset.CustomAssetTypeAttributeDefinition
import org.openremote.model.asset.CustomAssetTypeDefinition
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.MetaItem
import org.openremote.model.attribute.MetaMap
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueConstraint
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

class CustomAssetTypeDefinitionValidationTest extends Specification implements ManagerContainerTrait {

    @Shared
    static CustomAssetTypeStorageService customAssetTypeStorageService

    @Shared
    static AssetStorageService assetStorageService

    def setupSpec() {
        startContainer(defaultConfig(), defaultServices())
        customAssetTypeStorageService = container.getService(CustomAssetTypeStorageService.class)
        assetStorageService = container.getService(AssetStorageService.class)
    }

    def "definition name must not collide with built-in asset types"() {
        when:
        customAssetTypeStorageService.persist(validDefinition("ThingAsset"))

        then:
        thrown(IllegalArgumentException)
    }

    def "definition names must use asset type identifier format"() {
        when:
        customAssetTypeStorageService.persist(validDefinition("Invalid-Type"))

        then:
        thrown(IllegalArgumentException)
    }

    def "attribute names must use attribute identifier format"() {
        given:
        def definition = validDefinition("InvalidAttributeNameAsset")
        definition.attributes = [
                attribute("supply-temperature", "number")
        ]

        when:
        customAssetTypeStorageService.persist(definition)

        then:
        thrown(IllegalArgumentException)
    }

    def "attribute names must be unique"() {
        given:
        def definition = validDefinition("DuplicateAttributeAsset")
        definition.attributes = [
                attribute("temperature", "number"),
                attribute("temperature", "number").setOptional(true)
        ]

        when:
        customAssetTypeStorageService.persist(definition)

        then:
        thrown(IllegalArgumentException)
    }

    def "value types must be in the phase one allowlist"() {
        given:
        def definition = validDefinition("UnsupportedValueTypeAsset")
        definition.attributes = [
                attribute("query", "assetQuery")
        ]

        when:
        customAssetTypeStorageService.persist(definition)

        then:
        thrown(IllegalArgumentException)
    }

    def "default values must match the selected value type"() {
        given:
        def definition = validDefinition("InvalidDefaultValueAsset")
        definition.attributes = [
                attribute("temperature", "number").setDefaultValue("not-a-number")
        ]

        when:
        customAssetTypeStorageService.persist(definition)

        then:
        thrown(IllegalArgumentException)
    }

    def "constraints must be compatible with the selected value type"() {
        given:
        def definition = validDefinition("InvalidConstraintAsset")
        definition.attributes = [
                attribute("temperature", "number").setConstraints([
                        new ValueConstraint.Pattern("[a-z]+")
                ] as ValueConstraint[])
        ]

        when:
        customAssetTypeStorageService.persist(definition)

        then:
        thrown(IllegalArgumentException)
    }

    def "default values must satisfy custom attribute constraints"() {
        given:
        def definition = validDefinition("InvalidConstrainedDefaultAsset")
        definition.attributes = [
                attribute("temperature", "number")
                        .setDefaultValue(-1d)
                        .setConstraints([
                                new ValueConstraint.Min(0)
                        ] as ValueConstraint[])
        ]

        when:
        customAssetTypeStorageService.persist(definition)

        then:
        thrown(IllegalArgumentException)
    }

    def "unsupported meta items are rejected"() {
        given:
        def definition = validDefinition("UnsupportedMetaAsset")
        definition.attributes = [
                attribute("secretCode", "text").setMeta(new MetaMap([
                        new MetaItem<>(MetaItemType.SECRET, true)
                ]))
        ]

        when:
        customAssetTypeStorageService.persist(definition)

        then:
        thrown(IllegalArgumentException)
    }

    def "supported definition is persisted"() {
        when:
        customAssetTypeStorageService.persist(validDefinition("ValidatedAsset"))

        then:
        customAssetTypeStorageService.find("ValidatedAsset") != null
    }

    def "in-use definitions allow adding optional attributes"() {
        given:
        def typeName = "InUseOptionalAttributeAsset"
        customAssetTypeStorageService.persist(validDefinition(typeName))
        persistCustomAsset(typeName)
        def updatedDefinition = customAssetTypeStorageService.find(typeName)
        updatedDefinition.attributes = updatedDefinition.attributes + [
                attribute("pressure", "number").setOptional(true)
        ]

        when:
        customAssetTypeStorageService.merge(updatedDefinition)

        then:
        customAssetTypeStorageService.find(typeName).attributes*.name.contains("pressure")
    }

    def "in-use definitions reject adding required attributes"() {
        given:
        def typeName = "InUseRequiredAttributeAsset"
        customAssetTypeStorageService.persist(validDefinition(typeName))
        persistCustomAsset(typeName)
        def updatedDefinition = customAssetTypeStorageService.find(typeName)
        updatedDefinition.attributes = updatedDefinition.attributes + [
                attribute("pressure", "number")
        ]

        when:
        customAssetTypeStorageService.merge(updatedDefinition)

        then:
        thrown(IllegalArgumentException)
    }

    def "in-use definitions reject changing existing attributes"() {
        given:
        def typeName = "InUseChangedAttributeAsset"
        customAssetTypeStorageService.persist(validDefinition(typeName))
        persistCustomAsset(typeName)
        def updatedDefinition = customAssetTypeStorageService.find(typeName)
        updatedDefinition.attributes = [
                attribute("temperature", "text").setDefaultValue("warm"),
                attribute("enabled", "boolean").setOptional(true).setDefaultValue(true).setMeta(new MetaMap([
                        new MetaItem<>(MetaItemType.LABEL, "Enabled"),
                        new MetaItem<>(MetaItemType.READ_ONLY, true),
                        new MetaItem<>(MetaItemType.STORE_DATA_POINTS, false)
                ]))
        ]

        when:
        customAssetTypeStorageService.merge(updatedDefinition)

        then:
        thrown(IllegalArgumentException)
    }

    def "in-use definitions cannot be deleted"() {
        given:
        def typeName = "DeleteInUseAsset"
        customAssetTypeStorageService.persist(validDefinition(typeName))
        persistCustomAsset(typeName)

        when:
        customAssetTypeStorageService.delete(typeName)

        then:
        thrown(IllegalStateException)
        customAssetTypeStorageService.find(typeName) != null
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
                        attribute("temperature", "number").setDefaultValue(21.5d),
                        attribute("enabled", "boolean").setOptional(true).setDefaultValue(true).setMeta(new MetaMap([
                                new MetaItem<>(MetaItemType.LABEL, "Enabled"),
                                new MetaItem<>(MetaItemType.READ_ONLY, true),
                                new MetaItem<>(MetaItemType.STORE_DATA_POINTS, false)
                        ]))
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

    private static void persistCustomAsset(String typeName) {
        def asset = new ThingAsset(typeName + " Instance").setRealm(Constants.MASTER_REALM)
        asset.type = typeName
        asset.addOrReplaceAttributes(new Attribute<>("temperature", ValueType.NUMBER, 21.5d))
        assetStorageService.merge(asset)
    }
}
