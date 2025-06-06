/*
 * Copyright 2022, OpenRemote Inc.
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
import { html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import "@openremote/or-components/or-ace-editor";
import { DialogAction } from "@openremote/or-mwc-components/or-mwc-dialog";
import { OrMwcDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import { createRef, Ref, ref } from "lit/directives/ref.js";
import { OrAceEditor, OrAceEditorChangedEvent } from "@openremote/or-components/or-ace-editor";
import { ManagerAppConfig, MapConfig } from "@openremote/model";

@customElement("or-conf-json")
export class OrConfJson extends LitElement {

    @property({attribute: false})
    public config: ManagerAppConfig | MapConfig = {};

    public heading: string;

    protected _aceEditor: Ref<OrAceEditor> = createRef();

    public beforeSave():false|string|undefined {
        if (!this._aceEditor.value) {
            return false;
        }
        const value = this._aceEditor.value.getValue()
        try {
            return JSON.parse(value || '{}');
        } catch (e) {
            return false;
        }
    }

    protected _showConfigDialog(){
        let dialog: OrMwcDialog;
        const _saveConfig = () => {
            const config = this.beforeSave()
            if (config) {
                this.config = config as ManagerAppConfig | MapConfig
                this.dispatchEvent(
                    new CustomEvent('saveLocalConfig',
                        {detail: {value: this.config}}
                    )
                )
                return true
            }
            return false
        }

        const dialogActions: DialogAction[] = [
            {
                actionName: "cancel",
                content: "cancel"
            },
            {
                actionName: "ok",
                content: "update",
                action: _saveConfig,
            }
        ];

        dialog = new OrMwcDialog()
            .setActions(dialogActions)
            .setHeading(this.heading)
            .setContent(html`
                <or-ace-editor 
                    ${ref(this._aceEditor)} 
                    .value="${this.config}"
                    @or-ace-editor-changed="${(ev: OrAceEditorChangedEvent) => {
                        const okButton = dialog.actions?.find(action => action.actionName === "ok");
                        if (okButton) {
                            okButton.disabled = !ev.detail.valid;
                            dialog.requestUpdate();
                        }
                    }}"
                ></or-ace-editor>
            `)
            .setStyles(html`
                <style>
                    .mdc-dialog__surface {
                        width: 1024px;
                        overflow-x: visible !important;
                        overflow-y: visible !important;
                    }
                    #dialog-content {
                        border-top-width: 1px;
                        border-top-style: solid;
                        border-bottom-width: 1px;
                        border-bottom-style: solid;
                        padding: 0;
                        overflow: visible;
                        height: 60vh;
                    }
                </style>
            `)
            .setDismissAction(null);

        showDialog(dialog);
    }

    render() {
        return html`
            <or-mwc-input type="button" label="JSON" outlined icon="pencil" @click="${() => {this._showConfigDialog()}}"></or-mwc-input>
        `
    }
}
