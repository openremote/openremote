package org.openremote.test.assets

import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.asset.ServerAsset
import org.openremote.model.Constants
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.asset.AssetMeta
import org.openremote.model.asset.AssetType
import org.openremote.model.attribute.*
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.logging.Logger

class AssetAttributeLinkingTest extends Specification implements ManagerContainerTrait {
    Logger LOG = Logger.getLogger(AssetAttributeLinkingTest.class.getName())

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
            assert noEventProcessedIn(assetProcessingService, 500)
        }

        when: "assets are created"
        def asset1 = new ServerAsset("Asset 1", AssetType.THING, null, Constants.MASTER_REALM)
        asset1.setAttributes(
            new AssetAttribute("button", AttributeType.STRING, Values.create("RELEASED"), getClockTimeOf(container))
        )
        asset1 = assetStorageService.merge(asset1)
        def asset2 = new ServerAsset("Asset 2", AssetType.THING, null, Constants.MASTER_REALM)
        asset2.setAttributes(
                new AssetAttribute("lightOnOff", AttributeType.BOOLEAN, Values.create(false), getClockTimeOf(container)),
                new AssetAttribute("counter", AttributeType.NUMBER, Values.create(0), getClockTimeOf(container))
        )
        asset2 = assetStorageService.merge(asset2)

        then: "the assets should be saved to the DB"
        assert asset1.id != null
        assert asset2.id != null

        when: "an attribute from one asset is linked to an attribute on the other"
        def converterOnOff = Values.createObject()
        converterOnOff.put("PRESSED", "@TOGGLE")
        converterOnOff.put("RELEASED", "@IGNORE")
        converterOnOff.put("LONG_PRESSED", "@IGNORE")
        def attributeLinkOnOff = Values.createObject()
        attributeLinkOnOff.put("attributeRef", new AttributeRef(asset2.id, "lightOnOff").toArrayValue())
        attributeLinkOnOff.put("converter", converterOnOff)

        def converterCounter = Values.createObject()
        converterCounter.put("PRESSED", "@INCREMENT")
        converterCounter.put("RELEASED", "@DECREMENT")
        converterCounter.put("LONG_PRESSED", "@IGNORE")
        def attributeLinkCounter = Values.createObject()
        attributeLinkCounter.put("attributeRef", new AttributeRef(asset2.id, "counter").toArrayValue())
        attributeLinkCounter.put("converter", converterCounter)

        asset1.getAttribute("button").get().addMeta(new MetaItem(AssetMeta.ATTRIBUTE_LINK, attributeLinkOnOff))
        asset1.getAttribute("button").get().addMeta(new MetaItem(AssetMeta.ATTRIBUTE_LINK, attributeLinkCounter))
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

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
