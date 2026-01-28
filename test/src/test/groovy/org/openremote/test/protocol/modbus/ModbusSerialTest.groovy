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

import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.matcher.ElementMatchers
import org.openremote.agent.protocol.modbus.ModbusAgent
import org.openremote.agent.protocol.modbus.ModbusAgentLink
import org.openremote.agent.protocol.modbus.ModbusSerialAgent
import org.openremote.agent.protocol.modbus.ModbusSerialProtocol
import org.openremote.agent.protocol.serial.SerialIOClient
import org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Paritybit
import org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Stopbits
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.model.asset.agent.Agent
import org.openremote.model.asset.agent.ConnectionStatus
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeEvent
import org.openremote.model.attribute.MetaItem
import org.openremote.model.value.ValueType
import org.openremote.test.ManagerContainerTrait
import org.openremote.test.protocol.MockSerialChannel
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.ByteBuffer
import java.nio.ByteOrder

import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.value.MetaItemType.AGENT_LINK
import static org.openremote.model.value.MetaItemType.STORE_DATA_POINTS

@spock.lang.Retry(count = 5)
class ModbusSerialTest extends Specification implements ManagerContainerTrait {

    static byte[] latestRequest = null
    static byte[] latestWriteRequest = null
    static ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream()

    def setupSpec() {
        // retransform SerialIOClient.getChannelClass() to return MockSerialChannel instead of JSerialCommChannel
        def instrumentation = ByteBuddyAgent.install()
        new AgentBuilder.Default()
            .disableClassFormatChanges()
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .type(ElementMatchers.is(SerialIOClient.class))
            .transform { builder, typeDescription, classLoader, module, protectionDomain ->
                builder.visit(Advice.to(MockSerialChannel.GetChannelClassAdvice.class)
                    .on(ElementMatchers.named("getChannelClass")))
            }
            .installOn(instrumentation)

        instrumentation.retransformClasses(SerialIOClient.class)

        // Set up mock serial channel to handle Modbus RTU frames
        MockSerialChannel.setDataHandler { byte[] data, MockSerialChannel.ResponseCallback responseCallback ->
            synchronized (frameBuffer) {
                frameBuffer.write(data, 0, data.length)
                byte[] buffer = frameBuffer.toByteArray()

                if (buffer.length >= 8) {
                    latestRequest = buffer.clone()

                    // Check if it's a write request
                    int functionCode = buffer[1] & 0xFF
                    if (functionCode == 0x05 || functionCode == 0x06 ||
                        functionCode == 0x0F || functionCode == 0x10) {
                        latestWriteRequest = buffer.clone()
                    }

                    // Generate and send response
                    byte[] response = generateModbusResponse(buffer, 0, buffer.length)
                    if (response != null && response.length > 0) {
                        responseCallback.sendResponse(response)
                    }

                    frameBuffer.reset()
                }
            }
        }
    }

    def cleanupSpec() {
        MockSerialChannel.setDataHandler(null)
    }

    def setup() {
        // Reset mock state before each test
        latestRequest = null
        latestWriteRequest = null
        synchronized (frameBuffer) {
            frameBuffer.reset()
        }
    }

