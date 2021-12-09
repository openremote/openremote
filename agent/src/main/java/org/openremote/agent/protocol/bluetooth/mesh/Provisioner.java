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
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Class definition of a Provisioner of mesh network
 */
public class Provisioner {

    private String meshUuid;

    private String provisionerUuid;

    private String provisionerName = "nRF Mesh Provisioner";

    List<AllocatedUnicastRange> allocatedUnicastRanges = new ArrayList<>();

    List<AllocatedGroupRange> allocatedGroupRanges = new ArrayList<>();

    List<AllocatedSceneRange> allocatedSceneRanges = new ArrayList<>();

    private Integer provisionerAddress = null;

    private int globalTtl = 5;

    private boolean lastSelected;

    private final Comparator<AddressRange> addressRangeComparator = (addressRange1, addressRange2) ->
        Integer.compare(addressRange1.getLowAddress(), addressRange2.getLowAddress());

    private final Comparator<AllocatedSceneRange> sceneRangeComparator = (sceneRange1, sceneRange2) ->
        Integer.compare(sceneRange1.getFirstScene(), sceneRange2.getFirstScene());

    /**
     * Constructs {@link Provisioner}
     */
    public Provisioner(final String provisionerUuid,
                       final List<AllocatedUnicastRange> allocatedUnicastRanges,
                       final List<AllocatedGroupRange> allocatedGroupRanges,
                       final List<AllocatedSceneRange> allocatedSceneRanges,
                       final String meshUuid) {

        this.provisionerUuid = provisionerUuid.toUpperCase(Locale.US);
        this.allocatedUnicastRanges = allocatedUnicastRanges;
        this.allocatedGroupRanges = allocatedGroupRanges;
        this.allocatedSceneRanges = allocatedSceneRanges;
        this.meshUuid = meshUuid;
    }
    /**
     * Returns the provisionerUuid of the Mesh network
     *
     * @return String provisionerUuid
     */
    public String getMeshUuid() {
        return meshUuid;
    }

    /**
     * Sets the provisionerUuid of the mesh network to this application key
     *
     * @param uuid mesh network provisionerUuid
     */
    public void setMeshUuid(final String uuid) {
        meshUuid = uuid;
    }

    /**
     * Returns the provisioner name
     *
     * @return name
     */
    public String getProvisionerName() {
        return provisionerName;
    }

    /**
     * Sets a friendly name to a provisioner
     *
     * @param provisionerName friendly name
     */
    public void setProvisionerName(final String provisionerName) throws IllegalArgumentException {
        if (provisionerName == null || provisionerName.length() == 0)
            throw new IllegalArgumentException("Name cannot be empty");
        this.provisionerName = provisionerName;
    }

    /**
     * Returns the provisionerUuid
     *
     * @return UUID
     */
    public String getProvisionerUuid() {
        return provisionerUuid;
    }

    public void setProvisionerUuid(final String provisionerUuid) {
        this.provisionerUuid = provisionerUuid;
    }

    /**
     * Returns {@link AllocatedGroupRange} for this provisioner
     *
     * @return allocated range of group addresses
     */
    public List<AllocatedGroupRange> getAllocatedGroupRanges() {
        return Collections.unmodifiableList(allocatedGroupRanges);
    }

    /**
     * Sets {@link AllocatedGroupRange} for this provisioner
     *
     * @param allocatedGroupRanges allocated range of group addresses
     */
    public void setAllocatedGroupRanges(final List<AllocatedGroupRange> allocatedGroupRanges) {
        this.allocatedGroupRanges = allocatedGroupRanges;
    }

    /**
     * Returns {@link AllocatedUnicastRange} for this provisioner
     *
     * @return allocated range of unicast addresses
     */
    public List<AllocatedUnicastRange> getAllocatedUnicastRanges() {
        return Collections.unmodifiableList(allocatedUnicastRanges);
    }

    /**
     * Sets {@link AllocatedGroupRange} for this provisioner
     *
     * @param allocatedUnicastRanges allocated range of unicast addresses
     */
    public void setAllocatedUnicastRanges(final List<AllocatedUnicastRange> allocatedUnicastRanges) {
        this.allocatedUnicastRanges = allocatedUnicastRanges;
    }

    /**
     * Returns {@link AllocatedSceneRange} for this provisioner
     *
     * @return allocated range of unicast addresses
     */
    public List<AllocatedSceneRange> getAllocatedSceneRanges() {
        return Collections.unmodifiableList(allocatedSceneRanges);
    }

    /**
     * Sets {@link AllocatedSceneRange} for this provisioner
     *
     * @param allocatedSceneRanges allocated range of unicast addresses
     */
    public void setAllocatedSceneRanges(final List<AllocatedSceneRange> allocatedSceneRanges) {
        this.allocatedSceneRanges = allocatedSceneRanges;
    }

    public Integer getProvisionerAddress() {
        return provisionerAddress;
    }

    /**
     * Set provisioner address
     *
     * @param address address of the provisioner
     */
    public void setProvisionerAddress(final Integer address) throws IllegalArgumentException {
        if (address != null && !MeshAddress.isValidUnicastAddress(address)) {
            throw new IllegalArgumentException("Unicast address must range between 0x0001 to 0x7FFF.");
        }
        this.provisionerAddress = address;
    }

