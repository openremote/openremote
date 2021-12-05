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

import org.openremote.agent.protocol.bluetooth.mesh.models.SigModelParser;
import org.openremote.agent.protocol.bluetooth.mesh.transport.MeshMessage;
import org.openremote.model.syslog.SyslogCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

public class ShadowMeshElement {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ShadowMeshElement.class.getName());

    private final ScheduledExecutorService executorService;
    private final BluetoothMeshNetwork meshNetwork;
    private final int address;
    private final Map<Integer, ShadowMeshModel> modelMap = new HashMap<>();

    public ShadowMeshElement(ScheduledExecutorService executorService, BluetoothMeshNetwork network, int address) {
        this.executorService = executorService;
        this.meshNetwork = network;
        this.address = address;
    }

    public synchronized void addShadowModel(int modelId, int appKeyIndex) {
        ShadowMeshModel model = modelMap.get(modelId);
        if (model != null) {
            model.setAppKeyIndex(appKeyIndex);
        } else {
            model = createShadowModel(modelId, appKeyIndex);
            if (model != null) {
                modelMap.put(modelId, model);
            }
        }
    }

    public synchronized void onMeshMessageReceived(MeshMessage meshMessage) {
        for (ShadowMeshModel model : modelMap.values()) {
            model.onMeshMessageReceived(meshMessage);
        }
    }

    public synchronized int getAddress() {
        return address;
    }

    public synchronized List<ShadowMeshModel> getMeshModels() {
        return new ArrayList<>(modelMap.values());
    }

    public synchronized ShadowMeshModel searchShadowModel(int modelId) {
        return modelMap.get(modelId);
    }

    private ShadowMeshModel createShadowModel(int modelId, int appKeyIndex) {
        ShadowMeshModel model = null;
        if ((SigModelParser.GENERIC_ON_OFF_SERVER & 0xFFFF) == modelId) {
            model = new ShadowGenericOnOffModel(executorService, meshNetwork, this, appKeyIndex);
        }
        return model;
    }
}
