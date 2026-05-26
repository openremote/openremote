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
import { EditorWorkspace } from "./editor-workspace";
import { FlowNode } from "./flow-node";
import { ConnectionLine } from "../flow-viewer";
import { NodeType } from "@openremote/model";
import { ContextMenuButton, ContextMenuSeparator } from "../models/context-menu-button";
import { integration, project, copyPasteManager, input, exporter, modal } from "./flow-editor";
import {i18next} from "@openremote/or-translate"
import { Utilities } from "../utils";
import { CopyMachine } from "../node-structure";
import { ContextMenu } from "./context-menu";
import { html } from "lit";

export const createContextMenuButtons = (workspace: EditorWorkspace, e: MouseEvent) => {
    const selectedNodes = input.selected.filter((s) => s instanceof FlowNode && s.selected) as FlowNode[];
    const selectedConnections = input.selected.filter((s) => s instanceof ConnectionLine && s.selected) as ConnectionLine[];
    const createNodeButtons = (type: NodeType) => {
        let nB: (ContextMenuButton | ContextMenuSeparator)[] = [
            {
                type: "button",
                label: "",
                icon: "arrow-left",
                action: () => { workspace.dispatchEvent(new MouseEvent("contextmenu", e)); }
            }
        ];
        nB = nB.concat(integration.nodes.filter((n) => n.type === type).map((node) => {
            const b: ContextMenuButton = {
                type: "button",
                label: i18next.t("flow."+node.name!, Utilities.humanLike(node.name!)),
                action: () => {
                    const copy = CopyMachine.copy(node);
                    copy.position = workspace.offsetToWorld({ x: e.offsetX - workspace.offsetLeft, y: e.offsetY - workspace.offsetTop });
                    project.createUndoSnapshot();
                    project.addNode(copy);
                }
            };
            return b;
        }));
        return nB;
    };

    let buttons: (ContextMenuButton | ContextMenuSeparator)[] = [
        {
            type: "button",
            label: i18next.t("input", "Input"),
            icon: "arrow-collapse-right",
            action: () => { ContextMenu.open(e.pageX, e.pageY, workspace, createNodeButtons(NodeType.INPUT)); }
        },
        {
            type: "button",
            label: i18next.t("processors", "Processors"),
            icon: "cog",
            action: () => { ContextMenu.open(e.pageX, e.pageY, workspace, createNodeButtons(NodeType.PROCESSOR)); }
        },
        {
            type: "button",
            label: i18next.t("output", "Output"),
            icon: "arrow-expand-right",
            action: () => { ContextMenu.open(e.pageX, e.pageY, workspace, createNodeButtons(NodeType.OUTPUT)); }
        },
    ];

    buttons = buttons.concat([
        { type: "separator" },
        {
            type: "button",
            icon: "content-copy",
            label: i18next.t("copy", "Copy"),
            action: () => {
                const pos = workspace.offsetToWorld({ x: e.offsetX, y: e.offsetY });
                copyPasteManager.copy(pos.x, pos.y);
            },
            disabled: selectedNodes.length === 0
        },
        {
            type: "button",
            icon: "content-paste",
            label: i18next.t("paste", "Paste"),
            action: () => {
                project.createUndoSnapshot();
                const pos = workspace.offsetToWorld({ x: e.offsetX, y: e.offsetY });
                project.notifyChange();
                workspace.pasteAt(pos.x, pos.y);
            },
            disabled: copyPasteManager.isFull
        },
        {
            type: "button",
            icon: "delete",
            label: i18next.t("delete", "Delete"),
            action: () => {
                project.createUndoSnapshot();
                selectedNodes.forEach((n) => {
                    if (n.frozen) { return; }
                    project.removeNode(n.node);
                });
                selectedConnections.forEach((n) => project.removeConnection(n.connection));
                project.notifyChange();
            },
            disabled: (selectedNodes.length === 0 && selectedConnections.length === 0) || (selectedNodes.length !== 0 && selectedNodes.every((n) => n.frozen))
        },
        { type: "separator" },
        {
            type: "button",
            icon: "fit-to-page-outline",
            label: i18next.t("fitViewToSelectedNodes", "Fit view to selected nodes"),
            action: () => workspace.fitCamera(selectedNodes.map((n) => n.node)),
            disabled: selectedNodes.length === 0
        },
        {
            type: "button",
            icon: "snowflake",
            label: "TOGGLE FROZEN [DEBUG FUNCTION]",
            action: () => selectedNodes.forEach((n) => n.frozen = !n.frozen),
            disabled: selectedNodes.length === 0
        },
        {type: "separator"},
        {
            type: "button",
            icon: "export",
            label: "Export to JSON",
            action: () => {
                modal.anything("Export result", html`
                <div style="user-select: all;font-family: monospace; padding: 5px; overflow: auto; background: whitesmoke; max-width:50vw; max-height:50vh">
                ${exporter.flowToJson(project.toNodeCollection("Exported on " + new Date(), ""))}
                </div>
                `);
            },
        },
    ]);
    return buttons;
};
