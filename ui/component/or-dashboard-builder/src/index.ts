import {html, css, LitElement, unsafeCSS } from "lit";
import { customElement, state } from "lit/decorators.js";
import "./or-dashboard-tree";
import "./or-dashboard-browser";
import "./or-dashboard-editor";
import "./or-dashboard-widgetsettings";
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import "@openremote/or-icon";
import {style} from "./style";
import { MDCTabBar } from "@material/tab-bar";
import {AddOutput, ORGridStackNode, SelectOutput} from "./or-dashboard-editor";
import {Dashboard, DashboardGridItem, DashboardScalingPreset, DashboardTemplate, DashboardWidget, DashboardWidgetType } from "@openremote/model";

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const tabStyle = require("@material/tab/dist/mdc.tab.css");
const tabbarStyle = require("@material/tab-bar/dist/mdc.tab-bar.css");
const tabIndicatorStyle = require("@material/tab-indicator/dist/mdc.tab-indicator.css");
const tabScrollerStyle = require("@material/tab-scroller/dist/mdc.tab-scroller.css");

// language=CSS
const styling = css`
    
    /* Header related styling */
    #header {
        display: table-row;
        height: 0.1%;
        background: white;
    }
    #header-wrapper {
        padding: 20px 20px 14px 20px;
        display: flex;
        flex-direction: row;
    }
    #header-title {
        font-size: 18px;
    }
    #header-title > or-icon {
        margin-right: 10px;
    }
    #header-actions {
        flex: 1 1 auto;
        text-align: right;
    }
    #header-actions-content {
        display: flex;
        flex-direction: row;
        align-items: center;
        float: right;
    }
    
    /* ----------------------------- */
    /* Editor/builder related styling */
    #builder {
        flex-grow: 2;
        align-items: stretch;
        z-index: 0;
        padding: 3vh 4vw 3vh 4vw;
    }
    
    /* ----------------------------- */
    /* Sidebar related styling (drag and drop widgets / configuration) */
    #sidebar {
        display: flex;
        flex-direction: column;
        width: 300px;
        background: white;
    }
    #browser {
        flex-grow: 1;
        align-items: stretch;
        z-index: 1;
        max-width: 300px;
    }
    
    #save-btn { margin-left: 15px; }
    #view-btn { margin-left: 15px; }

    
    /* Material Design Tab Bar overrides (for now just placed them here) */
    .mdc-tab--active .mdc-tab__text-label {
        color: white !important;
    }
    .mdc-tab .mdc-tab__text-label {
        color: rgba(255, 255, 255, 0.74);
    }
    .mdc-tab-indicator .mdc-tab-indicator__content--underline {
        border-color: white;
    }
`;

@customElement("or-dashboard-builder")
export class OrDashboardBuilder extends LitElement {

    // Importing Styles; the unsafe GridStack css, and all custom css
    static get styles() {
        return [unsafeCSS(tabStyle), unsafeCSS(tabbarStyle), unsafeCSS(tabIndicatorStyle), unsafeCSS(tabScrollerStyle), styling, style]
    }

    @state()
    protected sidebarMenuIndex: number;

    @state()
    protected dashboard: Dashboard;

    @state()
    protected selectedWidget: DashboardWidget | undefined;


    // Main constructor; after the component is rendered/updated, we start rendering the grid.
    constructor() {
        super();
        this.sidebarMenuIndex = 0;
        this.dashboard = this.generateInitialDashboard();

        this.updateComplete.then(() => {
            if(this.shadowRoot != null) {

                // Setting up tabs (widgets/settings) in sidebar.
                const tabBar = this.shadowRoot.getElementById("tab-bar");
                if (tabBar != null) {
                    const mdcTabBar = new MDCTabBar(tabBar);
                    mdcTabBar.activateTab(this.sidebarMenuIndex); // Activate initial tab
                    mdcTabBar.listen("MDCTabBar:activated", (event: CustomEvent) => {
                        this.sidebarMenuIndex = event.detail.index; // 0 = Widget Browser, and 1 = Settings menu.
                    })
                }
            }
        });
    }

    /* ----------------- */

    // Method for creating Widgets (reused at many places)
    createWidget(gridStackNode: ORGridStackNode): DashboardWidget {
        const randomId = (Math.random() + 1).toString(36).substring(2);
        let displayName = this.generateWidgetDisplayName(gridStackNode.widgetType);
        if(displayName == undefined) { displayName = "Widget #" + randomId; } // If no displayName, set random ID as name.
        const gridItem: DashboardGridItem = this.generateGridItem(gridStackNode, displayName);

        const widget = {
            id: randomId,
            displayName: displayName,
            gridItem: gridItem,
            widgetType: gridStackNode.widgetType
        } as DashboardWidget;
        return widget;
    }

