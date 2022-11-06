import {css, html, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, query, state} from "lit/decorators.js";
import {AppStateKeyed, Page, PageProvider, router} from "@openremote/or-app";
import {EnhancedStore} from "@reduxjs/toolkit";
import "@openremote/or-dashboard-builder"
import { Dashboard, DashboardTemplate } from "@openremote/model";
import manager from "@openremote/core";
import "@openremote/or-chart";

export function pageDashboardProvider(store: EnhancedStore<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "dashboard",
        routes: [
            "dashboard/:id"
        ],
        pageCreator: () => {
            const page = new PageDashboard(store);
            return page;
        }
    };
}

@customElement("page-dashboard")
export class PageDashboard extends Page<AppStateKeyed> {

    @state()
    private template: DashboardTemplate;

    @state()
    private dashboardId: string;

    @state()
    private isLoading: boolean = true;


    get name(): string {
        return "dashboard"
    }

    static get styles() {
        // language=CSS
        return [css`
            .text {
                height: 100%;
                width: 100%;
                display: flex;
                justify-content: center;
                align-items: center;
            }
        `]
    }

    fetchDashboard(dashboardId: string) {
        manager.rest.api.DashboardResource.get(dashboardId).then((dashboard) => {
            this.template = (dashboard.data as Dashboard).template;
            this.isLoading = false;
        });
    }


    stateChanged(state: AppStateKeyed): void {
        if(state.app.params && state.app.params.id) {
            this.dashboardId = state.app.params.id;
            this.fetchDashboard(state.app.params.id);
        }
    }

    protected render(): TemplateResult {
        return html`
            ${this.template != undefined ? html`
                <or-dashboard-preview .template="${this.template}" .realm="${manager.displayRealm}"
                                     .editMode="${false}" .readonly="${true}" .fullscreen="${true}"
                ></or-dashboard-preview>
            ` : (this.isLoading ? html`
                <span class="text">Loading..</span>
            ` : html`
                <span class="text">No dashboard found..</span>
            `)}
        `
    }

}
