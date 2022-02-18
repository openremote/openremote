package org.openremote.test.assets

import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.Constants
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.*
import org.openremote.model.value.JsonPathFilter
import org.openremote.model.value.ValueFilter
import org.openremote.model.util.ValueUtil
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit

import static org.openremote.model.value.ValueType.*
import static org.openremote.model.value.MetaItemType.*

class AttributeLinkingTest extends Specification implements ManagerContainerTrait {

    def "Check processing of asset attributes that are linked to other attributes"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        when: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)

        then: "the system should settle down"
        conditions.eventually {
            assert noEventProcessedIn(assetProcessingService, 300)
        }

        when: "assets are created"
        def asset1 = new ThingAsset("Asset 1")
        asset1.setRealm(Constants.MASTER_REALM)
        asset1.addOrReplaceAttributes(
            new Attribute<>("button", TEXT, "RELEASED"),
            new Attribute<>("array", JSON_OBJECT.asArray(), null)
        )
        asset1 = assetStorageService.merge(asset1)

        def asset2 = new ThingAsset("Asset 2")
            .setRealm(Constants.MASTER_REALM)
            .addOrReplaceAttributes(
            new Attribute<>("lightOnOff", BOOLEAN, false),
            new Attribute<>("counter", NUMBER, 0d),
            new Attribute<>("item2Prop1", BOOLEAN, null)
        )
        asset2 = assetStorageService.merge(asset2)

        then: "the assets should be saved to the DB"
        assert asset1.id != null
        assert asset2.id != null

        when: "attributes from one asset is linked to attributes on the other"
        def converterOnOff = ValueUtil.createJsonObject()
        converterOnOff.put("PRESSED", "@TOGGLE")
        converterOnOff.put("RELEASED", "@IGNORE")
        converterOnOff.put("LONG_PRESSED", "@IGNORE")
        def attributeLinkOnOff = new AttributeLink(new AttributeRef(asset2.id, "lightOnOff"), converterOnOff, null)

        def converterCounter = ValueUtil.createJsonObject()
        converterCounter.put("PRESSED", "@INCREMENT")
        converterCounter.put("RELEASED", "@DECREMENT")
        converterCounter.put("LONG_PRESSED", "@IGNORE")
        def attributeLinkCounter = new AttributeLink(new AttributeRef(asset2.id, "counter"), converterCounter, null)

        def attributeLinkProp = new AttributeLink(new AttributeRef(asset2.id, "item2Prop1"), null, [
            new JsonPathFilter("\$[1].prop1", true, false)
        ] as ValueFilter[])

        asset1.getAttribute("button").get().addMeta(new MetaItem<>(ATTRIBUTE_LINKS, [attributeLinkOnOff, attributeLinkCounter] as AttributeLink[]))
        asset1.getAttribute("array").get().addMeta(new MetaItem<>(ATTRIBUTE_LINKS, [attributeLinkProp] as AttributeLink[]))
        asset1 = assetStorageService.merge(asset1)

        and: "the button is pressed for a short period"
        def buttonPressed = new AttributeEvent(
                new AttributeState(new AttributeRef(asset1.id, "button"), "PRESSED")
        )
        assetProcessingService.sendAttributeEvent(buttonPressed)
        advancePseudoClock(1, TimeUnit.SECONDS, container)
        def buttonReleased = new AttributeEvent(
                new AttributeState(new AttributeRef(asset1.id, "button"), "RELEASED")
        )
        assetProcessingService.sendAttributeEvent(buttonReleased)

        then: "the linked attribute value should be toggled on"
        conditions.eventually {
            asset2 = assetStorageService.find(asset2.id, true)
            assert asset2.getAttribute("lightOnOff", Boolean.class).flatMap{it.value}.orElse(false)
        }

        when: "the button is pressed again for a short period"
        buttonPressed = new AttributeEvent(
                new AttributeState(new AttributeRef(asset1.id, "button"), "PRESSED")
        )
        assetProcessingService.sendAttributeEvent(buttonPressed)
        advancePseudoClock(1, TimeUnit.SECONDS, container)
        buttonReleased = new AttributeEvent(
                new AttributeState(new AttributeRef(asset1.id, "button"), "RELEASED")
        )
        assetProcessingService.sendAttributeEvent(buttonReleased)

        then: "the linked attribute value should be toggled off"
        conditions.eventually {
            asset2 = assetStorageService.find(asset2.id, true)
            assert !asset2.getAttribute("lightOnOff", Boolean.class).flatMap{it.value}.orElse(false)
        }
        when: "a long button press occurs"
        def buttonLongPressed = new AttributeEvent(
                new AttributeState(new AttributeRef(asset1.id, "button"), "LONG_PRESSED")
        )
        assetProcessingService.sendAttributeEvent(buttonLongPressed)
        advancePseudoClock(1, TimeUnit.SECONDS, container)
        buttonReleased = new AttributeEvent(
                new AttributeState(new AttributeRef(asset1.id, "button"), "RELEASED")
        )
        assetProcessingService.sendAttributeEvent(buttonReleased)

        then: "the linked attribute value should not have changed and the system has settled down"
        conditions.eventually {
            asset2 = assetStorageService.find(asset2.id, true)
            assert !asset2.getAttribute("lightOnOff").flatMap{it.value}.orElse(false)
            assert noEventProcessedIn(assetProcessingService, 500)
        }

        // Need to reset counter due to synchronisation issues (ideally counter would still be at 0 as
        // each press event had a corresponding release event)
        when: "the counter is reset"
        def attr = asset2.getAttribute("counter").get()
        attr.setValue(0.0)
        asset2.addOrReplaceAttributes(attr)
        asset2 = assetStorageService.merge(asset2)

        and: "A button press event occurs without a release event"
        buttonPressed = new AttributeEvent(
                new AttributeState(new AttributeRef(asset1.id, "button"), "PRESSED")
        )
        assetProcessingService.sendAttributeEvent(buttonPressed)

        then: "the counter should increment"
        conditions.eventually {
            asset2 = assetStorageService.find(asset2.id, true)
            assert asset2.getAttribute("counter").flatMap{it.value}.orElse(0d) == 1.0
        }

        when: "A button release event occur"
        buttonReleased = new AttributeEvent(
                new AttributeState(new AttributeRef(asset1.id, "button"), "RELEASED")
        )
        assetProcessingService.sendAttributeEvent(buttonReleased)

        then: "the counter should decrement"
        conditions.eventually {
            asset2 = assetStorageService.find(asset2.id, true)
            assert asset2.getAttribute("counter").flatMap{it.value}.orElse(0d) == 0.0
        }

        /* TODO Test has timing issues, fails in line 182 when run from CLI gradle clean build, works in IDE!
        when: "the linked attribute is linked back to the source attribute (to create circular reference)"
        def converterLoop = ValueUtil.createObjectMap()
        converterLoop.put("TRUE", "PRESSED")
        converterLoop.put("FALSE", "PRESSED")
        def attributeLinkLoop = ValueUtil.createObjectMap()
        attributeLinkLoop.put("ref", new AttributeRef(asset1.id, "button"))
        attributeLinkLoop.put("converter", converterLoop)
        asset2.getAttribute("lightOnOff").get().addMeta(new MetaItem<>(AssetMeta.ATTRIBUTE_LINK, attributeLinkLoop))
        asset2 = assetStorageService.merge(asset2)

        and: "the button is pressed for a short period"
        buttonPressed = new AttributeEvent(
            new AttributeState(new AttributeRef(asset1.id, "button"), "PRESSED")
        )
        assetProcessingService.sendAttributeEvent(buttonPressed)
        Thread.sleep(10)
        buttonReleased = new AttributeEvent(
            new AttributeState(new AttributeRef(asset1.id, "button"), "RELEASED")
        )
        assetProcessingService.sendAttributeEvent(buttonReleased)

        then: "the linked attribute value should be toggled on"
        conditions.eventually {
            asset2 = assetStorageService.find(asset2.id, true)
            assert asset2.getAttribute("lightOnOff").flatMap{it.value}.orElse(false)
        }

        and: "no more events should be processed"
        conditions.eventually {
            assert noEventProcessedIn(assetProcessingService, 500)
        }
        */

        when: "the array attribute is written to"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(asset1.id, "array", ValueUtil.parse("[{\"prop1\": true, \"prop2\": \"a\"},{\"prop1\": false, \"prop2\": \"b\"}]").orElse(null)))

        then: "the linked attribute on the other asset should contain the value from the json path"
        conditions.eventually {
            asset2 = assetStorageService.find(asset2.id, true)
            assert !asset2.getAttribute("item2Prop1").flatMap{it.value}.orElse(true)
        }
    }
}
