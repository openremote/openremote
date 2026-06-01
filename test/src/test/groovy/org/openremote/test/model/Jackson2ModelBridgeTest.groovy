package org.openremote.test.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.openremote.container.json.Jackson2Config
import org.openremote.model.asset.AssetDescriptor
import org.openremote.model.asset.AssetTypeInfo
import org.openremote.model.asset.agent.AgentLink
import org.openremote.model.asset.agent.DefaultAgentLink
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.MetaItem
import org.openremote.model.attribute.MetaMap
import org.openremote.model.util.ValueUtil
import org.openremote.model.value.AttributeDescriptor
import org.openremote.model.value.MetaItemDescriptor
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueDescriptor
import org.openremote.model.value.ValueType
import spock.lang.Shared
import spock.lang.Specification

class Jackson2ModelBridgeTest extends Specification {

    @Shared
    ObjectMapper jackson2

    def setupSpec() {
        if (ValueUtil.getValueDescriptor("text").isEmpty() || ValueUtil.getMetaItemDescriptor("readOnly").isEmpty()) {
            ValueUtil.initialise(null)
        }
        jackson2 = Jackson2Config.configureObjectMapper(new ObjectMapper())
    }

    def "Jackson 2 reads asset model descriptors emitted by Jackson 3"() {
        given:
        def assetTypeInfo = new AssetTypeInfo(
            new AssetDescriptor("BridgeAsset", "bridge-icon", "#123456"),
            [
                new AttributeDescriptor<String>("label", ValueType.TEXT),
                new AttributeDescriptor<Integer>("brightness", ValueType.POSITIVE_INTEGER).withOptional(true)
            ] as AttributeDescriptor[],
            [
                MetaItemType.READ_ONLY,
                MetaItemType.STORE_DATA_POINTS
            ] as MetaItemDescriptor[],
            [
                ValueType.TEXT,
                ValueType.POSITIVE_INTEGER
            ] as ValueDescriptor[]
        )

        when:
        String json = ValueUtil.JSON.writeValueAsString(assetTypeInfo)
        AssetTypeInfo result = jackson2.readValue(json, AssetTypeInfo.class)

        then:
        result.assetDescriptor.name == "BridgeAsset"
        result.attributeDescriptors["label"].type == ValueType.TEXT
        result.attributeDescriptors["brightness"].type == ValueType.POSITIVE_INTEGER
        result.metaItemDescriptors*.name.containsAll("readOnly", "storeDataPoints")
        result.valueDescriptors*.name.containsAll("text", "positiveInteger")
    }

    def "Jackson 2 reads attributes with meta emitted by Jackson 3"() {
        given:
        def attribute = new Attribute<Double>("temperature", ValueType.NUMBER, 21.5d, 123L)
            .addMeta(
                new MetaItem<Boolean>(MetaItemType.READ_ONLY, true),
                new MetaItem<AgentLink>(MetaItemType.AGENT_LINK, new DefaultAgentLink("agent-1").setWriteValue("fixed"))
            )

        when:
        String json = ValueUtil.JSON.writeValueAsString(attribute)
        Attribute<?> result = jackson2.readValue(json, Attribute.class)
        AgentLink<?> agentLink = result.meta.get(MetaItemType.AGENT_LINK)
            .flatMap { it.getValue(AgentLink.class) }
            .orElse(null)

        then:
        result.name == "temperature"
        result.type == ValueType.NUMBER
        result.value.orElse(null) == 21.5d
        result.timestamp.orElse(null) == 123L
        result.meta.get(MetaItemType.READ_ONLY).flatMap { it.getValue(Boolean.class) }.orElse(false)
        agentLink instanceof DefaultAgentLink
        agentLink.id == "agent-1"
        agentLink.writeValue.orElse(null) == "fixed"
    }

    def "Jackson 2 reads meta maps with agent links emitted by Jackson 3"() {
        given:
        def metaMap = new MetaMap([
            new MetaItem<Boolean>(MetaItemType.READ_ONLY, true),
            new MetaItem<AgentLink>(MetaItemType.AGENT_LINK, new DefaultAgentLink("agent-1").setWriteValue("fixed"))
        ])

        when:
        String json = ValueUtil.JSON.writeValueAsString(metaMap)
        MetaMap result = jackson2.readValue(json, MetaMap.class)
        AgentLink<?> agentLink = result.get(MetaItemType.AGENT_LINK)
            .flatMap { it.getValue(AgentLink.class) }
            .orElse(null)

        then:
        result.get(MetaItemType.READ_ONLY).flatMap { it.getValue(Boolean.class) }.orElse(false)
        agentLink instanceof DefaultAgentLink
        agentLink.id == "agent-1"
        agentLink.writeValue.orElse(null) == "fixed"
    }

    def "Jackson 2 reads Jackson 3 object nodes emitted by Jackson 3"() {
        given:
        tools.jackson.databind.node.ObjectNode objectNode = ValueUtil.JSON.createObjectNode()
        objectNode.put("name", "bridge")
        objectNode.putObject("nested").put("enabled", true)

        when:
        String json = ValueUtil.JSON.writeValueAsString(objectNode)
        tools.jackson.databind.node.ObjectNode result = jackson2.readValue(json, tools.jackson.databind.node.ObjectNode.class)

        then:
        result.toString() == objectNode.toString()
    }
}
