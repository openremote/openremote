import { LitElement, css, html } from "lit";
import {customElement, property} from "lit/decorators.js";
import { GlobalRuleset } from "@openremote/model";
import rest from "@openremote/rest";
import { i18next, translate } from "@openremote/or-translate";
import { Utilities } from "../utils";
import { Status } from "../models/status";
import { exporter, project, modal } from "./flow-editor";

@customElement("rule-browser")
export class RuleBrowser extends translate(i18next)(LitElement) {
    @property({ type: Number }) private status = Status.Idle;
    private retrievedRules: GlobalRuleset[] = [];

    public static get styles() {
        return css`
        .list-button {
            cursor: pointer;
            padding: 8px 0 8px 8px;
        }
        .list-button:hover {
            background: whitesmoke;
        }
        .list-button:active {
            background: none;
        }
        or-icon{
            width: 18px;
            vertical-align: text-top;
        }
        or-icon[icon=loading]{
            animation: spin 600ms infinite linear;
        }
        @keyframes spin{
            0%{
                transform: rotateZ(0deg);
            }
            100%{
                transform: rotateZ(360deg);
            }
        }`;
    }

    protected async firstUpdated() {
        this.status = Status.Loading;
        try {
            const response = await rest.api.RulesResource.getGlobalRulesets();
            this.retrievedRules = response.data;
            this.status = Status.Success;
        } catch (error) {
            this.status = Status.Failure;
        }
    }

    protected render() {
        let result = html``;
        switch (this.status) {
            case Status.Loading:
                result = html`<span style="text-align: center;"><or-icon icon="loading"></or-icon></span>`;
                break;
            case Status.Success:
                result = html`${this.retrievedRules.length === 0 ?
                    html`<span>No rules to display</span>` :
                    this.retrievedRules.map((r: GlobalRuleset) => this.getButton(r))}`;
                break;
            case Status.Failure:
                result = html`<span>Failed to load rules</span>`;
                break;
        }
        return html`
        <div style="display: flex; flex-direction: column; width: auto; align-items: stretch;">
        ${result}
        </div>`;
    }

    private loadRule = async (r: GlobalRuleset) => {
        this.status = Status.Loading;
        let response: { data: GlobalRuleset };
        try {
            response = await rest.api.RulesResource.getGlobalRuleset(r.id!);
        } catch (error) {
            modal.notification("Failure", "Something went wrong loading " + r.name);
            this.status = Status.Failure;
            return;
        }
        const ruleset = response.data;
        const collection = exporter.jsonToFlow(ruleset.rules!);
        project.fromNodeCollection(collection);
        project.setCurrentProject(r.id!, r.name!, collection.description!);
        this.dispatchEvent(new CustomEvent("ruleloaded"));
    }

    private getButton = (r: GlobalRuleset) => {
        return html`<div class="list-button" @click="${() => { this.loadRule(r); }}">${Utilities.ellipsis(r.name!, 50)} 
        ${r.error ? html`<or-icon title="${Utilities.humanLike(r.status!)}" icon="alert-outline"></or-icon>` : null}
        ${r.enabled ? null : html`<or-icon title="${i18next.t("disabled") as string}" icon="sleep"></or-icon>`}
        </div>`;
    }
}
