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

public class ProvisioningFailedState extends ProvisioningState {

    private int error;

    public ProvisioningFailedState() {
        super();
    }

    @Override
    public State getState() {
        return State.PROVISIONING_FAILED;
    }

    @Override
    public void executeSend() {

    }

    @Override
    public boolean parseData(final byte[] data) {
        error = data[2];

        return true;
    }

    public int getErrorCode() {
        return error;
    }

    public static String parseProvisioningFailure(final int errorCode) {
        switch (ProvisioningFailureCode.fromErrorCode(errorCode)) {
            case PROHIBITED:
                return "PROHIBITED";/* context.getString(R.string.error_prohibited);*/
            case INVALID_PDU:
                return "INVALID_PDU";/*context.getString(R.string.error_invalid_pdu);*/
            case INVALID_FORMAT:
                return "INVALID_FORMAT";/*context.getString(R.string.error_invalid_format);*/
            case UNEXPECTED_PDU:
                return "UNEXPECTED_PDU";/*context.getString(R.string.error_prohibited);*/
            case CONFIRMATION_FAILED:
                return "CONFIRMATION_FAILED";/*context.getString(R.string.error_confirmation_failed);*/
            case OUT_OF_RESOURCES:
                return "OUT_OF_RESOURCES";/*context.getString(R.string.error_prohibited);*/
            case DECRYPTION_FAILED:
                return "DECRYPTION_FAILED";/*context.getString(R.string.error_decryption_failed);*/
            case UNEXPECTED_ERROR:
                return "UNEXPECTED_ERROR";/*context.getString(R.string.error_unexpected_error);*/
            case CANNOT_ASSIGN_ADDRESSES:
                return "CANNOT_ASSIGN_ADDRESSES";/*context.getString(R.string.error_cannot_assign_addresses);*/
            case UNKNOWN_ERROR_CODE:
            default:
                return "UNKNOWN_ERROR_CODE";/*context.getString(R.string.error_rfu);*/
        }
    }

    public enum ProvisioningFailureCode {
        PROHIBITED(0x00),
        INVALID_PDU(0x01),
        INVALID_FORMAT(0x02),
        UNEXPECTED_PDU(0x03),
        CONFIRMATION_FAILED(0x04),
        OUT_OF_RESOURCES(0x05),
        DECRYPTION_FAILED(0x06),
        UNEXPECTED_ERROR(0x07),
        CANNOT_ASSIGN_ADDRESSES(0x08),
        UNKNOWN_ERROR_CODE(0x09);

        private final int errorCode;

        ProvisioningFailureCode(final int errorCode) {
            this.errorCode = errorCode;
        }

        public static ProvisioningFailureCode fromErrorCode(final int errorCode) {
            for (ProvisioningFailureCode failureCode : ProvisioningFailureCode.values()) {
                if (failureCode.getErrorCode() == errorCode) {
                    return failureCode;
                }
            }
            return UNKNOWN_ERROR_CODE;
            //throw new RuntimeException("Enum not found");
        }

        public final int getErrorCode() {
            return errorCode;
        }
    }

}

