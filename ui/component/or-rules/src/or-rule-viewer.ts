import {css, customElement, html, LitElement, property, PropertyValues, query, TemplateResult, unsafeCSS} from "lit-element";
import {
    OrRulesSaveStartEvent,
    OrRulesRuleChangedEvent,
    OrRulesRuleUnsupportedEvent,
    OrRulesSaveEndEvent,
    RulesConfig,
    RuleView
} from "./index";
import {RulesetLang, RulesetUnion} from "@openremote/model";
import manager, {DefaultBoxShadow} from "@openremote/core";
import "./json-viewer/or-rule-json-viewer";
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
    }

    .wrapper {
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        height: 100%;
    }
    
    #rule-name {
        width: 400px;
    }
    
    #rule-header {
        display: flex;
        align-items: center;
        width: 100%;
        box-sizing: border-box;
        min-height: var(--internal-or-rules-header-height);
        height: var(--internal-or-rules-header-height);
        z-index: 1;
        padding: 0 20px;
        background-color: var(--internal-or-rules-header-background-color);
        --or-icon-fill: var(--internal-or-rules-panel-color);
        box-shadow: ${unsafeCSS(DefaultBoxShadow)};
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
    
    #rule-viewer {
        flex-grow: 1;
        background-color: var(--internal-or-rules-background-color);
    }
`;

@customElement("or-rule-viewer")
export class OrRuleViewer extends translate(i18next)(LitElement) {

    constructor() {
        super();

        this.addEventListener(OrRulesRuleChangedEvent.NAME, this._onRuleChanged);
        this.addEventListener(OrRulesRuleUnsupportedEvent.NAME, this._onRuleUnsupported);
    }

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

    @query("#rule-viewer")
    public view?: RuleView;

    public get valid() {
        return this.ruleset && this.view && this._ruleValid && this.ruleset.name && this.ruleset.name.length >= 3 && this.ruleset.name.length > 255;
    }

    public shouldUpdate(_changedProperties: PropertyValues): boolean {
        if (_changedProperties.has("ruleset")) {
            this._supported = true;

            if (!this.ruleset) {
                this.modified = false;
                this._ruleValid = false;
            }
        }
        return super.shouldUpdate(_changedProperties);
    }

    protected render(): TemplateResult | void {

        if (!this.ruleset) {
            return html`<div class="wrapper"><or-translate value="noRuleSelected"></or-translate></div>`;
        }

        let viewer: TemplateResult | string = ``;
        if (!this._supported) {
            viewer = html`
                <div>TEXT EDITOR</div>
            `;
        } else {
            switch (this.ruleset.lang!) {
                case RulesetLang.JSON:
                    viewer = html`<or-rule-json-viewer id="rule-viewer" .ruleset="${this.ruleset}" .config="${this.config}" .readonly="${this.readonly}"></or-rule-json-viewer>`;
                    break;
                case RulesetLang.GROOVY:
                case RulesetLang.JAVASCRIPT:
                    break;
                default:
                    viewer = html`<div class="wrapper"><or-translate value="notSupported"></or-translate></div>`;
            }
        }

        // TODO: load the appropriate viewer depending on state and ruleset language
        return html`
            <div class="wrapper">            
                <div id="rule-header">
                    <or-input id="rule-name" .type="${InputType.TEXT}" .value="${this.ruleset ? this.ruleset.name : null}" ?disabled="${this._isReadonly()}" fullwidth required minlength="3" maxlength="255" @or-input-changed="${(e: OrInputChangedEvent) => this._changeName(e.detail.value)}"></or-input>
                    
                    <div id="rule-header-controls">
                        <span id="active-wrapper">
                            <or-translate value="active"></or-translate>
                            
                            <or-input .type="${InputType.SWITCH}" .value="${this.ruleset && this.ruleset.enabled}" ?disabled="${this._cannotSave()}" @or-input-changed="${this._toggleEnabled}"></or-input>
                        </span>
    
                        <or-input .type="${InputType.BUTTON}" id="save-btn" .label="${i18next.t("save")}" raised ?disabled="${this._cannotSave()}" @or-input-changed="${this._doSave}"></or-input>
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
        return this.readonly || !manager.hasRole("write:rules");
    }

    protected _cannotSave() {
        return this._isReadonly() || !this.ruleset || !this.modified || !this.valid;
    }

    protected _changeName(name: string) {
        if (this.ruleset && this.ruleset.name !== name) {
            this.ruleset.name = name;
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
        }
    }

    protected _onRuleUnsupported() {
        this._supported = false;
    }

    protected _toggleEnabled() {
        if (this.ruleset) {
            this.ruleset.enabled = !this.ruleset.enabled;
            this.requestUpdate();
        }
    }
}