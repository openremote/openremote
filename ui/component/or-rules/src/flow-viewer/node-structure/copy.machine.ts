/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import { IdentityAssigner } from "./identity.assigner";
import { Node, NodeSocket } from "@openremote/model";

export class CopyMachine {
    public static copy(node: Node): Node {
        const minimalNode: Node = {};

        minimalNode.inputs = (node.inputs || []).map((i: NodeSocket) => {
            return {
                name: i.name, type: i.type
            };
        });

        minimalNode.internals = node.internals || [];
        minimalNode.name = node.name;
        minimalNode.displayCharacter = node.displayCharacter;

        minimalNode.outputs = (node.outputs || []).map((i) => {
            return {
                name: i.name, type: i.type
            };
        });

        minimalNode.type = node.type;
        minimalNode.position = { x: 0, y: 0 };
        minimalNode.size = { x: 0, y: 0 };

        const clone: Node = JSON.parse(JSON.stringify(minimalNode));
        clone.id = IdentityAssigner.generateIdentity();
        clone.inputs!.forEach((socket) => {
            socket.nodeId = clone.id;
            socket.id = IdentityAssigner.generateIdentity();
        });

        clone.outputs!.forEach((socket) => {
            socket.nodeId = clone.id;
            socket.id = IdentityAssigner.generateIdentity();
        });

        return clone;
    }
}
