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

import tuwien.auto.calimero.server.knxnetip.DefaultServiceContainer

import static org.openremote.model.asset.AssetMeta.DESCRIPTION
import static org.openremote.model.asset.AssetMeta.LABEL

import org.openremote.agent.protocol.knx.KNXProtocol
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.manager.server.agent.AgentService
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.asset.ServerAsset
import org.openremote.model.Constants
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.asset.AssetMeta
import org.openremote.model.asset.AssetType
import org.openremote.model.asset.agent.ProtocolConfiguration
import org.openremote.model.attribute.*
import org.openremote.model.value.Values
import org.openremote.test.KNXTestingNetworkLink
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions
import tuwien.auto.calimero.server.Launcher

/**
 * This tests the KNX protocol and protocol implementation.
 */
class KNXProtocolTest extends Specification implements ManagerContainerTrait {

    def "Check KNX protocol"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 1, delay: 1)

        and: "the KNX emulation server is started"
        def configUri = getClass().getResource("/org/openremote/test/protocol/knx/knx-server-config.xml").toURI().toString()
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
        def knxTestingNetwork = KNXTestingNetworkLink.getInstance()

        and: "the container is started"
        def serverPort = findEphemeralPort()
        def container = startContainerNoDemoImport(defaultConfig(serverPort), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        

        when: "a KNX agent that uses the KNX protocol is created with a valid protocol configuration"
        def knxAgent = new ServerAsset()
        knxAgent.setName("KNX Agent")
        knxAgent.setType(AssetType.AGENT)
        knxAgent.setAttributes(
            ProtocolConfiguration.initProtocolConfiguration(new AssetAttribute("knxConfig"), KNXProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(KNXProtocol.META_KNX_GATEWAY_IP, Values.create("localhost")),
                    new MetaItem(KNXProtocol.META_KNX_LOCAL_IP, Values.create("localhost"))
                ),
            ProtocolConfiguration.initProtocolConfiguration(new AssetAttribute("knxConfigError1"), KNXProtocol.PROTOCOL_NAME),
            ProtocolConfiguration.initProtocolConfiguration(new AssetAttribute("knxConfigError2"), KNXProtocol.PROTOCOL_NAME)
                .addMeta(
                    new MetaItem(KNXProtocol.META_KNX_IP_CONNECTION_TYPE, Values.create("dummy"))
                )
        )
        knxAgent.setRealmId(Constants.MASTER_REALM)
        knxAgent = assetStorageService.merge(knxAgent)

        then: "the protocol configurations should be linked and their deployment status should be available in the agent service"
        conditions.eventually {
            assert agentService.getProtocolDeploymentStatus(knxAgent.getAttribute("knxConfig").get().getReferenceOrThrow()) == ConnectionStatus.CONNECTED
        }
        conditions.eventually {
            assert agentService.getProtocolDeploymentStatus(knxAgent.getAttribute("knxConfigError1").get().getReferenceOrThrow()) == ConnectionStatus.ERROR
        }
        conditions.eventually {
            assert agentService.getProtocolDeploymentStatus(knxAgent.getAttribute("knxConfigError2").get().getReferenceOrThrow()) == ConnectionStatus.ERROR
        }


        when: "a thing asset is created that links it's attributes to the knx protocol configuration"
        def knxThing = new ServerAsset("Living Room Assset", AssetType.THING, knxAgent)
        knxThing.setAttributes(
                new AssetAttribute("light1ToggleOnOff", AttributeType.BOOLEAN)
                    .setMeta(
                        new MetaItem(LABEL, Values.create("Light 1 Toggle On/Off")),
                        new MetaItem(DESCRIPTION, Values.create("Light 1 for living room")),
                        new MetaItem(KNXProtocol.META_KNX_ACTION_GA, Values.create("1/0/17")),
                        new MetaItem(KNXProtocol.META_KNX_STATUS_GA, Values.create("0/4/14")),
                        new MetaItem(KNXProtocol.META_KNX_DPT, Values.create("1.001")),
                        new MetaItem(AssetMeta.AGENT_LINK, new AttributeRef(knxAgent.getId(), "knxConfig").toArrayValue())
                    )
        )
        knxThing = assetStorageService.merge(knxThing)

        then: "the living room thing to be fully deployed"
        conditions.eventually {
            ((KNXProtocol) agentService.getProtocol(knxAgent.getAttribute("knxConfig").get())).getAttributeActionMap().get(knxThing.getAttribute("light1ToggleOnOff")) != null
        }
        
        
        when: "change light1ToggleOnOff value to 'true'"
        def switchChange = new AttributeEvent(knxThing.getId(), "light1ToggleOnOff", Values.create(true))
        assetProcessingService.sendAttributeEvent(switchChange)
                
        then: "the correct data should arrive on KNX bus"
        conditions.eventually {
           assert knxTestingNetwork.getLastDataReceived().equals("0081") == true
        }

        when: "change light1ToggleOnOff value to 'false'"
        switchChange = new AttributeEvent(knxThing.getId(), "light1ToggleOnOff", Values.create(false))
        assetProcessingService.sendAttributeEvent(switchChange)
                
        then: "the correct data should arrive on KNX bus"
        conditions.eventually {
            assert knxTestingNetwork.getLastDataReceived().equals("0080") == true
        }
        
        cleanup: "the server should be stopped"
        stopContainer(container)
        knxEmulationServer.quit()

    }
}