    // Generating the Grid Item details like X and Y coordinates for GridStack to work.
    generateGridItem(gridstackNode: ORGridStackNode, displayName: string): DashboardGridItem {
        const randomId = (Math.random() + 1).toString(36).substring(2);
        return {
            id: randomId,
            x: gridstackNode.x,
            y: gridstackNode.y,
            w: 2,
            h: 2,
            minW: this.getWidgetMinWidth(gridstackNode.widgetType),
            minH: this.getWidgetMinWidth(gridstackNode.widgetType),
            noResize: false,
            noMove: false,
            locked: false,
            content: this.getWidgetContent(gridstackNode.widgetType, displayName)
        }
    }
    generateWidgetDisplayName(widgetType: DashboardWidgetType): string | undefined {
        if(this.dashboard != null && this.dashboard.template != null && this.dashboard.template.widgets != null) {
            const filteredWidgets: DashboardWidget[] = this.dashboard.template.widgets.filter((x) => { return x.widgetType == widgetType; });
            switch (widgetType) {
                case DashboardWidgetType.MAP: return "Map #" + (filteredWidgets.length + 1);
                case DashboardWidgetType.CHART: return "Chart #" + (filteredWidgets.length + 1);
            }
        }
        return undefined;
    }
    getWidgetMinWidth(widgetType: DashboardWidgetType): number {
        switch (widgetType) {
            case DashboardWidgetType.CHART: return 2;
            case DashboardWidgetType.MAP: return 4;
        }
    }
    getWidgetContent(widgetType: DashboardWidgetType, displayName: string): any {
        switch (widgetType) {
            case DashboardWidgetType.CHART: {
                return ('<div class="gridItem"><or-chart></or-chart></div>');
            } case DashboardWidgetType.MAP: {
                return ('<div class="gridItem"><or-map center="5.454250, 51.445990" zoom="5" style="height: 500px; width: 100%;"></or-map></div>');
            }
        }
    }

    /* ----------------- */

    selectWidget(event: SelectOutput): void {
        const foundWidget = this.dashboard.template?.widgets?.find((x) => { return x.gridItem?.id == event.gridItem?.id; });
        if(foundWidget != null) {
            console.log("Selected a new Widget! [" + foundWidget.displayName + "]");
            this.selectedWidget = foundWidget;
        }
    }
    deselectCurrentWidget() {
        console.log("Deselecting a widget..");
        this.selectedWidget = undefined;
    }
    deleteCurrentWidget() {
        if(this.selectedWidget != null) {
            const index = this.dashboard.template?.widgets?.indexOf(this.selectedWidget);
            if(index != null) {
                this.dashboard.template?.widgets?.splice(index, 1);
                console.log("Removed the selected Widget from the dashboard! Current dashboard:");
                console.log(this.dashboard);
                this.requestUpdate();
            }
        }
    }

    /* ----------------- */

