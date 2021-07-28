import { LitElement, html, css } from "lit";
import {customElement, property, query} from "lit/decorators.js";
import { NodeSocket } from "@openremote/model";
import { IdentityDomLink } from "../node-structure";
import { Utilities } from "../utils";
import { i18next, translate } from "@openremote/or-translate";
import { project } from "./flow-editor";
import { FlowNode } from "./flow-node";

@customElement("flow-node-socket")
export class FlowNodeSocket extends translate(i18next)(LitElement) {

    public get connectionPosition() {
        return Utilities.getCenter(this.circleElem.getBoundingClientRect());
    }

    public static get styles() {
        return css`
        :host{
            width: auto;
            height: var(--socket-size);
            margin: 2px;
            display: inline-block;
        }
        .socket{
            background: none;
            height: var(--socket-size);
            width: var(--socket-size);
            border-radius: 100%;
            display: inline-flex;
            justify-content: center;
            align-items: center;
        }
        .socket:hover{
            background: var(--highlight);
        }
        .label{
            display:inline-block;
            vertical-align: top;
            color: rgba(0,0,0,.5);
            text-transform: lowercase;
        }
        .circle{
            box-sizing: border-box;
            background: grey;
            width: var(--socket-display-size);
            height: var(--socket-display-size);
            border-radius: 100%;
            pointer-events: none;
            border: 1px solid rgb(80,80,80);
        }`;
    }

    public get socketTypeString() {
        return this.socket.type!.toString().toLowerCase();
    }
    @property({ type: Object }) public socket!: NodeSocket;
    @property({ type: String }) public side!: "input" | "output";
    @property({ type: Boolean }) public renderLabel = false;

    private identityDeleted = false;
    @query("#circle")
    private circleElem!: HTMLElement;

    public disconnectedCallback() {
        this.identityDeleted = delete IdentityDomLink.map[this.socket.id!];
        project.removeListener("connectioncreated", this.forceUpdate);
        project.removeListener("connectionremoved", this.forceUpdate);
        project.removeListener("nodeadded", this.forceUpdate);
        project.removeListener("noderemoved", this.forceUpdate);
        project.removeListener("cleared", this.forceUpdate);
        (IdentityDomLink.map[this.socket.nodeId!] as FlowNode).removeEventListener("updated", this.forceUpdate);
    }

    protected firstUpdated() {
        this.title = Utilities.humanLike(this.socketTypeString);
        this.linkIdentity();
        project.addListener("connectioncreated", this.forceUpdate);
        project.addListener("connectionremoved", this.forceUpdate);
        project.addListener("nodeadded", this.forceUpdate);
        project.addListener("noderemoved", this.forceUpdate);
        project.addListener("cleared", this.forceUpdate);
        (IdentityDomLink.map[this.socket.nodeId!] as FlowNode).addEventListener("updated", this.forceUpdate);

        const isInputSocket = this.side === "input";

        const md = (e: MouseEvent) => {
            this.linkIdentity();
            if (e.buttons !== 1) { return; }
            if (project.isCurrentlyConnecting) { return; }
            project.startConnectionDrag(e, this.socket, isInputSocket);
            e.stopPropagation();
            e.stopImmediatePropagation();
        };

        const mu = (e: MouseEvent) => {
            project.createUndoSnapshot();
            this.linkIdentity();
            project.endConnectionDrag(e, this.socket, isInputSocket);
            project.notifyChange();
            if (e.buttons !== 1) { return; }
            e.stopPropagation();
            e.stopImmediatePropagation();
        };

        this.addEventListener("mousedown", md);
        this.addEventListener("mouseup", mu);
    }

    protected updated() {
        this.linkIdentity();
    }

    protected render() {
        const color = `var(--${this.socketTypeString})`;

        const socket = html`<div class="socket">
            <div class="circle" id="circle" style="background: ${color}"></div>
            </div>`;

        if (!this.renderLabel) { return socket; }
        const label = html`<div class="label">${i18next.t(this.socket.name!)}</div>`;
        if (this.side === "input") {
            return html`${socket}${label}`;
        } else {
            return html`${label}${socket}`;
        }
    }

    private forceUpdate = () => this.requestUpdate();

    private linkIdentity() {
        if (!this.identityDeleted) {
            IdentityDomLink.map[this.socket.id!] = this;
        }
    }
}
