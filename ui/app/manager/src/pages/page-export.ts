import {css, html, unsafeCSS, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";
import "@openremote/or-rules";
import {Store} from "@reduxjs/toolkit";
import {Page, PageProvider} from "@openremote/or-app";
import {AppStateKeyed} from "@openremote/or-app";
import {i18next} from "@openremote/or-translate";
import manager, { DefaultColor3, Util } from "@openremote/core";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {OrAttributePickerPickedEvent, OrAttributePicker } from "@openremote/or-attribute-picker";
import { AttributeRef } from "@openremote/model";
import moment from "moment";
import { buttonStyle } from "@openremote/or-rules";
import {createSelector} from "reselect";
import { showDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

export function pageExportProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "export",
        routes: [
            "data-export"
        ],
        pageCreator: () => {
            return new PageExport(store);
        }
    };
}

export interface OrExportConfig {
    realm: string;
    selectedAttributes: AttributeRef[];
}

interface TableRow {
    assetName: string,
    assetId: string,
    attributeName: string,
    oldestTimestamp: number,
    latestTimestamp: number
}

@customElement("page-export")
export class PageExport extends Page<AppStateKeyed> {

    static get styles() {
        // language=CSS
        return [
            unsafeCSS(tableStyle),
            unsafeCSS(buttonStyle),
            css`

                #wrapper {
                    height: 100%;
                    width: 100%;
                    display: flex;
                    flex-direction: column;
                    overflow: auto;
                }

                #title {
                    padding: 0 20px;
                    font-size: 18px;
                    font-weight: bold;
                    width: calc(100% - 40px);
                    max-width: 1360px;
                    margin: 20px auto;
                    align-items: center;
                    display: flex;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                }

                #title or-icon {
                    margin-right: 10px;
                    margin-left: 14px;
                }

                .panel {
                    width: calc(100% - 90px);
                    max-width: 1310px;
                    background-color: white;
                    border: 1px solid #e5e5e5;
                    border-radius: 5px;
                    position: relative;
                    margin: 0 auto;
                    padding: 24px;
                }

                .panel-title {
                    text-transform: uppercase;
                    font-weight: bolder;
                    line-height: 1em;
                    color: var(--internal-or-asset-viewer-title-text-color);
                    margin-bottom: 20px;
                    margin-top: 0;
                    flex: 0 0 auto;
                    letter-spacing: 0.025em;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                }

                th {
                    color: ${unsafeCSS(DefaultColor3)};
                }
                
                .mdc-data-table__row {
                    border-top-color: #D3D3D3;
                }

                td, th {
                    width: 25%
                }

                .meta-item-container {
                    flex-direction: row;
                    overflow: hidden;
                    max-height: 0;
                    transition: max-height 0.25s ease-out;
                    padding-left: 16px;
                }

                .mdc-data-table__header-cell {
                    font-weight: bold;
                    color: ${unsafeCSS(DefaultColor3)};
                }
                
                .button {
                    cursor: pointer;
                    display: flex;
                    flex-direction: row;
                    align-content: center;
                    padding: 16px;
                    align-items: center;
                    font-size: 14px;
                    text-transform: uppercase;
                    color: var(--or-app-color4);
                }
                .button or-icon {
                    --or-icon-fill: var(--or-app-color4);
                    margin-right: 5px;
                }

                .remove-button {
                    right: 20px;
                    top: 13px;
                    transform: translate(50%, -50%);
                    position: relative;
                }

                tr:hover .remove-button {
                    visibility: visible;
                }

                @media screen and (max-width: 768px) {
                    #title {
                        padding: 0;
                        width: 100%;
                    }

                    .hide-mobile {
                        display: none;
                    }

                    .panel {
                        border-radius: 0;
                        border-left: 0px;
                        border-right: 0px;
                        width: calc(100% - 48px);
                    }

                    td, th {
                        width: 50%
                    }
                }
                
                .timerange-wrapper {
                    margin-bottom: 2em
                }
                
                .export-btn-wrapper {
                    display: flex;
                    justify-content: space-between;
                }

                .export-btn-wrapper .button {
                    padding: 0;
                    margin-right: 0;
                    margin-bottom: 0;
                }
            `,
        ];
    }

    @property({type: Object, attribute: false})
    public tableRowsHtml?: TemplateResult;

    @property({type: Number})
    private oldestTimestamp: number = moment().subtract(1, 'months').valueOf();
    @property({type: Number})
    private latestTimestamp: number = moment().valueOf();

