import {css, html, LitElement} from "lit";
import { customElement, property} from "lit/decorators.js";
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import "@openremote/or-icon";
import {style} from "./style";
import 'gridstack/dist/h5/gridstack-dd-native';
import { Dashboard, DashboardAccess, DashboardScalingPreset, DashboardScreenPreset} from "@openremote/model";
import manager from "@openremote/core";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import "@openremote/or-mwc-components/or-mwc-menu";
import { getContentWithMenuTemplate } from "@openremote/or-mwc-components/or-mwc-menu"; //nosonar
import {dashboardAccessToString, DashboardSizeOption} from ".";

//language=css
const treeStyling = css`
`;

@customElement("or-dashboard-tree")
export class OrDashboardTree extends LitElement {

    // Importing Styles; the unsafe GridStack css, and all custom css
    static get styles() {
        return [style, treeStyling];
    }

    @property()
    protected realm?: string;

    @property()
    private dashboards: Dashboard[] | undefined;

    @property()
    private selected: Dashboard | undefined;

    @property()
    public showControls: boolean = true;


    /* --------------- */

    constructor() {
        super();
        this.updateComplete.then(async () => {
            if(this.dashboards == undefined) {
                await this.getAllDashboards();
            }
        });
    }

    private async getAllDashboards() {
        return manager.rest.api.DashboardResource.getAllRealmDashboards(this.realm!).then((result) => {
            this.dashboards = result.data;
        });
    }

    updated(changedProperties: Map<string, any>) {
        console.log(changedProperties);
        if(this.realm == undefined) { this.realm = manager.displayRealm; }

        if(changedProperties.has("dashboards")) {
            this.dispatchEvent(new CustomEvent("updated", { detail: this.dashboards }));
        }
        if(changedProperties.has("selected") && this.selected != undefined) {
            this.dispatchEvent(new CustomEvent("select", { detail: this.selected }));
        }
    }


    /* ---------------------- */

    private createDashboard(size: DashboardSizeOption) {
        const randomId = (Math.random() + 1).toString(36).substring(2);
        const dashboard = {
            realm: this.realm!,
            displayName: "Dashboard" + (this.dashboards != null ? (this.dashboards.length + 1) : "X"),
            template: {
                id: randomId,
                columns: this.getDefaultColumns(size),
                maxScreenWidth: 4000,
                screenPresets: this.getDefaultScreenPresets(size),
            }
        } as Dashboard
        console.log(dashboard);
        manager.rest.api.DashboardResource.create(dashboard).then((response => {
            if(response.status == 200) {
                console.log(response); // expects a dashboard response
                this.dashboards?.push(response.data);
                this.requestUpdate("dashboards");
                this.dispatchEvent(new CustomEvent("created", { detail: { dashboard: response.data, size: size }}));

                // Select the item that was created
                this.selected = this.dashboards?.find((x) => { return x.id == response.data.id; });
            }
        }))
    }

    private selectDashboard(id: string) {
        this.selected = this.dashboards?.find((dashboard) => { return dashboard.id == id; });
    }

    private deleteDashboard(dashboard: Dashboard) {
        if(dashboard.id != null) {
            manager.rest.api.DashboardResource.delete({dashboardId: [dashboard.id]}).then((response) => {
                console.log(response);
                if(response.status == 204) {
                    this.getAllDashboards();
                }
            })
        }
    }

    /* ---------------------- */

