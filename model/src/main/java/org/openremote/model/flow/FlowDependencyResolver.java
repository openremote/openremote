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

import java.util.*;

public abstract class FlowDependencyResolver {

    public void populateDependencies(Flow flow, boolean hydrate) {
        flow.clearDependencies();

        List<FlowDependency> superDependencies = new ArrayList<>();
        populateSuperDependencies(flow, 0, superDependencies);
        Collections.reverse(superDependencies);
        flow.setSuperDependencies(superDependencies.toArray(new FlowDependency[superDependencies.size()]));

        List<FlowDependency> subDependencies = new ArrayList<>();
        populateSubDependencies(flow, hydrate, 0, subDependencies);
        flow.setSubDependencies(subDependencies.toArray(new FlowDependency[subDependencies.size()]));
    }

    public void updateDependencies(Flow flow, boolean flowWillBeDeleted) {
        if (flowWillBeDeleted)
            flow.removeProducerConsumerNodes();

        flow.clearDependencies();
        populateDependencies(flow, false);

        for (FlowDependency superDependency : flow.getDirectSuperDependencies()) {
            Flow superFlow = findFlow(superDependency.getId());

            boolean superFlowModified = false;
            boolean superFlowShouldBeStopped = false;

            for (Node subflowNode : superFlow.findSubflowNodes()) {
                if (!subflowNode.getSubflowId().equals(flow.getId()))
                    continue;

                if (flowWillBeDeleted) {
                    // Easy case, the flow will be deleted, so delete subflow node and all its wires
                    superFlow.removeNode(subflowNode);
                    superFlowModified = true;
                    superFlowShouldBeStopped = true;
                } else {

                    // Find slots we no longer have and delete them and their wires (also include any slots without wires)
                    Slot[] slotsWithoutPeer = superFlow.findSlotsWithoutPeer(subflowNode, flow, false);
                    for (Slot slotWithoutPeer : slotsWithoutPeer) {

                        // Slots with a property path must be ignored, they are not mapped to peers by definition
                        if (slotWithoutPeer.getPropertyPath() != null)
                            continue;

                        if (superFlow.removeSlot(subflowNode, slotWithoutPeer.getId())) {
                            superFlowModified = true;
                            superFlowShouldBeStopped = true;
                        }
                    }

                    // All other slots which are still valid, update the label
                    for (Slot subflowSlot : subflowNode.getSlots()) {

                        // Slots with a property path must be ignored, they are not mapped to peers by definition
                        if (subflowSlot.getPropertyPath() != null)
                            continue;

                        Node peerNode = flow.findOwnerNode(subflowSlot.getPeerId());
                        if (peerNode.getLabel() != null && !peerNode.getLabel().equals(subflowSlot.getLabel())) {
                            subflowSlot.setLabel(peerNode.getLabel());
                            superFlowModified = true;
                            superFlowShouldBeStopped = false;
                        }
                    }

                    // Add new slots for any new consumers/producers
                    List<Slot> newSlots = new ArrayList<>();
                    Node[] consumers = flow.findNodes(Node.TYPE_CONSUMER);
                    for (Node consumer : consumers) {
                        Slot firstSink = consumer.findSlots(Slot.TYPE_SINK)[0];
                        if (subflowNode.findSlotWithPeer(firstSink.getId()) == null)
                            newSlots.add(new Slot(generateGlobalUniqueId(), firstSink, consumer.getLabel()));
                    }
                    Node[] producers = flow.findNodes(Node.TYPE_PRODUCER);
                    for (Node producer : producers) {
                        Slot firstSource = producer.findSlots(Slot.TYPE_SOURCE)[0];
                        if (subflowNode.findSlotWithPeer(firstSource.getId()) == null)
                            newSlots.add(new Slot(generateGlobalUniqueId(), firstSource, producer.getLabel()));
                    }
                    if (newSlots.size() > 0) {
                        subflowNode.addSlots(newSlots.toArray(new Slot[newSlots.size()]));
                        superFlowModified = true;
                        superFlowShouldBeStopped = false;
                    }
                }
            }

            if (superFlowModified) {
                superFlow.clearDependencies();
                if (superFlowShouldBeStopped)
                    stopFlowIfRunning(superFlow);
                storeFlow(superFlow);
            }
        }
    }

    protected void populateSuperDependencies(Flow flow, int level, List<FlowDependency> dependencyList) {

        Flow[] subflowDependents = findSubflowDependents(flow.getId());

        for (Flow subflowDependent : subflowDependents) {

            // Is this dependency breakable (wires attached?) or already broken (invalid peers?)
            boolean flowHasWiresAttached = false;
            boolean flowHasInvalidPeers = false;

            Node[] subflowNodes = subflowDependent.findSubflowNodes();

            for (Node subflowNode : subflowNodes) {
                if (!subflowNode.getSubflowId().equals(flow.getId()))
                    continue;

                // Is the subflow node attached to any other node or does nobody care if we replace it silently?
                boolean nodeHasWires = subflowDependent.findWiresAttachedToNode(subflowNode).length > 0;

                // Are any of its (wired) slot peers no longer in the current flow?
                boolean nodeHasMissingPeers = false;
                if (nodeHasWires) {
                    nodeHasMissingPeers = subflowDependent.findSlotsWithoutPeer(subflowNode, flow, true).length > 0;
                }

                flowHasWiresAttached = flowHasWiresAttached || nodeHasWires;
                flowHasInvalidPeers = flowHasInvalidPeers || nodeHasMissingPeers;
            }

            dependencyList.add(
                new FlowDependency(
                    subflowDependent.getLabel(),
                    subflowDependent.getId(),
                    subflowDependent.getType(),
                    level,
                    flowHasWiresAttached,
                    flowHasInvalidPeers
                )
            );

            populateSuperDependencies(subflowDependent, level + 1, dependencyList);
        }
    }

    protected void populateSubDependencies(Flow flow, boolean hydrate, int level, List<FlowDependency> dependencyList) {

        Set<String> added = new HashSet<>();

        Node[] subflowNodes = flow.findSubflowNodes();

        for (Node subflowNode : subflowNodes) {
            Flow subflow = findFlow(subflowNode.getSubflowId());

            if (subflow == null)
                throw new IllegalStateException(
                    "Missing subflow dependency '" + subflowNode.getSubflowId() + "': in " + subflowNode
                );

            // Do we have a loop?
            if (subflow.getId().equals(flow.getId())) {
                throw new IllegalStateException(
                    "Loop detected, can't have flow as its own subflow: " + flow
                );
            }
            for (FlowDependency superDependency : flow.getSuperDependencies()) {
                if (subflow.getId().equals(superDependency.getId())) {
                    throw new IllegalStateException(
                        "Loop detected in '" + flow + "', subflow is also super dependency: " + subflow
                    );
                }
            }

            if (!added.contains(subflow.getId())) {
                dependencyList.add(
                    new FlowDependency(
                        subflow.getLabel(),
                        subflow.getId(),
                        subflow.getType(),
                        hydrate ? subflow : null,
                        level
                    )
                );
                added.add(subflow.getId());
                populateSubDependencies(subflow, hydrate, level + 1, dependencyList);
            }

        }
    }

    protected abstract String generateGlobalUniqueId();

    protected abstract Flow findFlow(String flowId);

    protected abstract Flow[] findSubflowDependents(String flowId);

    protected abstract void stopFlowIfRunning(Flow flow);

    protected abstract void storeFlow(Flow flow);

}