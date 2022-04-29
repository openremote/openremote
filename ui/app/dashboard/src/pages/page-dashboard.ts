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


    get name(): string {
        return "dashboard"
    }

    fetchDashboard(dashboardId: string) {
        manager.rest.api.DashboardResource.get(dashboardId).then((dashboard) => {
            this.template = (dashboard.data as Dashboard).template;
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
            <or-dashboard-editor .editMode="${false}" .template="${this.template}"></or-dashboard-editor>
        `
    }

}
