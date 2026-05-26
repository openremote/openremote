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
import rest from "@openremote/rest";
import { Node } from "@openremote/model";
import { EventEmitter } from "events";

export class Integration extends EventEmitter {
    public nodes: Node[] = [];
    
    public async refreshNodes() {
        this.nodes = [];
        const allNodes = (await rest.api.FlowResource.getAllNodeDefinitions()).data;
        for (const n of allNodes) {
            this.nodes.push(n);
        }
    }
}
