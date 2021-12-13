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
package org.openremote.agent.protocol.bluetooth.mesh.utils;

public enum AuthenticationOOBMethods {
    //Selecting one of the authentication methods defined below during the provisioning process will require the user to,

    // - Nothing
    NO_OOB_AUTHENTICATION((short) 0),
    // - enter a 16-bit value provided by the device manufacturer to be entered during hte provisioning process
    STATIC_OOB_AUTHENTICATION((short) 1),
    // - enter the number of times the device blinked, beeped, vibrated, displayed or an alphanumeric value displayed by the device
    OUTPUT_OOB_AUTHENTICATION((short) 2),
    // - push, twist, input a number or an alpha numeric value displayed on the provisioner app
    INPUT_OOB_AUTHENTICATION((short) 3);

    private short authenticationMethod;

    AuthenticationOOBMethods(final short authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    /**
     * Returns the authentication method
     */
    public short getAuthenticationMethod() {
        return authenticationMethod;
    }

    /**
     * Returns the oob method used for authentication
     *
     * @param method auth method used
     */
    public static AuthenticationOOBMethods fromValue(final int method) {
        switch (method) {
            case 0:
                return NO_OOB_AUTHENTICATION;
            case 1:
                return STATIC_OOB_AUTHENTICATION;
            case 2:
                return OUTPUT_OOB_AUTHENTICATION;
            case 3:
                return INPUT_OOB_AUTHENTICATION;
            default:
                return null;
        }
    }

    /**
     * Parses the authentication method used.
     */
    public static String getAuthenticationMethodName(final AuthenticationOOBMethods type) {
        switch (type) {
            case NO_OOB_AUTHENTICATION:
                return "No OOB";
            case STATIC_OOB_AUTHENTICATION:
                return "Static OOB";
            case OUTPUT_OOB_AUTHENTICATION:
                return "Output OOB";
            case INPUT_OOB_AUTHENTICATION:
                return "Input OOB";
            default:
                return "Prohibited";
        }
    }
}
