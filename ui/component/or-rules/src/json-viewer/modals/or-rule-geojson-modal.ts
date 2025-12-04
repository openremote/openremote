import { html, LitElement, css } from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {
    AssetDescriptor,
    AttributePredicate,
    AssetQuery,
    GeoJSONGeofencePredicate
} from "@openremote/model";
import {
    getAssetTypeFromQuery,
} from "../../index";
import "@openremote/or-mwc-components/or-mwc-input";
import {InputType} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next, translate} from "@openremote/or-translate"
import {OrRulesJsonRuleChangedEvent} from "../or-rule-json-viewer";

import {DialogAction, OrMwcDialog, OrMwcDialogOpenedEvent} from "@openremote/or-mwc-components/or-mwc-dialog";

@customElement("or-rule-geojson-modal")
export class OrRuleGeoJSONModal extends translate(i18next)(LitElement) {

    static styles = css`
        .help-text a { color: var(--or-app-color4, #3869B1); text-decoration: none; }
        .help-text a:hover { text-decoration: underline; }
        .example-section code { display: block; white-space: pre; font-family: 'Monaco','Menlo','Ubuntu Mono','Consolas','source-code-pro',monospace; }
        .editor-input or-mwc-input { width: 100%; }
    `;

    @property({type: Object})
    public assetDescriptor?: AssetDescriptor;

    @property({type: Object})
    public attributePredicate?: AttributePredicate;

    @property({type: Object})
    public query?: AssetQuery;

    @state()
    private geoJSONText: string = "";

    @state()
    private validationError: string | null = null;

    @state()
    private showExample: boolean = false;

    constructor() {
        super();
        this.addEventListener(OrMwcDialogOpenedEvent.NAME, this.initGeoJSONEditor)
    }

    initGeoJSONEditor() {
        const modal = this.shadowRoot!.getElementById('geojson-modal');
        if (!modal) return;

        // Initialize textarea with current value
        const valuePredicate = this.attributePredicate?.value as any;
        if (valuePredicate?.geoJSON) {
            this.geoJSONText = this.formatGeoJSON(valuePredicate.geoJSON);
        } else {
            this.geoJSONText = this.getExampleGeoJSON();
        }
        this.validationError = null;
    }

    protected getAttributeName(attributePredicate: AttributePredicate): string | undefined {
        return attributePredicate && attributePredicate.name ? attributePredicate.name.value : undefined;
    }

    protected setValuePredicateProperty(propertyName: string, value: any) {
        if(!this.attributePredicate) return;
        if(!this.attributePredicate.value) return;

        const valuePredicate = this.attributePredicate.value;

        (valuePredicate as any)[propertyName] = value;
        this.attributePredicate = {...this.attributePredicate};
        this.dispatchEvent(new OrRulesJsonRuleChangedEvent());
        this.requestUpdate();
    }

    protected validateAndSetGeoJSON(geoJSONText: string): boolean {
        try {
            // Try to parse as JSON
            const parsed = JSON.parse(geoJSONText);

            // Basic GeoJSON validation
            if (!parsed.type) {
                this.validationError = "GeoJSON must have a 'type' field";
                return false;
            }

            const validTypes = ["Feature", "FeatureCollection", "Polygon", "MultiPolygon",
                                "Point", "LineString", "MultiPoint", "MultiLineString", "GeometryCollection"];

            if (!validTypes.includes(parsed.type)) {
                this.validationError = `Invalid GeoJSON type: ${parsed.type}. Must be one of: ${validTypes.join(", ")}`;
                return false;
            }

            // Additional validation for specific types
            if (parsed.type === "Feature" && !parsed.geometry) {
                this.validationError = "Feature must have a 'geometry' field";
                return false;
            }

            if (parsed.type === "FeatureCollection" && !parsed.features) {
                this.validationError = "FeatureCollection must have a 'features' field";
                return false;
            }

            const geometryTypes = ["Polygon", "MultiPolygon", "Point", "LineString",
                                   "MultiPoint", "MultiLineString", "GeometryCollection"];
            if (geometryTypes.includes(parsed.type) && !parsed.coordinates && !parsed.geometries) {
                this.validationError = `Geometry type '${parsed.type}' must have 'coordinates' or 'geometries' field`;
                return false;
            }

            this.validationError = null;
            // Store as compact JSON (no extra whitespace)
            this.setValuePredicateProperty('geoJSON', JSON.stringify(parsed));
            return true;
        } catch (e) {
            this.validationError = `Invalid JSON: ${(e as Error).message}`;
            return false;
        }
    }

    protected formatGeoJSON(geoJSONString: string): string {
        try {
            const parsed = JSON.parse(geoJSONString);
            return JSON.stringify(parsed, null, 2);
        } catch (e) {
            return geoJSONString;
        }
    }

