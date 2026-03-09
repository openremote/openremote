/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.test.protocol.entsoe

import jakarta.ws.rs.client.ClientRequestContext
import jakarta.ws.rs.client.ClientRequestFilter
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.openremote.agent.protocol.entsoe.EntsoeAgent
import org.openremote.agent.protocol.entsoe.EntsoeAgentLink
import org.openremote.agent.protocol.entsoe.EntsoeProtocol
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetPredictedDatapointService
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import org.openremote.model.datapoint.ValueDatapoint
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.io.IOException
import java.time.Instant

import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.ValueType.NUMBER

class EntsoeProtocolTest extends Specification implements ManagerContainerTrait {

    @Shared
    Map<String, Integer> requestCountByZone = [:].withDefault { 0 }

    @Shared
    def mockServer = new ClientRequestFilter() {

        @Override
        void filter(ClientRequestContext requestContext) throws IOException {
            def requestUri = requestContext.uri

            if (requestUri.host == "web-api.tp.entsoe.eu" && requestUri.path == "/api") {
                def zone = requestUri.query?.split("&")
                        ?.find { it.startsWith("in_Domain=") }
                        ?.split("=")
                        ?.with { it.size() > 1 ? it[1] : null }

                def content
                if (zone == "10YBE----------2") {
                    content = '''<?xml version="1.0" encoding="utf-8"?>
<Publication_MarketDocument xmlns="urn:iec62325.351:tc57wg16:451-3:publicationdocument:7:3">
  <mRID>68f443af488b4a0a9c4442bddd8c59c0</mRID>
  <revisionNumber>1</revisionNumber>
  <type>A44</type>
  <sender_MarketParticipant.mRID codingScheme="A01">10X1001A1001A450</sender_MarketParticipant.mRID>
  <sender_MarketParticipant.marketRole.type>A32</sender_MarketParticipant.marketRole.type>
  <receiver_MarketParticipant.mRID codingScheme="A01">10X1001A1001A450</receiver_MarketParticipant.mRID>
  <receiver_MarketParticipant.marketRole.type>A33</receiver_MarketParticipant.marketRole.type>
  <createdDateTime>2026-02-17T09:45:56Z</createdDateTime>
  <period.timeInterval>
    <start>2026-02-16T23:00Z</start>
    <end>2026-02-17T23:00Z</end>
  </period.timeInterval>
  <TimeSeries>
    <mRID>1</mRID>
    <auction.type>A01</auction.type>
    <businessType>A62</businessType>
    <in_Domain.mRID codingScheme="A01">10YBE----------2</in_Domain.mRID>
    <out_Domain.mRID codingScheme="A01">10YBE----------2</out_Domain.mRID>
    <contract_MarketAgreement.type>A01</contract_MarketAgreement.type>
    <currency_Unit.name>EUR</currency_Unit.name>
    <price_Measure_Unit.name>MWH</price_Measure_Unit.name>
    <curveType>A03</curveType>
    <Period>
      <timeInterval>
        <start>2026-02-16T23:00Z</start>
        <end>2026-02-17T23:00Z</end>
      </timeInterval>
      <resolution>PT15M</resolution>
      <Point>
        <position>1</position>
        <price.amount>73.24</price.amount>
      </Point>
      <Point>
        <position>2</position>
        <price.amount>69.79</price.amount>
      </Point>
      <Point>
        <position>3</position>
        <price.amount>65.84</price.amount>
      </Point>
      <Point>
        <position>4</position>
        <price.amount>65.05</price.amount>
      </Point>
    </Period>
  </TimeSeries>
</Publication_MarketDocument>
'''
                } else if (zone == "10YNL----------L") {
                    content = '''<?xml version="1.0" encoding="utf-8"?>
<Publication_MarketDocument xmlns="urn:iec62325.351:tc57wg16:451-3:publicationdocument:7:3">
  <mRID>bbf443af488b4a0a9c4442bddd8c59c9</mRID>
  <revisionNumber>1</revisionNumber>
  <type>A44</type>
  <createdDateTime>2026-02-17T09:45:56Z</createdDateTime>
  <period.timeInterval>
    <start>2026-02-16T23:00Z</start>
    <end>2026-02-17T23:00Z</end>
  </period.timeInterval>
  <TimeSeries>
    <mRID>1</mRID>
    <curveType>A03</curveType>
    <Period>
      <timeInterval>
        <start>2026-02-16T23:00Z</start>
        <end>2026-02-17T23:00Z</end>
      </timeInterval>
      <resolution>PT15M</resolution>
      <Point>
        <position>1</position>
        <price.amount>81.11</price.amount>
      </Point>
      <Point>
        <position>2</position>
        <price.amount>82.22</price.amount>
      </Point>
      <Point>
        <position>3</position>
        <price.amount>83.33</price.amount>
      </Point>
      <Point>
        <position>4</position>
        <price.amount>84.44</price.amount>
      </Point>
    </Period>
  </TimeSeries>
</Publication_MarketDocument>
'''
                } else if (zone == "10YERR----------X") {
                    requestCountByZone[zone] = requestCountByZone[zone] + 1

                    if (requestCountByZone[zone] == 1) {
                        content = '''<?xml version="1.0" encoding="utf-8"?>
<Publication_MarketDocument xmlns="urn:iec62325.351:tc57wg16:451-3:publicationdocument:7:3">
  <period.timeInterval>
    <start>2026-02-16T23:00Z</start>
    <end>2026-02-17T23:00Z</end>
  </period.timeInterval>
  <TimeSeries>
    <Period>
      <timeInterval>
        <start>2026-02-16T23:00Z</start>
        <end>2026-02-17T23:00Z</end>
      </timeInterval>
      <resolution>PT15M</resolution>
      <Point>
        <position>1</position>
        <price.amount>91.11</price.amount>
      </Point>
      <Point>
        <position>2</position>
        <price.amount>92.22</price.amount>
      </Point>
      <Point>
        <position>3</position>
        <price.amount>93.33</price.amount>
      </Point>
      <Point>
        <position>4</position>
        <price.amount>94.44</price.amount>
      </Point>
    </Period>
  </TimeSeries>
</Publication_MarketDocument>
'''
                    } else {
                        requestContext.abortWith(Response.status(500).build())
                        return
                    }
                } else {
                    requestContext.abortWith(Response.serverError().build())
                    return
                }

                requestContext.abortWith(Response.ok(content, MediaType.APPLICATION_XML_TYPE).build())
                return
            }

            requestContext.abortWith(Response.serverError().build())
        }
    }

