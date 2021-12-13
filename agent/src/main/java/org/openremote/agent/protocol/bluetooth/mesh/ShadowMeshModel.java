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

import org.openremote.agent.protocol.bluetooth.mesh.transport.MeshMessage;
import org.openremote.model.syslog.SyslogCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class ShadowMeshModel {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ShadowMeshModel.class.getName());


    protected final ShadowMeshElement element;
    protected final BluetoothMeshNetwork meshNetwork;
    protected int appKeyIndex;
    private final List<Consumer<Object>> sensorValueConsumers = new ArrayList<>();
    protected final ScheduledExecutorService executorService;

    public ShadowMeshModel(ScheduledExecutorService executorService, BluetoothMeshNetwork network, ShadowMeshElement element, int appKeyIndex) {
        this.executorService = executorService;
        this.meshNetwork = network;
        this.element = element;
        this.appKeyIndex = appKeyIndex;
    }

    public synchronized void addSensorValueConsumer(Consumer<Object> consumer) {
        if (consumer != null && !sensorValueConsumers.contains(consumer)) {
            sensorValueConsumers.add(consumer);
        }
    }

    public synchronized void removeSensorValueConsumer(Consumer<Object> consumer) {
        if (consumer != null && sensorValueConsumers.contains(consumer)) {
            sensorValueConsumers.remove(consumer);
        }
    }

    public synchronized void setAppKeyIndex(int appKeyIndex) {
        this.appKeyIndex = appKeyIndex;
    }

    public synchronized void sendSetCommand(Object value) {
        ApplicationKey key = meshNetwork.getApplicationKey(appKeyIndex);
        if (key != null) {
            MeshMessage meshMessage = createSetMeshMessage(key, value);
            if (meshMessage != null) {
                meshNetwork.getMeshManagerApi().createMeshPdu(element.getAddress(), meshMessage);
            }
        } else {
            LOG.severe(
                "Failed to send mesh model '" + getModelName() + "' command value '" + value +
                    "' because couldn't find application key for index '" + appKeyIndex + "', address: '" + element.getAddress()
            );
        }
    }

    public synchronized void sendGetCommand() {
        ApplicationKey key = meshNetwork.getApplicationKey(appKeyIndex);
        if (key != null) {
            MeshMessage meshMessage = createGetMeshMessage(key);
            if (meshMessage != null) {
                meshNetwork.getMeshManagerApi().createMeshPdu(element.getAddress(), meshMessage);
            } else {
                LOG.severe(
                    "Failed to send mesh model '" + getModelName() + "' get status command " +
                        "because couldn't find application key for index '" + appKeyIndex + "'"
                );
            }
        }
    }

    protected void executeSensorValueConsumers(final Object value) {
        executorService.execute(() -> {
            synchronized (ShadowMeshModel.this) {
                for (Consumer<Object> consumer : sensorValueConsumers) {
                    consumer.accept(value);
                }
            }
        });
    }

    public abstract void onMeshMessageReceived(MeshMessage meshMessage);

    protected abstract MeshMessage createSetMeshMessage(ApplicationKey applicationKey, Object value);

    protected abstract MeshMessage createGetMeshMessage(ApplicationKey applicationKey);

    protected abstract String getModelName();

    protected abstract int getModelId();
}
