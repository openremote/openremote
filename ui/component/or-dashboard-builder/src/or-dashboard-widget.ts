import manager from "@openremote/core";
import {Asset, Attribute, AttributeRef, DashboardWidget, DashboardWidgetType } from "@openremote/model";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { i18next } from "@openremote/or-translate";
import {css, html, LitElement, TemplateResult } from "lit";
import { customElement, property, query, state } from "lit/decorators.js";
import { until } from "lit/directives/until.js";
import { cache } from "lit/directives/cache.js";
import {throttle} from "lodash";
import {style} from "./style";

//language=css
const styling = css`
    .gridItem {
        height: 100%;
        overflow: hidden;
        box-sizing: border-box;
    }
`

/* ------------------------------------ */

// Interfaces that contain Configurations of each Widget Type.
// The database stores them in any type, however these can be used
// for type checking.

export interface ChartWidgetConfig {
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
export interface KPIWidgetConfig {
    displayName: string;
    attributeRefs: AttributeRef[];
    period?: 'year' | 'month' | 'week' | 'day' | 'hour' | 'minute' | 'second';
}
export function generateWidgetConfig(widget: DashboardWidget): Object {
    console.error("Generating widget config..")
    switch (widget.widgetType) {
        case DashboardWidgetType.LINE_CHART: {
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
        case DashboardWidgetType.KPI: {
            return {
                displayName: widget.displayName,
                attributeRefs: [],
                period: "day"
            } as KPIWidgetConfig
        }
        default: {
            return {};
        }
    }
}

/* ------------------------------------ */

@customElement("or-dashboard-widget")
export class OrDashboardWidget extends LitElement {

    @property({ hasChanged(oldValue, newValue) { return JSON.stringify(oldValue) != JSON.stringify(newValue); }})
    protected readonly widget?: DashboardWidget;

    @property()
    protected readonly editMode?: boolean;

    @property()
    protected readonly realm?: string;

    @state()
    protected error?: string;

    @state()
    protected resizeObserver?: ResizeObserver;

    @query("#widget-container")
    protected widgetContainerElement?: Element;


    static get styles() {
        return [styling, style];
    }

    constructor() {
        super();
    }

    updated(changedProperties: Map<string, any>) {
        console.log(changedProperties);
    }
    firstUpdated(_changedProperties: Map<string, any>) {
        this.updateComplete.then(() => {
            const gridItemElement = this.widgetContainerElement;
            if(gridItemElement) {
                this.resizeObserver?.disconnect();
                this.resizeObserver = new ResizeObserver(throttle(() => {
                    const isMinimumSize = (this.widget?.gridItem?.minPixelW && this.widget.gridItem?.minPixelH &&
                        (this.widget.gridItem?.minPixelW < gridItemElement.clientWidth) && (this.widget.gridItem?.minPixelH < gridItemElement.clientHeight)
                    );
                    this.error = (isMinimumSize ? undefined : i18next.t('dashboard.widgetTooSmall'));
                    this.requestUpdate();
                }, 200));
                this.resizeObserver.observe(gridItemElement);
            } else {
                console.error("gridItemElement could not be found!");
            }
        });
    }


    protected render() {
        console.error("Rendering or-dashboard-widget [" + this.widget?.displayName + "]")
        return html`
            <div id="widget-container" style="height: 100%; padding: 8px 16px 8px 16px; display: flex; flex-direction: column;">
                <div style="display: flex; height: 36px; justify-content: space-between; align-items: center; margin-right: -12px;">
                    <span class="panel-title">${this.widget?.displayName?.toUpperCase()}</span>
                    <div>
                        <!--<or-mwc-input type="${InputType.BUTTON}" outlined label="Period"></or-mwc-input>-->
                        <!--<or-mwc-input type="${InputType.BUTTON}" label="Settings"></or-mwc-input>-->
                        <or-mwc-input type="${InputType.BUTTON}" icon="refresh" @or-mwc-input-changed="${() => { this.requestUpdate(); }}"></or-mwc-input>
                    </div>
                </div>
                ${until(this.getWidgetContent(this.widget!).then((content) => {
                    return content;
                }), html`${i18next.t('loading')}`)}
            </div>
        `
    }


    async getWidgetContent(widget: DashboardWidget): Promise<TemplateResult> {
        const _widget = Object.assign({}, widget);
        if(_widget.gridItem) {
            let assets: Asset[] = [];
            let attributes: [number, Attribute<any>][] = [];

            // Pulling data from database, however only when in editMode!!
            // KPI widgetType does use real data in EDIT mode as well, so separate check
            if(!this.editMode || _widget.widgetType == DashboardWidgetType.KPI) {
                const response = await manager.rest.api.AssetResource.queryAssets({
                    ids: widget.widgetConfig?.attributeRefs?.map((x: AttributeRef) => { return x.id; }) as string[]
                }).catch((reason => {
                    console.error(reason);
                    showSnackbar(undefined, i18next.t('errorOccurred'));
                }));
                if(response) {
                    assets = response.data;
                    attributes = widget.widgetConfig?.attributeRefs?.map((attrRef: AttributeRef) => {
                        const assetIndex = assets.findIndex((asset) => asset.id === attrRef.id);
                        const foundAsset = assetIndex >= 0 ? assets[assetIndex] : undefined;
                        return foundAsset && foundAsset.attributes ? [assetIndex, foundAsset.attributes[attrRef.name!]] : undefined;
                    }).filter((indexAndAttr: any) => !!indexAndAttr) as [number, Attribute<any>][];
                }
            }

            switch (_widget.widgetType) {
                case DashboardWidgetType.LINE_CHART: {

                    // Generation of fake data when in editMode.
                    if(this.editMode) {
                        _widget.widgetConfig?.attributeRefs?.forEach((attrRef: AttributeRef) => {
                            if(!assets.find((asset: Asset) => { return asset.id == attrRef.id; })) {
                                assets.push({ id: attrRef.id, name: (i18next.t('asset') +" X"), type: "ThingAsset" });
                            }
                        });
                        attributes = [];
                        _widget.widgetConfig?.attributeRefs?.forEach((attrRef: AttributeRef) => {
                            attributes.push([0, { name: attrRef.name }]);
                        });
                    }
                    return html`
                        <div class="gridItem" id="gridItem-${_widget.id}">
                            ${cache(this.error ? html`
                                <span>${this.error}</span>
                            ` : html`
                                <or-chart .assets="${assets}" .assetAttributes="${attributes}" .period="${widget.widgetConfig?.period}" denseLegend="${true}" 
                                          .dataProvider="${this.editMode ? (async (startOfPeriod: number, endOfPeriod: number, _timeUnits: any, _stepSize: number) => { return this.generateMockData(_widget, startOfPeriod, endOfPeriod, 20); }) : undefined}"
                                          showLegend="${(_widget.widgetConfig?.showLegend != null) ? _widget.widgetConfig?.showLegend : true}" .realm="${this.realm}" .showControls="${_widget.widgetConfig?.showTimestampControls}" style="height: 100%"
                                ></or-chart>                            
                            `)}
                        </div>
                    `;
                }

                case DashboardWidgetType.KPI: {
                    return html`
                        <div class='gridItem' id="gridItem-${widget.id}" style="display: flex;">
                            ${cache(this.error ? html`
                                <span>${this.error}</span>
                            ` : html`
                                <or-attribute-card .assets="${assets}" .assetAttributes="${attributes}" .period="${widget.widgetConfig?.period}" 
                                                   showControls="${false}" .realm="${this.realm}" style="height: 100%;"
                                ></or-attribute-card>
                            `)}
                        </div>
                    `;
                }
            }
        }
        return html`<span>${i18next.t('error')}!</span>`;
    }


    @state()
    protected cachedMockData?: Map<string, { period: any, data: any[] }> = new Map<string, { period: any, data: any[] }>();

    protected generateMockData(widget: DashboardWidget, startOfPeriod: number, _endOfPeriod: number, amount: number = 10): any {
        switch (widget.widgetType) {
            case DashboardWidgetType.LINE_CHART: {
                const mockTime: number = startOfPeriod;
                const chartData: any[] = [];
                const interval = (Date.now() - startOfPeriod) / amount;

                // Generating random coordinates on the chart
                let data: any[] = [];
                const cached: { period: any, data: any[] } | undefined = this.cachedMockData?.get(widget.id!);
                if(cached && (cached.data.length == widget.widgetConfig?.attributeRefs?.length) && (cached.period == widget.widgetConfig?.period)) {
                    data = this.cachedMockData?.get(widget.id!)!.data!;
                } else {
                    widget.widgetConfig?.attributeRefs?.forEach((_attrRef: AttributeRef) => {
                        let valueEntries: any[] = [];
                        let prevValue: number = 100;
                        for(let i = 0; i < amount; i++) {
                            const value = Math.floor(Math.random() * ((prevValue + 2) - (prevValue - 2)) + (prevValue - 2))
                            valueEntries.push({
                                x: (mockTime + (i * interval)),
                                y: value
                            });
                            prevValue = value;
                        }
                        data.push(valueEntries);
                    });
                    this.cachedMockData?.set(widget.id!, { period: widget.widgetConfig?.period, data: data });
                }

                // Making a line for each attribute
                widget.widgetConfig?.attributeRefs?.forEach((attrRef: AttributeRef) => {
                    chartData.push({
                        backgroundColor: ["#3869B1", "#DA7E30", "#3F9852", "#CC2428", "#6B4C9A", "#922427", "#958C3D", "#535055"][chartData.length],
                        borderColor: ["#3869B1", "#DA7E30", "#3F9852", "#CC2428", "#6B4C9A", "#922427", "#958C3D", "#535055"][chartData.length],
                        data: data[chartData.length],
                        fill: false,
                        label: attrRef.name,
                        pointRadius: 2
                    });
                });
                return chartData;
            }
            default: {
                console.error("No Widget type could be found when generating mock data..");
            }
        }
        return [];
    }
}
