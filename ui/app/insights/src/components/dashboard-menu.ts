import { html, css, LitElement, TemplateResult } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import "@openremote/or-mwc-components/or-mwc-drawer";
import {Dashboard} from "@openremote/model";
import {when} from "lit/directives/when.js";
import {i18next} from "@openremote/or-translate";
import { RealmAppConfig } from "@openremote/or-app";
import { OrMwcDrawer } from "@openremote/or-mwc-components/or-mwc-drawer";
import {style} from "../style"

//language=css
const styling = css`
    #list-container {
        display: flex;
        flex-direction: column;
        gap: 16px;
    }
`
export interface DrawerConfig {
    enabled: boolean,
    showHeader: boolean
}

@customElement("dashboard-menu")
export class DashboardMenu extends LitElement {

    @property()
    protected readonly dashboards!: Dashboard[];

    @property() // id of the selected dashboard
    protected readonly selectedId?: string;

    @property()
    protected readonly userId!: string;

    @property()
    protected readonly realmName?: string;

    @property()
    protected readonly realmConfig?: RealmAppConfig;

    @property()
    protected drawerConfig?: DrawerConfig;

    @property()
    protected readonly loading: boolean = false;

    @query("#drawer")
    protected drawer: OrMwcDrawer;

    @query("#drawer-custom-scrim")
    protected drawerScrim: HTMLElement;

    static get styles() {
        return [style, styling];
    }

    // Set defaults if not set yet
    firstUpdated() {
        if(!this.drawerConfig) {
            this.drawerConfig = {
                enabled: true,
                showHeader: true
            }
        }
    }

    /* ------------------------------------------ */

    // PUBLIC METHODS FOR EXTERNAL CONTROL

    // For toggling drawer elsewhere, such as page-insights.ts
    // Returned value is the new status of the Drawer
    public async toggleDrawer(state?: boolean): Promise<boolean> {
        if(state && state == this.drawer.open) {
            return this.drawer.open;
        }
        if(this.drawer.open) {
            this.drawerScrim?.classList.add("drawer-scrim-fadeout");
            this.drawer.toggle();
            await new Promise(r => setTimeout(r, 250)); // one-liner for waiting until fadeout animation is done
        } else {
            this.drawer.toggle();
        }
        this.requestUpdate(); // to reload the component, which removes the drawerScrim we added earlier from the DOM
        this.dispatchEvent(new CustomEvent('toggle', { detail: { value: this.drawer.open }}))
        return this.drawer.open;
    }

    public isDrawerOpen(): boolean {
        return this.drawer?.open;
    }



    /* -------------------------------------------------------- */

    protected selectDashboard(id: string) {
        this.dispatchEvent(new CustomEvent('change', { detail: { value: id }}));
    }


    protected render() {
        let headerTemplate;
        if(this.drawerConfig?.showHeader) {
            // header template with inline styling; limitation of injecting html like this.
            headerTemplate = html`
                <div style="display: flex; padding: 13px 16px; border-bottom: 1px solid #E0E0E0; align-items: center; gap: 16px;">
                    <img id="logo-mobile" width="34" height="34" src="${this.realmConfig?.logoMobile}" />
                    <span style="font-size: 18px; font-weight: bold;">${this.realmName}</span>
                </div>
            `;
        }
        return html`
            ${when(this.drawerConfig?.enabled, () => html`
                <or-mwc-drawer id="drawer" .header="${headerTemplate}" .dismissible="${true}">
                    ${getDashboardListTemplate(this.dashboards, this.selectedId, (id: string) => this.selectDashboard(id), this.userId, this.loading)}
                </or-mwc-drawer>
                ${when(this.isDrawerOpen(), () => html`
                    <div id="drawer-custom-scrim" @click="${() => this.toggleDrawer(false)}"></div>
                `)}
            `, () => html`
                ${getDashboardListTemplate(this.dashboards, this.selectedId, (id: string) => this.selectDashboard(id), this.userId, this.loading)}
            `)}
        `
    }
}


/* --------------------------------------------------------------- */

// TEMPLATE RENDERING FUNCTIONS
// Wrote standalone from the component, to allow external use if needed.

export function getDashboardListTemplate(dashboards: Dashboard[], selectedId: string, onSelect: (id: string) => void, userId: string, loading: boolean = false): TemplateResult {
    return html`
        ${when(loading, () => html`
            <or-loading-indicator></or-loading-indicator>
        `, () => {
            const menuItems = getDashboardMenuItems(dashboards, userId);
            return html`
                <div style="padding: 16px 0;">
                    <div id="list-container">
                        ${when(menuItems[0] == undefined, () => html`
                            <span style="width: 100%; text-align: center;">${i18next.t('noDashboardFound')}</span>
                        `, () => html`
                            ${when(menuItems[0].length > 0, () => html`
                                <div>
                                    <span style="margin-left: 16px;">${i18next.t('dashboard.myDashboards')}</span>
                                    <or-mwc-list .listItems="${menuItems[0]}" .values="${selectedId}"
                                                 @or-mwc-list-changed="${(ev: CustomEvent) => onSelect(ev.detail[0].value)}"
                                    ></or-mwc-list>
                                </div>
                            `)}
                            ${when(menuItems[1].length > 0, () => html`
                                <div>
                                    <span style="margin-left: 16px;">${i18next.t('dashboard.createdByOthers')}</span>
                                    <or-mwc-list .listItems="${menuItems[1]}" .values="${selectedId}"
                                                 @or-mwc-list-changed="${(ev: CustomEvent) => onSelect(ev.detail[0].value)}"
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

export function getDashboardMenuItems(dashboards: Dashboard[], userId: string): ListItem[][] {
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
