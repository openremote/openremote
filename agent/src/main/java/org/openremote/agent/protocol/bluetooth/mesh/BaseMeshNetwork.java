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

import org.openremote.agent.protocol.bluetooth.mesh.transport.ProvisionedMeshNode;
import org.openremote.agent.protocol.bluetooth.mesh.transport.ConfigNetKeyUpdate;
import org.openremote.agent.protocol.bluetooth.mesh.transport.ConfigAppKeyUpdate;
import org.openremote.agent.protocol.bluetooth.mesh.transport.ConfigKeyRefreshPhaseSet;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;
import org.openremote.agent.protocol.bluetooth.mesh.utils.ProxyFilter;
import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;
import org.openremote.agent.protocol.bluetooth.mesh.NetworkKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;


abstract public class BaseMeshNetwork {

    public static final Logger LOG = Logger.getLogger(BaseMeshNetwork.class.getName());

    // Key refresh phases
    public static final int NORMAL_OPERATION = 0; //Normal operation
    public static final int IV_UPDATE_ACTIVE = 1; //IV Update active

    final String meshUUID;
    protected final Comparator<ApplicationKey> appKeyComparator = (key1, key2) -> Integer.compare(key1.getKeyIndex(), key2.getKeyIndex());
    protected final Comparator<NetworkKey> netKeyComparator = (key1, key2) -> Integer.compare(key1.getKeyIndex(), key2.getKeyIndex());
    protected MeshNetworkCallbacks mCallbacks;
    String schema = "http://json-schema.org/draft-04/schema#";
    String id = "http://www.bluetooth.com/specifications/assigned-numbers/mesh-profile/cdb-schema.json#";
    String version = "1.0";
    String meshName = "nRF Mesh Network";
    long timestamp = System.currentTimeMillis();
    boolean partial = false;
    IvIndex ivIndex = new IvIndex(0, false, Calendar.getInstance());
    List<NetworkKey> netKeys = new ArrayList<>();
    List<ApplicationKey> appKeys = new ArrayList<>();
    List<Provisioner> provisioners = new ArrayList<>();
    List<ProvisionedMeshNode> nodes = new ArrayList<>();
    List<Group> groups = new ArrayList<>();
    List<Scene> scenes = new ArrayList<>();
    protected Map<Integer, ArrayList<Integer>> networkExclusions = new HashMap<>();
    //Library related attributes
    int unicastAddress = 0x0001;
    boolean lastSelected;
    //protected SparseIntArray sequenceNumbers = new SparseIntArray();
    protected Map<Integer, Integer> sequenceNumbers = new HashMap<>();
    private ProxyFilter proxyFilter;
    protected final Comparator<ProvisionedMeshNode> nodeComparator = (node1, node2) ->
        Integer.compare(node1.getUnicastAddress(), node2.getUnicastAddress());
    protected final Comparator<Group> groupComparator = (group1, group2) ->
        Integer.compare(group1.getAddress(), group2.getAddress());
    // protected final Comparator<Scene> sceneComparator = (scene1, scene2) ->
    //    Integer.compare(scene1.getNumber(), scene2.getNumber());
    protected final Comparator<AllocatedUnicastRange> unicastRangeComparator = (range1, range2) ->
        Integer.compare(range1.getLowAddress(), range2.getLowAddress());
    protected final Comparator<AllocatedGroupRange> groupRangeComparator = (range1, range2) ->
        Integer.compare(range1.getLowAddress(), range2.getLowAddress());
    protected final Comparator<AllocatedSceneRange> sceneRangeComparator = (range1, range2) ->
        Integer.compare(range1.getFirstScene(), range2.getFirstScene());

    BaseMeshNetwork(final String meshUUID) {
        this.meshUUID = meshUUID;
    }

