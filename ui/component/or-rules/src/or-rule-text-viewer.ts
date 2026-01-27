/*
 * Copyright 2025, OpenRemote Inc.
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
import {OrRulesRuleChangedEvent, RulesConfig, RuleView} from "./index";
import {css, html, LitElement, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import {RulesetLang, RulesetUnion} from "@openremote/model";
import "ace-builds/src-noconflict/mode-javascript";
import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-noconflict/mode-groovy";
import {OrAceEditor, OrAceEditorChangedEvent} from "@openremote/or-components/or-ace-editor";
import {createRef, ref, Ref} from "lit/directives/ref.js";

// language=CSS
const style = css`
    :host {
        display: flex;
        width: 100%;
        height: 100%;
    }

    #ace-editor {
        position: relative;
        height: 100%;
        width: 100%;
    }

    @media screen and (max-width: 1400px) {
        :host > * {
            flex-grow: 0;
        }

        :host {
            flex-direction: column;
        }
    }
`;

@customElement("or-rule-text-viewer")
export class OrRuleTextViewer extends LitElement implements RuleView {

    static get styles() {
        return style;
    }

    @property({attribute: false})
    public readonly?: boolean;

    @property({attribute: false})
    public config?: RulesConfig;

    @property({attribute: false})
    protected _ruleset!: RulesetUnion;

    protected _rules?: string;
    protected _aceEditor: Ref<OrAceEditor> = createRef();

    public set ruleset(ruleset: RulesetUnion) {
        if (this._ruleset === ruleset) {
            return;
        }

        this._ruleset = ruleset;

        if (!ruleset.rules) {
            // New ruleset so start a new rule
            this._rules = this._createRules();
        } else {
            this._rules = ruleset.rules;
        }
    }

    protected _createRules(): string {
        return "";
    }

    protected render(): TemplateResult | void {
        return html`
            <or-ace-editor ${ref(this._aceEditor)} @or-ace-editor-changed="${(ev: OrAceEditorChangedEvent) => this._onEditorChanged(ev)}" .mode="${this._getMode()}" .value="${this._getRulesString()}"></or-ace-editor>
        `;
    }

    protected _getMode() {
        switch (this._ruleset.lang) {
            case RulesetLang.JAVASCRIPT:
                return "ace/mode/javascript";
            case RulesetLang.GROOVY:
                return "ace/mode/groovy";
            case RulesetLang.JSON:
                return "ace/mode/json";
        }
    }

    protected _getRulesString() {
        if (!this._rules) {
            return "";
        }

        switch (this._ruleset.lang) {
            case RulesetLang.JAVASCRIPT:
            case RulesetLang.GROOVY:
                return this._rules;
            case RulesetLang.JSON:
                return JSON.stringify(JSON.parse(this._rules), null, 2);
        }
    }

    protected _onEditorChanged(ev: OrAceEditorChangedEvent) {
        const valid = ev.detail.valid;
        this.dispatchEvent(new OrRulesRuleChangedEvent(valid));
    }

    public beforeSave() {
        if (!this._aceEditor.value) {
            return;
        }

        this._ruleset.rules = this._aceEditor.value.getValue();
    }

    public validate(): boolean {
        return this._aceEditor.value ? this._aceEditor.value.validate() : false;
    }
}
