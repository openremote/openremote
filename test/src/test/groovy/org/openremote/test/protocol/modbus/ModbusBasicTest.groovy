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
package org.openremote.test.protocol.modbus

import net.solarnetwork.*
import net.solarnetwork.io.modbus.tcp.TcpModbusMessage;
import net.solarnetwork.io.modbus.tcp.netty.NettyTcpModbusServer
import org.openremote.agent.protocol.modbus.ModbusAgentLink
import org.openremote.agent.protocol.modbus.ModbusTcpAgent
import org.openremote.agent.protocol.modbus.ModbusTcpProtocol
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.asset.agent.Agent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ShipAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.value.MetaItemType.AGENT_LINK

class ModbusBasicTest extends Specification implements ManagerContainerTrait {

    @Shared
    int modbusServerPort;

    @Shared
    NettyTcpModbusServer server;

    def setupSpec() {
        modbusServerPort = findEphemeralPort();
        server = new NettyTcpModbusServer(modbusServerPort)
        server.setMessageHandler { msg, sender ->
            def tcpMessage = msg.unwrap(TcpModbusMessage.class)
            def tcpParsedMessage = tcpMessage.validate();
            print(tcpParsedMessage);
        }

        server.start()
    }

    def cleanupSpec() {
        server.stop();
    }


    def "Modbus Integration Test"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        and: "a mock Modbus agent is created"
        def agent = new ModbusTcpAgent("Modbus")
        agent.setRealm(MASTER_REALM)

        agent.setHost("localhost")
        agent.setPort(modbusServerPort)

        agent = assetStorageService.merge(agent)

        then: "the protocol instance for the agent should be created"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((ModbusTcpProtocol)agentService.getProtocolInstance(agent.id)) != null
        }

        and: "the connection status should be CONNECTED"
        conditions.eventually {
            agent = assetStorageService.find(agent.getId());
            agent.getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
        }

        when: "A ShipAsset is created"
        ShipAsset ship = new ShipAsset("testAsset");
        ship.setRealm(MASTER_REALM)
        ship.addOrReplaceAttributes(new Attribute<Object>(ShipAsset.SPEED).addOrReplaceMeta(new MetaItem<>(
                AGENT_LINK,
                new ModbusAgentLink(
                        id: agent.getId(),
                        unitId: 1,
                        refresh: 1000,
                        readType: ModbusAgentLink.ReadType.HOLDING,
                        readValueType: ModbusAgentLink.ModbusDataType.UINT,
                        readAddress: 2,
                        writeType: ModbusAgentLink.WriteType.HOLDING,
                        writeAddress: 3,
                        writeValueType: ModbusAgentLink.ModbusDataType.UINT
                )
        )));

        ship.addOrReplaceAttributes(new Attribute<Object>("coil1", ValueType.BOOLEAN).addOrReplaceMeta(new MetaItem<>(
                AGENT_LINK,
                new ModbusAgentLink(
                        id: agent.getId(),
                        unitId: 1,
                        refresh: 1000,
                        readType: ModbusAgentLink.ReadType.COIL,
                        readValueType: ModbusAgentLink.ModbusDataType.BOOL,
                        readAddress: 2,
                        writeType: ModbusAgentLink.WriteType.COIL,
                        writeAddress: 5,
                        writeValueType: ModbusAgentLink.ModbusDataType.BOOL
                )
        )));

        ship = assetStorageService.merge(ship);

        then: "a client should be created and the pollingMap is populated"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((ModbusTcpProtocol)agentService.getProtocolInstance(agent.id)) != null

            ship = assetStorageService.find(ship.getId(), true)
            assert ship.getAttribute(ShipAsset.SPEED).flatMap { it.getValue() }.orElse(null) <= 10
        }

        and: "I should receive the correct value in the coil"

        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((ModbusTcpProtocol)agentService.getProtocolInstance(agent.id)) != null

            ship = assetStorageService.find(ship.getId(), true)
            assert ship.getAttribute("coil1").flatMap { it.getValue() }.orElse(null) == false
        }

        when: "the attribute is updated"

        assetProcessingService.sendAttributeEvent(new AttributeEvent(ship.getId(), ShipAsset.SPEED, 123D))

        then: "the value is sent to the Modbus server"

        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((ModbusTcpProtocol)agentService.getProtocolInstance(agent.id)) != null

            ship = assetStorageService.find(ship.getId(), true)
            assert ship.getAttribute(ShipAsset.SPEED).flatMap { it.getValue() }.orElse(null) < 10
        }

        and: "the coil is read properly"

        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((ModbusTcpProtocol)agentService.getProtocolInstance(agent.id)) != null

            ship = assetStorageService.find(ship.getId(), true)
            assert ship.getAttribute("coil1").flatMap { it.getValue() }.orElse(null) == false
        }

        assetProcessingService.sendAttributeEvent(new AttributeEvent(ship.getId(), "coil1", true))

//        and: "I send the correct boolean to the coil"


    }
}
