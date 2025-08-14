/*
 * Copyright 2025, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import {i18next, translate} from "@openremote/or-translate";
import {html, LitElement, PropertyValues } from "lit";
import { customElement, property, query, state } from "lit/decorators.js";
import {style} from "./or-rule-viewer";
import { InputType, OrMwcInput } from "@openremote/or-mwc-components/or-mwc-input";
import {OrRulesGroupNameChangeEvent} from "./index";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";

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
    protected _lastSaved?: string;

    @query("#rule-name")
    protected _groupNameInput?: OrMwcInput;

    static get styles() {
        return [style];
    }

    willUpdate(changedProps: PropertyValues) {
        if(changedProps.has("group")) {
            this._groupNameInput?.focus(); // regain focus on the input (if lost)
        }
        return super.willUpdate(changedProps);
    }

    render() {
        return html`
            <div id="main-wrapper" class="wrapper">
                <div id="rule-header">
                    <or-mwc-input id="rule-name" outlined .type="${InputType.TEXT}" .label="${i18next.t("ruleGroupName")}" focused
                                  .value="${this.group}" ?disabled="${this.readonly}" required minlength="1" maxlength="255" @input="${this._onGroupNameInput}"
                    ></or-mwc-input>
                    <div id="rule-header-controls">
                        <or-mwc-input .type="${InputType.BUTTON}" id="save-btn" label="save" raised ?disabled="${this._cannotSave()}" @or-mwc-input-changed="${this._onSaveClicked}"></or-mwc-input>
                    </div>
                </div>
            </div>
        `
    }

    protected _onGroupNameInput(ev: InputEvent) {
        const value = this._groupNameInput?.nativeValue;
        if(value) this._changeName(value);
        this.requestUpdate();
    }

    protected _changeName(name: string) {
        if(name.length >= 3 && name.length <= 255) {
            this.group = name;
        }
    }

    protected _cannotSave() {
        return this.readonly || !this._groupNameInput?.nativeValue || !this._groupNameInput?.valid || (this._groupNameInput?.nativeValue || "") === this._lastSaved;
    }

    protected _onSaveClicked(): void {
        if(this.group && !this._cannotSave()) {
            const success = this.dispatchEvent(new OrRulesGroupNameChangeEvent(this.group));
            if(success) {
                this._lastSaved = this.group;
            } else {
                showSnackbar(undefined, 'ruleGroupExistsError');
            }
        }
    }
}
