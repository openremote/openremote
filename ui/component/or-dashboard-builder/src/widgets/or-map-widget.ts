import {
    Asset,
    Attribute,
    AttributeRef,
    DashboardWidget,
    WellknownAttributes,
    WellknownMetaItems,
    GeoJSONPoint,
    AssetDescriptor,
} from "@openremote/model";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate";
import {html, LitElement, TemplateResult} from "lit";
import {customElement, property, state, query} from "lit/decorators.js";
import {OrWidgetConfig, OrWidgetEntity} from "./or-base-widget";
import {SettingsPanelType, widgetSettingsStyling} from "../or-dashboard-settingspanel";
import {style} from "../style";
import manager, {Util} from "@openremote/core";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import "@openremote/or-map";
import {
    OrMap,
    MapMarkerAssetConfig,
    LngLatLike,
    AttributeMarkerColours,
    RangeAttributeMarkerColours,
    AttributeMarkerColoursRange,
    MapMarkerColours,
} from "@openremote/or-map";
import {LngLat} from "maplibre-gl";

export interface MapWidgetConfig extends OrWidgetConfig {
    // General values
    displayName: string,
    attributeRefs: AttributeRef[];
    // Map related values
    zoom?: number,
    center?: LngLatLike,
    lat?: number,
    lng?: number,
    // Map marker related values
    showLabels: boolean,
    showUnits: boolean,
    showGeoJson: boolean,
    boolColors: MapMarkerColours,
    textColors: [string, string][],
    // Threshold related values
    thresholds: [number, string][],
    min?: number,
    max?: number,
    // Asset type related values
    assetType?: string,
    valueType?: string,
    attributeName?: string,
    assetTypes: AssetDescriptor[],
    assetIds: string[],
    attributes: string[],
}


export class OrMapWidget implements OrWidgetEntity {

    // Properties
    readonly DISPLAY_NAME: string = "Map";
    readonly DISPLAY_MDI_ICON: string = "map"; // https://materialdesignicons.com;
    readonly MIN_COLUMN_WIDTH: number = 2;
    readonly MIN_PIXEL_WIDTH: number = 0;
    readonly MIN_PIXEL_HEIGHT: number = 0;

    getDefaultConfig(widget: DashboardWidget): MapWidgetConfig {
        return {
            displayName: widget?.displayName,
            attributeRefs: [],
            showLabels: false,
            showUnits: false,
            showGeoJson: true,
            boolColors: {type: 'boolean', 'false': '#ef5350', 'true': '#4caf50'},
            textColors: [['example', '4caf50'], ['example2', 'ef5350']],
            thresholds: [[0, "#4caf50"], [75, "#ff9800"], [90, "#ef5350"]],
            assetTypes: [],
            assetType: undefined,
            assetIds: [],
            attributes: [],
        } as MapWidgetConfig;
    }

    // Triggered every update to double check if the specification.
    // It will merge missing values, or you can add custom logic to process here.
    verifyConfigSpec(widget: DashboardWidget): MapWidgetConfig {
        return Util.mergeObjects(this.getDefaultConfig(widget), widget.widgetConfig, false) as MapWidgetConfig;
    }

    getWidgetHTML(widget: DashboardWidget, editMode: boolean, realm: string): TemplateResult {
        return html`
            <or-map-widget .widget="${widget}" .editMode="${editMode}" .realm="${realm}" style="overflow: hidden;"></or-map-widget>`;
    }

    getSettingsHTML(widget: DashboardWidget, realm: string): TemplateResult {
        return html`
            <or-map-widgetsettings .widget="${widget}" .realm="${realm}"></or-map-widgetsettings>`;
    }
}


@customElement('or-map-widget')
export class OrMapWidgetContent extends LitElement {

    @property()
    public readonly widget?: DashboardWidget;

    @property()
    public editMode?: boolean;

    @property()
    public realm?: string;

    @state()
    private assets?: Asset[] = [];

    @query("#miniMap")
    protected _map?: OrMap;

    private markers: MapMarkerAssetConfig = {};


    /* ---------- */

