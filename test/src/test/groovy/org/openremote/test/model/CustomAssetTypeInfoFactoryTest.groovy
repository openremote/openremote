package org.openremote.test.model

import org.openremote.manager.asset.CustomAssetTypeInfoFactory
import org.openremote.model.asset.Asset
import org.openremote.model.asset.CustomAssetTypeAttributeDefinition
import org.openremote.model.asset.CustomAssetTypeDefinition
import org.openremote.model.attribute.MetaItem
import org.openremote.model.attribute.MetaMap
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueConstraint
import org.openremote.model.value.ValueFormat
import org.openremote.model.value.ValueType
import spock.lang.Specification

class CustomAssetTypeInfoFactoryTest extends Specification {

    def setupSpec() {
        ValueUtil.initialise(null)
    }

    def "converts custom asset type definition into dynamic asset type info"() {
        given:
        def definition = new CustomAssetTypeDefinition(
                "BoilerAsset",
                "Boiler",
                "water-boiler",
                "#cc5500",
                "Boiler equipment type",
                true,
                [
                        attribute("enabled", "boolean", true, true, null, null, null, new MetaMap([
                                new MetaItem<>(MetaItemType.LABEL, "Enabled"),
                                new MetaItem<>(MetaItemType.READ_ONLY, true),
                                new MetaItem<>(MetaItemType.STORE_DATA_POINTS, false)
                        ]), 20),
                        attribute("temperature", "number", false, 21.5d, ["celsius"] as String[], ValueFormat.NUMBER_1_DP(), [
                                new ValueConstraint.Min(0),
                                new ValueConstraint.Max(120)
                        ] as ValueConstraint[], new MetaMap([
                                new MetaItem<>(MetaItemType.LABEL, "Temperature"),
                                new MetaItem<>(MetaItemType.STORE_DATA_POINTS, true)
                        ]), 10)
                ]
        )

        when:
        def assetTypeInfo = new CustomAssetTypeInfoFactory().toAssetTypeInfo(definition)
        def attributes = assetTypeInfo.attributeDescriptors

        then:
        assetTypeInfo.assetDescriptor.name == "BoilerAsset"
        assetTypeInfo.assetDescriptor.icon == "water-boiler"
        assetTypeInfo.assetDescriptor.colour == "#cc5500"
        assetTypeInfo.assetDescriptor.dynamic
        assetTypeInfo.assetDescriptor.type == null

        and: "custom attributes are sorted by position"
        attributes.keySet().findAll { it in ["temperature", "enabled"] }.toList() == ["temperature", "enabled"]

        and: "custom attributes reference existing value descriptors"
        attributes.temperature.type.is(ValueType.NUMBER)
        attributes.enabled.type.is(ValueType.BOOLEAN)

        and: "attribute metadata and descriptor fields are preserved"
        attributes.temperature.units == ["celsius"] as String[]
        attributes.temperature.format.maximumFractionDigits == 1
        attributes.temperature.constraints.length == 2
        attributes.temperature.meta.get(MetaItemType.LABEL).flatMap { it.value }.orElse(null) == "Temperature"
        attributes.temperature.meta.get(MetaItemType.STORE_DATA_POINTS).flatMap { it.value }.orElse(null) == true
        !attributes.temperature.optional
        attributes.enabled.optional
        attributes.enabled.meta.get(MetaItemType.READ_ONLY).flatMap { it.value }.orElse(null) == true

        and: "default values are not encoded into runtime descriptors"
        !ValueUtil.asJSON(assetTypeInfo).orElseThrow().contains("defaultValue")

        and: "common ThingAsset backing descriptors are included"
        attributes.containsKey(Asset.LOCATION.name)
        attributes.containsKey(Asset.MANUFACTURER.name)

        and: "custom definitions do not create custom value or meta descriptors"
        assetTypeInfo.valueDescriptors.length == 0
        assetTypeInfo.metaItemDescriptors.length == 0
    }

    def "conversion rejects unknown value descriptor names"() {
        given:
        def definition = new CustomAssetTypeDefinition(
                "UnsupportedAsset",
                "Unsupported",
                "cube-outline",
                null,
                null,
                true,
                [
                        attribute("unsupported", "unsupportedValue", false, null, null, null, null, null, 0)
                ]
        )

        when:
        new CustomAssetTypeInfoFactory().toAssetTypeInfo(definition)

        then:
        thrown(IllegalArgumentException)
    }

    private static CustomAssetTypeAttributeDefinition attribute(
            String name,
            String type,
            Boolean optional,
            Object defaultValue,
            String[] units,
            ValueFormat format,
            ValueConstraint[] constraints,
            MetaMap meta,
            Integer position
    ) {
        new CustomAssetTypeAttributeDefinition(
                name,
                type,
                optional,
                defaultValue,
                units,
                format,
                constraints,
                meta,
                position
        )
    }
}
