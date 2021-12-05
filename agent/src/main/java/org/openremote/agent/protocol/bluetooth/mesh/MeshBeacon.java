package org.openremote.agent.protocol.bluetooth.mesh;

/**
 * Abstract class containing mesh beacon information
 */
public abstract class MeshBeacon {

    private static final String TAG = MeshBeacon.class.getSimpleName();
    static final int MESH_BEACON = 0x2B;
    final byte[] beaconData;
    final int beaconType;


    /**
     * Constructs a {@link MeshBeacon} object
     *
     * @param beaconData beacon data advertised by the mesh beacon
     * @throws IllegalArgumentException if beacon data provided is empty or null
     */
    MeshBeacon(final byte[] beaconData) {
        if (beaconData == null)
            throw new IllegalArgumentException("Invalid beacon data");
        this.beaconData = beaconData;
        beaconType = beaconData[0];
    }

    /**
     * Returns the beacon type value
     */
    public abstract int getBeaconType();

}
