/*
 * Copyright 2025, OpenRemote Inc.
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
import {TreeNode} from "./model";

/**
 * Utility function that moves an array of {@link TreeNode} into another {@link TreeNode}, by adding them to their children.
 * The function takes care of removing the nodes from the former group, and makes sure no duplicates end up in the list.
 *
 * @param nodesToMove - The array of nodes that are moved into a group.
 * @param groupNode - The group node to insert nodesToMove in.
 * @param treeNodes - Full list of nodes in the tree menu.
 */
export function moveNodesToGroupNode(nodesToMove: TreeNode[], groupNode?: TreeNode, treeNodes: TreeNode[] = []): TreeNode[] {
    console.debug(`Moving nodes '${nodesToMove.map(node => node.label).join(', ')}' into group '${groupNode?.label}'. Tree nodes are`, groupNode);

    function filterAndAdd(nodes: TreeNode[]): TreeNode[] {
        return nodes.map(node => {

            if (nodesToMove.some(nodeToMove => node.id === nodeToMove.id)) {
                console.debug("Removed the node from original position.")
                return null; // Removes the node from its original position
            }

            const newNode: TreeNode = { ...node };

            // Recursively loop through children
            if (newNode.children) {
                newNode.children = filterAndAdd(newNode.children).filter(child => child !== null);
            }

            // If the currently looped node is the target group node, add the nodes to its children
            if (groupNode && node.id === groupNode?.id) {
                nodesToMove.forEach(nodeToMove => {
                    newNode.children = newNode.children ? [...newNode.children, nodeToMove] : [nodeToMove];
                });
            }
            return newNode;

        }).filter(node => node !== null) as TreeNode[];
    }

    // Start the recursive function
    const newNodes = filterAndAdd(treeNodes);

    // If no groupNode is provided, add the nodesToMove to the top level
    if (!groupNode) {
        newNodes.push(...nodesToMove);
    }
    return newNodes;
}

