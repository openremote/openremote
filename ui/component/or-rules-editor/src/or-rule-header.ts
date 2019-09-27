import {customElement, html, LitElement, property} from "lit-element";
import "@openremote/or-translate";
import "@openremote/or-attribute-input";
import "@openremote/or-select";
import "@openremote/or-icon";
import {TenantRuleset} from "@openremote/model";

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
            this.requestUpdate();
        }
    }

    protected render() {

        return html`
            <div id="container">
                    <input id="title" ?disabled="${this.readonly}" required placeholder=" " @change="${this.changeName}" type="text" minlength="3" maxlength="255" .value="${this.ruleset ? this.ruleset.name : null}" />
                    
                    <div id="controls">
                        <span id="toggle-label" ?data-disabled="${!this.ruleset!.id}"><or-translate value="active"></or-translate></span>
                        <label id="toggle" ?data-disabled="${!this.ruleset!.id}" @click="${this.toggleEnabled}" ?data-checked="${this.ruleset!.enabled}">
                            <span></span>
                        </label>                    

                        ${this.ruleset ? html`
                            <button id="save-btn" ?disabled="${!this.saveEnabled}" @click="${this.saveRuleset}"><or-translate value="save"></or-translate></button>
                        ` : ``}
                    </div>                        
            </div>
        `;
    }
}
