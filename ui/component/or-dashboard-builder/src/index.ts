import {css, html, LitElement, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import "./or-dashboard-tree";
import "./or-dashboard-browser";
import "./or-dashboard-editor";
import "./or-dashboard-widgetsettings";
import {InputType, OrInputChangedEvent } from '@openremote/or-mwc-components/or-mwc-input';
import "@openremote/or-icon";
import {style} from "./style";
import {MDCTabBar} from "@material/tab-bar";
import {ORGridStackNode} from "./or-dashboard-editor";
import {Dashboard, DashboardGridItem, DashboardTemplate, DashboardWidget, DashboardWidgetType} from "@openremote/model";
import manager, {DefaultColor3 } from "@openremote/core";
import { getContentWithMenuTemplate } from "@openremote/or-mwc-components/or-mwc-menu";
import { ListItem } from "@openremote/or-mwc-components/or-mwc-list";

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const tabStyle = require("@material/tab/dist/mdc.tab.css");
const tabbarStyle = require("@material/tab-bar/dist/mdc.tab-bar.css");
const tabIndicatorStyle = require("@material/tab-indicator/dist/mdc.tab-indicator.css");
const tabScrollerStyle = require("@material/tab-scroller/dist/mdc.tab-scroller.css");

// language=CSS
const styling = css`

    #tree {
        flex-grow: 1;
        align-items: stretch;
        z-index: 1;
        max-width: 300px;
        box-shadow: rgb(0 0 0 / 21%) 0px 1px 3px 0px;
    }
    
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
        align-items: center;
        border-bottom: 1px solid #E0E0E0;
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

    /* Header related styling */
    #fullscreen-header {
        display: table-row;
        height: 0.1%;
    }
    #fullscreen-header-wrapper {
        padding: 17.5px 20px;
        display: flex;
        flex-direction: row;
        align-items: center;
    }
    #fullscreen-header-title {
        font-size: 18px;
        font-weight: bold;
    }
    #fullscreen-header-title > or-mwc-input {
        margin-right: 4px;
        --or-icon-fill: ${unsafeCSS(DefaultColor3)};
    }
    #fullscreen-header-actions {
        flex: 1 1 auto;
        text-align: right;
    }
    #fullscreen-header-actions-content {
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
        /*padding: 3vh 4vw 3vh 4vw;*/
    }
    
    /* ----------------------------- */
    /* Sidebar related styling (drag and drop widgets / configuration) */
    #sidebar {
        display: flex;
        flex-direction: column;
        width: 300px;
        background: white;
        border-left: 1px solid #E0E0E0;
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

export interface DashboardBuilderConfig {

}

@customElement("or-dashboard-builder")
export class OrDashboardBuilder extends LitElement {

    // Importing Styles; the unsafe GridStack css, and all custom css
    static get styles() {
        return [unsafeCSS(tabStyle), unsafeCSS(tabbarStyle), unsafeCSS(tabIndicatorStyle), unsafeCSS(tabScrollerStyle), styling, style]
    }

    @property()
    protected readonly config: DashboardBuilderConfig | undefined;

    @property()
    protected readonly editMode: boolean | undefined;

    @property()
    protected readonly selectedId: string | undefined;


    /* ------------------- */

    @state()
    protected dashboards: Dashboard[] | undefined;

    @state()
    protected selectedDashboard: Dashboard | undefined;

    @state()
    protected initialDashboardJSON: string | undefined;

    @state()
    protected currentTemplate: DashboardTemplate | undefined;

    @state()
    protected initialTemplateJSON: string | undefined;

    @state()
    protected currentWidget: DashboardWidget | undefined;

    @state()
    protected isInitializing: boolean;

    @state()
    protected isLoading: boolean;

    @state()
    protected hasChanged: boolean;


    /* ------------- */

    constructor() {
        super();
        this.isInitializing = true;
        this.isLoading = true;
        this.hasChanged = false;
        /*this.getAllDashboards().then((dashboards: Dashboard[]) => {
            this.dashboards = dashboards;
        });*/
        this.updateComplete.then(async () => {
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

            // Getting dashboards
            await manager.rest.api.DashboardResource.getAllUserDashboards().then((result) => {
                this.dashboards = result.data;
            });

            // Setting dashboard if selectedId is given by parent component
            if(this.selectedId != undefined) {
                console.log("dashboardId parameter detected! Value is [" + this.selectedId + "]");
                manager.rest.api.DashboardResource.get(this.selectedId).then((dashboard) => {
                    /*this.selected = dashboard.data;*/
                    this.selectedDashboard = Object.assign({}, this.dashboards?.find(x => { return x.id == dashboard.data.id; }));
                });

            // Otherwise, just select the 1st one in the list
            } else {
                if(this.dashboards != null) {
                    this.selectedDashboard = Object.assign({}, this.dashboards[0]);
                }
            }
        });
    }

    updated(changedProperties: Map<string, any>) {
        console.log(changedProperties);
        this.isLoading = (this.selectedDashboard == undefined); // Update loading state on whether a dashboard is selected
        this.isInitializing = (this.selectedDashboard == undefined);
        if(changedProperties.has("selectedDashboard")) {
            this.hasChanged = (JSON.stringify(this.selectedDashboard) != JSON.stringify(this.initialDashboardJSON) || JSON.stringify(this.currentTemplate) != JSON.stringify(this.initialTemplateJSON));
            if(this.selectedDashboard != null) {

                // Set widgets to an empty array if null for GridStack to work.
                if(this.selectedDashboard.template != null && this.selectedDashboard.template.widgets == null) {
                    this.selectedDashboard.template.widgets = [];
                }
            } else if(this.selectedDashboard == undefined && this.dashboards != null) {
                if(this.selectedId != null) {
                    this.selectedDashboard = this.dashboards.find((x) => { return x.id == this.selectedId; })
                } else {
                    this.selectedDashboard = this.dashboards[0];
                }
            }
            this.currentTemplate = this.selectedDashboard?.template;
            this.dispatchEvent(new CustomEvent("selected", { detail: this.selectedDashboard }))
        }
        if(changedProperties.has("currentTemplate")) {
            this.hasChanged = !(JSON.stringify(this.selectedDashboard) == this.initialDashboardJSON || JSON.stringify(this.currentTemplate) == this.initialTemplateJSON);
            if(this.selectedDashboard != null) {
                this.selectedDashboard.template = this.currentTemplate;
            }
        }
        if(changedProperties.has("dashboards")) {
            console.log(this.dashboards);
        }
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

        const tempTemplate = this.currentTemplate;
        tempTemplate?.widgets?.push(widget);
        this.currentTemplate = Object.assign({}, tempTemplate);
        return widget;
    }

    deleteWidget(widget: DashboardWidget) {
        if(this.currentTemplate != null && this.currentTemplate.widgets != null) {
            const tempTemplate = this.currentTemplate;
            tempTemplate.widgets = tempTemplate.widgets?.filter((x) => { return x.id != widget.id; });
            this.currentTemplate = Object.assign({}, tempTemplate);
            // this.requestUpdate();
        }
        if(this.currentWidget?.id == widget.id) {
            this.deselectWidget();
        }
    }

    /* ------------------------------ */

    selectWidget(widget: DashboardWidget): void {
        const foundWidget = this.currentTemplate?.widgets?.find((x) => { return x.gridItem?.id == widget.gridItem?.id; });
        if(foundWidget != null) {
            console.log("Selected a new Widget! [" + foundWidget.displayName + "]");
            this.currentWidget = foundWidget;
        }
    }
    deselectWidget() {
        this.currentWidget = undefined;
    }

    /* --------------------- */

    selectDashboard(dashboard: Dashboard) {
        if(this.dashboards != null) {
            this.selectedDashboard = this.dashboards.find((x) => { return x.id == dashboard.id; });
            console.log("Updating selected Dashboard!");
            console.log(this.selectedDashboard);
            this.initialDashboardJSON = JSON.stringify(this.selectedDashboard);
            this.initialTemplateJSON = JSON.stringify(this.selectedDashboard?.template);
        }
    }

    saveDashboard() {
        if(this.selectedDashboard != null) {
            this.isLoading = true;
            manager.rest.api.DashboardResource.update(this.selectedDashboard).then((response) => {
                console.log(response);
                if(this.dashboards != null && this.selectedDashboard != null) {
                    this.initialDashboardJSON = JSON.stringify(this.selectedDashboard);
                    this.initialTemplateJSON = JSON.stringify(this.selectedDashboard.template);
                    this.dashboards[this.dashboards?.indexOf(this.selectedDashboard)] = this.selectedDashboard;
                    this.currentTemplate = Object.assign({}, this.selectedDashboard.template);
                }
                this.isLoading = false;
            })
        }
    }

    changeDashboardName(value: string) {
        if(this.selectedDashboard != null) {
            const dashboard = this.selectedDashboard;
            dashboard.displayName = value;
            this.selectedDashboard = Object.assign({}, dashboard);
        }
    }

    shareUrl(method: string) {
        let url = window.location.href.replace("true", "false"); // TODO: vulnerable to cases where the word 'true' is inside the Dashboard ID.
        if(method == 'copy') {
            navigator.clipboard.writeText(url);
        } else if(method == 'tab') {
            window.open(url, '_blank')?.focus()
        }
    }

    /* ----------------- */

    @state()
    protected sidebarMenuIndex: number = 0;

    @state()
    protected showDashboardTree: boolean = true;

    // Rendering the page
    render(): any {
        const menuItems: ListItem[] = [
            { icon: "content-copy", text: "Copy URL", value: "copy" },
            { icon: "open-in-new", text: "Open in new Tab", value: "tab" },
        ]
        return (!this.isInitializing || (this.dashboards != null && this.dashboards.length == 0)) ? html`
            <div id="container">
                ${this.showDashboardTree ? html`
                    <or-dashboard-tree id="tree" .selected="${this.selectedDashboard}" .dashboards="${this.dashboards}" @updated="${(event: CustomEvent) => { this.dashboards = event.detail; this.selectedDashboard = undefined; }}" @select="${(event: CustomEvent) => { this.selectDashboard(event.detail); }}"></or-dashboard-tree>
                ` : undefined}
                <div id="container" style="display: table;">
                    ${this.editMode ? html`
                        <div id="header">
                            <div id="header-wrapper">
                                <div id="header-title">
                                    <or-icon icon="view-dashboard"></or-icon>
                                    <or-mwc-input type="${InputType.TEXT}" min="1" max="1023" comfortable required outlined label="Name" 
                                                  .value="${this.selectedDashboard != null ? this.selectedDashboard.displayName : ' '}"
                                                  .disabled="${this.isLoading || (this.selectedDashboard == null)}" 
                                                  @or-mwc-input-changed="${(event: OrInputChangedEvent) => { this.changeDashboardName(event.detail.value); }}"
                                                  style="min-width: 320px;">
                                        
                                    </or-mwc-input>
                                </div>
                                <div id="header-actions">
                                    <div id="header-actions-content">
                                        <!--<or-mwc-input id="share-btn" .disabled="${this.isLoading || (this.selectedDashboard == null)}" type="${InputType.BUTTON}" icon="share-variant"></or-mwc-input>-->
                                        ${getContentWithMenuTemplate(
                                                html`<or-mwc-input id="share-btn" .disabled="${this.isLoading || (this.selectedDashboard == null)}" type="${InputType.BUTTON}" icon="share-variant"></or-mwc-input>`,
                                                menuItems, "monitor", (method: any) => { this.shareUrl(method); }
                                        )}
                                        <or-mwc-input id="save-btn" .disabled="${this.isLoading || this.selectedDashboard == null || !this.hasChanged}" type="${InputType.BUTTON}" raised label="Save" @click="${() => { this.saveDashboard(); }}"></or-mwc-input>
                                        <or-mwc-input id="view-btn" .disabled="${this.isLoading || (this.selectedDashboard == null)}" type="${InputType.BUTTON}" outlined icon="eye" label="View" @click="${() => { this.dispatchEvent(new CustomEvent('editToggle', { detail: false })); }}"></or-mwc-input>
                                    </div>
                                </div>
                            </div>
                        </div>
                    ` : html`
                        <div id="fullscreen-header">
                            <div id="fullscreen-header-wrapper">
                                <div id="fullscreen-header-title">
                                    <!--<or-icon icon="menu" @click="${() => { this.showDashboardTree = !this.showDashboardTree; }}"></or-icon>-->
                                    <or-mwc-input type="${InputType.BUTTON}" icon="menu" @click="${() => { this.showDashboardTree = !this.showDashboardTree; }}"></or-mwc-input>   
                                    <span>${this.selectedDashboard?.displayName}</span>
                                </div>
                                <div id="fullscreen-header-actions">
                                    <div id="fullscreen-header-actions-content">
                                        <!--<or-mwc-input id="share-btn" .disabled="${this.isLoading || (this.selectedDashboard == null)}" type="${InputType.BUTTON}" icon="share-variant"></or-mwc-input>-->
                                        ${getContentWithMenuTemplate(
                                                html`<or-mwc-input id="share-btn" .disabled="${this.isLoading || (this.selectedDashboard == null)}" type="${InputType.BUTTON}" icon="share-variant"></or-mwc-input>`,
                                                menuItems, "monitor", (method: any) => { this.shareUrl(method); }
                                        )}
                                        <or-mwc-input id="view-btn" .disabled="${this.isLoading || (this.selectedDashboard == null)}" type="${InputType.BUTTON}" outlined icon="pencil" label="Modify" @click="${() => { this.dispatchEvent(new CustomEvent('editToggle', { detail: true })); }}"></or-mwc-input>
                                    </div>
                                </div>
                            </div>
                        </div>
                    `}
                    <div id="content">
                        <div id="container">
                            <div id="builder">
                                ${(this.selectedDashboard != null) ? html`
                                    <or-dashboard-editor class="editor" style="background: transparent;" .template="${this.currentTemplate}" .selected="${this.currentWidget}" .editMode="${this.editMode}" .isLoading="${this.isLoading}"
                                                         @selected="${(event: CustomEvent) => { console.log(event); this.selectWidget(event.detail); }}"
                                                         @deselected="${(event: CustomEvent) => { console.log(event); this.deselectWidget(); }}"
                                                         @dropped="${(event: CustomEvent) => { console.log(event); this.createWidget(event.detail); }}"
                                                         @changed="${(event: CustomEvent) => { console.log(event); this.currentTemplate = Object.assign({}, event.detail.template); }}"
                                    ></or-dashboard-editor>
                                ` : html`
                                    <div style="justify-content: center; display: flex; align-items: center; height: 100%;">
                                        <span>Please select a Dashboard from the left.</span>
                                    </div>
                                `}
                            </div>
                            ${(this.editMode) ? html`
                                <div id="sidebar">
                                    ${this.currentWidget != null ? html`
                                        <div>
                                            <div id="menu-header">
                                                <div id="title-container">
                                                    <span id="title">${this.currentWidget?.displayName}:</span>
                                                </div>
                                                <div>
                                                    <or-mwc-input type="${InputType.BUTTON}" icon="close" style="" @click="${(event: CustomEvent) => { this.deselectWidget(); }}"></or-mwc-input>
                                                </div>
                                            </div>
                                            <div id="content" style="display: block;">
                                                <or-dashboard-widgetsettings .selectedWidget="${this.currentWidget}"
                                                                             @delete="${(event: CustomEvent) => { this.deleteWidget(event.detail); }}"
                                                ></or-dashboard-widgetsettings>
                                            </div>
                                        </div>
                                    ` : undefined}
                                    <div style="${this.currentWidget != null ? css`display: none` : null}">
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
                            ` : undefined}
                        </div>
                    </div>
                </div>
            </div>
        ` : null
    }

    private async getAllDashboards(): Promise<Dashboard[]> {
        return manager.rest.api.DashboardResource.getAllUserDashboards().then((response) => {
            return response.data as Dashboard[];
        });
    }

    /* ======================== */

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
        if(this.selectedDashboard != null && this.currentTemplate != null && this.currentTemplate.widgets != null) {
            const filteredWidgets: DashboardWidget[] = this.currentTemplate.widgets.filter((x) => { return x.widgetType == widgetType; });
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

    getWidgetContent(widgetType: DashboardWidgetType, displayName: string): string {
        switch (widgetType) {
            case DashboardWidgetType.CHART: {
                return ('<div class="gridItem"><or-chart></or-chart></div>');
            } case DashboardWidgetType.MAP: {
                return ('<div class="gridItem"><or-map center="5.454250, 51.445990" zoom="5" style="height: 500px; width: 100%;"></or-map></div>');
            }
        }
    }

    /* ----------------- */

    /*generateInitialDashboard(): Dashboard {
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
    }*/
}
