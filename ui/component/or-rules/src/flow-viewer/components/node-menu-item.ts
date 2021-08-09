import { LitElement, html, css } from "lit";
import {customElement, property} from "lit/decorators.js";
import { Node, NodeType } from "@openremote/model";
import { EditorWorkspace } from "./editor-workspace";
import { Utilities } from "../utils";
import { i18next, translate } from "@openremote/or-translate";
import { newIds, project } from "./flow-editor";
import { CopyMachine } from "../node-structure";

@customElement("node-menu-item")
export class NodeMenuItem extends translate(i18next)(LitElement) {
    @property({ attribute: false }) public node!: Node;
    @property({ attribute: false }) private isDragging = false;

    @property({ attribute: false }) private x = 0;
    @property({ attribute: false }) private y = 0;

    @property({ attribute: false }) private workspace !: EditorWorkspace;

    private xOffset = 0;
    private yOffset = 0;

    constructor() {
        super();
        this.addEventListener("mousedown", this.startDrag);
    }
    static get styles() {
        return css`
        :host, .node-drag-item{
            padding: 4px;
            margin: 0 0 15px 0;
            display: inline-block;
            text-align: center;
            color: white;
            border-radius: var(--roundness);
            box-shadow: rgba(0, 0, 0, 0.2) 0px 5px 5px -5px;

            width: calc(var(--nodepanel-width) - 4px * 2);
            height: 22px;
            line-height: 22px;
            cursor:grab;
            transition: box-shadow 150ms;
        }
        :host(.small), .small{
            width: 26px;
            height: 26px;
            line-height: 26px;
            margin: 0;
        }
        .node-drag-item{
            z-index: 5000;
            position: fixed;
            background: inherit;
            box-shadow: rgba(0, 0, 0, 0.2) 0 2px 15px;
            filter: opacity(90%);
            pointer-events: none;
        }
        `;
    }

    protected render() {
        switch (this.node.type) {
            case NodeType.INPUT:
                this.classList.add("input-node");
                break;
            case NodeType.PROCESSOR:
                this.classList.add("processor-node");
                break;
            case NodeType.OUTPUT:
                this.classList.add("output-node");
                break;
        }

        if (this.node.displayCharacter) {
            this.classList.add("small");
        }
        this.title = i18next.t(this.node.name!, Utilities.humanLike(this.node.name!));
        return html`
        <div class="label">${this.flowNodeName}</div>
        ${this.isDragging ? this.dragNodeTemplate : null}
        `;
    }

    private get dragNodeTemplate() {
        return html`<div class="node-drag-item ${(this.node.displayCharacter ? "small" : null)}" style="top: ${this.y - this.yOffset}px; left: ${this.x - this.xOffset}px"><div class="label">${this.flowNodeName}</div></div>`;
    }

    private get flowNodeName() {
        return i18next.t(this.node.displayCharacter || this.node.name!, 
            this.node.displayCharacter || Utilities.humanLike(this.node.name || "invalid node name"));
    }

    private startDrag = (e: MouseEvent) => {
        this.xOffset = e.offsetX;
        this.yOffset = e.offsetY;
        this.x = e.clientX;
        this.y = e.clientY;

        this.isDragging = true;
        window.addEventListener("mouseup", this.stopDrag);
        window.addEventListener("mousemove", this.onMove);
    }

    private onMove = (e: MouseEvent) => {
        this.x = e.clientX;
        this.y = e.clientY;
    }

    private stopDrag = (e: MouseEvent) => {
        window.removeEventListener("mouseup", this.stopDrag);
        window.removeEventListener("mousemove", this.onMove);
        this.isDragging = false;

        if (Utilities.isPointInsideBox(e.offsetX, e.offsetY, {
            x: this.workspace.clientRect.left,
            y: this.workspace.clientRect.top,
            width: this.workspace.clientRect.width,
            height: this.workspace.clientRect.height,
        })) {
            const copy = CopyMachine.copy(this.node);
            newIds.add(copy.id!);
            copy.position = this.workspace.offsetToWorld({ x: e.offsetX - this.workspace.clientRect.left, y: e.offsetY - this.workspace.clientRect.top });
            project.createUndoSnapshot();
            project.addNode(copy);
            project.notifyChange();
        }
    }
}
