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

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

@Entity
public class BluetoothMeshAgent extends Agent<BluetoothMeshAgent, BluetoothMeshProtocol, BluetoothMeshAgentLink> {

    public static final AttributeDescriptor<String> NETWORK_KEY = new AttributeDescriptor<String>("networkKey", ValueType.TEXT);
    public static final AttributeDescriptor<String> APPLICATION_KEY = new AttributeDescriptor<String>("applicationKey", ValueType.TEXT);
    public static final AttributeDescriptor<String> SOURCE_ADDRESS = new AttributeDescriptor<String>("sourceAddress", ValueType.TEXT);
    public static final AttributeDescriptor<Integer> SEQUENCE_NUMBER = new AttributeDescriptor<Integer>("sequenceNumber", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Integer> MTU = new AttributeDescriptor<Integer>("mtu", ValueType.POSITIVE_INTEGER);

    public static AgentDescriptor<BluetoothMeshAgent, BluetoothMeshProtocol, BluetoothMeshAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        BluetoothMeshAgent.class, BluetoothMeshProtocol.class, BluetoothMeshAgentLink.class, null
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected BluetoothMeshAgent() {

    }

    public BluetoothMeshAgent(String name) {
        super(name);

    }

    @Override
    public BluetoothMeshProtocol getProtocolInstance() {
        return new BluetoothMeshProtocol(this);
    }

    public Optional<String> getNetworkKey() {
        return getAttributes().getValue(NETWORK_KEY);
    }

    public BluetoothMeshAgent setNetworkKey(String value) {
        getAttributes().getOrCreate(NETWORK_KEY).setValue(value);
        return this;
    }

    public Optional<String> getApplicationKey() {
        return getAttributes().getValue(APPLICATION_KEY);
    }

    public BluetoothMeshAgent setApplicationKey(String value) {
        getAttributes().getOrCreate(APPLICATION_KEY).setValue(value);
        return this;
    }

    public Optional<String> getSourceAddress() {
        return getAttributes().getValue(SOURCE_ADDRESS);
    }

    public BluetoothMeshAgent setSourceAddress(String value) {
        getAttributes().getOrCreate(SOURCE_ADDRESS).setValue(value);
        return this;
    }

    public Optional<Integer> getSequenceNumber() {
        return getAttributes().getValue(SEQUENCE_NUMBER);
    }

    public BluetoothMeshAgent setSequenceNumber(Integer value) {
        getAttributes().getOrCreate(SEQUENCE_NUMBER).setValue(value);
        return this;
    }

    public Optional<Integer> getMtu() {
        return getAttributes().getValue(MTU);
    }

    public BluetoothMeshAgent setMtu(Integer value) {
        getAttributes().getOrCreate(MTU).setValue(value);
        return this;
    }
}
