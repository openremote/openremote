import {i18next, translate} from "@openremote/or-translate";
import {html, LitElement } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import {style} from "./or-rule-viewer";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {OrRulesGroupNameChangeEvent} from "./index";

@customElement("or-rule-group-viewer")
export class OrRuleGroupViewer extends translate(i18next)(LitElement) {

    /**
     * Name of the group
     */
    @property({type: String})
    public group?: string;

    @property({type: Boolean})
    public readonly = false;

    @state()
    protected _modified = false;

    static get styles() {
        return [style];
    }

    render() {
        return html`
            <div id="main-wrapper" class="wrapper">
                <div id="rule-header">
                    <or-mwc-input id="rule-name" outlined .type="${InputType.TEXT}" .label="${i18next.t("ruleGroupName")}"
                                  .value="${this.group}" ?disabled="${this.readonly}" required minlength="3" maxlength="255"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => this._changeName(e.detail.value)}"
                    ></or-mwc-input>
                    <div id="rule-header-controls">
                        <or-mwc-input .type="${InputType.BUTTON}" id="save-btn" label="save" raised ?disabled="${this._cannotSave()}" @or-mwc-input-changed="${this._onSaveClicked}"></or-mwc-input>
                    </div>
                </div>
            </div>
        `
    }

    protected _changeName(name: string) {
        if(name.length >= 3 && name.length <= 255) {
            this.group = name;
            this._modified = true;
        }
    }

    protected _cannotSave() {
        return this.readonly || !this.group || !this._modified || !this.valid;
    }

    public valid() {
        return !!this.group && this.group.length >= 3 && this.group.length <= 255;
    }

    protected _onSaveClicked(): void {
        if(this.group && !this._cannotSave()) {
            this.dispatchEvent(new OrRulesGroupNameChangeEvent(this.group));
            this._modified = false;
        }
    }
}
