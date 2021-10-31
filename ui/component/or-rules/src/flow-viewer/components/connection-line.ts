import { html, css } from "lit";
import {customElement, property} from "lit/decorators.js";
import { NodeConnection } from "@openremote/model";
import { IdentityDomLink, IdentityAssigner, NodeUtilities } from "../node-structure";
import { FlowNodeSocket } from "./flow-node-socket";
import { ResizeObserver } from "resize-observer";
import { SelectableElement } from "./selectable-element";
import { EditorWorkspace } from "./editor-workspace";
import { FlowNode } from "./flow-node";
import { project } from "./flow-editor";

@customElement("connection-line")
export class ConnectionLine extends SelectableElement {
    @property({ attribute: false }) public connection!: NodeConnection;
    @property({ attribute: false }) public workspace!: EditorWorkspace;

    @property({ type: String }) private polylineId: string;

    private fromNodeElement!: FlowNode;
    private toNodeElement!: FlowNode;

    private fromElement!: FlowNodeSocket;
    private toElement!: FlowNodeSocket;
    private resizeObserver!: ResizeObserver;
    private readonly fancyLine = true;
    private readonly curveIntensity = 1;

    constructor() {
        super();
        this.polylineId = IdentityAssigner.generateIdentity();
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
        this.resizeObserver.disconnect();
        project.removeListener("connectionremoved", this.nodeChanged);
    }

    protected firstUpdated() {
        super.firstUpdated();
        const elem = this.shadowRoot!.getElementById(this.polylineId)!;
        if (elem)
            this.setHandle(elem);
    }

    protected render() {
        if (this.isInvalid) {
            const fromSocket = NodeUtilities.getSocketFromID(this.connection.from!, project.nodes);
            const toSocket = NodeUtilities.getSocketFromID(this.connection.to!, project.nodes);
            this.fromNodeElement = IdentityDomLink.map[fromSocket!.nodeId!];
            this.toNodeElement = IdentityDomLink.map[toSocket!.nodeId!];
            this.fromElement = IdentityDomLink.map[fromSocket!.id!];
            this.toElement = IdentityDomLink.map[toSocket!.id!];
            if (this.isInvalid) {
                console.warn(this.fromNodeElement);
                console.warn(this.toNodeElement);
                console.warn(this.fromElement);
                console.warn(this.toElement);

                console.warn("Attempt to render invalid connection");
                return html``;
            } else {
                this.fromNodeElement.addEventListener("updated", this.nodeChanged);
                this.toNodeElement.addEventListener("updated", this.nodeChanged);
                this.resizeObserver = new ResizeObserver(this.nodeChanged);
                this.resizeObserver.observe(this.fromNodeElement);
                this.resizeObserver.observe(this.toNodeElement);
            }
        }

        const parentSize = this.workspace.clientRect;
        const from = this.fromElement.connectionPosition;
        const to = this.toElement.connectionPosition;

        if (this.fancyLine) {

            const x1 = from.x - parentSize.left;
            const y1 = from.y - parentSize.top;

            const x2 = to.x - parentSize.left;
            const y2 = to.y - parentSize.top;

            const intensity = this.curveIntensity * (Math.abs(x1 - x2) / 2);
            // const intensity = 100 * this.workspace.camera.zoom;

            return html`<svg style="stroke-width: ${this.workspace.camera.zoom * (this.selected ? 5 : 3)}px;">
                <path id="${this.polylineId}" 
                d="M ${x1} ${y1} C ${x1 + intensity} ${y1}, ${x2 - intensity} ${y2}, ${x2} ${y2}"
                selected="${this.selected}"
                />
            </svg>`;
        }

        return html`<svg style="stroke-width: ${this.workspace.camera.zoom * (this.selected ? 5 : 3)}px;"><polyline id="${this.polylineId}"
        selected="${this.selected}"
        points="
        ${from.x - parentSize.left}, ${from.y - parentSize.top} 
        ${to.x - parentSize.left}, ${to.y - parentSize.top}"
        ></polyline>
        </svg>`;
    }

    public static get styles() {
        return css`
            svg{
                fill: none;  
                overflow: visible;
                position: absolute;
                pointer-events: none;
                stroke-linejoin: round;
            }
            path, line{
                position: absolute;
                pointer-events: all;
            }
            path:hover, line:hover, path[selected = true], line[selected = true]{
                stroke: var(--highlight);
            }
            text{
                fill: red;
                stroke: none;
            }`;
    }

    private nodeChanged = () => { this.requestUpdate(); };

    private get isInvalid() {
        return (!this.fromNodeElement || !this.toNodeElement || !this.fromElement || !this.toElement);
    }
}
