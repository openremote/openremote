package org.openremote.test.model

import jakarta.validation.ConstraintViolationException
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.asset.CustomAssetTypeStorageService
import org.openremote.model.Constants
import org.openremote.model.asset.Asset
import org.openremote.model.asset.CustomAssetTypeAttributeDefinition
import org.openremote.model.asset.CustomAssetTypeDefinition
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.MetaItem
import org.openremote.model.attribute.MetaMap
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueConstraint
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

class CustomAssetTypeAssetTest extends Specification implements ManagerContainerTrait {

    private static final String CUSTOM_TYPE = "PayloadBoilerAsset"

    @Shared
    static CustomAssetTypeStorageService customAssetTypeStorageService

    @Shared
    static AssetStorageService assetStorageService

    def setupSpec() {
        startContainer(defaultConfig(), defaultServices())
        customAssetTypeStorageService = container.getService(CustomAssetTypeStorageService.class)
        assetStorageService = container.getService(AssetStorageService.class)
        customAssetTypeStorageService.persist(validDefinition(CUSTOM_TYPE))
    }

    def "custom asset JSON deserializes as ThingAsset with descriptor-backed attributes"() {
        when:
        def asset = ValueUtil.parse("""
            {
              "name": "Boiler from JSON",
              "type": "${CUSTOM_TYPE}",
              "realm": "${Constants.MASTER_REALM}",
              "attributes": {
                "temperature": {
                  "name": "temperature",
                  "value": 21.5
                }
              }
            }
        """, Asset).orElse(null)

        then:
        asset instanceof ThingAsset
        asset.type == CUSTOM_TYPE
        asset.getAttribute("temperature").map { it.type }.orElse(null).is(ValueType.NUMBER)
        asset.getAttribute("temperature").flatMap { it.getValue(Double) }.orElse(null) == 21.5d
    }

    def "missing required custom attributes fail asset validation"() {
        given:
        def asset = new ThingAsset("Missing temperature").setRealm(Constants.MASTER_REALM)
        asset.type = CUSTOM_TYPE

        when:
        assetStorageService.merge(asset)

        then:
        thrown(ConstraintViolationException)
    }

    def "new custom assets initialise missing attributes from authored defaults"() {
        given:
        def typeName = "DefaultedBoilerAsset"
        customAssetTypeStorageService.persist(validDefinition(typeName, 21.5d))
        def asset = new ThingAsset("Defaulted boiler").setRealm(Constants.MASTER_REALM)
        asset.type = typeName

        when:
        def saved = assetStorageService.merge(asset)
        def found = assetStorageService.find(saved.id)

        then:
        found.type == typeName
        found.getAttribute("temperature").map { it.type }.orElse(null).is(ValueType.NUMBER)
        found.getAttribute("temperature").flatMap { it.getValue(Double) }.orElse(null) == 21.5d
    }

    def "new custom assets initialise empty attributes from authored defaults"() {
        given:
        def typeName = "EmptyDefaultedBoilerAsset"
        customAssetTypeStorageService.persist(validDefinition(typeName, 21.5d))
        def asset = new ThingAsset("Empty defaulted boiler").setRealm(Constants.MASTER_REALM)
        asset.type = typeName
        asset.addOrReplaceAttributes(new Attribute<>("temperature", ValueType.NUMBER))

        when:
        def saved = assetStorageService.merge(asset)
        def found = assetStorageService.find(saved.id)

        then:
        found.type == typeName
        found.getAttribute("temperature").map { it.type }.orElse(null).is(ValueType.NUMBER)
        found.getAttribute("temperature").flatMap { it.getValue(Double) }.orElse(null) == 21.5d
    }

    def "invalid custom attribute values fail asset validation"() {
        given:
        def asset = new ThingAsset("Invalid temperature").setRealm(Constants.MASTER_REALM)
        asset.type = CUSTOM_TYPE
        asset.addOrReplaceAttributes(new Attribute<>("temperature", ValueType.NUMBER, -1d))

        when:
        assetStorageService.merge(asset)

        then:
        thrown(ConstraintViolationException)
    }

    def "unknown asset types keep the generic fallback behaviour"() {
        when:
        def asset = ValueUtil.parse("""
            {
              "name": "Unknown from JSON",
              "type": "PayloadUnknownAsset",
              "realm": "${Constants.MASTER_REALM}",
              "attributes": {
                "freeform": {
                  "name": "freeform",
                  "value": "raw"
                }
              }
            }
        """, Asset).orElse(null)

        then:
        asset instanceof ThingAsset
        asset.type == "PayloadUnknownAsset"
        asset.getAttribute("freeform").map { it.type }.orElse(null) == null
    }

    def "saving and reading a custom asset preserves the stored custom type"() {
        given:
        def asset = new ThingAsset("Stored custom asset").setRealm(Constants.MASTER_REALM)
        asset.type = CUSTOM_TYPE
        asset.addOrReplaceAttributes(new Attribute<>("temperature", ValueType.NUMBER, 22.0d))

        when:
        def saved = assetStorageService.merge(asset)
        def found = assetStorageService.find(saved.id)

        then:
        found.type == CUSTOM_TYPE
        found.getAttribute("temperature").map { it.type }.orElse(null).is(ValueType.NUMBER)
        found.getAttribute("temperature").flatMap { it.getValue(Double) }.orElse(null) == 22.0d
    }

    private static CustomAssetTypeDefinition validDefinition(String name) {
        validDefinition(name, null)
    }

    private static CustomAssetTypeDefinition validDefinition(String name, Object defaultValue) {
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
                                defaultValue,
                                null,
                                null,
                                [new ValueConstraint.Min(0), new ValueConstraint.Max(120)] as ValueConstraint[],
                                new MetaMap([
                                        new MetaItem<>(MetaItemType.LABEL, "Temperature")
                                ]),
                                10
                        )
                ]
        )
    }
}
