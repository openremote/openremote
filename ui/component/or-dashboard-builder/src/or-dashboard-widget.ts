import {DashboardWidget } from "@openremote/model";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { i18next } from "@openremote/or-translate";
import {css, html, LitElement, TemplateResult } from "lit";
import { customElement, property, query, state } from "lit/decorators.js";
import { when } from "lit/directives/when.js";
import {throttle} from "lodash";
import {style} from "./style";
import "@openremote/or-gauge";
import {widgetTypes} from "./index";

//language=css
const styling = css`
`

/* ------------------------------------ */

@customElement("or-dashboard-widget")
export class OrDashboardWidget extends LitElement {

    @property({ hasChanged(oldValue, newValue) { return JSON.stringify(oldValue) != JSON.stringify(newValue); }})
    protected readonly widget?: DashboardWidget;

    @property()
    protected readonly editMode?: boolean;

    @property()
    protected readonly realm?: string;

    @property()
    protected loading: boolean = false;

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

    shouldUpdate(changedProperties: Map<PropertyKey, unknown>): boolean {
        const changed = changedProperties;
        changed.delete('resizeObserver');
        return changed.size > 0;
    }

    updated(changedProperties: Map<string, any>) {
        console.error(changedProperties);
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
                    // this.requestUpdate();
                }, 200));
                this.resizeObserver.observe(gridItemElement);
            } else {
                console.error("gridItemElement could not be found!");
            }
        });
    }


    protected render() {
        console.warn("Rendering or-dashboard-widget [" + this.widget?.displayName + "]");
        console.error(this.widget);
        return html`
            <div id="widget-container" style="height: 100%; padding: 8px 16px 8px 16px; display: flex; flex-direction: column; overflow: auto;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-right: -12px; height: 36px;">
                    <span class="panel-title">${this.widget?.displayName?.toUpperCase()}</span>
                    <div>
                            <!--<or-mwc-input type="${InputType.BUTTON}" outlined label="Period"></or-mwc-input>-->
                            <!--<or-mwc-input type="${InputType.BUTTON}" label="Settings"></or-mwc-input>-->
                            <!--<or-mwc-input type="${InputType.BUTTON}" icon="refresh" @or-mwc-input-changed="${() => { this.requestUpdate(); }}"></or-mwc-input>-->
                    </div>
                </div>
                ${when((!this.error && !this.loading), () => html`
                    ${this.getWidgetContent(this.widget!)}
                `, () => html`
                    ${this.error ? html`${this.error}` : html`${i18next.t('loading')}`}
                `)}
            </div>
        `
    }

    getWidgetContent(widget: DashboardWidget): TemplateResult {
        const _widget = Object.assign({}, widget);
        if(_widget.gridItem) {

            const widgetEntity = widgetTypes.get(widget.widgetTypeId!);
            return widgetEntity!.getWidgetHTML(this.widget!, this.editMode!, this.realm!);

            /*switch (_widget.widgetType) {
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
                case DashboardWidgetType.GAUGE: {
                    const config: OrGaugeConfig = {
                        attributeRef: widget.widgetConfig.attributeRef
                    }
                    return html`
                        <div class='gridItem' id="gridItem-${widget.id}" style="display: flex;">
                            ${cache(this.error ? html`
                                <span>${this.error}</span>
                            ` : html`
                                <or-gauge .config="${this.widget?.widgetConfig}" style="width: 100%;"></or-gauge>
                            `)}
                        </div>
                    `;
                }
            }*/
        }
        return html`<span>${i18next.t('error')}!</span>`;
    }


    /*@state()
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
    }*/
}
