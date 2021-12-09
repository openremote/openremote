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
package org.openremote.agent.protocol.bluetooth.mesh.provisionerstates;

public class ProvisioningCompleteState extends ProvisioningState {

    private final UnprovisionedMeshNode unprovisionedMeshNode;

    public ProvisioningCompleteState(final UnprovisionedMeshNode unprovisionedMeshNode) {
        super();
        this.unprovisionedMeshNode = unprovisionedMeshNode;
        unprovisionedMeshNode.setIsProvisioned(true);
        unprovisionedMeshNode.setProvisionedTime(System.currentTimeMillis());
    }

    @Override
    public State getState() {
        return State.PROVISIONING_COMPLETE;
    }

    @Override
    public void executeSend() {

    }

    @Override
    public boolean parseData(final byte[] data) {
        return true;
    }

}

