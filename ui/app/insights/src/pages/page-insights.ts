import {html, TemplateResult} from "lit";
import {customElement, query, property, state} from "lit/decorators.js";
import {AppStateKeyed, Page, router, PageProvider, RealmAppConfig} from "@openremote/or-app";
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
import { styleMap } from 'lit/directives/style-map.js';
import "../components/dashboard-menu";
import {DashboardMenu} from "../components/dashboard-menu";

export function pageInsightsProvider(store: EnhancedStore<AppStateKeyed>, realmConfigs: {[p: string]: RealmAppConfig}): PageProvider<AppStateKeyed> {
    return {
        name: "insights",
        routes: [
            "",
            ":id"
        ],
        pageCreator: () => {
            const page = new PageInsights(store);
            page.realmConfig = realmConfigs[manager.displayRealm];
            return page;
        }
    };
}

@customElement("page-insights")
export class PageInsights extends Page<AppStateKeyed> {

    @property()
    public realmConfig?: RealmAppConfig;

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

    @query('#dashboard-menu')
    private dashboardMenu: DashboardMenu;


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

    // If URL has a dashboard ID when loading, select it immediately.
    stateChanged(state: AppStateKeyed): void {
        if(state.app.params.id != this.selectedDashboard?.id) {
            this.updateComplete.then(async () => {
                await Promise.all(this.activePromises); // await for dashboard fetches
                if(this.dashboards) {
                    this.selectDashboard(state.app.params.id, false);
                } else {
                    console.error("No dashboards found!");
                }
            });
        }
    }

    // Update URL if another dashboard is selected.
    updated(changedProperties: Map<string, any>) {
        if(changedProperties.has("_dashboardId") || changedProperties.has("selectedDashboard")) {
            this._updateRoute();
        }
    }

    // Util method for updating URL
    protected _updateRoute(silent: boolean = true) {
        if(this.selectedDashboard) {
            router.navigate(this.selectedDashboard.id, {
                callHooks: !silent,
                callHandler: !silent
            });
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

    protected selectDashboard(dashboardId?: string, toggleDrawer: boolean = true) {
        const dashboard = this.dashboards.find((dashboard) => dashboard.id == dashboardId);
        if (dashboard) {
            if (toggleDrawer) {
                this.dashboardMenu?.toggleDrawer(false).then(() => {
                    this.selectedDashboard = dashboard;
                    this.requestUpdate();
                });
            } else {
                this.selectedDashboard = dashboard;
            }
        }
    }

    protected render(): TemplateResult {
        console.log("Rendering page-insights!");
        const pageStyles = {
            display: 'flex',
            flexDirection: 'column',
            pointerEvents: this.dashboardMenu?.isDrawerOpen() ? 'none' : 'auto'
        }
        return html`
            <dashboard-menu id="dashboard-menu" .dashboards="${this.dashboards}" .selectedId="${this.selectedDashboard?.id}" 
                            .realm="${manager.displayRealm}" .realmConfig="${this.realmConfig}" .userId="${this._userId}" .loading="${this.isLoading()}"
                            @change="${(ev: CustomEvent) => this.selectDashboard(ev.detail.value)}"
            ></dashboard-menu>
            <div style="flex: 1; ${styleMap(pageStyles)}">
                ${this.getDashboardHeaderTemplate(() => this.dashboardMenu?.toggleDrawer())}
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
