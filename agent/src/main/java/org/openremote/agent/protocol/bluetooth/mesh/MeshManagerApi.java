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
package org.openremote.agent.protocol.bluetooth.mesh;

import org.openremote.agent.protocol.bluetooth.mesh.provisionerstates.UnprovisionedMeshNode;
import org.openremote.agent.protocol.bluetooth.mesh.transport.MeshMessage;
import org.openremote.agent.protocol.bluetooth.mesh.transport.NetworkLayerCallbacks;
import org.openremote.agent.protocol.bluetooth.mesh.transport.ProvisionedMeshNode;
import org.openremote.agent.protocol.bluetooth.mesh.transport.UpperTransportLayerCallbacks;
import org.openremote.agent.protocol.bluetooth.mesh.utils.ExtendedInvalidCipherTextException;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;
import org.openremote.agent.protocol.bluetooth.mesh.utils.ProxyFilter;
import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import java.nio.ByteBuffer;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MeshManagerApi implements MeshMngrApi{

    public static final Logger LOG = Logger.getLogger(MeshManagerApi.class.getName());
    public final static UUID MESH_PROVISIONING_UUID = UUID.fromString("00001827-0000-1000-8000-00805F9B34FB");
    public final static UUID MESH_PROXY_UUID = UUID.fromString("00001828-0000-1000-8000-00805F9B34FB");
    public static final byte PDU_TYPE_PROVISIONING = 0x03;

    private static final long PROXY_SAR_TRANSFER_TIME_OUT = 20 * 1000; // According to the spec the proxy protocol must contain an SAR timeout of 20 seconds.

    //PDU types
    public static final byte PDU_TYPE_NETWORK = 0x00;
    public static final byte PDU_TYPE_MESH_BEACON = 0x01;
    public static final byte PDU_TYPE_PROXY_CONFIGURATION = 0x02;
    //GATT level segmentation
    private static final byte GATT_SAR_COMPLETE = 0b00;
    private static final byte GATT_SAR_START = 0b01;
    private static final byte GATT_SAR_CONTINUATION = 0b10;
    private static final byte GATT_SAR_END = 0b11;
    //GATT level segmentation mask
    private static final int GATT_SAR_MASK = 0xC0;
    private static final int GATT_SAR_UNMASK = 0x3F;
    private static final int SAR_BIT_OFFSET = 6;

    private boolean ivUpdateTestModeActive = false;
    private boolean allowIvIndexRecoveryOver42 = false;

    private MeshNetwork mMeshNetwork;

    private MeshManagerCallbacks mMeshManagerCallbacks;
    private final MeshMessageHandler mMeshMessageHandler;
    private final MeshProvisioningHandler mMeshProvisioningHandler;
    // private final ImportExportUtils mImportExportUtils;

    private byte[] mOutgoingBuffer;
    private int mOutgoingBufferOffset;
    private byte[] mIncomingBuffer;
    private int mIncomingBufferOffset;

    private final ScheduledExecutorService executorService;

    private final Runnable mProxyProtocolTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            mMeshMessageHandler.onIncompleteTimerExpired(MeshAddress.UNASSIGNED_ADDRESS);
        }
    };

    /**
     * The mesh manager api constructor.
     */
    public MeshManagerApi(ScheduledExecutorService executorService) {
        this.executorService = executorService;
        // mHandler = new Handler(Looper.getMainLooper());
        mMeshProvisioningHandler = new MeshProvisioningHandler(internalTransportCallbacks, internalMeshMgrCallbacks);
        mMeshMessageHandler = new MeshMessageHandler(internalTransportCallbacks, networkLayerCallbacks, upperTransportLayerCallbacks);
        // mImportExportUtils = new ImportExportUtils();
        initBouncyCastle();
        //Init database
        // initDb(context);
    }


    @Override
    public synchronized void setMeshManagerCallbacks(final MeshManagerCallbacks callbacks) {
        mMeshManagerCallbacks = callbacks;
    }

    @Override
    public void setMeshStatusCallbacks(final MeshStatusCallbacks callbacks) {
        mMeshMessageHandler.setMeshStatusCallbacks(callbacks);
    }

    @Override
    public synchronized final void handleWriteCallbacks(final int mtuSize, final byte[] data) {
        byte[] unsegmentedPdu;
        if (!shouldWaitForMoreData(data)) {
            unsegmentedPdu = data;
        } else {
            final byte[] combinedPdu = appendWritePdu(mtuSize, data);
            if (combinedPdu == null)
                return;
            else {
                unsegmentedPdu = removeSegmentation(mtuSize, combinedPdu);
            }
        }
        handleWriteCallbacks(unsegmentedPdu);
    }

    @Override
    public synchronized final void handleNotifications(final int mtuSize, final byte[] data) {
        byte[] unsegmentedPdu;
        if (!shouldWaitForMoreData(data)) {
            unsegmentedPdu = data;
        } else {
            final byte[] combinedPdu = appendPdu(mtuSize, data);
            if (combinedPdu == null) {
                //Start the timer
                toggleProxyProtocolSarTimeOut(data);
                return;
            } else {
                toggleProxyProtocolSarTimeOut(data);
                unsegmentedPdu = removeSegmentation(mtuSize, combinedPdu);
            }
        }
        parseNotifications(unsegmentedPdu);
    }

    public String dataAsHexString(byte[] data) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            builder.append(String.format("0x%02X%s", data[i] & 0xFF, i == (data.length - 1) ? "" : ", "));
        }
        return builder.toString();
    }


    public synchronized MeshNetwork getMeshNetwork() {
        return mMeshNetwork;
    }

    /**
     * Toggles the Segmentation and Reassembly timeout for proxy configuration messages received via proxy protocol
     *
     * @param data pdu
     */
    private void toggleProxyProtocolSarTimeOut(final byte[] data) {
        final int pduType = MeshParserUtils.unsignedByteToInt(data[0]);
        if (pduType == ((GATT_SAR_START << SAR_BIT_OFFSET) | MeshManagerApi.PDU_TYPE_PROXY_CONFIGURATION)) {
            // mHandler.postDelayed(mProxyProtocolTimeoutRunnable, PROXY_SAR_TRANSFER_TIME_OUT);
            scheduleTimeoutHandler();
        } else if (pduType == ((GATT_SAR_END << SAR_BIT_OFFSET) | MeshManagerApi.PDU_TYPE_PROXY_CONFIGURATION)) {
            // mHandler.removeCallbacks(mProxyProtocolTimeoutRunnable);
            cancelTimeoutHandler();
        }
    }

    private ScheduledFuture<?> scheduledFuture;

    private void scheduleTimeoutHandler() {
        cancelTimeoutHandler();
        scheduledFuture = executorService.schedule(mProxyProtocolTimeoutRunnable, PROXY_SAR_TRANSFER_TIME_OUT, TimeUnit.MILLISECONDS);
    }

    private void cancelTimeoutHandler() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        scheduledFuture = null;
    }

    /**
     * Parses notifications received by the client.
     *
     * @param unsegmentedPdu pdu received by the client.
     */
    private void parseNotifications(final byte[] unsegmentedPdu) {
        try {
            switch (unsegmentedPdu[0]) {
                case PDU_TYPE_NETWORK:
                    //MeshNetwork PDU
                    LOG.info("Received network pdu: " + MeshParserUtils.bytesToHex(unsegmentedPdu, true));
                    mMeshMessageHandler.parseMeshPduNotifications(unsegmentedPdu, mMeshNetwork);
                    break;
                case PDU_TYPE_MESH_BEACON:
                    //Validate SNBs against all network keys
                    NetworkKey networkKey;
                    for (int i = 0; i < mMeshNetwork.getNetKeys().size(); i++) {
                        networkKey = mMeshNetwork.getNetKeys().get(i);
                        final byte[] receivedBeaconData = new byte[unsegmentedPdu.length - 1];
                        System.arraycopy(unsegmentedPdu, 1, receivedBeaconData, 0, receivedBeaconData.length);
                        final SecureNetworkBeacon receivedBeacon = new SecureNetworkBeacon(receivedBeaconData);

                        final byte[] n = networkKey.getTxNetworkKey();
                        final int flags = receivedBeacon.getFlags();
                        final byte[] networkId = SecureUtils.calculateK3(n);
                        final int ivIndex = receivedBeacon.getIvIndex().getIvIndex();
                        LOG.info("Received mesh beacon: " + receivedBeacon.toString());

                        final SecureNetworkBeacon localSecureNetworkBeacon = SecureUtils.createSecureNetworkBeacon(n, flags, networkId, ivIndex);
                        //Check the the beacon received is a valid by matching the authentication values
                        if (Arrays.equals(receivedBeacon.getAuthenticationValue(), localSecureNetworkBeacon.getAuthenticationValue())) {
                            LOG.info("Secure Network Beacon beacon authenticated.");

                            //  The library does not retransmit Secure Network Beacon.
                            //  If this node is a member of a primary subnet and receives a Secure Network
                            //  beacon on a secondary subnet, it will disregard it.
                            if (mMeshNetwork.getPrimaryNetworkKey() != null && networkKey.keyIndex != 0) {
                                LOG.info("Discarding beacon for secondary subnet with network key index: " + networkKey.keyIndex);
                                return;
                            }

                            // Get the last IV Index.
                            /// The last used IV Index for this mesh network.
                            final IvIndex lastIvIndex = mMeshNetwork.getIvIndex();
                            LOG.info("Last IV Index: " + lastIvIndex.getIvIndex());
                            /// The date of the last change of IV Index or IV Update Flag.
                            final Calendar lastTransitionDate = lastIvIndex.getTransitionDate();
                            /// A flag whether the IV has recently been updated using IV Recovery procedure.
                            /// The at-least-96h requirement for the duration of the current state will not apply.
                            /// The node shall not execute more than one IV Index Recovery within a period of 192 hours.
                            final boolean isIvRecoveryActive = lastIvIndex.getIvRecoveryFlag();
                            /// The test mode disables the 96h rule, leaving all other behavior unchanged.
                            final boolean isIvTestModeActive = ivUpdateTestModeActive;

                            final boolean flag = allowIvIndexRecoveryOver42;
                            if (!receivedBeacon.canOverwrite(lastIvIndex, lastTransitionDate, isIvRecoveryActive, isIvTestModeActive, flag)) {
                                String numberOfHoursSinceDate = ((Calendar.getInstance().getTimeInMillis() -
                                    lastTransitionDate.getTimeInMillis()) / (3600 * 1000)) + "h";
                                LOG.info("Discarding beacon " + receivedBeacon.getIvIndex() +
                                    ", last " + lastIvIndex.getIvIndex() + ", changed: "
                                    + numberOfHoursSinceDate + "ago, test mode: " + ivUpdateTestModeActive);
                                return;
                            }

                            final IvIndex receivedIvIndex = receivedBeacon.getIvIndex();
                            mMeshNetwork.ivIndex = new IvIndex(receivedIvIndex.getIvIndex(), receivedIvIndex.isIvUpdateActive(), lastTransitionDate);

                            if (mMeshNetwork.ivIndex.getIvIndex() > lastIvIndex.getIvIndex()) {
                                LOG.info("Applying: " + mMeshNetwork.ivIndex.getIvIndex());
                            }

                            // If the IV Index used for transmitting messages effectively increased,
                            // the Node shall reset the sequence number to 0x000000.
                            if (mMeshNetwork.ivIndex.getTransmitIvIndex() > lastIvIndex.getTransmitIvIndex()) {
                                LOG.info("Resetting local sequence numbers to 0");
                                final Provisioner provisioner = mMeshNetwork.getSelectedProvisioner();
                                final ProvisionedMeshNode node = mMeshNetwork.getNode(provisioner.getProvisionerUuid());
                                node.setSequenceNumber(0);
                            }

                            //Updating the iv recovery flag
                            if (lastIvIndex != mMeshNetwork.ivIndex) {
                                final boolean ivRecovery = mMeshNetwork.getIvIndex().getIvIndex() > lastIvIndex.getIvIndex() + 1
                                    && !receivedBeacon.getIvIndex().isIvUpdateActive();
                                mMeshNetwork.getIvIndex().setIvRecoveryFlag(ivRecovery);
                            }

                            if (!mMeshNetwork.ivIndex.getIvRecoveryFlag()) {
                                final Iterator<Map.Entry<Integer, ArrayList<Integer>>> iterator = mMeshNetwork.networkExclusions.entrySet().iterator();
                                while (iterator.hasNext()) {
                                    final Map.Entry<Integer, ArrayList<Integer>> exclusions = iterator.next();
                                    final int expectedIncrement = exclusions.getKey() + 2;
                                    if (mMeshNetwork.ivIndex.getIvIndex() >= expectedIncrement) {
                                        // Clear the last known sequence number of addresses that are to be removed from the exclusion list.
                                        // Decided to retain the last known sequence number as the IV Indexes increment the sequence number
                                        // will be greater than the last known anyways
                                        //for (Integer address : mMeshNetwork.networkExclusions.get(expectedIncrement)) {
                                        //    mMeshNetwork.sequenceNumbers.removeAt(address);
                                        //}
                                        iterator.remove();
                                    }
                                }
                            }
                        }
                    }
                    break;
                case PDU_TYPE_PROXY_CONFIGURATION:
                    //Proxy configuration
                    LOG.info("Received proxy configuration message: " + MeshParserUtils.bytesToHex(unsegmentedPdu, true));
                    mMeshMessageHandler.parseMeshPduNotifications(unsegmentedPdu, mMeshNetwork);
                    break;
                case PDU_TYPE_PROVISIONING:
                    //Provisioning PDU
                    LOG.info("Received provisioning message: " + MeshParserUtils.bytesToHex(unsegmentedPdu, true));
                    mMeshProvisioningHandler.parseProvisioningNotifications(unsegmentedPdu);
                    break;
            }
        } catch (ExtendedInvalidCipherTextException ex) {
            //TODO handle decryption failure
        } catch (IllegalArgumentException ex) {
            LOG.severe("Parsing notification failed: " + MeshParserUtils.bytesToHex(unsegmentedPdu, true) + " - " + ex.getMessage());
        }
    }



    /**
     * Handles callbacks after writing to characteristics to maintain/update the state machine
     *
     * @param data written to the peripheral
     */
    private void handleWriteCallbacks(final byte[] data) {
        switch (data[0]) {
            case PDU_TYPE_NETWORK: // MeshNetwork PDU
                LOG.info("MeshNetwork pdu sent: " + MeshParserUtils.bytesToHex(data, true));
                break;
            case PDU_TYPE_MESH_BEACON: // MESH BEACON
                LOG.info("Mesh beacon pdu sent: " + MeshParserUtils.bytesToHex(data, true));
                break;
            case PDU_TYPE_PROXY_CONFIGURATION: // Proxy configuration
                LOG.info("Proxy configuration pdu sent: " + MeshParserUtils.bytesToHex(data, true));
                break;
            case PDU_TYPE_PROVISIONING: // Provisioning PDU
                LOG.info("Provisioning pdu sent: " + MeshParserUtils.bytesToHex(data, true));
                mMeshProvisioningHandler.handleProvisioningWriteCallbacks();
                break;
        }
    }

    private boolean shouldWaitForMoreData(final byte[] pdu) {
        final int gattSar = (pdu[0] & GATT_SAR_MASK) >> SAR_BIT_OFFSET;
        switch (gattSar) {
            case GATT_SAR_START:
            case GATT_SAR_CONTINUATION:
            case GATT_SAR_END:
                return true;
            default:
                return false;
        }
    }

    /**
     * Appends the PDUs that are segmented at gatt layer.
     *
     * @param mtuSize mtu size supported by the device/node
     * @param pdu     pdu received by the provisioner
     * @return the combine pdu or returns null if not complete.
     */
    private byte[] appendPdu(final int mtuSize, final byte[] pdu) {
        if (mIncomingBuffer == null) {
            final int length = Math.min(pdu.length, mtuSize);
            mIncomingBufferOffset = 0;
            mIncomingBufferOffset += length;
            mIncomingBuffer = pdu;
        } else {
            final int length = Math.min(pdu.length, mtuSize);
            final byte[] buffer = new byte[mIncomingBuffer.length + length];
            System.arraycopy(mIncomingBuffer, 0, buffer, 0, mIncomingBufferOffset);
            System.arraycopy(pdu, 0, buffer, mIncomingBufferOffset, length);
            mIncomingBufferOffset += length;
            mIncomingBuffer = buffer;
            if (length < mtuSize) {
                final byte[] packet = mIncomingBuffer;
                mIncomingBuffer = null;
                return packet;
            }
        }
        return null;
    }

    /**
     * Appends the PDUs that are segmented at gatt layer.
     *
     * @param mtuSize mtu size supported by the device/node
     * @param pdu     pdu received by the provisioner
     * @return the combine pdu or returns null if not complete.
     */
    private byte[] appendWritePdu(final int mtuSize, final byte[] pdu) {
        if (mOutgoingBuffer == null) {
            final int length = Math.min(pdu.length, mtuSize);
            mOutgoingBufferOffset = 0;
            mOutgoingBufferOffset += length;
            mOutgoingBuffer = pdu;
        } else {
            final int length = Math.min(pdu.length, mtuSize);
            final byte[] buffer = new byte[mOutgoingBuffer.length + length];
            System.arraycopy(mOutgoingBuffer, 0, buffer, 0, mOutgoingBufferOffset);
            System.arraycopy(pdu, 0, buffer, mOutgoingBufferOffset, length);
            mOutgoingBufferOffset += length;
            mOutgoingBuffer = buffer;
            if (length < mtuSize) {
                final byte[] packet = mOutgoingBuffer;
                mOutgoingBuffer = null;
                return packet;
            }
        }
        return null;
    }


    @Override
    public synchronized void createMeshPdu(final int dst, final MeshMessage meshMessage) {
        if (!MeshAddress.isAddressInRange(dst)) {
            throw new IllegalArgumentException("Invalid address, destination address must be a valid 16-bit value.");
        }
        final Provisioner provisioner = mMeshNetwork.getSelectedProvisioner();
        if (provisioner != null && provisioner.getProvisionerAddress() != null) {
            UUID label = null;
            if (MeshAddress.isValidVirtualAddress(dst)) {
                label = mMeshNetwork.getLabelUuid(dst);
                if (label == null) {
                    throw new IllegalArgumentException("Label UUID unavailable for the virtual address provided");
                }
            }
            mMeshMessageHandler.createMeshMessage(provisioner.getProvisionerAddress(), dst, label, meshMessage);
        } else {
            throw new IllegalArgumentException("Provisioner address not set, please assign an address to the provisioner.");
        }
    }

    // public synchronized final void resetMeshNetwork() {
    public synchronized final void resetMeshNetwork(int provisionerAddress) {
        //We delete the existing network as the user has already given the
        ivUpdateTestModeActive = false;
        allowIvIndexRecoveryOver42 = false;
        final MeshNetwork meshNet = mMeshNetwork;
        // deleteMeshNetworkFromDb(meshNet);
        final MeshNetwork newMeshNetwork = generateMeshNetwork(provisionerAddress);
        newMeshNetwork.setCallbacks(callbacks);
        insertNetwork(newMeshNetwork);
        mMeshNetwork = newMeshNetwork;
        mMeshManagerCallbacks.onNetworkLoaded(newMeshNetwork);
    }


    private MeshNetwork generateMeshNetwork(int provisionerAddress) {
        final String meshUuid = UUID.randomUUID().toString().toUpperCase(Locale.US);

        final MeshNetwork network = new MeshNetwork(meshUuid);
        // network.netKeys = generateNetKeys(meshUuid);
        // network.appKeys = generateAppKeys(meshUuid);
        //final AllocatedUnicastRange unicastRange = new AllocatedUnicastRange(0x0001, 0x199A);
        final AllocatedUnicastRange unicastRange = new AllocatedUnicastRange(provisionerAddress, provisionerAddress);
        final AllocatedGroupRange groupRange = new AllocatedGroupRange(0xC000, 0xCC9A);
        final AllocatedSceneRange sceneRange = new AllocatedSceneRange(0x0001, 0x3333);
        final Provisioner provisioner = network.createProvisioner("nRF Mesh Provisioner", unicastRange, groupRange, sceneRange);
        final int unicast = provisioner.getAllocatedUnicastRanges().get(0).getLowAddress();
        provisioner.assignProvisionerAddress(unicast);
        network.selectProvisioner(provisioner);
        network.addProvisioner(provisioner);
        final ProvisionedMeshNode node = network.getNode(unicast);
        if (node != null) {
            network.unicastAddress = node.getUnicastAddress() + (node.getNumberOfElements() - 1);
        } else {
            network.unicastAddress = 1;
        }
        network.lastSelected = true;
        network.sequenceNumbers.clear(); //Clear the sequence numbers first
        network.loadSequenceNumbers();
        ivUpdateTestModeActive = false;
        allowIvIndexRecoveryOver42 = false;
        return network;
    }

    private List<NetworkKey> generateNetKeys(final String meshUuid) {
        final List<NetworkKey> networkKeys = new ArrayList<>();
        final NetworkKey networkKey = new NetworkKey(0, SecureUtils.generateRandomNumber());
        networkKey.setMeshUuid(meshUuid);
        networkKeys.add(networkKey);
        return networkKeys;
    }

    private List<ApplicationKey> generateAppKeys(final String meshUuid) {
        final List<ApplicationKey> appKeys = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final ApplicationKey appKey = new ApplicationKey(i, SecureUtils.generateRandomNumber());
            appKey.setMeshUuid(meshUuid);
            appKeys.add(appKey);
        }
        return appKeys;
    }

    private void deleteNode(final ProvisionedMeshNode meshNode) {
        deleteSceneAddress(meshNode.getUnicastAddress());
        // We should not remove the last known sequence number when resetting a node.
        // This should be kept until the current iv index has incremented by 2 and delete it when
        // clearing the exclusion lists
        // mMeshNetwork.sequenceNumbers.delete(meshNode.getUnicastAddress());
        mMeshMessageHandler.resetState(meshNode.getUnicastAddress());
        // mMeshNetworkDb.deleteNode(mProvisionedNodeDao, meshNode);
        mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
    }

    /**
     * Deletes an address from the scenes in the network. This is to be called when resetting or deleting a node from the network.
     *
     * @param address Address to be removed.
     */
    private void deleteSceneAddress(final int address) {
        for (Scene scene : mMeshNetwork.getScenes()) {
            if (scene.addresses.remove((Integer) address)) {
                LOG.info("Node removed from " + scene.getName());
            }
        }
    }


    /**
     * Callbacks observing user updates on the mesh network object
     */
    private final MeshNetworkCallbacks callbacks = new MeshNetworkCallbacks() {
        @Override
        public void onMeshNetworkUpdated() {
            synchronized (MeshManagerApi.this) {
                mMeshNetwork.setTimestamp(System.currentTimeMillis());
                // mMeshNetworkDb.update(mMeshNetworkDao, mMeshNetwork);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onNetworkKeyAdded(final NetworkKey networkKey) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.insert(mNetworkKeyDao, networkKey);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onNetworkKeyUpdated(final NetworkKey networkKey) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.update(mNetworkKeyDao, networkKey);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onNetworkKeyDeleted(final NetworkKey networkKey) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.delete(mNetworkKeyDao, networkKey);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onApplicationKeyAdded(final ApplicationKey applicationKey) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.insert(mApplicationKeyDao, applicationKey);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onApplicationKeyUpdated(final ApplicationKey applicationKey) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.update(mApplicationKeyDao, applicationKey);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onApplicationKeyDeleted(final ApplicationKey applicationKey) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.delete(mApplicationKeyDao, applicationKey);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onProvisionerAdded(final Provisioner provisioner) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.insert(mProvisionerDao, provisioner);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onProvisionerUpdated(final Provisioner provisioner) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.update(mProvisionerDao, provisioner);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onProvisionersUpdated(final List<Provisioner> provisioners) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.update(mProvisionerDao, provisioners);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onProvisionerDeleted(Provisioner provisioner) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.delete(mProvisionerDao, provisioner);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onNodeDeleted(final ProvisionedMeshNode meshNode) {
            deleteNode(meshNode);
        }

        @Override
        public void onNodeAdded(final ProvisionedMeshNode meshNode) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.insert(mProvisionedNodeDao, meshNode);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onNodeUpdated(final ProvisionedMeshNode meshNode) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.update(mProvisionedNodeDao, meshNode);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onNodesUpdated() {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.update(mProvisionedNodesDao, mMeshNetwork.nodes);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onGroupAdded(final Group group) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.insert(mGroupDao, group);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onGroupUpdated(final Group group) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.update(mGroupDao, group);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onGroupDeleted(final Group group) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.delete(mGroupDao, group);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onSceneAdded(final Scene scene) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.insert(mSceneDao, scene);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onSceneUpdated(final Scene scene) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.update(mSceneDao, scene);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        @Override
        public void onSceneDeleted(final Scene scene) {
            synchronized (MeshManagerApi.this) {
                // mMeshNetworkDb.delete(mSceneDao, scene);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }
    };

    private final InternalMeshManagerCallbacks internalMeshMgrCallbacks = new InternalMeshManagerCallbacks() {
        @Override
        public void onNodeProvisioned(final ProvisionedMeshNode meshNode) {
            synchronized (MeshManagerApi.this) {
                updateProvisionedNodeList(meshNode);
                mMeshNetwork.sequenceNumbers.put(meshNode.getUnicastAddress(), meshNode.getSequenceNumber());
                mMeshNetwork.unicastAddress = mMeshNetwork.nextAvailableUnicastAddress(meshNode.getNumberOfElements(), mMeshNetwork.getSelectedProvisioner());
                //Set the mesh network uuid to the node so we can identify nodes belonging to a network
                meshNode.setMeshUuid(mMeshNetwork.getMeshUUID());
                // mMeshNetworkDb.insert(mProvisionedNodeDao, meshNode);
                // mMeshNetworkDb.update(mProvisionerDao,
                //    mMeshNetwork.getSelectedProvisioner());
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }

        private void updateProvisionedNodeList(final ProvisionedMeshNode meshNode) {
            for (int i = 0; i < mMeshNetwork.nodes.size(); i++) {
                final ProvisionedMeshNode node = mMeshNetwork.nodes.get(i);
                if (meshNode.getUuid().equals(node.getUuid())) {
                    mMeshNetwork.nodes.remove(i);
                    break;
                }
            }
            mMeshNetwork.nodes.add(meshNode);
        }
    };

    private final NetworkLayerCallbacks networkLayerCallbacks = new NetworkLayerCallbacks() {

        @Override
        public Provisioner getProvisioner() {
            synchronized (MeshManagerApi.this) {
                return mMeshNetwork.getSelectedProvisioner();
            }
        }

        @Override
        public Provisioner getProvisioner(final int unicastAddress) {
            synchronized (MeshManagerApi.this) {
                for (Provisioner provisioner : mMeshNetwork.getProvisioners()) {
                    if (provisioner.isLastSelected())
                        return provisioner;
                }
                return null;
            }
        }

        @Override
        public NetworkKey getPrimaryNetworkKey() {
            synchronized (MeshManagerApi.this) {
                return mMeshNetwork.getPrimaryNetworkKey();
            }
        }

        @Override
        public NetworkKey getNetworkKey(final int keyIndex) {
            synchronized (MeshManagerApi.this) {
                return mMeshNetwork.getNetKey(keyIndex);
            }
        }

        @Override
        public List<NetworkKey> getNetworkKeys() {
            synchronized (MeshManagerApi.this) {
                return mMeshNetwork.getNetKeys();
            }
        }
    };

    private final UpperTransportLayerCallbacks upperTransportLayerCallbacks = new UpperTransportLayerCallbacks() {

        @Override
        public ProvisionedMeshNode getNode(final int unicastAddress) {
            synchronized (MeshManagerApi.this) {
                return mMeshNetwork.getNode(unicastAddress);
            }
        }

        @Override
        public byte[] getIvIndex() {
            synchronized (MeshManagerApi.this) {
                int ivIndex = mMeshNetwork.getIvIndex().getTransmitIvIndex();
                return ByteBuffer.allocate(4).putInt(ivIndex).array();
            }
        }

        @Override
        public byte[] getApplicationKey(final int aid) {
            synchronized (MeshManagerApi.this) {
                for (ApplicationKey key : mMeshNetwork.getAppKeys()) {
                    final byte[] k = key.getKey();
                    if (aid == SecureUtils.calculateK4(k)) {
                        return key.getKey();
                    }
                }
                return null;
            }
        }

        @Override
        public List<ApplicationKey> getApplicationKeys(final int boundNetKeyIndex) {
            synchronized (MeshManagerApi.this) {
                final List<ApplicationKey> keys = new ArrayList<>();
                for (ApplicationKey key : mMeshNetwork.getAppKeys()) {
                    if (key.getBoundNetKeyIndex() == boundNetKeyIndex) {
                        keys.add(key);
                    }
                }
                return keys;
            }
        }

        /* @Nullable */
        @Override
        public List<Group> gerVirtualGroups() {
            synchronized (MeshManagerApi.this) {
                return mMeshNetwork.getGroups();
            }
        }
    };

    private final InternalTransportCallbacks internalTransportCallbacks = new InternalTransportCallbacks() {

        @Override
        public List<ApplicationKey> getApplicationKeys(final int boundNetKeyIndex) {
            synchronized (MeshManagerApi.this) {
                return mMeshNetwork.getAppKeys(boundNetKeyIndex);
            }
        }

        @Override
        public ProvisionedMeshNode getNode(final int unicast) {
            synchronized (MeshManagerApi.this) {
                return mMeshNetwork.getNode(unicast);
            }
        }

        @Override
        public Provisioner getProvisioner(final int unicast) {
            return null;
        }

        @Override
        public void sendProvisioningPdu(final UnprovisionedMeshNode meshNode, final byte[] pdu) {
            synchronized (MeshManagerApi.this) {
                final int mtu = mMeshManagerCallbacks.getMtu();
                mMeshManagerCallbacks.sendProvisioningPdu(meshNode, applySegmentation(mtu, pdu));
            }
        }

        @Override
        public void onMeshPduCreated(final int dst, final byte[] pdu) {
            synchronized (MeshManagerApi.this) {
                //We must save the mesh network state for every message that is being sent out.
                //This will specifically save the sequence number for every message sent.
                final ProvisionedMeshNode meshNode = mMeshNetwork.getNode(dst);
                updateNetwork(meshNode);
                final int mtu = mMeshManagerCallbacks.getMtu();
                mMeshManagerCallbacks.onMeshPduCreated(applySegmentation(mtu, pdu));
            }
        }

        @Override
        public ProxyFilter getProxyFilter() {
            synchronized (MeshManagerApi.this) {
                return mMeshNetwork.getProxyFilter();
            }
        }

        @Override
        public void setProxyFilter(final ProxyFilter filter) {
            synchronized (MeshManagerApi.this) {
                mMeshNetwork.setProxyFilter(filter);
            }
        }

        @Override
        public void updateMeshNetwork(final MeshMessage message) {
            synchronized (MeshManagerApi.this) {
                final ProvisionedMeshNode meshNode = mMeshNetwork.getNode(message.getSrc());
                updateNetwork(meshNode);
            }
        }

        @Override
        public void onMeshNodeReset(final ProvisionedMeshNode meshNode) {
            synchronized (MeshManagerApi.this) {
                if (meshNode != null) {
                    if (mMeshNetwork.deleteNode(meshNode)) {
                        deleteNode(meshNode);
                    }
                }
            }
        }

        @Override
        public MeshNetwork getMeshNetwork() {
            synchronized (MeshManagerApi.this) {
                return mMeshNetwork;
            }
        }

        @Override
        public void storeScene(final int address, final int currentScene, final List<Integer> scenes) {
            synchronized (MeshManagerApi.this) {
                final Scene scene = mMeshNetwork.getScene(currentScene);
                if (scene != null && !scene.getAddresses().contains(address)) {
                    scene.addresses.add(address);
                }
            }
        }

        @Override
        public void deleteScene(final int address, final int currentScene, final List<Integer> scenes) {
            synchronized (MeshManagerApi.this) {
                final Scene scene = mMeshNetwork.getScene(currentScene);
                if (scene != null && scene.getAddresses().contains(address)) {
                    scene.addresses.remove((Integer) address);
                }
            }
        }

        private void updateNetwork(final ProvisionedMeshNode meshNode) {
            synchronized (MeshManagerApi.this) {
                if (meshNode != null) {
                    for (int i = 0; i < mMeshNetwork.nodes.size(); i++) {
                        if (meshNode.getUnicastAddress() == mMeshNetwork.nodes.get(i).getUnicastAddress()) {
                            mMeshNetwork.nodes.set(i, meshNode);
                            break;
                        }
                    }
                }
                mMeshNetwork.setTimestamp(System.currentTimeMillis());
                // mMeshNetworkDb.update(mMeshNetwork, mMeshNetworkDao, mNetworkKeysDao, mApplicationKeysDao, mProvisionersDao, mProvisionedNodesDao,
                //    mGroupsDao, mScenesDao);
                mMeshManagerCallbacks.onNetworkUpdated(mMeshNetwork);
            }
        }
    };

    private byte[] applySegmentation(final int mtuSize, final byte[] pdu) {
        int srcOffset = 0;
        int dstOffset = 0;
        final int chunks = (pdu.length + (mtuSize - 1)) / mtuSize;

        final int pduType = pdu[0];
        if (chunks > 1) {
            final byte[] segmentedBuffer = new byte[pdu.length + chunks - 1];
            int length;
            for (int i = 0; i < chunks; i++) {
                if (i == 0) {
                    length = Math.min(pdu.length - srcOffset, mtuSize);
                    System.arraycopy(pdu, srcOffset, segmentedBuffer, dstOffset, length);
                    segmentedBuffer[0] = (byte) ((GATT_SAR_START << 6) | pduType);
                } else if (i == chunks - 1) {
                    length = Math.min(pdu.length - srcOffset, mtuSize);
                    segmentedBuffer[dstOffset] = (byte) ((GATT_SAR_END << 6) | pduType);
                    System.arraycopy(pdu, srcOffset, segmentedBuffer, dstOffset + 1, length);
                } else {
                    length = Math.min(pdu.length - srcOffset, mtuSize - 1);
                    segmentedBuffer[dstOffset] = (byte) ((GATT_SAR_CONTINUATION << 6) | pduType);
                    System.arraycopy(pdu, srcOffset, segmentedBuffer, dstOffset + 1, length);
                }
                srcOffset += length;
                dstOffset += mtuSize;
            }
            return segmentedBuffer;
        }
        return pdu;
    }

    private byte[] removeSegmentation(final int mtuSize, final byte[] data) {
        int srcOffset = 0;
        int dstOffset = 0;
        final int chunks = (data.length + (mtuSize - 1)) / mtuSize;
        if (chunks > 1) {
            final byte[] buffer = new byte[data.length - (chunks - 1)];
            int length;
            for (int i = 0; i < chunks; i++) {
                // when removing segmentation bits we only remove the start because the pdu type would be the same for each segment.
                // Therefore we can ignore this pdu type byte as they are already put together in the ble
                length = Math.min(buffer.length - dstOffset, mtuSize);
                if (i == 0) {
                    System.arraycopy(data, srcOffset, buffer, dstOffset, length);
                    buffer[0] = (byte) (buffer[0] & GATT_SAR_UNMASK);
                } else if (i == chunks - 1) {
                    System.arraycopy(data, srcOffset + 1, buffer, dstOffset, length);
                } else {
                    length = length - 1;
                    System.arraycopy(data, srcOffset + 1, buffer, dstOffset, length);
                }
                srcOffset += mtuSize;
                dstOffset += length;
            }
            return buffer;
        }
        return data;
    }

    private void insertNetwork(final MeshNetwork meshNetwork) {
        meshNetwork.setLastSelected(true);
        //If there is only one provisioner we default to the zeroth
        if (meshNetwork.provisioners.size() == 1) {
            meshNetwork.provisioners.get(0).setLastSelected(true);
        }
        /*
        mMeshNetworkDb.insertNetwork(mMeshNetworkDao,
            mNetworkKeysDao,
            mApplicationKeysDao,
            mProvisionersDao,
            mProvisionedNodesDao,
            mGroupsDao, mScenesDao,
            meshNetwork);
         */
    }

    private void initBouncyCastle() {
        Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 1);
    }
}
