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
package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.Features;
import org.openremote.agent.protocol.bluetooth.mesh.NodeKey;
import org.openremote.agent.protocol.bluetooth.mesh.utils.NetworkTransmitSettings;
import org.openremote.agent.protocol.bluetooth.mesh.utils.RelaySettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class ProvisionedBaseMeshNode  {

    public static final int DISABLED = 0;
    public static final int ENABLED = 1;
    public static final int UNSUPPORTED = 2;
    public static final int LOW = 0; //Low security
    public static final int HIGH = 1; //High security
    protected static final String TAG = ProvisionedBaseMeshNode.class.getSimpleName();
    public long mTimeStampInMillis;
    protected String nodeName = "My Node";
    protected Integer ttl = 5;
    protected Boolean secureNetworkBeaconSupported;
    protected NetworkTransmitSettings networkTransmitSettings;
    protected RelaySettings relaySettings;
    /**
     * Unique identifier of the mesh network
     */
    String meshUuid;
    /**
     * Device UUID
     */
    String uuid;
    int security = LOW;
    int unicastAddress;
    boolean isConfigured;
    byte[] deviceKey;
    int sequenceNumber = 0;
    Integer companyIdentifier = null;
    Integer productIdentifier = null;
    Integer versionIdentifier = null;
    Integer crpl = null;
    Features nodeFeatures = null;
    // SparseIntArrayParcelable mSeqAuth = new SparseIntArrayParcelable();
    Map<Integer, Integer> mSeqAuth = new HashMap<>();
    List<NodeKey> mAddedNetKeys = new ArrayList<>();
    List<NodeKey> mAddedAppKeys = new ArrayList<>();
    byte[] mFlags;
    Map<Integer, Element> mElements = new LinkedHashMap<>();
    boolean excluded = false;
    int nodeIdentityState;

    public ProvisionedBaseMeshNode() {
    }

    public String getMeshUuid() {
        return meshUuid;
    }

    public void setMeshUuid(final String meshUuid) {
        this.meshUuid = meshUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public final boolean isConfigured() {
        return isConfigured;
    }

    public final void setConfigured(final boolean configured) {
        isConfigured = configured;
    }

    public final String getNodeName() {
        return nodeName;
    }

    public final void setNodeName(final String nodeName) {
        if (nodeName != null && nodeName.length() > 0)
            this.nodeName = nodeName;
    }

    public final int getUnicastAddress() {
        return unicastAddress;
    }

    /**
     * Sets the unicast address of the node
     * <p>This is to be used only by the library</p>
     */
    public final void setUnicastAddress(final int unicastAddress) {
        this.unicastAddress = unicastAddress;
    }

    /**
     * Returns the number of elements in the node
     */
    public int getNumberOfElements() {
        return mElements.size();
    }

    /**
     * Returns the unicast address used by the last element in the node
     */
    public int getLastUnicastAddress() {
        final int elementCount = getNumberOfElements();
        return elementCount == 1 ? unicastAddress : (unicastAddress + (elementCount - 1));
    }

    public final Integer getTtl() {
        return ttl;
    }

    public final void setTtl(final Integer ttl) {
        this.ttl = ttl;
    }

    public final byte[] getFlags() {
        return mFlags;
    }

    public final void setFlags(final byte[] flags) {
        this.mFlags = flags;
    }

    public long getTimeStamp() {
        return mTimeStampInMillis;
    }

    public void setTimeStamp(final long timestamp) {
        mTimeStampInMillis = timestamp;
    }

    //
    // Returns the {@link SecurityState} of the node
    //
    //@SecurityState
    public int getSecurity() {
        return security;
    }

    //
    // Set security state of the node {@link SecurityState}
    //
    public void setSecurity(/*@SecurityState*/ final int security) {
        this.security = security;
    }

    /**
     * Returns true if the node is blacklisted or false otherwise
     *
     * @deprecated Use {@link #isExcluded()} instead
     */
    @Deprecated
    public boolean isBlackListed() {
        return isExcluded();
    }

    /**
     * Blacklist a node.
     *
     * @param blackListed true if blacklisted
     * @deprecated Use {@link #setExcluded(boolean)} instead
     */
    @Deprecated
    public void setBlackListed(final boolean blackListed) {
        setExcluded(blackListed);
    }

    //
    // Returns the {@link SecureNetworkBeacon} beacon of this node
    //
    public Boolean isSecureNetworkBeaconSupported() {
        return secureNetworkBeaconSupported;
    }

    //
    //Sets the {@link SecureNetworkBeacon} beacon for this node
    //
    public void setSecureNetworkBeaconSupported(final Boolean enable) {
        this.secureNetworkBeaconSupported = enable;
    }

    /**
     * Returns {@link NetworkTransmitSettings} of this node
     */
    public NetworkTransmitSettings getNetworkTransmitSettings() {
        return networkTransmitSettings;
    }

    /**
     * Sets {@link NetworkTransmitSettings} of this node
     */
    public void setNetworkTransmitSettings(final NetworkTransmitSettings networkTransmitSettings) {
        this.networkTransmitSettings = networkTransmitSettings;
    }

    /**
     * Returns {@link RelaySettings} of this node
     */
    public RelaySettings getRelaySettings() {
        return relaySettings;
    }

    /**
     * Sets {@link NetworkTransmitSettings} of this node
     */
    public void setRelaySettings(final RelaySettings relaySettings) {
        this.relaySettings = relaySettings;
    }

    /**
     * Returns true if the node is marked as excluded.
     *
     * @return true if marked as excluded or false otherwise.
     */
    public boolean isExcluded() {
        return excluded;
    }

    //
    // Returns the node {@link NodeIdentityState}.
    //
    // @NodeIdentityState
    public int getNodeIdentityState() {
        return nodeIdentityState;
    }

    //
    // Marks a node as excluded. Note that to exclude a node from a network, users must call
    // {@link MeshNetwork#excludeNode(ProvisionedMeshNode)}
    //
    // @param excluded true if the node is to be excluded or false otherwise
    //
    public void setExcluded(final boolean excluded) {
        this.excluded = excluded;
    }

    /*
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LOW, HIGH})
    public @interface SecurityState {
    }
     */

    /**
     * Secure Network Beacon state determines if a node is periodically broadcasting Secure Network Beacons.
     */
    /*
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DISABLED, ENABLED})
    public @interface SecureNetworkBeaconState {
    }
     */

    /**
     * The Node Identity state determines if a node is advertising with Node Identity messages on a subnet.
     * If the Mesh Proxy Service is exposed, the node can be configured to advertise with Node Identity on a subnet.
     */
    /*
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DISABLED, ENABLED, UNSUPPORTED})
    public @interface NodeIdentityState {
    }
     */
}
