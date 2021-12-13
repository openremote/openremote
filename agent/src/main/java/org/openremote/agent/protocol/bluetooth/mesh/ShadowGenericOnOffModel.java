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
import org.openremote.agent.protocol.bluetooth.mesh.transport.GenericOnOffGet;
import org.openremote.agent.protocol.bluetooth.mesh.transport.GenericOnOffSetUnacknowledged;
import org.openremote.agent.protocol.bluetooth.mesh.transport.GenericOnOffStatus;
import org.openremote.agent.protocol.bluetooth.mesh.transport.MeshMessage;
import org.openremote.model.syslog.SyslogCategory;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.bluetooth.mesh.opcodes.ApplicationMessageOpCodes.GENERIC_ON_OFF_STATUS;

public class ShadowGenericOnOffModel extends ShadowMeshModel {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ShadowGenericOnOffModel.class.getName());

    public ShadowGenericOnOffModel(ScheduledExecutorService executorService, BluetoothMeshNetwork network, ShadowMeshElement element, int appKeyIndex) {
        super(executorService, network, element, appKeyIndex);
    }

    @Override
    public synchronized void onMeshMessageReceived(MeshMessage meshMessage) {
        if (meshMessage.getOpCode() == GENERIC_ON_OFF_STATUS && meshMessage instanceof GenericOnOffStatus) {
            final GenericOnOffStatus status = (GenericOnOffStatus) meshMessage;
            Boolean isOn = status.getPresentState();
            LOG.info("Received model status update: [model=GenericOnOffServer, address=" + element.getAddress() + ", state=" + (isOn ? "ON":"OFF") + "]");
            executeSensorValueConsumers(isOn);
        }
    }

    @Override
    protected MeshMessage createSetMeshMessage(ApplicationKey applicationKey, Object value) {
        MeshMessage meshMessage = null;
        int tid = new Random().nextInt();
        Boolean onOffValue = toBoolean(value);
        if (onOffValue != null) {
            meshMessage = new GenericOnOffSetUnacknowledged(applicationKey, onOffValue, tid);
        }
        return meshMessage;
    }

    @Override
    protected MeshMessage createGetMeshMessage(ApplicationKey applicationKey) {
        return new GenericOnOffGet(applicationKey);
    }

    @Override
    protected String getModelName() {
        return "Generic On Off";
    }

    @Override
    protected int getModelId() {
        return SigModelParser.GENERIC_ON_OFF_SERVER & 0xFFFF;
    }

    private Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        Boolean onOffValue = null;
        if (value instanceof Boolean) {
            onOffValue = (Boolean)value;
        } else if (value instanceof Integer) {
            onOffValue = ((Integer)value) != 0;
        } else if (value instanceof Double) {
            onOffValue = ((Double)value) != 0;
        } else if (value instanceof String) {
            onOffValue = fromStringToBoolean((String) value);
        }
        return onOffValue;
    }

    private Boolean fromStringToBoolean(String stringValue) {
        if (stringValue == null) {
            return null;
        }
        Boolean boolValue = null;
        stringValue = stringValue.trim();
        if (stringValue.equalsIgnoreCase("false") ||
            stringValue.equalsIgnoreCase("off")   ||
            stringValue.equalsIgnoreCase("0")) {
            boolValue = false;
        } else if (stringValue.equalsIgnoreCase("true") ||
                   stringValue.equalsIgnoreCase("on")   ||
                   stringValue.equalsIgnoreCase("1")) {
            boolValue = true;
        }
        return boolValue;
    }
}
