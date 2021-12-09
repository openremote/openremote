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

import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothCommandStatus;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.ScanResult;
import org.openremote.agent.protocol.bluetooth.mesh.models.SigModel;
import org.openremote.agent.protocol.bluetooth.mesh.models.SigModelParser;
import org.openremote.agent.protocol.bluetooth.mesh.provisionerstates.UnprovisionedMeshNode;
import org.openremote.agent.protocol.bluetooth.mesh.transport.ControlMessage;
import org.openremote.agent.protocol.bluetooth.mesh.transport.Element;
import org.openremote.agent.protocol.bluetooth.mesh.transport.MeshMessage;
import org.openremote.agent.protocol.bluetooth.mesh.transport.MeshModel;
import org.openremote.agent.protocol.bluetooth.mesh.transport.ProvisionedMeshNode;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.syslog.SyslogCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class BluetoothMeshNetwork extends BluetoothCentralManagerCallback implements MeshManagerCallbacks, MeshStatusCallbacks, BluetoothMeshProxyRxCallback{

    // Class Members ------------------------------------------------------------------------------

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, BluetoothMeshNetwork.class.getName());


    // Constants ----------------------------------------------------------------------------------

    public static final int SCAN_DURATION = 10000;


    // Private Instance Fields --------------------------------------------------------------------

    private volatile BluetoothMeshProxyScanner proxyScanner;
    private volatile BluetoothMeshProxy bluetoothMeshProxy;
    private volatile MeshManagerApi meshManagerApi;
    private volatile boolean isConnected = false;

    private final ScheduledExecutorService executorService;
    private final BluetoothCentralManager bluetoothCentral;
    private final SequenceNumberPersistencyManager sequenceNumberManager;
    private final MainThreadManager mainThreadManager;
    private final Consumer<ConnectionStatus> statusConsumer;
    private final NetworkKey networkKey;
    private final int sourceUnicastAddress;
    private final Map<Integer, ApplicationKey> applicationKeyMap;
    private final Integer mtu;
    private final Map<Integer, ShadowMeshElement> elementMap = new HashMap<>();


    // Constructors -------------------------------------------------------------------------------

    public BluetoothMeshNetwork(BluetoothCentralManager bluetoothCentral, SequenceNumberPersistencyManager sequenceNumberManager, MainThreadManager mainThread,
                                int unicastAddress, NetworkKey networkKey, Map<Integer, ApplicationKey> applicationKeyMap,
                                int mtu, int sequenceNumber, ScheduledExecutorService executorService, Consumer<ConnectionStatus> statusConsumer) {
        this.bluetoothCentral = bluetoothCentral;
        this.sequenceNumberManager = sequenceNumberManager;
        this.mainThreadManager = mainThread;
        this.networkKey = networkKey;
        this.sourceUnicastAddress = unicastAddress;
        this.applicationKeyMap = applicationKeyMap;
        this.mtu = mtu;
        this.executorService = executorService;
        this.statusConsumer = statusConsumer;

        this.meshManagerApi = new MeshManagerApi(executorService);
        this.meshManagerApi.setMeshManagerCallbacks(this);
        this.meshManagerApi.setMeshStatusCallbacks(this);

        this.meshManagerApi.resetMeshNetwork(unicastAddress);
        MeshNetwork network = this.meshManagerApi.getMeshNetwork();
        network.addNetKey(networkKey);
        for (Map.Entry<Integer, ApplicationKey> keyEntry : applicationKeyMap.entrySet()) {
            network.addAppKey(keyEntry.getValue());
        }
        int provisionerAddress = getMeshNetwork().getSelectedProvisioner().getProvisionerAddress();
        ProvisionedMeshNode provisionerNode = getMeshNetwork().getNode(provisionerAddress);
        provisionerNode.setSequenceNumber(sequenceNumber);
    }


    // Implements BluetoothCentralManagerCallback -------------------------------------------------

    @Override
    public synchronized void onConnectedPeripheral(BluetoothPeripheral peripheral) {
        if (proxyScanner != null) {
            proxyScanner.onConnectedPeripheral(peripheral);
        }
        if (bluetoothMeshProxy != null) {
            bluetoothMeshProxy.onConnectedPeripheral(peripheral);
        }
    }

    @Override
    public synchronized void onConnectionFailed(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
        if (proxyScanner != null) {
            proxyScanner.onConnectionFailed(peripheral, status);
        }
        if (bluetoothMeshProxy != null) {
            bluetoothMeshProxy.onConnectionFailed(peripheral, status);
        }
    }

    @Override
    public synchronized void onDisconnectedPeripheral(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
        if (proxyScanner != null) {
            proxyScanner.onDisconnectedPeripheral(peripheral, status);
        }
        if (bluetoothMeshProxy != null) {
            bluetoothMeshProxy.onDisconnectedPeripheral(peripheral, status);
        }
    }

    @Override
    public synchronized void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
        if (proxyScanner != null) {
            proxyScanner.onDiscoveredPeripheral(peripheral, scanResult);
        }
        if (bluetoothMeshProxy != null) {
            bluetoothMeshProxy.onDiscoveredPeripheral(peripheral, scanResult);
        }
    }

    @Override
    public synchronized void onScanFailed(int errorCode) {
        if (proxyScanner != null) {
            proxyScanner.onScanFailed(errorCode);
        }
        if (bluetoothMeshProxy != null) {
            bluetoothMeshProxy.onScanFailed(errorCode);
        }
    }


    // Implements MeshManagerCallbacks ------------------------------------------------------------

    @Override
    public void onNetworkLoaded(MeshNetwork meshNetwork) {

    }

    @Override
    public void onNetworkUpdated(MeshNetwork meshNetwork) {

    }

    @Override
    public void onNetworkLoadFailed(String error) {

    }

    @Override
    public void onNetworkImported(MeshNetwork meshNetwork) {

    }

    @Override
    public void onNetworkImportFailed(String error) {

    }

    @Override
    public void sendProvisioningPdu(UnprovisionedMeshNode meshNode, byte[] pdu) {

    }

    @Override
    public synchronized void onMeshPduCreated(byte[] pdu) {
        LOG.info("Sending PDU to mesh proxy: [data:" + dataAsHexString(pdu) + "]");
        if (bluetoothMeshProxy != null) {
            bluetoothMeshProxy.sendData(getMtu(), pdu, (proxy, data, isSuccess) -> {
                if (isSuccess) {
                    LOG.info("Succeeded to send PDU to mesh proxy: [data:" + dataAsHexString(pdu) + "]");
                    meshManagerApi.handleWriteCallbacks(getMtu(), data);
                } else {
                    LOG.warning("Failed to send PDU to mesh proxy: [data:" + dataAsHexString(pdu) + "]");
                }
            });
            Integer sourceAddress = getMeshNetwork().getSelectedProvisioner().getProvisionerAddress();
            if (sourceAddress != null) {
                ProvisionedMeshNode sourceNode = getMeshNetwork().getNode(sourceAddress);
                if (sourceNode != null) {
                    final int newSequenceNumber = sourceNode.getSequenceNumber();
                    executorService.execute(() -> sequenceNumberManager.save(networkKey, sourceUnicastAddress, newSequenceNumber));
                }
            }
        }
    }

    @Override
    public int getMtu() {
        return mtu;
    }


    // Implements BluetoothMeshProxyRxCallback ----------------------------------------------------

    @Override
    public synchronized void onRxData(byte[] data) {
        if (meshManagerApi != null) {
            meshManagerApi.handleNotifications(getMtu(), data);
        }
    }


    // Implements MeshStatusCallbacks -------------------------------------------------------------

    @Override
    public void onTransactionFailed(int dst, boolean hasIncompleteTimerExpired) {

    }

    @Override
    public void onUnknownPduReceived(int src, byte[] accessPayload) {

    }

    @Override
    public void onBlockAcknowledgementProcessed(int dst, ControlMessage message) {

    }

    @Override
    public void onBlockAcknowledgementReceived(int src, ControlMessage message) {

    }

    @Override
    public void onMeshMessageProcessed(int dst, MeshMessage meshMessage) {

    }

    @Override
    public synchronized void onMeshMessageReceived(int src, MeshMessage meshMessage) {
        LOG.info("Received mesh message: address='" + String.format("0x%04X", src) + "', message='" + meshMessage.getClass().getName() + "'");
        ShadowMeshElement element = elementMap.get(src);
        if (element != null) {
            element.onMeshMessageReceived(meshMessage);
        } else {
            LOG.info(
                "Could not find element to process the received mesh message: address='" + String.format("0x%04X", src) +
                    "', message='" + meshMessage.getClass().getName() + "'"
            );
        }
    }

    @Override
    public void onMessageDecryptionFailed(String meshLayer, String errorMessage) {
        LOG.warning("Failed to decrypt Bluetooth mesh message: [meshLayer: '" + meshLayer+ "', errorMessage: '" + errorMessage + "']");
    }


    // Public Instance Methods --------------------------------------------------------------------

    public synchronized void connect() {
        if (isConnected) {
            return;
        }
        isConnected = true;
        bluetoothMeshProxy = null;
        proxyScanner = new BluetoothMeshProxyScanner(mainThreadManager, bluetoothCentral, executorService);
        proxyScanner.start(networkKey, SCAN_DURATION, new BluetoothMeshProxyScannerCallback() {
            @Override
            public void onMeshProxiesScanned(List<BluetoothMeshProxy> meshProxies, Integer errorCode) {
                LOG.info("Finished scanning Bluetooth mesh proxies.");
                if (errorCode == null) {
                    for (BluetoothMeshProxy curMeshProxy : meshProxies) {
                        LOG.info("Scan found Bluetooth mesh proxy: [Name=" + curMeshProxy.getPeripheral().getName() + ", Address=" + curMeshProxy.getPeripheral().getAddress() + ", Rssi=" + curMeshProxy.getRssi() + "]");
                    }
                    if (meshProxies.size() > 0) {
                        synchronized (BluetoothMeshNetwork.this) {
                            // TODO: consider rssi and choose proxy in close proximity
                            BluetoothMeshNetwork.this.bluetoothMeshProxy = meshProxies.get(0);
                            BluetoothMeshNetwork.this.bluetoothMeshProxy.setRxDataCallback(BluetoothMeshNetwork.this);
                            BluetoothMeshNetwork.this.bluetoothMeshProxy.connect(statusConsumer, new BluetoothMeshProxyConnectCallback() {
                                @Override
                                public void onMeshProxyConnected(BluetoothPeripheral peripheral, boolean isSuccess) {
                                    synchronized (BluetoothMeshNetwork.this) {
                                        if (isSuccess) {
                                            LOG.info("Successfully connected to Bluetooth mesh proxy: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + "]");
                                        } else {
                                            LOG.warning("Failed to connect to Bluetooth mesh proxy: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + "]");
                                            BluetoothMeshNetwork.this.bluetoothMeshProxy.disconnect();
                                            BluetoothMeshNetwork.this.bluetoothMeshProxy = null;
                                        }
                                    }
                                }
                            });
                        }
                    } else {
                        LOG.info("No Bluetooth mesh proxy found!");
                        executorService.execute(() -> statusConsumer.accept(ConnectionStatus.ERROR));
                    }
                } else {
                    LOG.info("Failed to scan Bluetooth mesh proxies: [error code=" + errorCode + "]");
                    executorService.execute(() -> statusConsumer.accept(ConnectionStatus.ERROR));
                }
            }
        });
    }

    public synchronized void disconnect() {
        isConnected = false;
        if (proxyScanner != null) {
            proxyScanner.stop();
            proxyScanner = null;
        }
        if (bluetoothMeshProxy != null) {
            bluetoothMeshProxy.disconnect();
            bluetoothMeshProxy = null;
        }
        bluetoothMeshProxy = null;
    }

    public synchronized boolean isConnected() {
        boolean isConnected = false;
        if (bluetoothMeshProxy != null) {
            isConnected = bluetoothMeshProxy.isConnected();
        }
        return isConnected;
    }

    public synchronized MeshManagerApi getMeshManagerApi() {
        return meshManagerApi;
    }

    public ApplicationKey getApplicationKey(int keyIndex) {
        return applicationKeyMap.get(keyIndex);
    }

    public NetworkKey getNetworkKey() {
        return networkKey;
    }

    public synchronized MeshNetwork getMeshNetwork() {
        MeshNetwork network = null;
        MeshManagerApi api = getMeshManagerApi();
        if (api != null) {
            network = api.getMeshNetwork();
        }
        return network;
    }

    public synchronized ProvisionedMeshNode getNode(int sourceAddress) {
        ProvisionedMeshNode node = null;
        MeshNetwork network = getMeshNetwork();
        if (network != null) {
            node = network.getNode(sourceAddress);
        }
        return node;
    }

    public synchronized void addMeshModel(int address, int modelId, int appKeyIndex) {
        MeshNetwork network = getMeshNetwork();
        MeshModel model = SigModelParser.getSigModel(modelId);
        if (model == null) {
            LOG.severe("Bluetooth Mesh model with ID='" + String.format("0x%04X", modelId) + "' is not supported.");
            return;
        }
        // TODO: proper handling of unicast and group addresses
        if (address < 0xC000) {
            ProvisionedMeshNode node = getNode(address);
            if (node == null) {
                List<NetworkKey> networkKeys = new ArrayList<>(1);
                networkKeys.add(networkKey);
                List<ApplicationKey> applicationKeys = new ArrayList<>(applicationKeyMap.size());
                applicationKeys.addAll(applicationKeyMap.values());
                node = new ProvisionedMeshNode(network.getSelectedProvisioner(), networkKeys, applicationKeys);
                node.setUnicastAddress(address);
            }
            Map<Integer, Element> oldElementMap = node.getElements();
            Map<Integer, Element> newElementMap = new HashMap<>();
            Map<Integer, MeshModel> newModelMap = new HashMap<>();
            Element oldElement = oldElementMap.get(address);
            if (oldElement != null) {
                newModelMap.putAll(oldElement.getMeshModels());
            }
            int locationDescriptor = 0;
            model.setBoundAppKeyIndex(appKeyIndex);
            newModelMap.put(modelId, model);
            Element newElement = new Element(address, locationDescriptor, newModelMap);
            newElementMap.putAll(oldElementMap);
            newElementMap.put(address, newElement);
            node.setElements(newElementMap);
            if (getNode(address) == null) {
                network.nodes.add(node);
                network.sequenceNumbers.put(node.getUnicastAddress(), node.getSequenceNumber());
                network.unicastAddress = network.nextAvailableUnicastAddress(node.getNumberOfElements(), network.getSelectedProvisioner());
                node.setMeshUuid(network.getMeshUUID());
                network.loadSequenceNumbers();
            }
        }
        addShadowModel(address, modelId, appKeyIndex);
    }

    public synchronized void addSensorValueConsumer(int address, int modelId, Consumer<Object> consumer) {
        ShadowMeshModel model = searchShadowModel(address, modelId);
        if (model != null) {
            model.addSensorValueConsumer(consumer);
        }
    }

    public synchronized void removeSensorValueConsumer(int address, int modelId, Consumer<Object> consumer) {
        ShadowMeshModel model = searchShadowModel(address, modelId);
        if (model != null) {
            model.removeSensorValueConsumer(consumer);
        }
    }

    public synchronized void sendMeshSetCommand(int address, int modelId, Object value) {
        if (isConnected()) {
            ShadowMeshModel model = searchShadowModel(address, modelId);
            if (model != null) {
                model.sendSetCommand(value);
            } else {
                SigModel sigModel = SigModelParser.getSigModel(modelId);
                String modelName = sigModel != null ? sigModel.getModelName() : String.format("0x%04X", modelId);
                LOG.warning(
                    "Failed to send mesh command value '" + value + "' because couldn't find mesh model: address:'" +
                        String.format("0x%04X", address) + "', model: '" + modelName + '"'
                );
            }
        }
    }

    public synchronized void sendMeshGetCommand(int address, int modelId) {
        if (isConnected()) {
            ShadowMeshModel model = searchShadowModel(address, modelId);
            if (model != null) {
                model.sendGetCommand();
            } else {
                SigModel sigModel = SigModelParser.getSigModel(modelId);
                String modelName = sigModel != null ? sigModel.getModelName() : String.format("0x%04X", modelId);
                LOG.warning(
                    "Failed to send mesh get status command because couldn't find mesh model: address:'" +
                        String.format("0x%04X", address) + "', model: '" + modelName + '"'
                );
            }
        }
    }

    public synchronized void sendMeshGetCommands(int address) {
        if (isConnected()) {
            ShadowMeshElement element = elementMap.get(address);
            if (element != null) {
                List<ShadowMeshModel> models = element.getMeshModels();
                for (ShadowMeshModel curModel : models) {
                    sendMeshGetCommand(address, curModel.getModelId());
                }
            }
        }
    }

    public synchronized void sendMeshGetCommands() {
        if (isConnected()) {
            for (Integer address : elementMap.keySet()) {
                sendMeshGetCommands(address);
            }
        }
    }


    // Private Instance Methods -------------------------------------------------------------------

    private ShadowMeshModel searchShadowModel(int address, int modelId) {
        ShadowMeshModel model = null;
        ShadowMeshElement element = elementMap.get(address);
        if (element != null) {
            model = element.searchShadowModel(modelId);
        }
        return model;
    }

    private void addShadowModel(int address, int modelId, int appKeyIndex) {
        if (!elementMap.containsKey(address)) {
            elementMap.put(address, new ShadowMeshElement(executorService,this, address));
        }
        ShadowMeshElement element = elementMap.get(address);
        element.addShadowModel(modelId, appKeyIndex);
    }

    private String dataAsHexString(byte[] data) {
        if (data == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            builder.append(String.format("0x%02X%s", data[i] & 0xFF, i == (data.length - 1) ? "" : ", "));
        }
        return builder.toString();
    }
}
