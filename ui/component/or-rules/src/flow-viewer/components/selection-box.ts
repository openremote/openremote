import { LitElement, html, css } from "lit";
import {customElement, property} from "lit/decorators.js";
import { Utilities } from "../utils";
import { EditorWorkspace } from "./editor-workspace";
import { input } from "./flow-editor";

@customElement("selection-box")
export class SelectionBox extends LitElement {

    public static get styles() {
        return css`
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
    @property({ attribute: false }) public workspace!: EditorWorkspace;

    public readonly distanceTreshold = 20;

    @property({ attribute: false }) private isSelecting = false;

    @property({ attribute: false }) private selectionStartPosition = { x: 0, y: 0 };
    @property({ attribute: false }) private selectionBox = { x: 0, y: 0, width: 0, height: 0 };

    protected firstUpdated() {
        this.workspace.addEventListener("mousedown", this.workspaceMouseDown);
        window.addEventListener("mouseup", this.workspaceMouseUp);
    }

    protected render() {
        if (!this.isSelecting) { return html``; }
        return html`<svg><rect x="${this.selectionBox.x}px" y="${this.selectionBox.y}px" width="${this.selectionBox.width}px" height="${this.selectionBox.height}px"></rect></svg>`;
    }

    private workspaceMouseDown = (e: MouseEvent) => {
        if (e.buttons !== 1) { return; }
        this.selectionBox.x = e.offsetX;
        this.selectionBox.y = e.offsetY;
        this.selectionStartPosition.x = this.selectionBox.x;
        this.selectionStartPosition.y = this.selectionBox.y;
        window.addEventListener("mousemove", this.mouseMove);
    }

    private mouseMove = (e: MouseEvent) => {
        const width = e.pageX - this.workspace.clientRect.left - this.selectionStartPosition.x;
        const height = e.pageY - this.workspace.clientRect.top - this.selectionStartPosition.y;

        if (width < 0) {
            this.selectionBox.x = e.pageX - this.workspace.clientRect.left;
            this.selectionBox.width = Math.abs(this.selectionBox.x - this.selectionStartPosition.x);
        } else {
            this.selectionBox.x = this.selectionStartPosition.x;
            this.selectionBox.width = Math.abs(width);
        }

        if (height < 0) {
            this.selectionBox.y = e.pageY - this.workspace.clientRect.top;
            this.selectionBox.height = Math.abs(this.selectionBox.y - this.selectionStartPosition.y);
        } else {
            this.selectionBox.y = this.selectionStartPosition.y;
            this.selectionBox.height = Math.abs(height);
        }
        if (this.selectionBox.width + this.selectionBox.height > this.distanceTreshold) { this.isSelecting = true; }

        this.requestUpdate();
    }

    private workspaceMouseUp = () => {
        window.removeEventListener("mousemove", this.mouseMove);
        if (this.isSelecting) {
            this.selectInBounds();
            this.isSelecting = false;
        }
    }

    private selectInBounds() {
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
}
