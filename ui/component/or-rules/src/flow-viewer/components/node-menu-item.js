var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { LitElement, html, css } from "lit";
import { customElement, property } from "lit/decorators.js";
import { Utilities } from "../utils";
import { i18next, translate } from "@openremote/or-translate";
import { newIds, project } from "./flow-editor";
import { CopyMachine } from "../node-structure";
let NodeMenuItem = class NodeMenuItem extends translate(i18next)(LitElement) {
    constructor() {
        super();
        this.isDragging = false;
        this.x = 0;
        this.y = 0;
        this.xOffset = 0;
        this.yOffset = 0;
        this.startDrag = (e) => {
            this.xOffset = e.offsetX;
            this.yOffset = e.offsetY;
            this.x = e.clientX;
            this.y = e.clientY;
            this.isDragging = true;
            window.addEventListener("mouseup", this.stopDrag);
            window.addEventListener("mousemove", this.onMove);
        };
        this.onMove = (e) => {
            this.x = e.clientX;
            this.y = e.clientY;
        };
        this.stopDrag = (e) => {
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
                newIds.add(copy.id);
                copy.position = this.workspace.offsetToWorld({ x: e.offsetX - this.workspace.clientRect.left, y: e.offsetY - this.workspace.clientRect.top });
                project.createUndoSnapshot();
                project.addNode(copy);
                project.notifyChange();
            }
        };
        this.addEventListener("mousedown", this.startDrag);
    }
    static get styles() {
        return css `
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
    render() {
        switch (this.node.type) {
            case "INPUT" /* NodeType.INPUT */:
                this.classList.add("input-node");
                break;
            case "PROCESSOR" /* NodeType.PROCESSOR */:
                this.classList.add("processor-node");
                break;
            case "OUTPUT" /* NodeType.OUTPUT */:
                this.classList.add("output-node");
                break;
        }
        if (this.node.displayCharacter) {
            this.classList.add("small");
        }
        this.title = i18next.t(this.node.name, Utilities.humanLike(this.node.name));
        return html `
        <div class="label">${this.flowNodeName}</div>
        ${this.isDragging ? this.dragNodeTemplate : null}
        `;
    }
    get dragNodeTemplate() {
        return html `<div class="node-drag-item ${(this.node.displayCharacter ? "small" : null)}" style="top: ${this.y - this.yOffset}px; left: ${this.x - this.xOffset}px"><div class="label">${this.flowNodeName}</div></div>`;
    }
    get flowNodeName() {
        return i18next.t(this.node.displayCharacter || this.node.name, this.node.displayCharacter || Utilities.humanLike(this.node.name || "invalid node name"));
    }
};
__decorate([
    property({ attribute: false })
], NodeMenuItem.prototype, "node", void 0);
__decorate([
    property({ attribute: false })
], NodeMenuItem.prototype, "isDragging", void 0);
__decorate([
    property({ attribute: false })
], NodeMenuItem.prototype, "x", void 0);
__decorate([
    property({ attribute: false })
], NodeMenuItem.prototype, "y", void 0);
__decorate([
    property({ attribute: false })
], NodeMenuItem.prototype, "workspace", void 0);
NodeMenuItem = __decorate([
    customElement("node-menu-item")
], NodeMenuItem);
export { NodeMenuItem };
//# sourceMappingURL=node-menu-item.js.map