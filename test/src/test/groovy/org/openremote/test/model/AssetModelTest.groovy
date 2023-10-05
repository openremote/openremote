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
import org.openremote.model.geo.GeoJSONPoint
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

import java.lang.reflect.Array
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
        stopPseudoClock()

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
                new Attribute<>(ModelTestAsset.ALLOWED_VALUES_ENUM_ATTRIBUTE_DESCRIPTOR, ModelTestAsset.TestValue.ENUM_2).addMeta(
                        new MetaItem<>(MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.NotNull()))
                ),
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
                new Attribute<>(ModelTestAsset.NOT_BLANK_STRING_ATTRIBUTE_DESCRIPTOR, "abcde"),
                new Attribute<>(ModelTestAsset.OBJECT_ATTRIBUTE_DESCRIPTOR, new ModelTestAsset.TestObject(110, true)),
                // Custom attributes
                new Attribute<>("custom1"),
                new Attribute<>("custom2", ValueType.GEO_JSON_POINT, new GeoJSONPoint(1.234, 5.678)).addMeta(
                        new MetaItem<>(MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.NotNull()))
                )
        )
        def updatedModelTestAsset = assetResource.update(null, modelTestAsset.id, modelTestAsset)

        then: "the save should succeed and correct values should be stored"
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
        modelTestAsset.getAttribute(ModelTestAsset.SIZE_ARRAY_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null)[1] == "arrayValue2"
        modelTestAsset.getAttribute(ModelTestAsset.ALLOWED_VALUES_ENUM_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) == ModelTestAsset.TestValue.ENUM_2
        modelTestAsset.getAttribute(ModelTestAsset.ALLOWED_VALUES_STRING_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) == "Allowed1"
        modelTestAsset.getAttribute(ModelTestAsset.ALLOWED_VALUES_NUMBER_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) == 1.5d
        modelTestAsset.getAttribute(ModelTestAsset.PAST_TIMESTAMP_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) < timerService.getCurrentTimeMillis()
        modelTestAsset.getAttribute(ModelTestAsset.PAST_OR_PRESENT_DATE_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null).getTime() <= timerService.getCurrentTimeMillis()
        modelTestAsset.getAttribute(ModelTestAsset.FUTURE_ISO8601_ATTRIBUTE_DESCRIPTOR).map { TimeUtil.parseTimeIso8601(it.value.orElse(null)).toEpochMilli()}.orElse(null) > timerService.getCurrentTimeMillis()
        modelTestAsset.getAttribute(ModelTestAsset.FUTURE_OR_PRESENT_TIMESTAMP_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) >= timerService.getCurrentTimeMillis()
        modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_STRING_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) == "abcde"
        modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_MAP_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null).size() == 1
        modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_MAP_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null).get("key1") == true
        modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_ARRAY_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null).length == 1
        modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_ARRAY_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null)[0] == 1
        modelTestAsset.getAttribute(ModelTestAsset.NOT_BLANK_STRING_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) == "abcde"
        modelTestAsset.getAttribute("custom1").flatMap {it.value}.orElse(null) == null
        modelTestAsset.getAttribute("custom2").flatMap {it.value}.orElse(null).x == 1.234d
        modelTestAsset.getAttribute("custom2").flatMap {it.value}.orElse(null).y == 5.678d
        modelTestAsset.getAttribute(ModelTestAsset.OBJECT_ATTRIBUTE_DESCRIPTOR).flatMap {it.value} != null
        modelTestAsset.getAttribute(ModelTestAsset.OBJECT_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.orElse(null) != null
        modelTestAsset.getAttribute(ModelTestAsset.OBJECT_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.get().range == 110
        modelTestAsset.getAttribute(ModelTestAsset.OBJECT_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.get().active != null
        modelTestAsset.getAttribute(ModelTestAsset.OBJECT_ATTRIBUTE_DESCRIPTOR).flatMap {it.value}.get().active.booleanValue()

        when: "The attribute values violate the constraints"
        modelTestAsset.getAttribute(ModelTestAsset.REQUIRED_POSITIVE_INT_ATTRIBUTE_DESCRIPTOR).ifPresent {it.value = -1}
        modelTestAsset.getAttribute(ModelTestAsset.NEGATIVE_INT_ATTRIBUTE_DESCRIPTOR).ifPresent {it.value = 1}
        modelTestAsset.getAttribute(ModelTestAsset.SIZE_STRING_ATTRIBUTE_DESCRIPTOR).ifPresent {it.value = "abc"}
        modelTestAsset.getAttribute(ModelTestAsset.SIZE_MAP_ATTRIBUTE_DESCRIPTOR).ifPresent {it.value = [
                ("key1"): 1d
        ] as ValueType.DoubleMap}
        modelTestAsset.getAttribute(ModelTestAsset.SIZE_ARRAY_ATTRIBUTE_DESCRIPTOR).ifPresent {it.value = [
                "arrayValue1"
        ] as String[]}
        modelTestAsset.getAttribute(ModelTestAsset.ALLOWED_VALUES_ENUM_ATTRIBUTE_DESCRIPTOR).ifPresent {
            it.value = null
        }
        modelTestAsset.getAttribute(ModelTestAsset.ALLOWED_VALUES_STRING_ATTRIBUTE_DESCRIPTOR).ifPresent {it.value = "NotAllowed"}
        modelTestAsset.getAttribute(ModelTestAsset.ALLOWED_VALUES_NUMBER_ATTRIBUTE_DESCRIPTOR).ifPresent {it.value = 3.0d}
        modelTestAsset.getAttribute(ModelTestAsset.PAST_TIMESTAMP_ATTRIBUTE_DESCRIPTOR).ifPresent {it.value = timerService.getCurrentTimeMillis() + 100000}
        modelTestAsset.getAttribute(ModelTestAsset.PAST_OR_PRESENT_DATE_ATTRIBUTE_DESCRIPTOR).ifPresent {it.value = timerService.getNow().plusMillis(100000).toDate()}
        modelTestAsset.getAttribute(ModelTestAsset.FUTURE_ISO8601_ATTRIBUTE_DESCRIPTOR).ifPresent { it.value = DateTimeFormatter.ISO_INSTANT.format(timerService.getNow().minus(1, ChronoUnit.DAYS))}
        modelTestAsset.getAttribute(ModelTestAsset.FUTURE_OR_PRESENT_TIMESTAMP_ATTRIBUTE_DESCRIPTOR).ifPresent {it.value = timerService.getNow().minus(1, ChronoUnit.DAYS).toEpochMilli()}
        modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_STRING_ATTRIBUTE_DESCRIPTOR).ifPresent {it.value = ""}
        modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_MAP_ATTRIBUTE_DESCRIPTOR).ifPresent {it.value = [] as ValueType.BooleanMap}
        modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_ARRAY_ATTRIBUTE_DESCRIPTOR).ifPresent {it.value = [] as Integer[]}
        modelTestAsset.getAttribute(ModelTestAsset.NOT_BLANK_STRING_ATTRIBUTE_DESCRIPTOR).ifPresent {it.value = "      "}
        modelTestAsset.getAttribute(ModelTestAsset.OBJECT_ATTRIBUTE_DESCRIPTOR).ifPresent {it.value = new ModelTestAsset.TestObject(90, null)}
        modelTestAsset.getAttribute("custom1").ifPresent {it.value = 123}
        modelTestAsset.getAttribute("custom2").ifPresent {it.value = null}
        modelTestAsset = assetResource.update(null, modelTestAsset.id, modelTestAsset)

        then: "a constraint violation exception should be thrown"
        ex = thrown()
        ex.response.status == 400

        when: "the report is extracted"
        report = ex.response.readEntity(ViolationReport)

        then: "it should contain all the errors"
        report.propertyViolations.size() == 19
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.REQUIRED_POSITIVE_INT_ATTRIBUTE_DESCRIPTOR.name}].value" && it.message == "must be greater than or equal to 0"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.NEGATIVE_INT_ATTRIBUTE_DESCRIPTOR.name}].value" && it.message == "must be less than or equal to 0"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.SIZE_STRING_ATTRIBUTE_DESCRIPTOR.name}].value" && it.message == "size must be between 5 and 10"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.SIZE_MAP_ATTRIBUTE_DESCRIPTOR.name}].value" && it.message == "size must be between 2 and 3"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.SIZE_ARRAY_ATTRIBUTE_DESCRIPTOR.name}].value" && it.message == "size must be between 2 and 3"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.ALLOWED_VALUES_ENUM_ATTRIBUTE_DESCRIPTOR.name}].value" && it.message == "must not be null"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.ALLOWED_VALUES_STRING_ATTRIBUTE_DESCRIPTOR.name}].value" && it.message == "must be one of [Allowed1, Allowed2]"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.ALLOWED_VALUES_NUMBER_ATTRIBUTE_DESCRIPTOR.name}].value" && it.message == "must be one of [1.5, 2.5]"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.PAST_TIMESTAMP_ATTRIBUTE_DESCRIPTOR.name}].value" && it.message == "must be a past date"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.PAST_OR_PRESENT_DATE_ATTRIBUTE_DESCRIPTOR.name}].value" && it.message == "must be a date in the past or in the present"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.FUTURE_ISO8601_ATTRIBUTE_DESCRIPTOR.name}].value" && it.message == "must be a future date"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.FUTURE_OR_PRESENT_TIMESTAMP_ATTRIBUTE_DESCRIPTOR.name}].value" && it.message == "must be a date in the present or in the future"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.NOT_EMPTY_STRING_ATTRIBUTE_DESCRIPTOR.name}].value" && it.message == "must not be empty"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.NOT_EMPTY_MAP_ATTRIBUTE_DESCRIPTOR.name}].value" && it.message == "must not be empty"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.NOT_EMPTY_ARRAY_ATTRIBUTE_DESCRIPTOR.name}].value" && it.message == "must not be empty"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.NOT_BLANK_STRING_ATTRIBUTE_DESCRIPTOR.name}].value" && it.message == "Not blank custom message"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.OBJECT_ATTRIBUTE_DESCRIPTOR.name}].value.range" && it.message == "must be greater than or equal to 100"}
        report.propertyViolations.any {it.path == "attributes[${ModelTestAsset.OBJECT_ATTRIBUTE_DESCRIPTOR.name}].value.active" && it.message == "must not be null"}
        report.classViolations.size() == 1
        report.classViolations.get(0).path == ""
        report.classViolations.get(0).message == "Asset is not valid"

        when: "we convert the asset to JSON object so we can manipulate it"
        modelTestAsset = updatedModelTestAsset // Reset attributes to valid values
        def assetObjectNode = ValueUtil.convert(modelTestAsset, ObjectNode) as ObjectNode

        and: "we simulate an attribute descriptor being removed or changed (by changing the attribute name)"
        def noDescriptorAttr = ((ObjectNode)assetObjectNode.get("attributes")).remove("pastTimestamp") as ObjectNode
        noDescriptorAttr.put("name", "missingAttr")
        ((ObjectNode)assetObjectNode.get("attributes")).set("missingAttr", noDescriptorAttr)

        and: "we simulate a meta item descriptor being removed or changed (by changing the meta item name)"
        def noDescriptorMeta = ((ObjectNode)((ObjectNode)((ObjectNode)assetObjectNode.get("attributes")).get(ModelTestAsset.NOT_EMPTY_STRING_ATTRIBUTE_DESCRIPTOR.name)).get("meta")).remove(MetaItemType.CONSTRAINTS.name)
        ((ObjectNode)((ObjectNode)((ObjectNode)assetObjectNode.get("attributes")).get(ModelTestAsset.NOT_EMPTY_STRING_ATTRIBUTE_DESCRIPTOR.name)).get("meta")).set("invalid", noDescriptorMeta)

        and: "we deserialise the object back into an Asset"
        modelTestAsset = ValueUtil.parse(assetObjectNode.toString(), Asset).orElse(null) as ModelTestAsset

        then: "the attribute with the missing descriptor should now have an ANY value type"
        modelTestAsset.getAttribute("missingAttr").map {it.type}.orElse(null) == ValueType.ANY

        and: "it should still contain the value"
        modelTestAsset.getAttribute("missingAttr").flatMap {it.value}.orElse(null) instanceof Long

        and: "the meta item with the missing descriptor should now have an ANY value type and the value should be an array of generic maps representing the value constraints"
        modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_STRING_ATTRIBUTE_DESCRIPTOR).flatMap {it.getMeta().get("invalid")}.orElse(null) != null
        modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_STRING_ATTRIBUTE_DESCRIPTOR).flatMap {it.getMeta().get("invalid")}.orElse(null).type == ValueType.ANY
        modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_STRING_ATTRIBUTE_DESCRIPTOR).flatMap {it.getMeta().get("invalid")}.flatMap {it.value}.orElse(null).getClass().isArray()
        Array.getLength(modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_STRING_ATTRIBUTE_DESCRIPTOR).flatMap {it.getMeta().get("invalid")}.flatMap {it.value}.orElse(null)) == 1
        Array.get(modelTestAsset.getAttribute(ModelTestAsset.NOT_EMPTY_STRING_ATTRIBUTE_DESCRIPTOR).flatMap {it.getMeta().get("invalid")}.flatMap {it.value}.orElse(null), 0) instanceof Map

        when: "the attribute with the missing attribute descriptor now violates its' previous constraint"
        modelTestAsset.getAttribute("missingAttr").ifPresent {it.value = timerService.getCurrentTimeMillis() + 100000}

        and: "this asset is now merged"
        modelTestAsset = assetResource.update(null, modelTestAsset.id, modelTestAsset)

        then: "it should succeed and attribute value should be correct"
        modelTestAsset != null
        modelTestAsset.getAttribute("missingAttr").flatMap {it.value}.orElse(null) == timerService.getCurrentTimeMillis() + 100000

        and: "the custom attribute with the missing value type should now just be a number array"
        modelTestAsset.getAttribute("custom2").get().type == ValueType.ANY
        modelTestAsset.getAttribute("custom2").get().value.isPresent()
        modelTestAsset.getAttribute("custom2").get().value.get() instanceof Map
        (modelTestAsset.getAttribute("custom2").get().value.get() as Map).get("type") == "Point"
        (modelTestAsset.getAttribute("custom2").get().value.get() as Map).get("coordinates") != null
        (modelTestAsset.getAttribute("custom2").get().value.get() as Map).get("coordinates").class.isArray()
        Array.getLength((modelTestAsset.getAttribute("custom2").get().value.get() as Map).get("coordinates")) == 2
        Array.get((modelTestAsset.getAttribute("custom2").get().value.get() as Map).get("coordinates"), 0) == 1.234
        Array.get((modelTestAsset.getAttribute("custom2").get().value.get() as Map).get("coordinates"), 1) == 5.678
    }

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
        !thingAssetInfo2.get().attributeDescriptors.find { (it == Asset.LOCATION) }.optional
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
        velbusTcpAgent.attributeDescriptors.any {it == VelbusTCPAgent.HOST && !it.optional}
        velbusTcpAgent.attributeDescriptors.any {it == VelbusTCPAgent.PORT && !it.optional}
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
        asset2.getAttribute("testAttribute").flatMap{it.getMetaValue(MetaItemType.AGENT_LINK)}.orElse(null) instanceof HTTPAgentLink
        asset2.getAttribute("testAttribute").flatMap{it.getMetaValue(MetaItemType.AGENT_LINK)}.map{(HTTPAgentLink)it}.flatMap{it.path}.orElse("") == "test_path"
        asset2.getAttribute("testAttribute").flatMap{it.getMetaValue(MetaItemType.AGENT_LINK)}.map{(HTTPAgentLink)it}.flatMap{it.pagingMode}.orElse(false)

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