    protected getExampleGeoJSON(): string {
        return JSON.stringify({
            "type": "Polygon",
            "coordinates": [
                [
                    4.48556382778753,
                    51.91779377518452
                ],
                [
                    4.479700876335301,
                    51.91779377518452
                ],
                [
                    4.479700876335301,
                    51.91514625782321
                ],
                [
                    4.48556382778753,
                    51.91514625782321
                ],
                [
                    4.48556382778753,
                    51.91779377518452
                ]
            ]
        }, null, 2);
    }

    private toggleExample() {
        this.showExample = !this.showExample;
    }

    renderDialogHTML(value: GeoJSONGeofencePredicate) {
        const dialog: OrMwcDialog = this.shadowRoot!.getElementById("geojson-modal") as OrMwcDialog;
        if (!dialog) return;

        dialog.content = html`
            <div class="geojson-editor-wrapper">
                <div class="scroll-pane">
                    <div class="geojson-editor">
                        <div class="help-text">
                            ${i18next.t("geojsonEditorHelp", "Enter a valid GeoJSON Polygon, MultiPolygon, Feature, or FeatureCollection to define the geofence area.")}
                            <br />
                            <a href="https://geojson.io" target="_blank" rel="noopener noreferrer">geojson.io</a>
                            ${i18next.t("geojsonEditorDrawHelper", "can help you draw and export GeoJSON.")}
                        </div>
                        <div class="editor-input">
                            <or-mwc-input
                                .type="${InputType.JSON_OBJECT}"
                                
                                minrows="14"
                                maxrows="40"
                                class="${this.validationError ? 'error' : ''}"
                                .value="${this.geoJSONText}"
                                .label="${i18next.t("geoJSON", "GeoJSON")}"
                                placeholder="${i18next.t("geojsonPlaceholder", "Paste or edit your GeoJSON Feature/FeatureCollection here...")}"
                                @or-mwc-input-changed="${(e: CustomEvent) => {
                                    this.geoJSONText = e.detail.value;
                                    if (this.validationError) this.validationError = null;
                                }}"
                            ></or-mwc-input>
                        </div>

                        <div class="dialog-footer">
                            ${this.validationError ? html`<div class="error-message">${this.validationError}</div>` : html`<div class="error-spacer"></div>`}
                            <div class="example-toggle" @click="${() => this.toggleExample()}">
                                <or-icon icon="${this.showExample ? 'chevron-down' : 'chevron-right'}" size="14"></or-icon>
                                <span>${i18next.t("showExample", this.showExample ? "Hide example" : "Show example")}</span>
                            </div>
                            ${this.showExample ? html`
                                <div class="example-section">
                                    <strong>${i18next.t("example", "Example Polygon")}</strong>
                                    <code>${this.getExampleGeoJSON()}</code>
                                </div>
                            `: ''}
                        </div>
                    </div>
                </div>
            </div>`;
    }

    protected render() {
        if(!this.attributePredicate) return html``;
        if(!this.query) return html``;

        const valuePredicate = this.attributePredicate.value;
        if (!this.assetDescriptor || !valuePredicate) {
            return html``;
        }

        const attributeName = this.getAttributeName(this.attributePredicate);
        const assetType = getAssetTypeFromQuery(this.query);
        const value: GeoJSONGeofencePredicate = valuePredicate as any;

        const geoJSONPickerModalActions: DialogAction[] = [
            {
                actionName: "cancel",
                content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" label="cancel"></or-mwc-input>`,
                action: () => {
                    // Revert changes
                    this.validationError = null;
                }
            },
            {
                actionName: "ok",
                default: true,
                content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" label="ok"></or-mwc-input>`,
                action: () => {
                    // Validate and save
                    if (!this.validateAndSetGeoJSON(this.geoJSONText)) {
                        // Prevent dialog from closing if validation fails
                        return false;
                    }
                }
            }
        ];

        const geoJSONPickerModalOpen = () => {
            const dialog: OrMwcDialog = this.shadowRoot!.getElementById("geojson-modal") as OrMwcDialog;
            if (dialog) {
                dialog.dismissAction = null;
                dialog.open();
                this.renderDialogHTML(value);
            }
        };

        this.renderDialogHTML(value);

        return html`
            <or-mwc-input .type="${InputType.BUTTON}" label="${i18next.t("defineArea", "Define Area")}" @or-mwc-input-changed="${geoJSONPickerModalOpen}"></or-mwc-input>
            <or-mwc-dialog id="geojson-modal" heading="${i18next.t("geoJSONGeofence", "GeoJSON Geofence")}" .actions="${geoJSONPickerModalActions}"></or-mwc-dialog>
        `
    }
}
