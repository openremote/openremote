import { html, css, LitElement, TemplateResult, unsafeCSS } from "lit";
import { customElement, property, query, state } from "lit/decorators.js";
import "@openremote/or-mwc-components/or-mwc-drawer";
import {when} from "lit/directives/when.js";
import { Dashboard } from "@openremote/model";
import { i18next } from "@openremote/or-translate";
import { ListItem } from "@openremote/or-mwc-components/or-mwc-list";
import { OrMwcDrawer } from "@openremote/or-mwc-components/or-mwc-drawer";
import {style} from "../style";
import { OrMwcDialog, showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
import {DefaultColor5} from "@openremote/core";

// language=css
const styling = css`
    #list-container {
        display: flex;
        flex-direction: column;
        gap: 16px;
    }
    #drawer-wrapper {
        height: 100%;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
    }
    #drawer-actions-container {
        border-top: 1px solid var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
        display: flex;
        flex-direction: column;
        gap: 8px;
    }
`

@customElement("dashboard-menu")
export class DashboardMenu extends LitElement {

    @property() // list of dashboards: if set to 'undefined', it will pull new dashboards automatically.
    protected readonly dashboards?: Dashboard[];

    @property() // Custom realm object that only requires name and displayName.
    protected readonly realms?: { name: string, displayName: string }[];

    @property() // id of the selected dashboard
    protected readonly selectedId?: string;

    @property() // id of the current user
    protected readonly userId?: string;

    @property()
    protected readonly realm?: string;

    @property()
    protected readonly loading: boolean = false;

    @property()
    protected readonly logoMobileSrc?: string;

    @state()
    protected selectedActions: string[] = [];

    @query("#drawer")
    protected drawer: OrMwcDrawer;

    @query("#drawer-custom-scrim")
    protected drawerScrim: HTMLElement;

    static get styles() {
        return [style, styling];
    }

    /* ------------------------------------------ */

    // PUBLIC METHODS FOR EXTERNAL CONTROL

    // For toggling drawer elsewhere, such as page-insights.ts
    // Returned value is the new status of the Drawer
    public async toggleDrawer(state?: boolean): Promise<boolean> {
        if(state != undefined && state == this.drawer.open) {
            return this.drawer.open;
        }
        if(this.drawer?.open) {
            this.drawerScrim?.classList.add("drawer-scrim-fadeout");
            this.drawer.toggle();
            await new Promise(r => setTimeout(r, 250)); // one-liner for waiting until fadeout animation is done
        } else {
            this.drawer?.toggle();
        }
        this.requestUpdate(); // to reload the component, since we need to remove the drawerScrim we added earlier
        this.dispatchEvent(new CustomEvent('toggle', { detail: { value: this.drawer.open }}))
        return this.drawer?.open;
    }

    public isDrawerOpen(): boolean {
        return this.drawer?.open;
    }


    /* ---------------------------- */

    // METHODS OF THE UI, THAT MOSTLY DISPATCH EVENTS TO PARENT COMPONENTS

    protected selectDashboard(id: string) {
        const dashboard = this.dashboards.find((d) => d.id == id);
        this.dispatchEvent(new CustomEvent('change', { detail: { value: dashboard }}));
    }

    protected changeRealm(realm: string) {
        if(realm != this.realm) {
            this.dispatchEvent(new CustomEvent("realm", { detail: { value: realm }}));
        }
    }

    protected logout() {
        this.dispatchEvent(new CustomEvent("logout"));
    }

    protected login() {
        this.dispatchEvent(new CustomEvent("login"))
    }

    protected promptRealmSwitch() {
        showDialog(new OrMwcDialog()
            .setHeading(i18next.t('changeRealm'))
            .setDismissAction(null)
            .setActions(Object.entries(this.realms).map(([key, value]) => ({
                content: html`<span>${value.displayName}</span>`,
                actionName: key,
                action: () => {
                    this.changeRealm(value.name);
                }
            })))
        );
    }


    /* ------------------------------------------- */

    protected render(): TemplateResult {
        const curRealm = this.realms?.find((r) => r.name == this.realm);
        const headerTemplate = html`
            <div style="display: flex; align-items: center; gap: 16px; padding: 13px 16px; border-bottom: 1px solid var(--or-app-color5, ${unsafeCSS(DefaultColor5)});">
                ${this.loading ? html`
                    <div style="height: 34px;">
                        <!-- Placeholder height to prevent changes during the realm fetch -->
                    </div>
                ` : html`
                    <img id="logo-mobile" width="34" height="34" alt="${curRealm.displayName} Logo" src="${this.logoMobileSrc}" />
                    <span style="font-size: 18px; font-weight: bold;">${curRealm.displayName}</span>
                `}
            </div>
        `;
        const actionItems: ListItem[] = [];
        if(this.realms !== undefined && this.realms.length > 1) {
            actionItems.push({ icon: 'domain', text: i18next.t('changeRealm'), value: 'realm' })
        }
        if(this.userId !== undefined) {
            actionItems.push({ icon: 'logout', text: i18next.t('logout'), value: 'logout' });
        } else {
            actionItems.push({ icon: 'login', text: i18next.t('login'), value: 'login' })
        }

        return html`
            <or-mwc-drawer id="drawer" .header="${headerTemplate}" .dismissible="${true}">
                <div id="drawer-wrapper">
                    <div>
                        ${getDashboardListTemplate(this.dashboards, this.selectedId, (id: string) => this.selectDashboard(id), this.userId, this.loading)}
                    </div>
                    <div id="drawer-actions-container">
                        <or-mwc-list .listItems="${actionItems}" .values="${this.selectedActions}" @or-mwc-list-changed="${(ev: CustomEvent) => {
                            switch (ev.detail[0].value) {
                                case 'realm': {
                                    this.promptRealmSwitch(); break;
                                } case 'logout': {
                                    this.logout(); break;
                                } case 'login': {
                                    this.login(); break;
                                }
                            }
                            // Hacky way to remove "select" status on or-mwc-list by updating selectedActions 
                            this.selectedActions = this.selectedActions == null ? [] : null;
                        }}"
                        ></or-mwc-list.>
                    </div>
                </div>
            </or-mwc-drawer>
            ${when(this.isDrawerOpen(), () => html`
                <div id="drawer-custom-scrim" @click="${() => this.toggleDrawer(false)}"></div>
            `)}
        `
    }
}


/* --------------------------------------------------------------- */

// TEMPLATE RENDERING FUNCTIONS
// Wrote standalone from the component, to allow external use if needed.

function getDashboardListTemplate(dashboards: Dashboard[], selectedId: string, onSelect: (id: string) => void, userId?: string, loading = false): TemplateResult {
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

function getDashboardMenuItems(dashboards: Dashboard[], userId?: string): ListItem[][] {
    const dashboardItems: ListItem[][] = [];
    if(dashboards?.length > 0) {
        const myDashboards: Dashboard[] = [];
        const otherDashboards: Dashboard[] = [];
        dashboards?.forEach((d) => {
            (userId != null && d.ownerId == userId) ? myDashboards.push(d) : otherDashboards.push(d);
        });
        [myDashboards, otherDashboards].forEach((array, index) => {
            dashboardItems[index] = [];
            array.sort((a, b) => a.displayName ? a.displayName.localeCompare(b.displayName!) : 0).forEach((d) => {
                dashboardItems[index].push({ icon: "view-dashboard", text: d.displayName, value: d.id })
            })
        })
    }
    return dashboardItems;
}
