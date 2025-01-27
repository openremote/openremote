import {css, html} from "lit";
import {customElement, property} from "lit/decorators.js";
import {Node, NodeInternal, NodeInternalBreakType} from "@openremote/model";
import {IdentityDomLink} from "../node-structure";
import {FlowNodeStyle} from "../styles/flow-node-style";
import {i18next} from "@openremote/or-translate";
import {nodeConverter} from "../converters/node-converter";
import {SelectableElement} from "./selectable-element";
import {EditorWorkspace} from "./editor-workspace";
import {newIds, project} from "./flow-editor";
import {Utilities} from "../utils";

@customElement("flow-node")
export class FlowNode extends SelectableElement {
    @property({ converter: nodeConverter }) public node!: Node;
    @property({ attribute: false }) public workspace!: EditorWorkspace;
    @property({ type: Boolean, reflect: true }) public frozen = false;

    @property({ type: Boolean, reflect: true }) private minimal = false;
    @property({ attribute: false }) private isBeingDragged = false;

    constructor() {
        super();
    }

    public static get styles() {
        return [FlowNodeStyle,
            css`
            .internal-item {
                display: flex;
                flex-direction: column;
                align-items: flex-start;
                flex: 0 0 auto;
            }
            .internal-title {
                font-size: 0.8em;
                margin-bottom: 0;
                margin-top:3px;
                text-align: left;
            }
        `]
    }

    public disconnectedCallback() {
        this.workspace.removeEventListener("pan", this.setTranslate);
        this.workspace.removeEventListener("zoom", this.setTranslate);
        project.removeListener("cleared", this.forceUpdate);
        project.removeListener("connectionremoved", this.linkIdentity);
        super.disconnectedCallback();
    }

    public bringToFront() {
        if (this.frozen) { return; }
        this.style.zIndex = `${this.workspace.topNodeZindex++}`;
    }

    protected async firstUpdated() {
        super.firstUpdated();
        this.linkIdentity();
        this.workspace.addEventListener("pan", this.setTranslate);
        this.workspace.addEventListener("zoom", this.setTranslate);
        project.addListener("cleared", this.forceUpdate);
        project.addListener("connectionremoved", this.linkIdentity);
        this.minimal = (this.node.displayCharacter || "").length !== 0;
        this.bringToFront();
        if (newIds.has(this.node.id!)) { // node centering has to keep track of which nodes were created by the user
            await this.updateComplete;
            await this.updateComplete;
            const size = this.getBoundingClientRect();
            this.node.position!.x! -= size.width / 2 / this.workspace.camera.zoom;
            this.node.position!.y! -= size.height / 2 / this.workspace.camera.zoom;
            this.setTranslate();
            newIds.delete(this.node.id!);
        }
        if (this.minimal) {
            this.addEventListener("mousedown", this.startDrag);
        }
    }


    protected async updated() {
        this.linkIdentity();
        this.dispatchEvent(new CustomEvent("updated"));
    }

    protected render() {
        if (this.minimal) {
            this.style.background = `var(--${this.node.type!.toLowerCase()}-color)`;
        } else {
            this.style.background = "";
        }

        this.setTranslate();
        this.style.boxShadow = this.selected ? "var(--highlight) 0 0 0 3px" : "";

        const title = this.minimal ?
            html`<div class="title minimal" ?singlechar="${this.node.displayCharacter!.length === 1}">${i18next.t("flow."+this.node.displayCharacter!, this.node.displayCharacter!)}</div>` :
            html`<div class="title ${this.node.type!.toLowerCase()}" @mousedown="${this.startDrag}">${i18next.t("flow."+this.node.name!) || "invalid"}</div>`;

        const inputSide = html`<div class="socket-side inputs">${this.node.inputs!.map((i) => html`<flow-node-socket ?renderlabel="${!this.minimal}" .socket="${i}" side="input"></flow-node-socket>`)}</div>`;
        const outputSide = html`<div class="socket-side outputs">${this.node.outputs!.map((i) => html`<flow-node-socket ?renderlabel="${!this.minimal}" .socket="${i}" side="output"></flow-node-socket>`)}</div>`;
        const spacer = html`<div style="width: 10px"></div>`;
        // Gather the elements between NEW_LINES in distinct groups, that can then be iterated per-group and per-element below. Should help with adding as many internals in any configuration we would like.
        return html`
        ${title}
        ${this.node.inputs!.length > 0 ? inputSide : spacer}
        ${(this.minimal) ? null : html`
            <div class="internal-container" style="padding-top: 8px">
            ${this.node.internals!.reduce((acc, i, index, array) => {
                acc[acc.length - 1].push(i);
                if (i.breakType === NodeInternalBreakType.NEW_LINE && index < array.length - 1) {
                    acc.push([]);
                }
                return acc;
            }, [[]] as NodeInternal[][]).map((group) => html`
            <div class="internal-group" style="max-width: fit-content; min-width: fit-content; padding-bottom: 8px;">
                ${group.map((i) => html`
                    <div class="internal-item" style="display: inline-block">
                        <internal-picker style="pointer-events: ${(this.frozen ? "none" : "normal")};" @picked="${async () => {
                        this.forceUpdate();
                        await this.updateComplete;
                        project.removeInvalidConnections();
                    }}" .node="${this.node}" .internalIndex="${this.node.internals!.indexOf(i)}"></internal-picker>
                    </div>
                `)}
            </div>
        `)}
        </div>`}
        ${this.node.outputs!.length > 0 ? outputSide : spacer}
        ${(this.frozen ? html`<or-icon class="lock-icon ${this.node.type!.toLowerCase()}" icon="lock"></or-icon>` : ``)}
        `;
    }

    private forceUpdate = () => { this.requestUpdate(); };

    private setTranslate = () => {
        const pos = this.workspace.worldToOffset(this.node.position!);
        this.style.transform = `translate(${pos.x}px, ${pos.y}px) scale(${this.workspace.camera.zoom})`;
        this.dispatchEvent(new CustomEvent("updated"));
    }

    private startDrag = (e: MouseEvent) => {
        if (this.frozen) { return; }
        if (e.buttons !== 1) { return; }
        project.createUndoSnapshot();
        this.bringToFront();
        window.addEventListener("mouseup", this.stopDrag);
        window.addEventListener("mousemove", this.onDrag);
        this.isBeingDragged = true;
    }

    private onDrag = (e: MouseEvent) => {
        this.node.position = {
            x: this.node.position!.x! + e.movementX / this.workspace.camera.zoom,
            y: this.node.position!.y! + e.movementY / this.workspace.camera.zoom
        };
        this.setTranslate();
    }

    private stopDrag = () => {
        window.removeEventListener("mouseup", this.stopDrag);
        window.removeEventListener("mousemove", this.onDrag);
        this.isBeingDragged = false;
        project.notifyChange();
    }

    private linkIdentity = () => {
        IdentityDomLink.map[this.node.id!] = this;
    }
}