    render() {
        this.markers = {};
        return html`
            <div style="height: 100%; display: flex; flex-direction: column; overflow: hidden;">
                <or-map id="miniMap" class="or-map" .zoom="${this.widget?.widgetConfig?.zoom}"
                        .center="${this.widget?.widgetConfig?.center}" .showGeoJson="${this.widget?.widgetConfig?.showGeoJson}"
                        style="flex: 1;">
                    ${(this.assets) ?
                            this.assets.filter((asset: Asset) => {
                                if (!asset.attributes) {
                                    return false;
                                }
                                const attr = asset.attributes[WellknownAttributes.LOCATION] as Attribute<GeoJSONPoint>;
                                return !attr || !attr.meta || !attr.meta.hasOwnProperty(WellknownMetaItems.SHOWONDASHBOARD) || !!Util.getMetaValue(WellknownMetaItems.SHOWONDASHBOARD, attr);
                            }).map(asset => {
                                if (this.markers) {
                                    // Configure map marker asset settings
                                    this.markers[asset.type!] = {attributeName: this.widget!.widgetConfig.attributeName};
                                    this.markers[asset.type!].showUnits = this.widget!.widgetConfig.showUnits;
                                    this.markers[asset.type!].showLabel = this.widget!.widgetConfig.showLabels;
                                    if (this.widget!.widgetConfig.valueType == 'boolean') {
                                        this.widget!.widgetConfig.boolColors.true = this.widget!.widgetConfig.boolColors.true.replace("#", "");
                                        this.widget!.widgetConfig.boolColors.false = this.widget!.widgetConfig.boolColors.false.replace("#", "");
                                        this.markers[asset.type!].colours = this.widget!.widgetConfig.boolColors;
                                    } else if (this.widget!.widgetConfig.valueType == 'text') {
                                        var colors: AttributeMarkerColours = {type: 'string',};
                                        (this.widget!.widgetConfig.textColors as [string, string][]).map((threshold) => {
                                            colors[threshold[0] as string] = (threshold[1] as string).replace('#', '');
                                        })
                                        this.markers[asset.type!].colours = colors;
                                    } else {
                                        var ranges: AttributeMarkerColoursRange[] = [];
                                        (this.widget!.widgetConfig.thresholds as [number, string][]).sort((x, y) => (x[0] > y[0]) ? -1 : 1).map((threshold, index) => {
                                            var range: AttributeMarkerColoursRange = {
                                                min: threshold[0],
                                                colour: threshold[1].replace('#', '')
                                            }
                                            ranges.push(range);
                                        })
                                        var colorsNum: RangeAttributeMarkerColours = {
                                            type: 'range',
                                            ranges: ranges
                                        };
                                        this.markers[asset.type!].colours = colorsNum;
                                    }
                                }
                                return html`
                                <or-map-marker-asset .asset="${asset}"
                                                     .config="${this.markers}"></or-map-marker-asset>
                            `
                            }) : undefined}
                    }
                </or-map>
            </div>
        `
    }

    updated(changedProperties: Map<string, any>) {
        if (changedProperties.has("widget") || changedProperties.has("editMode")) {
            const areAssetsIncorrect: boolean = this.assets?.find((a) => !this.widget?.widgetConfig?.assetIds?.includes(a.id)) != undefined;
            if (this.assets == undefined || this.assets?.length === 0 || areAssetsIncorrect) {
                this.fetchAssets(this.widget?.widgetConfig).then((assets) => {
                    this.assets = assets!;
                });
            }
        }
    }

    // Fetching the assets according to the selected attribute and asset type
    async fetchAssets(config: OrWidgetConfig | any): Promise<Asset[] | undefined> {
        if (config.assetType && config.attributeName) {
            let assets: Asset[] = [];
            await manager.rest.api.AssetResource.queryAssets({
                realm: {
                    name: this.realm
                },
                select: {
                    attributes: [config.attributeName, 'location']
                },
                types: [config.assetType],

            }).then(response => {
                assets = response.data;
                this.markers = {};
            }).catch((reason) => {
                console.error(reason);
                showSnackbar(undefined, i18next.t('errorOccurred'));
            });
            return assets;
        }
    }
}

@customElement("or-map-widgetsettings")
class OrMapWidgetSettings extends LitElement {

    @property()
    public widget?: DashboardWidget;

    @state()
    private assetTypes: string[] = [];

    // Default values
    private expandedPanels: string[] = [i18next.t('configuration.mapSettings'), i18next.t('attributes'), i18next.t('thresholds')];


    static get styles() {
        return [style, widgetSettingsStyling];
    }

