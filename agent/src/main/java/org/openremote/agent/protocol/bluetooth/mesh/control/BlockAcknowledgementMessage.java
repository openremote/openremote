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
package org.openremote.agent.protocol.bluetooth.mesh.control;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.logging.Logger;

public class BlockAcknowledgementMessage extends TransportControlMessage {

    public static final Logger LOG = Logger.getLogger(BlockAcknowledgementMessage.class.getName());

    public BlockAcknowledgementMessage(final byte[] acknowledgementPayload) {

    }

    /**
     * Calculates the block acknowledgement payload.
     * <p>
     * This method will set the segO bit to 1
     * </p>
     *
     * @param blockAck block acknowledgement payload to be sent
     * @param segO     segment index
     */
    public static Integer calculateBlockAcknowledgement(final Integer blockAck, final int segO) {
        int ack = 0;
        if (blockAck == null) {
            ack |= 1 << segO;
            LOG.info("Block ack value: " + Integer.toString(ack, 16));
            return ack;
        } else {
            ack = blockAck;
            ack |= 1 << segO;
            LOG.info("Block ack value: " + Integer.toString(ack, 16));
            return ack;
        }
    }

    /**
     * Calculates the block acknowledgement payload.
     * <p>
     * This method will set the segO bit to 1
     * </p>
     *
     * @param segN number of segments
     */
    public static int calculateBlockAcknowledgement(final int segN) {
        final int segmentCount = segN + 1;
        int ack = 0;
        for (int i = 0; i < segmentCount; i++) {
            ack |= 1 << i;
        }
        return ack;
    }

    @Override
    public TransportControlMessageState getState() {
        return TransportControlMessageState.LOWER_TRANSPORT_BLOCK_ACKNOWLEDGEMENT;
    }

    /**
     * Parses the block acknowledgement payload
     * <p>
     * This method will iterate though the block acknowledgement to find out which segments needs to be retransmitted.
     * </p>
     *
     * @param blockAcknowledgement acknowledgement payload received
     * @param segmentCount         number of segments
     */
    public static ArrayList<Integer> getSegmentsToBeRetransmitted(final byte[] blockAcknowledgement, final int segmentCount) {
        final ArrayList<Integer> retransmitSegments = new ArrayList<>();
        final int blockAck = ByteBuffer.wrap(blockAcknowledgement).order(ByteOrder.BIG_ENDIAN).getInt();
        for (int i = 0; i < segmentCount; i++) {
            int bit = (blockAck >> i) & 1;
            if (bit == 1) {
                LOG.info("Segment " + i + " of " + (segmentCount - 1) + " received by peer");
            } else {
                retransmitSegments.add(i);
                LOG.info("Segment " + i + " of " + (segmentCount - 1) + " not received by peer");
            }
        }
        return retransmitSegments;
    }

    /**
     * Checks if all segments are received based on the segment count
     *
     * @param blockAcknowledgement acknowledgement payload received
     * @param segN                 number of segments
     */
    public static boolean hasAllSegmentsBeenReceived(final Integer blockAcknowledgement, final int segN) {
        if (blockAcknowledgement == null)
            return false;
        LOG.info("Block ack: " + blockAcknowledgement);
        final int blockAck = blockAcknowledgement;
        int setBitCount = 0;
        for (int i = 0; i < segN; i++) {
            int bit = (blockAck >> i) & 1;
            if (bit == 1) {
                setBitCount++;
            }
        }
        LOG.info("bit count: " + setBitCount);
        return setBitCount == segN + 1; //Since segN is 0 based add 1 as the bit count represents the number of segments
    }
}
