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

import com.welie.blessed.BluetoothCommandStatus;
import com.welie.blessed.BluetoothGattCharacteristic;
import com.welie.blessed.BluetoothPeripheral;
import org.openremote.model.syslog.SyslogCategory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SendSingleDataSegmentCommand implements SendDataCommand {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, SendSingleDataSegmentCommand.class.getName());


    // Private Instance Fields ----------------------------------------------------------------

    private final BluetoothGattCharacteristic dataInCharacteristic;
    protected final BluetoothMeshProxySendDataCallback callback;
    private final byte[] data;
    protected final ScheduledExecutorService executorService;
    private final BluetoothMeshProxy meshProxy;
    private final MainThreadManager commandSerializer;
    private volatile boolean isWaitForCallback = false;
    private final BlockingQueue<BluetoothCommandStatus> resultQueue = new ArrayBlockingQueue<>(1);


    // Constructors ---------------------------------------------------------------------------

    public SendSingleDataSegmentCommand(BluetoothMeshProxy proxy, MainThreadManager commandSerializer, ScheduledExecutorService executorService, BluetoothGattCharacteristic characteristic, byte[] data, BluetoothMeshProxySendDataCallback callback) {
        this.meshProxy = proxy;
        this.commandSerializer = commandSerializer;
        this.executorService = executorService;
        this.dataInCharacteristic = characteristic;
        this.data = data;
        this.callback = callback;
    }


    // Public Instance Methods ----------------------------------------------------------------

    public boolean sendData() {
        boolean isSuccess = false;
        if (isWaitForCallback) {
            return false;
        }
        isWaitForCallback = true;

        LOG.info("Sending '" + data.length + "' bytes to mesh proxy: [data=" + dataAsHexString(data) + "]");

        Runnable runnable = () -> this.dataInCharacteristic.getService().getPeripheral().writeCharacteristic(
            this.dataInCharacteristic, data, BluetoothGattCharacteristic.WriteType.WITHOUT_RESPONSE
        );
        commandSerializer.enqueue(runnable);
        try {
            // TODO: timeout constant
            BluetoothCommandStatus status = resultQueue.poll(5000, TimeUnit.MILLISECONDS);
            if (status != null) {
                isSuccess = (status == BluetoothCommandStatus.COMMAND_SUCCESS);
                if (isSuccess) {
                    LOG.info("Successfully sent '" + data.length + "' bytes to mesh proxy: [data=" + dataAsHexString(data) + "]");
                } else {
                    LOG.warning("Failed to send '" + data.length + "' bytes to mesh proxy: [data=" + dataAsHexString(data) + ", status=" + status + "]");
                }
            } else {
                // Callback timeout
                LOG.severe("Failed to send '" + data.length + "' bytes to mesh proxy [data=" + dataAsHexString(data) + "] because of confirmation timeout");
                isSuccess = false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        if (callback != null) {
            final boolean callbackResult = isSuccess;
            executorService.execute(() -> callback.onDataSent(meshProxy, data, callbackResult));
        }
        return isSuccess;
    }


    // Implements BluetoothPeripheralCallback -------------------------------------------------

    @Override
    public void onCharacteristicWrite(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, BluetoothCommandStatus status) {
        if (dataInCharacteristic.getService().getPeripheral().getAddress().equals(peripheral.getAddress()) &&
            dataInCharacteristic.getService().getUuid().equals(characteristic.getService().getUuid()) &&
            dataInCharacteristic.getUuid().equals(characteristic.getUuid())) {
            if (resultQueue.isEmpty()) {
                try {
                    resultQueue.put(status);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }


    // Private Instance Methods -------------------------------------------------------------------

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