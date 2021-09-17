import { LitElement, html, css } from "lit";
import {customElement, property, query} from "lit/decorators.js";
import { Integration } from "../services/integration";
import { CopyPasteManager } from "../services/copy-paste-manager";
import { Project } from "../services/project";
import { Input } from "../services/input";
import { ModalService } from "../services/modal";
import { Exporter } from "../services/exporter";
import { Shortcuts } from "../services/shortcuts";
import { NodePanel } from "./node-panel";
import { EditorWorkspace } from "./editor-workspace";
import { TopBar } from "./top-bar";
import { RuleView, OrRulesRuleChangedEvent } from "../..";
import { RulesetUnion } from "@openremote/model";
import { translate, i18next } from "@openremote/or-translate";
import { NodeUtilities } from "../node-structure";

export const integration = new Integration();
export const copyPasteManager = new CopyPasteManager();
export const project = new Project();
export const input = new Input();
export const modal = new ModalService();
export const exporter = new Exporter();
export const shortcuts = new Shortcuts();
export const newIds: Set<string> = new Set<string>();

@customElement("flow-editor")
export class FlowEditor extends translate(i18next)(LitElement) implements RuleView {
    @query("node-panel") public nodePanel!: NodePanel;
    @query("top-bar") public topBar!: TopBar;
    @query("editor-workspace") public editorWorkspace!: EditorWorkspace;
    @property({ type: Boolean }) public showTopbar = false;
    
    @property({ attribute: false })
    public readonly?: boolean | undefined;
    @property({ attribute: false })
    protected _ruleset!: RulesetUnion;

    constructor() {
        super();
    }

    public validate() {
        return NodeUtilities.validate(exporter.jsonToFlow(this._ruleset.rules || "{}"));
    }

    public beforeSave() {
        if (this.readonly) { return; }
        this.serialiseRule();
    }

    public set ruleset(ruleset: RulesetUnion) {
        if (this._ruleset === ruleset) { return; }
        this._ruleset = ruleset;
        if (ruleset.rules) {
            const collection = exporter.jsonToFlow(ruleset.rules!);
            project.fromNodeCollection(collection);
            project.setCurrentProject(ruleset.id!, ruleset.name!, collection.description!);
        } else {
            project.clear(true);
        }
    }

    public static get styles() {
        return [
            css`
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
            }`];
    }

    protected async firstUpdated() {
        await integration.refreshNodes();
        this.requestUpdate();
        await this.updateComplete;
        project.emit("fitview");
        project.addListener("changed", () => {
            this.serialiseRule();
            this.dispatchEvent(new OrRulesRuleChangedEvent(this.validate()));
        });
    }

    protected render() {
        if (this.showTopbar) {
            this.style.gridTemplateRows = "var(--topbar-height) 1fr";
            this.style.gridTemplateAreas = '"topbar topbar" "workspace node-panel";';
        }

        return html`
        ${(this.showTopbar ? html`<top-bar .application="${this}" style="grid-area: topbar"></top-bar>` : ``)}
        <node-panel .application="${this}" style="grid-area: node-panel" .nodes= "${integration.nodes}"></node-panel>
        <editor-workspace .application="${this}" id="workspace" style="grid-area: workspace"></editor-workspace>
        <context-menu></context-menu>
        <popup-modal id="popup-modal"></popup-modal>
        `;
    }

    private serialiseRule() {
        this._ruleset.rules = exporter.flowToJson(project.toNodeCollection(this._ruleset.name!, project.existingFlowRuleDesc!));
    }
}
