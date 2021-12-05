package org.openremote.agent.protocol.bluetooth.mesh;

import org.openremote.agent.protocol.bluetooth.mesh.transport.ProvisionedMeshNode;

import java.util.List;

public interface MeshNetworkCallbacks {

    void onMeshNetworkUpdated();

    void onNetworkKeyAdded(final NetworkKey networkKey);

    void onNetworkKeyUpdated(final NetworkKey networkKey);

    void onNetworkKeyDeleted(final NetworkKey networkKey);

    void onApplicationKeyAdded(final ApplicationKey applicationKey);

    void onApplicationKeyUpdated(final ApplicationKey applicationKey);

    void onApplicationKeyDeleted(final ApplicationKey applicationKey);

    void onProvisionerAdded(final Provisioner provisioner);

    void onProvisionerUpdated(final Provisioner provisioner);

    void onProvisionersUpdated(final List<Provisioner> provisioner);

    void onProvisionerDeleted(final Provisioner provisioner);

    void onNodeDeleted(final ProvisionedMeshNode meshNode);

    void onNodeAdded(final ProvisionedMeshNode meshNode);

    void onNodeUpdated(final ProvisionedMeshNode meshNode);

    void onNodesUpdated();

    void onGroupAdded(final Group group);

    void onGroupUpdated(final Group group);

    void onGroupDeleted(final Group group);

    void onSceneAdded(final Scene scene);

    void onSceneUpdated(final Scene scene);

    void onSceneDeleted(final Scene scene);
}
