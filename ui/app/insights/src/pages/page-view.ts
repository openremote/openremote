import {html, TemplateResult} from "lit";
import {customElement, query, property, state} from "lit/decorators.js";
import {AppStateKeyed, DefaultMobileLogo, Page, PageProvider, RealmAppConfig, router, updateRealm} from "@openremote/or-app";
import {EnhancedStore} from "@reduxjs/toolkit";
import {when} from "lit/directives/when.js";
import {styleMap} from 'lit/directives/style-map.js';
import {guard} from "lit/directives/guard.js";
import {Dashboard, Realm} from "@openremote/model";
import {registerWidgetTypes} from "@openremote/or-dashboard-builder";
import manager from "@openremote/core";
import {DashboardMenu} from "../components/dashboard-menu";
import {InputType} from "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate";
import {style} from "../style";
import {isAxiosError} from "@openremote/rest";
import "../components/dashboard-menu";

export function pageViewProvider(store: EnhancedStore<AppStateKeyed>, realmConfigs: {[p: string]: RealmAppConfig}): PageProvider<AppStateKeyed> {
    return {
        name: "view",
        routes: [
            "view",
            "view/:id",
            "view/:id/:showMenu"
        ],
        pageCreator: () => {
            const page = new PageView(store);
            page.realmConfigs = realmConfigs;
            return page;
        }
    };
}

@customElement("page-view")
export class PageView extends Page<AppStateKeyed> {

    get name(): string {
        return "view"
    }

    @property() // configs of all available realms; contains details such as images
    public realmConfigs?: {[p: string]: RealmAppConfig};

    @property() // setting this to true removes the menu, and only fetches that specific dashboard to avoid clutter
    protected viewDashboardOnly: boolean = false;

    @state()
    protected _loadedDashboards?: Dashboard[];

    @state()
    protected _loadedRealms: Realm[] = [];

    @state() // ID of the selected dashboard
    protected _selectedId?: string;

    @state() // ID of the user. Will be undefined if not logged in.
    protected _userId?: string;

    @state()
    protected _realm: string = manager.displayRealm;

    @state() // list of active fetches, to make sure no duplicate fetch calls take place, and use for 'isLoading' checks
    protected _activePromises: Map<string, Promise<any>> = new Map<string, Promise<any>>();

    @state() // boolean that is put to true if a fresh render should take place. Similar to the one used in or-dashboard-builder.
    private rerenderPending: boolean = false;

    @query('#dashboard-menu')
    private dashboardMenu: DashboardMenu;

    static get styles() {
        return [style];
    }


    /* -------------------- */

    constructor(props) {
        super(props);

        // Register user
        const userPromise = manager.rest.api.UserResource.getCurrent().then((response: any) => { this._userId = response.data.id; });
        this.registerPromise('user', userPromise, true, true);

        // Load available realms
        const realmPromise = manager.rest.api.RealmResource.getAccessible().then((response: any) => { this._loadedRealms = response.data; });
        this.registerPromise('realm', realmPromise, true, true);

        // Register dashboard related utils
        registerWidgetTypes();
    }


    /* ------------------------ */

    // STATE RELATED FUNCTIONS

    // When state / url properties change
    stateChanged(state: AppStateKeyed): void {
        this._realm = state.app.realm;
        this.viewDashboardOnly = (state.app.params?.showMenu != undefined ? (state.app.params?.showMenu.toLowerCase() != 'true') : false);
        this.selectDashboard(state.app.params?.id, false);
    }

