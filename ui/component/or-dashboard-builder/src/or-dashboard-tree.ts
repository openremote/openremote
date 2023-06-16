import {css, html, LitElement} from "lit";
import { customElement, property} from "lit/decorators.js";
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import "@openremote/or-icon";
import {style} from "./style";
import {Dashboard, DashboardScalingPreset, DashboardScreenPreset} from "@openremote/model";
import manager from "@openremote/core";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import "@openremote/or-mwc-components/or-mwc-menu";
import {showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import { i18next } from "@openremote/or-translate";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {style as OrAssetTreeStyle} from "@openremote/or-asset-tree";

//language=css
const treeStyling = css`
    #header-btns {
        display: flex;
        flex-direction: row;
        padding-right: 5px;
    }
    .node-container {
        align-items: center;
        padding-left: 10px;
    }
`;

enum DashboardSizeOption {
    DESKTOP, MOBILE
}

@customElement("or-dashboard-tree")
export class OrDashboardTree extends LitElement {

    static get styles() {
        return [style, treeStyling, OrAssetTreeStyle];
    }

    @property()
    protected realm?: string;

    @property()
    private dashboards: Dashboard[] | undefined;

    @property()
    private selected: Dashboard | undefined;

    @property() // REQUIRED
    private readonly userId?: string;

    @property()
    private readonly readonly: boolean = true;

    @property()
    protected hasChanged: boolean = false;

    @property()
    protected showControls: boolean = true;


    /* --------------- */

    constructor() {
        super();
        this.updateComplete.then(async () => {
            if(this.dashboards == undefined) {
                await this.getAllDashboards();
            }
        });
    }

    shouldUpdate(changedProperties: Map<string, any>) {
        if(changedProperties.size == 1) {

            // Prevent any update since it is not necessary in its current state.
            // However, do update when dashboard is saved (aka when hasChanged is set back to false)
            if(changedProperties.has("hasChanged") && this.hasChanged) {
                return false;
            }
        }
        return super.shouldUpdate(changedProperties);
    }

    private async getAllDashboards() {
        return manager.rest.api.DashboardResource.getAllRealmDashboards(this.realm!)
            .then((result) => {
                this.dashboards = result.data;
            }).catch((reason) => {
                console.error(reason);
                showSnackbar(undefined, i18next.t('errorOccurred'));
            });
    }

    updated(changedProperties: Map<string, any>) {
        if(this.realm == undefined) { this.realm = manager.displayRealm; }

        if(changedProperties.has("dashboards") && changedProperties.get("dashboards") != null) {
            this.dispatchEvent(new CustomEvent("updated", { detail: this.dashboards }));
        }
        if(changedProperties.has("selected")) {
            this.dispatchEvent(new CustomEvent("select", { detail: this.selected }));
        }
    }


    /* ---------------------- */

    private createDashboard(size: DashboardSizeOption) {
        const randomId = (Math.random() + 1).toString(36).substring(2);
        const dashboard = {
            realm: this.realm!,
            displayName: this.getDefaultDisplayName(size),
            template: {
                id: randomId,
                columns: this.getDefaultColumns(size),
                maxScreenWidth: this.getDefaultMaxScreenWidth(size),
                screenPresets: this.getDefaultScreenPresets(size),
            }
        } as Dashboard
        manager.rest.api.DashboardResource.create(dashboard).then((response => {
            if(response.status == 200) {
                this.dashboards?.push(response.data);
                this.requestUpdate("dashboards");
                this.dispatchEvent(new CustomEvent("created", { detail: { dashboard: response.data }}));

                // Select the item that was created
                this.selected = this.dashboards?.find((x) => { return x.id == response.data.id; });
            }
        })).catch((reason) => {
            console.error(reason);
            showSnackbar(undefined, i18next.t('errorOccurred'));
        })
    }

    private selectDashboard(id: string | Dashboard | undefined) {
        if(typeof id == 'string') {
            this.selected = this.dashboards?.find((dashboard) => { return dashboard.id == id; });
        } else {
            this.selected = id;
        }
    }

    private deleteDashboard(dashboard: Dashboard) {
        if(dashboard.id != null) {
            manager.rest.api.DashboardResource.delete(this.realm, dashboard.id)
                .then((response) => {
                    if(response.status == 204) {
                        this.getAllDashboards();
                    }
                }).catch((reason) => {
                    console.error(reason);
                    showSnackbar(undefined, i18next.t('errorOccurred'));
            })
        }
    }

    /* ---------------------- */

    protected render() {
        const dashboardItems: ListItem[][] = []
        if(this.dashboards!.length > 0) {
            if(this.userId) {
                const myDashboards: Dashboard[] = [];
                const otherDashboards: Dashboard[] = [];
                this.dashboards?.forEach((d) => {
                    (d.ownerId == this.userId) ? myDashboards.push(d) : otherDashboards.push(d);
                })
                if(myDashboards.length > 0) {
                    const items: ListItem[] = [];
                    myDashboards.sort((a, b) => a.displayName ? a.displayName.localeCompare(b.displayName!) : 0).forEach((d) => {
                        items.push({ icon: "view-dashboard", text: d.displayName, value: d.id });
                    });
                    dashboardItems.push(items);
                }
                if(otherDashboards.length > 0) {
                    const items: ListItem[] = [];
                    otherDashboards.sort((a, b) => a.displayName ? a.displayName.localeCompare(b.displayName!) : 0).forEach((d) => {
                        items.push({ icon: "view-dashboard", text: d.displayName, value: d.id });
                    });
                    dashboardItems.push(items);
                }
            }
        }
        return html`
            <div id="menu-header">
                <div id="title-container">
                    <span id="title">${i18next.t('insights')}</span>
                </div>
                ${this.showControls ? html`
                    <div id="header-btns">
                        ${this.selected != null ? html`
                            <or-mwc-input type="${InputType.BUTTON}" icon="close" @or-mwc-input-changed="${() => { this.selectDashboard(undefined); }}"></or-mwc-input>
                            ${!this.readonly ? html`
                                <or-mwc-input type="${InputType.BUTTON}" icon="delete" @or-mwc-input-changed="${() => { if(this.selected != null) {
                                    showOkCancelDialog(i18next.t('areYouSure'), i18next.t('dashboard.deletePermanentWarning', { dashboard: this.selected.displayName }), i18next.t('delete')).then((ok: boolean) => { if(ok) { this.deleteDashboard(this.selected!); }});
                                }}}"></or-mwc-input>
                            ` : undefined}
                        ` : undefined}
                        ${!this.readonly ? html`
                            <or-mwc-input type="${InputType.BUTTON}" class="hideMobile" icon="plus"
                                            @or-mwc-input-changed="${() => { this.createDashboard(DashboardSizeOption.DESKTOP); }}"
                            ></or-mwc-input>
                        ` : undefined}
                    </div>
                ` : undefined}
            </div>
            <div id="content">
                <div style="padding-top: 8px;">
                    ${dashboardItems.map((items, index) => {
                        return (items != null && items.length > 0) ? html`
                            <div style="padding: 8px 0;">
                                <span style="font-weight: 500; padding-left: 14px; color: #000000;">${(index == 0 ? i18next.t('dashboard.myDashboards') : i18next.t('dashboard.createdByOthers'))}</span>
                                <div id="list-container" style="overflow: hidden;">
                                    <ol id="list">
                                        ${items.map((listItem: ListItem) => {
                                            return html`
                                                <li ?data-selected="${listItem.value == this.selected?.id}" @click="${(_evt: MouseEvent) => {
                                                    if(listItem.value != this.selected?.id) {
                                                        if(this.hasChanged) {
                                                            showOkCancelDialog(i18next.t('areYouSure'), i18next.t('confirmContinueDashboardModified'), i18next.t('discard')).then((ok: boolean) => {
                                                                if(ok) { this.selectDashboard(listItem.value); }
                                                            });
                                                        } else {
                                                            this.selectDashboard(listItem.value);
                                                        }
                                                    }
                                                }}">
                                                    <div class="node-container">
                                                        <span class="node-name">${listItem.text} </span>
                                                    </div>
                                                </li>
                                            `
                                        })}
                                    </ol>
                                </div>
                            </div>
                        ` : undefined
                    })}
                </div>
            </div>
        `
    }



    /* ------------------ */

    // TODO: Needs to be moved to probably model itself
    private getDefaultColumns(preset: DashboardSizeOption): number {
        switch (preset) {
            case DashboardSizeOption.MOBILE: { return 4; }
            case DashboardSizeOption.DESKTOP: { return 12; }
            default: { return 12; }
        }
    }

    // TODO: Needs to be moved to probably model itself
    private getDefaultDisplayName(preset: DashboardSizeOption): string {
        switch (preset) {
            case DashboardSizeOption.DESKTOP: { return i18next.t('dashboard.initialName'); }
            case DashboardSizeOption.MOBILE: { return i18next.t('dashboard.initialName') + " (" + i18next.t('dashboard.size.mobile') + ")"; }
        }
    }

    // TODO: Needs to be moved to probably model itself
    private getDefaultScreenPresets(preset: DashboardSizeOption): DashboardScreenPreset[] {
        switch (preset) {
            case DashboardSizeOption.MOBILE: {
                return [{
                    id: "mobile",
                    displayName: 'dashboard.size.mobile',
                    breakpoint: 640,
                    scalingPreset: DashboardScalingPreset.KEEP_LAYOUT
                }];
            }
            default: { // DashboardSizeOption.DESKTOP since that is the default
                return [{
                    id: "mobile",
                    displayName: 'dashboard.size.mobile',
                    breakpoint: 640,
                    scalingPreset: DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN
                }];
            }
        }
    }

    // TODO: Needs to be moved to probably model itself
    private getDefaultMaxScreenWidth(preset: DashboardSizeOption): number {
        switch (preset) {
            case DashboardSizeOption.DESKTOP: return 4000;
            case DashboardSizeOption.MOBILE: return 640;
        }
    }
}
