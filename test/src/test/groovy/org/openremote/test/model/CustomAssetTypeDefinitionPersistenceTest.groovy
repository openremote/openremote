package org.openremote.test.model

import org.openremote.manager.asset.CustomAssetTypeStorageService
import org.openremote.model.asset.CustomAssetTypeAttributeDefinition
import org.openremote.model.asset.CustomAssetTypeDefinition
import org.openremote.model.attribute.MetaItem
import org.openremote.model.attribute.MetaMap
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueConstraint
import org.openremote.model.value.ValueFormat
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

class CustomAssetTypeDefinitionPersistenceTest extends Specification implements ManagerContainerTrait {

    @Shared
    static CustomAssetTypeStorageService customAssetTypeStorageService

    def setupSpec() {
        startContainer(defaultConfig(), defaultServices())
        customAssetTypeStorageService = container.getService(CustomAssetTypeStorageService.class)
    }

    def "custom asset type definitions persist authored schema without runtime descriptors"() {
        given: "a custom asset type definition"
        def temperature = new CustomAssetTypeAttributeDefinition(
                "temperature",
                "number",
                false,
                21.5d,
                ["celsius"] as String[],
                ValueFormat.NUMBER_1_DP(),
                [new ValueConstraint.Min(0), new ValueConstraint.Max(120)] as ValueConstraint[],
                new MetaMap([
                        new MetaItem<>(MetaItemType.LABEL, "Temperature"),
                        new MetaItem<>(MetaItemType.STORE_DATA_POINTS, true)
                ]),
                10
        )
        def enabled = new CustomAssetTypeAttributeDefinition(
                "enabled",
                "boolean",
                true,
                true,
                null,
                null,
                null,
                new MetaMap([
                        new MetaItem<>(MetaItemType.READ_ONLY, true)
                ]),
                20
        )
        def definition = new CustomAssetTypeDefinition(
                "BoilerAsset",
                "Boiler",
                "water-boiler",
                "#cc5500",
                "Boiler equipment type",
                true,
                [temperature, enabled]
        )

        when: "the definition is persisted and read back"
        customAssetTypeStorageService.merge(definition)
        def stored = customAssetTypeStorageService.find("BoilerAsset")

        then: "the authored schema round-trips"
        stored != null
        stored.name == "BoilerAsset"
        stored.displayName == "Boiler"
        stored.icon == "water-boiler"
        stored.colour == "#cc5500"
        stored.description == "Boiler equipment type"
        stored.enabled
        stored.attributes*.name == ["temperature", "enabled"]

        and: "value types are stored as descriptor names"
        stored.attributes[0].type == "number"
        stored.attributes[1].type == "boolean"

        and: "authored value metadata round-trips"
        stored.attributes[0].defaultValue == 21.5d
        stored.attributes[0].units == ["celsius"] as String[]
        stored.attributes[0].format.maximumFractionDigits == 1
        stored.attributes[0].constraints.length == 2
        stored.attributes[0].meta.get(MetaItemType.LABEL).flatMap { it.value }.orElse(null) == "Temperature"
        stored.attributes[0].meta.get(MetaItemType.STORE_DATA_POINTS).flatMap { it.value }.orElse(null) == true
        stored.attributes[1].meta.get(MetaItemType.READ_ONLY).flatMap { it.value }.orElse(null) == true
    }

    def "duplicate custom asset type names are rejected"() {
        given:
        def first = new CustomAssetTypeDefinition(
                "DuplicateAsset",
                "Duplicate",
                "cube-outline",
                null,
                null,
                true,
                []
        )
        def second = new CustomAssetTypeDefinition(
                "DuplicateAsset",
                "Duplicate 2",
                "cube-outline",
                null,
                null,
                true,
                []
        )

        when:
        customAssetTypeStorageService.persist(first)
        customAssetTypeStorageService.persist(second)

        then:
        thrown(RuntimeException)
    }

    def "usage count matches assets by stored type"() {
        expect:
        customAssetTypeStorageService.getUsageCount("NoSuchCustomAsset") == 0
    }
}
