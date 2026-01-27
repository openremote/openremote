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
import { FlowNode } from "../components/flow-node";
import { ConnectionLine } from "../components/connection-line";
import { input, project } from "../components/flow-editor";

export class Shortcuts {
    public actions: { keys: string[], requireCtrl?: boolean, action: () => void }[] = [
        {
            keys: ["Delete", "Backspace"],
            action: () => {
                project.createUndoSnapshot();
                const selectedNodes = input.selected.filter((s) => s instanceof FlowNode && s.selected && !s.frozen) as FlowNode[];
                const selectedConnections = input.selected.filter((s) => s instanceof ConnectionLine && s.selected) as ConnectionLine[];
                selectedConnections.forEach((n) => project.removeConnection(n.connection));
                selectedNodes.forEach((n) => project.removeNode(n.node));
                project.notifyChange();
            }
        },
        {
            keys: ["KeyA"],
            requireCtrl: true,
            action: () => {
                if (input.selected.length > 0) {
                    input.clearSelection(true);
                } else {
                    input.selectables.forEach((s) => {
                        input.select(s, true);
                    });
                }
            }
        },
        {
            keys: ["KeyZ"],
            requireCtrl: true,
            action: () => {
                project.undo();
            }
        },
        {
            keys: ["KeyY"],
            requireCtrl: true,
            action: () => {
                project.redo();
            }
        }
    ];

    constructor() {
        window.addEventListener("keydown", (e: KeyboardEvent) => {
            if (!(document.activeElement instanceof HTMLBodyElement)) { return; }
            this.actions.forEach((a) => {
                if (!a.keys.includes(e.key) && !a.keys.includes(e.code)) { return; }
                if (a.requireCtrl && !e.ctrlKey) { return; }
                a.action();
            });
        });
    }
}
