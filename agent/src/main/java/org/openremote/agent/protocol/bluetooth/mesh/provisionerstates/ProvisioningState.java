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

public abstract class ProvisioningState {

    static final byte TYPE_PROVISIONING_INVITE = 0x00;
    static final byte TYPE_PROVISIONING_CAPABILITIES = 0x01;
    static final byte TYPE_PROVISIONING_START = 0x02;
    static final byte TYPE_PROVISIONING_PUBLIC_KEY = 0x03;
    static final byte TYPE_PROVISIONING_INPUT_COMPLETE = 0x04;
    static final byte TYPE_PROVISIONING_CONFIRMATION = 0x05;
    static final byte TYPE_PROVISIONING_RANDOM_CONFIRMATION = 0x06;
    static final byte TYPE_PROVISIONING_DATA = 0x07;
    static final byte TYPE_PROVISIONING_COMPLETE = 0x08;

    public ProvisioningState() {
    }

    public abstract State getState();

    public abstract void executeSend();

    public abstract boolean parseData(final byte[] data);

    public enum State {
        PROVISIONING_INVITE(0), PROVISIONING_CAPABILITIES(1), PROVISIONING_START(2), PROVISIONING_PUBLIC_KEY(3),
        PROVISIONING_INPUT_COMPLETE(4), PROVISIONING_CONFIRMATION(5), PROVISIONING_RANDOM(6),
        PROVISIONING_DATA(7), PROVISIONING_COMPLETE(8), PROVISIONING_FAILED(9);

        private int state;

        State(final int state) {
            this.state = state;
        }

        public int getState() {
            return state;
        }

    }

    public enum States {
        PROVISIONING_INVITE(0),
        PROVISIONING_CAPABILITIES(1),
        PROVISIONING_START(2),
        PROVISIONING_PUBLIC_KEY_SENT(3),
        PROVISIONING_PUBLIC_KEY_RECEIVED(4),
        PROVISIONING_AUTHENTICATION_INPUT_OOB_WAITING(5),
        PROVISIONING_AUTHENTICATION_OUTPUT_OOB_WAITING(6),
        PROVISIONING_AUTHENTICATION_STATIC_OOB_WAITING(7),
        PROVISIONING_AUTHENTICATION_INPUT_ENTERED(8),
        PROVISIONING_INPUT_COMPLETE(9),
        PROVISIONING_CONFIRMATION_SENT(10),
        PROVISIONING_CONFIRMATION_RECEIVED(11),
        PROVISIONING_RANDOM_SENT(12),
        PROVISIONING_RANDOM_RECEIVED(13),
        PROVISIONING_DATA_SENT(14),
        PROVISIONING_COMPLETE(15),
        PROVISIONING_FAILED(16),
        COMPOSITION_DATA_GET_SENT(17),
        COMPOSITION_DATA_STATUS_RECEIVED(18),
        SENDING_DEFAULT_TTL_GET(19),
        DEFAULT_TTL_STATUS_RECEIVED(20),
        SENDING_APP_KEY_ADD(21),
        APP_KEY_STATUS_RECEIVED(22),
        SENDING_NETWORK_TRANSMIT_SET(23),
        NETWORK_TRANSMIT_STATUS_RECEIVED(24),
        SENDING_BLOCK_ACKNOWLEDGEMENT(98),
        BLOCK_ACKNOWLEDGEMENT_RECEIVED(99);

        private int state;

        States(final int state) {
            this.state = state;
        }

        public int getState() {
            return state;
        }

        public static States fromStatusCode(final int statusCode) {
            for (States state : States.values()) {
                if (state.getState() == statusCode) {
                    return state;
                }
            }
            throw new IllegalStateException("Invalid state");
        }
    }
}
