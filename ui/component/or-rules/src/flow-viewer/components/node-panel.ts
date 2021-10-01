import { LitElement, html, css } from "lit";
import {customElement, property, query} from "lit/decorators.js";
import { NodeType, Node } from "@openremote/model";
import { i18next, translate } from "@openremote/or-translate";
import { OrMwcDrawer } from "@openremote/or-mwc-components/or-mwc-drawer";
import { FlowEditor } from "./flow-editor";
import { OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";

@customElement("node-panel")
export class NodePanel extends translate(i18next)(LitElement)  {
    @property({ type: Array }) public nodes: Node[] = [];
    static get styles() {
        return css`
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

    @query("or-mwc-drawer") public drawer!: OrMwcDrawer;
    @property({ attribute: false }) public application!: FlowEditor;

    protected firstUpdated() {
        if (this.drawer) this.drawer.open = true;
    }

    protected render() {
        return html`
        <!-- <or-mwc-drawer rightsided dismissible transparent> -->
            ${this.listTemplate}
        <!-- </or-mwc-drawer> -->
        `;
    }

    private nodeTemplate(node: Node) {
        return html`<node-menu-item class="node-item" .node="${node}" .workspace="${this.application.editorWorkspace}"></node-menu-item>`;
    }

    private get listTemplate() {
        return html`
        <div class="list">
            <div class="category"> <span>${i18next.t("input", "Input")}</span> 
                ${this.nodes.filter((n) => n.type === NodeType.INPUT).map((n) => this.nodeTemplate(n))}
            </div>

            <div class="category"><span>${i18next.t("processors", "Processors")}</span> 
                <div class="small-node-grid">
                    ${this.nodes.filter((n) => n.type === NodeType.PROCESSOR && n.displayCharacter).map((n) => this.nodeTemplate(n))}
                </div>
                ${this.nodes.filter((n) => n.type === NodeType.PROCESSOR && !n.displayCharacter).map((n) => this.nodeTemplate(n))}
            </div>

            <div class="category"> <span>${i18next.t("output", "Output")}</span> 
                ${this.nodes.filter((n) => n.type === NodeType.OUTPUT).map((n) => this.nodeTemplate(n))}
            </div>
        </div>
        `;
    }
}
