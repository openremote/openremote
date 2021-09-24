import {html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import "@openremote/or-chart";
import "@openremote/or-translate";
import {translate} from "@openremote/or-translate";
import "@openremote/or-components/or-panel";
import {OrChartConfig, OrChartEvent} from "@openremote/or-chart";
import {Asset, Attribute} from "@openremote/model";
import {style} from "./style";
import i18next from "i18next";
import {styleMap} from "lit/directives/style-map";
import {classMap} from "lit/directives/class-map";
import "@openremote/or-attribute-card";

export type PanelType = "chart" | "kpi";

export interface DefaultAssets {
    assetId?: string;
    attributes?: string[];
}

export interface PanelConfig {
    type?: PanelType;
    hide?: boolean;
    hideOnMobile?: boolean;
    defaults?: DefaultAssets[];
    include?: string[];
    exclude?: string[];
    readonly?: string[];
    panelStyles?: { [style: string]: string };
    fieldStyles?: { [field: string]: { [style: string]: string } };
}

export interface DataViewerConfig {
    panels: {[name: string]: PanelConfig};
    viewerStyles?: { [style: string]: string };
    propertyViewProvider?: (property: string, value: any, viewerConfig: DataViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    attributeViewProvider?: (attribute: Attribute<any>, viewerConfig: DataViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    panelViewProvider?: (attributes: Attribute<any>[], panelName: string, viewerConfig: DataViewerConfig, panelConfig: PanelConfig) => TemplateResult | undefined;
    chartConfig?: OrChartConfig;
}

class EventHandler {
    public _callbacks: Function[];

    constructor() {
        this._callbacks = [];
    }

    public startCallbacks() {
        return new Promise<void>((resolve, reject) => {
            if (this._callbacks && this._callbacks.length > 0) {
                this._callbacks.forEach((cb) => cb());
            }
            resolve();
        });

    }

    public addCallback(callback: Function) {
        this._callbacks.push(callback);
    }
}
const onRenderComplete = new EventHandler();

@customElement("or-data-viewer")
export class OrDataViewer extends translate(i18next)(LitElement) {

    static get styles() {
        return [
            style
        ];
    }

    public static DEFAULT_PANEL_TYPE: PanelType = "chart";

    public static DEFAULT_CONFIG: DataViewerConfig = {
        viewerStyles: {

        },
        panels: {
            chart: {
                type: "chart",
                hideOnMobile: true,
                panelStyles: {
                    gridColumn: "1 / -1"
                }
            },
            kpi1: {
                type: "kpi",
                hideOnMobile: false
            },
            kpi2: {
                type: "kpi",
                hideOnMobile: false
            },
            kpi3: {
                type: "kpi",
                hideOnMobile: false
            },
            kpi4: {
                type: "kpi",
                hideOnMobile: false
            },
            kpi5: {
                type: "kpi",
                hideOnMobile: false
            },
            kpi6: {
                type: "kpi",
                hideOnMobile: false
            },
            kpi7: {
                type: "kpi",
                hideOnMobile: false
            },
            kpi8: {
                type: "kpi",
                hideOnMobile: false
            },
            chart2: {
                type: "chart",
                hideOnMobile: true,
                panelStyles: {
                    gridColumn: "1 / -1"
                }
            },
            chart3: {
                type: "chart",
                hideOnMobile: true,
                panelStyles: {
                    gridColumn: "1 / -1"
                }
            }
        }
    };

    public static generateGrid(shadowRoot: ShadowRoot | null) {
        if (shadowRoot) {
            const grid = shadowRoot.querySelector("#container");
            if (grid) {
                const rowHeight = parseInt(window.getComputedStyle(grid).getPropertyValue("grid-auto-rows"), 10);
                const rowGap = parseInt(window.getComputedStyle(grid).getPropertyValue("grid-row-gap"), 10);
                const items = shadowRoot.querySelectorAll(".panel");
                if (items) {
                    items.forEach((item) => {
                        const content = item.querySelector(".panel-content-wrapper");
                        if (content) {
                            const rowSpan = Math.ceil((content.getBoundingClientRect().height + rowGap) / (rowHeight + rowGap));
                            (item as HTMLElement).style.gridRowEnd = "span " + rowSpan;
                        }
                    });
                }
            }
        }
    }

    public config?: DataViewerConfig;

    @property({type: Array, attribute: false})
    protected _assets?: Asset[];

    @property()
    protected _loading: boolean = false;

    @property()
    public realm?: string;

    protected _resizeHandler = () => {
        OrDataViewer.generateGrid(this.shadowRoot)
    };

    constructor() {
        super();
        this.addEventListener(OrChartEvent.NAME, () => OrDataViewer.generateGrid(this.shadowRoot));
    }

    connectedCallback() {
        super.connectedCallback();
        window.addEventListener("resize", this._resizeHandler);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        window.removeEventListener("resize", this._resizeHandler);
    }

    public async onCompleted() {
        await this.updateComplete;
    }

    public getPanel(name: string, panelConfig: PanelConfig) {
        const content = this.getPanelContent(name, panelConfig);

        if (!content) {
            return;
        }

        return html`
            <pre>${this.realm}</pre>
            <div class=${classMap({panel: true, mobileHidden: panelConfig.hideOnMobile === true})} id="${name}-panel" style="${panelConfig && panelConfig.panelStyles ? styleMap(panelConfig.panelStyles) : ""}">
                <div class="panel-content-wrapper">
                    ${(panelConfig && panelConfig.type === "chart") ? html`
                        <div class="panel-title">
                            <or-translate value="${name}"></or-translate>
                        </div>
                    ` :  ``}
                   
                    <div class="panel-content">
                        ${content}
                    </div>
                </div>
            </div>
        `;
    }

    public getPanelContent(panelName: string,  panelConfig: PanelConfig): TemplateResult | undefined {
        if (panelConfig.hide || !this.config) {
            return;
        }

        let content: TemplateResult | undefined;

        if (panelConfig && panelConfig.type === "chart") {
            content = html`<or-chart id="chart" panelName="${panelName}" .config="${this.config.chartConfig}"></or-chart>`;
        }

        if (panelConfig && panelConfig.type === "kpi") {
            content = html`
                <or-attribute-card panelName="${panelName}" .config="${this.config.chartConfig}"></or-attribute-card>
            `;
        }
        return content;
    }

    protected render() {

        if (this._loading) {
            return html`
                <div class="msg"><or-translate value="loading"></or-translate></div>
            `;
        }

        if (!this.config) {
            this.config = {...OrDataViewer.DEFAULT_CONFIG};
        }

        return html`
            <div id="wrapper">
                <div id="container" style="${this.config.viewerStyles ? styleMap(this.config.viewerStyles) : ""}">
                    ${Object.entries(this.config.panels).map(([name, panelConfig]) => this.getPanel(name,  panelConfig))}
                </div>
            </div>
        `;
    }

    protected updated(_changedProperties: PropertyValues) {
        super.updated(_changedProperties);

        this.onCompleted().then(() => {
            onRenderComplete.startCallbacks().then(() => {
                OrDataViewer.generateGrid(this.shadowRoot);
            });
        });
    }
}
