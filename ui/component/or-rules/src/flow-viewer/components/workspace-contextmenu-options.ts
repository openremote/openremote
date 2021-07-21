import { EditorWorkspace } from "./editor-workspace";
import { FlowNode } from "./flow-node";
import { ConnectionLine } from "../flow-viewer";
import { NodeType } from "@openremote/model";
import { ContextMenuButton, ContextMenuSeparator } from "../models/context-menu-button";
import { integration, project, copyPasteManager, input, exporter, modal } from "./flow-editor";
import i18next from "i18next";
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
                label: i18next.t(node.name!, Utilities.humanLike(node.name!)),
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
            label: i18next.t("output", "Ouput"),
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
