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
import { LitElement, html, css } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { Integration } from "../services/integration";
import { CopyPasteManager } from "../services/copy-paste-manager";
import { Project } from "../services/project";
import { Input } from "../services/input";
import { ModalService } from "../services/modal";
import { Exporter } from "../services/exporter";
import { Shortcuts } from "../services/shortcuts";
import { OrRulesRuleChangedEvent } from "../..";
import { translate, i18next } from "@openremote/or-translate";
import { NodeUtilities } from "../node-structure";
export const integration = new Integration();
export const copyPasteManager = new CopyPasteManager();
export const project = new Project();
export const input = new Input();
export const modal = new ModalService();
export const exporter = new Exporter();
export const shortcuts = new Shortcuts();
export const newIds = new Set();
let FlowEditor = class FlowEditor extends translate(i18next)(LitElement) {
    constructor() {
        super();
        this.showTopbar = false;
    }
    validate() {
        return NodeUtilities.validate(exporter.jsonToFlow(this._ruleset.rules || "{}"));
    }
    beforeSave() {
        if (this.readonly) {
            return;
        }
        this.serialiseRule();
    }
    set ruleset(ruleset) {
        if (this._ruleset === ruleset) {
            return;
        }
        this._ruleset = ruleset;
        if (ruleset.rules) {
            const collection = exporter.jsonToFlow(ruleset.rules);
            project.fromNodeCollection(collection);
            project.setCurrentProject(ruleset.id, ruleset.name, collection.description);
        }
        else {
            project.clear(true);
        }
    }
    static get styles() {
        return [
            css `
            :host{
                user-select: none;
                display: grid;
                grid-template-columns: 1fr auto;
                grid-template-rows: 100%;
                grid-template-areas: 
                "workspace node-panel";

                width: 100%;
                height: 100%;
                
                --or-app-color4: rgb(76,76,76);

                --socket-size: 24px;
                --socket-display-size: 14px;
                
                --topbar-height: 50px;
                --nodepanel-width: 195px;
                --roundness: 3px;
                
                --highlight-faded: hsla(102, 100%, 31%, 0.2);
                --highlight: hsla(102, 100%, 31%, 0.5);
                --highlight-opaque: hsla(102, 100%, 31%);
                
                --any: rgb(162, 0, 255);
                --number: rgb(165, 228, 50);
                --boolean: rgb(0, 102, 255);
                --string: rgb(45, 180, 221);
                --color: rgb(255, 228, 78);
                
                --input-color: hsl(222, 60%, 46%);
                --processor-color: hsl(102, 58%, 39%);
                --output-color: hsl(282, 60%, 47%);
                
                --input-color-h: hsl(222, 58%, 54%);
                --processor-color-h: hsl(102, 48%, 49%);
                --output-color-h: hsl(282, 58%, 54%);
            }`
        ];
    }
    firstUpdated() {
        return __awaiter(this, void 0, void 0, function* () {
            yield integration.refreshNodes();
            this.requestUpdate();
            yield this.updateComplete;
            project.emit("fitview");
            project.addListener("changed", () => {
                this.serialiseRule();
                this.dispatchEvent(new OrRulesRuleChangedEvent(this.validate()));
            });
        });
    }
    render() {
        if (this.showTopbar) {
            this.style.gridTemplateRows = "var(--topbar-height) 1fr";
            this.style.gridTemplateAreas = '"topbar topbar" "workspace node-panel";';
        }
        return html `
        ${(this.showTopbar ? html `<top-bar .application="${this}" style="grid-area: topbar"></top-bar>` : ``)}
        <node-panel .application="${this}" style="grid-area: node-panel" .nodes= "${integration.nodes}"></node-panel>
        <editor-workspace .application="${this}" id="workspace" style="grid-area: workspace"></editor-workspace>
        <context-menu></context-menu>
        <popup-modal id="popup-modal"></popup-modal>
        `;
    }
    serialiseRule() {
        this._ruleset.rules = exporter.flowToJson(project.toNodeCollection(this._ruleset.name, project.existingFlowRuleDesc));
    }
};
__decorate([
    query("node-panel")
], FlowEditor.prototype, "nodePanel", void 0);
__decorate([
    query("top-bar")
], FlowEditor.prototype, "topBar", void 0);
__decorate([
    query("editor-workspace")
], FlowEditor.prototype, "editorWorkspace", void 0);
__decorate([
    property({ type: Boolean })
], FlowEditor.prototype, "showTopbar", void 0);
__decorate([
    property({ attribute: false })
], FlowEditor.prototype, "readonly", void 0);
__decorate([
    property({ attribute: false })
], FlowEditor.prototype, "_ruleset", void 0);
FlowEditor = __decorate([
    customElement("flow-editor")
], FlowEditor);
export { FlowEditor };
//# sourceMappingURL=flow-editor.js.map