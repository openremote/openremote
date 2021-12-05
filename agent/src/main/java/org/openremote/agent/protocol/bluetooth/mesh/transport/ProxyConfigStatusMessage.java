package org.openremote.agent.protocol.bluetooth.mesh.transport;

/**
 * Abstract wrapper class for mesh message.
 */
abstract class ProxyConfigStatusMessage extends MeshMessage {

    ProxyConfigStatusMessage(final ControlMessage message) {
        mMessage = message;
    }

    /**
     * Parses the status parameters returned by a status message
     */
    abstract void parseStatusParameters();

    @Override
    int getAid() {
        return -1;
    }

    @Override
    int getAkf() {
        return -1;
    }

}
