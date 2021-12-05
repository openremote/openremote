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
import org.openremote.model.syslog.SyslogCategory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class BluetoothMeshProxyScanner extends BluetoothCentralManagerCallback {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, BluetoothMeshProxyScanner.class.getName());

    /**
     * Mesh provisioning service UUID
     */
    public final static UUID MESH_PROXY_UUID = UUID.fromString("00001828-0000-1000-8000-00805F9B34FB");

    private final static int ADVERTISED_NETWORK_ID_OFFSET = 1; //Offset of the network id contained in the advertisement service data
    private static final int ADVERTISEMENT_TYPE_NETWORK_ID = 0x00;
    private static final int ADVERTISEMENT_TYPE_NODE_IDENTITY = 0x01;
    private final static int ADVERTISED_NETWORK_ID_LENGTH = 8; //Length of the network id contained in the advertisement service data
    private final static int ADVERTISED_HASH_LENGTH = 8; // Length of the hash contained in the advertisement service data
    private final static int ADVERTISED_HASH_OFFSET = 1; // Offset of the hash contained in the advertisement service data

    private final MainThreadManager bluetoothCommandSerializer;
    private final BluetoothCentralManager bluetoothCentral;
    private final ScheduledExecutorService executorService;
    private volatile BluetoothMeshProxyScannerCallback callback;
    private volatile NetworkKey networkKey;
    private volatile boolean isStarted = false;

    private ScheduledFuture<?> timeoutFuture;

    private final Map<String, BluetoothMeshProxy> meshProxyMap = new HashMap<>();


    // Constructors -------------------------------------------------------------------------------

    public BluetoothMeshProxyScanner(MainThreadManager bluetoothCommandSerializer, BluetoothCentralManager central, ScheduledExecutorService executorService) {
        this.bluetoothCommandSerializer = bluetoothCommandSerializer;
        this.bluetoothCentral = central;
        this.executorService = executorService;
    }

    public synchronized void start(final NetworkKey networkKey, int duration, final BluetoothMeshProxyScannerCallback callback) {
        LOG.info("Starting mesh proxy scanner");
        if (isStarted) {
            stop();
        }
        this.callback = callback;
        this.networkKey = networkKey;
        meshProxyMap.clear();
        isStarted = true;
        Runnable runnable = () -> {
            LOG.info("Scan ON");
            bluetoothCentral.scanForPeripheralsWithServices(new UUID[] {MESH_PROXY_UUID});
        };
        bluetoothCommandSerializer.enqueue(runnable);
        timeoutFuture = executorService.schedule(new Runnable() {
            @Override
            public void run() {
                synchronized (BluetoothMeshProxyScanner.this) {
                    if (isStarted && callback != null) {
                        final List<BluetoothMeshProxy> proxies = new ArrayList<>(meshProxyMap.values());
                        executorService.schedule(new Runnable() {
                            @Override
                            public void run() {
                                callback.onMeshProxiesScanned(proxies, null);
                            }
                        }, 0, TimeUnit.MILLISECONDS);
                    }
                    timeoutFuture = null;
                    stop();
                }
            }
        }, duration, TimeUnit.MILLISECONDS);
    }


    // Public Instance Methods -----------------------------------------------------------------------------

    public synchronized void stop() {
        LOG.info("Stopping mesh proxy scanner");
        if (isStarted) {
            // bluetoothCentral.stopScan();
            Runnable runnable = () -> {
                LOG.info("Scan OFF");
                bluetoothCentral.stopScan();
            };
            bluetoothCommandSerializer.enqueue(runnable);
            meshProxyMap.clear();
            isStarted = false;
            if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
                timeoutFuture = null;
            }
        }
    }

    // Implements BluetoothCentralManagerCallback -------------------------------------------------

    @Override
    public synchronized void onConnectedPeripheral(final BluetoothPeripheral peripheral) {

    }

    @Override
    public synchronized void onConnectionFailed(final BluetoothPeripheral peripheral, final BluetoothCommandStatus status) {

    }

    @Override
    public synchronized void onDisconnectedPeripheral(final BluetoothPeripheral peripheral, final BluetoothCommandStatus status) {

    }

    @Override
    public synchronized void onDiscoveredPeripheral(final BluetoothPeripheral peripheral, final ScanResult scanResult) {
        if (!isStarted) {
            return;
        }
        LOG.info("Scanned Bluetooth mesh proxy: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + ", connectionState=" + peripheral.getState() + "]");
        if (!meshProxyMap.containsKey(peripheral.getAddress())) {
            byte[] serviceData = scanResult.getServiceData().get(MESH_PROXY_UUID.toString());
            if (serviceData != null) {
                if (isAdvertisingWithNetworkIdentity(serviceData)) {
                    LOG.info("Checking network identity of scanned Bluetooth mesh proxy: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + "]");
                    if (networkIdMatches(serviceData, networkKey)) {
                        LOG.info("Network identity of scanned Bluetooth mesh proxy matches: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + "]");
                        meshProxyMap.put(peripheral.getAddress(), new BluetoothMeshProxy(bluetoothCommandSerializer, executorService, bluetoothCentral, peripheral, scanResult));
                    } else {
                        LOG.info("Network identity of scanned Bluetooth mesh proxy does NOT match: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + "]");
                    }
                } else if (isAdvertisedWithNodeIdentity(serviceData)) {
                    LOG.info("Checking node identity of scanned Bluetooth mesh proxy: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + "]");
                    if (checkIfNodeIdentityMatches(serviceData)) {
                        LOG.info("Node identity of scanned Bluetooth mesh proxy matches: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + "]");
                        meshProxyMap.put((peripheral.getAddress()), new BluetoothMeshProxy(bluetoothCommandSerializer, executorService, bluetoothCentral, peripheral, scanResult));
                    } else {
                        LOG.info("Node identity of scanned Bluetooth mesh proxy does NOT match: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + "]");
                    }
                } else {
                    LOG.info("Could NOT find network or node identity in advertisement of Bluetooth mesh proxy: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + "]");
                }
            } else {
                LOG.warning("Could NOT find service data in advertisement of Bluetooth mesh proxy [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + "] for mesh proxy service UUID:" + MESH_PROXY_UUID.toString());
            }
        }
    }

    @Override
    public synchronized void onScanFailed(final int errorCode) {
        if (isStarted) {
            stop();
            executorService.execute(() -> {
                if (callback != null) {
                    callback.onMeshProxiesScanned(new ArrayList<>(), errorCode);
                }
            });
        }
    }

    // Private Instance Methods -------------------------------------------------------------------

    private boolean isAdvertisingWithNetworkIdentity(final byte[] serviceData) {
        return serviceData != null &&
               serviceData.length == 9 &&
               serviceData[ADVERTISED_NETWORK_ID_OFFSET - 1] == ADVERTISEMENT_TYPE_NETWORK_ID;
    }

    private boolean networkIdMatches(final byte[] serviceData, NetworkKey networkKey) {
        final byte[] advertisedNetworkId = getAdvertisedNetworkId(serviceData);
        if (advertisedNetworkId != null) {
            if (Arrays.equals(networkKey.getNetworkId(), advertisedNetworkId)) {
                return true;
            }
        }
        return false;
    }

    private byte[] getAdvertisedNetworkId(final byte[] serviceData) {
        if (serviceData == null)
            return null;
        final ByteBuffer advertisedNetworkID = ByteBuffer.allocate(ADVERTISED_NETWORK_ID_LENGTH).order(ByteOrder.BIG_ENDIAN);
        advertisedNetworkID.put(serviceData, ADVERTISED_NETWORK_ID_OFFSET, ADVERTISED_HASH_LENGTH);
        return advertisedNetworkID.array();
    }

    private boolean isAdvertisedWithNodeIdentity(final byte[] serviceData) {
        return serviceData != null &&
               serviceData.length == 17 &&
               serviceData[ADVERTISED_HASH_OFFSET - 1] == ADVERTISEMENT_TYPE_NODE_IDENTITY;
    }

    private boolean checkIfNodeIdentityMatches(final byte[] serviceData) {
        // TODO
        /*
        final MeshNetwork network = mMeshManagerApi.getMeshNetwork();
        if (network != null) {
            for (ProvisionedMeshNode node : network.getNodes()) {
                if (mMeshManagerApi.nodeIdentityMatches(node, serviceData)) {
                    return true;
                }
            }
        }
        return false;
         */
        return false;
    }

    /*
    public boolean nodeIdentityMatches(@NonNull final ProvisionedMeshNode meshNode, @NonNull final byte[] serviceData) {
        final byte[] advertisedHash = getAdvertisedHash(serviceData);
        //If there is no advertised hash return false as this is used to match against the generated hash
        if (advertisedHash == null) {
            return false;
        }

        //If there is no advertised random return false as this is used to generate the hash to match against the advertised
        final byte[] random = getAdvertisedRandom(serviceData);
        if (random == null) {
            return false;
        }

        for (NetworkKey key : mMeshNetwork.netKeys) {
            if (Arrays.equals(advertisedHash, SecureUtils.
                calculateHash(key.getIdentityKey(), random, MeshAddress.addressIntToBytes(meshNode.getUnicastAddress()))) ||
                Arrays.equals(advertisedHash, SecureUtils.
                    calculateHash(key.getOldIdentityKey(), random, MeshAddress.addressIntToBytes(meshNode.getUnicastAddress()))))
                return true;
        }
        return false;
    }
    */
}
