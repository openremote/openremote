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
import { LitElement, html } from "lit";
import { customElement, property } from "lit/decorators.js";
import { repeat } from "lit/directives/repeat.js";
import { IdentityDomLink } from "../node-structure";
import { List } from "linqts";
import { EditorWorkspaceStyle } from "../styles/editor-workspace-style";
import { i18next, translate } from "@openremote/or-translate";
import { ContextMenu } from "./context-menu";
import { project, input, copyPasteManager } from "./flow-editor";
import { createContextMenuButtons } from "./workspace-contextmenu-options";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
let EditorWorkspace = class EditorWorkspace extends translate(i18next)(LitElement) {
    get clientRect() {
        return this.cachedClientRect;
    }
    static get styles() {
        return EditorWorkspaceStyle;
    }
    get halfSize() {
        const box = this.cachedClientRect;
        return { x: box.width / 2, y: box.height / 2 };
    }
    get isCameraInDefaultPosition() {
        const errorMargin = 0.05;
        return (Math.abs(this.camera.x) < errorMargin && Math.abs(this.camera.y) < errorMargin && this.camera.zoom > (1 - errorMargin) && this.camera.zoom < (1 + errorMargin));
    }
    constructor() {
        super();
        this.camera = { x: 0, y: 0, zoom: 1 };
        this.topNodeZindex = 1;
        this.scrollSensitivity = 1.25;
        this.zoomLowerBound = .2;
        this.zoomUpperBound = 10;
        this.connectionDragging = false;
        this.connectionFrom = { x: 0, y: 0 };
        this.connectionTo = { x: 0, y: 0 };
        this.isPanning = false;
        this.onMove = (event) => {
            const documentZoom = window.devicePixelRatio;
            this.camera = {
                x: this.camera.x + event.movementX / this.camera.zoom / documentZoom,
                y: this.camera.y + event.movementY / this.camera.zoom / documentZoom,
                zoom: this.camera.zoom
            };
            this.dispatchEvent(new CustomEvent("pan"));
        };
        this.onZoom = (event) => {
            if (this.connectionDragging) {
                return;
            }
            if (this.isPanning) {
                return;
            }
            const magnification = 0.9 * this.scrollSensitivity;
            const rz = event.deltaY < 0 ? this.camera.zoom * magnification : this.camera.zoom / magnification;
            if (rz < this.zoomLowerBound || rz > this.zoomUpperBound) {
                return;
            }
            this.camera = {
                x: this.camera.x,
                y: this.camera.y,
                zoom: rz
            };
            this.dispatchEvent(new CustomEvent("zoom"));
        };
        this.onEmptyConnectionRelease = (ee) => {
            if (project.isCurrentlyConnecting) {
                project.endConnectionDrag(ee, null, false);
            }
        };
        this.stopPan = () => {
            if (!this.isPanning) {
                return;
            }
            window.removeEventListener("mousemove", this.onMove);
            this.isPanning = false;
        };
        project.addListener("nodeadded", (n) => { this.requestUpdate(); });
        project.addListener("noderemoved", (n) => { this.requestUpdate(); });
        project.addListener("cleared", () => { this.requestUpdate(); });
        project.addListener("connectionstart", (e, s) => {
            if (e.buttons !== 1) {
                return;
            }
            const pos = IdentityDomLink.map[s.id].connectionPosition;
            this.connectionFrom = this.pageToOffset(pos);
            this.addEventListener("mousemove", project.connectionDragging);
            this.addEventListener("mouseup", this.onEmptyConnectionRelease);
        });
        project.addListener("connecting", (e) => {
            this.connectionTo = { x: e.pageX - this.clientRect.left, y: e.pageY - this.clientRect.top };
            this.connectionDragging = true;
        });
        project.addListener("connectionend", () => {
            this.connectionDragging = false;
            this.removeEventListener("mousemove", project.connectionDragging);
        });
        project.addListener("fitview", () => {
            this.fitCamera(project.nodes);
        });
        window.addEventListener("resize", () => {
            this.cachedClientRect = this.getBoundingClientRect();
            this.dispatchEvent(new CustomEvent("pan"));
        });
        this.addEventListener("mousedown", (e) => {
            this.startPan(e);
            if (e.buttons === 1) {
                input.clearSelection();
            }
        });
        this.addEventListener("contextmenu", (e) => {
            const buttons = createContextMenuButtons(this, e);
            ContextMenu.open(e.pageX, e.pageY, this, buttons);
            e.preventDefault();
        });
        project.workspace = this;
        this.addEventListener("wheel", this.onZoom, { passive: true });
    }
    resetCamera() {
        this.camera = { x: 0, y: 0, zoom: 1 };
        this.dispatchEvent(new CustomEvent("pan"));
        this.dispatchEvent(new CustomEvent("zoom"));
    }
    fitCamera(nodes, padding = 25) {
        const enumerable = new List();
        enumerable.AddRange(nodes);
        if (enumerable.Count() == 0)
            return;
        const XouterleastNode = enumerable.OrderBy((a) => a.position.x).First();
        const YouterleastNode = enumerable.OrderBy((a) => a.position.y).First();
        const XoutermostNode = enumerable.OrderByDescending((a) => a.position.x + IdentityDomLink.map[a.id].scrollWidth).First();
        const YoutermostNode = enumerable.OrderByDescending((a) => a.position.y + IdentityDomLink.map[a.id].scrollHeight).First();
        const XoutermostWidth = IdentityDomLink.map[XoutermostNode.id].scrollWidth;
        const YoutermostHeight = IdentityDomLink.map[YoutermostNode.id].scrollHeight;
        const fitBounds = {
            left: XouterleastNode.position.x - padding,
            right: XoutermostNode.position.x + padding + XoutermostWidth,
            top: YoutermostNode.position.y + padding + YoutermostHeight,
            bottom: YouterleastNode.position.y - padding,
        };
        const fitWidth = fitBounds.right - fitBounds.left;
        const fitHeight = fitBounds.top - fitBounds.bottom;
        const totalBounds = this.clientRect;
        const center = {
            x: (fitBounds.left + fitBounds.right) / 2.0,
            y: (fitBounds.top + fitBounds.bottom) / 2.0
        };
        const targetZoom = Math.min(totalBounds.width / fitWidth, totalBounds.height / fitHeight);
        this.camera = {
            x: -center.x,
            y: -center.y,
            zoom: Math.min(Math.max(this.zoomLowerBound, targetZoom), this.zoomUpperBound)
        };
        this.dispatchEvent(new CustomEvent("pan"));
        this.dispatchEvent(new CustomEvent("zoom"));
    }
    offsetToWorld(point) {
        const halfSize = this.halfSize;
        return {
            x: (point.x - halfSize.x) / this.camera.zoom - this.camera.x,
            y: (point.y - halfSize.y) / this.camera.zoom - this.camera.y
        };
    }
    worldToOffset(point) {
        const halfSize = this.halfSize;
        return {
            x: (point.x + this.camera.x) * this.camera.zoom + halfSize.x,
            y: (point.y + this.camera.y) * this.camera.zoom + halfSize.y
        };
    }
    pageToOffset(point) {
        const box = this.clientRect;
        return {
            x: point.x - box.left,
            y: point.y - box.top
        };
    }
    pasteAt(x, y) {
        return __awaiter(this, void 0, void 0, function* () {
            const clone = copyPasteManager.getFromClipboard({ x, y });
            clone.nodes.forEach((n) => {
                project.addNode(n);
            });
            yield this.updateComplete;
            clone.connections.forEach((c) => {
                project.createConnection(c.from, c.to);
            });
        });
    }
    firstUpdated() {
        this.cachedClientRect = this.getBoundingClientRect();
        if (this.application.nodePanel.drawer) {
            this.application.nodePanel.drawer.addEventListener("or-mwc-drawer-changed", (e) => __awaiter(this, void 0, void 0, function* () {
                this.style.width = e.detail ? "calc(100% - 255px)" : "";
                yield this.updateComplete;
                this.cachedClientRect = this.getBoundingClientRect();
                this.dispatchEvent(new CustomEvent("pan"));
            }));
        }
    }
    render() {
        return html `
        ${repeat(project.nodes, (i) => i.id, (n) => html `<flow-node .node="${n}" .workspace="${this}"></flow-node>`)}
        <connection-container .workspace="${this}"></connection-container>
        <svg>
            <line style="display: 
            ${this.connectionDragging ? null : `none`}; 
            stroke-dasharray: ${20 * this.camera.zoom}, ${10 * this.camera.zoom}; 
            stroke-opacity: 0.25; stroke-width: ${this.camera.zoom * 4}px" 

            x1="${this.connectionFrom.x}" 
            y1="${this.connectionFrom.y}" 
            x2="${this.connectionTo.x}" 
            y2="${this.connectionTo.y}"></line>
        </svg>
        <selection-box .workspace="${this}"></selection-box>
        <div class="view-options" style="z-index: ${this.topNodeZindex + 1}">
            ${!this.isCameraInDefaultPosition ? html `<or-mwc-input type="${InputType.BUTTON}" icon="vector-square" @or-mwc-input-changed="${this.resetCamera}" label="${i18next.t("resetView", "Reset view")}"></or-mwc-input>` : null}
            ${project.nodes.length !== 0 ? html `<or-mwc-input type="button" icon="fit-to-page-outline" @or-mwc-input-changed="${() => this.fitCamera(project.nodes)}" label="${i18next.t("fitView", "Fit view")}"></or-mwc-input>` : null}
        </div>
        `;
    }
    startPan(event) {
        if (this.connectionDragging) {
            return false;
        }
        if (event.buttons !== 4) {
            return false;
        }
        this.isPanning = true;
        window.addEventListener("mousemove", this.onMove);
        window.addEventListener("mouseup", this.stopPan);
        event.preventDefault();
    }
};
__decorate([
    property({ attribute: false })
], EditorWorkspace.prototype, "camera", void 0);
__decorate([
    property({ attribute: false })
], EditorWorkspace.prototype, "topNodeZindex", void 0);
__decorate([
    property({ attribute: false })
], EditorWorkspace.prototype, "scrollSensitivity", void 0);
__decorate([
    property({ attribute: false })
], EditorWorkspace.prototype, "zoomLowerBound", void 0);
__decorate([
    property({ attribute: false })
], EditorWorkspace.prototype, "zoomUpperBound", void 0);
__decorate([
    property({ attribute: false })
], EditorWorkspace.prototype, "application", void 0);
__decorate([
    property({ attribute: false })
], EditorWorkspace.prototype, "connectionDragging", void 0);
__decorate([
    property({ attribute: false })
], EditorWorkspace.prototype, "connectionFrom", void 0);
__decorate([
    property({ attribute: false })
], EditorWorkspace.prototype, "connectionTo", void 0);
EditorWorkspace = __decorate([
    customElement("editor-workspace")
], EditorWorkspace);
export { EditorWorkspace };
//# sourceMappingURL=editor-workspace.js.map