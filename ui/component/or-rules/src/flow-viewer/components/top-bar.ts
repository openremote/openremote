import { LitElement, html, css } from "lit";
import {customElement, property} from "lit/decorators.js";
import { IdentityDomLink } from "../node-structure";
import {InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { Utilities } from "../utils";
import { i18next, translate } from "@openremote/or-translate";
import { project, modal, input, exporter, FlowEditor } from "./flow-editor";
import { NodePanel } from "./node-panel";

@customElement("top-bar")
export class TopBar extends translate(i18next)(LitElement) {
    static get styles() {
        return css`
        :host{
            display: flex;
            width: 100%;
            justify-content: flex-start;
            align-items: center;
            box-shadow: rgba(0, 0, 0, 0.2) 0px 5px 5px -5px;
            line-height: var(--topbar-height);
            z-index: 150;
        }
        .button{
            padding: 0 25px 0 25px;
            cursor: pointer;
        }
        .button:hover{
            background: #fafafa;
        }
        .button:active{
            background: whitesmoke;
        }
        .debug.button{
            background: yellow;
        }
        .title{
            margin: 0 15px 0 15px;
            text-transform: uppercase;
            font-weight: bold;
        }
        .right{ 
            margin-left: auto;
        }`;
    }

    @property({ attribute: false }) public application!: FlowEditor;

    protected firstUpdated() {
        project.addListener("unsavedstateset", () => {
            this.requestUpdate();
        });
    }

    protected render() {
        return html`
        <span class="title">${i18next.t("flowEditor")}</span>
        <a class="button" @click="${() => {
                if (project.unsavedState) {
                    modal.confirmation(() => { project.clear(true); }, "New project");
                } else {
                    project.clear(true);
                }
            }}">New</a>
        <a class="button" @click="${this.save}">${i18next.t("save")} <i>${Utilities.ellipsis(project.existingFlowRuleName!)}</i>${project.unsavedState && project.existingFlowRuleId !== -1 ? "*" : ""}</a>
        ${project.existingFlowRuleId === -1 ? null : html`<a @click="${this.showSaveAsDialog}" class="button">Save as...</a>`}
        <a class="button" @click="${this.showRuleBrowser}">Open</a>
        <a class="button right" @click="${() => {
                this.application.nodePanel.drawer.toggle();
            }}"><or-icon icon="menu"></or-icon></a>
        `;
    }

    private save() {
        if (project.existingFlowRuleId === -1) {
            this.showSaveAsDialog();
        } else {
            exporter.exportAsExisting(
                project.existingFlowRuleId,
                project.toNodeCollection(project.existingFlowRuleName!, project.existingFlowRuleDesc!));
        }
    }

    private showRuleBrowser() {
        modal.element.content = html`<rule-browser @ruleloaded="${() => modal.element.close()}"></rule-browser>`;
        modal.element.header = "Select a Flow rule";
        modal.element.open();
    }

    private showSaveAsDialog() {
        let chosenName = "";
        let chosenDesc = "";
        modal.element.content = html`
            <div style="display: flex; flex-direction: column; width: auto; justify-content: space-between; align-items: stretch;">
            <or-mwc-input style="margin-bottom: 16px; width:100%;" required type="text" label="${i18next.t("name", "Name")!}"
            @or-mwc-input-changed="${(e: OrInputChangedEvent) => { chosenName = e.detail.value; }}"
            ></or-mwc-input>
            <or-mwc-input style="margin-bottom: 16px; width:100%;" fullwidth type="textarea" label="${i18next.t("description", "Description")!}"
            @or-mwc-input-changed="${(e: OrInputChangedEvent) => { chosenDesc = e.detail.value; }}"
            ></or-mwc-input>
            <div>
                <or-mwc-input style="text-align: left; margin-right: 10px" type="${InputType.BUTTON}" label="${i18next.t("cancel", "Cancel")!}" @or-mwc-input-changed="${modal.element.close}"></or-mwc-input>
                <or-mwc-input style="text-align: right" type="${InputType.BUTTON}" unelevated label="${i18next.t("save", "Save")!}" @or-mwc-input-changed="${() => {
                if (!chosenName) { return; }
                exporter.exportAsNew(project.toNodeCollection(chosenName, chosenDesc));
                modal.element.close();
            }}"></or-mwc-input>
            </div>
            </div>
        `;
        modal.element.header = "Save project";
        modal.element.open();
    }
}
