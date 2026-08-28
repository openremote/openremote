/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
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
import tools.jackson.databind.SerializationFeature

class Jackson2ModelBridgeTest extends Specification {

    @Shared
    ObjectMapper jackson2

    // ValueUtil.JSON is a mutable global that a dev mode container (started by another test in the
    // same JVM) rebuilds with INDENT_OUTPUT, so pin a compact writer to keep output deterministic
    @Shared
    tools.jackson.databind.ObjectWriter jackson3

    def setupSpec() {
        if (ValueUtil.getValueDescriptor("text").isEmpty() || ValueUtil.getMetaItemDescriptor("readOnly").isEmpty()) {
            ValueUtil.initialise(null)
        }
        jackson2 = Jackson2Config.configureObjectMapper(new ObjectMapper())
        jackson3 = ValueUtil.JSON.writer().without(SerializationFeature.INDENT_OUTPUT)
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

    def "Jackson 2 preserves null attribute values and empty meta"() {
        given: "an attribute with an explicit null value, empty meta and a timestamp"
        def attribute = new Attribute<Double>("power", ValueType.NUMBER)
            .setMeta(new MetaMap())
            .setTimestamp(123L)

        when: "the attribute is serialized with the Jackson 2 bridge"
        String json = jackson2.writeValueAsString(attribute)

        then: "the null value and empty meta are not stripped"
        json.contains('"value":null')
        json.contains('"meta":{}')
        json.contains('"timestamp":123')

        and: "Jackson 3 emits the same fields"
        String json3 = jackson3.writeValueAsString(attribute)
        json3.contains('"value":null')
        json3.contains('"meta":{}')
        json3.contains('"timestamp":123')

        when: "the Jackson 2 JSON is read back by the bridge"
        Attribute<?> result = jackson2.readValue(json, Attribute.class)

        then: "the attribute state survives the round trip"
        result.name == "power"
        result.type == ValueType.NUMBER
        result.value.isEmpty()
        result.timestamp.orElse(null) == 123L
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
