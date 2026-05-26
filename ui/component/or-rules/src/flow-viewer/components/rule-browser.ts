/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
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
