import { GeoJsonConfig } from "@openremote/model";
import {DialogAction, OrMwcDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import { html, LitElement } from "lit";
import { customElement, property, state, query } from "lit/decorators.js";
import "@openremote/or-components/or-ace-editor";
import { OrAceEditorChangedEvent } from "@openremote/or-components/or-ace-editor";

@customElement("or-conf-map-geojson")
export class OrConfMapGeoJson extends LitElement {

    @property()
    protected geoJson?: GeoJsonConfig;

    @state()
    protected _jsonValid: boolean = true;

    @query("#geojson-modal")
    protected _dialog: OrMwcDialog

    // Value of or-ace-editor without state to prevent UI update
    protected _aceEditorValue: string;


    /* -------------- */

    protected render() {
        const heading = "GeoJSON editor"
        const content = html`
            <or-ace-editor .value="${this.geoJson?.source}"
                           @or-ace-editor-changed="${(ev: OrAceEditorChangedEvent) => {
                               this._jsonValid = ev.detail.valid;
                               if(this._jsonValid) {
                                   this._aceEditorValue = ev.detail.value;
                               }}}"
            ></or-ace-editor>
        `;
        const actions: DialogAction[] = [
            {
                actionName: "close",
                content: "close"
            },
            {
                actionName: "ok",
                content: "update",
                disabled: !this._jsonValid,
                action: () => {
                    this.geoJson = this.parseGeoJson(this._aceEditorValue); // update with new value
                    this.dispatchEvent(new CustomEvent("update", { detail: { value: this.geoJson }}))
                }
            },
        ]
        const styles = html`
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
        `
        return html`
            <or-mwc-input type="button" label="geoJson" outlined icon="pencil" @click="${() => {this.openJsonEditor()}}"></or-mwc-input>
            <or-mwc-dialog id="geojson-modal" .heading="${heading}" .content="${content}" .actions="${actions}" .styles="${styles}" .dismissAction="${null}"></or-mwc-dialog>
        `
    }
    protected openJsonEditor() {
        this._dialog.open();
    }

    protected parseGeoJson(jsonString: string): GeoJsonConfig {
        let geoJsonObj;
        try {
            geoJsonObj = JSON.parse(jsonString);
        } catch (err) {
            console.warn(err);
            this._jsonValid = false;
            return;
        }
        return {
            source: geoJsonObj,
            layers: []
        } as GeoJsonConfig;
    }
}
