var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { LitElement, html, css } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { i18next, translate } from "@openremote/or-translate";
let NodePanel = class NodePanel extends translate(i18next)(LitElement) {
    constructor() {
        super(...arguments);
        this.nodes = [];
    }
    static get styles() {
        return css `
        .list{
            overflow-x: hidden;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
            align-items: center;
            height: 100%;
            background: #F9F9F9;
        }
        .category{
            display: flex;
            width: 80%;
            flex-direction: column;
            align-items: center;
            text-align: center;
            padding: 15px 15px 0 15px;
        }
        .category span{
            margin:0;
            color: rgb(125,125,125);
            padding: 0 0 15px 0 ;
        }
        .small-node-grid{
            display: grid;
            grid-template-columns: repeat(5, 1fr);
            grid-template-rows: repeat(2, 1fr);
            grid-gap: 6px;
            justify-items: stretch;
            align-items: stretch;
            margin-bottom: 15px;
            width: var(--nodepanel-width);
        }
        .input-node{ background-color: var(--input-color); }
        .processor-node{ background-color: var(--processor-color); }
        .output-node{ background-color: var(--output-color); }
        .input-node:hover{ background-color: var(--input-color-h); }
        .processor-node:hover{ background-color: var(--processor-color-h); }
        .output-node:hover{ background-color: var(--output-color-h); }`;
    }
    firstUpdated() {
        if (this.drawer)
            this.drawer.open = true;
    }
    render() {
        return html `
        <!-- <or-mwc-drawer rightsided dismissible transparent> -->
            ${this.listTemplate}
        <!-- </or-mwc-drawer> -->
        `;
    }
    nodeTemplate(node) {
        return html `<node-menu-item class="node-item" .node="${node}" .workspace="${this.application.editorWorkspace}"></node-menu-item>`;
    }
    get listTemplate() {
        return html `
        <div class="list">
            <div class="category"> <span>${i18next.t("input", "Input")}</span> 
                ${this.nodes.filter((n) => n.type === "INPUT" /* NodeType.INPUT */).map((n) => this.nodeTemplate(n))}
            </div>

            <div class="category"><span>${i18next.t("processors", "Processors")}</span> 
                <div class="small-node-grid">
                    ${this.nodes.filter((n) => n.type === "PROCESSOR" /* NodeType.PROCESSOR */ && n.displayCharacter).map((n) => this.nodeTemplate(n))}
                </div>
                ${this.nodes.filter((n) => n.type === "PROCESSOR" /* NodeType.PROCESSOR */ && !n.displayCharacter).map((n) => this.nodeTemplate(n))}
            </div>

            <div class="category"> <span>${i18next.t("output", "Output")}</span> 
                ${this.nodes.filter((n) => n.type === "OUTPUT" /* NodeType.OUTPUT */).map((n) => this.nodeTemplate(n))}
            </div>
        </div>
        `;
    }
};
__decorate([
    property({ type: Array })
], NodePanel.prototype, "nodes", void 0);
__decorate([
    query("or-mwc-drawer")
], NodePanel.prototype, "drawer", void 0);
__decorate([
    property({ attribute: false })
], NodePanel.prototype, "application", void 0);
NodePanel = __decorate([
    customElement("node-panel")
], NodePanel);
export { NodePanel };
//# sourceMappingURL=node-panel.js.map