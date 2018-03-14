/*
 * Copyright 2015, OpenRemote Inc.
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

package org.openremote.model.flow;

import javax.persistence.*;
import java.util.*;


@Entity
@Table(name = "FLOW")
public class Flow extends FlowObject {

    public static final String TYPE = "urn:openremote:flow";

    @Version
    public int version;


    @Column(name = "CREATED_ON")
    @Temporal(TemporalType.TIMESTAMP)
    public Date createdOn = new Date();

    @Transient
    public Node[] nodes = new Node[0];

    @Transient
    public Wire[] wires = new Wire[0];

    @Transient
    public FlowDependency[] superDependencies = new FlowDependency[0];

    @Transient
    public FlowDependency[] subDependencies = new FlowDependency[0];


    protected Flow() {
    }


    public Flow(String label, String id) {
        super(label, id, TYPE);
    }


    public Flow(String label, String id, Node... nodes) {
        this(label, id);
        this.nodes = nodes;
    }


    public Flow(String label, String id, Node[] nodes, Wire[] wires) {
        this(label, id);
        this.nodes = nodes;
        this.wires = wires;
        if (wires != null) {
            // Consistency check, important to avoid duplicate wires
            Set<Wire> wireSet = new HashSet<>(Arrays.asList(wires));
            if (wireSet.size() != wires.length)
                throw new IllegalArgumentException("Duplicate wires: " + Arrays.toString(wires));
        }
    }


    public Date getCreatedOn() {
        return createdOn;
    }

    public Node[] getNodes() {
        return nodes;
    }

    public Node[] findNodes(String type) {
        List<Node> collection = new ArrayList<>(Arrays.asList(getNodes()));
        collection.removeIf(node -> !node.isOfType(type));
        return collection.toArray(new Node[collection.size()]);
    }

    public Node[] findConsumerProducerNodes() {
        Set<Node> collection = new HashSet<>();
        for (Node node : getNodes()) {
            if (node.isOfTypeConsumerOrProducer())
                collection.add(node);
        }
        return collection.toArray(new Node[collection.size()]);
    }

    public Node[] findClientWidgetNodes() {
        Set<Node> collection = new HashSet<>();
        for (Node node : getNodes()) {
            if (node.isClientWidget())
                collection.add(node);
        }
        return collection.toArray(new Node[collection.size()]);
    }

    public void addNode(Node node) {
        Set<Node> collection = new HashSet<>(Arrays.asList(getNodes()));
        collection.add(node);
        this.nodes = collection.toArray(new Node[collection.size()]);
    }

    public Wire[] removeNode(Node node) {
        Set<Node> collection = new HashSet<>(Arrays.asList(getNodes()));
        collection.remove(node);
        this.nodes = collection.toArray(new Node[collection.size()]);
        return removeWiresOf(node);
    }

    public Node[] removeProducerConsumerNodes() {
        Set<Node> collection = new HashSet<>(Arrays.asList(getNodes()));
        Node[] consumersProducers = findConsumerProducerNodes();
        for (Node consumersProducer : consumersProducers) {
            removeNode(consumersProducer);
        }
        return collection.toArray(new Node[collection.size()]);
    }

    public void clearDependencies() {
        setSuperDependencies(new FlowDependency[0]);
        setSubDependencies(new FlowDependency[0]);
    }

    public FlowDependency[] getSuperDependencies() {
        return superDependencies;
    }

    public void setSuperDependencies(FlowDependency[] superDependencies) {
        this.superDependencies = superDependencies;
    }

    public FlowDependency[] getSubDependencies() {
        return subDependencies;
    }

    public void setSubDependencies(FlowDependency[] subDependencies) {
        this.subDependencies = subDependencies;
    }

    public FlowDependency findSubDependency(Node subflowNode) {
        for (FlowDependency subDependency : getSubDependencies()) {
            if (subDependency.getId().equals(subflowNode.getSubflowId()))
                return subDependency;
        }
        return null;
    }

    public FlowDependency[] getDirectSuperDependencies() {
        List<FlowDependency> list = new ArrayList<>();
        for (FlowDependency flowDependency : getSuperDependencies()) {
            if (flowDependency.getLevel() == 0)
                list.add(flowDependency);
        }
        return list.toArray(new FlowDependency[list.size()]);
    }

    public boolean hasDirectWiredSuperDependencies() {
        for (FlowDependency superDependency : getDirectSuperDependencies()) {
            if (superDependency.isWired())
                return true;
        }
        return false;
    }

    public Wire[] getWires() {
        return wires;
    }

    public Wire addWireBetweenSlots(Slot sourceSlot, Slot sinkSlot) {
        return addWire(new Wire(sourceSlot.getId(), sinkSlot.getId()));
    }

    public Wire addWire(Wire wire) {
        for (Wire existing : getWires()) {
            if (existing.equals(wire))
                return null;
        }
        List<Wire> list = new ArrayList<>(Arrays.asList(getWires()));
        list.add(wire);
        this.wires = list.toArray(new Wire[list.size()]);
        return wire;
    }

    public Wire removeWire(Wire wire) {
        Wire removed = null;
        ArrayList<Wire> collection = new ArrayList<>(Arrays.asList(getWires()));
        Iterator<Wire> it = collection.iterator();
        while (it.hasNext()) {
            if (it.next().equals(wire)) {
                removed = wire;
                it.remove();
            }
        }
        this.wires = collection.toArray(new Wire[collection.size()]);
        return removed;
    }

    public Wire removeWireBetweenSlots(Slot sourceSlot, Slot sinkSlot) {
        Wire removed = null;
        ArrayList<Wire> collection = new ArrayList<>(Arrays.asList(getWires()));
        Iterator<Wire> it = collection.iterator();
        while (it.hasNext()) {
            Wire wire = it.next();
            if (wire.getSourceId().equals(sourceSlot.getId())
                && wire.getSinkId().equals(sinkSlot.getId())) {
                it.remove();
                removed = wire;
            }
        }
        this.wires = collection.toArray(new Wire[collection.size()]);
        return removed;
    }

    public Wire[] removeWiresOf(Node node) {
        Set<Wire> collection = new HashSet<>();
        for (Slot slot : node.getSlots()) {
            Wire[] wires = findWiresAttachedToSlot(slot.getId());
            for (Wire wire : wires) {
                collection.add(removeWire(wire));
            }
        }
        return collection.toArray(new Wire[collection.size()]);
    }

    public Slot findSlot(String slotId) {
        for (Node node : getNodes()) {
            Slot slot = node.findSlot(slotId);
            if (slot != null)
                return slot;
        }
        return null;
    }

    public boolean removeSlot(Node node, String slotId) {
        if (findNode(node.getId()) == null)
            throw new IllegalStateException("Node not in flow: " + node);
        Slot slot = node.findSlot(slotId);
        if (slot == null)
            return false;
        List<Slot> collection = new ArrayList<>(Arrays.asList(node.getSlots()));
        Iterator<Slot> it = collection.iterator();
        boolean removed = false;
        while (it.hasNext()) {
            Slot next = it.next();
            if (next.getId().equals(slotId)) {
                removed = true;
                it.remove();
                Wire[] wires = findWiresAttachedToSlot(next.getId());
                for (Wire wire : wires) {
                    removeWire(wire);
                }
                break;
            }
        }
        node.setSlots(collection.toArray(new Slot[collection.size()]));
        return removed;
    }

    public boolean hasWires(String slotId) {
        for (Wire wire : getWires()) {
            if (wire.getSourceId().equals(slotId))
                return true;
            if (wire.getSinkId().equals(slotId))
                return true;
        }
        return false;
    }

    public Wire[] findWiresAttachedToSlot(String slotId) {
        List<Wire> list = new ArrayList<>();
        for (Wire wire : getWires()) {
            if (wire.getSourceId().equals(slotId) || wire.getSinkId().equals(slotId))
                list.add(wire);
        }
        return list.toArray(new Wire[list.size()]);
    }

    public Wire[] findWiresForSource(String slotId) {
        List<Wire> list = new ArrayList<>();
        for (Wire wire : getWires()) {
            if (wire.getSourceId().equals(slotId))
                list.add(wire);
        }
        return list.toArray(new Wire[list.size()]);
    }

    public Wire[] findWiresForSink(String slotId) {
        List<Wire> list = new ArrayList<>();
        for (Wire wire : getWires()) {
            if (wire.getSinkId().equals(slotId))
                list.add(wire);
        }
        return list.toArray(new Wire[list.size()]);
    }

    public Node findNode(String nodeId) {
        for (Node node : getNodes()) {
            if (node.getId().equals(nodeId))
                return node;
        }
        return null;
    }

    public Node[] findSubflowNodes() {
        Set<Node> collection = new HashSet<>();
        for (Node node : getNodes()) {
            if (node.isOfType(Node.TYPE_SUBFLOW))
                collection.add(node);
        }
        return collection.toArray(new Node[collection.size()]);
    }

    public Slot[] findSlotsWithoutPeer(Node subflowNode, Flow flow, boolean onlyWiredSlots) {
        if (!subflowNode.isOfTypeSubflow() || !flow.getId().equals(subflowNode.getSubflowId()))
            throw new IllegalArgumentException(
                "Node '" + subflowNode + "' is not a subflow node using: " + flow
            );
        List<Slot> list = new ArrayList<>();

        Slot[] slots = subflowNode.getSlots();
        for (Slot slot : slots) {
            // Is there a wire attached?
            boolean wireAttachedToSlot = findWiresAttachedToSlot(slot.getId()).length > 0;
            if (!wireAttachedToSlot) {
                // Not going to be a problem, this slot can be removed or updated silently
                if (!onlyWiredSlots)
                    list.add(slot);
                continue;
            }

            // Do we still have the peer linked to the slot in the given flow?
            boolean peerMissing = flow.findSlot(slot.getPeerId()) == null;

            // If we no longer have its peer, then the slot and all its attached wires are broken
            if (peerMissing)
                list.add(slot);
        }
        return list.toArray(new Slot[list.size()]);
    }

    public Wire[] findWiresAttachedToNode(Node node) {
        Set<Wire> collection = new HashSet<>();
        for (Slot slot : node.getSlots()) {
            collection.addAll(Arrays.asList(findWiresAttachedToSlot(slot.getId())));
        }
        return collection.toArray(new Wire[collection.size()]);
    }

    public Wire[] findWiresBetween(Node a, Node b) {
        Set<Wire> collection = new HashSet<>();
        for (Wire wire : getWires()) {
            Node sourceOwner = findOwnerNode(wire.getSourceId());
            Node sinkOwner = findOwnerNode(wire.getSinkId());
            if ((sourceOwner.getId().equals(a.getId()) && sinkOwner.getId().equals(b.getId()))
                || (sourceOwner.getId().equals(b.getId()) && sinkOwner.getId().equals(a.getId()))) {
                collection.add(wire);
            }
        }
        return collection.toArray(new Wire[collection.size()]);
    }

    public Node findOwnerNode(String slotId) {
        for (Node node : getNodes()) {
            Slot slot = node.findSlot(slotId);
            if (slot != null)
                return node;
        }
        return null;
    }


    public void printWires(StringBuilder sb) {
        sb.append("\n").append("Wires of ").append(this).append(" => ").append(getWires().length).append("\n");
        printWires(sb, getWires());
    }


    public void printWires(StringBuilder sb, Wire[] wires) {
        for (Wire wire : wires) {
            sb.append("--------------------------------------------------------------------------------------------");
            sb.append("\n");
            Node sourceNode = findOwnerNode(wire.getSourceId());
            Node sinkNode = findOwnerNode(wire.getSinkId());
            sb.append(sourceNode).append(" => ").append(sinkNode);
            sb.append("\n");
            Slot sourceSlot = findSlot(wire.getSourceId());
            Slot sinkSlot = findSlot(wire.getSinkId());
            sb.append(sourceSlot).append(" => ").append(sinkSlot);
            sb.append("\n");
        }
    }
}
