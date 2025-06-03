/*
 * Copyright 2025, OpenRemote Inc.
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

import net.solarnetwork.io.modbus.BitsModbusMessage
import net.solarnetwork.io.modbus.ModbusBlockType
import net.solarnetwork.io.modbus.ModbusErrorCode
import net.solarnetwork.io.modbus.ModbusFunctionCode
import net.solarnetwork.io.modbus.ModbusMessage
import net.solarnetwork.io.modbus.RegistersModbusMessage
import net.solarnetwork.io.modbus.netty.msg.BaseModbusMessage
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
import org.openremote.model.attribute.MetaMap
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.atomic.AtomicReference

import static net.solarnetwork.io.modbus.ModbusFunctionCodes.WRITE_COIL
import static net.solarnetwork.io.modbus.ModbusFunctionCodes.WRITE_COILS
import static net.solarnetwork.io.modbus.ModbusFunctionCodes.WRITE_HOLDING_REGISTER
import static net.solarnetwork.io.modbus.ModbusFunctionCodes.WRITE_HOLDING_REGISTERS
import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.readCoilsResponse
import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.readDiscretesResponse
import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.writeCoilResponse
import static net.solarnetwork.io.modbus.netty.msg.BitsModbusMessage.writeCoilsResponse
import static net.solarnetwork.io.modbus.netty.msg.RegistersModbusMessage.*
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.value.MetaItemType.AGENT_LINK

class ModbusBasicTest extends Specification implements ManagerContainerTrait {

    @Shared
    int modbusServerPort

    @Shared
    NettyTcpModbusServer server

    @Shared
    AtomicReference<ModbusMessage> latestReadMessage = new AtomicReference(null)

    @Shared
    AtomicReference<ModbusMessage> latestWriteMessage = new AtomicReference(null)
//    @Shared
//    Map<, Number> messageMap = [:]

    def setupSpec() {
        modbusServerPort = findEphemeralPort()
        server = new NettyTcpModbusServer(modbusServerPort)
        server.setMessageHandler((msg, sender) -> {
            ModbusMessage modbusMessage = null
            switch (msg.getFunction().blockType()) {
                case ModbusBlockType.Coil -> {
                    BitsModbusMessage tcpRequest = msg.unwrap(BitsModbusMessage.class)
                    if (msg.getFunction().isReadFunction()) {
                        modbusMessage = readCoilsResponse(tcpRequest.getUnitId(), tcpRequest.getAddress(), tcpRequest.getCount(), BigInteger.valueOf(0xFFFF))
                    } else {
                        if (tcpRequest.getFunction().getCode() == WRITE_COILS) {
                            modbusMessage = writeCoilsResponse(tcpRequest.getUnitId(), tcpRequest.getAddress(), tcpRequest.getCount())
                        } else if (tcpRequest.getFunction().getCode() == WRITE_COIL) {
                            modbusMessage = writeCoilResponse(tcpRequest.getUnitId(), tcpRequest.getAddress(), true)
                        }
                    }
                }
                case ModbusBlockType.Discrete -> {
                    BitsModbusMessage tcpRequest = msg.unwrap(BitsModbusMessage.class)
                    modbusMessage = readDiscretesResponse(tcpRequest.getUnitId(), tcpRequest.getAddress(), tcpRequest.getCount(), BigInteger.valueOf(0xFFFF))
                }
                case ModbusBlockType.Holding -> {
                    RegistersModbusMessage registerRequest = msg.unwrap(RegistersModbusMessage.class)
                    if (msg.getFunction().isReadFunction()) {
                        modbusMessage = readHoldingsResponse(registerRequest.getUnitId(), registerRequest.getAddress(), new short[]{1, 2, 3, 4, 5, 6, 7, 8, 12.3f, 10})
                    } else {
                        if (registerRequest.getFunction().getCode() == WRITE_HOLDING_REGISTERS) {
                            modbusMessage = writeHoldingsResponse(registerRequest.getUnitId(), registerRequest.getAddress(), registerRequest.getCount())
                        } else if (registerRequest.getFunction().getCode() == WRITE_HOLDING_REGISTER) {
                            modbusMessage = writeHoldingResponse(registerRequest.getUnitId(), registerRequest.getAddress(), 21)
                        }
                    }
                }
                case ModbusBlockType.Input -> {
                    RegistersModbusMessage registerRequest = msg.unwrap(RegistersModbusMessage.class)
                    modbusMessage = readInputsResponse(registerRequest.getUnitId(), registerRequest.getAddress(), new short[]{11, 12, 13, 14, 15, 16, 17, 18, 19, 20})
                }
                case ModbusBlockType.Diagnostic -> {
                    modbusMessage = new BaseModbusMessage(msg.getUnitId(), msg.getFunction(), ModbusErrorCode.Acknowledge)
                }
            }

            if (msg.getFunction().isReadFunction()) {
                latestReadMessage.set(msg)
            } else {
                latestWriteMessage.set(msg)
            }
            sender.accept(modbusMessage)

    })
        server.start()
    }

    def cleanupSpec() {
        server.stop()
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

        agent.setHost("127.0.0.1")
        agent.setPort(modbusServerPort)
        agent.setUnitId(1)

        agent = assetStorageService.merge(agent)

        then: "the protocol instance for the agent should be created"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert ((ModbusTcpProtocol)agentService.getProtocolInstance(agent.id)) != null
        }

        and: "the connection status should be CONNECTED"
        conditions.eventually {
            agent = assetStorageService.find(agent.getId())
            agent.getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
        }

        when: "A ShipAsset is created with an agent link"
        ShipAsset ship = new ShipAsset("testAsset")
        ship.setRealm(MASTER_REALM)
        ship.addOrReplaceAttributes(new Attribute<Object>(ShipAsset.SPEED).addOrReplaceMeta(new MetaItem<>(
                AGENT_LINK,
                new ModbusAgentLink(
                        id: agent.getId(),
                        pollingMillis: 1000,
                        readMemoryArea: ModbusAgentLink.ReadMemoryArea.HOLDING,
                        readValueType: ModbusAgentLink.ModbusDataType.UINT,
                        readAddress: 2,
                        writeMemoryArea: ModbusAgentLink.WriteMemoryArea.HOLDING,
                        writeAddress: 3
                )
        )))
        ship = assetStorageService.merge(ship)
        ModbusAgentLink agentLink

        then: "a client should be created and the pollingMap is populated"
        conditions.eventually {

            assert agentService.getProtocolInstance(agent.id) != null
            assert ((ModbusTcpProtocol)agentService.getProtocolInstance(agent.id)) != null
            assert ((ModbusTcpProtocol)agentService.getProtocolInstance(agent.id)).pollingMap.size() == 1

            ship = assetStorageService.find(ship.getId(), true)
            agentLink = ship.getAttribute(ShipAsset.SPEED).get().getMetaItem(AGENT_LINK).get().getValue(ModbusAgentLink.class).get()
            assert ship.getAttribute(ShipAsset.SPEED).flatMap { it.getValue() }.orElse(null) == 1

            assert (latestReadMessage.get() as ModbusMessage).getUnitId() === 1
            assert (latestReadMessage.get() as ModbusMessage).getFunction() === ModbusFunctionCode.ReadHoldingRegisters
            assert (latestReadMessage.get() as ModbusMessage).unwrap(RegistersModbusMessage.class).getAddress() == agentLink.getReadAddress().get()-1
            assert (latestReadMessage.get() as ModbusMessage).unwrap(RegistersModbusMessage.class).getCount() == 1
        }

        when: "the attribute is updated"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(ship.getId(), ShipAsset.SPEED, 123D))
        ship.addOrReplaceAttributes(new Attribute<?>(ShipAsset.SPEED, 10))

        then: "the value is sent to the Modbus server"

        conditions.eventually {
            def msg = (latestWriteMessage.get() as ModbusMessage).unwrap(RegistersModbusMessage)
            assert msg != null
            assert msg.getCount() == 1
            assert msg.getAddress() == agentLink.getWriteAddress().get()-1
            assert msg.dataDecodeUnsigned() == [123] as int[]

            ship = assetStorageService.find(ship.getId(), true)
            assert ship.getAttribute(ShipAsset.SPEED).get().getValue().orElse(0) == 1.0
        }

        when: "I add a new coil attribute and remove the Agent Link from the speed attribute"
        ship.addOrReplaceAttributes(new Attribute<Integer>("coil1", ValueType.BOOLEAN).addOrReplaceMeta(new MetaItem<>(
                AGENT_LINK,
                new ModbusAgentLink(
                        id: agent.getId(),
                        pollingMillis: 1000,
                        readMemoryArea: ModbusAgentLink.ReadMemoryArea.COIL,
                        readValueType: ModbusAgentLink.ModbusDataType.BOOL,
                        readAddress: 5,
                        writeMemoryArea: ModbusAgentLink.WriteMemoryArea.COIL,
                        writeAddress: 6
                )
        )))
        ship.addOrReplaceAttributes(new Attribute<Object>(ShipAsset.SPEED).setMeta(Map.of() as MetaMap))
        latestReadMessage.set(null)
        latestWriteMessage.set(null)

        and: "I merge it"
        ship = assetStorageService.merge(ship)

        then: "I should receive the correct value in the coil"

        conditions.eventually {
            agentLink = ship.getAttribute("coil1").get().getMetaItem(AGENT_LINK).get().getValue(ModbusAgentLink.class).get()

            assert agentService.getProtocolInstance(agent.id) != null
            assert ((ModbusTcpProtocol)agentService.getProtocolInstance(agent.id)) != null

            def msg = (latestReadMessage.get() as ModbusMessage).unwrap(BitsModbusMessage)
            assert msg != null
            assert msg.getCount() == 1
            assert msg.getAddress() == agentLink.getReadAddress().get()-1

            ship = assetStorageService.find(ship.getId(), true)
            assert ship.getAttribute("coil1").flatMap { it.getValue() }.orElse(null) == true
        }

        when: "I update the value of the coil"

        assetProcessingService.sendAttributeEvent(new AttributeEvent(ship.getId(), "coil1", true))

        then: "the coil is read properly"

        conditions.eventually {
            def msg = (latestWriteMessage.get() as ModbusMessage).unwrap(BitsModbusMessage)
            assert msg != null
            assert msg.getCount() == 1
            assert msg.getAddress() == 5
            assert msg.getBits() == BigInteger.valueOf(0x0001)

            ship = assetStorageService.find(ship.getId(), true)
            assert ship.getAttribute("coil1").flatMap { it.getValue() }.orElse(null) == true
        }
    }
}