    // UI Rendering
    render() {
        const config = JSON.parse(JSON.stringify(this.widget!.widgetConfig)) as MapWidgetConfig; // duplicate to edit, to prevent parent updates. Please trigger updateConfig()
        return html`
            <div>
                ${this.generateExpandableHeader(i18next.t('configuration.mapSettings'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('configuration.mapSettings')) ? html`
                    <div class="expanded-panel">
                        <div>
                            <or-mwc-input .type="${InputType.NUMBER}" style="width: 100%;"
                                          .value="${this.widget?.widgetConfig?.zoom}" label="${i18next.t('dashboard.zoom')}"
                                          @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                              config.zoom = event.detail.value;
                                              this.updateConfig(this.widget!, config);
                                          }}"
                            ></or-mwc-input>
                        </div>
                        <div style="display: flex; gap: 8px;">
                            <or-mwc-input .type="${InputType.TEXT}" style="width: 100%;"
                                          .value="${this.widget?.widgetConfig?.center ? (Object.values(this.widget.widgetConfig.center))[0] + ', ' + (Object.values(this.widget.widgetConfig.center))[1] : undefined}"
                                          label="${i18next.t('dashboard.center')}"
                                          @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                              if (event.detail.value) {
                                                  const lngLatArr = (event.detail.value as string).split(/[, ]/).filter(v => !!v);
                                                  if (lngLatArr.length === 2) {
                                                      var value = new LngLat(
                                                              Number.parseFloat(lngLatArr[0]),
                                                              Number.parseFloat(lngLatArr[1])
                                                      );
                                                      config.center = value as LngLatLike;
                                                      this.updateConfig(this.widget!, config);
                                                  }
                                              }
                                          }}"
                            ></or-mwc-input>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <span>${i18next.t('dashboard.showGeoJson')}</span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="width: 70px;"
                                          .value="${config.showGeoJson}"
                                          @or-mwc-input-changed="${(event: CustomEvent) => {
                                              config.showGeoJson = event.detail.value;
                                              this.updateConfig(this.widget!, config, true);
                                          }}"
                            ></or-mwc-input>
                        </div>
                    </div>
                ` : null}
            </div>
            <div>
                ${this.generateExpandableHeader(i18next.t('attributes'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('attributes')) ? html`
                    <or-dashboard-settingspanel .type="${SettingsPanelType.ASSETTYPES}"
                                                .widgetConfig="${this.widget?.widgetConfig}"
                                                @updated="${(event: CustomEvent) => {
                                                    this.updateConfig(this.widget!, event.detail.changes.get('config'));
                                                }}">
                    </or-dashboard-settingspanel>
                ` : null}
            </div>
            ${this.widget?.widgetConfig?.assetIds?.length > 0 ? html`
                <div>
                    ${this.generateExpandableHeader(i18next.t('thresholds'))}
                </div>
                <div>
                    ${this.expandedPanels.includes(i18next.t('thresholds')) ? html`
                        <or-dashboard-settingspanel .type="${SettingsPanelType.THRESHOLDS}"
                                                    .widgetConfig="${this.widget?.widgetConfig}"
                                                    @updated="${(event: CustomEvent) => {
                                                        this.updateConfig(this.widget!, event.detail.changes.get('config'));
                                                    }}"
                        ></or-dashboard-settingspanel>
                    ` : null}
                </div>
            ` : null}
        `
    }

    updated(changedProperties: Map<string, any>){
        if(!this.widget?.widgetConfig?.zoom && !this.widget?.widgetConfig?.center){
            manager.rest.api.MapResource.getSettings().then((response) => {
                this.widget!.widgetConfig.zoom = response.data.options.default.zoom;
                this.widget!.widgetConfig.center = response.data.options.default.center;
                this.updateConfig(this.widget!, this.widget!.widgetConfig);
            })
        }
    }

    updateConfig(widget: DashboardWidget, config: OrWidgetConfig | any, force: boolean = false) {
        const oldWidget = JSON.parse(JSON.stringify(widget)) as DashboardWidget;
        widget.widgetConfig = config;
        this.requestUpdate("widget", oldWidget);
        this.forceParentUpdate(new Map<string, any>([["widget", widget]]), force);
    }

    // Method to update the Grid. For example after changing a setting.
    public forceParentUpdate(changes: Map<string, any>, force: boolean = false) {
        this.dispatchEvent(new CustomEvent('updated', {detail: {changes: changes, force: force}}));
    }

    generateExpandableHeader(name: string): TemplateResult {
        return html`
            <span class="expandableHeader" @click="${() => {
                this.expandPanel(name);
            }}">
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
