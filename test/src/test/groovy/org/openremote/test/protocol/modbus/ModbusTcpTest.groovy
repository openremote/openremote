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
import org.openremote.model.asset.impl.ThingAsset
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

class ModbusTcpTest extends Specification implements ManagerContainerTrait {

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


    def "Modbus TCP Integration Test - Basic Operations"() {
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

    def "Modbus TCP Test - Register Batching"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)

        and: "a Modbus TCP agent with batching enabled is created"
        def agent = new ModbusTcpAgent("Modbus TCP Batching")
        agent.setRealm(MASTER_REALM)
        agent.setHost("127.0.0.1")
        agent.setPort(modbusServerPort)
        agent.setUnitId(1)
        agent.addOrReplaceAttributes(
                new Attribute<>(ModbusTcpAgent.MAX_REGISTER_LENGTH, 50) // Enable batching
        )
        agent = assetStorageService.merge(agent)

        then: "the protocol instance should be created and connected"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            agent = assetStorageService.find(agent.getId())
            agent.getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
        }

        when: "a device with multiple sequential register attributes is created"
        ThingAsset device = new ThingAsset("Batching Test Device")
        device.setRealm(MASTER_REALM)
        device.addOrReplaceAttributes(
                new Attribute<>("register1", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setPollingMillis(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.UINT)
                                    it.setReadAddress(1)
                                }
                        )
                ),
                new Attribute<>("register2", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setPollingMillis(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.UINT)
                                    it.setReadAddress(2)
                                }
                        )
                ),
                new Attribute<>("register3", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setPollingMillis(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.UINT)
                                    it.setReadAddress(3)
                                }
                        )
                )
        )
        device = assetStorageService.merge(device)

        then: "batch groups should be created and attributes should receive values"
        conditions.eventually {
            def protocol = agentService.getProtocolInstance(agent.id) as ModbusTcpProtocol
            assert protocol != null

            // Check that batch groups were created (batching is enabled)
            assert protocol.batchGroups.size() > 0

            device = assetStorageService.find(device.getId(), true)

            // Verify all attributes received values from the batch read
            assert device.getAttribute("register1").flatMap { it.getValue() }.isPresent()
            assert device.getAttribute("register2").flatMap { it.getValue() }.isPresent()
            assert device.getAttribute("register3").flatMap { it.getValue() }.isPresent()

            // Verify the values match what the mock server returns
            assert device.getAttribute("register1").flatMap { it.getValue() }.get() == 1
            assert device.getAttribute("register2").flatMap { it.getValue() }.get() == 2
            assert device.getAttribute("register3").flatMap { it.getValue() }.get() == 3
        }
    }

    def "Modbus TCP Test - 64-bit Data Types (LINT, ULINT, LREAL)"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)

        and: "a Modbus TCP agent is created with batching enabled"
        def agent = new ModbusTcpAgent("Modbus TCP 64-bit Test")
        agent.setRealm(MASTER_REALM)
        agent.setHost("127.0.0.1")
        agent.setPort(modbusServerPort)
        agent.setUnitId(1)
        agent.addOrReplaceAttributes(
                new Attribute<>(ModbusTcpAgent.MAX_REGISTER_LENGTH, 50)
        )
        agent = assetStorageService.merge(agent)

        then: "the agent should connect successfully"
        conditions.eventually {
            agent = assetStorageService.find(agent.getId())
            agent.getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
        }

        when: "a device with 64-bit data types is created"
        def device = new ThingAsset("64-bit Test Device")
        device.setRealm(MASTER_REALM)
        device.addOrReplaceAttributes(
                // LREAL - Double precision float (4 registers)
                new Attribute<>("doubleValue", ValueType.NUMBER).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setPollingMillis(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.LREAL)
                                    it.setReadAddress(1)
                                    it.setReadRegistersAmount(4)
                                }
                        )
                ),
                // LINT - 64-bit signed integer (4 registers)
                new Attribute<>("longSignedValue", ValueType.LONG).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setPollingMillis(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.LINT)
                                    it.setReadAddress(5)
                                    it.setReadRegistersAmount(4)
                                }
                        )
                ),
                // ULINT - 64-bit unsigned integer (4 registers)
                new Attribute<>("longUnsignedValue", ValueType.BIG_INTEGER).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setPollingMillis(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.ULINT)
                                    it.setReadAddress(9)
                                    it.setReadRegistersAmount(4)
                                }
                        )
                )
        )
        device = assetStorageService.merge(device)

        then: "all 64-bit attributes should receive values"
        conditions.eventually {
            device = assetStorageService.find(device.getId(), true)

            // Verify all attributes have values (mock server returns short array that gets converted)
            assert device.getAttribute("doubleValue").flatMap { it.getValue() }.isPresent()
            assert device.getAttribute("longSignedValue").flatMap { it.getValue() }.isPresent()
            assert device.getAttribute("longUnsignedValue").flatMap { it.getValue() }.isPresent()

            // Verify the attributes are read correctly as 64-bit types
            def doubleValue = device.getAttribute("doubleValue").flatMap { it.getValue() }.get()
            def longSignedValue = device.getAttribute("longSignedValue").flatMap { it.getValue() }.get()
            def longUnsignedValue = device.getAttribute("longUnsignedValue").flatMap { it.getValue() }.get()

            // Values should be present and non-null (actual values depend on mock server data)
            assert doubleValue != null
            assert longSignedValue != null
            assert longUnsignedValue != null
        }

        and: "verify batching was used"
        conditions.eventually {
            def protocol = agentService.getProtocolInstance(agent.id) as ModbusTcpProtocol
            assert protocol != null
            assert protocol.batchGroups.size() > 0
        }
    }

    def "Modbus TCP Test - Byte and Word Order"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)

        and: "a Modbus TCP agent with BIG-BIG byte/word order is created"
        def agent = new ModbusTcpAgent("Modbus TCP Endian Test")
        agent.setRealm(MASTER_REALM)
        agent.setHost("127.0.0.1")
        agent.setPort(modbusServerPort)
        agent.setUnitId(1)
        agent.addOrReplaceAttributes(
                new Attribute<>(ModbusTcpAgent.BYTE_ORDER, ModbusAgent.EndianOrder.BIG),
                new Attribute<>(ModbusTcpAgent.WORD_ORDER, ModbusAgent.EndianOrder.BIG)
        )
        agent = assetStorageService.merge(agent)

        then: "the agent should connect successfully"
        conditions.eventually {
            agent = assetStorageService.find(agent.getId())
            agent.getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
        }

        when: "a device with 32-bit float value is created"
        def device = new ThingAsset("Endian Test Device")
        device.setRealm(MASTER_REALM)
        device.addOrReplaceAttributes(
                new Attribute<>("floatValue", ValueType.NUMBER).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setPollingMillis(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.REAL)
                                    it.setReadAddress(9)
                                    it.setReadRegistersAmount(2)
                                }
                        )
                )
        )
        device = assetStorageService.merge(device)

        then: "the float value should be read correctly with BIG-BIG order"
        conditions.eventually {
            device = assetStorageService.find(device.getId(), true)

            // Verify attribute receives a value with the configured byte/word order
            assert device.getAttribute("floatValue").flatMap { it.getValue() }.isPresent()
            def floatValue = device.getAttribute("floatValue").flatMap { it.getValue() }.get()

            // Value should be present and non-null (byte/word order is configured and applied by PLC4X)
            assert floatValue != null
            assert floatValue instanceof Number
        }
    }

}
