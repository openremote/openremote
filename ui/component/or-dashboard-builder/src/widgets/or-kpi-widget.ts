import manager from "@openremote/core";
import {Asset, Attribute, AttributeRef, DashboardWidget } from "@openremote/model";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { i18next } from "@openremote/or-translate";
import { html, LitElement, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import {OrWidgetConfig, OrWidgetEntity} from "./or-base-widget";
import {style} from "../style";
import {SettingsPanelType, widgetSettingsStyling} from "../or-dashboard-settingspanel";

export interface KpiWidgetConfig extends OrWidgetConfig {
    displayName: string;
    attributeRefs: AttributeRef[];
    period?: 'year' | 'month' | 'week' | 'day' | 'hour' | 'minute' | 'second';
}

export class OrKpiWidget implements OrWidgetEntity {

    readonly DISPLAY_MDI_ICON: string = "label";
    readonly DISPLAY_NAME: string = "KPI";
    readonly MIN_COLUMN_WIDTH: number = 2;
    readonly MIN_PIXEL_HEIGHT: number = 200;
    readonly MIN_PIXEL_WIDTH: number = 200;

    getDefaultConfig(widget: DashboardWidget): OrWidgetConfig {
        return {
            displayName: widget.displayName,
            attributeRefs: [],
            period: "day"
        } as KpiWidgetConfig;
    }

    getSettingsHTML(widget: DashboardWidget, realm: string) {
        return html`<or-kpi-widgetsettings .widget="${widget}" realm="${realm}"></or-kpi-widgetsettings>`;
    }

    getWidgetHTML(widget: DashboardWidget, editMode: boolean, realm: string) {
        return html`<or-kpi-widget .widget="${widget}" .editMode="${editMode}" realm="${realm}" style="height: 100%; overflow: hidden;"></or-kpi-widget>`;
    }

}

@customElement("or-kpi-widget")
export class OrKpiWidgetContent extends LitElement {

    @property()
    public readonly widget?: DashboardWidget;

    @property()
    public editMode?: boolean;

    @property()
    public realm?: string;

    @state()
    private assets: Asset[] = [];

    @state()
    private assetAttributes: [number, Attribute<any>][] = [];

    render() {
        return html`
            <or-attribute-card .assets="${this.assets}" .assetAttributes="${this.assetAttributes}" showControls="${false}" showTitle="${false}" realm="${this.realm}" style="height: 100%;"></or-attribute-card>
        `
    }

    updated(changedProperties: Map<string, any>) {
        console.error(changedProperties);
        if(changedProperties.has("widget") || changedProperties.has("editMode")) {
            this.fetchAssets(this.widget?.widgetConfig).then((assets) => {
                this.assets = assets!;
                this.assetAttributes = this.widget?.widgetConfig.attributeRefs.map((attrRef: AttributeRef) => {
                    const assetIndex = assets!.findIndex((asset) => asset.id === attrRef.id);
                    const foundAsset = assetIndex >= 0 ? assets![assetIndex] : undefined;
                    return foundAsset && foundAsset.attributes ? [assetIndex, foundAsset.attributes[attrRef.name!]] : undefined;
                }).filter((indexAndAttr: any) => !!indexAndAttr) as [number, Attribute<any>][];
                this.requestUpdate();
            });
        }
    }

    // Fetching the assets according to the AttributeRef[] input in DashboardWidget if required. TODO: Simplify this to only request data needed for attribute list
    async fetchAssets(config: OrWidgetConfig | any): Promise<Asset[] | undefined> {
        if(config.attributeRefs) {
            console.error("Fetching assets in or-chart-widget!");
            if (config.attributeRefs != null) {
                let assets: Asset[] = [];
                await manager.rest.api.AssetResource.queryAssets({
                    ids: config.attributeRefs?.map((x: AttributeRef) => {
                        return x.id;
                    }) as string[]
                }).then(response => {
                    assets = response.data;
                }).catch((reason) => {
                    console.error(reason);
                    showSnackbar(undefined, i18next.t('errorOccurred'));
                });
                return assets;
            }
        } else {
            console.error("Error: attributeRefs are not present in widget config!");
        }
    }
}



@customElement("or-kpi-widgetsettings")
export class OrKpiWidgetSettings extends LitElement {

    @property()
    public widget?: DashboardWidget;

    // Default values
    private expandedPanels: string[] = [i18next.t('attributes')];
    private loadedAssets: Asset[] = [];


    static get styles() {
        return [style, widgetSettingsStyling];
    }

    // UI Rendering
    render() {
        const config = this.widget?.widgetConfig;
        console.error(this.widget);
        return html`
            <div>
                ${this.generateExpandableHeader(i18next.t('attributes'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('attributes')) ? html`
                    <or-dashboard-settingspanel .type="${SettingsPanelType.SINGLE_ATTRIBUTE}" .widget="${this.widget}"
                                                @updated="${(event: CustomEvent) => { this.onAttributesUpdate(event.detail.changes); }}"
                    ></or-dashboard-settingspanel>
                ` : null}
            </div>
        `
    }

    onAttributesUpdate(changes: Map<string, any>) {
        if(changes.has('loadedAssets')) {
            this.loadedAssets = changes.get('loadedAssets');
        }
        if(changes.has('widget')) {
            const widget = changes.get('widget') as DashboardWidget;
            if(widget.widgetConfig.attributeRefs.length > 0) {
                this.widget!.displayName = this.loadedAssets[0].name + " - " + this.loadedAssets[0].attributes![widget.widgetConfig.attributeRefs[0].name].name;
            }
        }
        this.forceParentUpdate(changes, false);
    }

    // Method to update the Grid. For example after changing a setting.
    forceParentUpdate(changes: Map<string, any>, force: boolean = false) {
        this.requestUpdate();
        this.dispatchEvent(new CustomEvent('updated', {detail: {changes: changes, force: force}}));
    }

    generateExpandableHeader(name: string): TemplateResult {
        return html`
            <span class="expandableHeader panel-title" @click="${() => { this.expandPanel(name); }}">
                <or-icon icon="${this.expandedPanels.includes(name) ? 'chevron-down' : 'chevron-right'}"></or-icon>
                <span style="margin-left: 6px; height: 25px; line-height: 25px;">${name}</span>
            </span>
        `
    }
    expandPanel(panelName: string): void {
        if (this.expandedPanels.includes(panelName)) {
            const indexOf = this.expandedPanels.indexOf(panelName, 0);
            if (indexOf > -1) {
                this.expandedPanels.splice(indexOf, 1);
            }
        } else {
            this.expandedPanels.push(panelName);
        }
        this.requestUpdate();
    }
}