    /**
     * Assigns provisioner address
     *
     * @param address address of the provisioner
     */
    public boolean assignProvisionerAddress(final Integer address) throws IllegalArgumentException {
        if (address != null && !MeshAddress.isValidUnicastAddress(address)) {
            throw new IllegalArgumentException("Unicast address must range between 0x0001 to 0x7FFF.");
        }
        if (isAddressWithinAllocatedRange(address)) {
            this.provisionerAddress = address;
            return true;
        } else {
            throw new IllegalArgumentException("Address must be within the allocated address range.");
        }
    }

    public int getGlobalTtl() {
        return globalTtl;
    }

    /**
     * Returns true if the provisioner is allowed to configure the network
     */
    public boolean supportsConfiguration() {
        return provisionerAddress != null;
    }

    /**
     * Set the ttl of the provisioner
     *
     * @param ttl ttl
     * @throws IllegalArgumentException if invalid ttl value is set
     */
    public void setGlobalTtl(final int ttl) throws IllegalArgumentException {
        if (!MeshParserUtils.isValidTtl(ttl))
            throw new IllegalArgumentException("Invalid ttl, ttl must range from 0 - 127");
        this.globalTtl = ttl;
    }

    public boolean isLastSelected() {
        return lastSelected;
    }

    public void setLastSelected(final boolean lastSelected) {
        this.lastSelected = lastSelected;
    }

    /**
     * Add a range to the provisioner
     *
     * @param allocatedRange {@link Range}
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean addRange(final Range allocatedRange) {
        if (allocatedRange instanceof AllocatedUnicastRange) {
            allocatedUnicastRanges.add((AllocatedUnicastRange) allocatedRange);
            final ArrayList<AllocatedUnicastRange> ranges = new ArrayList<>(allocatedUnicastRanges);
            Collections.sort(ranges, addressRangeComparator);
            allocatedUnicastRanges.clear();
            allocatedUnicastRanges.addAll(Range.mergeUnicastRanges(ranges));
            return true;
        } else if (allocatedRange instanceof AllocatedGroupRange) {
            allocatedGroupRanges.add((AllocatedGroupRange) allocatedRange);
            final ArrayList<AllocatedGroupRange> ranges = new ArrayList<>(allocatedGroupRanges);
            Collections.sort(ranges, addressRangeComparator);
            allocatedGroupRanges.clear();
            allocatedGroupRanges.addAll(Range.mergeGroupRanges(ranges));
            return true;
        } else if (allocatedRange instanceof AllocatedSceneRange) {
            allocatedSceneRanges.add((AllocatedSceneRange) allocatedRange);
            final ArrayList<AllocatedSceneRange> ranges = new ArrayList<>(allocatedSceneRanges);
            Collections.sort(allocatedSceneRanges, sceneRangeComparator);
            allocatedSceneRanges.clear();
            allocatedSceneRanges.addAll(Range.mergeSceneRanges(ranges));
            return true;
        }
        return false;
    }

    /**
     * Add a range to the provisioner
     *
     * @param range {@link Range}
     */
    public boolean removeRange(final Range range) {
        if (range instanceof AllocatedUnicastRange) {
            return allocatedUnicastRanges.remove(range);
        } else if (range instanceof AllocatedGroupRange) {
            return allocatedGroupRanges.remove(range);
        } else if (range instanceof AllocatedSceneRange) {
            return allocatedSceneRanges.remove(range);
        }
        return false;
    }

    /**
     * Checks if a given Unicast Address is within an allocated unicast address range
     *
     * @param address Unicast Address
     * @return true if it is within a range or false otherwise
     * @throws IllegalArgumentException if address is invalid or out of range
     */
    boolean isAddressWithinAllocatedRange(final Integer address) throws IllegalArgumentException {
        if (address == null)
            return true;

        if (!MeshAddress.isValidUnicastAddress(address)) {
            throw new IllegalArgumentException("Unicast address must range from 0x0001 - 0x7FFF");
        }

        for (AllocatedUnicastRange range : allocatedUnicastRanges) {
            if (address >= range.getLowAddress() && address <= range.getHighAddress())
                return true;
        }
        return false;
    }

    public boolean hasOverlappingUnicastRanges(final List<AllocatedUnicastRange> otherRanges) {
        for (AllocatedUnicastRange range : allocatedUnicastRanges) {
            for (AllocatedUnicastRange other : otherRanges) {
                if (range.overlaps(other)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasOverlappingGroupRanges(final List<AllocatedGroupRange> otherRanges) {
        for (AllocatedGroupRange range : allocatedGroupRanges) {
            for (AllocatedGroupRange other : otherRanges) {
                if (range.overlaps(other)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasOverlappingSceneRanges(final List<AllocatedSceneRange> otherRanges) {
        for (AllocatedSceneRange range : allocatedSceneRanges) {
            for (AllocatedSceneRange other : otherRanges) {
                if (range.overlaps(other)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean isNodeAddressInUse(final List<ProvisionedMeshNode> nodes) {
        if (provisionerAddress == null)
            return false;

        for (ProvisionedMeshNode node : nodes) {
            if (!node.getUuid().equalsIgnoreCase(provisionerUuid)) {
                if (node.getUnicastAddress() == provisionerAddress) {
                    return true;
                }
            }
        }
        return false;
    }
}
