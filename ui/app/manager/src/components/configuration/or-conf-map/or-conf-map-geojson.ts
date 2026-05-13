import { GeoJsonConfig } from "@openremote/model";
import { OrMwcDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import { html, LitElement } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import "@openremote/or-components/or-ace-editor";
import { OrAceEditorChangedEvent } from "@openremote/or-components/or-ace-editor";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";

@customElement("or-conf-map-geojson")
export class OrConfMapGeoJson extends LitElement {

    @property()
    protected geoJson?: GeoJsonConfig;

    @state()
    protected _jsonValid: boolean = true;

    protected _dialog: OrMwcDialog
    // Value of or-ace-editor without state to prevent UI update
    protected _aceEditorValue: string;

    /* -------------- */

    protected render() {
        return html`<or-mwc-input type="${InputType.BUTTON}" label="geoJson" outlined icon="pencil" @or-mwc-input-changed="${this.showDialog}"></or-mwc-input>`
    }

    protected showDialog() {
        this._dialog = showDialog(new OrMwcDialog()
            .setHeading("GeoJSON editor")
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
                </style>`)
            .setActions([
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
            ])
            .setContent(() => html`
                <or-ace-editor .value="${this.geoJson?.source}" @or-ace-editor-changed="${(ev: OrAceEditorChangedEvent) => {
                    this._jsonValid = ev.detail.valid;
                    if (this._jsonValid) {
                        this._aceEditorValue = ev.detail.value;
                    }}}"
                ></or-ace-editor>`
            ).setDismissAction(null));
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