    def "ENTSO-E integration test writes predicted datapoints from publication document"() {
        given: "the container environment is started"
        requestCountByZone.clear()
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        EntsoeProtocol.initClient()

        if (!EntsoeProtocol.client.get().configuration.isRegistered(mockServer)) {
            EntsoeProtocol.client.get().register(mockServer, Integer.MAX_VALUE)
        }

        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)
        def agentService = container.getService(AgentService.class)

        when: "an ENTSO-E agent is created"
        def agent = new EntsoeAgent("ENTSO-E Agent")
                .setRealm(MASTER_REALM)
                .setSecurityToken("test-token")
        agent = assetStorageService.merge(agent)

        then: "the protocol instance for the agent should be created and connected"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((EntsoeProtocol) agentService.getProtocolInstance(agent.id)) != null
            assert agentService.getAgent(agent.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        when: "an asset attribute is linked to the ENTSO-E agent"
        def entsoeLink = new EntsoeAgentLink(agent.id)
        entsoeLink.setZone("10YBE----------2")

        def asset = new ThingAsset("Energy Price Asset")
                .setRealm(MASTER_REALM)
                .addOrReplaceAttributes(
                        new Attribute<>("energyPrice", NUMBER)
                                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, entsoeLink))
                )
        asset = assetStorageService.merge(asset)

        def attributeRef = new AttributeRef(asset.id, "energyPrice")
        def protocol = (EntsoeProtocol) agentService.getProtocolInstance(agent.id)
        List<List> firstSnapshot = null

        and: "the attribute is linked by protocol"
        conditions.eventually {
            assert protocol.getLinkedAttributes().containsKey(attributeRef)
        }

        and: "a polling update is triggered"
        protocol.updateAllLinkedAttributes()

        then: "predicted datapoints are written using period start and resolution"
        conditions.eventually {
            List<ValueDatapoint> datapoints = assetPredictedDatapointService.getDatapoints(attributeRef)
            assert datapoints.size() == 4

            def asc = datapoints.sort { it.timestamp }
            def start = Instant.parse("2026-02-16T23:00:00Z").toEpochMilli()
            def step = 15 * 60 * 1000L

            assert asc[0].timestamp == start
            assert asc[1].timestamp == start + step
            assert asc[2].timestamp == start + (2 * step)
            assert asc[3].timestamp == start + (3 * step)

            assert (asc[0].value as BigDecimal).compareTo(73.24G) == 0
            assert (asc[1].value as BigDecimal).compareTo(69.79G) == 0
            assert (asc[2].value as BigDecimal).compareTo(65.84G) == 0
            assert (asc[3].value as BigDecimal).compareTo(65.05G) == 0
        }

        cleanup: "remove mock client"
        if (EntsoeProtocol.client.get() != null) {
            EntsoeProtocol.client.set(null)
        }
    }

    def "ENTSO-E integration test writes predicted datapoints for 2 linked attributes with different zones"() {
        given: "the container environment is started"
        requestCountByZone.clear()
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        EntsoeProtocol.initClient()

        if (!EntsoeProtocol.client.get().configuration.isRegistered(mockServer)) {
            EntsoeProtocol.client.get().register(mockServer, Integer.MAX_VALUE)
        }

        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)
        def agentService = container.getService(AgentService.class)

        when: "an ENTSO-E agent is created"
        def agent = new EntsoeAgent("ENTSO-E Agent")
                .setRealm(MASTER_REALM)
                .setSecurityToken("test-token")
        agent = assetStorageService.merge(agent)

        then: "the protocol instance for the agent should be created and connected"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((EntsoeProtocol) agentService.getProtocolInstance(agent.id)) != null
            assert agentService.getAgent(agent.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        when: "2 attributes are linked to the same agent with different zones"
        def beLink = new EntsoeAgentLink(agent.id)
        beLink.setZone("10YBE----------2")
        def nlLink = new EntsoeAgentLink(agent.id)
        nlLink.setZone("10YNL----------L")

        def asset = new ThingAsset("Multi Zone Energy Price Asset")
                .setRealm(MASTER_REALM)
                .addOrReplaceAttributes(
                        new Attribute<>("energyPriceBe", NUMBER)
                                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, beLink)),
                        new Attribute<>("energyPriceNl", NUMBER)
                                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, nlLink))
                )
        asset = assetStorageService.merge(asset)

        def beRef = new AttributeRef(asset.id, "energyPriceBe")
        def nlRef = new AttributeRef(asset.id, "energyPriceNl")
        def protocol = (EntsoeProtocol) agentService.getProtocolInstance(agent.id)

        and: "both attributes are linked by protocol"
        conditions.eventually {
            assert protocol.getLinkedAttributes().containsKey(beRef)
            assert protocol.getLinkedAttributes().containsKey(nlRef)
        }

        and: "a polling update is triggered"
        protocol.updateAllLinkedAttributes()

        then: "each attribute receives predicted datapoints for its configured zone"
        conditions.eventually {
            List<ValueDatapoint> beDatapoints = assetPredictedDatapointService.getDatapoints(beRef).sort { it.timestamp }
            List<ValueDatapoint> nlDatapoints = assetPredictedDatapointService.getDatapoints(nlRef).sort { it.timestamp }

            assert beDatapoints.size() == 4
            assert nlDatapoints.size() == 4

            assert (beDatapoints[0].value as BigDecimal).compareTo(73.24G) == 0
            assert (beDatapoints[1].value as BigDecimal).compareTo(69.79G) == 0
            assert (beDatapoints[2].value as BigDecimal).compareTo(65.84G) == 0
            assert (beDatapoints[3].value as BigDecimal).compareTo(65.05G) == 0

            assert (nlDatapoints[0].value as BigDecimal).compareTo(81.11G) == 0
            assert (nlDatapoints[1].value as BigDecimal).compareTo(82.22G) == 0
            assert (nlDatapoints[2].value as BigDecimal).compareTo(83.33G) == 0
            assert (nlDatapoints[3].value as BigDecimal).compareTo(84.44G) == 0
        }

        cleanup: "remove mock client"
        if (EntsoeProtocol.client.get() != null) {
            EntsoeProtocol.client.set(null)
        }
    }

    def "ENTSO-E integration test keeps existing predicted datapoints when subsequent poll fetch fails"() {
        given: "the container environment is started"
        requestCountByZone.clear()
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        EntsoeProtocol.initClient()

        if (!EntsoeProtocol.client.get().configuration.isRegistered(mockServer)) {
            EntsoeProtocol.client.get().register(mockServer, Integer.MAX_VALUE)
        }

        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)
        def agentService = container.getService(AgentService.class)

        when: "an ENTSO-E agent is created"
        def agent = new EntsoeAgent("ENTSO-E Agent")
                .setRealm(MASTER_REALM)
                .setSecurityToken("test-token")
        agent = assetStorageService.merge(agent)

        then: "the protocol instance for the agent should be created and connected"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((EntsoeProtocol) agentService.getProtocolInstance(agent.id)) != null
            assert agentService.getAgent(agent.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        when: "an attribute is linked to a zone that fails after first successful fetch"
        def errLink = new EntsoeAgentLink(agent.id)
        errLink.setZone("10YERR----------X")

        def asset = new ThingAsset("Error On Second Poll Asset")
                .setRealm(MASTER_REALM)
                .addOrReplaceAttributes(
                        new Attribute<>("energyPrice", NUMBER)
                                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, errLink))
                )
        asset = assetStorageService.merge(asset)

        def attributeRef = new AttributeRef(asset.id, "energyPrice")
        def protocol = (EntsoeProtocol) agentService.getProtocolInstance(agent.id)

        and: "the attribute is linked by protocol"
        conditions.eventually {
            assert protocol.getLinkedAttributes().containsKey(attributeRef)
        }

        and: "first poll succeeds"
        protocol.updateAllLinkedAttributes()

        then: "predicted datapoints are written"
        def firstSnapshot
        conditions.eventually {
            List<ValueDatapoint> datapoints = assetPredictedDatapointService.getDatapoints(attributeRef).sort { it.timestamp }
            assert datapoints.size() == 4
            assert (datapoints[0].value as BigDecimal).compareTo(91.11G) == 0
            assert (datapoints[1].value as BigDecimal).compareTo(92.22G) == 0
            assert (datapoints[2].value as BigDecimal).compareTo(93.33G) == 0
            assert (datapoints[3].value as BigDecimal).compareTo(94.44G) == 0
            firstSnapshot = datapoints.collect { [it.timestamp, (it.value as BigDecimal)] }
        }

        when: "next poll cycle fetch fails"
        protocol.updateAllLinkedAttributes()

        then: "existing predicted datapoints remain available"
        conditions.eventually {
            List<ValueDatapoint> datapoints = assetPredictedDatapointService.getDatapoints(attributeRef).sort { it.timestamp }
            assert datapoints.size() == 4
            assert firstSnapshot != null
            assert datapoints.collect { [it.timestamp, (it.value as BigDecimal)] } == firstSnapshot
        }

        cleanup: "remove mock client"
        if (EntsoeProtocol.client.get() != null) {
            EntsoeProtocol.client.set(null)
        }
    }
}
