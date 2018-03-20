package org.openremote.test.assets

import com.fasterxml.uuid.Generators
import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.value.Values
import spock.lang.Specification

import java.util.stream.Collectors

import static org.openremote.model.AbstractValueTimestampHolder.VALUE_TIMESTAMP_FIELD_NAME
import static org.openremote.model.asset.AssetAttribute.getAddedOrModifiedAttributes
import static org.openremote.model.asset.AssetType.THING
import static org.openremote.model.attribute.AttributeType.STRING

class AssetModelTest extends Specification {

    def "Modifying asset identifier"() {

        when: "an asset is created without identifier"
        Asset asset = new Asset("foo", THING).setAttributes(
                new AssetAttribute("foo1", STRING, Values.create("foo111")),
                new AssetAttribute("foo2", STRING, Values.create("foo222"))
        )

        then: "the attributes should not have an identifier"
        !asset.getAttribute("foo1").get().getAssetId().isPresent()
        !asset.getAttribute("foo2").get().getAssetId().isPresent()
        !asset.getAttributesList()[0].getAssetId().isPresent()
        !asset.getAttributesList()[1].getAssetId().isPresent()

        when: "the asset identifier is set later"
        def id = UniqueIdentifierGenerator.generateId()
        asset.setId(id)

        then: "the attributes should have an identifier"
        asset.getAttribute("foo1").get().getAssetId().get() == id
        asset.getAttribute("foo2").get().getAssetId().get() == id
        asset.getAttributesList()[0].getAssetId().get() == id
        asset.getAttributesList()[1].getAssetId().get() == id
    }

    def "Comparing asset attributes"() {

        when: "two attributes have different value timestamps"
        def timestamp = System.currentTimeMillis()
        def timestamp2 = timestamp + 1000

        def attributeA = new AssetAttribute("a", STRING, Values.create("foo"), timestamp)
        def attributeB = new AssetAttribute("b", STRING, Values.create("foo"), timestamp2)

        then: "they should be different"
        !attributeA.getObjectValue().equalsIgnoreKeys(attributeB.getObjectValue(), null)

        and: "if we ignore the timestamp they should be equal"
        attributeA.getObjectValue().equalsIgnoreKeys(attributeB.getObjectValue(), { key -> key == VALUE_TIMESTAMP_FIELD_NAME })

        when: "an attribute has no timestamp"
        def attributeC = new AssetAttribute("c", STRING, Values.create("foo"))

        then: "it should be different than attributes with a timestamp"
        !attributeA.getObjectValue().equalsIgnoreKeys(attributeC.getObjectValue(), null)
        !attributeB.getObjectValue().equalsIgnoreKeys(attributeC.getObjectValue(), null)

        and: "if we ignore the timestamp they all should be equal"
        attributeA.getObjectValue().equalsIgnoreKeys(attributeC.getObjectValue(), { key -> key == VALUE_TIMESTAMP_FIELD_NAME })
        attributeB.getObjectValue().equalsIgnoreKeys(attributeC.getObjectValue(), { key -> key == VALUE_TIMESTAMP_FIELD_NAME })
    }

    def "Comparing asset attribute lists"() {

        when: "two lists of asset attributes are compared"
        def timestamp = System.currentTimeMillis()
        def attributesA = [
                new AssetAttribute("a1", STRING, Values.create("a111"), timestamp),
                new AssetAttribute("a2", STRING, Values.create("a222"), timestamp),
        ]
        def attributesB = [
                new AssetAttribute("a1", STRING, Values.create("a111"), timestamp),
                new AssetAttribute("a2", STRING, Values.create("a222"), timestamp),
                new AssetAttribute("a3", STRING, Values.create("a333"), timestamp),
        ]
        List<AssetAttribute> addedOrModifiedAttributes = getAddedOrModifiedAttributes(attributesA, attributesB).collect(Collectors.toList())

        then: "they should be different"
        addedOrModifiedAttributes.size() == 1
        addedOrModifiedAttributes[0].name.get() == "a3"

        when: "two lists of asset attributes are compared, ignoring some"
        timestamp = System.currentTimeMillis()
        attributesA = [
                new AssetAttribute("a1", STRING, Values.create("a111"), timestamp),
                new AssetAttribute("a2", STRING, Values.create("a222"), timestamp),
        ]
        attributesB = [
                new AssetAttribute("a1", STRING, Values.create("a111"), timestamp),
                new AssetAttribute("a2", STRING, Values.create("a222"), timestamp),
                new AssetAttribute("a3", STRING, Values.create("a333"), timestamp),
        ]
        addedOrModifiedAttributes = getAddedOrModifiedAttributes(attributesA, attributesB, { name -> name == "a3" }, { key -> false }).collect(Collectors.toList())

        then: "they should be the same"
        addedOrModifiedAttributes.size() == 0

        when: "two lists of asset attributes with different value timestamp are compared"
        timestamp = System.currentTimeMillis()
        def timestamp2 = timestamp + 1000

        attributesA = [
                new AssetAttribute("a1", STRING, Values.create("a111"), timestamp),
                new AssetAttribute("a2", STRING, Values.create("a222"), timestamp),
        ]
        attributesB = [
                new AssetAttribute("a1", STRING, Values.create("a111"), timestamp2),
                new AssetAttribute("a2", STRING, Values.create("a222"), timestamp2),
        ]
        addedOrModifiedAttributes = getAddedOrModifiedAttributes(attributesA, attributesB).collect(Collectors.toList())

        then: "they should be different"
        addedOrModifiedAttributes.size() == 2
        addedOrModifiedAttributes[0].name.get() == "a1"
        addedOrModifiedAttributes[1].name.get() == "a2"

        when: "two lists of asset attributes with different value timestamp are compared, ignoring timestamps"
        timestamp = System.currentTimeMillis()
        timestamp2 = timestamp + 1000

        attributesA = [
                new AssetAttribute("a1", STRING, Values.create("a111"), timestamp),
                new AssetAttribute("a2", STRING, Values.create("a222"), timestamp),
        ]
        attributesB = [
                new AssetAttribute("a1", STRING, Values.create("a111"), timestamp2),
                new AssetAttribute("a2", STRING, Values.create("a222"), timestamp2),
        ]
        addedOrModifiedAttributes = getAddedOrModifiedAttributes(attributesA, attributesB, { key -> key == VALUE_TIMESTAMP_FIELD_NAME }).collect(Collectors.toList())

        then: "they should be the same"
        addedOrModifiedAttributes.size() == 0
    }

}
