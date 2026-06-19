package org.openremote.test.model

import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.asset.CustomAssetTypeStorageService
import org.openremote.model.Constants
import org.openremote.model.asset.CustomAssetTypeAttributeDefinition
import org.openremote.model.asset.CustomAssetTypeDefinition
import org.openremote.model.asset.impl.BuildingAsset
import org.openremote.model.asset.impl.CityAsset
import org.openremote.model.asset.impl.GroupAsset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.query.AssetQuery
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

class CustomAssetTypeQueryAndGroupTest extends Specification implements ManagerContainerTrait {

    @Shared
    static CustomAssetTypeStorageService customAssetTypeStorageService

    @Shared
    static AssetStorageService assetStorageService

    def setupSpec() {
        startContainer(defaultConfig(), defaultServices())
        customAssetTypeStorageService = container.getService(CustomAssetTypeStorageService.class)
        assetStorageService = container.getService(AssetStorageService.class)
    }

    def "asset type queries use exact stored custom type names"() {
        given:
        def typeName = "QueryExactBoilerAsset"
        customAssetTypeStorageService.persist(validDefinition(typeName))
        def thingAsset = assetStorageService.merge(new ThingAsset("Query generic thing").setRealm(Constants.MASTER_REALM))
        def customAsset = customThing(typeName, "Query custom boiler")
        customAsset = assetStorageService.merge(customAsset)

        expect:
        queryIds(new AssetQuery().ids(thingAsset.id, customAsset.id).types(ThingAsset.class)) == [thingAsset.id] as Set

        when:
        def customTypeQuery = new AssetQuery().ids(thingAsset.id, customAsset.id)
        customTypeQuery.types = [typeName] as String[]

        then:
        queryIds(customTypeQuery) == [customAsset.id] as Set

        when:
        def combinedTypeQuery = new AssetQuery().ids(thingAsset.id, customAsset.id)
        combinedTypeQuery.types = [ThingAsset.class.simpleName, typeName] as String[]

        then:
        queryIds(combinedTypeQuery) == [thingAsset.id, customAsset.id] as Set
    }

    def "group configured for custom asset type accepts exact custom stored type"() {
        given:
        def typeName = "GroupExactBoilerAsset"
        customAssetTypeStorageService.persist(validDefinition(typeName))
        def group = assetStorageService.merge(new GroupAsset("Custom boiler group", typeName).setRealm(Constants.MASTER_REALM))
        def customAsset = customThing(typeName, "Grouped custom boiler").setParentId(group.id)

        when:
        def stored = assetStorageService.merge(customAsset)

        then:
        stored.parentId == group.id
    }

    def "group configured for ThingAsset rejects custom stored type"() {
        given:
        def typeName = "GroupThingRejectBoilerAsset"
        customAssetTypeStorageService.persist(validDefinition(typeName))
        def group = assetStorageService.merge(new GroupAsset("Thing group", ThingAsset.class).setRealm(Constants.MASTER_REALM))
        def customAsset = customThing(typeName, "Rejected custom boiler").setParentId(group.id)

        when:
        assetStorageService.merge(customAsset)

        then:
        thrown(IllegalStateException)
    }

    def "built-in group type hierarchy still accepts built-in subtypes"() {
        given:
        def group = assetStorageService.merge(new GroupAsset("City descendants", CityAsset.class).setRealm(Constants.MASTER_REALM))

        when:
        def building = assetStorageService.merge(new BuildingAsset("Building child").setRealm(Constants.MASTER_REALM).setParentId(group.id))

        then:
        building.parentId == group.id
    }

    private static Set<String> queryIds(AssetQuery query) {
        assetStorageService.findAll(query)*.id as Set
    }

    private static ThingAsset customThing(String typeName, String name) {
        def asset = new ThingAsset(name).setRealm(Constants.MASTER_REALM)
        asset.type = typeName
        asset.addOrReplaceAttributes(new Attribute<>("temperature", ValueType.NUMBER, 21.5d))
        asset
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
                                null,
                                10
                        )
                ]
        )
    }
}
