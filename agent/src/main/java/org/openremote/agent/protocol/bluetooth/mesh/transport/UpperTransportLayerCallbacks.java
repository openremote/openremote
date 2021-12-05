package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.ApplicationKey;
import org.openremote.agent.protocol.bluetooth.mesh.Group;

import java.util.List;

/**
 * Upper transport layer call backs
 */
public interface UpperTransportLayerCallbacks {


    /**
     * Callback to get the mesh node from the list of provisioned mesh node.
     *
     * @param unicastAddress unicast address of the mesh node
     */
    ProvisionedMeshNode getNode(final int unicastAddress);

    /**
     * Returns the IV Index of the mesh network
     */
    byte[] getIvIndex();

    /**
     * Returns the application key with the specific application key identifier
     *
     * @param aid application key identifier
     */
    byte[] getApplicationKey(final int aid);

    /**
     * Returns a list of Application Keys matching the bound net key index and AID.
     *
     * @param boundNetKeyIndex Index of the bound network key.
     */
    List<ApplicationKey> getApplicationKeys(final int boundNetKeyIndex);

    /**
     * Returns the list of groups
     */
    List<Group> gerVirtualGroups();
}
