import {OrWidgetConfig, OrWidgetEntity} from "./or-base-widget";
import {style} from "../style";
import {SettingsPanelType, widgetSettingsStyling} from "../or-dashboard-settingspanel";
import {Asset, Attribute, AttributeRef, DashboardWidget } from "@openremote/model";
import { html, LitElement, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import "@openremote/or-gauge";
import { i18next } from "@openremote/or-translate";
import manager from "@openremote/core";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import { when } from "lit/directives/when.js";

export interface GaugeWidgetConfig extends OrWidgetConfig {
    displayName: string;
    attributeRefs: AttributeRef[];
    thresholds: [number, string][];
    deltaFormat: "absolute" | "percentage";
    min: number,
    max: number
}

export class OrGaugeWidget implements OrWidgetEntity {

    readonly DISPLAY_MDI_ICON: string = "gauge";
    readonly DISPLAY_NAME: string = "Gauge";
    readonly MIN_COLUMN_WIDTH: number = 2;
    readonly MIN_PIXEL_HEIGHT: number = 150;
    readonly MIN_PIXEL_WIDTH: number = 150;

    getDefaultConfig(widget: DashboardWidget): OrWidgetConfig {
        return {
            displayName: widget.displayName,
            attributeRefs: [],
            thresholds: [[0, "#4caf50"],[75, "#ff9800"],[90, "#ef5350"]], // colors from https://mui.com/material-ui/customization/palette/ as reference (since material has no official colors)
            deltaFormat: "absolute",
            min: 0,
            max: 100
        } as GaugeWidgetConfig;
    }

    getSettingsHTML(widget: DashboardWidget, realm: string) {
        return html`<or-gauge-widgetsettings .widget="${widget}" realm="${realm}"></or-gauge-widgetsettings>`;
    }

    getWidgetHTML(widget: DashboardWidget, editMode: boolean, realm: string) {
        return html`<or-gauge-widget .widget="${widget}" .editMode="${editMode}" realm="${realm}" style="height: 100%; overflow: hidden;"></or-gauge-widget>`;
    }

}

@customElement("or-gauge-widget")
export class OrGaugeWidgetContent extends LitElement {

    @property()
    public readonly widget?: DashboardWidget;

    @property()
    public editMode?: boolean;

    @property()
    public realm?: string;

    @state()
    private assets: Asset[] = [];

    @state()
    private assetAttributes: [number, Attribute<any>][] = [];


    render() {
        console.warn(this.widget?.widgetConfig.attributeRefs);
        console.error(this.assets);
        console.error(this.assetAttributes);
        return html`
            ${when(this.assets && this.assetAttributes && this.assets.length > 0 && this.assetAttributes.length > 0, () => { 
                return html`
                    <or-gauge .asset="${this.assets[0]}" .assetAttribute="${this.assetAttributes[0]}" .thresholds="${this.widget?.widgetConfig.thresholds}"
                              .min="${this.widget?.widgetConfig.min}" .max="${this.widget?.widgetConfig.max}"
                              style="height: 100%;"></or-gauge>
                `;
            }, () => {
                return html`
                    <span>No attributes selected.</span>
                `
            })}
            <!--<or-gauge .attrRef="${this.widget?.widgetConfig.attributeRefs[0]}"></or-gauge>-->
        `
    }

    willUpdate(changedProperties: Map<string, any>) {
        console.error(changedProperties);
    }

    updated(changedProperties: Map<string, any>) {
        console.error(changedProperties);
        if(changedProperties.has("widget") || changedProperties.has("editMode")) {
            this.fetchAssets(this.widget?.widgetConfig).then((assets) => {
                console.error("Fetched the following assets:");
                console.error(assets);
                this.assets = assets!;
                this.assetAttributes = this.widget?.widgetConfig.attributeRefs.map((attrRef: AttributeRef) => {
                    const assetIndex = assets!.findIndex((asset) => asset.id === attrRef.id);
                    const foundAsset = assetIndex >= 0 ? assets![assetIndex] : undefined;
                    return foundAsset && foundAsset.attributes ? [assetIndex, foundAsset.attributes[attrRef.name!]] : undefined;
                }).filter((indexAndAttr: any) => !!indexAndAttr) as [number, Attribute<any>][];
                this.requestUpdate();
            });
        }
    }

    // Fetching the assets according to the AttributeRef[] input in DashboardWidget if required. TODO: Simplify this to only request data needed for attribute list
    async fetchAssets(config: OrWidgetConfig | any): Promise<Asset[] | undefined> {
        console.error("Fetching assets..")
        if(config.attributeRefs && config.attributeRefs.length > 0) {
            console.error("Making the call..")
            let assets: Asset[] = [];
            await manager.rest.api.AssetResource.queryAssets({
                ids: config.attributeRefs?.map((x: AttributeRef) => x.id) as string[]
            }).then(response => {
                console.error(response.data);
                assets = response.data;
            }).catch((reason) => {
                console.error(reason);
                showSnackbar(undefined, i18next.t('errorOccurred'));
            });
            return assets;
        } else {
            console.error("Error: attributeRefs are not present in widget config!");
        }
    }
}



@customElement("or-gauge-widgetsettings")
export class OrGaugeWidgetSettings extends LitElement {

    @property({hasChanged(oldVal, newVal) { return JSON.stringify(oldVal) == JSON.stringify(newVal); }})
    protected readonly widget?: DashboardWidget;

    // Default values
    private expandedPanels: string[] = [i18next.t('attributes'), i18next.t('values'), i18next.t('thresholds')];
    private loadedAssets: Asset[] = [];

    static get styles() {
        return [style, widgetSettingsStyling];
    }

    updated(changedProperties: Map<string, any>) {
        console.error(changedProperties);
    }
    shouldUpdate(changedProperties: Map<string, any>) {
        console.error(changedProperties);
        return super.shouldUpdate(changedProperties);
    }

    // UI Rendering
    render() {
        console.error("[or-gauge-widgetsettings] Rendering..");
        const config = this.widget?.widgetConfig as GaugeWidgetConfig;
        console.error(config);
        return html`
            <div>
                ${this.generateExpandableHeader(i18next.t('attributes'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('attributes')) ? html`
                    <or-dashboard-settingspanel .type="${SettingsPanelType.SINGLE_ATTRIBUTE}" .widget="${this.widget}"
                                                @updated="${(event: CustomEvent) => { this.onAttributesUpdate(event.detail.changes); }}"
                    ></or-dashboard-settingspanel>
                ` : null}
            </div>
            <div>
                ${this.generateExpandableHeader(i18next.t('values'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('values')) ? html`
                    <div style="padding: 12px 24px 48px 24px; display: flex; flex-direction: column; gap: 16px;">
                        <div style="display: flex; gap: 8px;">
                            <or-mwc-input type="${InputType.NUMBER}" label="${i18next.t('min')}" .value="${this.widget?.widgetConfig.min}"
                                          @or-mwc-input-changed="${(event: CustomEvent) => {
                                              this.widget!.widgetConfig.min = event.detail.value;
                                              (this.widget!.widgetConfig as GaugeWidgetConfig).thresholds.sort((x, y) => (x[0] < y[0]) ? -1 : 1).forEach((threshold, index) => {
                                                  if(threshold[0] < event.detail.value || (index == 0 && threshold[0] != event.detail.value)) {
                                                      (this.widget!.widgetConfig as GaugeWidgetConfig).thresholds[index][0] = event.detail.value;
                                                  }
                                              });
                                              this.requestUpdate("widget");
                                              this.forceParentUpdate(new Map<string, any>([['widget', this.widget]]));
                                          }}"
                            ></or-mwc-input>
                            <or-mwc-input type="${InputType.NUMBER}" label="${i18next.t('max')}" .value="${this.widget?.widgetConfig.max}"
                                          @or-mwc-input-changed="${(event: CustomEvent) => {
                                              this.widget!.widgetConfig.max = event.detail.value;
                                              this.forceParentUpdate(new Map<string, any>([['widget', this.widget]]));
                                          }}"
                            ></or-mwc-input>
                        </div>
                    </div>
                ` : null}
            </div>
            <div>
                ${this.generateExpandableHeader(i18next.t('thresholds'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('thresholds')) ? html`
                    <or-dashboard-settingspanel .type="${SettingsPanelType.THRESHOLDS}" .widget="${this.widget}"
                                                @updated="${(event: CustomEvent) => { this.forceParentUpdate(event.detail.changes, false); }}">
                    </or-dashboard-settingspanel>
                ` : null}
            </div>
        `
    }


    /* ------------------------------ */

    onAttributesUpdate(changes: Map<string, any>) {
        console.error(changes);
        if(changes.has('loadedAssets')) {
            this.loadedAssets = changes.get('loadedAssets');
        }
        if(changes.has('widget')) {
            const widget = changes.get('widget') as DashboardWidget;
            if(widget.widgetConfig.attributeRefs.length > 0) {
                this.widget!.displayName = this.loadedAssets[0].name + " - " + this.loadedAssets[0].attributes![widget.widgetConfig.attributeRefs[0].name].name;
            }
        }
        this.forceParentUpdate(changes, false);
    }

    // Method to update the Grid. For example after changing a setting.
    forceParentUpdate(changes: Map<string, any>, force: boolean = false) {
        this.requestUpdate();
        this.dispatchEvent(new CustomEvent('updated', {detail: {changes: changes, force: force}}));
    }

    generateExpandableHeader(name: string): TemplateResult {
        return html`
            <span class="expandableHeader panel-title" @click="${() => { this.expandPanel(name); }}">
                <or-icon icon="${this.expandedPanels.includes(name) ? 'chevron-down' : 'chevron-right'}"></or-icon>
                <span style="margin-left: 6px; height: 25px; line-height: 25px;">${name}</span>
            </span>
        `
    }
    expandPanel(panelName: string): void {
        if (this.expandedPanels.includes(panelName)) {
            const indexOf = this.expandedPanels.indexOf(panelName, 0);
            if (indexOf > -1) {
                this.expandedPanels.splice(indexOf, 1);
            }
        } else {
            this.expandedPanels.push(panelName);
        }
        this.requestUpdate();
    }
}