    // On every update, but before rendering anything
    willUpdate(changedProps: Map<string, any>) {

        // Update url if properties have changed that are present in the URL
        if(changedProps.has("_selectedId") || changedProps.has("viewDashboardOnly")) {
            this._updateRoute(this._selectedId, !this.viewDashboardOnly, false);
        }

        // Clear loadedDashboards when people switch between realms or viewing modes.
        // For example, if 'view this dashboard only' is active, we only fetch that specific dashboard.
        // When switched to 'view all dashboards', or the other way around, we should fetch all dashboard(s) of that realm again.
        if(changedProps.has("_realm") || changedProps.has("viewDashboardOnly")) {
            this._loadedDashboards = undefined;
        }

        // Fetch dashboard(s) if none exist, and no related fetch calls are active.
        if(this._loadedDashboards == undefined && !this.getPromise('dashboard/all') && !this.getPromise('dashboard/' + this._selectedId)) {

            // When visiting '/{id}/false', only fetch dashboard with that ID.
            if(this.viewDashboardOnly && this._selectedId) {
                this.fetchDashboard(this._selectedId).then((dashboard: Dashboard) => {
                    this._loadedDashboards = (dashboard ? [dashboard] : undefined);
                })
            }
            // When visiting '/{id}/true', fetch dashboard with that ID first...
            else if(!this.viewDashboardOnly && this._selectedId) {
                this.fetchDashboard(this._selectedId).then((dashboard: Dashboard) => {
                    this._loadedDashboards = dashboard ? [dashboard] : undefined;

                    // If dashboard does not exist, but menu should be shown, then just deselect the dashboard.
                    if(dashboard === undefined) {
                        this._selectedId = undefined; // aka redirect to '/'
                    }
                    // If dashboard fetched with success, fetch all other dashboards of that realm as well.
                    else {
                        this.fetchAllDashboards().then((dashboards) => {
                            this._loadedDashboards = dashboards;
                        });
                    }
                })
            }
            // When visiting '/'
            else {
                this.fetchAllDashboards().then((dashboards) => {
                    if(!manager.authenticated && dashboards?.length === 0) {
                        manager.login(); // if not logged in, auto login if no public dashboards are available
                    } else {
                        this._loadedDashboards = dashboards;
                        if(dashboards.length === 1) {
                            this._selectedId = this._loadedDashboards[0].id;
                        }
                    }
                })
            }
        }
    }


    /* -------------------------------- */

    // FETCH RELATED FUNCTIONS

    protected async fetchAllDashboards(realmName?: string): Promise<Dashboard[]> {
        if(!realmName) {
            realmName = manager.displayRealm;
        }
        let promise = this.getPromise('dashboard/all');
        if(promise === undefined) {
            promise = this.registerPromise('dashboard/all', manager.rest.api.DashboardResource.getAllRealmDashboards(realmName), true, false);
        }
        try {
            const response = await promise;
            return response.data;
        } catch (ex) {
            console.error(ex);
            return [];
        }
    }

    protected async fetchDashboard(id: string, loginRedirect: boolean = true): Promise<Dashboard | undefined> {
        let promise = this.getPromise('dashboard/' + id);
        if(promise == undefined) {
            promise = this.registerPromise(('dashboard/' + id), manager.rest.api.DashboardResource.get(this._realm, id), true, false);
        }
        try {
            const response = await promise;
            return response.data;
        } catch (ex) {
            if(isAxiosError(ex)) {
                if(ex.response.status === 404 && manager.isSuperUser()) {
                    return undefined;
                }
                if(!manager.authenticated && ex.response.status === 403 && loginRedirect) {
                    manager.login();
                }
            }
            return undefined;
        }
    }


    /* ---------------------------- */

    protected render(): TemplateResult {
        const realmConfig = this.realmConfigs ? this.realmConfigs[this._realm] : undefined;
        const selected: Dashboard | undefined = this._loadedDashboards?.find((d) => d.id === this._selectedId);
        let logoMobile = realmConfig?.logoMobile;
        if(logoMobile == undefined) {
            logoMobile = DefaultMobileLogo;
        }
        const pageStyles = {
            display: 'flex',
            flexDirection: 'column',
            pointerEvents: this.dashboardMenu?.isDrawerOpen() ? 'none' : 'auto'
        }
        return html`
            ${when(!this.viewDashboardOnly, () => {
                const realms = this._loadedRealms?.map((r) => ({ name: r.name, displayName: r.displayName }));
                return html`
                    <dashboard-menu id="dashboard-menu" .dashboards="${this._loadedDashboards}" .realms="${realms}" .realm="${this._realm}"
                                    .selectedId="${this._selectedId}" .userId="${this._userId}" .loading="${this.isLoading()}" .logoMobileSrc="${logoMobile}"
                                    @realm="${(ev: CustomEvent) => this.changeRealm(ev.detail.value)}"
                                    @change="${(ev: CustomEvent) => this.selectDashboard((ev.detail.value as Dashboard).id, true)}"
                                    @login="${() => { manager.login(); }}"
                                    @logout="${() => { this._updateRoute(); manager.logout(); }}"
                    ></dashboard-menu>
                `
            })}
            <div style="flex: 1; ${styleMap(pageStyles)}">
                ${getDashboardHeaderTemplate(!this.viewDashboardOnly, selected, () => this.dashboardMenu?.toggleDrawer(), () => this.rerenderPending = true)}
                <div style="flex: 1;">
                    ${guard([this._selectedId, this._loadedDashboards, this.rerenderPending], () => html`
                        ${when(this._selectedId && selected !== undefined, () => html`
                            <or-dashboard-preview style="background: transparent;" .rerenderPending="${this.rerenderPending}" 
                                                  .realm="${manager.displayRealm}" .template="${selected?.template}" .editMode="${false}" 
                                                  .fullscreen="${true}" .readonly="${true}"
                                                  @rerenderfinished="${() => this.rerenderPending = false}"
                            ></or-dashboard-preview>
                        `, () => html`
                            <div id="dashboard-error-text">
                                <span>${this.getErrorMsg(true)}</span>
                            </div>
                        `)}
                    `)}
                </div>
            </div>
        `
    }