    protected render() {
        const menuItems: ListItem[] = [
            { icon: "monitor", text: "Large", value: DashboardSizeOption.LARGE },
            { icon: "tablet", text: "Medium", value: DashboardSizeOption.MEDIUM },
            { icon: "cellphone", text: "Small", value: DashboardSizeOption.SMALL }
        ]
        const groups = [DashboardAccess.PRIVATE, DashboardAccess.SHARED, DashboardAccess.PUBLIC];
        const dashboardItems: ListItem[][] = []
        if(this.dashboards!.length > 0) {
            groups.forEach((group) => {
                const foundDashboards = this.dashboards?.filter((dashboard) => { return dashboard.viewAccess == group; });
                const items: ListItem[] = [];
                foundDashboards?.forEach((dashboard) => { items.push({ icon: "view-dashboard", text: dashboard.displayName, value: dashboard.id }); })
                dashboardItems.push(items);
            });
        }
        return html`
            <div id="menu-header">
                <div id="title-container">
                    <span id="title">Dashboards</span>
                </div>
                ${this.showControls ? html`
                    <div>
                        <or-mwc-input type="${InputType.BUTTON}" icon="delete" style="margin-right: -4px;" @or-mwc-input-changed="${() => { if(this.selected != null) { this.deleteDashboard(this.selected); }}}"></or-mwc-input>
                        <span style="--or-icon-fill: black">
                            ${getContentWithMenuTemplate(
                                html`<or-mwc-input type="${InputType.BUTTON}" icon="plus" style="margin-left: -4px; --or-icon-fill: white;"></or-mwc-input>`,
                                menuItems, "monitor", (value: string | string[]) => {
                                    const size: DashboardSizeOption = +value;
                                    this.createDashboard(size);
                                }
                            )}                        
                        </span>
                    </div>
                ` : undefined}
            </div>
            <div id="content">
                <div style="padding-top: 8px;">
                    ${dashboardItems.map((items, index) => {
                        return (items != null && items.length > 0) ? html`
                            <div style="padding: 8px 0;">
                                <span style="font-weight: 500; padding-left: 8px; color: #000000;">${dashboardAccessToString(groups[index])}</span>
                                <or-mwc-list .listItems="${items}" .values="${this.selected?.id}" @or-mwc-list-changed="${(event: CustomEvent) => { if(event.detail.length == 1) { this.selectDashboard(event.detail[0].value); }}}"></or-mwc-list>
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
            case DashboardSizeOption.SMALL: { return 4; }
            case DashboardSizeOption.MEDIUM: { return 8; }
            case DashboardSizeOption.LARGE: { return 12; }
            default: { return 12; }
        }
    }

    // TODO: Needs to be moved to probably model itself
    private getDefaultScreenPresets(preset: DashboardSizeOption): DashboardScreenPreset[] {
        switch (preset) {
            case DashboardSizeOption.LARGE: {
                return [{
                    id: "large",
                    displayName: "Large",
                    breakpoint: 1000000, // TODO: change this
                    scalingPreset: DashboardScalingPreset.KEEP_LAYOUT
                }, {
                    id: "medium",
                    displayName: "Medium",
                    breakpoint: 1280,
                    scalingPreset: DashboardScalingPreset.KEEP_LAYOUT
                }, {
                    id: "small",
                    displayName: "Small",
                    breakpoint: 640,
                    scalingPreset: DashboardScalingPreset.BLOCK_DEVICE
                }];
            }
            case DashboardSizeOption.SMALL: {
                return [{
                    id: "large",
                    displayName: "Large",
                    breakpoint: 1000000,
                    scalingPreset: DashboardScalingPreset.BLOCK_DEVICE
                }, {
                    id: "medium",
                    displayName: "Medium",
                    breakpoint: 1280,
                    scalingPreset: DashboardScalingPreset.KEEP_LAYOUT
                }, {
                    id: "small",
                    displayName: "Small",
                    breakpoint: 640,
                    scalingPreset: DashboardScalingPreset.KEEP_LAYOUT
                }];
            }
            default: { // or DashboardSizeOption.MEDIUM since that is the default
                return [{
                    id: "large",
                    displayName: "Large",
                    breakpoint: 1000000,
                    scalingPreset: DashboardScalingPreset.KEEP_LAYOUT
                }, {
                    id: "medium",
                    displayName: "Medium",
                    breakpoint: 1280,
                    scalingPreset: DashboardScalingPreset.KEEP_LAYOUT
                }, {
                    id: "small",
                    displayName: "Small",
                    breakpoint: 640,
                    scalingPreset: DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN
                }];
            }
        }
    }
}
