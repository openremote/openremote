import {css, html, LitElement, unsafeCSS} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import {when} from 'lit/directives/when.js';
import {styleMap} from 'lit/directives/style-map.js';
import "./or-dashboard-tree";
import "./or-dashboard-browser";
import "./or-dashboard-preview";
import "./or-dashboard-widgetsettings";
import "./or-dashboard-boardsettings";
import {InputType, OrInputChangedEvent} from '@openremote/or-mwc-components/or-mwc-input';
import "@openremote/or-icon";
import {style} from "./style";
import {ORGridStackNode} from "./or-dashboard-preview";
import {
    ClientRole,
    Dashboard,
    DashboardAccess,
    DashboardGridItem,
    DashboardScalingPreset,
    DashboardScreenPreset,
    DashboardTemplate,
    DashboardWidget
} from "@openremote/model";
import manager, {DefaultColor1, DefaultColor3, DefaultColor5, Util} from "@openremote/core";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import {OrMwcTabItem} from "@openremote/or-mwc-components/or-mwc-tabs";
import "@openremote/or-mwc-components/or-mwc-tabs";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {i18next} from "@openremote/or-translate";
import { showOkCancelDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import {DashboardKeyEmitter} from "./or-dashboard-keyhandler";
import {OrWidgetEntity} from "./widgets/or-base-widget";
import {OrChartWidget} from "./widgets/or-chart-widget";
import { OrKpiWidget } from "./widgets/or-kpi-widget";
import { OrGaugeWidget } from "./widgets/or-gauge-widget";
import {OrMapWidget} from "./widgets/or-map-widget";

// language=CSS
const styling = css`
    
    @media only screen and (min-width: 641px){
        #tree {
            min-width: 300px !important;
        }
    }
    @media only screen and (max-width: 641px) {
        #tree {
            flex: 1 !important;
        }
        #builder {
            max-height: inherit !important;
        }
    }
    
    #tree {
        flex: 0;
        align-items: stretch;
        z-index: 1;
        box-shadow: rgb(0 0 0 / 21%) 0px 1px 3px 0px;
    }
    
    /* Header related styling */
    #header {
        background: var(--or-app-color1, ${unsafeCSS(DefaultColor1)});
    }
    #header-wrapper {
        padding: 14px 30px;
        display: flex;
        flex-direction: row;
        align-items: center;
        border-bottom: 1px solid ${unsafeCSS(DefaultColor5)};
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
    @media screen and (max-width: 700px) {
        #fullscreen-header-wrapper {
            padding: 11px !important;
        }
    }
    #fullscreen-header-wrapper {
        min-height: 36px;
        padding: 20px 30px 15px;
        display: flex;
        flex-direction: row;
        align-items: center;
    }
    #fullscreen-header-title {
        font-size: 18px;
        font-weight: bold;
        color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
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
        flex: 1 0 auto;
        height: 100%;
    }
    
    /* ----------------------------- */
    /* Sidebar related styling (drag and drop widgets / configuration) */
    #sidebar {
        vertical-align: top;
        position: relative;
        width: 300px;
        background: white;
        border-left: 1px solid ${unsafeCSS(DefaultColor5)};
    }
    #sidebar-widget-headeractions {
        flex: 0;
        display: flex;
        flex-direction: row;
        padding-right: 5px;
    }
    .settings-container {
        display: flex;
        flex-direction: column;
        height: 100%;
    }
    #browser {
        flex-grow: 1;
        align-items: stretch;
        z-index: 1;
        max-width: 300px;
    }
    
    #save-btn { margin-left: 15px; }
    #view-btn { margin-left: 18px; }
    
    .small-btn {
        height: 36px;
        margin-top: -12px;
    }
    
    .hidescroll {
        -ms-overflow-style: none; /* for Internet Explorer, Edge */
        scrollbar-width: none; /* for Firefox */
    }
    .hidescroll::-webkit-scrollbar {
        display: none; /* for Chrome, Safari, and Opera */
    }
`;

export interface DashboardBuilderConfig {
    // no configuration built yet
}
export const MAX_BREAKPOINT = 1000000;

// Enum to Menu String method
export function scalingPresetToString(scalingPreset: DashboardScalingPreset | undefined): string {
    return (scalingPreset != null ? i18next.t("dashboard.presets." + scalingPreset.toLowerCase()) : "undefined");
}
export function dashboardAccessToString(access: DashboardAccess): string {
    return i18next.t("dashboard.access." + access.toLowerCase());
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

export function getActivePreset(gridWidth: number, presets: DashboardScreenPreset[]): DashboardScreenPreset | undefined {
    let activePreset: DashboardScreenPreset | undefined;
    sortScreenPresets(presets, true).forEach((preset) => {
        if(preset.breakpoint != null && gridWidth <= preset.breakpoint) {
            activePreset = preset;
        }
    });
    return activePreset;
}
export const widgetTypes: Map<string, OrWidgetEntity> = new Map<string, OrWidgetEntity>();

export function registerWidgetTypes() {
    widgetTypes.set("linechart", new OrChartWidget());
    widgetTypes.set("kpi", new OrKpiWidget());
    widgetTypes.set("gauge", new OrGaugeWidget());
    widgetTypes.set("map", new OrMapWidget());
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
    protected readonly editMode: boolean = false;

    @property()
    protected readonly fullscreen: boolean = true;

    @property() // Originally from URL
    protected readonly selectedId: string | undefined;

    @property() // Originally just manager.displayRealm
    protected realm: string | undefined;

    @property() // REQUIRED userId
    protected userId: string | undefined;

    @property()
    protected readonly: boolean = true;


    /* ------------------- */

    @state()
    protected dashboards: Dashboard[] | undefined;

    @state() // Separate local template object
    protected currentTemplate: DashboardTemplate | undefined;

    @state()
    protected selectedDashboard: Dashboard | undefined;

    @state()
    protected selectedWidgetId: string | undefined;

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
    protected rerenderPending: boolean;

    private readonly keyEmitter: DashboardKeyEmitter = new DashboardKeyEmitter();


    /* ------------- */

    constructor() {
        super();
        this.isInitializing = true;
        this.isLoading = true;
        this.hasChanged = false;
        this.rerenderPending = false;

        registerWidgetTypes();

        this.updateComplete.then(async () => {
            await this.updateDashboards(this.realm!);
        });
    }

    connectedCallback() {
        super.connectedCallback();
        this.keyEmitter.addListener('delete', (_e: KeyboardEvent) => {
            if(this.selectedWidgetId) {
                const selectedWidget = this.selectedDashboard?.template?.widgets?.find(w => w.id == this.selectedWidgetId);
                if(selectedWidget) { showOkCancelDialog(i18next.t('areYouSure'), i18next.t('dashboard.deleteWidgetWarning'), i18next.t('delete')).then((ok: boolean) => { if(ok) { this.deleteWidget(selectedWidget); }}); }
            }
        });
        this.keyEmitter.addListener('deselect', (_e: KeyboardEvent) => { this.deselectWidget(); });
        this.keyEmitter.addListener('save', (_e: KeyboardEvent) => { this.saveDashboard(); });
    }
    disconnectedCallback() {
        super.disconnectedCallback();
        this.keyEmitter.removeAllListeners();
    }

    async updateDashboards(realm: string) {

        // Getting dashboards
        await manager.rest.api.DashboardResource.getAllRealmDashboards(realm).then((result) => {
            this.dashboards = result.data;
        }).catch((reason) => {
            showSnackbar(undefined, i18next.t('errorOccurred'));
            console.error(reason);
        });

        // Setting dashboard if selectedId is given by parent component
        if(this.selectedId != undefined) {
            this.selectedDashboard = this.dashboards?.find(x => { return x.id == this.selectedId; });
        }
    }

    /* ------------- */

    // On every property update
    updated(changedProperties: Map<string, any>) {
        this.isLoading = (this.dashboards == undefined);
        this.isInitializing = (this.dashboards == undefined);
        if(this.realm == undefined) { this.realm = manager.displayRealm; }

        // On any update (except widget selection), check whether hasChanged should be updated.
        if(!(changedProperties.size == 1 && changedProperties.has('selectedWidget'))) {
            const dashboardEqual = Util.objectsEqual(this.selectedDashboard, this.initialDashboardJSON ? JSON.parse(this.initialDashboardJSON) : undefined);
            const templateEqual = Util.objectsEqual(this.currentTemplate, this.initialTemplateJSON ? JSON.parse(this.initialTemplateJSON) : undefined);
            this.hasChanged = (!dashboardEqual || !templateEqual);
        }

        // Support for realm switching
        if(changedProperties.has("realm") && changedProperties.get("realm") != undefined) {
            this.updateDashboards(this.realm);
        }

        // Any update on the dashboard
        if(changedProperties.has("selectedDashboard")) {
            this.deselectWidget();
            this.currentTemplate = this.selectedDashboard?.template;
            this.dispatchEvent(new CustomEvent("selected", { detail: this.selectedDashboard }))
        }

        // Update on the Grid and its widget
        if(changedProperties.has("currentTemplate")) {
            if(this.selectedDashboard != null) {
                this.selectedDashboard.template = this.currentTemplate;
            }
        }
        // When edit/view mode gets toggled
        if(changedProperties.has("editMode")) {
            this.deselectWidget();
            this.showDashboardTree = true;
        }
    }

    /* ----------------- */


    createWidget(gridStackNode: ORGridStackNode): DashboardWidget {
        const randomId = (Math.random() + 1).toString(36).substring(2);
        let displayName = widgetTypes.get(gridStackNode.widgetTypeId)?.DISPLAY_NAME;
        if(displayName == undefined) { displayName = (i18next.t('dashboard.widget') + " #" + randomId); } // If no displayName, set random ID as name.
        const gridItem: DashboardGridItem = generateGridItem(gridStackNode, displayName);

        const widget = {
            id: randomId,
            displayName: displayName,
            gridItem: gridItem,
            widgetTypeId: gridStackNode.widgetTypeId,
        } as DashboardWidget;
        widget.widgetConfig = widgetTypes.get(gridStackNode.widgetTypeId)?.getDefaultConfig(widget);

        const tempTemplate = JSON.parse(JSON.stringify(this.currentTemplate)) as DashboardTemplate;
        if(tempTemplate.widgets == undefined) {
            tempTemplate.widgets = [];
        }
        tempTemplate.widgets?.push(widget);
        this.currentTemplate = tempTemplate;
        return widget;
    }

    deleteWidget(widget: DashboardWidget) {
        if(this.currentTemplate != null && this.currentTemplate.widgets != null) {
            const tempTemplate = this.currentTemplate;
            tempTemplate.widgets = tempTemplate.widgets?.filter((x: DashboardWidget) => { return x.id != widget.id; });
            this.currentTemplate = tempTemplate;
        }
        if(this.selectedWidgetId == widget.id) {
            this.deselectWidget();
        }
    }

    /* ------------------------------ */

    selectWidget(widget: DashboardWidget): void {
        const foundWidget = this.currentTemplate?.widgets?.find((x) => { return x.gridItem?.id == widget.gridItem?.id; });
        if(foundWidget != null) {
            this.selectedWidgetId = foundWidget.id;
        } else {
            console.error("The selected widget does not exist!");
        }
    }
    deselectWidget() {
        this.selectedWidgetId = undefined;
    }

    /* --------------------- */

    selectDashboard(dashboard: Dashboard | undefined) {
        if(this.dashboards != null) {
            if(this.selectedDashboard && this.initialDashboardJSON) {
                const indexOf = this.dashboards.indexOf(this.selectedDashboard);
                if(indexOf) {
                    this.dashboards[indexOf] = JSON.parse(this.initialDashboardJSON) as Dashboard;
                }
            }
            this.selectedDashboard = (dashboard ? this.dashboards.find((x) => { return x.id == dashboard.id; }) : undefined);
            this.initialDashboardJSON = JSON.stringify(this.selectedDashboard);
            this.initialTemplateJSON = JSON.stringify(this.selectedDashboard?.template);
        }
    }

    changeDashboardName(value: string) {
        if(this.selectedDashboard != null) {
            const dashboard = this.selectedDashboard;
            dashboard.displayName = value;
            this.requestUpdate("selectedDashboard");
        }
    }

    openDashboardInInsights() {
        if(this.selectedDashboard != null) {
            const insightsUrl: string = (window.location.origin + "/insights/?realm=" + manager.displayRealm + "#/view/" + this.selectedDashboard.id + "/true/"); // Just using relative URL to origin, as its enough for now.
            window.open(insightsUrl)?.focus();
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
        if(this.selectedDashboard != null && !this._isReadonly() && this._hasEditAccess()) {
            this.isLoading = true;

            // Saving object into the database
            manager.rest.api.DashboardResource.update(this.selectedDashboard).then(() => {
                if(this.dashboards != null && this.selectedDashboard != null) {
                    this.initialDashboardJSON = JSON.stringify(this.selectedDashboard);
                    this.initialTemplateJSON = JSON.stringify(this.selectedDashboard.template);
                    this.dashboards[this.dashboards?.indexOf(this.selectedDashboard)] = this.selectedDashboard;
                    this.currentTemplate = Object.assign({}, this.selectedDashboard.template);
                    showSnackbar(undefined, i18next.t('dashboard.saveSuccessful'));
                }
            }).catch((reason) => {
                console.error(reason);
                showSnackbar(undefined, i18next.t('errorOccurred'));
            }).finally(() => {
                this.isLoading = false;
            })
        } else {
            console.error("The selected dashboard could not be found..");
            showSnackbar(undefined, i18next.t('errorOccurred'));
        }
    }

    protected _isReadonly(): boolean {
        return this.readonly || !manager.hasRole(ClientRole.WRITE_INSIGHTS);
    }
    protected _hasEditAccess(): boolean {
        return this.userId != null && (this.selectedDashboard?.editAccess == DashboardAccess.PRIVATE ? this.selectedDashboard?.ownerId == this.userId : true)
    }
    protected _hasViewAccess(): boolean {
        return this.userId != null && (this.selectedDashboard?.viewAccess == DashboardAccess.PRIVATE ? this.selectedDashboard?.ownerId == this.userId : true)
    }

    /* ----------------- */

    @state()
    protected sidebarMenuIndex: number = 0;

    @state()
    protected showDashboardTree: boolean = true;

    private readonly menuItems: ListItem[] = [
        { icon: "content-copy", text: (i18next.t("copy") + " URL"), value: "copy" },
        { icon: "open-in-new", text: i18next.t("dashboard.openInNewTab"), value: "tab" },
    ];
    private readonly tabItems: OrMwcTabItem[] = [
        { name: i18next.t("dashboard.widgets") }, { name: i18next.t("settings") }
    ];

    // Rendering the page
    render(): any {
        if(window.matchMedia("(max-width: 600px)").matches && this.editMode) {
            this.dispatchEvent(new CustomEvent('editToggle', { detail: false }));
            this.showDashboardTree = true;
        }
        const builderStyles = {
            display: (this.editMode && (this._isReadonly() || !this._hasEditAccess())) ? 'none' : undefined,
            maxHeight: this.editMode ? "calc(100vh - 77px - 50px)" : "inherit"
        };
        return (!this.isInitializing || (this.dashboards != null && this.dashboards.length == 0)) ? html`
            <div id="container">
                ${(this.showDashboardTree) ? html`
                    <or-dashboard-tree id="tree" class="${this.selectedDashboard ? 'hideMobile' : undefined}"
                                       .realm="${this.realm}" .hasChanged="${this.hasChanged}" .selected="${this.selectedDashboard}" .dashboards="${this.dashboards}" .showControls="${true}" .userId="${this.userId}" .readonly="${this._isReadonly()}"
                                       @created="${(_event: CustomEvent) => { this.dispatchEvent(new CustomEvent('editToggle', { detail: true })); }}"
                                       @updated="${(event: CustomEvent) => { this.dashboards = event.detail; this.selectedDashboard = undefined; }}"
                                       @select="${(event: CustomEvent) => { this.selectDashboard(event.detail); }}"
                    ></or-dashboard-tree>
                ` : undefined}
                <div class="${this.selectedDashboard == null ? 'hideMobile' : undefined}" style="flex: 1; display: flex; flex-direction: column;">
                    ${this.editMode ? html`
                        <div id="header" class="hideMobile">
                            <div id="header-wrapper">
                                <div id="header-title">
                                    <or-icon icon="view-dashboard"></or-icon>
                                    ${this.selectedDashboard != null ? html`
                                        <or-mwc-input .type="${InputType.TEXT}" min="1" max="1023" comfortable required outlined .label="${i18next.t('name') + '*\xa0'}" 
                                                      ?readonly="${this._isReadonly()}" .value="${this.selectedDashboard.displayName}" 
                                                      .disabled="${this.isLoading}" style="width: 300px;" 
                                                      @or-mwc-input-changed="${(event: OrInputChangedEvent) => { this.changeDashboardName(event.detail.value); }}"
                                        ></or-mwc-input>
                                    ` : undefined}
                                </div>
                                <div id="header-actions">
                                    <div id="header-actions-content">
                                        <or-mwc-input id="refresh-btn" class="small-btn" .disabled="${this.isLoading || (this.selectedDashboard == null)}" type="${InputType.BUTTON}" icon="refresh"
                                                      @or-mwc-input-changed="${() => { this.rerenderPending = true; }}">
                                        </or-mwc-input>
                                        <or-mwc-input id="responsive-btn" class="small-btn" .disabled="${this.isLoading || (this.selectedDashboard == null)}" type="${InputType.BUTTON}" icon="responsive"
                                                      @or-mwc-input-changed="${() => { this.dispatchEvent(new CustomEvent('fullscreenToggle', { detail: !this.fullscreen })); }}">
                                        </or-mwc-input>
                                        <or-mwc-input id="share-btn" class="small-btn" .disabled="${this.isLoading || (this.selectedDashboard == null)}" type="${InputType.BUTTON}" icon="open-in-new"
                                                      @or-mwc-input-changed="${() => { this.openDashboardInInsights(); }}">
                                        </or-mwc-input>
                                        <or-mwc-input id="save-btn" ?hidden="${this._isReadonly() || !this._hasEditAccess()}" .disabled="${this.isLoading || !this.hasChanged || (this.selectedDashboard == null)}" type="${InputType.BUTTON}" raised label="${i18next.t('save')}"
                                                      @or-mwc-input-changed="${() => { this.saveDashboard(); }}">
                                        </or-mwc-input>
                                        <or-mwc-input id="view-btn" ?hidden="${this._isReadonly() || !this._hasViewAccess()}" type="${InputType.BUTTON}" outlined icon="eye" label="${i18next.t('viewAsset')}"
                                                      @or-mwc-input-changed="${() => { this.dispatchEvent(new CustomEvent('editToggle', { detail: false })); }}">
                                        </or-mwc-input>
                                    </div>
                                </div>
                            </div>
                        </div>
                    ` : html`
                        <div id="fullscreen-header">
                            <div id="fullscreen-header-wrapper">
                                <div id="fullscreen-header-title" style="display: flex; align-items: center;">
                                    <or-icon class="showMobile" style="margin-right: 10px;" icon="chevron-left" @click="${() => { this.selectedDashboard = undefined; }}"></or-icon>
                                    <or-icon class="hideMobile" style="margin-right: 10px;" icon="menu" @click="${() => { this.showDashboardTree = !this.showDashboardTree; }}"></or-icon>
                                    <span>${this.selectedDashboard?.displayName}</span>
                                </div>
                                <div id="fullscreen-header-actions">
                                    <div id="fullscreen-header-actions-content">
                                        <or-mwc-input id="refresh-btn" class="small-btn" .disabled="${(this.selectedDashboard == null)}" type="${InputType.BUTTON}" icon="refresh"
                                                      @or-mwc-input-changed="${() => { this.rerenderPending = true; }}"
                                        ></or-mwc-input>
                                        <or-mwc-input id="share-btn" class="small-btn" .disabled="${(this.selectedDashboard == null)}" type="${InputType.BUTTON}" icon="open-in-new"
                                                      @or-mwc-input-changed="${() => { this.openDashboardInInsights(); }}"
                                        ></or-mwc-input>
                                        <or-mwc-input id="view-btn" class="hideMobile" ?hidden="${this.selectedDashboard == null || this._isReadonly() || !this._hasEditAccess()}" type="${InputType.BUTTON}" outlined icon="pencil" label="${i18next.t('editAsset')}"
                                                      @or-mwc-input-changed="${() => { this.dispatchEvent(new CustomEvent('editToggle', { detail: true })); }}"></or-mwc-input>
                                    </div>
                                </div>
                            </div>
                        </div>
                    `}
                    <div id="content" style="flex: 1;">
                        <div id="container">
                            ${(this.editMode && (this._isReadonly() || !this._hasEditAccess())) ? html`
                                <div style="display: flex; justify-content: center; align-items: center; height: 100%;">
                                    <span>${!this._hasEditAccess() ? i18next.t('noDashboardWriteAccess') : i18next.t('errorOccurred')}.</span>
                                </div>
                            ` : undefined}
                            <div id="builder" style="${styleMap(builderStyles)}">
                                ${(this.selectedDashboard != null) ? html`
                                    <or-dashboard-preview class="editor" style="background: transparent;"
                                                          .realm="${this.realm}" .template="${this.currentTemplate}" .rerenderPending="${this.rerenderPending}"
                                                          .selectedWidget="${this.selectedDashboard?.template?.widgets?.find(w => w.id == this.selectedWidgetId)}" .editMode="${this.editMode}"
                                                          .fullscreen="${this.fullscreen}" .readonly="${this._isReadonly()}"
                                                          @selected="${(event: CustomEvent) => { this.selectWidget(event.detail); }}"
                                                          @deselected="${() => { this.deselectWidget(); }}"
                                                          @dropped="${(event: CustomEvent) => { this.createWidget(event.detail as ORGridStackNode)}}"
                                                          @changed="${(event: CustomEvent) => { this.currentTemplate = event.detail.template; }}"
                                                          @rerenderfinished="${(event: CustomEvent) => { this.rerenderPending = false; }}"
                                    ></or-dashboard-preview>
                                ` : html`
                                    <div style="display: flex; justify-content: center; align-items: center; height: 100%;">
                                        <span>${i18next.t('noDashboardSelected')}</span>
                                    </div>
                                `}
                            </div>
                            ${when((this.selectedDashboard != null && this.editMode && !this._isReadonly() && this._hasEditAccess()), () => {
                                const selectedWidget = this.selectedDashboard?.template?.widgets?.find(w => w.id == this.selectedWidgetId);
                                return html`
                                    <div id="sidebar" class="hideMobile">
                                        ${this.selectedWidgetId != null ? html`
                                            <div class="settings-container">
                                                <div id="menu-header">
                                                    <div id="title-container">
                                                        <span id="title" title="${selectedWidget?.displayName}">${selectedWidget?.displayName}</span>
                                                    </div>
                                                    <div id="sidebar-widget-headeractions">
                                                        <or-mwc-input type="${InputType.BUTTON}" icon="delete" @or-mwc-input-changed="${() => {
                                                            showOkCancelDialog(i18next.t('areYouSure'), i18next.t('dashboard.deleteWidgetWarning'), i18next.t('delete')).then((ok: boolean) => {
                                                                if(ok) { this.deleteWidget(selectedWidget!); }
                                                            })
                                                        }}"></or-mwc-input>
                                                        <or-mwc-input type="${InputType.BUTTON}" icon="close" @or-mwc-input-changed="${() => { this.deselectWidget(); }}"></or-mwc-input>
                                                    </div>
                                                </div>
                                                <div id="content" class="hidescroll" style="flex: 1; overflow: hidden auto;">
                                                    <div style="position: relative;">
                                                        <or-dashboard-widgetsettings style="position: absolute;" .selectedWidget="${selectedWidget}" .realm="${this.realm}"
                                                                                     @delete="${(event: CustomEvent) => { this.deleteWidget(event.detail); }}"
                                                                                     @update="${(event: CustomEvent) => {
                                                                                         this.currentTemplate = Object.assign({}, this.selectedDashboard?.template);
                                                                                         if(event.detail.force) { this.rerenderPending = true; }}}"
                                                        ></or-dashboard-widgetsettings>
                                                    </div>
                                                </div>
                                            </div>
                                        ` : undefined}
                                        <div class="settings-container" style="${this.selectedWidgetId != null ? css`display: none` : null}">
                                            <div style="border-bottom: 1px solid ${unsafeCSS(DefaultColor5)};">
                                                <or-mwc-tabs .items="${this.tabItems}" noScroll @activated="${(event: CustomEvent) => { this.sidebarMenuIndex = event.detail.index; }}" style="pointer-events: ${this.selectedDashboard ? undefined : 'none'}"></or-mwc-tabs>
                                            </div>
                                            <div id="content" class="hidescroll" style="flex: 1; overflow: hidden auto;">
                                                <div style="position: relative;">
                                                    <or-dashboard-browser id="browser" style="position: absolute; ${this.sidebarMenuIndex != 0 ? css`display: none` : null}"></or-dashboard-browser>
                                                    <or-dashboard-boardsettings style="position: absolute; ${this.sidebarMenuIndex != 1 ? css`display: none` : null}" 
                                                                                .dashboard="${this.selectedDashboard}" .showPerms="${this.selectedDashboard?.ownerId == this.userId}" 
                                                                                @update="${(event: CustomEvent) => {
                                                                                    this.currentTemplate = Object.assign({}, this.selectedDashboard?.template);
                                                                                    if(event.detail.force) { this.rerenderPending = true; }}}"
                                                    ></or-dashboard-boardsettings>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                `
                            })}
                        </div>
                    </div>
                </div>
            </div>
        ` : html`
            <div id="container" style="justify-content: center; align-items: center;">
                ${this.isInitializing ? html`
                    <span>${i18next.t("loading")}.</span>
                ` : html`
                    <span>${i18next.t("errorOccurred")}.</span>
                `}
            </div>
        `
    }

    /* ======================== */

}

// Generating the Grid Item details like X and Y coordinates for GridStack to work.
export function generateGridItem(gridstackNode: ORGridStackNode, _displayName: string): DashboardGridItem {
    const randomId = (Math.random() + 1).toString(36).substring(2);
    const widgetType = widgetTypes.get(gridstackNode.widgetTypeId)!;
    return {
        id: randomId,
        x: gridstackNode.x,
        y: gridstackNode.y,
        w: 2,
        h: 2,
        minW: widgetType.MIN_COLUMN_WIDTH,
        minH: widgetType.MIN_COLUMN_WIDTH,
        minPixelW: widgetType.MIN_PIXEL_WIDTH,
        minPixelH: widgetType.MIN_PIXEL_HEIGHT,
        noResize: false,
        noMove: false,
        locked: false,
    }
}
