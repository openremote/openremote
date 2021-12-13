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

public interface MeshMngrApi {
    /**
     * Sets the {@link MeshManagerCallbacks} listener
     *
     * @param callbacks callbacks
     */
    void setMeshManagerCallbacks(final MeshManagerCallbacks callbacks);

    /**
     * Sets the {@link MeshManagerCallbacks} listener to return mesh status callbacks.
     *
     * @param callbacks callbacks
     */
    void setMeshStatusCallbacks(final MeshStatusCallbacks callbacks);

    /**
     * Handles notifications received by the client.
     * <p>
     * This method will check if the library should wait for more data in case of a gatt layer segmentation.
     * If its required the method will remove the segmentation bytes and reassemble the pdu together.
     * </p>
     *
     * @param mtuSize GATT MTU size
     * @param data    PDU received by the client
     */
    void handleNotifications(final int mtuSize, final byte[] data);

    /**
     * Must be called to handle provisioning states
     *
     * @param mtuSize GATT MTU size
     * @param data    PDU received by the client
     */
    void handleWriteCallbacks(final int mtuSize, final byte[] data);

    /**
     * Sends the specified  mesh message specified within the {@link MeshMessage} object
     *
     * @param dst         destination address
     * @param meshMessage {@link MeshMessage} Mesh message containing the message opcode and message parameters
     */
    void createMeshPdu(final int dst, final MeshMessage meshMessage) throws IllegalArgumentException;
}
