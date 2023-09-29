package org.openremote.test.model

import com.fasterxml.jackson.databind.node.ObjectNode
import jakarta.ws.rs.WebApplicationException
import org.jboss.resteasy.api.validation.ViolationReport
import org.openremote.agent.protocol.http.HTTPAgentLink
import org.openremote.agent.protocol.simulator.SimulatorAgent
import org.openremote.agent.protocol.velbus.VelbusTCPAgent
import org.openremote.container.timer.TimerService
import org.openremote.manager.asset.AssetModelService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetModelResource
import org.openremote.model.asset.AssetResource
import org.openremote.model.asset.AssetTypeInfo
import org.openremote.model.asset.agent.AgentDescriptor
import org.openremote.model.asset.agent.AgentLink
import org.openremote.model.asset.agent.DefaultAgentLink
import org.openremote.model.asset.impl.GroupAsset
import org.openremote.model.asset.impl.LightAsset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.MetaItem
import org.openremote.model.rules.AssetState
import org.openremote.model.util.TimeUtil
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.*
import org.openremote.model.value.impl.ColourRGB
import org.openremote.setup.integration.model.asset.ModelTestAsset
import org.openremote.setup.integration.protocol.http.HTTPServerTestAgent
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

import java.time.format.DateTimeFormatter

import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD
import static org.openremote.container.security.IdentityProvider.OR_ADMIN_PASSWORD_DEFAULT
import static org.openremote.container.util.MapAccess.getString
import static org.openremote.model.Constants.*
import static org.openremote.model.value.ValueType.BIG_NUMBER

// TODO: Define new asset model tests (setValue - equality checking etc.)
class AssetModelTest extends Specification implements ManagerContainerTrait {

    @Shared
    static AssetModelResource assetModelResource

