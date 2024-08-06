var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { LitElement, html, css } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { IdentityDomLink } from "../node-structure";
import { Utilities } from "../utils";
import { i18next, translate } from "@openremote/or-translate";
import { project } from "./flow-editor";
let FlowNodeSocket = class FlowNodeSocket extends translate(i18next)(LitElement) {
    constructor() {
        super(...arguments);
        this.renderLabel = false;
        this.identityDeleted = false;
        this.forceUpdate = () => this.requestUpdate();
    }
    get connectionPosition() {
        return Utilities.getCenter(this.circleElem.getBoundingClientRect());
    }
    static get styles() {
        return css `
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
    get socketTypeString() {
        return this.socket.type.toString().toLowerCase();
    }
    disconnectedCallback() {
        this.identityDeleted = delete IdentityDomLink.map[this.socket.id];
        project.removeListener("connectioncreated", this.forceUpdate);
        project.removeListener("connectionremoved", this.forceUpdate);
        project.removeListener("nodeadded", this.forceUpdate);
        project.removeListener("noderemoved", this.forceUpdate);
        project.removeListener("cleared", this.forceUpdate);
        IdentityDomLink.map[this.socket.nodeId].removeEventListener("updated", this.forceUpdate);
    }
    firstUpdated() {
        this.title = Utilities.humanLike(this.socketTypeString);
        this.linkIdentity();
        project.addListener("connectioncreated", this.forceUpdate);
        project.addListener("connectionremoved", this.forceUpdate);
        project.addListener("nodeadded", this.forceUpdate);
        project.addListener("noderemoved", this.forceUpdate);
        project.addListener("cleared", this.forceUpdate);
        IdentityDomLink.map[this.socket.nodeId].addEventListener("updated", this.forceUpdate);
        const isInputSocket = this.side === "input";
        const md = (e) => {
            this.linkIdentity();
            if (e.buttons !== 1) {
                return;
            }
            if (project.isCurrentlyConnecting) {
                return;
            }
            project.startConnectionDrag(e, this.socket, isInputSocket);
            e.stopPropagation();
            e.stopImmediatePropagation();
        };
        const mu = (e) => {
            project.createUndoSnapshot();
            this.linkIdentity();
            project.endConnectionDrag(e, this.socket, isInputSocket);
            project.notifyChange();
            if (e.buttons !== 1) {
                return;
            }
            e.stopPropagation();
            e.stopImmediatePropagation();
        };
        this.addEventListener("mousedown", md);
        this.addEventListener("mouseup", mu);
    }
    updated() {
        this.linkIdentity();
    }
    render() {
        const color = `var(--${this.socketTypeString})`;
        const socket = html `<div class="socket">
            <div class="circle" id="circle" style="background: ${color}"></div>
            </div>`;
        if (!this.renderLabel) {
            return socket;
        }
        const label = html `<div class="label">${i18next.t(this.socket.name)}</div>`;
        if (this.side === "input") {
            return html `${socket}${label}`;
        }
        else {
            return html `${label}${socket}`;
        }
    }
    linkIdentity() {
        if (!this.identityDeleted) {
            IdentityDomLink.map[this.socket.id] = this;
        }
    }
};
__decorate([
    property({ type: Object })
], FlowNodeSocket.prototype, "socket", void 0);
__decorate([
    property({ type: String })
], FlowNodeSocket.prototype, "side", void 0);
__decorate([
    property({ type: Boolean })
], FlowNodeSocket.prototype, "renderLabel", void 0);
__decorate([
    query("#circle")
], FlowNodeSocket.prototype, "circleElem", void 0);
FlowNodeSocket = __decorate([
    customElement("flow-node-socket")
], FlowNodeSocket);
export { FlowNodeSocket };
//# sourceMappingURL=flow-node-socket.js.map