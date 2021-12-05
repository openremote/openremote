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