    def setupSpec() {
        def container = startContainer(defaultConfig(), defaultServices())
        def assetModelService = container.getService(AssetModelService.class)
        assetModelResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM).proxy(AssetModelResource.class)
    }

    def "Serialise/Deserialise asset and attribute events and test validation"() {

        given: "an authenticated admin user"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), OR_ADMIN_PASSWORD, OR_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the asset resource"
        def assetResource = getClientApiTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)

        and: "A well known asset is instantiated"
        def assetStorageService = container.getService(AssetStorageService.class)
        def timerService = container.getService(TimerService.class)
        def modelTestAsset = new ModelTestAsset("Test Asset")
        modelTestAsset.setRealm("master")

        when: "try to save the asset without meeting constraints on required attributes"
        assetResource.create(null, modelTestAsset)

        then: "a constraint violation exception should be thrown"
        WebApplicationException ex = thrown()
        ex.response.status == 400
        def report = ex.response.readEntity(ViolationReport)
        report.propertyViolations.size() == 1
        report.propertyViolations.get(0).path == "attributes[positiveInt].value"
        report.propertyViolations.get(0).message == "must not be null"
        report.classViolations.size() == 1
        report.classViolations.get(0).path == ""
        report.classViolations.get(0).message == "Asset is not valid"

        when: "the issue is fixed and the asset is saved again"
        modelTestAsset.getAttribute(ModelTestAsset.REQUIRED_POSITIVE_INT_ATTRIBUTE_DESCRIPTOR).map(attr -> attr.setValue(1))
        modelTestAsset = assetResource.create(null, modelTestAsset)

        then: "the save should succeed"
        modelTestAsset != null
        modelTestAsset.getAttribute(ModelTestAsset.REQUIRED_POSITIVE_INT_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) == 1

        when: "other optional attributes are added with valid values"
        modelTestAsset.addAttributes(
                new Attribute<>(ModelTestAsset.NEGATIVE_INT_ATTRIBUTE_DESCRIPTOR, -1),
                new Attribute<>(ModelTestAsset.SIZE_STRING_ATTRIBUTE_DESCRIPTOR, "abcde"),
                new Attribute<>(ModelTestAsset.SIZE_MAP_ATTRIBUTE_DESCRIPTOR, [
                        ("key1"): 1d,
                        ("key2"): 2d,
                        ("key3"): 3d
                ] as ValueType.DoubleMap
            ),
                new Attribute<>(ModelTestAsset.SIZE_ARRAY_ATTRIBUTE_DESCRIPTOR, [
                        "arrayValue1",
                        "arrayValue2"
                ] as String[]),
                new Attribute<>(ModelTestAsset.ALLOWED_VALUES_ENUM_ATTRIBUTE_DESCRIPTOR, ModelTestAsset.TestValue.ENUM_2),
                new Attribute<>(ModelTestAsset.ALLOWED_VALUES_STRING_ATTRIBUTE_DESCRIPTOR, "Allowed1"),
                new Attribute<>(ModelTestAsset.ALLOWED_VALUES_NUMBER_ATTRIBUTE_DESCRIPTOR, 1.5d),
                new Attribute<>(ModelTestAsset.PAST_TIMESTAMP_ATTRIBUTE_DESCRIPTOR, timerService.getCurrentTimeMillis()-1000L),
                new Attribute<>(ModelTestAsset.PAST_OR_PRESENT_DATE_ATTRIBUTE_DESCRIPTOR, timerService.getNow().toDate()),
                new Attribute<>(ModelTestAsset.FUTURE_ISO8601_ATTRIBUTE_DESCRIPTOR, DateTimeFormatter.ISO_INSTANT.format(timerService.getNow().plusMillis(100000))),
                new Attribute<>(ModelTestAsset.FUTURE_OR_PRESENT_TIMESTAMP_ATTRIBUTE_DESCRIPTOR, timerService.getCurrentTimeMillis()+100000),
                new Attribute<>(ModelTestAsset.NOT_EMPTY_STRING_ATTRIBUTE_DESCRIPTOR, "abcde"),
                new Attribute<>(ModelTestAsset.NOT_EMPTY_ARRAY_ATTRIBUTE_DESCRIPTOR, [1] as Integer[]),
                new Attribute<>(ModelTestAsset.NOT_EMPTY_MAP_ATTRIBUTE_DESCRIPTOR, [
                        ("key1"): true
                ] as ValueType.BooleanMap),
                new Attribute<>(ModelTestAsset.NOT_BLANK_STRING_ATTRIBUTE_DESCRIPTOR, "abcde")
        )
        //Why value constraint is deserialised as BigDecimal - serialise ValueConstraint and see what it deserialises as
        def asset = ValueUtil.asJSON(modelTestAsset).flatMap(str -> ValueUtil.parse(str, Asset))
        assetResource.update(null, modelTestAsset.id, modelTestAsset)

        then: "the save should succeed"
        modelTestAsset != null
        modelTestAsset.getAttribute(ModelTestAsset.REQUIRED_POSITIVE_INT_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) == 1
        modelTestAsset.getAttribute(ModelTestAsset.NEGATIVE_INT_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) == -1
        modelTestAsset.getAttribute(ModelTestAsset.SIZE_STRING_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) == "abcde"
        modelTestAsset.getAttribute(ModelTestAsset.SIZE_MAP_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null).size() == 3
        modelTestAsset.getAttribute(ModelTestAsset.SIZE_MAP_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null).get("key1") == 1d
        modelTestAsset.getAttribute(ModelTestAsset.SIZE_MAP_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null).get("key2") == 2d
        modelTestAsset.getAttribute(ModelTestAsset.SIZE_MAP_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null).get("key3") == 3d
        modelTestAsset.getAttribute(ModelTestAsset.SIZE_ARRAY_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null).size() == 2
        modelTestAsset.getAttribute(ModelTestAsset.SIZE_ARRAY_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null)[0] == "arrayValue1"
        modelTestAsset.getAttribute(ModelTestAsset.SIZE_ARRAY_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null)[0] == "arrayValue2"
        modelTestAsset.getAttribute(ModelTestAsset.ALLOWED_VALUES_ENUM_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) == ModelTestAsset.TestValue.ENUM_2
        modelTestAsset.getAttribute(ModelTestAsset.ALLOWED_VALUES_STRING_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) == "Allowed1"
        modelTestAsset.getAttribute(ModelTestAsset.ALLOWED_VALUES_NUMBER_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) == 1.5d
        modelTestAsset.getAttribute(ModelTestAsset.PAST_TIMESTAMP_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) < timerService.getCurrentTimeMillis()
        modelTestAsset.getAttribute(ModelTestAsset.PAST_OR_PRESENT_DATE_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null).getTime() <= timerService.getCurrentTimeMillis()
        modelTestAsset.getAttribute(ModelTestAsset.FUTURE_ISO8601_ATTRIBUTE_DESCRIPTOR).flatMap { TimeUtil.parseTimeIso8601(it.value.orElse(null))}.orElse(null) > timerService.getCurrentTimeMillis()
        modelTestAsset.getAttribute(ModelTestAsset.FUTURE_OR_PRESENT_TIMESTAMP_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) >= timerService.getCurrentTimeMillis()
        modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_STRING_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) == "abcde"
        modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_MAP_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null).size() == 1
        modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_MAP_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null).get("key1") == true
        modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_ARRAY_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null).length == 1
        modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_ARRAY_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null)[0] == 1
        modelTestAsset.getAttribute(ModelTestAsset.NOT_BLANK_STRING_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) == "abcde"

