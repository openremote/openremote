import { GeoJsonConfig } from "@openremote/model";
import {DialogAction, OrMwcDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import { i18next } from "@openremote/or-translate";
import { html, LitElement } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import "@openremote/or-components/or-ace-editor";
import { OrAceEditorChangedEvent } from "@openremote/or-components/or-ace-editor";

@customElement("or-conf-map-geojson")
export class OrConfMapGeoJson extends LitElement {

    @property()
    protected geoJson: GeoJsonConfig;

    @state()
    protected _dialog: OrMwcDialog

    @state()
    protected _jsonValid: boolean;

    protected openJsonEditor() {
        const dialogActions: DialogAction[] = [
            {
                actionName: "cancel",
                content: i18next.t("cancel")
            },
            {
                actionName: "ok",
                content: i18next.t("update"),
                disabled: this._jsonValid,
                action: () => {
                    if(this._jsonValid) {
                        this.dispatchEvent(new CustomEvent("update", { detail: { value: this.geoJson }}))
                    }
                }
            },
        ];
        this._dialog = new OrMwcDialog()
            .setActions(dialogActions)
            .setHeading("GeoJSON editor")
            .setContent(html`
                <or-ace-editor .value="${this.geoJson?.source}" @or-ace-editor-changed="${(ev: OrAceEditorChangedEvent) => {
                try {
                    this.geoJson = {
                        source: JSON.parse(ev.detail.value),
                        layers: []
                    } as GeoJsonConfig;
                } catch (err) {
                    console.warn(err);
                    this._jsonValid = false;
                }
                this._jsonValid = ev.detail.valid;
            }}"></or-ace-editor>
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

        showDialog(this._dialog);
    }

    render() {
        return html`
            <or-mwc-input type="button" label="JSON" outlined icon="pencil" @click="${() => {this.openJsonEditor()}}"></or-mwc-input>
        `
    }
}
