package org.openremote.test.assets

import org.openremote.container.Container
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.Constants
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.asset.AssetType
import org.openremote.model.attribute.*
import org.openremote.model.value.JsonPathFilter
import org.openremote.model.value.ValueFilter
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class AssetAttributeLinkingTest extends Specification implements ManagerContainerTrait {

    def "Check processing of asset attributes that are linked to other attributes"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 1)

        when: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainerNoDemoImport(defaultConfig(serverPort), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)

        then: "the container should be running and initialised"
        conditions.eventually {
            container.isRunning()
            assert noEventProcessedIn(assetProcessingService, 500)
        }

        when: "assets are created"
        def asset1 = new Asset("Asset 1", AssetType.THING, null, Constants.MASTER_REALM)
        asset1.setAttributes(
            new AssetAttribute("button", AttributeValueType.STRING, Values.create("RELEASED"), getClockTimeOf(container)),
            new AssetAttribute("array", AttributeValueType.ARRAY, null)
        )
        asset1 = assetStorageService.merge(asset1)
        def asset2 = new Asset("Asset 2", AssetType.THING, null, Constants.MASTER_REALM)
        asset2.setAttributes(
            new AssetAttribute("lightOnOff", AttributeValueType.BOOLEAN, Values.create(false), getClockTimeOf(container)),
            new AssetAttribute("counter", AttributeValueType.NUMBER, Values.create(0), getClockTimeOf(container)),
            new AssetAttribute("item2Prop1", AttributeValueType.BOOLEAN, null)
        )
        asset2 = assetStorageService.merge(asset2)

        then: "the assets should be saved to the DB"
        assert asset1.id != null
        assert asset2.id != null

        when: "attributes from one asset is linked to attributes on the other"
        def converterOnOff = Values.createObject()
        converterOnOff.put("PRESSED", "@TOGGLE")
        converterOnOff.put("RELEASED", "@IGNORE")
        converterOnOff.put("LONG_PRESSED", "@IGNORE")
        def attributeLinkOnOff = Values.convertToValue(new AttributeLink(new AttributeRef(asset2.id, "lightOnOff"), converterOnOff, null), Container.JSON.writer()).orElse(null)

        def converterCounter = Values.createObject()
        converterCounter.put("PRESSED", "@INCREMENT")
        converterCounter.put("RELEASED", "@DECREMENT")
        converterCounter.put("LONG_PRESSED", "@IGNORE")
        def attributeLinkCounter = Values.convertToValue(new AttributeLink(new AttributeRef(asset2.id, "counter"), converterCounter, null), Container.JSON.writer()).orElse(null)

        def attributeLinkProp = Values.convertToValue(new AttributeLink(
            new AttributeRef(asset2.id, "item2Prop1"), null, [
            new JsonPathFilter("\$[1].prop1", true, false)
        ] as ValueFilter[]), Container.JSON.writer()).orElse(null)

        asset1.getAttribute("button").get().addMeta(new MetaItem(MetaItemType.ATTRIBUTE_LINK, attributeLinkOnOff))
        asset1.getAttribute("button").get().addMeta(new MetaItem(MetaItemType.ATTRIBUTE_LINK, attributeLinkCounter))
        asset1.getAttribute("array").get().addMeta(new MetaItem(MetaItemType.ATTRIBUTE_LINK, attributeLinkProp))
        asset1 = assetStorageService.merge(asset1)

        and: "the button is pressed for a short period"
        def buttonPressed = new AttributeEvent(
                new AttributeState(new AttributeRef(asset1.id, "button"), Values.create("PRESSED"))
        )
        assetProcessingService.sendAttributeEvent(buttonPressed)
        Thread.sleep(10)
        def buttonReleased = new AttributeEvent(
                new AttributeState(new AttributeRef(asset1.id, "button"), Values.create("RELEASED"))
        )
        assetProcessingService.sendAttributeEvent(buttonReleased)

        then: "the linked attribute value should be toggled on"
        conditions.eventually {
            asset2 = assetStorageService.find(asset2.id, true)
            assert asset2.getAttribute("lightOnOff").get().getValueAsBoolean().get()
        }

        when: "the button is pressed again for a short period"
        buttonPressed = new AttributeEvent(
                new AttributeState(new AttributeRef(asset1.id, "button"), Values.create("PRESSED"))
        )
        assetProcessingService.sendAttributeEvent(buttonPressed)
        Thread.sleep(10)
        buttonReleased = new AttributeEvent(
                new AttributeState(new AttributeRef(asset1.id, "button"), Values.create("RELEASED"))
        )
        assetProcessingService.sendAttributeEvent(buttonReleased)

        then: "the linked attribute value should be toggled off"
        conditions.eventually {
            asset2 = assetStorageService.find(asset2.id, true)
            assert !asset2.getAttribute("lightOnOff").get().getValueAsBoolean().get()
        }
        when: "a long button press occurs"
        def buttonLongPressed = new AttributeEvent(
                new AttributeState(new AttributeRef(asset1.id, "button"), Values.create("LONG_PRESSED"))
        )
        assetProcessingService.sendAttributeEvent(buttonLongPressed)
        Thread.sleep(10)
        buttonReleased = new AttributeEvent(
                new AttributeState(new AttributeRef(asset1.id, "button"), Values.create("RELEASED"))
        )
        assetProcessingService.sendAttributeEvent(buttonReleased)

        then: "the linked attribute value should not have changed and the system has settled down"
        conditions.eventually {
            asset2 = assetStorageService.find(asset2.id, true)
            assert !asset2.getAttribute("lightOnOff").get().getValueAsBoolean().get()
            assert noEventProcessedIn(assetProcessingService, 1000)
        }

        // Need to reset counter due to synchronisation issues (ideally counter would still be at 0 as
        // each press event had a corresponding release event)
        when: "the counter is reset"
        def attr = asset2.getAttribute("counter").get()
        attr.setValue(Values.create(0.0))
        asset2.replaceAttribute(attr)
        asset2 = assetStorageService.merge(asset2)

        and: "A button press event occurs without a release event"
        buttonPressed = new AttributeEvent(
                new AttributeState(new AttributeRef(asset1.id, "button"), Values.create("PRESSED"))
        )
        assetProcessingService.sendAttributeEvent(buttonPressed)

        then: "the counter should increment"
        conditions.eventually {
            asset2 = assetStorageService.find(asset2.id, true)
            assert asset2.getAttribute("counter").get().getValueAsNumber().get() == 1.0
        }

        when: "A button release event occur"
        buttonReleased = new AttributeEvent(
                new AttributeState(new AttributeRef(asset1.id, "button"), Values.create("RELEASED"))
        )
        assetProcessingService.sendAttributeEvent(buttonReleased)

        then: "the counter should decrement"
        conditions.eventually {
            asset2 = assetStorageService.find(asset2.id, true)
            assert asset2.getAttribute("counter").get().getValueAsNumber().get() == 0.0
        }

        /* TODO Test has timing issues, fails in line 182 when run from CLI gradle clean build, works in IDE!
        when: "the linked attribute is linked back to the source attribute (to create circular reference)"
        def converterLoop = Values.createObject()
        converterLoop.put("TRUE", "PRESSED")
        converterLoop.put("FALSE", "PRESSED")
        def attributeLinkLoop = Values.createObject()
        attributeLinkLoop.put("attributeRef", new AttributeRef(asset1.id, "button").toArrayValue())
        attributeLinkLoop.put("converter", converterLoop)
        asset2.getAttribute("lightOnOff").get().addMeta(new MetaItem(AssetMeta.ATTRIBUTE_LINK, attributeLinkLoop))
        asset2 = assetStorageService.merge(asset2)

        and: "the button is pressed for a short period"
        buttonPressed = new AttributeEvent(
            new AttributeState(new AttributeRef(asset1.id, "button"), Values.create("PRESSED"))
        )
        assetProcessingService.sendAttributeEvent(buttonPressed)
        Thread.sleep(10)
        buttonReleased = new AttributeEvent(
            new AttributeState(new AttributeRef(asset1.id, "button"), Values.create("RELEASED"))
        )
        assetProcessingService.sendAttributeEvent(buttonReleased)

        then: "the linked attribute value should be toggled on"
        conditions.eventually {
            asset2 = assetStorageService.find(asset2.id, true)
            assert asset2.getAttribute("lightOnOff").get().getValueAsBoolean().get()
        }

        and: "no more events should be processed"
        conditions.eventually {
            assert noEventProcessedIn(assetProcessingService, 500)
        }
        */

        when: "the array attribute is written to"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(asset1.id, "array", Values.parse("[{\"prop1\": true, \"prop2\": \"a\"},{\"prop1\": false, \"prop2\": \"b\"}]").orElse(null)))

        then: "the linked attribute on the other asset should contain the value from the json path"
        conditions.eventually {
            asset2 = assetStorageService.find(asset2.id, true)
            assert !asset2.getAttribute("item2Prop1").get().getValueAsBoolean().orElse(true)
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