    private boolean isNetKeyExists(final byte[] key) {
        for (int i = 0; i < netKeys.size(); i++) {
            if (Arrays.equals(key, netKeys.get(i).getKey())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates an Network key
     *
     * @return {@link NetworkKey}
     * @throws IllegalArgumentException in case the generated application key already exists
     */
    public synchronized NetworkKey createNetworkKey() throws IllegalArgumentException {
        final NetworkKey key = new NetworkKey(getAvailableNetKeyIndex(), MeshParserUtils.toByteArray(SecureUtils.generateRandomNetworkKey()));
        key.setMeshUuid(meshUUID);
        return key;
    }

    /**
     * Adds a Net key to the list of net keys with the given key index
     *
     * @param newNetKey Network key
     * @throws IllegalArgumentException if the key already exists.
     */
    public synchronized boolean addNetKey(final NetworkKey newNetKey) {
        if (isNetKeyExists(newNetKey.getKey())) {
            throw new IllegalArgumentException("Net key already exists, check the contents of the key!");
        } else {
            newNetKey.setMeshUuid(meshUUID);
            netKeys.add(newNetKey);
            notifyNetKeyAdded(newNetKey);
        }
        return true;
    }

    private int getAvailableNetKeyIndex() {
        if (netKeys.isEmpty()) {
            return 0;
        } else {
            Collections.sort(netKeys, netKeyComparator);
            final int index = netKeys.size() - 1;
            return netKeys.get(index).getKeyIndex() + 1;
        }
    }

    /**
     * Update a network key with the given 16-byte hexadecimal string in the mesh network.
     *
     * <p>
     * Updating a NetworkKey's key value requires initiating a Key Refresh Procedure. A NetworkKey that's in use
     * would require a Key Refresh Procedure to update it's key contents. However a NetworkKey that's not in could
     * be updated without this procedure. If the key is in use, call {@link #distributeNetKey(NetworkKey, byte[])}
     * to initiate the Key Refresh Procedure.
     * </p>
     *
     * @param networkKey Network key
     * @param newNetKey  16-byte hexadecimal string
     * @throws IllegalArgumentException if the key is already in use
     */
    public synchronized boolean updateNetKey(final NetworkKey networkKey, final String newNetKey) throws IllegalArgumentException {
        if (MeshParserUtils.validateKeyInput(newNetKey)) {
            final byte[] key = MeshParserUtils.toByteArray(newNetKey);
            if (isNetKeyExists(key)) {
                throw new IllegalArgumentException("Net key value is already in use.");
            }

            final int keyIndex = networkKey.getKeyIndex();
            final NetworkKey netKey = getNetKey(keyIndex);
            if (!isKeyInUse(netKey)) {
                //We check if the contents of the key are the same
                //This will return true only if the key index and the key are the same
                if (netKey.equals(networkKey)) {
                    netKey.setKey(key);
                    return updateMeshKey(netKey);
                } else {
                    return false;
                }
            } else {
                throw new IllegalArgumentException("Unable to update a network key that's already in use. ");
            }
        }
        return false;
    }

    /**
     * Update a network key in the mesh network.
     *
     * <p>
     * Updating a NetworkKey's key value requires initiating a Key Refresh Procedure. A NetworkKey that's in use
     * would require a Key Refresh Procedure to update it's key contents. However a NetworkKey that's not in could
     * be updated without this procedure. If the key is in use, call {@link #distributeNetKey(NetworkKey, byte[])}
     * to initiate the Key Refresh Procedure.
     * </p>
     *
     * @param networkKey Network key
     * @throws IllegalArgumentException if the key is already in use
     */
    public synchronized boolean updateNetKey(final NetworkKey networkKey) throws IllegalArgumentException {
        final int keyIndex = networkKey.getKeyIndex();
        final NetworkKey key = getNetKey(keyIndex);
        //We check if the contents of the key are the same
        //This will return true only if the key index and the key are the same
        if (key.equals(networkKey)) {
            // The name might be updated so we must update the key.
            return updateMeshKey(networkKey);
        } else {
            //If the keys are not the same we check if its in use before updating the key
            if (!isKeyInUse(key)) {
                //We check if the contents of the key are the same
                //This will return true only if the key index and the key are the same
                return updateMeshKey(networkKey);
            } else {
                throw new IllegalArgumentException("Unable to update a network key that's already in use.");
            }
        }
    }

    /**
     * Distribute Net Key will start the key refresh procedure and return the newly updated key.
     *
     * <p>
     * This process contains three phases.
     * {@link NetworkKey#KEY_DISTRIBUTION} - Distribution of the new Keys {@link #distributeNetKey(NetworkKey, byte[])}.
     * {@link NetworkKey#USING_NEW_KEYS} - Switching to the new keys {@link #switchToNewKey(NetworkKey)}.
     * {@link NetworkKey#REVOKE_OLD_KEYS} - Revoking old keys {@link #revokeOldKey(NetworkKey)}.
     * The new key is distributed to the provisioner node by setting the currently used key as the old key and setting the
     * currently used key to the new key value. This will change the phase of the network key to{@link NetworkKey#KEY_DISTRIBUTION}.
     * During this phase a node will transmit using the old key but may receive using both old and the new key. After a successful
     * distribution to the provisioner, the user may start sending {@link ConfigNetKeyUpdate} messages to the respective nodes in the
     * network that requires updating. In addition if the user wishes to update the AppKey call {@link #distributeAppKey(ApplicationKey, byte[])}
     * to update the Application Key on the provisioner and then distribute it to other nodes by sending {@link ConfigAppKeyUpdate} to
     * update an AppKey. However it shall be only successfully processed if the NetworkKey bound to the Application Key is in
     * {@link NetworkKey#KEY_DISTRIBUTION} and the received app key value is different or when the received AppKey value is the same as
     * previously received value. Also note that sending a ConfigNetKeyUpdate during {@link NetworkKey#NORMAL_OPERATION} will switch the
     * phase to {@link NetworkKey#KEY_DISTRIBUTION}. Once distribution is completed, call {@link #switchToNewKey(NetworkKey)} and
     * send {@link ConfigKeyRefreshPhaseSet} to other nodes.
     * </p>
     *
     * @param networkKey Network key
     * @param newNetKey  16-byte key
     * @throws IllegalArgumentException the key value is already in use.
     */
    public synchronized NetworkKey distributeNetKey(final NetworkKey networkKey, final byte[] newNetKey) throws IllegalArgumentException {
        if (validateKey(newNetKey)) {
            if (isNetKeyExists(newNetKey)) {
                throw new IllegalArgumentException("Net key value is already in use.");
            }

            final int keyIndex = networkKey.getKeyIndex();
            final NetworkKey netKey = getNetKey(keyIndex);
            if (netKey.equals(networkKey)) {
                if (netKey.distributeKey(newNetKey)) {
                    updateNodeKeyStatus(netKey);
                    if (updateMeshKey(netKey)) {
                        return netKey;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Updates the NodeKey object for Network or Application Keys
     *
     * @param meshKey Updated Key
     */
    private void updateNodeKeyStatus(final MeshKey meshKey) {
        for (Provisioner provisioner : provisioners) {
            for (ProvisionedMeshNode node : nodes) {
                if (node.getUuid().equalsIgnoreCase(provisioner.getProvisionerUuid())) {
                    if (meshKey instanceof NetworkKey) {
                        for (NodeKey key : node.getAddedNetKeys()) {
                            if (key.getIndex() == meshKey.getKeyIndex()) {
                                key.setUpdated(true);
                            }
                        }
                    } else {
                        for (NodeKey key : node.getAddedAppKeys()) {
                            if (key.getIndex() == meshKey.getKeyIndex()) {
                                key.setUpdated(true);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Switches the new key, this will initiate the provisioner node transmitting messages using the new keys but will
     * support receiving messages using both old and the new key.
     *
     * <p>
     * This must be called after {@link #distributeNetKey(NetworkKey, byte[])}
     * </p>
     *
     * @param networkKey Network key to switch too
     * @return true if success or false otherwise
     * @throws IllegalArgumentException if the provided key is not the same as the distributed key.
     */
    public synchronized boolean switchToNewKey(final NetworkKey networkKey) throws IllegalArgumentException {
        if (!netKeys.contains(networkKey)) {
            throw new IllegalArgumentException("Network Key not distributed");
        }
        return networkKey.switchToNewKey();
    }

    /**
     * Revokes the old key.
     * <p>
     * This initiates {@link NetworkKey#REVOKE_OLD_KEYS} of the Key Refresh Procedure in which user must send {@link ConfigKeyRefreshPhaseSet}
     * message with transition set to {@link NetworkKey#REVOKE_OLD_KEYS} to the other nodes going through the Key Refresh Procedure.
     * The library at this point will set the given Network Key's Phase to {@link NetworkKey#NORMAL_OPERATION}.
     * </p>
     *
     * @param networkKey Network key that was distributed
     * @return true if success or false otherwise
     */
    public boolean revokeOldKey(final NetworkKey networkKey) {
        if (netKeys.contains(networkKey)) {
            return networkKey.revokeOldKey();
        }
        return false;
    }


    /**
     * Removes a network key from the network key list
     *
     * @param networkKey key to be removed
     * @throws IllegalArgumentException if the key is in use or if it does not exist in the list of keys
     */
    public synchronized boolean removeNetKey(final NetworkKey networkKey) throws IllegalArgumentException {
        if (!isKeyInUse(networkKey)) {
            if (netKeys.remove(networkKey)) {
                notifyNetKeyDeleted(networkKey);
                return true;
            } else {
                throw new IllegalArgumentException("Key does not exist.");
            }
        }
        throw new IllegalArgumentException("Unable to delete a network key that's already in use.");
    }

    /**
     * Returns an application key with a given key index
     *
     * @param keyIndex index
     */
    public synchronized NetworkKey getNetKey(final int keyIndex) {
        for (NetworkKey key : netKeys) {
            if (keyIndex == key.getKeyIndex()) {
                try {
                    return key.clone();
                } catch (CloneNotSupportedException e) {
                    LOG.severe("Error while cloning key: " + e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * Creates an application key
     *
     * @return {@link ApplicationKey}
     * @throws IllegalArgumentException in case the generated application key already exists
     */
    public synchronized ApplicationKey createAppKey() throws IllegalArgumentException {
        if (netKeys.isEmpty()) {
            throw new IllegalStateException("Cannot create an App Key without a Network key. Consider creating a network key first");
        }

        final ApplicationKey key = new ApplicationKey(getAvailableAppKeyIndex(), MeshParserUtils.toByteArray(SecureUtils.generateRandomApplicationKey()));
        key.setMeshUuid(meshUUID);
        return key;
    }

    /**
     * Adds an app key to the list of keys with the given key index. If there is an existing key with the same index,
     * an illegal argument exception is thrown.
     *
     * @param newAppKey application key
     * @throws IllegalArgumentException if app key already exists
     */
    public synchronized boolean addAppKey(final ApplicationKey newAppKey) {
        if (netKeys.isEmpty()) {
            throw new IllegalStateException("Cannot create an App Key without a Network key. Consider creating a network key first");
        }

        if (isAppKeyExists(newAppKey.getKey())) {
            throw new IllegalArgumentException("App key already exists, check the contents of the key!");
        } else {
            newAppKey.setMeshUuid(meshUUID);
            appKeys.add(newAppKey);
            notifyAppKeyAdded(newAppKey);
        }
        return true;
    }

    private int getAvailableAppKeyIndex() {
        if (appKeys.isEmpty()) {
            return 0;
        } else {
            Collections.sort(appKeys, appKeyComparator);
            final int index = appKeys.size() - 1;
            return appKeys.get(index).getKeyIndex() + 1;
        }
    }

    /**
     * Returns an application key with a given key index
     *
     * @param keyIndex index
     */
    public synchronized ApplicationKey getAppKey(final int keyIndex) {
        for (ApplicationKey key : appKeys) {
            if (keyIndex == key.getKeyIndex()) {
                try {
                    return key.clone();
                } catch (CloneNotSupportedException e) {
                    LOG.severe("Error while cloning key: " + e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * Returns a list of application keys bound to a Network key
     *
     * @param boundNetKeyIndex Network Key index
     */
    protected synchronized List<ApplicationKey> getAppKeys(final int boundNetKeyIndex) {
        final List<ApplicationKey> applicationKeys = new ArrayList<>();
        for (ApplicationKey applicationKey : appKeys) {
            if (applicationKey.getBoundNetKeyIndex() == boundNetKeyIndex) {
                applicationKeys.add(applicationKey);
            }
        }
        return applicationKeys;
    }

    private boolean isAppKeyExists(final byte[] appKey) {
        for (int i = 0; i < appKeys.size(); i++) {
            final ApplicationKey applicationKey = appKeys.get(i);
            if (Arrays.equals(applicationKey.getKey(), appKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates an app key with a given key in the mesh network.
     *
     * <p>
     * Updates the Key if it is not use, if not Updating a Key's key value requires initiating a Key Refresh Procedure.
     * This requires the bound NetworkKey of the AppKey to be updated. A NetworkKey that's in use would require a
     * Key Refresh Procedure to update it's key contents. However a NetworkKey that's not in could be updated without this
     * procedure. If the key is in use, call {@link #distributeNetKey(NetworkKey, byte[])} to initiate the Key Refresh Procedure.
     * </p>
     *
     * @param applicationKey {@link ApplicationKey}
     * @param newAppKey      Application key
     * @throws IllegalArgumentException if the key is in use.
     */
    public synchronized boolean updateAppKey(final ApplicationKey applicationKey, final String newAppKey) throws IllegalArgumentException {
        if (MeshParserUtils.validateKeyInput(newAppKey)) {
            final byte[] key = MeshParserUtils.toByteArray(newAppKey);
            if (isNetKeyExists(key)) {
                throw new IllegalArgumentException("Net key already in use");
            }

            final int keyIndex = applicationKey.getKeyIndex();
            final ApplicationKey appKey = getAppKey(keyIndex);
            if (!isKeyInUse(appKey)) {
                //We check if the contents of the key are the same
                //This will return true only if the key index and the key are the same
                if (appKey.equals(applicationKey)) {
                    appKey.setKey(key);
                    return updateMeshKey(appKey);
                } else {
                    return false;
                }
            } else {
                throw new IllegalArgumentException("Unable to update a application key that's already in use.");
            }
        }
        return false;
    }

    /**
     * Updates an app key in the mesh network.
     *
     * <p>
     * Updates the Key if it is not use, if not Updating a Key's key value requires initiating a Key Refresh Procedure. This requires
     * the bound NetworkKey of the AppKey to be updated. A NetworkKey that's in use would require aKey Refresh Procedure to update
     * it's key contents. However a NetworkKey that's not in could be updated without this procedure. If the key is in use, call
     * {@link #distributeNetKey(NetworkKey, byte[])} to initiate the Key Refresh Procedure. After distributing the NetworkKey bound to
     * the Application Key, user may call {@link #distributeAppKey(ApplicationKey, byte[])} to update the corresponding ApplicationKey.
     * </p>
     *
     * @param applicationKey {@link ApplicationKey}
     * @throws IllegalArgumentException if the key is already in use
     */
    public synchronized boolean updateAppKey(final ApplicationKey applicationKey) throws IllegalArgumentException {
        final int keyIndex = applicationKey.getKeyIndex();
        final ApplicationKey key = getAppKey(keyIndex);
        //If the keys are not the same we check if its in use before updating the key
        if (!isKeyInUse(key)) {
            //We check if the contents of the key are the same
            //This will return true only if the key index and the key are the same
            return updateMeshKey(applicationKey);
        } else {
            throw new IllegalArgumentException("Unable to update a application key that's already in use.");
        }
    }

    /**
     * Distributes/updates the provisioner node's the application key and returns the updated Application Key.
     *
     * <p>
     * This will only work if the NetworkKey bound to this ApplicationKey is in Phase 1 of the Key Refresh Procedure. Therefore the NetworkKey
     * must be updated first before updating it's bound application key. Call {@link #distributeNetKey(NetworkKey, byte[])} to initiate the
     * Key Refresh Procedure to update a Network Key that's in use by the provisioner or the nodes, if it has not been started already.
     * To update a key that's not in use call {@link #updateAppKey(ApplicationKey, String)}
     * <p>
     * Once the provisioner nodes' AppKey is updated user must distribute the updated AppKey to the nodes. This can be done by sending
     * {@link ConfigAppKeyUpdate} message with the new key.
     * </p>
     *
     * @param applicationKey Network key
     * @param newAppKey      16-byte key
     * @throws IllegalArgumentException the key value is already in use.
     */
    public synchronized ApplicationKey distributeAppKey(final ApplicationKey applicationKey, final byte[] newAppKey) throws IllegalArgumentException {
        if (validateKey(newAppKey)) {
            if (isAppKeyExists(newAppKey)) {
                throw new IllegalArgumentException("App key value is already in use.");
            }

            final int keyIndex = applicationKey.getKeyIndex();
            final ApplicationKey appKey = getAppKey(keyIndex);
            if (appKey.equals(applicationKey)) {
                if(appKey.distributeKey(newAppKey)){
                    updateNodeKeyStatus(appKey);
                    if (updateMeshKey(appKey)) {
                        return appKey;
                    }
                }
            }
        }
        return null;
    }

    private boolean updateMeshKey(final MeshKey key) {
        if (key instanceof ApplicationKey) {
            ApplicationKey appKey = null;
            for (int i = 0; i < appKeys.size(); i++) {
                final ApplicationKey tempKey = appKeys.get(i);
                if (tempKey.getKeyIndex() == key.getKeyIndex()) {
                    appKey = (ApplicationKey) key;
                    appKeys.set(i, appKey);
                    break;
                }
            }
            if (appKey != null) {
                notifyAppKeyUpdated(appKey);
                return true;
            }
        } else {
            NetworkKey netKey = null;
            for (int i = 0; i < netKeys.size(); i++) {
                final NetworkKey tempKey = netKeys.get(i);
                if (tempKey.getKeyIndex() == key.getKeyIndex()) {
                    netKey = (NetworkKey) key;
                    netKeys.set(i, netKey);
                    break;
                }
            }
            if (netKey != null) {
                netKey.setTimestamp(System.currentTimeMillis());
                notifyNetKeyUpdated(netKey);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes an app key from the app key list
     *
     * @param appKey app key to be removed
     * @throws IllegalArgumentException if the key is in use or if it does not exist in the list of keys
     */
    public synchronized boolean removeAppKey(final ApplicationKey appKey) throws IllegalArgumentException {
        if (isKeyInUse(appKey)) {
            throw new IllegalArgumentException("Unable to delete an app key that's in use.");
        } else {
            if (appKeys.remove(appKey)) {
                notifyAppKeyDeleted(appKey);
                return true;
            } else {
                throw new IllegalArgumentException("Key does not exist.");
            }
        }
    }

    /**
     * Checks if the app key is in use.
     *
     * <p>
     * This will check if the specified app key is added to a node other than the selected provisioner node
     * </p>
     *
     * @param meshKey {@link MeshKey}
     */
    public synchronized boolean isKeyInUse(final MeshKey meshKey) {
        for (ProvisionedMeshNode node : nodes) {
            if (!node.getUuid().equalsIgnoreCase(getSelectedProvisioner().getProvisionerUuid())) {
                final int index = meshKey.getKeyIndex();
                //We need to check if a key index is in use by checking in the added net/app key indexes
                if (meshKey instanceof ApplicationKey) {
                    return MeshParserUtils.isNodeKeyExists(node.getAddedAppKeys(), index);
                } else {
                    return MeshParserUtils.isNodeKeyExists(node.getAddedNetKeys(), index);
                }
            }
        }
        return false;
    }

    /**
     * Creates a provisioner
     *
     * @param name         Provisioner name
     * @param unicastRange {@link AllocatedUnicastRange} for the provisioner
     * @param groupRange   {@link AllocatedGroupRange} for the provisioner
     * @param sceneRange   {@link AllocatedSceneRange} for the provisioner
     * @return {@link Provisioner}
     * @throws IllegalArgumentException if the name is empty
     */
    public synchronized Provisioner createProvisioner(final String name,
                                         final AllocatedUnicastRange unicastRange,
                                         final AllocatedGroupRange groupRange,
                                         final AllocatedSceneRange sceneRange) throws IllegalArgumentException {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        final List<AllocatedUnicastRange> unicastRanges = new ArrayList<>();
        final List<AllocatedGroupRange> groupRanges = new ArrayList<>();
        final List<AllocatedSceneRange> sceneRanges = new ArrayList<>();
        unicastRanges.add(unicastRange != null ? unicastRange : new AllocatedUnicastRange(0x0001, 0x7FFF));
        groupRanges.add(groupRange != null ? groupRange : new AllocatedGroupRange(0xC000, 0xFEFF));
        sceneRanges.add(sceneRange != null ? sceneRange : new AllocatedSceneRange(0x0001, 0xFFFF));
        final Provisioner provisioner = new Provisioner(UUID.randomUUID().toString(), unicastRanges, groupRanges, sceneRanges, meshUUID);
        provisioner.setProvisionerName(name);
        return provisioner;
    }

    /**
     * Selects a provisioner if there are multiple provisioners.
     *
     * @param provisioner {@link Provisioner}
     */
    public synchronized final void selectProvisioner(final Provisioner provisioner) {
        provisioner.setLastSelected(true);
        for (Provisioner prov : provisioners) {
            if (!prov.getProvisionerUuid().equalsIgnoreCase(provisioner.getProvisionerUuid())) {
                prov.setLastSelected(false);
            }
        }
        notifyProvisionersUpdated(provisioners);
    }

    /**
     * Adds a provisioner to the network
     *
     * @param provisioner {@link Provisioner}
     * @throws IllegalArgumentException if unicast address is invalid, in use by a node
     */
    public synchronized boolean addProvisioner(final Provisioner provisioner) throws IllegalArgumentException {

        if (provisioner.allocatedUnicastRanges.isEmpty()) {
            if (provisioner.getProvisionerAddress() != null) {
                throw new IllegalArgumentException("Provisioner has no allocated unicast range assigned.");
            }
        }

        for (Provisioner other : provisioners) {
            if (provisioner.hasOverlappingUnicastRanges(other.getAllocatedUnicastRanges())
                || provisioner.hasOverlappingGroupRanges(other.getAllocatedGroupRanges())
                || provisioner.hasOverlappingSceneRanges(other.getAllocatedSceneRanges())) {
                throw new IllegalArgumentException("Provisioner ranges overlap.");
            }
        }

        if (!provisioner.isAddressWithinAllocatedRange(provisioner.getProvisionerAddress())) {
            throw new IllegalArgumentException("Unicast address assigned to a provisioner must be within an allocated unicast address range.");
        }

        if (isAddressInUse(provisioner.getProvisionerAddress())) {
            throw new IllegalArgumentException("Unicast address is in use by another node.");
        }

        if (provisioner.isNodeAddressInUse(nodes)) {
            throw new IllegalArgumentException("Unicast address is already in use.");
        }

        if (isProvisionerUuidInUse(provisioner.getProvisionerUuid())) {
            throw new IllegalArgumentException("Provisioner uuid already in use.");
        }

        provisioner.assignProvisionerAddress(provisioner.getProvisionerAddress());
        provisioners.add(provisioner);
        notifyProvisionerAdded(provisioner);
        if (provisioner.isLastSelected()) {
            selectProvisioner(provisioner);
        }
        if (provisioner.getProvisionerAddress() != null) {
            final ProvisionedMeshNode node = new ProvisionedMeshNode(provisioner, netKeys, appKeys);
            nodes.add(node);
            notifyNodeAdded(node);
        }
        return true;
    }

    /**
     * Returns the mesh node with the corresponding unicast address
     *
     * @param unicastAddress unicast address of the node
     */
    public synchronized ProvisionedMeshNode getNode(final int unicastAddress) {
        for (ProvisionedMeshNode node : nodes) {
            if (node.hasUnicastAddress(unicastAddress)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Returns the mesh node with the corresponding unicast address
     *
     * @param uuid unicast address of the node
     */
    public synchronized ProvisionedMeshNode getNode(final String uuid) {
        for (ProvisionedMeshNode node : nodes) {
            if (node.getUuid().equalsIgnoreCase(uuid)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Loads the sequence numbers known to the network
     */
    protected synchronized void loadSequenceNumbers() {
        for (ProvisionedMeshNode node : nodes) {
            sequenceNumbers.put(node.getUnicastAddress(), node.getSequenceNumber());
        }
    }

    private boolean isAddressInUse(final Integer address) {
        if (address == null)
            return false;

        for (ProvisionedMeshNode node : nodes) {
            if (address == node.getUnicastAddress()) {
                return true;
            }
        }
        return false;
    }

    protected synchronized boolean isProvisionerUuidInUse(final String uuid) {
        for (Provisioner provisioner : provisioners) {
            if (provisioner.getProvisionerUuid().equalsIgnoreCase(uuid)) {
                return true;
            }
        }
        return false;
    }




    final synchronized void notifyProvisionersUpdated(final List<Provisioner> provisioner) {
        if (mCallbacks != null) {
            mCallbacks.onProvisionersUpdated(provisioner);
        }
    }

    final synchronized void notifyProvisionerAdded(final Provisioner provisioner) {
        if (mCallbacks != null) {
            mCallbacks.onProvisionerAdded(provisioner);
        }
    }

    final synchronized void notifyNodeAdded(final ProvisionedMeshNode node) {
        if (mCallbacks != null) {
            mCallbacks.onNodeAdded(node);
        }
    }

    final synchronized void notifyNodeDeleted(final ProvisionedMeshNode meshNode) {
        if (mCallbacks != null) {
            mCallbacks.onNodeDeleted(meshNode);
        }
    }

    final synchronized void notifyProvisionerDeleted(final Provisioner provisioner) {
        if (mCallbacks != null) {
            mCallbacks.onProvisionerDeleted(provisioner);
        }
    }

    final synchronized void notifyNodeUpdated(final ProvisionedMeshNode node) {
        if (mCallbacks != null) {
            mCallbacks.onNodeUpdated(node);
        }
    }

    final synchronized void notifyNetworkUpdated() {
        if (mCallbacks != null) {
            mCallbacks.onMeshNetworkUpdated();
        }
    }

    final synchronized void notifyNetKeyAdded(final NetworkKey networkKey) {
        if (mCallbacks != null) {
            mCallbacks.onNetworkKeyAdded(networkKey);
        }
    }

    final synchronized void notifyNetKeyDeleted(final NetworkKey networkKey) {
        if (mCallbacks != null) {
            mCallbacks.onNetworkKeyDeleted(networkKey);
        }
    }

    final synchronized void notifyAppKeyAdded(final ApplicationKey appKey) {
        if (mCallbacks != null) {
            mCallbacks.onApplicationKeyAdded(appKey);
        }
    }

    final synchronized void notifyAppKeyUpdated(final ApplicationKey appKey) {
        if (mCallbacks != null) {
            mCallbacks.onApplicationKeyUpdated(appKey);
        }
    }

    final synchronized void notifyNetKeyUpdated(final NetworkKey networkKey) {
        if (mCallbacks != null) {
            mCallbacks.onNetworkKeyUpdated(networkKey);
        }
    }

    final synchronized void notifyAppKeyDeleted(final ApplicationKey appKey) {
        if (mCallbacks != null) {
            mCallbacks.onApplicationKeyDeleted(appKey);
        }
    }

    private boolean validateKey(final byte[] key) {
        if (key.length != 16)
            throw new IllegalArgumentException("Key must be 16 bytes");
        return true;
    }


    /**
     * Returns the list of {@link Provisioner}
     */
    public synchronized List<Provisioner> getProvisioners() {
        return Collections.unmodifiableList(provisioners);
    }

    /**
     * Returns the selected provisioner in the network
     */
    public synchronized Provisioner getSelectedProvisioner() {
        for (Provisioner provisioner : provisioners) {
            if (provisioner.isLastSelected()) {
                return provisioner;
            }
        }
        return null;
    }

    /**
     * Returns the list of {@link ProvisionedMeshNode}
     */
    public synchronized List<ProvisionedMeshNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    /**
     * Returns the map of network exclusions
     */
    public synchronized Map<Integer, ArrayList<Integer>> getNetworkExclusions() {
        return Collections.unmodifiableMap(networkExclusions);
    }

    /**
     * Returns the {@link ProxyFilter} set on the proxy
     */
    /* @Nullable */
    public synchronized ProxyFilter getProxyFilter() {
        return proxyFilter;
    }

    /**
     * Sets the {@link ProxyFilter} settings on the proxy
     * <p>
     * Please note that this is not persisted within the node since the filter is reinitialized to a whitelist filter upon connecting to a proxy node.
     * Therefore after setting a proxy filter and disconnecting users will have to manually
     * <p/>
     */
    public synchronized void setProxyFilter(final ProxyFilter proxyFilter) {
        this.proxyFilter = proxyFilter;
    }

    /**
     * Deletes a mesh node from the list of provisioned nodes
     *
     * <p>
     * Note that deleting a node manually will not reset the node, but only be deleted from the stored list of provisioned nodes.
     * However you may still be able to connect to the same node, if it was not reset since the network may still exist. This
     * would be useful to in case if a node was physically reset and needs to be removed from the mesh network/db
     * </p>
     *
     * @param meshNode node to be deleted
     * @return true if deleted and false otherwise
     */
    public synchronized boolean deleteNode(ProvisionedMeshNode meshNode) {
        //Let's go through the nodes and delete if a node exists
        boolean nodeDeleted = false;
        for (ProvisionedMeshNode node : nodes) {
            if (node.getUuid().equalsIgnoreCase(meshNode.getUuid())) {
                excludeNode(node);
                nodes.remove(node);
                notifyNodeDeleted(node);
                nodeDeleted = true;
                break;
            }
        }
        //We must also check if there is a provisioner based on the node we deleted
        if (nodeDeleted) {
            for (Provisioner provisioner : provisioners) {
                if (provisioner.getProvisionerUuid().equalsIgnoreCase(meshNode.getUuid())) {
                    provisioners.remove(provisioner);
                    notifyProvisionerDeleted(provisioner);
                    break;
                }
            }
        }

        return nodeDeleted;
    }

    /**
     * Excludes a node from the mesh network.
     * The given node will marked as excluded and added to the exclusion list and the node will be removed once
     * the Key Refresh Procedure is completed. After the IV update procedure, when the network transitions to an
     * IV Normal Operation state with a higher IV index, the exclusionList object that has the ivIndex property
     * value that is lower by a count of two (or more) than the current IV index of the network is removed from
     * the networkExclusions property array.
     *
     * @param node Provisioned mesh node.
     */
    private void excludeNode(final ProvisionedMeshNode node) {
        //Exclude node
        node.setExcluded(true);
        notifyNodeUpdated(node);
        ArrayList<Integer> addresses = networkExclusions.get(ivIndex.getIvIndex());
        if (addresses == null) {
            addresses = new ArrayList<>();
        }

        for (Integer address : node.getElements().keySet()) {
            if (!addresses.contains(address)) {
                addresses.add(address);
            }
        }

        networkExclusions.put(ivIndex.getIvIndex(), addresses);
        notifyNetworkUpdated();
    }



}
