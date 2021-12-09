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

import org.openremote.agent.protocol.bluetooth.mesh.transport.ControlMessage;
import org.openremote.agent.protocol.bluetooth.mesh.transport.MeshMessage;

/**
 * Callbacks to notify the status of the mesh messages
 */
public interface MeshStatusCallbacks {
    /**
     * Notifies if a transaction has failed
     * <p>
     * As of now this is only triggered if the incomplete timer has expired for a given segmented message.
     * The incomplete timer will wait for a minimum of 10 seconds on receiving a segmented message.
     * If all segments are not received during this period, that transaction shall be considered as failed.
     * </p>
     *
     * @param dst                       Unique dst address of the device
     * @param hasIncompleteTimerExpired Flag that notifies if the incomplete timer had expired
     */
    void onTransactionFailed(final int dst, final boolean hasIncompleteTimerExpired);

    /**
     * Notifies if an unknown pdu was received
     *
     * @param src           Address where the message originated from
     * @param accessPayload Access payload of the message
     */
    void onUnknownPduReceived(final int src, final byte[] accessPayload);

    /**
     * Notifies when a block acknowledgement has been processed
     *
     * <p>
     * This callback is invoked after {@link MeshManagerCallbacks#onMeshPduCreated(byte[])} where a mesh pdu is created.
     * </p>
     *
     * @param dst     Destination address to which the block ack was sent
     * @param message Control message containing the block acknowledgement
     */
    void onBlockAcknowledgementProcessed(final int dst, final ControlMessage message);

    /**
     * Notifies if a block acknowledgement was received
     *
     * @param src     Source address from which the block ack was received
     * @param message Control message containing the block acknowledgement
     */
    void onBlockAcknowledgementReceived(final int src, final ControlMessage message);

    /**
     * Callback to notify the mesh message has been processed to be sent to the bearer
     *
     * <p>
     * This callback is invoked after {@link MeshManagerCallbacks#onMeshPduCreated(byte[])} where
     * a mesh pdu is created and is ready to be sent.
     * </p>
     *
     * @param dst         Destination address to be sent
     * @param meshMessage {@link MeshMessage} containing the message that was sent
     */
    void onMeshMessageProcessed(final int dst, final MeshMessage meshMessage);

    /**
     * Callback to notify that a mesh status message was received from the bearer
     *
     * @param src         Source address where the message originated from
     * @param meshMessage {@link MeshMessage} containing the message that was received
     */
    void onMeshMessageReceived(final int src, final MeshMessage meshMessage);

    /**
     * Callback to notify if the decryption failed of a received mesh message
     *
     * @param meshLayer     Mesh layer name
     * @param errorMessage  Error message
     */
    void onMessageDecryptionFailed(final String meshLayer, final String errorMessage);
}
