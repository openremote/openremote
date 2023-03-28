import {html, TemplateResult} from "lit";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import "@openremote/or-mwc-components/or-mwc-drawer";
import {Dashboard} from "@openremote/model";
import {when} from "lit/directives/when.js";
import {i18next} from "@openremote/or-translate";

export interface DrawerConfig {
    enabled: boolean,
    opened: boolean,
    onToggle: () => void
}

export function getDashboardMenuTemplate(dashboards: Dashboard[], selected: Dashboard, onChange: (CustomEvent) => void, userId: string, realm: string, loading: boolean = false, drawerConfig?: DrawerConfig): TemplateResult {
    const headerTemplate = html`
        <div style="padding: 16px; border-bottom: 1px solid #E0E0E0;">
            <h1>${realm}</h1>
        </div>
    `;
    return html`
        ${when(drawerConfig.enabled, () => html`
            <or-mwc-drawer id="drawer" .header="${headerTemplate}" .dismissible="${true}">
                ${getDashboardListTemplate(dashboards, selected, onChange, userId, loading)}
            </or-mwc-drawer>
            ${when(drawerConfig.opened, () => html`
                <div id="drawer-custom-scrim" @click="${drawerConfig.onToggle}"></div>
            `)}
        `, () => html`
            ${getDashboardListTemplate(dashboards, selected, onChange, userId, loading)}
        `)}
    `
}


/* --------------------------------------------------------------- */

function getDashboardListTemplate(dashboards: Dashboard[], selected: Dashboard, onChange: (CustomEvent) => void, userId: string, loading: boolean = false): TemplateResult {
    return html`
        ${when(loading, () => html`
            <or-loading-indicator></or-loading-indicator>
        `, () => {
            const menuItems = getDashboardMenuItems(dashboards, userId);
            return html`
                <div style="padding: 32px 16px;">
                    <div style="display: flex; flex-direction: column; gap: 16px;">
                        ${when(menuItems[0] == undefined, () => html`
                            <span style="width: 100%; text-align: center;">${i18next.t('noDashboardFound')}</span>
                        `, () => html`
                            ${when(menuItems[0].length > 0, () => html`
                                <div>
                                    <span>${i18next.t('myDashboards')}</span>
                                    <or-mwc-list .listItems="${menuItems[0]}" .values="${selected?.id}"
                                                 @or-mwc-list-changed="${onChange}"
                                    ></or-mwc-list>
                                </div>
                            `)}
                            ${when(menuItems[1].length > 0, () => html`
                                <div>
                                    <span>${i18next.t('createdByOthers')}</span>
                                    <or-mwc-list .listItems="${menuItems[1]}" .values="${selected?.id}"
                                                 @or-mwc-list-changed="${onChange}"
                                    ></or-mwc-list>
                                </div>
                            `)}
                        `)}
                    </div>
                </div>
            `
        })}
    `
}

function getDashboardMenuItems(dashboards: Dashboard[], userId: string): ListItem[][] {
    const dashboardItems: ListItem[][] = [];
    if(dashboards.length > 0) {
        if(userId) {
            const myDashboards: Dashboard[] = [];
            const otherDashboards: Dashboard[] = [];
            dashboards?.forEach((d) => {
                (d.ownerId == userId) ? myDashboards.push(d) : otherDashboards.push(d);
            });
            [myDashboards, otherDashboards].forEach((array, index) => {
                dashboardItems[index] = [];
                array.sort((a, b) => a.displayName ? a.displayName.localeCompare(b.displayName!) : 0).forEach((d) => {
                    dashboardItems[index].push({ icon: "view-dashboard", text: d.displayName, value: d.id })
                })
            })
        }
    }
    return dashboardItems;
}