//        ex.constraintViolations != null
//        ex.constraintViolations.size() == 2
//        // Violation for required attribute with no value
//        ex.constraintViolations.any {it.propertyPath.any {it.name == "value"} && it.messageTemplate == ValueConstraint.NOT_NULL_MESSAGE_TEMPLATE}
//        // Violation for the overall AssetValid
//        ex.constraintViolations.any {it.messageTemplate == AssetValid.MESSAGE_TEMPLATE}
    }

    def "Test value constraints"() {


    }
//    Well known asset type with well known attribute with constraints (value type and attr descriptor):Check serialisation/deserialisationExpect attribute to be validated correctly
//
//
//
//
//    Add constraints to the attribute meta:Expect additional constraints to be applied
//
//
//
//
//    Custom number attribute with well known value type and object attribute with well known value type:Check serialisation/deserialisationExpect attributes to be validated correctly
//    Expect values to be hydrated correctly
//    Change values of above attributes to not be correct type:Check validationCheck deserialisation (should be null values) 
//
//    Change value types of above attributes (simulate removed value types):Check serialisation/deserialisationExpect attribute to be validated correctly
//
//
//    Simulate removed value descriptor:Expect asset to deserialiseExpect attributes to be presentExpect attribute value type to be unknown with primitive type (number or string) Expect descriptor and attribute constraints to be applied


    def "Retrieving all asset model info"() {

        when: "an asset info is serialised"
        def thingAssetInfo = ValueUtil.getAssetInfo(ThingAsset.class).orElse(null)
        def thingAssetInfoStr = ValueUtil.asJSON(thingAssetInfo)

        then: "it should contain the right information"
        thingAssetInfoStr.isPresent()

        when: "the JSON representation is deserialised"
        def thingAssetInfo2 = ValueUtil.parse(thingAssetInfoStr.get(), AssetTypeInfo.class)

        then: "it should have been successfully deserialised"
        thingAssetInfo2.isPresent()
        thingAssetInfo2.get().getAssetDescriptor().type == ThingAsset.class
        thingAssetInfo2.get().attributeDescriptors.find { (it == Asset.LOCATION) } != null
        thingAssetInfo2.get().attributeDescriptors.find { (it == Asset.LOCATION) }.required
        thingAssetInfo2.get().attributeDescriptors.find { (it == Asset.LOCATION) }.type == ValueType.GEO_JSON_POINT

        when: "the asset type value descriptor is retrieved"
        def assetValueType = ValueUtil.getValueDescriptor(ValueType.ASSET_TYPE.getName())

        then: "it should contain an allowed values constraint with all asset types listed"
        assetValueType.isPresent()
        assetValueType.get().constraints != null
        assetValueType.get().constraints.find {it instanceof ValueConstraint.AllowedValues} as ValueConstraint.AllowedValues != null
        (assetValueType.get().constraints.find {it instanceof ValueConstraint.AllowedValues} as ValueConstraint.AllowedValues).allowedValues.length == ValueUtil.getAssetInfos().length
        (assetValueType.get().constraints.find {it instanceof ValueConstraint.AllowedValues} as ValueConstraint.AllowedValues).allowedValues.any { (it == GroupAsset.class.getSimpleName()) }

        when: "All asset model infos are retrieved"
        def assetInfos = assetModelResource.getAssetInfos(null, null, null);

        then: "the asset model infos should be available"
        assetInfos.size() > 0
        assetInfos.size() == ValueUtil.assetTypeMap.size()
        def velbusTcpAgent = assetInfos.find {it.assetDescriptor.type == VelbusTCPAgent.class}
        velbusTcpAgent != null
        velbusTcpAgent.attributeDescriptors.any {it == VelbusTCPAgent.VELBUS_HOST && it.required}
        velbusTcpAgent.attributeDescriptors.any {it == VelbusTCPAgent.VELBUS_PORT && it.required}
    }

    def "Retrieving a specific asset model info"() {
        when: "The Thing Asset model info is retrieved"
        def thingAssetInfo = assetModelResource.getAssetInfo(null, null, ThingAsset.DESCRIPTOR.name)

        then: "the asset model should be available"
        thingAssetInfo != null
        thingAssetInfo.assetDescriptor != null
        thingAssetInfo.attributeDescriptors != null
        thingAssetInfo.metaItemDescriptors != null
        thingAssetInfo.valueDescriptors != null
        thingAssetInfo.attributeDescriptors.contains(Asset.LOCATION)
        thingAssetInfo.metaItemDescriptors.contains(MetaItemType.AGENT_LINK)
        ValueUtil.getAssetDescriptor(ThingAsset.class) != null
        ValueUtil.getAgentDescriptor(SimulatorAgent.class) != null

        when: "the http test server agent descriptor is retrieved (the test asset model provider should have registered test agents and assets)"
        def testAgentDescriptor = ValueUtil.getAgentDescriptor(HTTPServerTestAgent.DESCRIPTOR.name).orElse(null)

        then: "the descriptor should have been found"
        assert testAgentDescriptor != null

        and: "the descriptor should contain an agent link schema"
        def schema = ValueUtil.getSchema(AgentLink.class)
        def schema2 = ValueUtil.getSchema(ValueType.IntegerMap.class)
        assert schema != null
        assert schema.get("oneOf") != null
        assert schema.get("oneOf").size() == ValueUtil.getAssetDescriptors(null).findAll {it instanceof AgentDescriptor}.collect {(it as AgentDescriptor).getAgentLinkClass()}.unique {it.getSimpleName()}.size()
        assert schema2 != null
        assert schema2.get("type").asText() == "object"
        assert schema2.get("additionalProperties") != null
        assert schema2.get("additionalProperties").get("type").asText() == "integer"
    }

    def "Serialize/Deserialize asset model"() {
        given: "An asset"
        def asset = new LightAsset("Test light")
            .setRealm(MASTER_REALM)
            .setTemperature(100I)
            .setColourRGB(new ColourRGB(50, 100, 200))
            .addAttributes(
                new Attribute<>("testAttribute", BIG_NUMBER, 100.5, System.currentTimeMillis())
                    .addOrReplaceMeta(
                        new MetaItem<>(MetaItemType.AGENT_LINK, new HTTPAgentLink("http_agent_id")
                            .setPath("test_path")
                            .setPagingMode(true))
                    )
            )

        asset.getAttribute(LightAsset.COLOUR_RGB).ifPresent({
            it.addOrReplaceMeta(
                new MetaItem<>(MetaItemType.AGENT_LINK, new DefaultAgentLink("agent_id")
                    .setValueFilters(
                        [new SubStringValueFilter(0,10)] as ValueFilter[]
                    )
                )
            )
        })

        expect: "the attributes to match the set values"
        asset.getTemperature().orElse(null) == 100I
        asset.getColourRGB().map{it.getR()}.orElse(null) == 50I
        asset.getColourRGB().map{it.getG()}.orElse(null) == 100I
        asset.getColourRGB().map{it.getB()}.orElse(null) == 200I

        when: "the asset is serialised using default object mapper"
        def assetStr = ValueUtil.asJSON(asset).orElse(null)

        then: "the string should be valid JSON"
        def assetObjectNode = ValueUtil.parse(assetStr, ObjectNode.class).get()
        assetObjectNode.get("name").asText() == "Test light"
        assetObjectNode.get("attributes").get("colourRGB").get("timestamp") == null
        assetObjectNode.get("attributes").get("colourRGB").get("meta").get(MetaItemType.AGENT_LINK.name).isObject()
        assetObjectNode.get("attributes").get("colourRGB").get("meta").get(MetaItemType.AGENT_LINK.name).get("id").asText() == "agent_id"
        assetObjectNode.get("attributes").get("colourRGB").get("meta").get(MetaItemType.AGENT_LINK.name).get("type").asText() == DefaultAgentLink.class.getSimpleName()
        assetObjectNode.get("attributes").get("testAttribute").get("meta").get(MetaItemType.AGENT_LINK.name).isObject()
        assetObjectNode.get("attributes").get("testAttribute").get("value").decimalValue() == 100.5
        assetObjectNode.get("attributes").get("testAttribute").get("meta").get(MetaItemType.AGENT_LINK.name).get("id").asText() == "http_agent_id"
        assetObjectNode.get("attributes").get("testAttribute").get("meta").get(MetaItemType.AGENT_LINK.name).get("type").asText() == HTTPAgentLink.class.getSimpleName()

        when: "the asset is deserialized"
        def asset2 = ValueUtil.parse(assetStr, LightAsset.class).orElse(null)

        then: "it should match the original"
        asset.getName() == asset2.getName()
        asset2.getType() == asset.getType()
        asset2.getTemperature().orElse(null) == asset.getTemperature().orElse(null)
        asset2.getColourRGB().map{it.getR()}.orElse(null) == asset.getColourRGB().map{it.getR()}.orElse(null)
        asset2.getColourRGB().map{it.getG()}.orElse(null) == asset.getColourRGB().map{it.getG()}.orElse(null)
        asset2.getColourRGB().map{it.getB()}.orElse(null) == asset.getColourRGB().map{it.getB()}.orElse(null)
        asset2.getAttribute("testAttribute", BIG_NUMBER.type).flatMap{it.getMetaValue(MetaItemType.AGENT_LINK)}.orElse(null) instanceof HTTPAgentLink
        asset2.getAttribute("testAttribute", BIG_NUMBER.type).flatMap{it.getMetaValue(MetaItemType.AGENT_LINK)}.map{(HTTPAgentLink)it}.flatMap{it.path}.orElse("") == "test_path"
        asset2.getAttribute("testAttribute", BIG_NUMBER.type).flatMap{it.getMetaValue(MetaItemType.AGENT_LINK)}.map{(HTTPAgentLink)it}.flatMap{it.pagingMode}.orElse(false)

        when: "an attribute is cloned"
        def attribute = asset2.getAttribute(LightAsset.COLOUR_RGB).get()
        def clonedAttribute = ValueUtil.clone(attribute)

        then: "the cloned attribute should match the source"
        clonedAttribute.getName() == attribute.getName()
        clonedAttribute.getValue().orElse(null) == attribute.getValue().orElse(null)
        clonedAttribute.getMeta() == attribute.getMeta()

        when: "an asset state is serialized"
        def assetState = new AssetState(asset2, attribute, null)
        def assetStateStr = ValueUtil.asJSON(assetState).orElse(null)

        then: "it should look as expected"
        def assetStateObjectNode = ValueUtil.parse(assetStateStr, ObjectNode.class).get()
        assetStateObjectNode.get("name").asText() == LightAsset.COLOUR_RGB.name
        assetStateObjectNode.get("value").isTextual()
        assetStateObjectNode.get("value").asText() == "#3264C8"
    }
}
