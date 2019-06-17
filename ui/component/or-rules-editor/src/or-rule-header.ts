import {html, LitElement, property, customElement} from "lit-element";
import "@openremote/or-translate";
import "@openremote/or-input";
import "@openremote/or-select";
import "@openremote/or-icon";
import {TenantRuleset, Rule} from "@openremote/model";

import {headerStyle} from "./style";
import {OrRuleChangedEvent} from "./index";

@customElement("or-rule-header")
class OrRuleHeader extends LitElement {

    static get styles() {
        return headerStyle;
    }

    @property({type: Object})
    public ruleset?: TenantRuleset;

    @property({type: Boolean})
    public saveEnabled: boolean = false;

    public readonly: boolean = false;

    public changeName(e: any) {
        const value = e.target.value;
        if (this.ruleset) {
            this.ruleset.name = value;
            this.dispatchEvent(new OrRuleChangedEvent());
        }
    }

    public saveRuleset() {
        const event = new CustomEvent("rules:save-ruleset", {
            bubbles: true,
            composed: true
        });
        this.dispatchEvent(event);
    }

    public toggleEnabled() {
        if (this.ruleset) {
            this.ruleset.enabled = !this.ruleset.enabled;
            this.dispatchEvent(new OrRuleChangedEvent());
        }
    }

    protected render() {

        return html`
            <div class="rule-container">
              
                    <div class="layout horizontal">
                        <input ?disabled="${this.readonly}" required placeholder=" " @change="${this.changeName}" type="text" minlength="3" maxlength="255" .value="${this.ruleset ? this.ruleset.name : null}" />
                        
                        <div class="layout horizontal" style="margin-left: auto;">
                            <span style="margin: 9px 0;" class="toggle-label" ?data-disabled="${!this.ruleset!.id}"><or-translate value="active"></or-translate></span>
                            <label class="switch" ?data-disabled="${!this.ruleset!.id}">
                                <input @change="${this.toggleEnabled}" ?disabled="${!this.ruleset!.id || this.readonly}" ?checked="${this.ruleset!.enabled}" type="checkbox">
                                <span class="slider round"></span>
                            </label>                    

                            ${this.ruleset ? html`
                                <button ?disabled="${!this.saveEnabled}" @click="${this.saveRuleset}"><or-translate value="save"></or-translate></button>
                            ` : ``}
                        </div>
                        
                    </div>
            </div>
        `;
    }
}
