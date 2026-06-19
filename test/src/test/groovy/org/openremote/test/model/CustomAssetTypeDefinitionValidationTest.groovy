package org.openremote.test.model

import org.openremote.manager.asset.CustomAssetTypeStorageService
import org.openremote.model.asset.CustomAssetTypeAttributeDefinition
import org.openremote.model.asset.CustomAssetTypeDefinition
import org.openremote.model.attribute.MetaItem
import org.openremote.model.attribute.MetaMap
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueConstraint
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

class CustomAssetTypeDefinitionValidationTest extends Specification implements ManagerContainerTrait {

    @Shared
    static CustomAssetTypeStorageService customAssetTypeStorageService

    def setupSpec() {
        startContainer(defaultConfig(), defaultServices())
        customAssetTypeStorageService = container.getService(CustomAssetTypeStorageService.class)
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
}
