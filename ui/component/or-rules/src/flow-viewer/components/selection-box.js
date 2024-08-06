var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { LitElement, html, css } from "lit";
import { customElement, property } from "lit/decorators.js";
import { Utilities } from "../utils";
import { input } from "./flow-editor";
let SelectionBox = class SelectionBox extends LitElement {
    constructor() {
        super(...arguments);
        this.distanceTreshold = 20;
        this.isSelecting = false;
        this.selectionStartPosition = { x: 0, y: 0 };
        this.selectionBox = { x: 0, y: 0, width: 0, height: 0 };
        this.workspaceMouseDown = (e) => {
            if (e.buttons !== 1) {
                return;
            }
            this.selectionBox.x = e.offsetX;
            this.selectionBox.y = e.offsetY;
            this.selectionStartPosition.x = this.selectionBox.x;
            this.selectionStartPosition.y = this.selectionBox.y;
            window.addEventListener("mousemove", this.mouseMove);
        };
        this.mouseMove = (e) => {
            const width = e.pageX - this.workspace.clientRect.left - this.selectionStartPosition.x;
            const height = e.pageY - this.workspace.clientRect.top - this.selectionStartPosition.y;
            if (width < 0) {
                this.selectionBox.x = e.pageX - this.workspace.clientRect.left;
                this.selectionBox.width = Math.abs(this.selectionBox.x - this.selectionStartPosition.x);
            }
            else {
                this.selectionBox.x = this.selectionStartPosition.x;
                this.selectionBox.width = Math.abs(width);
            }
            if (height < 0) {
                this.selectionBox.y = e.pageY - this.workspace.clientRect.top;
                this.selectionBox.height = Math.abs(this.selectionBox.y - this.selectionStartPosition.y);
            }
            else {
                this.selectionBox.y = this.selectionStartPosition.y;
                this.selectionBox.height = Math.abs(height);
            }
            if (this.selectionBox.width + this.selectionBox.height > this.distanceTreshold) {
                this.isSelecting = true;
            }
            this.requestUpdate();
        };
        this.workspaceMouseUp = () => {
            window.removeEventListener("mousemove", this.mouseMove);
            if (this.isSelecting) {
                this.selectInBounds();
                this.isSelecting = false;
            }
        };
    }
    static get styles() {
        return css `
        svg{
            fill: none;  
            overflow: visible;
            position: absolute;
            pointer-events: all;
            stroke-linejoin: round;
            transition: stroke-width 120ms;
            z-index: 1000;
            width: 100vw;
            height: 100vh;
        }
        rect{
            fill: var(--highlight-faded);
            outline: solid 1px var(--highlight);
        }`;
    }
    firstUpdated() {
        this.workspace.addEventListener("mousedown", this.workspaceMouseDown);
        window.addEventListener("mouseup", this.workspaceMouseUp);
    }
    render() {
        if (!this.isSelecting) {
            return html ``;
        }
        return html `<svg><rect x="${this.selectionBox.x}px" y="${this.selectionBox.y}px" width="${this.selectionBox.width}px" height="${this.selectionBox.height}px"></rect></svg>`;
    }
    selectInBounds() {
        input.clearSelection();
        const offsetBox = {
            x: this.selectionBox.x + this.workspace.clientRect.left,
            y: this.selectionBox.y + this.workspace.clientRect.top,
            width: this.selectionBox.width,
            height: this.selectionBox.height,
        };
        for (const selectable of input.selectables) {
            const rect = selectable.handle.getBoundingClientRect();
            if (Utilities.isBoxInsideBox({ x: rect.left, y: rect.top, width: rect.width, height: rect.height }, offsetBox)) {
                input.select(selectable, true);
            }
        }
    }
};
__decorate([
    property({ attribute: false })
], SelectionBox.prototype, "workspace", void 0);
__decorate([
    property({ attribute: false })
], SelectionBox.prototype, "isSelecting", void 0);
__decorate([
    property({ attribute: false })
], SelectionBox.prototype, "selectionStartPosition", void 0);
__decorate([
    property({ attribute: false })
], SelectionBox.prototype, "selectionBox", void 0);
SelectionBox = __decorate([
    customElement("selection-box")
], SelectionBox);
export { SelectionBox };
//# sourceMappingURL=selection-box.js.map