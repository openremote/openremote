var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var OrDataViewer_1;
import { html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import "@openremote/or-chart";
import "@openremote/or-translate";
import { translate } from "@openremote/or-translate";
import "@openremote/or-components/or-panel";
import { OrChartEvent } from "@openremote/or-chart";
import { style } from "./style";
import i18next from "i18next";
import { styleMap } from "lit/directives/style-map.js";
import { classMap } from "lit/directives/class-map.js";
import "@openremote/or-attribute-card";
import manager from "@openremote/core";
export class OrDataViewerRenderCompleteEvent extends CustomEvent {
    constructor() {
        super(OrDataViewerRenderCompleteEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}
OrDataViewerRenderCompleteEvent.NAME = "or-data-viewer-render-complete-event";
export class OrDataViewerConfigInvalidEvent extends CustomEvent {
    constructor() {
        super(OrDataViewerConfigInvalidEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}
OrDataViewerConfigInvalidEvent.NAME = "or-data-viewer-config-invalid-event";
let OrDataViewer = OrDataViewer_1 = class OrDataViewer extends translate(i18next)(LitElement) {
    static get styles() {
        return [
            style
        ];
    }
    static generateGrid(shadowRoot) {
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
                            item.style.gridRowEnd = "span " + rowSpan;
                        }
                    });
                }
            }
        }
    }
    constructor() {
        super();
        this._loading = false;
        this._resizeHandler = () => {
            OrDataViewer_1.generateGrid(this.shadowRoot);
        };
        this.addEventListener(OrChartEvent.NAME, () => OrDataViewer_1.generateGrid(this.shadowRoot));
    }
    connectedCallback() {
        super.connectedCallback();
        window.addEventListener("resize", this._resizeHandler);
    }
    disconnectedCallback() {
        super.disconnectedCallback();
        window.removeEventListener("resize", this._resizeHandler);
    }
    refresh() {
        this.realm = manager.displayRealm;
    }
    getPanel(name, panelConfig) {
        const content = this.getPanelContent(name, panelConfig);
        if (!content) {
            return html ``;
        }
        return html `
            <div class=${classMap({ panel: true, mobileHidden: panelConfig.hideOnMobile === true })} id="${name}-panel" style="${panelConfig && panelConfig.panelStyles ? styleMap(panelConfig.panelStyles) : ""}">
                <div class="panel-content-wrapper">
                    ${(panelConfig && panelConfig.type === "chart") ? html `
                        <div class="panel-title">
                            <or-translate value="${name}"></or-translate>
                        </div>
                    ` : ``}
                   
                    <div class="panel-content">
                        ${content}
                    </div>
                </div>
            </div>
        `;
    }
    getPanelContent(panelName, panelConfig) {
        if (panelConfig.hide || !this.config) {
            return;
        }
        this.realm = manager.displayRealm;
        let content;
        if (panelConfig && panelConfig.type === "chart") {
            content = html `<or-chart id="chart" panelName="${panelName}" .config="${this.config.chartConfig}" .realm="${this.realm}"></or-chart>`;
        }
        if (panelConfig && panelConfig.type === "kpi") {
            content = html `
                <or-attribute-card panelName="${panelName}" .config="${this.config.chartConfig}" .realm="${this.realm}"></or-attribute-card>
            `;
        }
        return content;
    }
    render() {
        if (this._loading) {
            return html `
                <div class="msg"><or-translate value="loading"></or-translate></div>
            `;
        }
        if (!this.config) {
            this.config = Object.assign({}, OrDataViewer_1.DEFAULT_CONFIG);
        }
        return html `
            <div id="wrapper">
                <div id="container" style="${this.config.viewerStyles ? styleMap(this.config.viewerStyles) : ""}">
                    ${this.renderConfig()}
                </div>
            </div>
        `;
    }
    renderConfig() {
        const hasConfig = !!this.config;
        let config = hasConfig ? this.config : OrDataViewer_1.DEFAULT_CONFIG;
        try {
            return Object.entries(config.panels).map(([name, panelConfig]) => this.getPanel(name, panelConfig));
        }
        catch (e) {
            console.warn("OR data viewer config is invalid");
            this.config = undefined;
            this.dispatchEvent(new OrDataViewerConfigInvalidEvent());
            config = OrDataViewer_1.DEFAULT_CONFIG;
            return Object.entries(config.panels).map(([name, panelConfig]) => this.getPanel(name, panelConfig));
        }
    }
    updated(_changedProperties) {
        super.updated(_changedProperties);
        if (_changedProperties.has("realm")) {
            this.refresh();
        }
        this.updateComplete.then(() => {
            this.dispatchEvent(new OrDataViewerRenderCompleteEvent());
            OrDataViewer_1.generateGrid(this.shadowRoot);
        });
    }
};
OrDataViewer.DEFAULT_PANEL_TYPE = "chart";
OrDataViewer.DEFAULT_CONFIG = {
    viewerStyles: {},
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
__decorate([
    property()
], OrDataViewer.prototype, "config", void 0);
__decorate([
    property({ type: Array, attribute: false })
], OrDataViewer.prototype, "_assets", void 0);
__decorate([
    property()
], OrDataViewer.prototype, "_loading", void 0);
__decorate([
    property()
], OrDataViewer.prototype, "realm", void 0);
OrDataViewer = OrDataViewer_1 = __decorate([
    customElement("or-data-viewer")
], OrDataViewer);
export { OrDataViewer };
//# sourceMappingURL=index.js.map