    private config?: OrExportConfig;
    private realm: string;
    private isClearExportBtnDisabled: boolean = true;
    private isExportBtnDisabled: boolean = true;
    
    private tableRows: TableRow[] = [];

    protected _realmSelector = (state: AppStateKeyed) => state.app.realm || manager.displayRealm;

    protected getRealmState = createSelector(
        [this._realmSelector],
        async (realm) => {
            this.realm = realm;
            this.loadConfig();
        }
    );

    get name(): string {
        return "export";
    }

    constructor(store: Store<AppStateKeyed>) {
        super(store);
    }
    
    protected render() {

        const headers = [
            i18next.t('assetName'), 
            i18next.t('attributeName'), 
            i18next.t('oldestDatapoint'), 
            i18next.t('latestDatapoint')
        ];

        return html`
            <div id="wrapper">
                <div id="title">
                    <or-icon icon="database-export"></or-icon> 
                    ${i18next.t("dataExport")}
                </div>
                <div class="panel">
                    <p class="panel-title">${i18next.t("dataSelection")}</p>
                    <div class="mdc-data-table" style="width: 100%; max-height: 500px; overflow-y: auto;margin-bottom: 2em">
                        <table class="mdc-data-table__table" aria-label="attribute list" >
                            <thead>
                            <tr class="mdc-data-table__header-row">
                            ${headers.map(header => html`
                                <th class="mdc-data-table__header-cell" role="columnheader" scope="col">${header}</th>
                            `)}
                                <th></th>
                            </tr>
                            </thead>
                            <tbody id="table-body" class="mdc-data-table__content">
                            ${this.tableRowsHtml}
                            <tr class="mdc-data-table__row">
                                <td colspan="100%">
                                    <a class="button" @click="${() => this._openDialog()}"><or-icon icon="plus"></or-icon>${i18next.t("addAssetAttribute")}</a>
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                    <div class="timerange-wrapper">
                        <or-mwc-input .type="${InputType.DATETIME}" label="${Util.capitaliseFirstLetter(i18next.t("exportFrom"))}" .value="${moment(this.oldestTimestamp).toDate()}" @or-mwc-input-changed="${(evt: OrInputChangedEvent) => this.oldestTimestamp = evt.detail.value}"></or-mwc-input>
                        <or-mwc-input .type="${InputType.DATETIME}" label="${Util.capitaliseFirstLetter(i18next.t("to"))}" .value="${moment(this.latestTimestamp).toDate()}" @or-mwc-input-changed="${(evt: OrInputChangedEvent) => this.latestTimestamp = evt.detail.value}"></or-mwc-input>
                    </div>
                    <div class="export-btn-wrapper">
                        <or-mwc-input .disabled="${this.isClearExportBtnDisabled}" class="button" .type="${InputType.BUTTON}" label="clearTable" @click="${() => this.clearSelection()}"></or-mwc-input>
                        <or-mwc-input .disabled="${this.isExportBtnDisabled}" class="button" raised .type="${InputType.BUTTON}" label="export" @click="${() => this.export()}"></or-mwc-input>
                    </div>
                </div>
            </div>

        `;

    }
    
    protected async renderTable(attributeRefs: AttributeRef[]) {

        if (!attributeRefs || attributeRefs.length < 1) {
            this.clearSelection();
            return;
        }
        
        const dataPointInfoPromises = attributeRefs.map((attrRef: AttributeRef) => {
            return manager.rest.api.AssetDatapointResource.getDatapointPeriod({
                assetId: attrRef.id,
                attributeName: attrRef.name,
            });
        });

        Promise.all(dataPointInfoPromises).then(datapointPeriod => {

            const assetInfoPromises = datapointPeriod.map(result => {
                return manager.rest.api.AssetResource.get(result.data.assetId);
            });

            Promise.all(assetInfoPromises).then(assetInfos => {
                const allAssets = assetInfos.map(attr => attr.data);
                const allDatapoints = datapointPeriod.map(datapoints => datapoints.data);

                if (allDatapoints.length > 0) {
                    this.tableRows = allDatapoints.map(dataInfo => {
                        const relevantAssetInfo = allAssets.find(asset => asset.id === dataInfo.assetId);
                        return {
                            assetName: relevantAssetInfo.name,
                            assetId: relevantAssetInfo.id,
                            attributeName: dataInfo.attributeName,
                            oldestTimestamp: dataInfo.oldestTimestamp,
                            latestTimestamp: dataInfo.latestTimestamp
                        };
                    }).sort((a, b) => b.latestTimestamp - a.latestTimestamp);
                    this.renderTableRows();
                    this.isClearExportBtnDisabled = false;
                    this.isExportBtnDisabled = false;
                } else {
                    this.isClearExportBtnDisabled = true;
                    this.isExportBtnDisabled = true;
                }
            })

        });
        
    }
    
