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
import { LitElement, css, html } from "lit";
import { customElement, property } from "lit/decorators.js";
import rest from "@openremote/rest";
import { i18next, translate } from "@openremote/or-translate";
import { Utilities } from "../utils";
import { Status } from "../models/status";
import { exporter, project, modal } from "./flow-editor";
let RuleBrowser = class RuleBrowser extends translate(i18next)(LitElement) {
    constructor() {
        super(...arguments);
        this.status = Status.Idle;
        this.retrievedRules = [];
        this.loadRule = (r) => __awaiter(this, void 0, void 0, function* () {
            this.status = Status.Loading;
            let response;
            try {
                response = yield rest.api.RulesResource.getGlobalRuleset(r.id);
            }
            catch (error) {
                modal.notification("Failure", "Something went wrong loading " + r.name);
                this.status = Status.Failure;
                return;
            }
            const ruleset = response.data;
            const collection = exporter.jsonToFlow(ruleset.rules);
            project.fromNodeCollection(collection);
            project.setCurrentProject(r.id, r.name, collection.description);
            this.dispatchEvent(new CustomEvent("ruleloaded"));
        });
        this.getButton = (r) => {
            return html `<div class="list-button" @click="${() => { this.loadRule(r); }}">${Utilities.ellipsis(r.name, 50)} 
        ${r.error ? html `<or-icon title="${Utilities.humanLike(r.status)}" icon="alert-outline"></or-icon>` : null}
        ${r.enabled ? null : html `<or-icon title="${i18next.t("disabled")}" icon="sleep"></or-icon>`}
        </div>`;
        };
    }
    static get styles() {
        return css `
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
    firstUpdated() {
        return __awaiter(this, void 0, void 0, function* () {
            this.status = Status.Loading;
            try {
                const response = yield rest.api.RulesResource.getGlobalRulesets();
                this.retrievedRules = response.data;
                this.status = Status.Success;
            }
            catch (error) {
                this.status = Status.Failure;
            }
        });
    }
    render() {
        let result = html ``;
        switch (this.status) {
            case Status.Loading:
                result = html `<span style="text-align: center;"><or-icon icon="loading"></or-icon></span>`;
                break;
            case Status.Success:
                result = html `${this.retrievedRules.length === 0 ?
                    html `<span>No rules to display</span>` :
                    this.retrievedRules.map((r) => this.getButton(r))}`;
                break;
            case Status.Failure:
                result = html `<span>Failed to load rules</span>`;
                break;
        }
        return html `
        <div style="display: flex; flex-direction: column; width: auto; align-items: stretch;">
        ${result}
        </div>`;
    }
};
__decorate([
    property({ type: Number })
], RuleBrowser.prototype, "status", void 0);
RuleBrowser = __decorate([
    customElement("rule-browser")
], RuleBrowser);
export { RuleBrowser };
//# sourceMappingURL=rule-browser.js.map