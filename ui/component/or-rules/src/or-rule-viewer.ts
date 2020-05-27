import {css, customElement, html, LitElement, property, PropertyValues, query, TemplateResult} from "lit-element";
import {
    OrRulesRuleChangedEvent,
    OrRulesRuleUnsupportedEvent,
    OrRulesSaveEndEvent,
    OrRulesSaveStartEvent,
    RulesConfig,
    RuleView
} from "./index";
import {ClientRole, RulesetLang, RulesetUnion} from "@openremote/model";
import manager from "@openremote/core";
import "./json-viewer/or-rule-json-viewer";
import "./or-rule-text-viewer";
import "./flow-viewer/components/flow-editor";
import "@openremote/or-input";
import {translate} from "@openremote/or-translate";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import i18next from "i18next";

// language=CSS
export const style = css`

    :host {
        display: block;
        height: 100%;
        width: 100%;
        overflow-y: auto;
    }

    .wrapper {
        display: flex;
        flex-direction: column;
        align-items: center;
        height: 100%;
    }
    
    #rule-name {
        max-width: 400px;
        flex: 1 1 0;
        display: flex;
    }
    
    #rule-header {
        display: flex;
        align-items: center;
        width: 100%;
        box-sizing: border-box;
        min-height: var(--internal-or-rules-header-height);
        height: var(--internal-or-rules-header-height);
        z-index: 1;
        padding: 15px 20px;
        --or-icon-fill: var(--internal-or-rules-panel-color);
    }
    
    #rule-header-controls {
        margin-left: auto;
        display: flex;
        align-items: center;
    }
    
    #rule-header-controls > * {
        margin: 0 10px;
    }
    
    #active-wrapper {
        display: flex;
        align-items: center;
    }
    
    #active-wrapper > or-translate {
        margin-right: 10px;
    }
    
    #rule-view {
        flex-grow: 1;
        background-color: var(--internal-or-rules-background-color);
    }
`;

@customElement("or-rule-viewer")
export class OrRuleViewer extends translate(i18next)(LitElement) {

    static get styles() {
        return [
            style
        ];
    }

    @property({type: Object})
    public ruleset?: RulesetUnion;

    @property({type: Boolean})
    public readonly: boolean = false;

    @property({type: Boolean})
    public disabled: boolean = false;

    @property({type: Object})
    public config?: RulesConfig;

    @property({attribute: false})
    public modified = false;

    @property({attribute: false})
    protected _ruleValid = false;

    @property({attribute: false})
    protected _supported = true;

    @query("#rule-view")
    public view?: RuleView;

    protected _focusName = false;

    constructor() {
        super();

        this.addEventListener(OrRulesRuleChangedEvent.NAME, this._onRuleChanged);
        this.addEventListener(OrRulesRuleUnsupportedEvent.NAME, this._onRuleUnsupported);
    }

    public get valid() {
        return this.ruleset && this.view && this._ruleValid && this.ruleset.name && this.ruleset.name.length >= 3 && this.ruleset.name.length < 255;
    }

    public shouldUpdate(_changedProperties: PropertyValues): boolean {
        if (_changedProperties.has("ruleset")) {
            this._supported = true;
            this.modified = false;
            this._ruleValid = false;

            if (this.ruleset) {
                this._focusName = true;
            }
        }
        return super.shouldUpdate(_changedProperties);
    }

    protected render(): TemplateResult | void {

        if (!this.ruleset) {
            return html`<div class="wrapper" style="justify-content: center"><or-translate value="noRuleSelected"></or-translate></div>`;
        }

        let viewer: TemplateResult | string = ``;
        if (!this._supported || this.ruleset.lang === RulesetLang.GROOVY || this.ruleset.lang === RulesetLang.JAVASCRIPT) {
            viewer = html`
                <or-rule-text-viewer id="rule-view" .ruleset="${this.ruleset}" .config="${this.config}" .readonly="${this.readonly}"></or-rule-text-viewer>
            `;
        } else {
            switch (this.ruleset.lang!) {
                case RulesetLang.JSON:
                    viewer = html`<or-rule-json-viewer id="rule-view" .ruleset="${this.ruleset}" .config="${this.config}" .readonly="${this.readonly}"></or-rule-json-viewer>`;
                    break;
                case RulesetLang.FLOW:
                    viewer = html`<flow-editor id="rule-view" .ruleset="${this.ruleset}" .readonly="${this.readonly}"></flow-editor>`;
                    break;
                default:
                    viewer = html`<div class="wrapper"><or-translate value="notSupported"></or-translate></div>`;
            }
        }

        // TODO: load the appropriate viewer depending on state and ruleset language
        return html`
            <div class="wrapper">            
                <div id="rule-header">
                    <or-input id="rule-name" outlined .type="${InputType.TEXT}" .label="${i18next.t("ruleName")}" ?focused="${this._focusName}" .value="${this.ruleset ? this.ruleset.name : null}" ?disabled="${this._isReadonly()}" required minlength="3" maxlength="255" @or-input-changed="${(e: OrInputChangedEvent) => this._changeName(e.detail.value)}"></or-input>
                    <div id="rule-header-controls">
                        
                        <span id="active-wrapper">
                            <or-translate value="active"></or-translate>
                            <or-input .type="${InputType.SWITCH}" .value="${this.ruleset && this.ruleset.enabled}" ?disabled="${!this.ruleset.id}" @or-input-changed="${this._toggleEnabled}"></or-input>
                        </span>
           
                        <or-input .type="${InputType.BUTTON}" id="save-btn" .label="${i18next.t("save")}" raised ?disabled="${this._cannotSave()}" @click="${this._doSave}" @or-input-changed="${this._doSave}"></or-input>
                    </div>                        
                </div>

                ${viewer}
            </div>
        `;
    }

