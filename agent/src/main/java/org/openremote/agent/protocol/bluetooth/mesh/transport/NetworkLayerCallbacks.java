package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.NetworkKey;
import org.openremote.agent.protocol.bluetooth.mesh.Provisioner;

import java.util.List;

public interface NetworkLayerCallbacks {

    /**
     * Callback to retrieve the current provisioner of the network
     */
    Provisioner getProvisioner();

    /**
     * Callback to retrieve a provisioner of the mesh network
     *
     * @param unicastAddress address of the provisioner
     */
    Provisioner getProvisioner(final int unicastAddress);

    /**
     * Callback to retrieve the primary network key of the mesh network.
     * <p>This usually is the key with the 0th index in the netkey list</p>
     */
    NetworkKey getPrimaryNetworkKey();

    /**
     * Callback to retrieve the network key of the mesh network.
     *
     * @param keyIndex Index of the network key
     */
    NetworkKey getNetworkKey(final int keyIndex);

    /**
     * Callback to retrieve the list of {@link NetworkKey} belonging to this network.
     */
    List<NetworkKey> getNetworkKeys();
}
