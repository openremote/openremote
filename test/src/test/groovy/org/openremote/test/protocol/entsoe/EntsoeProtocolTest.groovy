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

import jakarta.validation.Validation
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

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.ValueType.NUMBER

class EntsoeProtocolTest extends Specification implements ManagerContainerTrait {
    private static final String DATASET_START = "2026-02-16T23:00:00.000Z"
    private static final String BEFORE_DATASET_START = "2026-02-16T22:00:00.000Z"

    @Shared
    Map<String, Integer> requestCountByZone = [:].withDefault { 0 }
    @Shared
    def validator = Validation.buildDefaultValidatorFactory().validator
    @Shared
    List<Map<String, String>> requestLog = Collections.synchronizedList(new ArrayList<>())

    @Shared
    def mockServer = new ClientRequestFilter() {

        @Override
        void filter(ClientRequestContext requestContext) throws IOException {
            // We want the call to take at least 1ms or we get issues with attribute events being ignored as outdated
            Thread.sleep(1)
            def requestUri = requestContext.uri

            if (requestUri.host == "web-api.tp.entsoe.eu" && requestUri.path == "/api") {
                def queryParams = [:]
                requestUri.query?.split("&")?.each { pair ->
                    def parts = pair.split("=", 2)
                    if (parts.length == 2) {
                        queryParams[parts[0]] = parts[1]
                    }
                }
                def zone = queryParams["in_Domain"]
                requestLog.add([
                        zone       : queryParams["in_Domain"],
                        periodStart: queryParams["periodStart"],
                        periodEnd  : queryParams["periodEnd"]
                ])

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
                } else if (zone == "10YER----------X") {
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
                } else if (zone == "10YNDATA-------A") {
                    content = '''<?xml version="1.0" encoding="UTF-8"?>
<Acknowledgement_MarketDocument xmlns="urn:iec62325.351:tc57wg16:451-1:acknowledgementdocument:7:0">
  <mRID>0985f391-49af-4</mRID>
  <createdDateTime>2026-03-11T06:29:10Z</createdDateTime>
  <Reason>
    <code>999</code>
    <text>No matching data found for Data item ENERGY_PRICES [12.1.D].</text>
  </Reason>
</Acknowledgement_MarketDocument>
'''
                } else if (zone == "10YGAP---------G") {
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
        <price.amount>81.11</price.amount>
      </Point>
      <Point>
        <position>2</position>
        <price.amount>82.22</price.amount>
      </Point>
      <Point>
        <position>4</position>
        <price.amount>84.44</price.amount>
      </Point>
    </Period>
  </TimeSeries>
</Publication_MarketDocument>
'''
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
        setPseudoClock(BEFORE_DATASET_START)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)
        def agentService = container.getService(AgentService.class)
        EntsoeAgent agent = null
        ThingAsset asset = null

        when: "an ENTSO-E agent is created"
        agent = new EntsoeAgent("ENTSO-E Agent")
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

        asset = new ThingAsset("Energy Price Asset")
                .setRealm(MASTER_REALM)
                .addOrReplaceAttributes(
                        new Attribute<>("energyPrice", NUMBER)
                                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, entsoeLink))
                )
        asset = assetStorageService.merge(asset)

        def attributeRef = new AttributeRef(asset.id, "energyPrice")
        def protocol = (EntsoeProtocol) agentService.getProtocolInstance(agent.id)

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
            def start = Instant.parse(DATASET_START).toEpochMilli()
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

        cleanup: "remove created assets and mock client"
        if (asset?.id) {
            assetStorageService.delete([asset.id])
        }
        if (agent?.id) {
            assetStorageService.delete([agent.id])
        }
        if (EntsoeProtocol.client.get() != null) {
            EntsoeProtocol.client.set(null)
        }
    }

    def "ENTSO-E integration test filters out points in the past when clock is mid-period"() {
        given: "the container environment is started with clock in the middle of the dataset period"
        requestCountByZone.clear()
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        EntsoeProtocol.initClient()

        if (!EntsoeProtocol.client.get().configuration.isRegistered(mockServer)) {
            EntsoeProtocol.client.get().register(mockServer, Integer.MAX_VALUE)
        }

        def container = startContainer(defaultConfig(), defaultServices())
        setPseudoClock("2026-02-16T23:20:00.000Z")
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)
        def agentService = container.getService(AgentService.class)
        EntsoeAgent agent = null
        ThingAsset asset = null

        when: "an ENTSO-E agent and linked attribute are created"
        agent = new EntsoeAgent("ENTSO-E Agent")
                .setRealm(MASTER_REALM)
                .setSecurityToken("test-token")
        agent = assetStorageService.merge(agent)

        def entsoeLink = new EntsoeAgentLink(agent.id)
        entsoeLink.setZone("10YBE----------2")

        asset = new ThingAsset("Energy Price Asset")
                .setRealm(MASTER_REALM)
                .addOrReplaceAttributes(
                        new Attribute<>("energyPrice", NUMBER)
                                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, entsoeLink))
                )
        asset = assetStorageService.merge(asset)

        def attributeRef = new AttributeRef(asset.id, "energyPrice")
        def protocol = (EntsoeProtocol) agentService.getProtocolInstance(agent.id)

        then: "the protocol is connected and attribute linked"
        conditions.eventually {
            assert protocol != null
            assert agentService.getAgent(agent.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            assert protocol.getLinkedAttributes().containsKey(attributeRef)
        }

        when: "a polling update is triggered"
        protocol.updateAllLinkedAttributes()

        then: "only datapoints after the cutoff time are stored"
        conditions.eventually {
            List<ValueDatapoint> datapoints = assetPredictedDatapointService.getDatapoints(attributeRef).sort { it.timestamp }
            assert datapoints.size() == 2

            def start = Instant.parse(DATASET_START).toEpochMilli()
            def step = 15 * 60 * 1000L

            assert datapoints[0].timestamp == start + (2 * step)
            assert datapoints[1].timestamp == start + (3 * step)
            assert (datapoints[0].value as BigDecimal).compareTo(65.84G) == 0
            assert (datapoints[1].value as BigDecimal).compareTo(65.05G) == 0
        }

        cleanup: "remove created assets and mock client"
        if (asset?.id) {
            assetStorageService.delete([asset.id])
        }
        if (agent?.id) {
            assetStorageService.delete([agent.id])
        }
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
        setPseudoClock(BEFORE_DATASET_START)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)
        def agentService = container.getService(AgentService.class)
        EntsoeAgent agent = null
        ThingAsset asset = null

        when: "an ENTSO-E agent is created"
        agent = new EntsoeAgent("ENTSO-E Agent")
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

        asset = new ThingAsset("Multi Zone Energy Price Asset")
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

        cleanup: "remove created assets and mock client"
        if (asset?.id) {
            assetStorageService.delete([asset.id])
        }
        if (agent?.id) {
            assetStorageService.delete([agent.id])
        }
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
        setPseudoClock(BEFORE_DATASET_START)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)
        def agentService = container.getService(AgentService.class)
        EntsoeAgent agent = null
        ThingAsset asset = null

        when: "an ENTSO-E agent is created"
        agent = new EntsoeAgent("ENTSO-E Agent")
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
        errLink.setZone("10YER----------X")

        asset = new ThingAsset("Error On Second Poll Asset")
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

        cleanup: "remove created assets and mock client"
        if (asset?.id) {
            assetStorageService.delete([asset.id])
        }
        if (agent?.id) {
            assetStorageService.delete([agent.id])
        }
        if (EntsoeProtocol.client.get() != null) {
            EntsoeProtocol.client.set(null)
        }
    }

    def "ENTSO-E integration test handles no-data acknowledgement response cleanly"() {
        given: "the container environment is started"
        requestCountByZone.clear()
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        EntsoeProtocol.initClient()

        if (!EntsoeProtocol.client.get().configuration.isRegistered(mockServer)) {
            EntsoeProtocol.client.get().register(mockServer, Integer.MAX_VALUE)
        }

        def container = startContainer(defaultConfig(), defaultServices())
        setPseudoClock(BEFORE_DATASET_START)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)
        def agentService = container.getService(AgentService.class)
        EntsoeAgent agent = null
        ThingAsset asset = null

        when: "an ENTSO-E agent is created"
        agent = new EntsoeAgent("ENTSO-E Agent")
                .setRealm(MASTER_REALM)
                .setSecurityToken("test-token")
        agent = assetStorageService.merge(agent)

        then: "the protocol instance for the agent should be created and connected"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((EntsoeProtocol) agentService.getProtocolInstance(agent.id)) != null
            assert agentService.getAgent(agent.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        when: "an attribute is linked to a zone that returns no-data acknowledgement"
        def noDataLink = new EntsoeAgentLink(agent.id)
        noDataLink.setZone("10YNDATA-------A")

        asset = new ThingAsset("No Data Zone Asset")
                .setRealm(MASTER_REALM)
                .addOrReplaceAttributes(
                        new Attribute<>("energyPrice", NUMBER)
                                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, noDataLink))
                )
        asset = assetStorageService.merge(asset)

        def attributeRef = new AttributeRef(asset.id, "energyPrice")
        def protocol = (EntsoeProtocol) agentService.getProtocolInstance(agent.id)

        and: "the attribute is linked by protocol"
        conditions.eventually {
            assert protocol.getLinkedAttributes().containsKey(attributeRef)
        }

        and: "a polling update is triggered"
        protocol.updateAllLinkedAttributes()

        then: "no predicted datapoints are written and no exception escapes"
        conditions.eventually {
            List<ValueDatapoint> datapoints = assetPredictedDatapointService.getDatapoints(attributeRef)
            assert datapoints.isEmpty()
        }

        cleanup: "remove created assets and mock client"
        if (asset?.id) {
            assetStorageService.delete([asset.id])
        }
        if (agent?.id) {
            assetStorageService.delete([agent.id])
        }
        if (EntsoeProtocol.client.get() != null) {
            EntsoeProtocol.client.set(null)
        }
    }

    def "ENTSO-E integration test keeps position timing when intermediate point is missing"() {
        given: "the container environment is started"
        requestCountByZone.clear()
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        EntsoeProtocol.initClient()

        if (!EntsoeProtocol.client.get().configuration.isRegistered(mockServer)) {
            EntsoeProtocol.client.get().register(mockServer, Integer.MAX_VALUE)
        }

        def container = startContainer(defaultConfig(), defaultServices())
        setPseudoClock(BEFORE_DATASET_START)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)
        def agentService = container.getService(AgentService.class)
        EntsoeAgent agent = null
        ThingAsset asset = null

        when: "an ENTSO-E agent and linked attribute are created for a zone with a missing point position"
        agent = new EntsoeAgent("ENTSO-E Agent")
                .setRealm(MASTER_REALM)
                .setSecurityToken("test-token")
        agent = assetStorageService.merge(agent)

        def gapLink = new EntsoeAgentLink(agent.id)
        gapLink.setZone("10YGAP---------G")

        asset = new ThingAsset("Gap Position Asset")
                .setRealm(MASTER_REALM)
                .addOrReplaceAttributes(
                        new Attribute<>("energyPrice", NUMBER)
                                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, gapLink))
                )
        asset = assetStorageService.merge(asset)

        def attributeRef = new AttributeRef(asset.id, "energyPrice")
        def protocol = (EntsoeProtocol) agentService.getProtocolInstance(agent.id)

        then: "the protocol is connected and attribute linked"
        conditions.eventually {
            assert protocol != null
            assert agentService.getAgent(agent.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            assert protocol.getLinkedAttributes().containsKey(attributeRef)
        }

        when: "a polling update is triggered"
        protocol.updateAllLinkedAttributes()

        then: "only provided points are stored and their timestamps follow position offsets"
        conditions.eventually {
            List<ValueDatapoint> datapoints = assetPredictedDatapointService.getDatapoints(attributeRef).sort { it.timestamp }
            assert datapoints.size() == 3

            def start = Instant.parse(DATASET_START).toEpochMilli()
            def step = 15 * 60 * 1000L

            assert datapoints[0].timestamp == start
            assert datapoints[1].timestamp == start + step
            assert datapoints[2].timestamp == start + (3 * step)
            assert datapoints[2].timestamp - datapoints[1].timestamp == 2 * step

            assert (datapoints[0].value as BigDecimal).compareTo(81.11G) == 0
            assert (datapoints[1].value as BigDecimal).compareTo(82.22G) == 0
            assert (datapoints[2].value as BigDecimal).compareTo(84.44G) == 0
        }

        cleanup: "remove created assets and mock client"
        if (asset?.id) {
            assetStorageService.delete([asset.id])
        }
        if (agent?.id) {
            assetStorageService.delete([agent.id])
        }
        if (EntsoeProtocol.client.get() != null) {
            EntsoeProtocol.client.set(null)
        }
    }

    def "ENTSO-E agent link validates zone against EIC regex pattern"() {
        given: "an ENTSO-E agent link"
        def link = new EntsoeAgentLink("agent-id")

        expect: "zone validation follows the EIC regex"
        link.setZone(zoneId)
        validator.validate(link).isEmpty() == valid

        where:
        zoneId               || valid
        "10YBE----------2"   || true
        "10YNL----------L"   || true
        "10YGAP---------G"   || true
        "1YBE----------2"    || false
        "10yBE----------2"   || false
        "10YBE----------"    || false
        "10YBE----------22"  || false
        "10YBE_____-----2"   || false
        "10YNDATA-------A"   || true
    }

    def "ENTSO-E integration test updates requested period range when clock advances by one day"() {
        given: "the container environment is started"
        requestCountByZone.clear()
        requestLog.clear()
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)
        def periodFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneId.of("UTC"))

        EntsoeProtocol.initClient()

        if (!EntsoeProtocol.client.get().configuration.isRegistered(mockServer)) {
            EntsoeProtocol.client.get().register(mockServer, Integer.MAX_VALUE)
        }

        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)
        EntsoeAgent agent = null
        ThingAsset asset = null

        when: "an ENTSO-E agent is created"
        agent = new EntsoeAgent("ENTSO-E Agent")
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
        def link = new EntsoeAgentLink(agent.id)
        link.setZone("10YBE----------2")

        asset = new ThingAsset("Time Range Asset")
                .setRealm(MASTER_REALM)
                .addOrReplaceAttributes(
                        new Attribute<>("energyPrice", NUMBER)
                                .addOrReplaceMeta(new MetaItem<>(AGENT_LINK, link))
                )
        asset = assetStorageService.merge(asset)

        def attributeRef = new AttributeRef(asset.id, "energyPrice")
        def protocol = (EntsoeProtocol) agentService.getProtocolInstance(agent.id)

        and: "the attribute is linked by protocol"
        conditions.eventually {
            assert protocol.getLinkedAttributes().containsKey(attributeRef)
        }

        and: "an update is triggered at the fixed time"
        stopPseudoClock()
        setPseudoClock("2026-02-10T10:15:00.000Z")
        def fixedNow = Instant.parse("2026-02-10T10:15:00.000Z")
        requestLog.clear()
        protocol.updateAllLinkedAttributes()

        then: "request periodStart/periodEnd use the fixed clock time"
        conditions.eventually {
            assert requestLog.any {
                it.zone == "10YBE----------2" &&
                        it.periodStart == periodFormatter.format(fixedNow) &&
                        it.periodEnd == periodFormatter.format(fixedNow.plus(1, java.time.temporal.ChronoUnit.DAYS))
            }
        }

        when: "clock advances by one day and another update is triggered"
        advancePseudoClock(1, TimeUnit.DAYS, container)
        def nextNow = fixedNow.plus(1, java.time.temporal.ChronoUnit.DAYS)
        requestLog.clear()
        protocol.updateAllLinkedAttributes()

        then: "request periodStart/periodEnd are shifted by one day"
        conditions.eventually {
            assert requestLog.any {
                it.zone == "10YBE----------2" &&
                        it.periodStart == periodFormatter.format(nextNow) &&
                        it.periodEnd == periodFormatter.format(nextNow.plus(1, java.time.temporal.ChronoUnit.DAYS))
            }
        }

        cleanup: "remove created assets and mock client"
        if (asset?.id) {
            assetStorageService.delete([asset.id])
        }
        if (agent?.id) {
            assetStorageService.delete([agent.id])
        }
        if (EntsoeProtocol.client.get() != null) {
            EntsoeProtocol.client.set(null)
        }
    }
}