    protected updated(_changedProperties: PropertyValues): void {
        if (_changedProperties.has("ruleset")) {
            if (this.ruleset && this.view) {
                this._ruleValid = this.view.validate();
            }
        }
    }

    protected _isReadonly() {
        return this.readonly || !manager.hasRole(ClientRole.WRITE_RULES);
    }

    protected _cannotSave() {
        return this._isReadonly() || !this.ruleset || !this.modified || !this.valid;
    }

    protected _changeName(name: string) {
        if (this.ruleset && this.ruleset.name !== name) {
            this.ruleset.name = name;
            this.modified = true;
            this.requestUpdate();
        }
    }

    protected _onRuleChanged(e: OrRulesRuleChangedEvent) {
        this.modified = true;
        this._ruleValid = e.detail;
    }

    protected _doSave() {
        if (!this.ruleset || !this.view) {
            return;
        }

        this.disabled = true;
        this.view.beforeSave();
        this.dispatchEvent(new OrRulesSaveStartEvent());

        if (this.ruleset.type === "tenant") {
            if (this.ruleset.id) {
                manager.rest.api.RulesResource.updateTenantRuleset(this.ruleset.id!, this.ruleset).then((response) => {
                    this.disabled = false;
                    this.modified = false;
                    this.dispatchEvent(new OrRulesSaveEndEvent(true));
                }).catch(reason => {
                    console.error("Failed to save ruleset: " + reason);
                    this.disabled = false;
                    this.dispatchEvent(new OrRulesSaveEndEvent(false));
                });
            } else {
                manager.rest.api.RulesResource.createTenantRuleset(this.ruleset).then((response) => {
                    if (this.ruleset) {
                        this.ruleset.id = response.data;
                    }
                    this.disabled = false;
                    this.modified = false;
                    this.dispatchEvent(new OrRulesSaveEndEvent(true));
                }).catch(reason => {
                    console.error("Failed to save ruleset: " + reason);
                    this.disabled = false;
                    this.dispatchEvent(new OrRulesSaveEndEvent(false));
                });
            }
        } else if (this.ruleset.type === "global") { 
            if (this.ruleset.id) {
                manager.rest.api.RulesResource.updateGlobalRuleset(this.ruleset.id!, this.ruleset).then((response) => {
                    this.disabled = false;
                    this.modified = false;
                    this.dispatchEvent(new OrRulesSaveEndEvent(true));
                }).catch(reason => {
                    console.error("Failed to save ruleset: " + reason);
                    this.disabled = false;
                    this.dispatchEvent(new OrRulesSaveEndEvent(false));
                });
            } else {
                manager.rest.api.RulesResource.createGlobalRuleset(this.ruleset).then((response) => {
                    if (this.ruleset) {
                        this.ruleset.id = response.data;
                    }
                    this.disabled = false;
                    this.modified = false;
                    this.dispatchEvent(new OrRulesSaveEndEvent(true));
                }).catch(reason => {
                    console.error("Failed to save ruleset: " + reason);
                    this.disabled = false;
                    this.dispatchEvent(new OrRulesSaveEndEvent(false));
                });
            }
        }
    }

    protected _onRuleUnsupported() {
        this._supported = false;
    }

    protected _toggleEnabled() {
        if (this.ruleset) {
            this.ruleset.enabled = !this.ruleset.enabled;
            this.modified = true;
            this.requestUpdate();
        }
    }
}