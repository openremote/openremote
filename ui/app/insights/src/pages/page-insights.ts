import {html, TemplateResult} from "lit";
import {customElement, query, state} from "lit/decorators.js";
import {AppStateKeyed, Page, PageProvider} from "@openremote/or-app";
import {EnhancedStore} from "@reduxjs/toolkit";
import "@openremote/or-dashboard-builder"
import {Dashboard} from "@openremote/model";
import manager from "@openremote/core";
import "@openremote/or-chart";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {i18next} from "@openremote/or-translate";
import {registerWidgetTypes} from "@openremote/or-dashboard-builder";
import "@openremote/or-components/or-loading-indicator";
import {when} from "lit/directives/when.js";
import {guard} from "lit/directives/guard.js";
import {style} from "../style";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { OrMwcDrawer } from "@openremote/or-mwc-components/or-mwc-drawer";
import { styleMap } from 'lit/directives/style-map.js';
import {DrawerConfig, getDashboardMenuTemplate} from "../components/dashboard-list";

export function pageInsightsProvider(store: EnhancedStore<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "insights",
        routes: [
            "insights",
            "insights/:id"
        ],
        pageCreator: () => {
            const page = new PageInsights(store);
            return page;
        }
    };
}

@customElement("page-insights")
export class PageInsights extends Page<AppStateKeyed> {

    @state()
    private dashboards?: Dashboard[];

    @state()
    private selectedDashboard?: Dashboard;

    @state()
    private activePromises: Promise<any>[] = [];

    @state()
    private _userId?: string;

    @state()
    private rerenderPending: boolean = false;

    @query("#drawer")
    private drawer: OrMwcDrawer;

    @query("#drawer-custom-scrim")
    private drawerScrim: HTMLElement;


    get name(): string {
        return "insights"
    }

    static get styles() {
        return [style];
    }

    constructor(props) {
        super(props);

        // Register user
        manager.rest.api.UserResource.getCurrent().then((response: any) => {
            this._userId = response.data.id;
        }).catch((ex) => {
            console.error(ex);
            showSnackbar(undefined, i18next.t('errorOccurred'));
        })

        // Register dashboard related utils
        registerWidgetTypes();
    }

    // Before updating, check whether there are dashboards loaded.
    // If not, fetch them from our backend
    shouldUpdate(changedProps): boolean | any {
        if(!this.dashboards && !this.isLoading()) {
            this.fetchAllDashboards();
        }
        return super.shouldUpdate(changedProps);
    }

    willUpdate(changedProps) {
        console.log(changedProps);
    }

    // If URL has an dashboard ID, select it immediately.
    stateChanged(state: AppStateKeyed): void {
        if(state.app.params && state.app.params.id) {
            this.selectDashboard(state.app.params.id);
        }
    }

    isLoading(): boolean {
        return this.activePromises.length > 0;
    }


    /* -------------------- */

    fetchAllDashboards() {
        const realm = manager.displayRealm;
        const promise = manager.rest.api.DashboardResource.getAllRealmDashboards(realm);
        this.activePromises.push(promise);
        promise.then((response) => {
            this.dashboards = (response.data as Dashboard[]);
        }).finally(() => {
            const index = this.activePromises.indexOf(promise, 0);
            if(index > -1) {
                this.activePromises.splice(index, 1);
                this.requestUpdate("activePromises");
            }
        })
    }

    selectDashboard(dashboardId?: string) {
        const dashboard = this.dashboards.find((dashboard) => dashboard.id == dashboardId);
        if(dashboard) {
            this.toggleDrawer().then(() => {
                this.selectedDashboard = dashboard;
                this.requestUpdate();
            });
        }
    }

    async toggleDrawer(): Promise<void> {
        if(this.drawer.open) {
            this.drawerScrim?.classList.add("drawer-scrim-fadeout");
            this.drawer.toggle();
            await new Promise(r => setTimeout(r, 250)); // one-liner for waiting until fadeout animation is done
        } else {
            this.drawer.toggle();
        }
    }

    protected render(): TemplateResult {
        console.log("Rendering page-insights!");
        const pageStyles = {
            display: 'flex',
            flexDirection: 'column',
            pointerEvents: this.drawer?.open ? 'none' : 'auto'
        }
        const drawerConfig: DrawerConfig = {
            enabled: true,
            opened: this.drawer?.open,
            onToggle: () => this.toggleDrawer().then(() => this.requestUpdate())
        }
        return html`
            ${getDashboardMenuTemplate(
                    this.dashboards,
                    this.selectedDashboard,
                    (ev) => this.selectDashboard(ev.detail[0].value),
                    this._userId,
                    manager.displayRealm,
                    this.isLoading(), 
                    drawerConfig
            )}
            <div style="flex: 1; ${styleMap(pageStyles)}">
                ${this.getDashboardHeaderTemplate(() => this.toggleDrawer().then(() => this.requestUpdate()))}
                <div style="flex: 1;">
                    ${guard([this.selectedDashboard, this.rerenderPending], () => html`
                        ${when(this.selectedDashboard, () => html`
                            <or-dashboard-preview style="background: transparent;" .rerenderPending="${this.rerenderPending}" 
                                                  .realm="${manager.displayRealm}" .template="${this.selectedDashboard.template}" .editMode="${false}" 
                                                  .fullscreen="${true}" .readonly="${true}"
                                                  @rerenderfinished="${() => this.rerenderPending = false}"
                            ></or-dashboard-preview>
                        `, () => html`
                            <div style="display: flex; height: 100%; justify-content: center; align-items: center;">
                                <span>${i18next.t('noDashboardSelected')}</span>
                            </div>
                        `)}
                    `)}
                </div>
            </div>
        `;
    }



    /* ------------------------------------------------------------ */


    protected getDashboardHeaderTemplate(onopen: (ev: Event) => void): TemplateResult {
        return html`
            <div id="fullscreen-header">
                <div id="fullscreen-header-wrapper">
                    <div>
                        <or-mwc-input type="${InputType.BUTTON}" icon="menu" @or-mwc-input-changed="${onopen}"></or-mwc-input>
                    </div>
                    <div id="fullscreen-header-title">
                        <span>${this.selectedDashboard?.displayName}</span>
                    </div>
                    <div>
                        <div>
                            <or-mwc-input id="refresh-btn" class="small-btn" .disabled="${(this.selectedDashboard == null)}" type="${InputType.BUTTON}" icon="refresh"
                                          @or-mwc-input-changed="${() => { this.rerenderPending = true; }}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </div>
            </div>
        `
    }
}