    getErrorMsg(translate: boolean = true): string {
        if(this.viewDashboardOnly && this._loadedDashboards?.length === 0) {
            return (translate ? i18next.t('dashboardNotFound') : 'dashboardNotFound');
        } else if(!this.viewDashboardOnly) {
            return (translate ? i18next.t('noDashboardSelected-mobile') : 'noDashboardSelected-mobile')
        }
        return (translate ? i18next.t('errorOccurred') : 'errorOccurred')
    }

    selectDashboard(id: string, closeDrawer: boolean = true) {
        if (id) {
            if (this.dashboardMenu && closeDrawer) {
                this.dashboardMenu.toggleDrawer(false).then(() => {
                    this._selectedId = id;
                });
            } else {
                this._selectedId = id;
            }
        }
    }

    // Method to switch realms.
    // If dashboardId is not set, it will navigate to '/' since the current selectedId is not present in the new realm.
    changeRealm(realm: string, dashboardId?: string) {
        this._selectedId = dashboardId;
        this.updateComplete.then(() => {
            this._store.dispatch(updateRealm(realm));
        })
    }




    /* ---------------------------- */

    // PROMISE HANDLING UTILITY FUNCTIONS

    // It uses _activePromises to keep track of active processes / fetches.
    // This list is used for checking 'loading' state, or do an "await Promise.all()" to wait until everything is loaded.

    isLoading(): boolean {
        return (this._activePromises.size > 0);
    }

    getPromise(topic: string) {
        return this._activePromises.get(topic);
    }

    registerPromise(topic: string, promise: Promise<any>, doUpdate: boolean = true, updateOnComplete: boolean = true): Promise<any> {
        if(this._activePromises.has(topic)) {
            promise = this._activePromises.get(topic);
        } else {
            this._activePromises.set(topic, promise);
        }
        promise.finally(() => { this.completePromise(topic, updateOnComplete); });
        if(doUpdate) { this.requestUpdate("_activePromises"); }
        return promise;
    }

    completePromise(topic: string, doUpdate: boolean = true) {
        this._activePromises.delete(topic);
        if(doUpdate) { this.requestUpdate("_activePromises"); }
    }


    // Util method for updating URL
    protected _updateRoute(id?: string, showMenu?: boolean, silent: boolean = true) {
        let path = "view";
        if(id) {
            path += "/" + id + (showMenu != undefined ? ('/' + showMenu) : undefined)
        }
        router.navigate(path, {
            callHooks: !silent,
            callHandler: !silent
        });
    }


}

function getDashboardHeaderTemplate(showMenu = true, selected?: Dashboard, onopen?: (ev: Event) => void, onrefresh?: (ev: Event) => void): TemplateResult {
    return html`
        <div id="fullscreen-header">
            <div id="fullscreen-header-wrapper">
                <div>
                    ${when(showMenu, () => html`
                        <or-mwc-input type="${InputType.BUTTON}" icon="menu" @or-mwc-input-changed="${onopen}"></or-mwc-input>
                    `)}
                </div>
                <div id="fullscreen-header-title">
                    <span>${selected?.displayName}</span>
                </div>
                <div>
                    <div>
                        <or-mwc-input id="refresh-btn" class="small-btn" .disabled="${(selected == null)}" type="${InputType.BUTTON}" icon="refresh"
                                      @or-mwc-input-changed="${onrefresh}"
                        ></or-mwc-input>
                    </div>
                </div>
            </div>
        </div>
    `
}
