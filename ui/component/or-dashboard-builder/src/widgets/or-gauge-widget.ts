import {OrWidgetConfig, OrWidgetEntity} from "./or-base-widget";
import {style} from "../style";
import {SettingsPanelType, widgetSettingsStyling} from "../or-dashboard-settingspanel";
import {Asset, Attribute, AttributeRef, DashboardWidget } from "@openremote/model";
import { html, LitElement, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import "@openremote/or-gauge";
import { i18next } from "@openremote/or-translate";
import manager, { Util } from "@openremote/core";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import {InputType} from "@openremote/or-mwc-components/or-mwc-input";
import { when } from "lit/directives/when.js";

export interface GaugeWidgetConfig extends OrWidgetConfig {
    displayName: string;
    attributeRefs: AttributeRef[];
    thresholds: [number, string][];
    decimals: number;
    min: number,
    max: number,
    valueType: string,
}

export class OrGaugeWidget implements OrWidgetEntity {

    readonly DISPLAY_MDI_ICON: string = "gauge";
    readonly DISPLAY_NAME: string = "Gauge";
    readonly MIN_COLUMN_WIDTH: number = 2;
    readonly MIN_PIXEL_HEIGHT: number = 0;
    readonly MIN_PIXEL_WIDTH: number = 0;

    getDefaultConfig(widget: DashboardWidget): OrWidgetConfig {
        return {
            displayName: widget.displayName,
            attributeRefs: [],
            thresholds: [[0, "#4caf50"],[75, "#ff9800"],[90, "#ef5350"]], // colors from https://mui.com/material-ui/customization/palette/ as reference (since material has no official colors)
            decimals: 0,
            min: 0,
            max: 100,
            valueType: 'number',
        } as GaugeWidgetConfig;
    }

    // Triggered every update to double check if the specification.
    // It will merge missing values, or you can add custom logic to process here.
    verifyConfigSpec(widget: DashboardWidget): GaugeWidgetConfig {
        return Util.mergeObjects(this.getDefaultConfig(widget), widget.widgetConfig, false) as GaugeWidgetConfig;
    }

    getWidgetHTML(widget: DashboardWidget, editMode: boolean, realm: string) {
        return html`<or-gauge-widget .widget="${widget}" .editMode="${editMode}" realm="${realm}"></or-gauge-widget>`;
    }

    getSettingsHTML(widget: DashboardWidget, realm: string) {
        return html`<or-gauge-widgetsettings .widget="${widget}" realm="${realm}"></or-gauge-widgetsettings>`;
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
        return html`
            ${when(this.assets && this.assetAttributes && this.assets.length > 0 && this.assetAttributes.length > 0, () => { 
                return html`
                    <or-gauge .asset="${this.assets[0]}" .assetAttribute="${this.assetAttributes[0]}" .thresholds="${this.widget?.widgetConfig.thresholds}"
                              .decimals="${this.widget?.widgetConfig.decimals}" .min="${this.widget?.widgetConfig.min}" .max="${this.widget?.widgetConfig.max}"
                              style="height: 100%; overflow: hidden;"></or-gauge>
                `;
            }, () => {
                return html`
                    <div style="height: 100%; display: flex; justify-content: center; align-items: center;">
                        <span>${i18next.t('noAttributeConnected')}</span>
                    </div>
                `
            })}
            <!--<or-gauge .attrRef="${this.widget?.widgetConfig.attributeRefs[0]}"></or-gauge>-->
        `
    }

    updated(changedProperties: Map<string, any>) {
        if(changedProperties.has("widget") || changedProperties.has("editMode")) {
            this.fetchAssets(this.widget?.widgetConfig).then((assets) => {
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
        if(config.attributeRefs && config.attributeRefs.length > 0) {
            let assets: Asset[] = [];
            await manager.rest.api.AssetResource.queryAssets({
                ids: config.attributeRefs?.map((x: AttributeRef) => x.id) as string[],
                select: {
                    attributes: config.attributeRefs?.map((x: AttributeRef) => x.name) as string[]
                }
            }).then(response => {
                assets = response.data;
            }).catch((reason) => {
                console.error(reason);
                showSnackbar(undefined, i18next.t('errorOccurred'));
            });
            return assets;
        }
    }
}



@customElement("or-gauge-widgetsettings")
export class OrGaugeWidgetSettings extends LitElement {

    @property()
    public readonly widget?: DashboardWidget;

    // Default values
    private expandedPanels: string[] = [i18next.t('attributes'), i18next.t('values'), i18next.t('thresholds')];
    private loadedAsset?: Asset;

    static get styles() {
        return [style, widgetSettingsStyling];
    }

    // UI Rendering
    render() {
        const config = JSON.parse(JSON.stringify(this.widget!.widgetConfig)) as GaugeWidgetConfig; // duplicate to edit, to prevent parent updates. Please trigger updateConfig()
        return html`
            <div>
                ${this.generateExpandableHeader(i18next.t('attributes'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('attributes')) ? html`
                    <or-dashboard-settingspanel .type="${SettingsPanelType.SINGLE_ATTRIBUTE}" .onlyDataAttrs="${false}" .widgetConfig="${this.widget?.widgetConfig}" 
                                                .attributeFilter="${(attribute: Attribute<any>) => {
                                                    return ["positiveInteger", "positiveNumber", "number", "long", "integer", "bigInteger", "negativeInteger", "negativeNumber", "bigNumber", "integerByte", "direction"]
                                                      .includes(attribute.type!)}}"
                                                @updated="${(event: CustomEvent) => {
                                                    this.updateConfig(this.widget!, event.detail.changes.get('config'));
                                                    this.onAttributesUpdate(event.detail.changes);
                                                }}"
                    ></or-dashboard-settingspanel>
                ` : null}
            </div>
            <div>
                ${this.generateExpandableHeader(i18next.t('values'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('values')) ? html`
                    <div class="expanded-panel">
                        <div style="display: flex; gap: 8px;">
                            <or-mwc-input type="${InputType.NUMBER}" label="${i18next.t('min')}" .max="${this.widget?.widgetConfig.max}" .value="${this.widget?.widgetConfig.min}"
                                          @or-mwc-input-changed="${(event: CustomEvent) => {
                                              if(event.detail.value < this.widget?.widgetConfig.max) {
                                                  config.min = event.detail.value;
                                                  if(config.thresholds[1][0] > event.detail.value) {
                                                      config.thresholds[0][0] = event.detail.value;
                                                  }
                                                  this.updateConfig(this.widget!, config);
                                              }
                                          }}"
                            ></or-mwc-input>
                            <or-mwc-input type="${InputType.NUMBER}" label="${i18next.t('max')}" .min="${this.widget?.widgetConfig.min}" .value="${this.widget?.widgetConfig.max}"
                                          @or-mwc-input-changed="${(event: CustomEvent) => {
                                              if(event.detail.value > this.widget?.widgetConfig.min) {
                                                  config.max = event.detail.value;
                                                  this.updateConfig(this.widget!, config);
                                              }
                                          }}"
                            ></or-mwc-input>
                        </div>
                        <div>
                            <or-mwc-input type="${InputType.NUMBER}" style="width: 100%;" .value="${config.decimals}" label="${i18next.t('decimals')}" .min="${0}"
                                          @or-mwc-input-changed="${(event: CustomEvent) => {
                                              config.decimals = event.detail.value;
                                              this.updateConfig(this.widget!, config);
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
                    <or-dashboard-settingspanel .type="${SettingsPanelType.THRESHOLDS}" .widgetConfig="${this.widget?.widgetConfig}"
                                                @updated="${(event: CustomEvent) => { this.updateConfig(this.widget!, event.detail.changes.get('config')); }}">
                    </or-dashboard-settingspanel>
                ` : null}
            </div>
        `
    }

    updateConfig(widget: DashboardWidget, config: OrWidgetConfig | any, force: boolean = false) {
        const oldWidget = JSON.parse(JSON.stringify(widget)) as DashboardWidget;
        widget.widgetConfig = config;
        this.requestUpdate("widget", oldWidget);
        this.forceParentUpdate(new Map<string, any>([["widget", widget]]), force);
    }


    /* ------------------------------ */

    onAttributesUpdate(changes: Map<string, any>) {
        if(changes.has('loadedAssets')) {
            this.loadedAsset = changes.get('loadedAssets')[0];
        }
        if(changes.has('config')) {
            const config = changes.get('config') as GaugeWidgetConfig;
            if(config.attributeRefs.length > 0) {
                this.widget!.displayName = this.loadedAsset?.name + " - " + this.loadedAsset?.attributes![config.attributeRefs[0].name!].name;
            }
        }
    }

    // Method to update the Grid. For example after changing a setting.
    forceParentUpdate(changes: Map<string, any>, force: boolean = false) {
        this.dispatchEvent(new CustomEvent('updated', {detail: {changes: changes, force: force}}));
    }

    generateExpandableHeader(name: string): TemplateResult {
        return html`
            <span class="expandableHeader" @click="${() => { this.expandPanel(name); }}">
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
