import {css, html, LitElement, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import "./or-dashboard-tree";
import "./or-dashboard-browser";
import "./or-dashboard-editor";
import "./or-dashboard-preview";
import "./or-dashboard-widgetsettings";
import "./or-dashboard-boardsettings";
import {InputType, OrInputChangedEvent } from '@openremote/or-mwc-components/or-mwc-input';
import "@openremote/or-icon";
import {style} from "./style";
import {ORGridStackNode} from "./or-dashboard-preview"; //nosonar
import {Dashboard, DashboardAccess, DashboardGridItem, DashboardScalingPreset,
    DashboardScreenPreset, DashboardTemplate, DashboardWidget, DashboardWidgetType} from "@openremote/model";
import manager, {DefaultColor3, DefaultColor5 } from "@openremote/core";
import { getContentWithMenuTemplate } from "@openremote/or-mwc-components/or-mwc-menu";
import { ListItem } from "@openremote/or-mwc-components/or-mwc-list";
import { OrMwcTabItem } from "@openremote/or-mwc-components/or-mwc-tabs";
import "@openremote/or-mwc-components/or-mwc-tabs"; //nosonar

// language=CSS
const styling = css`

    #tree {
        flex-shrink: 0;
        align-items: stretch;
        z-index: 1;
        max-width: 300px;
        box-shadow: rgb(0 0 0 / 21%) 0px 1px 3px 0px;
    }
    
    /* Header related styling */
    #header {
        display: table-row;
        height: 1px;
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
        flex-shrink: 0;
        flex-direction: column;
        width: 300px;
        background: white;
        border-left: 1px solid #E0E0E0;
        z-index: 0;
    }
    #browser {
        flex-grow: 1;
        align-items: stretch;
        z-index: 1;
        max-width: 300px;
    }
    
    #save-btn { margin-left: 15px; }
    #view-btn { margin-left: 15px; }
`;

export interface DashboardBuilderConfig {
    // no configuration built yet
}
export enum DashboardSizeOption {
    LARGE, MEDIUM, SMALL, FULLSCREEN, CUSTOM
}

// Enum to Menu String method
export function scalingPresetToString(scalingPreset: DashboardScalingPreset | undefined): string {
    if(scalingPreset != null) {
        switch (scalingPreset) {
            case DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN: {
                return "Wrap widgets to one column";
            } case DashboardScalingPreset.KEEP_LAYOUT: {
                return "Keep layout";
            } case DashboardScalingPreset.BLOCK_DEVICE: {
                return "Block this device";
            } case DashboardScalingPreset.REDIRECT: {
                return "Redirect to a different Dashboard.."
            }
        }
    }
    return "undefined";
}

// Menu String to enum method
export function stringToScalingPreset(scalingPreset: string): DashboardScalingPreset {
    switch (scalingPreset) {
        case "Wrap widgets to one column": { return DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN; }
        case "Keep layout": { return DashboardScalingPreset.KEEP_LAYOUT; }
        case "Block this device": { return DashboardScalingPreset.BLOCK_DEVICE; }
        case "Redirect to a different Dashboard..": { return DashboardScalingPreset.REDIRECT; }
        default: { return DashboardScalingPreset.KEEP_LAYOUT; }
    }

}

export function stringToSizeOption(sizeOption: string): DashboardSizeOption {
    switch (sizeOption) {
        case "Large": { return DashboardSizeOption.LARGE; }
        case "Medium": { return DashboardSizeOption.MEDIUM; }
        case "Small": { return DashboardSizeOption.SMALL; }
        default: { return DashboardSizeOption.MEDIUM; }
    }
}
export function sizeOptionToString(sizeOption: DashboardSizeOption): string {
    switch (sizeOption) {
        case DashboardSizeOption.FULLSCREEN: { return "Fullscreen"; }
        case DashboardSizeOption.LARGE: { return "Large"; }
        case DashboardSizeOption.MEDIUM: { return "Medium"; }
        case DashboardSizeOption.SMALL: { return "Small"; }
        case DashboardSizeOption.CUSTOM: { return "Custom Size"; }
    }
}
export function stringToDashboardAccess(access: string): DashboardAccess {
    switch (access) {
        case "Public": { return DashboardAccess.PUBLIC; }
        case "Realm users": { return DashboardAccess.SHARED; }
        case "Only me": { return DashboardAccess.PRIVATE; }
        default: { return DashboardAccess.SHARED; }
    }
}
export function dashboardAccessToString(access: DashboardAccess): string {
    switch (access) {
        case DashboardAccess.PUBLIC: { return "Public"; }
        case DashboardAccess.SHARED: { return "Realm users"; }
        case DashboardAccess.PRIVATE: { return "Only me"; }
    }
}

export function sortScreenPresets(presets: DashboardScreenPreset[], largetosmall: boolean = false): DashboardScreenPreset[] {
    return presets.sort((a, b) => {
        if(a.breakpoint != null && b.breakpoint != null) {
            if(a.breakpoint > b.breakpoint) {
                return (largetosmall ? 1 : -1);
            }
            if(a.breakpoint < b.breakpoint) {
                return (largetosmall ? 1 : -1);
            }
        }
        return 0;
    });
}

export function getWidthByPreviewSize(sizeOption?: DashboardSizeOption): string {
    switch (sizeOption) {
        case DashboardSizeOption.FULLSCREEN: return '100%';
        case DashboardSizeOption.LARGE: return '1920px';
        case DashboardSizeOption.MEDIUM: return '1280px';
        case DashboardSizeOption.SMALL: return '480px';
        default: return '900px';
    }
}

export function getHeightByPreviewSize(sizeOption?: DashboardSizeOption): string {
    switch (sizeOption) {
        case DashboardSizeOption.FULLSCREEN: return '100%';
        case DashboardSizeOption.LARGE: return '1080px';
        case DashboardSizeOption.MEDIUM: return '720px';
        case DashboardSizeOption.SMALL: return '640px';
        default: return '540px';
    }
}

export function getPreviewSizeByPx(width?: string, height?: string): DashboardSizeOption {
    if(width == null && height == null) {
        console.error("Neither the previewWidth, nor previewHeight, nor previewSize attributes have been specified!"); return DashboardSizeOption.CUSTOM;
    } else {
        if(width == '100%' && height == '100%') { return DashboardSizeOption.FULLSCREEN; }
        else if(width == '1920px' && height == '1080px') { return DashboardSizeOption.LARGE; }
        else if(width == '1280px' && height == '720px') { return DashboardSizeOption.MEDIUM; }
        else if(width == '480px' && height == '640px') { return DashboardSizeOption.SMALL; }
        else { return DashboardSizeOption.CUSTOM; }
    }
}

export function getActivePreset(gridWidth: number, presets: DashboardScreenPreset[]): DashboardScreenPreset | undefined {
    let activePreset: DashboardScreenPreset | undefined;
    sortScreenPresets(presets, true).forEach((preset) => {
        if(preset.breakpoint != null && gridWidth <= preset.breakpoint) {
            activePreset = preset;
        }
    });
    return activePreset;
}


@customElement("or-dashboard-builder")
export class OrDashboardBuilder extends LitElement {

    // Importing Styles; the unsafe GridStack css, and all custom css
    static get styles() {
        return [styling, style]
    }

    @property()
    protected readonly config: DashboardBuilderConfig | undefined;

    @property() // Originally from URL
    protected readonly editMode: boolean | undefined;

    @property() // Originally from URL
    protected readonly selectedId: string | undefined;


    /* ------------------- */

    @state()
    protected dashboards: Dashboard[] | undefined;

    @state() // Separate local template object
    protected currentTemplate: DashboardTemplate | undefined;

    @state()
    protected selectedDashboard: Dashboard | undefined;

    @state()
    protected selectedWidget: DashboardWidget | undefined;

    @state() // Used to toggle the SAVE button depending on whether changes have been made.
    protected initialDashboardJSON: string | undefined;

    @state() // Used to toggle the SAVE button depending on whether changes have been made.
    protected initialTemplateJSON: string | undefined;

    @state()
    protected isInitializing: boolean;

    @state()
    protected isLoading: boolean;

    @state() // Whether changes have been made
    protected hasChanged: boolean;

    @state()
    protected previewSize: DashboardSizeOption; // DashboardSizeOption

    @state()
    protected rerenderPending: boolean;


    /* ------------- */

    constructor() {
        super();
        this.isInitializing = true;
        this.isLoading = true;
        this.hasChanged = false;
        this.previewSize = DashboardSizeOption.MEDIUM; // default, almost never used
        this.rerenderPending = false;
        this.updateComplete.then(async () => {

            // Getting dashboards
            await manager.rest.api.DashboardResource.getAllRealmDashboards(manager.displayRealm).then((result) => {
                this.dashboards = result.data;
            });

            // Setting dashboard if selectedId is given by parent component
            if(this.selectedId != undefined) {
                manager.rest.api.DashboardResource.get(this.selectedId).then((dashboard) => {
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

    /* ------------- */

    // On every property update
    updated(changedProperties: Map<string, any>) { //nosonar
        console.log(changedProperties);
        this.isLoading = (this.selectedDashboard == undefined);
        this.isInitializing = (this.selectedDashboard == undefined);

        // Any update on the dashboard
        if(changedProperties.has("selectedDashboard")) {
            this.hasChanged = (JSON.stringify(this.selectedDashboard) != this.initialDashboardJSON || JSON.stringify(this.currentTemplate) != this.initialTemplateJSON);
            this.selectedWidget = undefined;
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

        // Update on the Grid and its widget
        if(changedProperties.has("currentTemplate")) {
            this.hasChanged = !(JSON.stringify(this.selectedDashboard) == this.initialDashboardJSON || JSON.stringify(this.currentTemplate) == this.initialTemplateJSON);
            if(this.selectedDashboard != null) {
                this.selectedDashboard.template = this.currentTemplate;
            }
        }
        if(changedProperties.has("editMode")) {
            this.showDashboardTree = true;
            this.previewSize = (this.editMode ? DashboardSizeOption.MEDIUM : DashboardSizeOption.FULLSCREEN)
        }
    }

    /* ----------------- */

    // Method for creating Widgets (reused at many places)
    createWidget(gridStackNode: ORGridStackNode): DashboardWidget {
        const randomId = (Math.random() + 1).toString(36).substring(2);
        let displayName = generateWidgetDisplayName(this.currentTemplate!, gridStackNode.widgetType);
        if(displayName == undefined) { displayName = "Widget #" + randomId; } // If no displayName, set random ID as name.
        const gridItem: DashboardGridItem = generateGridItem(gridStackNode, displayName);

        const widget = {
            id: randomId,
            displayName: displayName,
            gridItem: gridItem,
            widgetType: gridStackNode.widgetType
        } as DashboardWidget;

        const tempTemplate = this.currentTemplate;
        tempTemplate?.widgets?.push(widget);
        this.currentTemplate = Object.assign({}, tempTemplate); // Force property update
        return widget;
    }

    deleteWidget(widget: DashboardWidget) {
        if(this.currentTemplate != null && this.currentTemplate.widgets != null) {
            const tempTemplate = this.currentTemplate;
            tempTemplate.widgets = tempTemplate.widgets?.filter((x) => { return x.id != widget.id; });
            this.currentTemplate = Object.assign({}, tempTemplate);
        }
        if(this.selectedWidget?.id == widget.id) {
            this.deselectWidget();
        }
    }

    /* ------------------------------ */

    selectWidget(widget: DashboardWidget): void {
        const foundWidget = this.currentTemplate?.widgets?.find((x) => { return x.gridItem?.id == widget.gridItem?.id; });
        if(foundWidget != null) {
            this.selectedWidget = foundWidget;
        } else {
            console.error("The selected widget does not exist!");
        }
    }
    deselectWidget() {
        this.selectedWidget = undefined;
    }

    /* --------------------- */

    selectDashboard(dashboard: Dashboard) {
        if(this.dashboards != null) {
            this.selectedDashboard = this.dashboards.find((x) => { return x.id == dashboard.id; });
            this.initialDashboardJSON = JSON.stringify(this.selectedDashboard);
            this.initialTemplateJSON = JSON.stringify(this.selectedDashboard?.template);
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
        let url = window.location.href.replace("true", "false");
        if(method == 'copy') {
            navigator.clipboard.writeText(url);
        } else if(method == 'tab') {
            window.open(url, '_blank')?.focus()
        }
    }

    /* ----------------------------------- */

    saveDashboard() {
        if(this.selectedDashboard != null) {
            this.isLoading = true;

            // Saving object into the database
            manager.rest.api.DashboardResource.update(this.selectedDashboard).then(() => {
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

    /* ----------------- */

    @state()
    protected sidebarMenuIndex: number = 0;

    @state()
    protected showDashboardTree: boolean = true;

    // Rendering the page
    render(): any { //nosonar
        const menuItems: ListItem[] = [
            { icon: "content-copy", text: "Copy URL", value: "copy" },
            { icon: "open-in-new", text: "Open in new Tab", value: "tab" },
        ];
        const tabItems: OrMwcTabItem[] = [
            { name: "WIDGETS" }, { name: "SETTINGS"}
        ];
        return (!this.isInitializing || (this.dashboards != null && this.dashboards.length == 0)) ? html`
            <div id="container">
                ${this.showDashboardTree ? html`
                    <or-dashboard-tree id="tree" .selected="${this.selectedDashboard}" .dashboards="${this.dashboards}" .showControls="${true}"
                                       @created="${(event: CustomEvent) => { this.previewSize = event.detail.size; }}"
                                       @updated="${(event: CustomEvent) => { this.dashboards = event.detail; this.selectedDashboard = undefined; }}"
                                       @select="${(event: CustomEvent) => { this.selectDashboard(event.detail); }}"
                    ></or-dashboard-tree>
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
                                        ${getContentWithMenuTemplate(
                                                html`<or-mwc-input id="share-btn" .disabled="${this.isLoading || (this.selectedDashboard == null)}" type="${InputType.BUTTON}" icon="share-variant"></or-mwc-input>`,
                                                menuItems, "monitor", (method: any) => { this.shareUrl(method); }
                                        )}
                                        <or-mwc-input id="save-btn" .disabled="${this.isLoading || (this.selectedDashboard == null)}" type="${InputType.BUTTON}" raised label="Save" @or-mwc-input-changed="${() => { this.saveDashboard(); }}"></or-mwc-input>
                                        <or-mwc-input id="view-btn" type="${InputType.BUTTON}" outlined icon="eye" label="View" @or-mwc-input-changed="${() => { this.dispatchEvent(new CustomEvent('editToggle', { detail: false })); }}"></or-mwc-input>
                                    </div>
                                </div>
                            </div>
                        </div>
                    ` : html`
                        <div id="fullscreen-header">
                            <div id="fullscreen-header-wrapper">
                                <div id="fullscreen-header-title">
                                    <or-mwc-input type="${InputType.BUTTON}" icon="menu" @or-mwc-input-changed="${() => { this.showDashboardTree = !this.showDashboardTree; }}"></or-mwc-input>   
                                    <span>${this.selectedDashboard?.displayName}</span>
                                </div>
                                <div id="fullscreen-header-actions">
                                    <div id="fullscreen-header-actions-content">
                                        ${getContentWithMenuTemplate(
                                                html`<or-mwc-input id="share-btn" .disabled="${this.isLoading || (this.selectedDashboard == null)}" type="${InputType.BUTTON}" icon="share-variant"></or-mwc-input>`,
                                                menuItems, "monitor", (method: any) => { this.shareUrl(method); }
                                        )}
                                        <or-mwc-input id="view-btn" type="${InputType.BUTTON}" outlined icon="pencil" label="Modify" @or-mwc-input-changed="${() => { this.dispatchEvent(new CustomEvent('editToggle', { detail: true })); }}"></or-mwc-input>
                                    </div>
                                </div>
                            </div>
                        </div>
                    `}
                    <div id="content">
                        <div id="container">
                            <div id="builder">
                                ${(this.selectedDashboard != null) ? html`
                                    <!--<or-dashboard-editor class="editor" style="background: transparent;" .template="${this.currentTemplate}" .selected="${this.selectedWidget}" .editMode="${this.editMode}" .fullscreen="${!this.editMode}"
                                                         .previewSize="${this.previewSize}" .isLoading="${this.isLoading}" .rerenderPending="${this.rerenderPending}"
                                                         @selected="${(event: CustomEvent) => { this.selectWidget(event.detail); }}"
                                                         @deselected="${() => { this.deselectWidget(); }}"
                                                         @dropped="${(event: CustomEvent) => { this.createWidget(event.detail); }}"
                                                         @changed="${(event: CustomEvent) => { this.currentTemplate = Object.assign({}, event.detail.template); }}"
                                                         @rerender="${() => { this.rerenderPending = false; }}"
                                    ></or-dashboard-editor>-->
                                    <or-dashboard-preview class="editor" style="background: transparent;"
                                                          .template="${this.currentTemplate}"
                                                          .selectedWidget="${this.selectedWidget}" .editMode="${this.editMode}"
                                                          .previewSize="${this.previewSize}"
                                                          @selected="${(event: CustomEvent) => { this.selectWidget(event.detail); }}"
                                                          @changed="${(event: CustomEvent) => { this.currentTemplate = event.detail.template; }}"
                                                          @deselected="${() => { this.deselectWidget(); }}"
                                    ></or-dashboard-preview>
                                ` : html`
                                    <div style="justify-content: center; display: flex; align-items: center; height: 100%;">
                                        <span>Please select a Dashboard from the left.</span>
                                    </div>
                                `}
                            </div>
                            ${(this.editMode) ? html`
                                <div id="sidebar">
                                    ${this.selectedWidget != null ? html`
                                        <div>
                                            <div id="menu-header">
                                                <div id="title-container">
                                                    <span id="title">${this.selectedWidget?.displayName}:</span>
                                                </div>
                                                <div>
                                                    <or-mwc-input type="${InputType.BUTTON}" icon="close" style="" @or-mwc-input-changed="${() => { this.deselectWidget(); }}"></or-mwc-input>
                                                </div>
                                            </div>
                                            <div id="content" style="display: block;">
                                                <or-dashboard-widgetsettings .selectedWidget="${this.selectedWidget}"
                                                                             @delete="${(event: CustomEvent) => { this.deleteWidget(event.detail); }}"
                                                                             @update="${() => { this.currentTemplate = Object.assign({}, this.selectedDashboard?.template); }}"
                                                ></or-dashboard-widgetsettings>
                                            </div>
                                        </div>
                                    ` : undefined}
                                    <div style="${this.selectedWidget != null ? css`display: none` : null}">
                                        <div style="border-bottom: 1px solid ${unsafeCSS(DefaultColor5)};">
                                            <or-mwc-tabs .items="${tabItems}" @activated="${(event: CustomEvent) => { this.sidebarMenuIndex = event.detail.index; }}" style="pointer-events: ${this.selectedDashboard ? undefined : 'none'}"></or-mwc-tabs>
                                        </div>
                                        <div id="content" style="border: 1px solid #E0E0E0; height: 100%; display: contents;">
                                            <or-dashboard-browser id="browser" style="${this.sidebarMenuIndex != 0 ? css`display: none` : null}"></or-dashboard-browser>
                                            <div id="item" style="${this.sidebarMenuIndex != 1 ? css`display: none` : null}"> <!-- Setting display to none instead of not rendering it. -->
                                                <or-dashboard-boardsettings .dashboard="${this.selectedDashboard}"
                                                                            @update="${(event: CustomEvent) => { this.currentTemplate = Object.assign({}, this.selectedDashboard?.template); (event.detail.force == true ? this.rerenderPending = true : undefined); }}"
                                                ></or-dashboard-boardsettings>
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
        return manager.rest.api.DashboardResource.getAllRealmDashboards(manager.displayRealm).then((response) => {
            return response.data;
        });
    }

    /* ======================== */

}

// Generating the Grid Item details like X and Y coordinates for GridStack to work.
export function generateGridItem(gridstackNode: ORGridStackNode, displayName: string): DashboardGridItem {
    console.log("Creating gridItem for " + displayName);
    const randomId = (Math.random() + 1).toString(36).substring(2);
    return {
        id: randomId,
        x: gridstackNode.x,
        y: gridstackNode.y,
        w: 2,
        h: 2,
        minW: getWidgetMinWidth(gridstackNode.widgetType),
        minH: getWidgetMinWidth(gridstackNode.widgetType),
        noResize: false,
        noMove: false,
        locked: false,
        // content: this.getWidgetContent(gridstackNode.widgetType, displayName)
    }
}
export function generateWidgetDisplayName(template: DashboardTemplate, widgetType: DashboardWidgetType): string | undefined {
    if(template.widgets != null) {
        const filteredWidgets: DashboardWidget[] = template.widgets.filter((x) => { return x.widgetType == widgetType; });
        switch (widgetType) {
            case DashboardWidgetType.KPI: return "KPI #" + (filteredWidgets.length + 1);
            case DashboardWidgetType.CHART: return "Chart #" + (filteredWidgets.length + 1);
        }
    }
    return undefined;
}
export function getWidgetMinWidth(widgetType: DashboardWidgetType): number {
    switch (widgetType) {
        case DashboardWidgetType.CHART: return 2;
        case DashboardWidgetType.KPI: return 1;
    }
}
