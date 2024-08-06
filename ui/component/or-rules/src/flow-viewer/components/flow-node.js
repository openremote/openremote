var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { html } from "lit";
import { customElement, property } from "lit/decorators.js";
import { IdentityDomLink } from "../node-structure";
import { FlowNodeStyle } from "../styles/flow-node-style";
import { i18next } from "@openremote/or-translate";
import { nodeConverter } from "../converters/node-converter";
import { SelectableElement } from "./selectable-element";
import { project, newIds } from "./flow-editor";
let FlowNode = class FlowNode extends SelectableElement {
    constructor() {
        super();
        this.frozen = false;
        this.minimal = false;
        this.isBeingDragged = false;
        this.forceUpdate = () => { this.requestUpdate(); };
        this.setTranslate = () => {
            const pos = this.workspace.worldToOffset(this.node.position);
            this.style.transform = `translate(${pos.x}px, ${pos.y}px) scale(${this.workspace.camera.zoom})`;
            this.dispatchEvent(new CustomEvent("updated"));
        };
        this.startDrag = (e) => {
            if (this.frozen) {
                return;
            }
            if (e.buttons !== 1) {
                return;
            }
            project.createUndoSnapshot();
            this.bringToFront();
            window.addEventListener("mouseup", this.stopDrag);
            window.addEventListener("mousemove", this.onDrag);
            this.isBeingDragged = true;
        };
        this.onDrag = (e) => {
            this.node.position = {
                x: this.node.position.x + e.movementX / this.workspace.camera.zoom,
                y: this.node.position.y + e.movementY / this.workspace.camera.zoom
            };
            this.setTranslate();
        };
        this.stopDrag = () => {
            window.removeEventListener("mouseup", this.stopDrag);
            window.removeEventListener("mousemove", this.onDrag);
            this.isBeingDragged = false;
            project.notifyChange();
        };
        this.linkIdentity = () => {
            IdentityDomLink.map[this.node.id] = this;
        };
    }
    static get styles() {
        return FlowNodeStyle;
    }
    disconnectedCallback() {
        this.workspace.removeEventListener("pan", this.setTranslate);
        this.workspace.removeEventListener("zoom", this.setTranslate);
        project.removeListener("cleared", this.forceUpdate);
        project.removeListener("connectionremoved", this.linkIdentity);
        super.disconnectedCallback();
    }
    bringToFront() {
        if (this.frozen) {
            return;
        }
        this.style.zIndex = `${this.workspace.topNodeZindex++}`;
    }
    firstUpdated() {
        const _super = Object.create(null, {
            firstUpdated: { get: () => super.firstUpdated }
        });
        return __awaiter(this, void 0, void 0, function* () {
            _super.firstUpdated.call(this);
            this.linkIdentity();
            this.workspace.addEventListener("pan", this.setTranslate);
            this.workspace.addEventListener("zoom", this.setTranslate);
            project.addListener("cleared", this.forceUpdate);
            project.addListener("connectionremoved", this.linkIdentity);
            this.minimal = (this.node.displayCharacter || "").length !== 0;
            this.bringToFront();
            if (newIds.has(this.node.id)) { // node centering has to keep track of which nodes were created by the user
                yield this.updateComplete;
                yield this.updateComplete;
                const size = this.getBoundingClientRect();
                this.node.position.x -= size.width / 2 / this.workspace.camera.zoom;
                this.node.position.y -= size.height / 2 / this.workspace.camera.zoom;
                this.setTranslate();
                newIds.delete(this.node.id);
            }
            if (this.minimal) {
                this.addEventListener("mousedown", this.startDrag);
            }
        });
    }
    updated() {
        return __awaiter(this, void 0, void 0, function* () {
            this.linkIdentity();
            this.dispatchEvent(new CustomEvent("updated"));
        });
    }
    render() {
        if (this.minimal) {
            this.style.background = `var(--${this.node.type.toLowerCase()}-color)`;
        }
        else {
            this.style.background = "";
        }
        this.setTranslate();
        this.style.boxShadow = this.selected ? "var(--highlight) 0 0 0 3px" : "";
        const title = this.minimal ?
            html `<div class="title minimal" ?singlechar="${this.node.displayCharacter.length === 1}">${i18next.t(this.node.displayCharacter)}</div>` :
            html `<div class="title ${this.node.type.toLowerCase()}" @mousedown="${this.startDrag}">${i18next.t(this.node.name) || "invalid"}</div>`;
        const inputSide = html `<div class="socket-side inputs">${this.node.inputs.map((i) => html `<flow-node-socket ?renderlabel="${!this.minimal}" .socket="${i}" side="input"></flow-node-socket>`)}</div>`;
        const outputSide = html `<div class="socket-side outputs">${this.node.outputs.map((i) => html `<flow-node-socket ?renderlabel="${!this.minimal}" .socket="${i}" side="output"></flow-node-socket>`)}</div>`;
        const spacer = html `<div style="width: 10px"></div>`;
        return html `
        ${title}
        ${this.node.inputs.length > 0 ? inputSide : spacer}
        ${(this.minimal) ? null : html `<div class="internal-container">${this.node.internals.map((i) => html `<internal-picker style="pointer-events: ${(this.frozen ? "none" : "normal")}" @picked="${() => __awaiter(this, void 0, void 0, function* () {
            this.forceUpdate();
            yield this.updateComplete;
            project.removeInvalidConnections();
        })}" .node="${this.node}" .internalIndex="${this.node.internals.indexOf(i)}"></internal-picker>`)}</div>`}
        ${this.node.outputs.length > 0 ? outputSide : spacer}
        ${(this.frozen ? html `<or-icon class="lock-icon ${this.node.type.toLowerCase()}" icon="lock"></or-icon>` : ``)}
        `;
    }
};
__decorate([
    property({ converter: nodeConverter })
], FlowNode.prototype, "node", void 0);
__decorate([
    property({ attribute: false })
], FlowNode.prototype, "workspace", void 0);
__decorate([
    property({ type: Boolean, reflect: true })
], FlowNode.prototype, "frozen", void 0);
__decorate([
    property({ type: Boolean, reflect: true })
], FlowNode.prototype, "minimal", void 0);
__decorate([
    property({ attribute: false })
], FlowNode.prototype, "isBeingDragged", void 0);
FlowNode = __decorate([
    customElement("flow-node")
], FlowNode);
export { FlowNode };
//# sourceMappingURL=flow-node.js.map