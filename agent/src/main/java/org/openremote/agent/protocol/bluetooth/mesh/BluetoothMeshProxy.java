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
import com.welie.blessed.BluetoothGattCharacteristic;
import com.welie.blessed.BluetoothGattDescriptor;
import com.welie.blessed.BluetoothGattService;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.ScanResult;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.syslog.SyslogCategory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class BluetoothMeshProxy extends BluetoothCentralManagerCallback {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, BluetoothMeshProxy.class.getName());

    /**
     * Mesh provisioning service UUID
     */
    public final static UUID MESH_PROXY_UUID = UUID.fromString("00001828-0000-1000-8000-00805F9B34FB");

    /**
     * Mesh provisioning data in characteristic UUID
     */
    public final static UUID MESH_PROXY_DATA_IN = UUID.fromString("00002ADD-0000-1000-8000-00805F9B34FB");

    /**
     * Mesh provisioning data out characteristic UUID
     */
    public final static UUID MESH_PROXY_DATA_OUT = UUID.fromString("00002ADE-0000-1000-8000-00805F9B34FB");


    // Private Instance Fields --------------------------------------------------------------------

    private final BluetoothPeripheralCallbackImpl peripheralCallback;
    private final ScanResult scanResult;
    private int rssi;
    private final BluetoothMeshProxyStateMachine stateMachine;


    // Constructors -------------------------------------------------------------------------------

    public BluetoothMeshProxy(MainThreadManager bluetoothCommandSerializer, ScheduledExecutorService executorService, BluetoothCentralManager central, BluetoothPeripheral peripheral, ScanResult scanResult) {
        this.scanResult = scanResult;
        this.rssi = scanResult.getRssi();
        this.peripheralCallback = new BluetoothPeripheralCallbackImpl();
        this.stateMachine = new BluetoothMeshProxyStateMachine(this, central, executorService, bluetoothCommandSerializer, peripheral, peripheralCallback);
    }


    // Implements BluetoothCentralManagerCallback -------------------------------------------------

    @Override
    public synchronized void onConnectedPeripheral(BluetoothPeripheral peripheral) {
        stateMachine.onConnectedPeripheral(peripheral);
    }

    @Override
    public synchronized void onConnectionFailed(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
        stateMachine.onConnectionFailed(peripheral, status);
    }

    @Override
    public synchronized void onDisconnectedPeripheral(BluetoothPeripheral peripheral, BluetoothCommandStatus status) {
        stateMachine.onDisconnectedPeripheral(peripheral, status);
    }

    @Override
    public synchronized void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
        stateMachine.onDiscoveredPeripheral(peripheral, scanResult);
    }

    @Override
    public synchronized void onScanFailed(int errorCode) {
    }


    // Public instance methods --------------------------------------------------------------------

    public synchronized void connect(Consumer<ConnectionStatus> statusConsumer, BluetoothMeshProxyConnectCallback callback) {
        stateMachine.connect(statusConsumer, callback);
    }

    public synchronized void disconnect() {
        stateMachine.disconnect();
    }

    public synchronized void sendData(int mtuSize, byte[] data, BluetoothMeshProxySendDataCallback callback) {
        stateMachine.sendData(mtuSize, data, callback);
    }

    public synchronized void setRxDataCallback(BluetoothMeshProxyRxCallback callback) {
        stateMachine.setRxDataCallback(callback);
    }

    public synchronized BluetoothPeripheral getPeripheral() {
        return stateMachine.getPeripheral();
    }

    public synchronized ScanResult getScanResult() {
        return scanResult;
    }

    public synchronized int getRssi() {
        return rssi;
    }

    public synchronized boolean isConnected() {
        return stateMachine.isConnected();
    }


    // Nested Classes -----------------------------------------------------------------------------

    private class BluetoothPeripheralCallbackImpl extends BluetoothPeripheralCallback {

        public void onServicesDiscovered(final BluetoothPeripheral peripheral, List<BluetoothGattService> services) {
            LOG.info("BluetoothPeripheralCallback::onServicesDiscovered: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + "]");
            stateMachine.onServicesDiscovered(peripheral, services);
        }

        public void onNotificationStateUpdate(BluetoothPeripheral peripheral, BluetoothGattCharacteristic characteristic, BluetoothCommandStatus status) {
            LOG.info("BluetoothPeripheralCallback::onNotificationStateUpdate: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + ", Status=" + status + "]");
            stateMachine.onNotificationStateUpdate(peripheral, characteristic, status);
        }

        public void onCharacteristicUpdate(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, BluetoothCommandStatus status) {
            LOG.info("BluetoothPeripheralCallback::onCharacteristicUpdate: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + ", Status=" + status + "]");
            stateMachine.onCharacteristicUpdate(peripheral, value, characteristic, status);
        }

        public void onCharacteristicWrite(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, BluetoothCommandStatus status) {
            LOG.info("BluetoothPeripheralCallback::onCharacteristicWrite: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + ", Status=" + status + "]");
            stateMachine.onCharacteristicWrite(peripheral, value, characteristic, status);
        }

        public void onDescriptorRead(BluetoothPeripheral peripheral, byte[] value, BluetoothGattDescriptor descriptor, BluetoothCommandStatus status) {
            LOG.info("BluetoothPeripheralCallback::onDescriptorRead: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + ", Status=" + status + "]");
        }

        public void onDescriptorWrite(BluetoothPeripheral peripheral, byte[] value, BluetoothGattDescriptor descriptor, BluetoothCommandStatus status) {
            LOG.info("BluetoothPeripheralCallback::onDescriptorWrite: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + ", Status=" + status + "]");
        }

        public void onBondingStarted(BluetoothPeripheral peripheral) {
            LOG.info("BluetoothPeripheralCallback::onBondingStarted: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + "]");
        }

        public void onBondingSucceeded(BluetoothPeripheral peripheral) {
            LOG.info("BluetoothPeripheralCallback::OnBondingSucceeded: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + "]");
        }

        public void onBondingFailed(BluetoothPeripheral peripheral) {
            LOG.info("BluetoothPeripheralCallback::onBondingFailed: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + "]");
        }

        public void onBondLost(BluetoothPeripheral peripheral) {
            LOG.info("BluetoothPeripheralCallback::onBondLost: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + "]");
        }

        public void onReadRemoteRssi(BluetoothPeripheral peripheral, int rssi, BluetoothCommandStatus status) {
            LOG.info("BluetoothPeripheralCallback::onReadRemoteRssi: [Name=" + peripheral.getName() + ", Address=" + peripheral.getAddress() + ", RSSI=" + rssi + ", Status=" + status + "]");
        }
    }
}
