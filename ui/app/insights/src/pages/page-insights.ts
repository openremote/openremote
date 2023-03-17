import {html, TemplateResult} from "lit";
import {customElement, state} from "lit/decorators.js";
import {AppStateKeyed, Page, PageProvider} from "@openremote/or-app";
import {EnhancedStore} from "@reduxjs/toolkit";
import "@openremote/or-dashboard-builder"
import {Dashboard } from "@openremote/model";
import manager from "@openremote/core";
import "@openremote/or-chart";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { i18next } from "@openremote/or-translate";
import {when} from 'lit/directives/when.js';
import { registerWidgetTypes } from "@openremote/or-dashboard-builder";
import {style} from "../style";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";

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
    protected _fullscreen: boolean = true;

    @state()
    private rerenderPending: boolean = false;

    @state()
    private showDashboardTree: boolean = true;


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

    shouldUpdate(changedProps): boolean | any {
        console.log(changedProps);
        if(!this.dashboards && !this.isLoading()) {
            this.fetchAllDashboards();
        }
        return super.shouldUpdate(changedProps);
    }

    /* -------------------- */

    isLoading(): boolean {
        return this.activePromises.length > 0;
    }

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
        this.selectedDashboard = this.dashboards.find((dashboard) => dashboard.id == dashboardId);
    }


    stateChanged(state: AppStateKeyed): void {
        if(state.app.params && state.app.params.id) {
            this.selectDashboard(state.app.params.id);
        }
    }

    protected render(): TemplateResult {
        return html`
            <div style="flex: 1;">
                <div style="display: flex; flex-direction: row; height: 100%;">
                    ${when(this.showDashboardTree, () => html`
                        <div class="${this.selectedDashboard ? 'hideMobile' : 'fullWidthOnMobile'}" style="flex: 0 0 300px; display: flex; justify-content: center; align-items: center;">
                            ${when(this.dashboards && !this.isLoading(), () => html`
                            <or-dashboard-tree id="tree" .realm="${manager.displayRealm}"
                                               .selected="${this.selectedDashboard}" .dashboards="${this.dashboards}" .showControls="${true}" .userId="${this._userId}" .readonly="${true}"
                                               @select="${(event: CustomEvent) => { this.selectDashboard((event.detail as Dashboard)?.id); }}"
                            ></or-dashboard-tree>
                        `, () => html`
                            <div>
                                <span>Loading...</span>
                            </div>
                        `)}
                        </div>
                    `)}
                    <div class="${!this.selectedDashboard ? 'hideMobile' : ''}" style="flex: 1; display: flex; justify-content: center; align-items: center; flex-direction: column;">
                        ${when(this.selectedDashboard != null && !this.isLoading(), () => html`
                            <div style="width: 100%; flex: 1; display: flex; flex-direction: column;">
                                <div id="fullscreen-header">
                                    <div id="fullscreen-header-wrapper">
                                        <div id="fullscreen-header-title" style="display: flex; align-items: center;">
                                            <or-icon class="showMobile" style="margin-right: 10px;" icon="chevron-left" @click="${() => { this.selectedDashboard = undefined; }}"></or-icon>
                                            <or-icon class="hideMobile" style="margin-right: 10px;" icon="menu" @click="${() => { this.showDashboardTree = !this.showDashboardTree; }}"></or-icon>
                                            <span>${this.selectedDashboard?.displayName}</span>
                                        </div>
                                        <div id="fullscreen-header-actions">
                                            <div id="fullscreen-header-actions-content">
                                                <or-mwc-input id="refresh-btn" class="small-btn" .disabled="${(this.selectedDashboard == null)}" type="${InputType.BUTTON}" icon="refresh" @or-mwc-input-changed="${() => { this.rerenderPending = true; }}"></or-mwc-input>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div style="flex: 1;">
                                    <or-dashboard-preview style="background: transparent;" .rerenderPending="${this.rerenderPending}" 
                                                          .realm="${manager.displayRealm}" .template="${this.selectedDashboard.template}" .editMode="${false}" 
                                                          .fullscreen="${true}" .readonly="${true}"
                                                          @rerenderfinished="${() => { this.rerenderPending = false; }}"
                                    ></or-dashboard-preview>
                                </div>
                            </div>
                        `, () => {
                            if (this.isLoading()) {
                                return html`
                                    <or-loading-indicator .overlay="${true}"></or-loading-indicator>
                                `
                            } else {
                                return html`<span>No dashboard selected.</span>`
                            }
                        })}
                    </div>
                </div>
            </div>
        `
    }

}