    // Rendering the page
    render(): any {
        return html`
            <div id="container" style="display: table;">
                <div id="header">
                    <div id="header-wrapper">
                        <div id="header-title">
                            <!--<or-icon icon="view-dashboard"></or-icon>-->
                            <or-mwc-input type="${InputType.TEXT}" min="1" max="1023" comfortable required outlined label="Name" value="Dashboard 1" style="min-width: 320px;"></or-mwc-input>
                        </div>
                        <div id="header-actions">
                            <div id="header-actions-content">
                                <or-mwc-input id="share-btn" type="${InputType.BUTTON}" icon="share-variant"></or-mwc-input>
                                <or-mwc-input id="save-btn" type="${InputType.BUTTON}" raised label="Save"></or-mwc-input>
                                <or-mwc-input id="view-btn" type="${InputType.BUTTON}" outlined icon="eye" label="View"></or-mwc-input>
                            </div>
                        </div>
                    </div>
                </div>
                <div id="content">
                    <div id="container">
                        <div id="builder">
                            <or-dashboard-editor style="background: transparent;" .widgets="${this.dashboard.template?.widgets}" .selected="${this.selectedWidget}"
                                                 @select="${(event: CustomEvent) => { this.selectWidget(event.detail as SelectOutput); }}"
                                                 @deselect="${(event: CustomEvent) => { this.deselectCurrentWidget(); }}"
                                                 @add="${(event: CustomEvent) => { this.dashboard.template?.widgets?.push(this.createWidget((event.detail as AddOutput).gridStackNode)); this.requestUpdate(); }}"
                            ></or-dashboard-editor>
                        </div>
                        <div id="sidebar">
                            <div style="${this.selectedWidget == null ? css`display: none` : null}">
                                <div id="menu-header">
                                    <div id="title-container">
                                        <span id="title">${this.selectedWidget?.displayName}:</span>
                                    </div>
                                    <div>
                                        <or-mwc-input type="${InputType.BUTTON}" icon="close" style="" @click="${(event: any) => { this.deselectCurrentWidget(); }}"></or-mwc-input>
                                    </div>
                                </div>
                                <div id="content" style="display: block;">
                                    <or-dashboard-widgetsettings .selectedWidget="${this.selectedWidget}" @delete="${() => { this.deleteCurrentWidget(); }}"></or-dashboard-widgetsettings>
                                </div>
                            </div>
                            <div style="${this.selectedWidget != null ? css`display: none` : null}">
                                <div id="menu-header">
                                    <div class="mdc-tab-bar" role="tablist" id="tab-bar">
                                        <div class="mdc-tab-scroller">
                                            <div class="mdc-tab-scroller__scroll-area">
                                                <div class="mdc-tab-scroller__scroll-content">
                                                    <button class="mdc-tab" role="tab" aria-selected="false" tabindex="0">
                                                    <span class="mdc-tab__content">
                                                        <span class="mdc-tab__text-label">WIDGETS</span>
                                                    </span>
                                                        <span class="mdc-tab-indicator">
                                                        <span class="mdc-tab-indicator__content mdc-tab-indicator__content--underline"></span>
                                                    </span>
                                                        <span class="mdc-tab__ripple"></span>
                                                    </button>
                                                    <button class="mdc-tab" role="tab" aria-selected="false" tabindex="1">
                                                    <span class="mdc-tab__content">
                                                        <span class="mdc-tab__text-label">SETTINGS</span>
                                                    </span>
                                                        <span class="mdc-tab-indicator">
                                                        <span class="mdc-tab-indicator__content mdc-tab-indicator__content--underline"></span>
                                                    </span>
                                                        <span class="mdc-tab__ripple"></span>
                                                    </button>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div id="content" style="border: 1px solid #E0E0E0; height: 100%; display: contents;">
                                    <or-dashboard-browser id="browser" style="${this.sidebarMenuIndex != 0 ? css`display: none` : null}"></or-dashboard-browser>
                                    <div id="item" style="${this.sidebarMenuIndex != 1 ? css`display: none` : null}"> <!-- Setting display to none instead of not rendering it. -->
                                        <span>Settings to display here.</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `
    }

    /* ----------------- */


    generateInitialDashboard(): Dashboard {
        let dashboard: Dashboard = {
            id: ((Math.random() + 1).toString(36).substring(2)),
            createdOn: Date.now(),
            realm: 'unknown',
            version: 1,
            template: {
                id: ((Math.random() + 1).toString(36).substring(2)),
                columns: 12, // later on changed based on preset
                screenPresets: [
                    {
                        id: ((Math.random() + 1).toString(36).substring(2)),
                        displayName: "Small",
                        breakpoint: 480,
                        scalingPreset: DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN
                    },
                    {
                        id: ((Math.random() + 1).toString(36).substring(2)),
                        displayName: "Medium",
                        breakpoint: 1024,
                        scalingPreset: DashboardScalingPreset.RESIZE_WIDGETS
                    },
                    {
                        id: ((Math.random() + 1).toString(36).substring(2)),
                        displayName: "Large",
                        breakpoint: 1080,
                        scalingPreset: DashboardScalingPreset.RESIZE_WIDGETS
                    }
                ],
                widgets: []
            }
        };

        const widgets: DashboardWidget[] = [];
        widgets.push(this.createWidget({id: 'bla1', x: 0, y: 0, widgetType: DashboardWidgetType.CHART }));
        widgets.push(this.createWidget({id: 'bla2', x: 2, y: 1, widgetType: DashboardWidgetType.CHART }));
        widgets.push(this.createWidget({id: 'bla3', x: 6, y: 2, widgetType: DashboardWidgetType.CHART }));
        widgets.push(this.createWidget({id: 'bla4', x: 6, y: 4, widgetType: DashboardWidgetType.CHART }));
        if(dashboard.template != null) {
            dashboard.template.widgets = widgets;
        }
        console.log("Initiated the following dashboard:");
        console.log(dashboard);
        return dashboard;
    }
}
