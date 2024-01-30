import {css, html, PropertyValues, TemplateResult } from "lit";
import { customElement } from "lit/decorators.js";
import {OrAssetWidget} from "../util/or-asset-widget";
import {WidgetManifest} from "../util/or-widget";
import {WidgetConfig} from "../util/widget-config";
import {WidgetSettings} from "../util/widget-settings";
import {TableSettings} from "../settings/table-settings";
import {OrMwcTableRowClickEvent, TableColumn, TableRow, TableConfig} from "@openremote/or-mwc-components/or-mwc-table";
import {i18next} from "@openremote/or-translate";
import {Util} from "@openremote/core";
import {Asset, AssetModelUtil} from "@openremote/model";
import "@openremote/or-mwc-components/or-mwc-table";

export interface TableWidgetConfig extends WidgetConfig {
    assetType?: string
    assetIds: string[]
    attributeNames: string[],
    tableSize: number,
    tableOptions: number[]
}

function getDefaultConfig(): TableWidgetConfig {
    return {
        assetType: undefined,
        assetIds: [],
        attributeNames: [],
        tableSize: 10,
        tableOptions: [10, 25, 100]
    }
}

const styling = css`
    #widget-wrapper {
      height: 100%;
      overflow: hidden;
    }
`

@customElement("table-widget")
export class TableWidget extends OrAssetWidget {

    protected widgetConfig!: TableWidgetConfig;

    static getManifest(): WidgetManifest {
        return {
            displayName: "Table",
            displayIcon: "table",
            getContentHtml(config: WidgetConfig): OrAssetWidget {
                return new TableWidget(config);
            },
            getDefaultConfig(): WidgetConfig {
                return getDefaultConfig();
            },
            getSettingsHtml(config: WidgetConfig): WidgetSettings {
                return new TableSettings(config);
            }

        }
    }

    static get styles() {
        return [...super.styles, styling];
    }

    // TODO: Improve this to be more efficient
    refreshContent(force: boolean): void {
        this.widgetConfig = JSON.parse(JSON.stringify(this.widgetConfig)) as TableWidgetConfig;
    }

    // Lit Lifecycle
    protected willUpdate(changedProps: PropertyValues) {
        if(changedProps.has('widgetConfig') && this.widgetConfig) {
            this.loadAssets();
        }

        return super.willUpdate(changedProps);
    }


    /* --------------------------------------- */

    protected loadAssets() {
        if(this.widgetConfig.assetIds.find(id => !this.isAssetLoaded(id))) {
           this.queryAssets({
               ids: this.widgetConfig.assetIds,
               select: {
                   attributes: this.widgetConfig.attributeNames
               }
           }).then((assets) => {
               this.loadedAssets = assets;
           })
        }
    }

    protected getColumns(attributeNames: string[]): TableColumn[] {
        const referenceAsset: Asset | undefined = this.loadedAssets[0];
        const attrColumns = attributeNames.map(attrName => {
            let text = attrName;
            let numeric = false;
            if(this.widgetConfig.assetType && referenceAsset && referenceAsset.attributes && referenceAsset.attributes[attrName]) {
                const attributeDescriptor = AssetModelUtil.getAttributeDescriptor(attrName, this.widgetConfig.assetType);
                text = Util.getAttributeLabel(referenceAsset.attributes[attrName], attributeDescriptor, this.widgetConfig.assetType, true);
                numeric = attributeDescriptor?.format?.asNumber || attributeDescriptor?.format?.asSlider || false;
            }
            return {
                title: text,
                isSortable: true,
                isNumeric: numeric
            } as TableColumn;
        });
        return Array.of({ title: i18next.t('assetName'), isSortable: true }, ...attrColumns);
    }

    protected getRows(attributeNames: string[]): TableRow[] {
        return this.loadedAssets.map(asset => {
            const attrEntries = attributeNames.map(attrName => {
                if(asset.attributes && asset.attributes[attrName]) {
                    const attributeDescriptor = AssetModelUtil.getAttributeDescriptor(attrName, asset.type!);
                    const strValue = Util.getAttributeValueAsString(asset.attributes[attrName], attributeDescriptor, asset.type, false);
                    const numValue = parseFloat(strValue);
                    return isNaN(numValue) ? strValue : numValue;
                } else {
                    return 'N.A.'
                }
            })
            const content: any[] = Array.of(asset.name!, ...attrEntries);
            return {
                content: content
            };
        })
    }

    protected render(): TemplateResult {
        const tableConfig: any = {
            fullHeight: true,
            pagination: {
                enable: true,
                options: this.widgetConfig.tableOptions,
            }
        } as TableConfig
        const columns: TableColumn[] = this.getColumns(this.widgetConfig.attributeNames);
        const rows: TableRow[] = this.getRows(this.widgetConfig.attributeNames);
        return html`
            <div id="widget-wrapper">
                <or-mwc-table .columns="${columns}" .rows="${rows}" .config="${tableConfig}" .paginationSize="${this.widgetConfig.tableSize}"
                              @or-mwc-table-row-click="${(ev: OrMwcTableRowClickEvent) => this.onTableRowClick(ev)}"
                ></or-mwc-table>
            </div>
        `;
    }

    protected onTableRowClick(ev: OrMwcTableRowClickEvent) {

    }


}
