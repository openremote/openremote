import {LngLatLike} from "maplibre-gl";
import {Asset, AssetModelUtil, AttributeRef, DashboardWidget, DashboardWidgetType } from "@openremote/model";
import {css, html, LitElement, TemplateResult, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { InputType, OrInputChangedEvent } from "../../or-mwc-components/lib/or-mwc-input";
import { showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import {OrAttributePicker, OrAttributePickerPickedEvent } from "@openremote/or-attribute-picker";
import {style} from './style';
import { getAssetDescriptorIconTemplate } from "@openremote/or-icon";
import {DefaultColor5, manager } from "@openremote/core";

const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

//language=css
const widgetSettingsStyling = css`
    
    /* ------------------------------- */
    .expandableHeader {
        display: flex;
        align-items: center;
        padding: 12px;
        background: #F0F0F0;
        width: 100%;
        border: none;
    }
    .switchMwcInputContainer {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }
    /* ---------------------------- */
    #attribute-list {
        overflow: auto;
        flex: 1 1 0;
        min-height: 150px;
        width: 100%;
        display: flex;
        flex-direction: column;
    }

    .attribute-list-item {
        cursor: pointer;
        display: flex;
        flex-direction: row;
        align-items: center;
        padding: 0;
        min-height: 50px;
    }

    .attribute-list-item-label {
        display: flex;
        flex: 1 1 0;
        line-height: 16px;
        flex-direction: column;
    }

    .attribute-list-item-bullet {
        width: 14px;
        height: 14px;
        border-radius: 7px;
        margin-right: 10px;
    }

    .attribute-list-item .button.delete {
        display: none;
    }

    .attribute-list-item:hover .button.delete {
        display: block;
    }

    /* ---------------------------- */
    .button-clear {
        background: none;
        visibility: hidden;
        color: ${unsafeCSS(DefaultColor5)};
        --or-icon-fill: ${unsafeCSS(DefaultColor5)};
        display: inline-block;
        border: none;
        padding: 0;
        cursor: pointer;
    }

    .attribute-list-item:hover .button-clear {
        visibility: visible;
    }

    .button-clear:hover {
        --or-icon-fill: var(--or-app-color4);
    }
    /* ---------------------------- */
`

/* ------------------------------------ */

// Interfaces that contain Configurations of each Widget Type.
// The database stores them in any type, however these can be used
// for type checking.

interface ChartWidgetConfig {
    displayName: string;
    attributeRefs: AttributeRef[];
    period?: 'year' | 'month' | 'week' | 'day' | 'hour' | 'minute' | 'second';
    timestamp?: Date;
    compareTimestamp?: Date;
    decimals: number;
    deltaFormat: "absolute" | "percentage";
    showTimestampControls: boolean;
    showLegend: boolean;
}
interface MapWidgetConfig {
    displayName?: string;
    center?: LngLatLike;
    zoom?: number;
}

/* ------------------------------------ */

@customElement("or-dashboard-widgetsettings")
export class OrDashboardWidgetsettings extends LitElement {

    static get styles() {
        return [unsafeCSS(tableStyle), widgetSettingsStyling, style]
    }

    @property({type: Object})
    protected selectedWidget: DashboardWidget | undefined;

    @state() // list of assets that are loaded in the list
    protected loadedAssets: Asset[] | undefined;

    @state()
    protected expandedPanels: string[];

    constructor() {
        super();
        this.expandedPanels = [];
        this.updateComplete.then(() => {

            // Setting what panels are expanded, depending on WidgetType
            switch (this.selectedWidget?.widgetType) {
                case DashboardWidgetType.CHART: {
                    this.expandedPanels = ['Attributes', 'Display']; break;
                }
                case DashboardWidgetType.MAP: {
                    this.expandedPanels = ['Display']; break;
                }
            }
        })
    }

    updated(changedProperties: Map<string, any>) {
        super.updated(changedProperties);
        console.log(changedProperties);
        if(changedProperties.has("selectedWidget")) {
            if(this.selectedWidget != null) {
                if(this.selectedWidget.widgetConfig == null) {
                    this.selectedWidget.widgetConfig = this.generateWidgetConfig(this.selectedWidget);
                }
                this.fetchAssets();
            }
        }
    }

    /* ------------------------------------ */

    // Default values that are used when no config is specified.
    // This is always the case when a new Widget has been created.
    generateWidgetConfig(widget: DashboardWidget): Object {
        switch (widget.widgetType) {
            case DashboardWidgetType.CHART: {
                return {
                    displayName: widget.displayName,
                    attributeRefs: [],
                    period: "day",
                    timestamp: new Date(),
                    decimals: 2,
                    deltaFormat: "absolute",
                    showTimestampControls: false,
                    showLegend: true
                } as ChartWidgetConfig
            }
            case DashboardWidgetType.MAP: {
                return {
                    displayName: widget.displayName
                } as MapWidgetConfig
            }
            default: {
                return {};
            }
        }
    }

    /* ------------------------------ */

    // Method to update the Grid. For example after changing a setting.
    forceParentUpdate() {
        this.dispatchEvent(new CustomEvent('update'));
    }

    deleteSelectedWidget() {
        this.dispatchEvent(new CustomEvent("delete", {detail: this.selectedWidget }));
    }

    /* --------------------------------------- */

    // Fetching the assets according to the AttributeRef[] input in DashboardWidget if required.
    fetchAssets() {
        if(this.selectedWidget?.widgetConfig?.attributeRefs != null) {
            manager.rest.api.AssetResource.queryAssets({
                ids: this.selectedWidget?.widgetConfig?.attributeRefs?.map((x: AttributeRef) => { return x.id; }) as string[]
            }).then(response => {
                this.loadedAssets = response.data;
            })
        }
    }

    protected render() {
        if(this.selectedWidget?.widgetType != null && this.selectedWidget.widgetConfig != null) {
            return this.generateHTML(this.selectedWidget.widgetType, this.selectedWidget.widgetConfig);
        }
        return html`<span>Unknown Error</span>`
    }




    /* ----------------------------------- */

    // UI generation of all settings fields. Depending on the WidgetType it will
    // return different HTML containing different settings.
    generateHTML(widgetType: DashboardWidgetType, widgetConfig: any): TemplateResult {
        let htmlContent: TemplateResult;
        switch (widgetType) {

            case DashboardWidgetType.CHART: {
                const chartConfig = widgetConfig as ChartWidgetConfig;
                htmlContent = html`
                    <div>
                        ${this.generateExpandableHeader('Attributes')}
                    </div>
                    <div>
                        ${this.expandedPanels.includes('Attributes') ? html`
                            <div style="padding: 12px;">
                                ${(chartConfig.attributeRefs == null || chartConfig.attributeRefs.length == 0) ? html`
                                    <span>No attributes connected.</span>
                                ` : undefined}
                                <div id="attribute-list">
                                    ${(chartConfig.attributeRefs != null && this.loadedAssets != null) ? chartConfig.attributeRefs.map((attributeRef: AttributeRef) => {
                                        const asset = this.loadedAssets?.find((x: Asset) => { return x.id == attributeRef.id; }) as Asset;
                                        return (asset != null) ? html`
                                            <div class="attribute-list-item">
                                                <span style="margin-right: 10px; --or-icon-width: 20px;">${getAssetDescriptorIconTemplate(AssetModelUtil.getAssetDescriptor(asset.type), undefined, undefined)}</span>
                                                <div class="attribute-list-item-label">
                                                    <span>${asset.name}</span>
                                                    <span style="font-size:14px; color:grey;">${attributeRef.name}</span>
                                                </div>
                                                <button class="button-clear" @click="${() => this.removeWidgetAttribute(attributeRef)}">
                                                    <or-icon icon="close-circle" ></or-icon>
                                                </button>
                                            </div>
                                        ` : undefined;
                                    }) : undefined}
                                </div>
                                <or-mwc-input .type="${InputType.BUTTON}" label="Attribute" icon="plus" style="margin-top: 16px;" @click="${() => this.openDialog()}"></or-mwc-input>
                            </div>
                        ` : null}
                    </div>
                    <div>
                        ${this.generateExpandableHeader('Display')}
                    </div>
                    <div>
                        ${this.expandedPanels.includes('Display') ? html`
                            <div style="padding: 24px 24px 48px 24px;">
                                <div>
                                    <or-mwc-input .type="${InputType.SELECT}" style="width: 100%;" .options="${['year', 'month', 'week', 'day', 'hour', 'minute', 'second']}" .value="${chartConfig.period}" label="Default timeframe"
                                                  @or-mwc-input-changed="${(event: OrInputChangedEvent) => { (this.selectedWidget?.widgetConfig as ChartWidgetConfig).period = event.detail.value; this.forceParentUpdate(); }}"
                                    ></or-mwc-input>
                                </div>
                                <div class="switchMwcInputContainer" style="margin-top: 18px;">
                                    <span>Show Timestamp Controls</span>
                                    <or-mwc-input .type="${InputType.SWITCH}" style="width: 70px;" .value="${chartConfig.showTimestampControls}"
                                                  @or-mwc-input-changed="${(event: OrInputChangedEvent) => { (this.selectedWidget?.widgetConfig as ChartWidgetConfig).showTimestampControls = event.detail.value; this.forceParentUpdate(); }}"
                                    ></or-mwc-input>
                                </div>
                                <div class="switchMwcInputContainer">
                                    <span>Show Legend</span>
                                    <or-mwc-input .type="${InputType.SWITCH}" style="width: 70px;" .value="${chartConfig.showLegend}"
                                                  @or-mwc-input-changed="${(event: OrInputChangedEvent) => { (this.selectedWidget?.widgetConfig as ChartWidgetConfig).showLegend = event.detail.value; this.forceParentUpdate(); }}"
                                    ></or-mwc-input>
                                </div>
                            </div>
                        ` : null}
                    </div>
                    <div>
                        ${this.generateExpandableHeader('Settings')}
                    </div>
                    <div>
                        ${this.expandedPanels.includes('Settings') ? html`
                            <div style="padding: 24px 24px 48px 24px;">
                                <div>
                                    <or-mwc-input .type="${InputType.SELECT}" style="width: 100%;" .options="${['absolute', 'percentage']}" .value="${chartConfig.deltaFormat}" label="Show value as"
                                                  @or-mwc-input-changed="${(event: OrInputChangedEvent) => { (this.selectedWidget?.widgetConfig as ChartWidgetConfig).deltaFormat = event.detail.value; this.forceParentUpdate(); }}"
                                    ></or-mwc-input>
                                </div>
                                <div style="margin-top: 18px;">
                                    <or-mwc-input .type="${InputType.NUMBER}" style="width: 100%;" .value="${chartConfig.decimals}" label="Decimals"
                                                  @or-mwc-input-changed="${(event: OrInputChangedEvent) => { (this.selectedWidget?.widgetConfig as ChartWidgetConfig).decimals = event.detail.value; this.forceParentUpdate(); }}"
                                    ></or-mwc-input>
                                </div>
                            </div>
                        ` : undefined}
                    </div>
                `
                break;
            }

            case DashboardWidgetType.MAP: {
                htmlContent = html`
                    <div>
                        ${this.generateExpandableHeader('Display')}
                    </div>
                    <div>
                        ${this.expandedPanels.includes('Display') ? html`
                            <div style="padding: 12px;">
                                <span>Setting 2</span>
                            </div>
                        ` : null}
                    </div>
                    <div>
                        ${this.generateExpandableHeader('Settings')}
                    </div>
                `
                break;
            }
            default: {
                htmlContent = html`<span>Unknown error.</span>`
            }
        }
        return html`
            ${htmlContent}
            <div id="actions" style="position: absolute; bottom: 20px; right: 20px;">
                <or-mwc-input type="${InputType.BUTTON}" outlined icon="delete" label="Delete Component" @click="${() => { this.deleteSelectedWidget(); }}"></or-mwc-input>
            </div>
        `
    }


    // UI generation of an expandable panel header.
    generateExpandableHeader(name: string): TemplateResult {
        return html`
            <button class="expandableHeader" @click="${() => { this.expandPanel(name); }}">
                <or-icon icon="${this.expandedPanels.includes(name) ? 'chevron-down' : 'chevron-right'}"></or-icon>
                <span style="margin-left: 6px;">${name}</span>
            </button>
        `
    }



    /* -------------------------------------------- */

    // Methods for changing the list of attributes. They get triggered when
    // the attribute picker returns a new list of attributes, or someone
    // removes an item from the list.

    setWidgetAttributes(selectedAttrs?: AttributeRef[]) {
        if(this.selectedWidget?.widgetConfig != null) {
            this.selectedWidget.widgetConfig.attributeRefs = selectedAttrs;
            this.requestUpdate("selectedWidget");
            this.forceParentUpdate();
        }
    }

    removeWidgetAttribute(attributeRef: AttributeRef) {
        if(this.selectedWidget?.widgetConfig?.attributeRefs != null) {
            this.selectedWidget.widgetConfig.attributeRefs.splice(this.selectedWidget.widgetConfig.attributeRefs.indexOf(attributeRef), 1);
            this.requestUpdate("selectedWidget");
            this.forceParentUpdate();
        }
    }



    /* ---------------------------------------- */

    // Method when a user opens or closes a expansion panel. (UI related)
    expandPanel(panelName: string): void {
        if(this.expandedPanels.includes(panelName)) {
            const indexOf = this.expandedPanels.indexOf(panelName, 0);
            if(indexOf > -1) { this.expandedPanels.splice(indexOf, 1); }
        } else {
            this.expandedPanels.push(panelName);
        }
        this.requestUpdate();
    }

    // Opening the attribute picker dialog, and listening to its result. (UI related)
    openDialog() {
        let dialog: OrAttributePicker;
        if(this.selectedWidget?.widgetConfig?.attributeRefs != null) {
            dialog = showDialog(new OrAttributePicker().setMultiSelect(true).setSelectedAttributes(this.selectedWidget?.widgetConfig?.attributeRefs)); //.setShowOnlyDatapointAttrs(true)) //.setMultiSelect(true).setSelectedAttributes(this.selectedWidget?.dataConfig?.attributes))
        } else {
            dialog = showDialog(new OrAttributePicker().setMultiSelect(true))
        }
        dialog.addEventListener(OrAttributePickerPickedEvent.NAME, (event: CustomEvent) => {
            this.setWidgetAttributes(event.detail);
        })
    }
}
