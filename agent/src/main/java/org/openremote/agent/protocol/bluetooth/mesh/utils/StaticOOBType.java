package org.openremote.agent.protocol.bluetooth.mesh.utils;

public enum StaticOOBType {

    /**
     * Static OOB Type
     */
    STATIC_OOB_AVAILABLE((short) 0x0001);

    private static final String TAG = StaticOOBType.class.getSimpleName();
    private short staticOobType;

    StaticOOBType(final short staticOobType) {
        this.staticOobType = staticOobType;
    }

    public static String parseStaticOOBActionInformation(final StaticOOBType type) {
        switch (type) {
            case STATIC_OOB_AVAILABLE:
                return "Static OOB Actions available";
            default:
                return "Static OOB Actions unavailable";
        }
    }

    /**
     * Returns the static oob type value
     */
    public short getStaticOobType() {
        return staticOobType;
    }
}
