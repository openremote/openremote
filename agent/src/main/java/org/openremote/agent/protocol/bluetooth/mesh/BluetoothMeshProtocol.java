/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.bluetooth.mesh;

import com.welie.blessed.*;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.bluetooth.mesh.models.SigModelParser;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.syslog.SyslogCategory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.openremote.model.asset.agent.AgentLink.getOrThrowAgentLinkProperty;

public class BluetoothMeshProtocol extends AbstractProtocol<BluetoothMeshAgent, BluetoothMeshAgentLink> {

    // Constants ------------------------------------------------------------------------------------

    public static final String PROTOCOL_DISPLAY_NAME = "Bluetooth Mesh";
    public static final int DEFAULT_MTU = 20;
    public static final int DEFAULT_SEQUENCE_NUMBER = 1;
    public static final int DEFAULT_NETWORK_KEY_INDEX = 0;
    public static final int DEFAULT_APPLICATION_KEY_INDEX = 0;
    public static final String REGEXP_INDEX_AND_KEY = "^(\\s*(0|([1-9]+[0-9]*))\\s*:)?(\\s*[0-9A-Fa-f]{32}\\s*)";
    public static final String REGEXP_PROXY_ADDRESS = "^(?:[0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$";


    // Class Members --------------------------------------------------------------------------------

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, BluetoothMeshProtocol.class.getName());

    private static final MainThreadManager mainThread = new MainThreadManager();
    private static ScheduledFuture<?> mainThreadFuture = null;
    private static final BluetoothCentralManagerCallback bluetoothManagerCallback = new BluetoothCentralManagerCallback() {
        @Override
        public void onConnectedPeripheral(BluetoothPeripheral peripheral) {
            LOG.info("BluetoothCentralManager::onConnectedPeripheral: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + "]");

            synchronized (BluetoothMeshProtocol.class) {
                for (BluetoothMeshNetwork network : networkList) {
                    network.onConnectedPeripheral(peripheral);
                }
            }
        }

        @Override
        public void onConnectionFailed(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
            LOG.info("BluetoothCentralManager::onConnectionFailed: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + ", Status=" + status +"]");

            synchronized (BluetoothMeshProtocol.class) {
                for (BluetoothMeshNetwork network : networkList) {
                    network.onConnectionFailed(peripheral, status);
                }
            }
        }

        @Override
        public void onDisconnectedPeripheral(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
            LOG.info("BluetoothCentralManager::onDisconnectedPeripheral: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + ", Status=" + status +"]");

            synchronized (BluetoothMeshProtocol.class) {
                for (BluetoothMeshNetwork network : networkList) {
                    network.onDisconnectedPeripheral(peripheral, status);
                }
            }
        }

        @Override
        public void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
            LOG.info("BluetoothCentralManager::onDiscoveredPeripheral: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + ", ScanResult=" + scanResult +"]");

            synchronized (BluetoothMeshProtocol.class) {
                for (BluetoothMeshNetwork network : networkList) {
                    network.onDiscoveredPeripheral(peripheral, scanResult);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            LOG.info("BluetoothCentralManager::onScanFailed: [errorCode=" + errorCode + "]");

            synchronized (BluetoothMeshProtocol.class) {
                for (BluetoothMeshNetwork network : networkList) {
                    network.onScanFailed(errorCode);
                }
            }
        }
    };
    private static final BluetoothCentralManager bluetoothCentral = new BluetoothCentralManager(bluetoothManagerCallback);
    private static final List<BluetoothMeshNetwork> networkList = new LinkedList<>();
    // Not ideal this but will do for now
    private static SequenceNumberPersistencyManager sequenceNumberManager;

    public synchronized static void initMainThread(ScheduledExecutorService executorService) {
        if (mainThreadFuture == null) {
            mainThreadFuture = executorService.schedule(mainThread, 0, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized static void addNetwork(BluetoothMeshNetwork network) {
        networkList.add(network);
    }


    // Private Instance Fields --------------------------------------------------------------------

    private volatile BluetoothMeshNetwork meshNetwork;
    private final Map<AttributeRef, Consumer<Object>> sensorValueConsumerMap = new HashMap<>();


    // Constructors -------------------------------------------------------------------------------

    public BluetoothMeshProtocol(BluetoothMeshAgent agent) {
        super(agent);
    }


    // Implements Protocol --------------------------------------------------------------------------

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "bluetoothmesh://" + ((meshNetwork != null && meshNetwork.getNetworkKey() != null) ?
            MeshParserUtils.bytesToHex(meshNetwork.getNetworkKey().getKey(), false) : "");

    }


    // Implements AbstractProtocol ------------------------------------------------------------------

    @Override
    protected synchronized void doStart(Container container) throws Exception {

        synchronized (BluetoothMeshProtocol.mainThread) {
            if (BluetoothMeshProtocol.sequenceNumberManager == null) {
                Path storagePath = container.getService(PersistenceService.class).getStorageDir();
                BluetoothMeshProtocol.sequenceNumberManager = new SequenceNumberPersistencyManager(storagePath.resolve("bluetoothmesh"));
                BluetoothMeshProtocol.sequenceNumberManager.load();
            }
        }

        LOG.info("Starting Bluetooth Mesh protocol.");
        String meshNetKeyParam = agent.getNetworkKey().orElseThrow(() -> {
            String msg = "No Bluetooth Mesh network key provided for protocol: " + this;
            LOG.warning(msg);
            return new IllegalArgumentException(msg);
        });
        Integer netKeyIndex = extractIndex(meshNetKeyParam, DEFAULT_NETWORK_KEY_INDEX);
        String netKeyAsString = extractKey(meshNetKeyParam);
        if (netKeyIndex == null || netKeyAsString == null) {
            String msg = "Format of network key '" + meshNetKeyParam + "' is invalid for protocol: " + this;
            LOG.warning(msg);
            throw new IllegalArgumentException(msg);
        }
        NetworkKey networkKey = new NetworkKey(netKeyIndex, MeshParserUtils.toByteArray(netKeyAsString));

        String meshAppKeyParam = agent.getApplicationKey().orElseThrow(() -> {
            String msg = "No Bluetooth Mesh application key provided for protocol: " + this;
            LOG.warning(msg);
            return new IllegalArgumentException(msg);
        });
        Integer appKeyIndex = extractIndex(meshAppKeyParam, DEFAULT_APPLICATION_KEY_INDEX);
        String appKeyAsString = extractKey(meshAppKeyParam);
        if (appKeyIndex == null || appKeyAsString == null) {
            String msg = "Format of application key '" + meshAppKeyParam + "' is invalid for protocol: " + this;
            LOG.warning(msg);
            throw new IllegalArgumentException(msg);
        }
        ApplicationKey applicationKey = new ApplicationKey(appKeyIndex, MeshParserUtils.toByteArray(appKeyAsString));

        String proxyAddress = agent.getProxyAddress().orElse(null);
        proxyAddress = proxyAddress != null ? proxyAddress.trim() : null;
        if (proxyAddress != null && !proxyAddress.matches(REGEXP_PROXY_ADDRESS)) {
            String msg = "Format of proxy address '" + proxyAddress + "' is invalid for protocol: " + this;
            LOG.warning(msg);
            throw new IllegalArgumentException(msg);
        }

        String sourceAddressParam = agent.getSourceAddress().orElseThrow(() -> {
            String msg = "No Bluetooth Mesh unicast source address provided for protocol: " + this;
            LOG.warning(msg);
            return new IllegalArgumentException(msg);
        });
        final Integer sourceAddress = toIntegerAddress(sourceAddressParam, null);
        if (sourceAddress == null) {
            String msg = "Format of Bluetooth Mesh unicast source address '" + sourceAddressParam + "' is invalid for protocol: " + this;
            throw new IllegalArgumentException(msg);
        }

        int sequenceNumberParam = agent.getSequenceNumber().orElse(DEFAULT_SEQUENCE_NUMBER);
        final int mtuParam = agent.getMtu().orElse(DEFAULT_MTU);

        Map<Integer, ApplicationKey> applicationKeyMap = new HashMap<>();
        applicationKeyMap.put(appKeyIndex, applicationKey);
        final ScheduledExecutorService finalExecutorService = executorService;
        Consumer<ConnectionStatus> statusConsumer = new Consumer<ConnectionStatus>() {
            @Override
            public void accept(ConnectionStatus connectionStatus) {
                setConnectionStatus(connectionStatus);
                if (connectionStatus == ConnectionStatus.CONNECTED) {
                    finalExecutorService.execute(() -> updateAllAttributes());
                }
            }
        };

        Integer oldSequenceNumber = BluetoothMeshProtocol.sequenceNumberManager.getSequenceNumber(networkKey, sourceAddress);
        if (oldSequenceNumber == null) {
            oldSequenceNumber = sequenceNumberParam;
            BluetoothMeshProtocol.sequenceNumberManager.save(networkKey, sourceAddress, oldSequenceNumber);
        }
        BluetoothMeshProtocol.initMainThread(executorService);
        meshNetwork = new BluetoothMeshNetwork(
            BluetoothMeshProtocol.bluetoothCentral, BluetoothMeshProtocol.sequenceNumberManager, BluetoothMeshProtocol.mainThread,
            proxyAddress, sourceAddress, networkKey, applicationKeyMap, mtuParam, oldSequenceNumber, executorService, statusConsumer
        );
        BluetoothMeshProtocol.addNetwork(meshNetwork);
        BluetoothMeshProtocol.mainThread.enqueue(() -> meshNetwork.start());
    }

    @Override
    protected synchronized void doStop(Container container) throws Exception {
        LOG.info("Stopping Bluetooth Mesh protocol.");
        if (meshNetwork != null) {
            meshNetwork.stop();
            meshNetwork = null;
        }
    }

    @Override
    protected synchronized void doLinkAttribute(String assetId, Attribute<?> attribute, BluetoothMeshAgentLink agentLink) throws RuntimeException {
        if (meshNetwork == null) {
            return;
        }
        final AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
        Integer appKeyIndex = getOrThrowAgentLinkProperty(agentLink.getAppKeyIndex(), "Bluetooth Mesh Application Key Index");
        String modelName = getOrThrowAgentLinkProperty(agentLink.getModelName(), "Bluetooth Mesh Model Name");
        Integer modelId = toModelId(modelName, attributeRef);
        if (modelId == null) {
            return;
        }
        String addressAsString = getOrThrowAgentLinkProperty(agentLink.getAddress(), "Bluetooth Mesh Address");
        Integer address = toIntegerAddress(addressAsString, attributeRef);
        if (address == null) {
            return;
        }
        LOG.info(
            "Linking Bluetooth Mesh attribute: [address: '" + String.format("0x%04X", address) + "', model: '" +
                 modelName + "', appKeyIndex: '" + appKeyIndex + "'] - " + attributeRef
        );
        Class<?> clazz = attribute.getTypeClass();
        Consumer<Object> sensorValueConsumer = value -> updateLinkedAttribute(new AttributeState(attributeRef, toAttributeValue(value, clazz)));
        sensorValueConsumerMap.put(attributeRef, sensorValueConsumer);

        meshNetwork.addMeshModel(address, modelId, appKeyIndex);
        meshNetwork.addSensorValueConsumer(address, modelId, sensorValueConsumer);

        if (meshNetwork.isConnected()) {
            meshNetwork.sendMeshGetCommand(address, modelId);
        }
    }

    @Override
    protected synchronized void doUnlinkAttribute(String assetId, Attribute<?> attribute, BluetoothMeshAgentLink agentLink) {
        if (meshNetwork == null) {
            return;
        }
        final AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
        Integer appKeyIndex = getOrThrowAgentLinkProperty(agentLink.getAppKeyIndex(), "Bluetooth Mesh Application Key Index");
        String modelName = getOrThrowAgentLinkProperty(agentLink.getModelName(), "Bluetooth Mesh Model Name");
        Integer modelId = toModelId(modelName, attributeRef);
        if (modelId == null) {
            return;
        }
        String addressAsString = getOrThrowAgentLinkProperty(agentLink.getAddress(), "Bluetooth Mesh Address");
        Integer address = toIntegerAddress(addressAsString, attributeRef);
        if (address == null) {
            return;
        }
        LOG.info(
            "Unlinking Bluetooth Mesh attribute: [address: '" + String.format("0x%04X", address) + "', model: '" +
                 modelName + "', appKeyIndex: '" + appKeyIndex + "'] - " + attributeRef
        );
        Consumer<Object> sensorValueConsumer = sensorValueConsumerMap.remove(attributeRef);
        meshNetwork.removeSensorValueConsumer(address, modelId, sensorValueConsumer);
    }

    @Override
    protected synchronized void doLinkedAttributeWrite(BluetoothMeshAgentLink agentLink, AttributeEvent event, Object processedValue) {
        if (meshNetwork == null) {
            return;
        }
        Integer appKeyIndex = getOrThrowAgentLinkProperty(agentLink.getAppKeyIndex(), "Bluetooth Mesh Application Key Index");
        String modelName = getOrThrowAgentLinkProperty(agentLink.getModelName(), "Bluetooth Mesh Model Name");
        Integer modelId = toModelId(modelName, null);
        if (modelId == null) {
            return;
        }
        String addressAsString = getOrThrowAgentLinkProperty(agentLink.getAddress(), "Bluetooth Mesh Address");
        Integer address = toIntegerAddress(addressAsString, null);
        if (address == null) {
            return;
        }
        LOG.info(
            "Writing Bluetooth Mesh attribute: [address: '" + String.format("0x%04X", address) + "', model: '" +
                modelName + "', appKeyIndex: '" + appKeyIndex + "', value: '" + processedValue + "']"
        );

        meshNetwork.sendMeshSetCommand(address, modelId, processedValue);
        meshNetwork.sendMeshGetCommand(address, modelId);
    }


    // Private Instance Methods -------------------------------------------------------------------

    private synchronized void updateAllAttributes() {
        if (meshNetwork != null) {
            meshNetwork.sendMeshGetCommands();
        }
    }

    private Integer extractIndex(String indexAndKey, int defaultIndex) {
        Integer index = null;
        if (indexAndKey.matches(REGEXP_INDEX_AND_KEY)) {
            String[] indexAndKeyArr = indexAndKey.split(":");
            if (indexAndKeyArr.length == 2) {
                try {
                    index = Integer.decode(indexAndKeyArr[0].trim());
                } catch (NumberFormatException e) {}
            } else {
                index = defaultIndex;
            }
        }
        return index;
    }

    private String extractKey(String indexAndKey) {
        String key = null;
        if (indexAndKey.matches(REGEXP_INDEX_AND_KEY)) {
            String[] indexAndKeyArr = indexAndKey.split(":");
            key = indexAndKeyArr[indexAndKeyArr.length == 2 ? 1 : 0].trim();
        }
        return key;
    }

    private Integer toIntegerAddress(String addressAsString, AttributeRef attributeRef) {
        if (addressAsString == null) {
            return null;
        }
        Integer address = null;
        try {
            address = Integer.decode("0x" + addressAsString);
        } catch (NumberFormatException e) {}
        if (address == null) {
            if (attributeRef != null) {
                LOG.warning("Format of Bluetooth Mesh unicast address value '" + addressAsString + "' is invalid for protocol attribute: " + attributeRef);
            } else {
                LOG.warning("Format of Bluetooth Mesh unicast address value '" + addressAsString + "' is invalid.");
            }
        }
        return address;
    }

    private Integer toModelId(String modelName, AttributeRef attributeRef) {
        if (modelName == null) {
            return null;
        }
        Integer modelId = null;
        if (modelName.toUpperCase().contains("ONOFF")) {
            modelId = SigModelParser.GENERIC_ON_OFF_SERVER & 0xFFFF;
        }
        if (modelId == null) {
            if (attributeRef != null) {
                LOG.warning("Unknown or unsupported Bluetooth Mesh model name '" + modelName + "' for protocol attribute: " + attributeRef);
            } else {
                LOG.warning("Unknown or unsupported Bluetooth Mesh model name '" + modelName);
            }
        }
        return modelId;
    }

    private Object toAttributeValue(Object value, Class<?> clazz) {
        if (value == null || clazz == null) {
            return null;
        }
        Object retValue = null;
        if (clazz == String.class) {
            if (value instanceof Boolean) {
                retValue = ((Boolean)value) ? "On" : "Off";
            } else if (value instanceof Integer || value instanceof Double || value instanceof String) {
                retValue = value.toString();
            }
        } else if (clazz == Boolean.class) {
            if (value instanceof Boolean) {
                retValue = value;
            } else if (value instanceof String) {
                String strValue = ((String)value).trim().toUpperCase();
                if (strValue.equals("ON") || strValue.equals("TRUE") || strValue.equals("1")) {
                    retValue = Boolean.valueOf(true);
                } else if (strValue.equals("OFF") || strValue.equals("FALSE") || strValue.equals("0")) {
                    retValue = Boolean.valueOf(false);
                }
            } else if (value instanceof Integer) {
                retValue = ((Integer)value) == 0 ? Boolean.valueOf(false) : Boolean.valueOf(true);
            } else if (value instanceof Double) {
                retValue = ((Double)value) == 0 ? Boolean.valueOf(false) : Boolean.valueOf(true);
            }
        } else if (clazz == Integer.class) {
            if (value instanceof Boolean) {
                retValue = ((Boolean) value) ? Integer.valueOf(1) : Integer.valueOf(0);
            } else if (value instanceof String) {
                try {
                    retValue = Double.valueOf((String)value).intValue();
                } catch (NumberFormatException exception) {}
            } else if (value instanceof Integer) {
                retValue = value;
            } else if (value instanceof Double) {
                retValue = ((Double) value).intValue();
            }
        }
        return retValue;
    }
}
