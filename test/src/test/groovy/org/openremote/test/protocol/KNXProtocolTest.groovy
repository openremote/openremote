/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.test.protocol

import org.apache.commons.lang3.SystemUtils
import org.openremote.agent.protocol.knx.KNXAgent
import org.openremote.agent.protocol.knx.KNXAgentLink
import org.openremote.agent.protocol.knx.KNXProtocol
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.Constants
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions
import tuwien.auto.calimero.server.Launcher
import tuwien.auto.calimero.server.knxnetip.DefaultServiceContainer

import static org.openremote.model.value.MetaItemType.*
import static org.openremote.model.value.ValueType.*

/**
 * This tests the KNX protocol and protocol implementation.
 */
class KNXProtocolTest extends Specification implements ManagerContainerTrait {

    def "Check KNX protocol"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the KNX emulation server is started"
        def configFile = SystemUtils.IS_OS_MAC ? "/org/openremote/test/protocol/knx/knx-server-config-mac.xml" : "/org/openremote/test/protocol/knx/knx-server-config.xml"
        def configUri = getClass().getResource(configFile).toURI().toString()
        def knxEmulationServer = new Launcher(configUri)
        def sc = knxEmulationServer.xml.svcContainers.remove(0)
        def sc2 = new DefaultServiceContainer(
            sc.getName(),
            NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress()).name,
            sc.getControlEndpoint(),
            sc.getMediumSettings(),
            sc.reuseControlEndpoint(),
            sc.isNetworkMonitoringAllowed())
        knxEmulationServer.xml.svcContainers.add(sc2)
        def netIf = knxEmulationServer.xml.subnetNetIf.remove(sc)
        knxEmulationServer.xml.subnetNetIf.put(sc2, netIf)
        def linkClasses = knxEmulationServer.xml.subnetLinkClasses.remove(sc)
        knxEmulationServer.xml.subnetLinkClasses.put(sc2, linkClasses)
        def groupFilters = knxEmulationServer.xml.groupAddressFilters.remove(sc)
        knxEmulationServer.xml.groupAddressFilters.put(sc2, groupFilters)
        def addAddresses = knxEmulationServer.xml.additionalAddresses.remove(sc)
        knxEmulationServer.xml.additionalAddresses.put(sc2, addAddresses)
        def knxServerThread = new Thread(knxEmulationServer)
        knxServerThread.start()
        KNXTestingNetworkLink knxTestingNetwork

        expect: "the testing network to become available"
        conditions.eventually {
            knxTestingNetwork = KNXTestingNetworkLink.getInstance()
            assert knxTestingNetwork != null
        }

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        

        when: "KNX agents are created"
        def knxAgent1 = new KNXAgent("KNX Agent 1")
            .setHost("127.0.0.1")
            .setBindHost("127.0.0.1")
            .setRealm(Constants.MASTER_REALM)
        def knxAgent2 = new KNXAgent("KNX Agent 2")
            .setRealm(Constants.MASTER_REALM)

        knxAgent1 = assetStorageService.merge(knxAgent1)
        knxAgent2 = assetStorageService.merge(knxAgent2)

        then: "a protocol instance should be created for the valid agent but not the invalid one"
        conditions.eventually {
            assert agentService.getAgent(knxAgent1.id) != null
            assert agentService.getAgent(knxAgent2.id) != null
            assert agentService.getAgent(knxAgent1.id).getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
            assert agentService.getAgent(knxAgent2.id).getAgentStatus().orElse(null) == ConnectionStatus.ERROR
            assert agentService.getProtocolInstance(knxAgent1.id) != null
            assert agentService.getProtocolInstance(knxAgent2.id) == null
        }

        when: "a thing asset is created that links it's attributes to the valid knx agent"
        def knxThing = new ThingAsset("Living Room Asset")
            .setParent(knxAgent1)
            .addOrReplaceAttributes(
                new Attribute<>("light1ToggleOnOff", BOOLEAN)
                    .addOrReplaceMeta(
                        new MetaItem<>(LABEL, "Light 1 Toggle On/Off"),
                        new MetaItem<>(AGENT_LINK, new KNXAgentLink(knxAgent1.id, "1.001", "1/0/17", "0/4/14"))
                    )
        )
        knxThing = assetStorageService.merge(knxThing)

        then: "the living room thing to be fully deployed"
        conditions.eventually {
            assert ((KNXProtocol) agentService.getProtocolInstance(knxAgent1.id)).attributeStatusMap.get(new AttributeRef(knxThing.id, "light1ToggleOnOff")) != null
        }
        
        when: "change light1ToggleOnOff value to 'true'"
        def switchChange = new AttributeEvent(knxThing.getId(), "light1ToggleOnOff", true)
        assetProcessingService.sendAttributeEvent(switchChange)
                
        then: "the correct data should arrive on KNX bus"
        conditions.eventually {
           assert knxTestingNetwork.getLastDataReceived() == "0081"
        }

        when: "change light1ToggleOnOff value to 'false'"
        switchChange = new AttributeEvent(knxThing.getId(), "light1ToggleOnOff", false)
        assetProcessingService.sendAttributeEvent(switchChange)
                
        then: "the correct data should arrive on KNX bus"
        conditions.eventually {
            assert knxTestingNetwork.getLastDataReceived() == "0080"
        }
        
        cleanup: "the server should be stopped"
        if (knxThing != null) {
            assetStorageService.delete([knxThing.id])
        }
        if (knxAgent1 != null) {
            assetStorageService.delete([knxAgent1.id])
        }
        if (knxAgent2 != null) {
            assetStorageService.delete([knxAgent2.id])
        }
        if (knxEmulationServer != null) {
            knxEmulationServer.quit()
        }
        if (knxTestingNetwork != null) {
            knxTestingNetwork.close()
        }
    }
}
