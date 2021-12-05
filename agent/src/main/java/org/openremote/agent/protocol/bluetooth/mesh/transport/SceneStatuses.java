package org.openremote.agent.protocol.bluetooth.mesh.transport;

interface SceneStatuses {

    default String getStatusMessage(final int status) {
        switch (status) {
            case 0x00:
                return "Success.";
            case 0x01:
                return "Scene Register Full.";
            case 0x02:
                return "Scene Not Found.";
            default:
                return "Reserved for Future Use";

        }
    }
}
