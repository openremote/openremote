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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

public class SendMultiDataSegmentCommand implements SendDataCommand {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, SendMultiDataSegmentCommand.class.getName());


    // Private Instance Fields ----------------------------------------------------------------

    private final BluetoothMeshProxy meshProxy;
    private final MainThreadManager commandSerializer;
    private final BluetoothGattCharacteristic dataInCharacteristic;
    private final BluetoothMeshProxySendDataCallback callback;
    private final byte[] data;
    private final ScheduledExecutorService executorService;
    private final List<SendSingleDataSegmentCommand> commands;

    private volatile SendSingleDataSegmentCommand currentCommand;

    // Constructors ---------------------------------------------------------------------------

    public SendMultiDataSegmentCommand(BluetoothMeshProxy proxy, MainThreadManager commandSerializer, int mtuSize, ScheduledExecutorService executorService, BluetoothGattCharacteristic characteristic, byte[] data, BluetoothMeshProxySendDataCallback callback) {
        this.meshProxy = proxy;
        this.commandSerializer = commandSerializer;
        this.dataInCharacteristic = characteristic;
        this.callback = callback;
        this.executorService = executorService;
        this.data = data;
        this.commands = createCommands(mtuSize, executorService, characteristic, data, callback);
    }


    // Public Instance Methods ----------------------------------------------------------------

    public boolean sendData() {
        boolean isSuccess = true;
        for (SendSingleDataSegmentCommand curCommand : commands) {
            setCurrentCommand(curCommand);
            // Note: sendData is blocking
            if (!curCommand.sendData()) {
                isSuccess = false;
                break;
            }
        }
        if (callback != null) {
            final boolean callbackResult = isSuccess;
            executorService.execute(() -> callback.onDataSent(meshProxy, data, callbackResult));
        }

        return isSuccess;
    }


    // Implements BluetoothPeripheralCallback -------------------------------------------------

    @Override
    public synchronized void onCharacteristicWrite(BluetoothPeripheral peripheral, byte[] value, BluetoothGattCharacteristic characteristic, BluetoothCommandStatus status) {
        if (currentCommand != null) {
            currentCommand.onCharacteristicWrite(peripheral, value, characteristic, status);
        }
    }


    // Private Instance Methods ---------------------------------------------------------------

    private List<SendSingleDataSegmentCommand> createCommands(int mtuSize, ScheduledExecutorService executorService, BluetoothGattCharacteristic characteristic, byte[] data, BluetoothMeshProxySendDataCallback callback) {
        int numOfSegments = (data.length / mtuSize) + ((data.length % mtuSize) > 0 ? 1 : 0);
        List<SendSingleDataSegmentCommand> commandList = new ArrayList<>(numOfSegments);

        for (int i = 0; i < numOfSegments; i++) {
            int length = Math.min(data.length - mtuSize * i, mtuSize);
            byte[] segmentData = new byte[length];
            System.arraycopy(data, mtuSize * i, segmentData, 0, length);
            SendSingleDataSegmentCommand cmd = new SendSingleDataSegmentCommand(meshProxy, commandSerializer, executorService, characteristic, segmentData, /* callback */null);
            commandList.add(cmd);
        }

        return commandList;
    }

    private synchronized void setCurrentCommand(SendSingleDataSegmentCommand command) {
        this.currentCommand = command;
    }
}