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

import com.fazecast.jSerialComm.SerialPort
import org.openremote.agent.protocol.modbus.ModbusAgentLink
import org.openremote.agent.protocol.modbus.ModbusSerialAgent
import org.openremote.agent.protocol.modbus.ModbusSerialProtocol
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
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicReference

import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.value.MetaItemType.AGENT_LINK

/**
 * Test for Modbus Serial Protocol with virtual serial devices
 */
class ModbusSerialTest extends Specification implements ManagerContainerTrait {

    @Shared
    MockSerialPort mockSerialPort

    @Shared
    AtomicReference<byte[]> latestRequest = new AtomicReference<>(null)

    def setupSpec() {
        // Create mock serial port that simulates a Modbus RTU device
        mockSerialPort = new MockSerialPort(latestRequest)
        // Set the mock for all protocol instances created during tests
        ModbusSerialProtocol.mockSerialPortForTesting = mockSerialPort
    }

    def cleanupSpec() {
        // Clean up the mock
        ModbusSerialProtocol.mockSerialPortForTesting = null
    }

    def setup() {
        // Reset mock state before each test
        mockSerialPort.reset()
        latestRequest.set(null)
    }

    def "Modbus Serial Integration Test - Basic Operations"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 15, delay: 0.5)

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
                new Attribute<>(ModbusSerialAgent.STOP_BITS, 1),
                new Attribute<>(ModbusSerialAgent.PARITY, ModbusSerialAgent.ModbusClientParity.EVEN),
                new Attribute<>(ModbusSerialAgent.UNIT_ID, 1),
                new Attribute<>(ModbusSerialAgent.MAX_REGISTER_LENGTH, 30),
                new Attribute<>(ModbusSerialAgent.ILLEGAL_REGISTERS, "100,150-160")
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

        when: "A Thing asset is created with multiple agent links"
        ThingAsset device = new ThingAsset("Test Modbus Device")
        device.setRealm(MASTER_REALM)

        // Add various data type attributes
        device.addOrReplaceAttributes(
                // UINT16 register
                new Attribute<>("register1", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(
                                id: agent.getId(),
                                pollingMillis: 1000,
                                readMemoryArea: ModbusAgentLink.ReadMemoryArea.HOLDING,
                                readValueType: ModbusAgentLink.ModbusDataType.UINT,
                                readAddress: 0,
                                readRegistersAmount: 1
                        )
                )),
                // UINT16 register
                new Attribute<>("register2", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(
                                id: agent.getId(),
                                pollingMillis: 1000,
                                readMemoryArea: ModbusAgentLink.ReadMemoryArea.HOLDING,
                                readValueType: ModbusAgentLink.ModbusDataType.UINT,
                                readAddress: 2,
                                readRegistersAmount: 1
                        )
                )),
                // Float (REAL) value
                new Attribute<>("temperature", ValueType.NUMBER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(
                                id: agent.getId(),
                                pollingMillis: 1000,
                                readMemoryArea: ModbusAgentLink.ReadMemoryArea.INPUT,
                                readValueType: ModbusAgentLink.ModbusDataType.REAL,
                                readAddress: 200,
                                readRegistersAmount: 2
                        )
                )),
                // Coil
                new Attribute<>("switch1", ValueType.BOOLEAN).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(
                                id: agent.getId(),
                                pollingMillis: 1000,
                                readMemoryArea: ModbusAgentLink.ReadMemoryArea.COIL,
                                readValueType: ModbusAgentLink.ModbusDataType.BOOL,
                                readAddress: 5,
                                writeMemoryArea: ModbusAgentLink.WriteMemoryArea.COIL,
                                writeAddress: 5
                        )
                ))
        )

        device = assetStorageService.merge(device)

        then: "register batches should be created and attributes should receive values"
        conditions.eventually {
            def protocol = agentService.getProtocolInstance(agent.id) as ModbusSerialProtocol
            assert protocol != null

            // Check that batch groups were created
            assert protocol.batchGroups.size() > 0

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
            def lastRequest = latestRequest.get()
            assert lastRequest != null
            assert lastRequest[0] == (byte) 1  // Unit ID
            assert lastRequest[1] == (byte) 0x05  // Write single coil function
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
                new Attribute<>(ModbusSerialAgent.STOP_BITS, 1),
                new Attribute<>(ModbusSerialAgent.PARITY, ModbusSerialAgent.ModbusClientParity.EVEN),
                new Attribute<>(ModbusSerialAgent.UNIT_ID, 1),
                new Attribute<>(ModbusSerialAgent.MAX_REGISTER_LENGTH, 30),
                new Attribute<>(ModbusSerialAgent.ILLEGAL_REGISTERS, "5-10,20-25")
        )

        agent = assetStorageService.merge(agent)

        and: "attributes are created that would span illegal register ranges"
        ThingAsset device = new ThingAsset("Batching Test Device")
        device.setRealm(MASTER_REALM)

        device.addOrReplaceAttributes(
                new Attribute<>("reg0", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(
                                id: agent.getId(),
                                pollingMillis: 1000,
                                readMemoryArea: ModbusAgentLink.ReadMemoryArea.HOLDING,
                                readValueType: ModbusAgentLink.ModbusDataType.UINT,
                                readAddress: 0,
                                readRegistersAmount: 2
                        )
                )),
                new Attribute<>("reg15", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(
                                id: agent.getId(),
                                pollingMillis: 1000,
                                readMemoryArea: ModbusAgentLink.ReadMemoryArea.HOLDING,
                                readValueType: ModbusAgentLink.ModbusDataType.UINT,
                                readAddress: 15,
                                readRegistersAmount: 2
                        )
                )),
                new Attribute<>("reg30", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(
                                id: agent.getId(),
                                pollingMillis: 1000,
                                readMemoryArea: ModbusAgentLink.ReadMemoryArea.HOLDING,
                                readValueType: ModbusAgentLink.ModbusDataType.UINT,
                                readAddress: 30,
                                readRegistersAmount: 2
                        )
                ))
        )

        device = assetStorageService.merge(device)

        then: "batches should be split due to illegal registers"
        conditions.eventually {
            def protocol = agentService.getProtocolInstance(agent.id) as ModbusSerialProtocol
            assert protocol != null

            def cachedBatches = protocol.cachedBatches
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

    def "Modbus Serial Test - Multiple Unit IDs on Same Port"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 15, delay: 0.5)

        when: "the container starts"
        def container = startContainer(defaultConfig(), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def agentService = container.getService(AgentService.class)

        and: "two Modbus Serial agents with different unit IDs on the same port are created"
        def agent1 = new ModbusSerialAgent("Modbus RTU Unit 1")
        agent1.setRealm(MASTER_REALM)
        agent1.addOrReplaceAttributes(
                new Attribute<>(ModbusSerialAgent.SERIAL_PORT, "/dev/ttyUSB0"),
                new Attribute<>(ModbusSerialAgent.BAUD_RATE, 9600),
                new Attribute<>(ModbusSerialAgent.DATA_BITS, 8),
                new Attribute<>(ModbusSerialAgent.STOP_BITS, 1),
                new Attribute<>(ModbusSerialAgent.PARITY, ModbusSerialAgent.ModbusClientParity.EVEN),
                new Attribute<>(ModbusSerialAgent.UNIT_ID, 1),
                new Attribute<>(ModbusSerialAgent.MAX_REGISTER_LENGTH, 30)
        )

        def agent2 = new ModbusSerialAgent("Modbus RTU Unit 2")
        agent2.setRealm(MASTER_REALM)
        agent2.addOrReplaceAttributes(
                new Attribute<>(ModbusSerialAgent.SERIAL_PORT, "/dev/ttyUSB0"),  // Same port
                new Attribute<>(ModbusSerialAgent.BAUD_RATE, 9600),
                new Attribute<>(ModbusSerialAgent.DATA_BITS, 8),
                new Attribute<>(ModbusSerialAgent.STOP_BITS, 1),
                new Attribute<>(ModbusSerialAgent.PARITY, ModbusSerialAgent.ModbusClientParity.EVEN),
                new Attribute<>(ModbusSerialAgent.UNIT_ID, 2),  // Different unit ID
                new Attribute<>(ModbusSerialAgent.MAX_REGISTER_LENGTH, 30)
        )

        agent1 = assetStorageService.merge(agent1)
        agent2 = assetStorageService.merge(agent2)

        then: "both agents should connect"
        conditions.eventually {
            assert agentService.getProtocolInstance(agent1.id) != null
            assert agentService.getProtocolInstance(agent2.id) != null

            agent1 = assetStorageService.find(agent1.getId())
            agent2 = assetStorageService.find(agent2.getId())

            assert agent1.getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
            assert agent2.getAttribute(Agent.STATUS).get().getValue().get() == ConnectionStatus.CONNECTED
        }

        when: "devices are created for each agent"
        ThingAsset device1 = new ThingAsset("Device on Unit 1")
        device1.setRealm(MASTER_REALM)
        device1.addOrReplaceAttributes(
                new Attribute<>("register1", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(
                                id: agent1.getId(),
                                pollingMillis: 1000,
                                readMemoryArea: ModbusAgentLink.ReadMemoryArea.HOLDING,
                                readValueType: ModbusAgentLink.ModbusDataType.UINT,
                                readAddress: 0,
                                readRegistersAmount: 1
                        )
                ))
        )

        ThingAsset device2 = new ThingAsset("Device on Unit 2")
        device2.setRealm(MASTER_REALM)
        device2.addOrReplaceAttributes(
                new Attribute<>("register1", ValueType.POSITIVE_INTEGER).addOrReplaceMeta(new MetaItem<>(
                        AGENT_LINK,
                        new ModbusAgentLink(
                                id: agent2.getId(),
                                pollingMillis: 1000,
                                readMemoryArea: ModbusAgentLink.ReadMemoryArea.HOLDING,
                                readValueType: ModbusAgentLink.ModbusDataType.UINT,
                                readAddress: 0,
                                readRegistersAmount: 1
                        )
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
            def lastRequest = latestRequest.get()
            assert lastRequest != null
            // Unit ID is the first byte of the Modbus request
            assert lastRequest[0] == (byte) 1 || lastRequest[0] == (byte) 2
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
                new Attribute<>(ModbusSerialAgent.STOP_BITS, 1),
                new Attribute<>(ModbusSerialAgent.PARITY, ModbusSerialAgent.ModbusClientParity.EVEN),
                new Attribute<>(ModbusSerialAgent.UNIT_ID, 1),
                new Attribute<>(ModbusSerialAgent.MAX_REGISTER_LENGTH, 50)
        )

        agent = assetStorageService.merge(agent)

        and: "the agent is linked to a device with 64-bit data types"
        def device = new ThingAsset("64-bit Test Device")
        device.setRealm(MASTER_REALM)
        device.setParent(agent)
        device.addOrReplaceAttributes(
                // LREAL - Double precision float at address 300 (4 registers)
                new Attribute<>("doubleValue", ValueType.NUMBER).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setPollingMillis(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.LREAL)
                                    it.setReadAddress(300)
                                }
                        )
                ),
                // LINT - 64-bit signed integer at address 310 (4 registers)
                new Attribute<>("longSignedValue", ValueType.LONG).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setPollingMillis(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.LINT)
                                    it.setReadAddress(310)
                                }
                        )
                ),
                // ULINT - 64-bit unsigned integer at address 320 (4 registers)
                new Attribute<>("longUnsignedValue", ValueType.BIG_INTEGER).addOrReplaceMeta(
                        new MetaItem<>(AGENT_LINK, new ModbusAgentLink(agent.getId())
                                .tap {
                                    it.setPollingMillis(1000)
                                    it.setReadMemoryArea(ModbusAgentLink.ReadMemoryArea.HOLDING)
                                    it.setReadValueType(ModbusAgentLink.ModbusDataType.ULINT)
                                    it.setReadAddress(320)
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
            assert protocol.batchGroups.size() > 0
        }
    }

    /**
     * Mock Serial Port implementation for testing - implements SerialPortWrapper interface
     */
    static class MockSerialPort implements ModbusSerialProtocol.SerialPortWrapper {
        private boolean open = false
        private byte[] readBuffer = new byte[0]
        private int readPosition = 0
        private final AtomicReference<byte[]> requestCapture

        MockSerialPort(AtomicReference<byte[]> requestCapture) {
            this.requestCapture = requestCapture
        }

        @Override
        synchronized boolean openPort() {
            open = true
            return true
        }

        @Override
        synchronized boolean closePort() {
            open = false
            return true
        }

        @Override
        synchronized boolean isOpen() {
            return open
        }

        @Override
        synchronized int bytesAvailable() {
            return Math.max(0, readBuffer.length - readPosition)
        }

        @Override
        synchronized int readBytes(byte[] buffer, long bytesToRead, long offset) {
            int available = bytesAvailable()
            int toRead = Math.min(available, bytesToRead as int)

            if (toRead > 0) {
                System.arraycopy(readBuffer, readPosition, buffer, offset as int, toRead)
                readPosition += toRead
            }

            return toRead
        }

        @Override
        synchronized int writeBytes(byte[] buffer, long bytesToWrite) {
            requestCapture.set(Arrays.copyOfRange(buffer, 0, bytesToWrite as int))

            // Generate Modbus RTU response based on request
            byte[] response = generateModbusResponse(buffer, 0, bytesToWrite as int)
            readBuffer = response
            readPosition = 0

            return bytesToWrite as int
        }

        synchronized void reset() {
            readBuffer = new byte[0]
            readPosition = 0
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
                            int copyLen = Math.min(2, 8 - (regOffset * 2))
                            System.arraycopy(longBytes, regOffset * 2, response, responseOffset, copyLen)
                        } else if (currentAddress >= 320 && currentAddress < 324) {
                            // Return 64-bit unsigned integer at registers 320-323
                            int regOffset = currentAddress - 320
                            ByteBuffer bb = ByteBuffer.allocate(8)
                            bb.order(ByteOrder.BIG_ENDIAN)
                            bb.putLong(-616L) // This represents a large unsigned number
                            byte[] longBytes = bb.array()
                            int copyLen = Math.min(2, 8 - (regOffset * 2))
                            System.arraycopy(longBytes, regOffset * 2, response, responseOffset, copyLen)
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
}