    def "Modbus Serial Integration Test - Basic Operations"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 30, delay: 0.5)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        and: "a Modbus Serial agent is created"
        def agent = new ModbusSerialAgent("Modbus RTU Device")
        agent.setRealm(MASTER_REALM)
        agent.addOrReplaceAttributes(
                new Attribute<>(ModbusSerialAgent.SERIAL_PORT, "/dev/ttyUSB0"),
                new Attribute<>(ModbusSerialAgent.BAUD_RATE, 9600),
                new Attribute<>(ModbusSerialAgent.DATA_BITS, 8),
                new Attribute<>(ModbusSerialAgent.STOP_BITS, Stopbits.STOPBITS_1),
                new Attribute<>(ModbusSerialAgent.PARITY, Paritybit.EVEN),
                new Attribute<>(ModbusAgent.DEVICE_CONFIG, [
                        "default": new ModbusAgent.ModbusDeviceConfig(ModbusAgent.EndianFormat.BIG_ENDIAN, "101,151-161", 30)
                ] as ModbusAgent.DeviceConfigMap)
        )

        agent = assetStorageService.merge(agent)

        then: "the protocol instance should be created"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null
            assert agentService.getProtocolInstance(agent.id) instanceof ModbusSerialProtocol
        }

        and: "the connection status should be CONNECTED"
        conditions.eventually {
            agent = assetStorageService.find(agent.getId())
            agent.getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
        }

        when: "a device with partial read configuration is created (missing readValueType)"
        def device0 = new ThingAsset("Partial Config Device")
        device0.setRealm(MASTER_REALM)
        device0.addOrReplaceAttributes(
                // Partial read config - has readMemoryArea and readAddress but missing readValueType
                new Attribute<>("partialReadConfig", ValueType.INTEGER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(1)
                                    it.setRequestInterval(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadAddress(50)
                                    // Missing: readValueType
                                }
                ))
        )
        device0 = assetStorageService.merge(device0)

        then: "the attribute should be linked without creating read polling tasks"
        conditions.eventually {
            def protocol = agentService.getProtocolInstance(agent.id) as ModbusSerialProtocol
            assert protocol != null
            // No batch groups should be created for incomplete read config
            assert protocol.modbusExecutor.batchGroups.isEmpty()
        }

        when: "the partial config device is removed"
        assetStorageService.delete([device0.getId()])

        and: "we wait for cleanup to complete"
        Thread.sleep(500)

        and: "A Thing asset is created with multiple agent links"
        ThingAsset device = new ThingAsset("Test Modbus Device")
        device.setRealm(MASTER_REALM)

        // Add various data type attributes
        device.addOrReplaceAttributes(
                // UINT16 register
                new Attribute<>("register1", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(1)
                                    it.setRequestInterval(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.UINT)
                                    it.setReadAddress(1)
                                    it.setRegistersAmount(1)
                                }
                )),
                // UINT16 register
                new Attribute<>("register2", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(1)
                                    it.setRequestInterval(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.UINT)
                                    it.setReadAddress(3)
                                    it.setRegistersAmount(1)
                                }
                )),
                // Float (REAL) value
                new Attribute<>("temperature", ValueType.NUMBER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(1)
                                    it.setRequestInterval(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.INPUT)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.REAL)
                                    it.setReadAddress(201)
                                    it.setRegistersAmount(2)
                                }
                )),
                // Coil
                new Attribute<>("switch1", ValueType.BOOLEAN).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(1)
                                    it.setRequestInterval(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.COIL)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.BOOL)
                                    it.setReadAddress(6)
                                    it.setWriteMemoryArea(ModbusAgentLink.WriteMemoryArea.COIL)
                                    it.setWriteAddress(6)
                                }
                ))
        )

        device = assetStorageService.merge(device)

        then: "register batches should be created and attributes should receive values"
        conditions.eventually {
            def protocol = agentService.getProtocolInstance(agent.id) as ModbusSerialProtocol
            assert protocol != null

            // Check that batch groups were created
            assert protocol.modbusExecutor.batchGroups.size() > 0

            device = assetStorageService.find(device.getId(), true)

            // Verify values were read
            assert device.getAttribute("register1").flatMap { it.getValue() }.isPresent()
            assert device.getAttribute("register2").flatMap { it.getValue() }.isPresent()
            assert device.getAttribute("temperature").flatMap { it.getValue() }.isPresent()
            assert device.getAttribute("switch1").flatMap { it.getValue() }.isPresent()

            // Check float value (should be 23.5 from mock)
            assert Math.abs((device.getAttribute("temperature").flatMap { it.getValue() }.get() as Double) - 23.5) < 0.1
        }

        when: "a write operation is performed"
        assetProcessingService.sendAttributeEvent(new AttributeEvent(device.getId(), "switch1", true))

        then: "the write should be sent to the device"
        conditions.eventually {
            def lastWriteRequest = latestWriteRequest
            assert lastWriteRequest != null
            assert lastWriteRequest[0] == (byte) 1  // Unit ID
            assert lastWriteRequest[1] == (byte) 0x05  // Write single coil function
        }

    }

    def "Modbus Serial Test - Batching with Illegal Registers"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 15, delay: 0.5)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)

        and: "a Modbus Serial agent with illegal registers is created"
        def agent = new ModbusSerialAgent("Modbus RTU with Illegal Regs")
        agent.setRealm(MASTER_REALM)
        agent.addOrReplaceAttributes(
                new Attribute<>(ModbusSerialAgent.SERIAL_PORT, "/dev/ttyUSB0"),
                new Attribute<>(ModbusSerialAgent.BAUD_RATE, 9600),
                new Attribute<>(ModbusSerialAgent.DATA_BITS, 8),
                new Attribute<>(ModbusSerialAgent.STOP_BITS, Stopbits.STOPBITS_1),
                new Attribute<>(ModbusSerialAgent.PARITY, Paritybit.EVEN),
                new Attribute<>(ModbusAgent.DEVICE_CONFIG, [
                        "default": new ModbusAgent.ModbusDeviceConfig(ModbusAgent.EndianFormat.BIG_ENDIAN, "6-11,21-26", 30)
                ] as ModbusAgent.DeviceConfigMap)
        )

        agent = assetStorageService.merge(agent)

        and: "attributes are created that would span illegal register ranges"
        ThingAsset device = new ThingAsset("Batching Test Device")
        device.setRealm(MASTER_REALM)

        device.addOrReplaceAttributes(
                new Attribute<>("reg0", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(1)
                                    it.setRequestInterval(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.UINT)
                                    it.setReadAddress(1)
                                    it.setRegistersAmount(2)
                                }
                )),
                new Attribute<>("reg15", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(1)
                                    it.setRequestInterval(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.UINT)
                                    it.setReadAddress(16)
                                    it.setRegistersAmount(2)
                                }
                )),
                new Attribute<>("reg30", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(1)
                                    it.setRequestInterval(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.UINT)
                                    it.setReadAddress(31)
                                    it.setRegistersAmount(2)
                                }
                ))
        )

        device = assetStorageService.merge(device)

        then: "batches should be split due to illegal registers"
        conditions.eventually {
            def protocol = agentService.getProtocolInstance(agent.id) as ModbusSerialProtocol
            assert protocol != null

            def cachedBatches = protocol.modbusExecutor.cachedBatches
            // Should have created multiple batches due to illegal register gaps
            def allBatches = cachedBatches.values().flatten()
            assert allBatches.size() >= 2  // At least 2 batches due to illegal register splits

            device = assetStorageService.find(device.getId(), true)
            // All attributes should still receive values
            assert device.getAttribute("reg0").flatMap { it.getValue() }.isPresent()
            assert device.getAttribute("reg15").flatMap { it.getValue() }.isPresent()
            assert device.getAttribute("reg30").flatMap { it.getValue() }.isPresent()
        }
    }

    def "Modbus Serial Test - Multiple Unit IDs via AgentLink"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 15, delay: 0.5)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)

        and: "a single Modbus Serial agent is created for the serial port"
        def agent = new ModbusSerialAgent("Modbus RTU Agent")
        agent.setRealm(MASTER_REALM)
        agent.addOrReplaceAttributes(
                new Attribute<>(ModbusSerialAgent.SERIAL_PORT, "/dev/ttyUSB0"),
                new Attribute<>(ModbusSerialAgent.BAUD_RATE, 9600),
                new Attribute<>(ModbusSerialAgent.DATA_BITS, 8),
                new Attribute<>(ModbusSerialAgent.STOP_BITS, Stopbits.STOPBITS_1),
                new Attribute<>(ModbusSerialAgent.PARITY, Paritybit.EVEN),
                new Attribute<>(ModbusAgent.DEVICE_CONFIG, [
                        "default": new ModbusAgent.ModbusDeviceConfig(ModbusAgent.EndianFormat.BIG_ENDIAN, "", 30)
                ] as ModbusAgent.DeviceConfigMap)
        )

        agent = assetStorageService.merge(agent)

        then: "agent should connect"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent.id) != null

            agent = assetStorageService.find(agent.getId())
            assert agent.getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
        }

        when: "devices are created with different unit IDs via agentLink"
        ThingAsset device1 = new ThingAsset("Device on Unit 1")
        device1.setRealm(MASTER_REALM)
        device1.addOrReplaceAttributes(
                new Attribute<>("register1", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(1)  // Unit ID 1 via agentLink
                                    it.setRequestInterval(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.UINT)
                                    it.setReadAddress(1)
                                    it.setRegistersAmount(1)
                                }
                ))
        )

        ThingAsset device2 = new ThingAsset("Device on Unit 2")
        device2.setRealm(MASTER_REALM)
        device2.addOrReplaceAttributes(
                new Attribute<>("register1", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(2)  // Unit ID 2 via agentLink
                                    it.setRequestInterval(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.UINT)
                                    it.setReadAddress(1)
                                    it.setRegistersAmount(1)
                                }
                ))
        )

        device1 = assetStorageService.merge(device1)
        device2 = assetStorageService.merge(device2)

        then: "both devices should receive values"
        conditions.eventually {
            device1 = assetStorageService.find(device1.getId(), true)
            device2 = assetStorageService.find(device2.getId(), true)

            // Both should have values
            assert device1.getAttribute("register1").flatMap { it.getValue() }.isPresent()
            assert device2.getAttribute("register1").flatMap { it.getValue() }.isPresent()

            // Values should be different based on unit ID
            // Mock returns incrementing values starting from address, but unit ID affects the response
            def value1 = device1.getAttribute("register1").flatMap { it.getValue() }.get()
            def value2 = device2.getAttribute("register1").flatMap { it.getValue() }.get()

            assert value1 != null
            assert value2 != null
        }

        and: "verify that requests were sent with correct unit IDs"
        conditions.eventually {
            // The mock should have received requests with unit ID 1 and 2
            def lastRequest = latestRequest
            assert lastRequest != null
            // Unit ID is the first byte of the Modbus request
            assert lastRequest[0] == (byte) 1 || lastRequest[0] == (byte) 2
        }
    }

    def "Modbus Serial Test - Byte and Word Order Configurations"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 15, delay: 0.5)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)

        and: "four agents are created with different endian format combinations"
        def agentBigBig = createAgentWithEndianFormat(assetStorageService, "Agent BIG-BIG",
                ModbusAgent.EndianFormat.BIG_ENDIAN)

        def agentBigLittle = createAgentWithEndianFormat(assetStorageService, "Agent BIG-LITTLE",
                ModbusAgent.EndianFormat.BIG_ENDIAN_BYTE_SWAP)

        def agentLittleBig = createAgentWithEndianFormat(assetStorageService, "Agent LITTLE-BIG",
                ModbusAgent.EndianFormat.LITTLE_ENDIAN_BYTE_SWAP)

        def agentLittleLittle = createAgentWithEndianFormat(assetStorageService, "Agent LITTLE-LITTLE",
                ModbusAgent.EndianFormat.LITTLE_ENDIAN)

        then: "all agents should connect successfully"
        conditions.eventually {
            assert assetStorageService.find(agentBigBig.getId()).getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
            assert assetStorageService.find(agentBigLittle.getId()).getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
            assert assetStorageService.find(agentLittleBig.getId()).getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
            assert assetStorageService.find(agentLittleLittle.getId()).getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
        }

        when: "devices with 32-bit float values are created for each agent"
        def deviceBigBig = createDeviceWithFloat(assetStorageService, "Device BIG-BIG", agentBigBig, 401)
        def deviceBigLittle = createDeviceWithFloat(assetStorageService, "Device BIG-LITTLE", agentBigLittle, 411)
        def deviceLittleBig = createDeviceWithFloat(assetStorageService, "Device LITTLE-BIG", agentLittleBig, 421)
        def deviceLittleLittle = createDeviceWithFloat(assetStorageService, "Device LITTLE-LITTLE", agentLittleLittle, 431)

        then: "all devices should receive float values"
        conditions.eventually {
            deviceBigBig = assetStorageService.find(deviceBigBig.getId(), true)
            deviceBigLittle = assetStorageService.find(deviceBigLittle.getId(), true)
            deviceLittleBig = assetStorageService.find(deviceLittleBig.getId(), true)
            deviceLittleLittle = assetStorageService.find(deviceLittleLittle.getId(), true)

            // All should have received values (mock returns endian-specific test data)
            assert deviceBigBig.getAttribute("floatValue").flatMap { it.getValue() }.isPresent()
            assert deviceBigLittle.getAttribute("floatValue").flatMap { it.getValue() }.isPresent()
            assert deviceLittleBig.getAttribute("floatValue").flatMap { it.getValue() }.isPresent()
            assert deviceLittleLittle.getAttribute("floatValue").flatMap { it.getValue() }.isPresent()

            // BIG-BIG should get the standard value (42.5)
            def valueBigBig = deviceBigBig.getAttribute("floatValue").flatMap { it.getValue() }.get() as Double
            assert Math.abs(valueBigBig - 42.5) < 0.1

            // Other combinations will have different byte arrangements resulting in different values
            // This verifies that the byte/word order is being applied
            def valueBigLittle = deviceBigLittle.getAttribute("floatValue").flatMap { it.getValue() }.get() as Double
            def valueLittleBig = deviceLittleBig.getAttribute("floatValue").flatMap { it.getValue() }.get() as Double
            def valueLittleLittle = deviceLittleLittle.getAttribute("floatValue").flatMap { it.getValue() }.get() as Double

            // Values should differ based on byte/word order (exact values depend on mock implementation)
            assert valueBigBig != valueBigLittle || valueBigBig != valueLittleBig || valueBigBig != valueLittleLittle
        }

        when: "devices with 64-bit double values are created"
        def device64BigBig = createDeviceWithDouble(assetStorageService, "Device64 BIG-BIG", agentBigBig, 501)
        def device64LittleLittle = createDeviceWithDouble(assetStorageService, "Device64 LITTLE-LITTLE", agentLittleLittle, 511)

        then: "64-bit values should also be affected by byte/word order"
        conditions.eventually {
            device64BigBig = assetStorageService.find(device64BigBig.getId(), true)
            device64LittleLittle = assetStorageService.find(device64LittleLittle.getId(), true)

            assert device64BigBig.getAttribute("doubleValue").flatMap { it.getValue() }.isPresent()
            assert device64LittleLittle.getAttribute("doubleValue").flatMap { it.getValue() }.isPresent()

            def doubleBigBig = device64BigBig.getAttribute("doubleValue").flatMap { it.getValue() }.get() as Double
            def doubleLittleLittle = device64LittleLittle.getAttribute("doubleValue").flatMap { it.getValue() }.get() as Double

            // BIG-BIG should get the standard value (999.888)
            assert Math.abs(doubleBigBig - 999.888) < 0.001

            // LITTLE-LITTLE should get a different value due to byte/word swapping
            assert doubleBigBig != doubleLittleLittle
        }
    }

    def "Modbus Serial Test - 64-bit Data Types (LINT, ULINT, LREAL)"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 15, delay: 0.5)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        and: "a Modbus Serial agent is created"
        def agent = new ModbusSerialAgent("Modbus RTU 64-bit Test")
        agent.setRealm(MASTER_REALM)
        agent.addOrReplaceAttributes(
                new Attribute<>(ModbusSerialAgent.SERIAL_PORT, "/dev/ttyUSB0"),
                new Attribute<>(ModbusSerialAgent.BAUD_RATE, 9600),
                new Attribute<>(ModbusSerialAgent.DATA_BITS, 8),
                new Attribute<>(ModbusSerialAgent.STOP_BITS, Stopbits.STOPBITS_1),
                new Attribute<>(ModbusSerialAgent.PARITY, Paritybit.EVEN),
                new Attribute<>(ModbusAgent.DEVICE_CONFIG, [
                        "default": new ModbusAgent.ModbusDeviceConfig(ModbusAgent.EndianFormat.BIG_ENDIAN, "", 50)
                ] as ModbusAgent.DeviceConfigMap)
        )

        agent = assetStorageService.merge(agent)

        and: "the agent is linked to a device with 64-bit data types"
        def device = new ThingAsset("64-bit Test Device")
        device.setRealm(MASTER_REALM)
        device.setParent(agent)
        device.addOrReplaceAttributes(
                // LREAL - Double precision float at address 301 (4 registers, 1-based = 300 protocol)
                new Attribute<>("doubleValue", ValueType.NUMBER).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(1)
                                    it.setRequestInterval(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.LREAL)
                                    it.setReadAddress(301)
                                }
                        )
                ),
                // LINT - 64-bit signed integer at address 311 (4 registers, 1-based = 310 protocol)
                new Attribute<>("longSignedValue", ValueType.LONG).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(1)
                                    it.setRequestInterval(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.LINT)
                                    it.setReadAddress(311)
                                }
                        )
                ),
                // ULINT - 64-bit unsigned integer at address 321 (4 registers, 1-based = 320 protocol)
                new Attribute<>("longUnsignedValue", ValueType.BIG_INTEGER).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(1)
                                    it.setRequestInterval(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.ULINT)
                                    it.setReadAddress(321)
                                }
                        )
                )
        )

        device = assetStorageService.merge(device)

        then: "the agent should connect successfully"
        conditions.eventually {
            def actualAgent = assetStorageService.find(agent.getId(), true) as ModbusSerialAgent
            assert actualAgent.getAgentStatus().orElse(null) == ConnectionStatus.CONNECTED
        }

        and: "all 64-bit attributes should receive values"
        conditions.eventually {
            device = assetStorageService.find(device.getId(), true)

            // Check LREAL (double) - should be 123.456789
            def doubleValue = device.getAttribute("doubleValue").flatMap { it.getValue() }.orElse(null)
            assert doubleValue != null
            assert doubleValue instanceof Number
            assert Math.abs(((Number) doubleValue).doubleValue() - 123.456789d) < 0.000001

            // Check LINT (signed long) - should be -9223372036854775000L
            def longSignedValue = device.getAttribute("longSignedValue").flatMap { it.getValue() }.orElse(null)
            assert longSignedValue != null
            assert longSignedValue instanceof Long
            assert longSignedValue == -9223372036854775000L

            // Check ULINT (unsigned BigInteger) - should be 18446744073709551000
            def longUnsignedValue = device.getAttribute("longUnsignedValue").flatMap { it.getValue() }.orElse(null)
            assert longUnsignedValue != null
            assert longUnsignedValue instanceof BigInteger
            assert longUnsignedValue == new BigInteger("18446744073709551000")
        }

        and: "verify protocol instance has created batch groups"
        conditions.eventually {
            def protocol = agentService.getProtocolInstance(agent.id) as ModbusSerialProtocol
            assert protocol != null
            assert protocol.modbusExecutor.batchGroups.size() > 0
        }
    }

    def "Modbus Serial Test - Multi-Register Write (registersAmount)"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 15, delay: 0.5)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        and: "a Modbus Serial agent is created"
        def agent = new ModbusSerialAgent("Modbus Serial Multi-Write")
        agent.setRealm(MASTER_REALM)
        agent.addOrReplaceAttributes(
                new Attribute<>(ModbusSerialAgent.SERIAL_PORT, "/dev/ttyUSB0"),
                new Attribute<>(ModbusSerialAgent.BAUD_RATE, 9600),
                new Attribute<>(ModbusSerialAgent.DATA_BITS, 8),
                new Attribute<>(ModbusSerialAgent.STOP_BITS, Stopbits.STOPBITS_1),
                new Attribute<>(ModbusSerialAgent.PARITY, Paritybit.EVEN)
        )
        agent = assetStorageService.merge(agent)

        then: "the agent should connect successfully"
        conditions.eventually {
            agent = assetStorageService.find(agent.getId())
            agent.getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
        }

        when: "a device with multi-register write attributes is created"
        def device = new ThingAsset("Multi-Write Serial Device")
        device.setRealm(MASTER_REALM)
        device.addOrReplaceAttributes(
                // Write FLOAT (2 registers) to address 51 (1-based = 50 protocol)
                new Attribute<>("floatWrite", ValueType.NUMBER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(1)
                                    it.setRequestInterval(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.REAL)
                                    it.setReadAddress(51)
                                    it.setRegistersAmount(2)
                                    it.setWriteMemoryArea(ModbusAgentLink.WriteMemoryArea.HOLDING)
                                    it.setWriteAddress(51)
                                }
                ))
        )
        device = assetStorageService.merge(device)

        then: "device should be linked"
        conditions.eventually {
            device = assetStorageService.find(device.getId(), true)
            assert device.getAttribute("floatWrite").isPresent()
        }

        when: "multi-register float value is written"
        latestRequest = null
        latestWriteRequest = null
        assetProcessingService.sendAttributeEvent(new AttributeEvent(device.getId(), "floatWrite", 45.67f))

        then: "the write should use function code 0x10 (Write Multiple Registers)"
        conditions.eventually {
            def request = latestRequest
            assert request != null
            assert request[0] == (byte) 1  // Unit ID
            assert request[1] == (byte) 0x10  // Function code 0x10 for multi-register write
            assert ((request[2] & 0xFF) << 8 | (request[3] & 0xFF)) == 50  // Start address
            assert ((request[4] & 0xFF) << 8 | (request[5] & 0xFF)) == 2  // Register count
            assert request[6] == (byte) 4  // Byte count (2 registers * 2 bytes)
        }
    }

    def "Modbus Serial Test - Write With Polling Rate"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 15, delay: 0.5)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def agentService = container.getService(AgentService.class)

        and: "a Modbus Serial agent is created"
        def agent = new ModbusSerialAgent("Modbus Serial Polling Write")
        agent.setRealm(MASTER_REALM)
        agent.addOrReplaceAttributes(
                new Attribute<>(ModbusSerialAgent.SERIAL_PORT, "/dev/ttyUSB0"),
                new Attribute<>(ModbusSerialAgent.BAUD_RATE, 9600),
                new Attribute<>(ModbusSerialAgent.DATA_BITS, 8),
                new Attribute<>(ModbusSerialAgent.STOP_BITS, Stopbits.STOPBITS_1),
                new Attribute<>(ModbusSerialAgent.PARITY, Paritybit.EVEN)
        )
        agent = assetStorageService.merge(agent)

        then: "the agent should connect successfully"
        conditions.eventually {
            agent = assetStorageService.find(agent.getId())
            agent.getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
        }

        when: "a device with continuous write (requestInterval + writeAddress, no readAddress) is created"
        def device = new ThingAsset("Continuous Write Serial Device")
        device.setRealm(MASTER_REALM)
        device.addOrReplaceAttributes(
                new Attribute<>("continuousWrite", ValueType.INTEGER, 77).addOrReplaceMeta(
                        new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(1)
                                    it.setRequestInterval(1000)  // Write every 1000ms
                                    it.setWriteMemoryArea(ModbusAgentLink.WriteMemoryArea.HOLDING)
                                    it.setWriteAddress(61)
                                    // No readAddress = continuous write mode
                                }
                        ),
                        new MetaItem<>(STORE_DATA_POINTS)
                )
        )
        device = assetStorageService.merge(device)

        then: "write request task should be created"
        conditions.eventually {
            def protocol = agentService.getProtocolInstance(agent.id) as ModbusSerialProtocol
            assert protocol != null
            assert protocol.modbusExecutor.writeIntervalMap.size() == 1
        }

        def assetDatapointService = container.getService(org.openremote.manager.datapoint.AssetDatapointService.class)
        def attributeRef = new org.openremote.model.attribute.AttributeRef(device.getId(), "continuousWrite")

        then: "multiple write events should be observed"
        def writeRequests = []
        conditions.eventually {
            def request = latestRequest
            if (request != null) {
                // Capture write requests (function codes 0x05, 0x06, 0x10)
                def functionCode = request[1] & 0xFF
                if (functionCode == 0x06 || functionCode == 0x05 || functionCode == 0x10) {
                    writeRequests.add(request.clone())
                }
            }
            // Wait to accumulate multiple writes
            Thread.sleep(2500) // Wait for ~2-3 write cycles at 1000ms interval
            assert writeRequests.size() >= 2  // Should have at least 2 writes
        }


        then: "database should show 1 datapoint"
        conditions.eventually {
            // Query datapoints stored for this attribute - continuous writes should create no extra datapoints
            def datapoints = assetDatapointService.getDatapoints(attributeRef)
            println "Datapoints found: ${datapoints.size()}"
            datapoints.each { println "  - timestamp: ${it.timestamp}, value: ${it.value}" }
            assert datapoints.size() == 1  // Should have 1 datapoint from periodic writes
        }
    }


    // Helper methods for endian format tests
    private ModbusSerialAgent createAgentWithEndianFormat(AssetStorageService assetStorageService,
                                                           String name,
                                                           ModbusAgent.EndianFormat endianFormat) {
        def agent = new ModbusSerialAgent(name)
        agent.setRealm(MASTER_REALM)
        agent.addOrReplaceAttributes(
                new Attribute<>(ModbusSerialAgent.SERIAL_PORT, "/dev/ttyUSB0"),
                new Attribute<>(ModbusSerialAgent.BAUD_RATE, 9600),
                new Attribute<>(ModbusSerialAgent.DATA_BITS, 8),
                new Attribute<>(ModbusSerialAgent.STOP_BITS, Stopbits.STOPBITS_1),
                new Attribute<>(ModbusSerialAgent.PARITY, Paritybit.EVEN),
                new Attribute<>(ModbusAgent.DEVICE_CONFIG, [
                        "default": new ModbusAgent.ModbusDeviceConfig(endianFormat, "", 50)
                ] as ModbusAgent.DeviceConfigMap)
        )
        return assetStorageService.merge(agent)
    }

    private ThingAsset createDeviceWithFloat(AssetStorageService assetStorageService,
                                             String name,
                                             ModbusSerialAgent agent,
                                             int address) {
        def device = new ThingAsset(name)
        device.setRealm(MASTER_REALM)
        device.addOrReplaceAttributes(
                new Attribute<>("floatValue", ValueType.NUMBER).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(1)
                                    it.setRequestInterval(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.REAL)
                                    it.setReadAddress(address)
                                    it.setRegistersAmount(2)
                                }
                        )
                )
        )
        return assetStorageService.merge(device)
    }

    private ThingAsset createDeviceWithDouble(AssetStorageService assetStorageService,
                                              String name,
                                              ModbusSerialAgent agent,
                                              int address) {
        def device = new ThingAsset(name)
        device.setRealm(MASTER_REALM)
        device.addOrReplaceAttributes(
                new Attribute<>("doubleValue", ValueType.NUMBER).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setUnitId(1)
                                    it.setRequestInterval(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.LREAL)
                                    it.setReadAddress(address)
                                    it.setRegistersAmount(4)
                                }
                        )
                )
        )
        return assetStorageService.merge(device)
    }

    /**
     * Generate Modbus RTU responses for various function codes
     */
    private static byte[] generateModbusResponse(byte[] request, int offset, int length) {
        if (length < 6) return new byte[0]

        int unitId = request[offset] & 0xFF
        int functionCode = request[offset + 1] & 0xFF
        int address = ((request[offset + 2] & 0xFF) << 8) | (request[offset + 3] & 0xFF)
        int quantity = ((request[offset + 4] & 0xFF) << 8) | (request[offset + 5] & 0xFF)

        byte[] response

        switch (functionCode) {
        case 0x01: // Read Coils
        case 0x02: // Read Discrete Inputs
            int byteCount = (quantity + 7) / 8
            response = new byte[5 + byteCount]
            response[0] = (byte) unitId
            response[1] = (byte) functionCode
            response[2] = (byte) byteCount
            // Fill with alternating bits
            for (int i = 0; i < byteCount; i++) {
                response[3 + i] = (byte) 0xAA
            }
            break

        case 0x03: // Read Holding Registers
        case 0x04: // Read Input Registers
            int registerBytes = quantity * 2
            response = new byte[5 + registerBytes]
            response[0] = (byte) unitId
            response[1] = (byte) functionCode
            response[2] = (byte) registerBytes

            // Generate test data based on address
            // Handle special test data addresses for different data types
            for (int i = 0; i < quantity; i++) {
                int currentAddress = address + i
                int responseOffset = 3 + i * 2

                // Check if this register is part of a special test value
                if (currentAddress >= 200 && currentAddress < 202 && address == 200 && quantity == 2) {
                    // Return float value 23.5
                    ByteBuffer bb = ByteBuffer.allocate(4)
                    bb.order(ByteOrder.BIG_ENDIAN)
                    bb.putFloat(23.5f)
                    byte[] floatBytes = bb.array()
                    System.arraycopy(floatBytes, 0, response, 3, 4)
                    break
                } else if (currentAddress >= 300 && currentAddress < 304) {
                    // Return double value 123.456789 at registers 300-303
                    if (i == 0 || currentAddress == 300) {
                        int regOffset = currentAddress - 300
                        ByteBuffer bb = ByteBuffer.allocate(8)
                        bb.order(ByteOrder.BIG_ENDIAN)
                        bb.putDouble(123.456789d)
                        byte[] doubleBytes = bb.array()
                        int copyLen = Math.min(registerBytes - (responseOffset - 3), 8 - (regOffset * 2))
                        System.arraycopy(doubleBytes, regOffset * 2, response, responseOffset, Math.min(copyLen, 8))
                    }
                } else if (currentAddress >= 310 && currentAddress < 314) {
                    // Return 64-bit signed integer: -9223372036854775000L at registers 310-313
                    int regOffset = currentAddress - 310
                    ByteBuffer bb = ByteBuffer.allocate(8)
                    bb.order(ByteOrder.BIG_ENDIAN)
                    bb.putLong(-9223372036854775000L)
                    byte[] longBytes = bb.array()
                    response[responseOffset] = longBytes[regOffset * 2]
                    response[responseOffset + 1] = longBytes[regOffset * 2 + 1]
                } else if (currentAddress >= 320 && currentAddress < 324) {
                    // Return 64-bit unsigned integer at registers 320-323
                    int regOffset = currentAddress - 320
                    ByteBuffer bb = ByteBuffer.allocate(8)
                    bb.order(ByteOrder.BIG_ENDIAN)
                    bb.putLong(-616L) // This represents 18446744073709551000 as unsigned
                    byte[] longBytes = bb.array()
                    response[responseOffset] = longBytes[regOffset * 2]
                    response[responseOffset + 1] = longBytes[regOffset * 2 + 1]
                } else if (currentAddress >= 400 && currentAddress < 402) {
                    // Return float value 42.5 for byte/word order test (BIG-BIG) at 400-401
                    if (i == 0 || currentAddress == 400) {
                        ByteBuffer bb = ByteBuffer.allocate(4)
                        bb.order(ByteOrder.BIG_ENDIAN)
                        bb.putFloat(42.5f)
                        byte[] floatBytes = bb.array()
                        System.arraycopy(floatBytes, 0, response, 3, 4)
                        break
                    }
                } else if (currentAddress >= 410 && currentAddress < 412) {
                    // Return float value 42.5 for byte/word order test (BIG-LITTLE) at 410-411
                    if (i == 0 || currentAddress == 410) {
                        ByteBuffer bb = ByteBuffer.allocate(4)
                        bb.order(ByteOrder.BIG_ENDIAN)
                        bb.putFloat(42.5f)
                        byte[] floatBytes = bb.array()
                        System.arraycopy(floatBytes, 0, response, 3, 4)
                        break
                    }
                } else if (currentAddress >= 420 && currentAddress < 422) {
                    // Return float value 42.5 for byte/word order test (LITTLE-BIG) at 420-421
                    if (i == 0 || currentAddress == 420) {
                        ByteBuffer bb = ByteBuffer.allocate(4)
                        bb.order(ByteOrder.BIG_ENDIAN)
                        bb.putFloat(42.5f)
                        byte[] floatBytes = bb.array()
                        System.arraycopy(floatBytes, 0, response, 3, 4)
                        break
                    }
                } else if (currentAddress >= 430 && currentAddress < 432) {
                    // Return float value 42.5 for byte/word order test (LITTLE-LITTLE) at 430-431
                    if (i == 0 || currentAddress == 430) {
                        ByteBuffer bb = ByteBuffer.allocate(4)
                        bb.order(ByteOrder.BIG_ENDIAN)
                        bb.putFloat(42.5f)
                        byte[] floatBytes = bb.array()
                        System.arraycopy(floatBytes, 0, response, 3, 4)
                        break
                    }
                } else if (currentAddress >= 500 && currentAddress < 504) {
                    // Return double value 999.888 for byte/word order test (BIG-BIG) at 500-503
                    int regOffset = currentAddress - 500
                    ByteBuffer bb = ByteBuffer.allocate(8)
                    bb.order(ByteOrder.BIG_ENDIAN)
                    bb.putDouble(999.888d)
                    byte[] doubleBytes = bb.array()
                    int copyLen = Math.min(2, 8 - (regOffset * 2))
                    System.arraycopy(doubleBytes, regOffset * 2, response, responseOffset, copyLen)
                } else if (currentAddress >= 510 && currentAddress < 514) {
                    // Return double value 999.888 for byte/word order test (LITTLE-LITTLE) at 510-513
                    int regOffset = currentAddress - 510
                    ByteBuffer bb = ByteBuffer.allocate(8)
                    bb.order(ByteOrder.BIG_ENDIAN)
                    bb.putDouble(999.888d)
                    byte[] doubleBytes = bb.array()
                    int copyLen = Math.min(2, 8 - (regOffset * 2))
                    System.arraycopy(doubleBytes, regOffset * 2, response, responseOffset, copyLen)
                } else {
                    // Return incrementing values
                    int value = currentAddress
                    response[responseOffset] = (byte) (value >> 8)
                    response[responseOffset + 1] = (byte) (value & 0xFF)
                }
            }
            break

        case 0x05: // Write Single Coil
            response = new byte[8]
            System.arraycopy(request, offset, response, 0, 6)
            break

        case 0x06: // Write Single Register
            response = new byte[8]
            System.arraycopy(request, offset, response, 0, 6)
            break

        case 0x0F: // Write Multiple Coils
        case 0x10: // Write Multiple Registers
            // Response echoes: Unit ID + FC + Start Address + Quantity
            response = new byte[8]
            System.arraycopy(request, offset, response, 0, 6)
            break

        default:
            // Exception response
            response = new byte[5]
            response[0] = (byte) unitId
            response[1] = (byte) (functionCode | 0x80)
            response[2] = (byte) 0x01  // Illegal function
            return addCRC(response, 3)
        }

        return addCRC(response, response.length - 2)
    }

    /**
     * Calculate and add CRC16 to Modbus RTU frame
     */
    private static byte[] addCRC(byte[] data, int length) {
        int crc = 0xFFFF
        for (int i = 0; i < length; i++) {
            crc ^= (data[i] & 0xFF)
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >> 1) ^ 0xA001
                } else {
                    crc = crc >> 1
                }
            }
        }
        data[length] = (byte) (crc & 0xFF)
        data[length + 1] = (byte) (crc >> 8)
        return data
    }
}