    protected renderTableRows() {

        this.tableRowsHtml = html`
            ${this.tableRows.map(attr => html`
                <tr class="mdc-data-table__row">
                    <td class="padded-cell mdc-data-table__cell">${attr.assetName}</td>
                    <td class="padded-cell mdc-data-table__cell">${Util.camelCaseToSentenceCase(attr.attributeName)}</td>
                    <td class="padded-cell mdc-data-table__cell">${moment(attr.oldestTimestamp).format('llll')}</td>
                    <td class="padded-cell mdc-data-table__cell">${moment(attr.latestTimestamp).format('llll')}</td>
                    <td>
                        <button class="remove-button button-clear" @click="${() => this._deleteAttribute(attr.assetId, attr.attributeName)}">
                            <or-icon icon="close-circle"></or-icon>
                        </button>
                    </td>
                </tr>
            `)}
        `;
    }
    
    protected _deleteAttribute(assetId, attributeName) {
        const indexTablerows = this.tableRows.findIndex(e => e.assetId === assetId && e.attributeName === attributeName);
        const indexSelectedAttrs = this.config.selectedAttributes.findIndex(e => e.id === assetId && e.name === attributeName);

        this.tableRows.splice(indexTablerows, 1);
        this.config.selectedAttributes.splice(indexSelectedAttrs, 1);
        this.saveConfig();
        
        this.renderTableRows();
    }
    
    protected _openDialog() {

        const dialog = showDialog(new OrAttributePicker()
            .setShowOnlyDatapointAttrs(true)
            .setMultiSelect(true)
            .setSelectedAttributes(this.config.selectedAttributes)
        );

        dialog.addEventListener(OrAttributePickerPickedEvent.NAME, async (ev: OrAttributePickerPickedEvent) => {
            const selectedAttributes = ev.detail;
            await this.renderTable(selectedAttributes);
            this.config = {
                realm: this.realm,
                selectedAttributes: selectedAttributes
            }
            this.saveConfig();
        });
    }
    
    protected export = () => {
        manager.rest.api.AssetDatapointResource.getDatapointExport({
            attributeRefs: JSON.stringify(this.tableRows.map(attr => ({id: attr.assetId, name: attr.attributeName}))),
            fromTimestamp: this.oldestTimestamp,
            toTimestamp: this.latestTimestamp
        }, {
            responseType: "blob",
            
        }).then(response => {
            // This is the best we can do with xhr - would need to return a link to the file for proper streamed download support
            // @ts-ignore
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute("download", "dataexport.zip");
            document.body.appendChild(link);
            link.click();
        });
    }
    
    protected async loadConfig() {

        let configs: OrExportConfig[] = await manager.console.retrieveData("OrExportConfig") || [];
        if (!configs.length || !Object.getOwnPropertyNames(configs[0]).includes("realm")) {
            manager.console.storeData("OrExportConfig", null);
        }
            
        this.config = configs.find(e => e.realm === this.realm) || {realm:this.realm,selectedAttributes:[]};

        // prune removed assets that still exist in localstorage
        const response = await manager.rest.api.AssetResource.queryAssets({
            ids: this.config.selectedAttributes.map(e => e.id)
        });
        const assetsIds = response.data.map(e => e.id) || [];
        this.config.selectedAttributes = this.config.selectedAttributes.filter(e => assetsIds.includes(e.id));

        this.saveConfig();
        this.renderTable(this.config.selectedAttributes);

    }
    
    protected async saveConfig() {

        let configs: OrExportConfig[] = await manager.console.retrieveData("OrExportConfig") || [];
        let config = configs.find(c => c.realm === this.realm);
        if (config) {
            config.selectedAttributes = this.config.selectedAttributes;
        }
        manager.console.storeData("OrExportConfig", [...configs, this.config]);
    }
    
    protected clearSelection = () => {
        this.config = {
            realm: this.realm,
            selectedAttributes: []
        };
        this.saveConfig();
        this.tableRows = [];
        this.renderTableRows();
        this.isClearExportBtnDisabled = true;
        this.isExportBtnDisabled = true;
    }

    public stateChanged(state: AppStateKeyed) {
        this.getRealmState(state);
    }
    
}
