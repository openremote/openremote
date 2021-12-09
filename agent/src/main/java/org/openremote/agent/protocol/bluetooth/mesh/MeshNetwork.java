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

import org.openremote.agent.protocol.bluetooth.mesh.transport.Element;
import org.openremote.agent.protocol.bluetooth.mesh.transport.MeshModel;
import org.openremote.agent.protocol.bluetooth.mesh.transport.ProvisionedMeshNode;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MeshNetwork extends BaseMeshNetwork{

    public MeshNetwork(final String meshUUID) {
        super(meshUUID);
    }

    synchronized void setCallbacks(final MeshNetworkCallbacks mCallbacks) {
        this.mCallbacks = mCallbacks;
    }

    /**
     * Sets the time stamp
     *
     * @param timestamp timestamp
     */
    public synchronized void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns a list of scenes.
     */
    public synchronized List<Scene> getScenes() {
        return Collections.unmodifiableList(scenes);
    }

    /**
     * Returns a group based on the group number
     */
    /* @Nullable */
    public synchronized Group getGroup(final int address) {
        for (final Group group : groups) {
            if (address == group.getAddress()) {
                return group;
            }
        }
        return null;
    }

    public synchronized List<Group> getGroups() {
        return groups;
    }

    public synchronized String getMeshUUID() {
        return meshUUID;
    }

    synchronized void setNetKeys(final List<NetworkKey> netKeys) {
        this.netKeys = netKeys;
    }

    public synchronized List<NetworkKey> getNetKeys() {
        return Collections.unmodifiableList(netKeys);
    }

    public synchronized NetworkKey getPrimaryNetworkKey() {
        for (NetworkKey networkKey : netKeys) {
            if (networkKey.getKeyIndex() == 0) {
                return networkKey;
            }
        }
        return null;
    }

    /**
     * Returns a list of {@link ApplicationKey} belonging to the mesh network
     */
    public synchronized List<ApplicationKey> getAppKeys() {
        return Collections.unmodifiableList(appKeys);
    }

    public synchronized IvIndex getIvIndex() {
        return ivIndex;
    }

    public synchronized String getSchema() {
        return schema;
    }

    public synchronized String getId() {
        return id;
    }

    public synchronized String getVersion() {
        return version;
    }

    /**
     * Returns the name of the mesh network
     */
    public synchronized String getMeshName() {
        return meshName;
    }

    /**
     * Returns the time stamp of th e mesh network
     */
    public synchronized long getTimestamp() {
        return timestamp;
    }

    /**
     * Set the partial configuration flag to true. To be used internally
     *
     * @param partial true if the network is set to be exported partially.
     */
    public synchronized void setPartial(final boolean partial) {
        this.partial = partial;
    }

    public synchronized void setLastSelected(final boolean lastSelected) {
        this.lastSelected = lastSelected;
    }

    /**
     * Returns a scene based on the scene number.
     */
    /* @Nullable */
    public synchronized Scene getScene(final int number) {
        for (final Scene scene : scenes) {
            if (number == scene.getNumber()) {
                return scene;
            }
        }
        return null;
    }

    /**
     * Returns the uuid for a given virtual address
     *
     * @param address virtual address
     * @return The label uuid if it's known to the provisioner or null otherwise
     */
    public synchronized UUID getLabelUuid(final int address) throws IllegalArgumentException {
        if (!MeshAddress.isValidVirtualAddress(address)) {
            throw new IllegalArgumentException("Address type must be a virtual address ");
        }

        for (ProvisionedMeshNode node : nodes) {
            for (Map.Entry<Integer, Element> elementEntry : node.getElements().entrySet()) {
                final Element element = elementEntry.getValue();
                for (Map.Entry<Integer, MeshModel> modelEntry : element.getMeshModels().entrySet()) {
                    final MeshModel model = modelEntry.getValue();
                    if (model != null) {
                        if (model.getPublicationSettings() != null) {
                            if (model.getPublicationSettings().getLabelUUID() != null) {
                                if (address == MeshAddress.generateVirtualAddress(model.getPublicationSettings().getLabelUUID())) {
                                    return model.getPublicationSettings().getLabelUUID();
                                }
                            }
                        }
                        final UUID label = model.getLabelUUID(address);
                        if (label != null) {
                            return label;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the next unicast address for a node based on the number of elements
     * and the range allocated to the provisioner.
     * P.S. When setting up a new network don't forget to assign an address to the provisioner.
     * When importing a network make sure to create a new provisioner with a different address
     * which is the recommended approach. However you can also use the same provisioner
     * with a different address.
     *
     * @param elementCount Element count
     * @param provisioner  provisioner
     * @return Allocated unicast address or -1 if none
     * @throws IllegalArgumentException if there is no allocated unicast range to the provisioner
     */
    public synchronized int nextAvailableUnicastAddress(final int elementCount, final Provisioner provisioner) throws IllegalArgumentException {
        if (provisioner.getAllocatedUnicastRanges().isEmpty()) {
            throw new IllegalArgumentException("Please allocate a unicast address range to the provisioner");
        }

        // Populate all addresses that are currently in use
        final ArrayList<Integer> usedAddresses = new ArrayList<>();
        for (ProvisionedMeshNode node : nodes) {
            usedAddresses.addAll(node.getElements().keySet());
        }
        // Excluded addresses with the current IvIndex and current IvIndex - 1 must be considered as addresses in use.
        if (networkExclusions.get(ivIndex.getIvIndex()) != null)
            usedAddresses.addAll(networkExclusions.get(ivIndex.getIvIndex()));
        if (networkExclusions.get(ivIndex.getIvIndex() - 1) != null)
            usedAddresses.addAll(networkExclusions.get(ivIndex.getIvIndex() - 1));

        Collections.sort(usedAddresses);
        // Iterate through all nodes just once, while iterating over ranges.
        int index = 0;
        for (AllocatedUnicastRange range : provisioner.getAllocatedUnicastRanges()) {
            // Start from the beginning of the current range.
            int address = range.getLowAddress();

            // Iterate through nodes that weren't checked yet in essence the used addresses which include the excluded adresses
            for (int usedAddress : usedAddresses) {

                // Skip nodes with addresses below the range.
                if (address > usedAddress) {
                    continue;
                }

                // If we found a space before the current node, return the address.
                if (usedAddress > (address + (elementCount - 1))) {
                    return address;
                }

                // Else, move the address to the next available address.
                address = usedAddress + 1;

                // If the new address is outside of the range, go to the next one.
                if (range.highAddress < address + (elementCount - 1)) {
                    break;
                }
            }

            if (range.getHighAddress() >= address + (elementCount - 1)) {
                return address;
            }
        }

        // No address was found :(
        return -1;
    }
}
