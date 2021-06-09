import {css, customElement, html, unsafeCSS, property} from "lit-element";
import "@openremote/or-rules";
import {EnhancedStore} from "@reduxjs/toolkit";
import {Page, PageProvider} from "@openremote/or-app";
import {AppStateKeyed} from "@openremote/or-app";
import {i18next} from "@openremote/or-translate";
import manager, { DefaultColor3 } from "@openremote/core";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import {OrAddAttributeRefsEvent, OrMwcAttributeSelector } from "@openremote/or-mwc-components/or-mwc-dialog";
import { AttributeRef } from "@openremote/model";
const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

export function pageExportProvider<S extends AppStateKeyed>(store: EnhancedStore<S>): PageProvider<S> {
    return {
        name: "export",
        routes: [
            "export"
        ],
        pageCreator: () => {
            return new PageExport(store);
        }
    };
}

@customElement("page-export")
class PageExport<S extends AppStateKeyed> extends Page<S> {

    static get styles() {
        // language=CSS
        return [
            unsafeCSS(tableStyle),
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
                }

                h5 {
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

                or-mwc-input {
                    margin-bottom: 20px;
                    margin-right: 16px;
                }

                or-icon {
                    vertical-align: middle;
                    --or-icon-width: 20px;
                    --or-icon-height: 20px;
                    margin-right: 2px;
                    margin-left: -5px;
                }

                .row {
                    display: flex;
                    flex-direction: row;
                    margin: 10px 0;
                    flex: 1 1 0;
                }

                .column {
                    display: flex;
                    flex-direction: column;
                    margin: 0px;
                    flex: 1 1 0;

                }

                .mdc-data-table__header-cell {
                    font-weight: bold;
                    color: ${unsafeCSS(DefaultColor3)};
                }

                .mdc-data-table__header-cell:first-child {
                    padding-left: 36px;
                }

                .attribute-meta-row td {
                    padding: 0;
                }

                .attribute-meta-row.expanded .meta-item-container {
                    max-height: 1000px;
                    transition: max-height 1s ease-in;
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

                @media screen and (max-width: 768px) {
                    #title {
                        padding: 0;
                        width: 100%;
                    }

                    .hide-mobile {
                        display: none;
                    }

                    .row {
                        display: block;
                        flex-direction: column;
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
            `,
        ];
    }

    @property()
    private tableRows: any[] = []; //todo type this so it can be put in table

    private isExportBtnDisabled: boolean = true;
    private oldestTimestamp: number;
    private latestTimestamp: number;
    private attrRefs: any[];

    get name(): string {
        return "export";
    }

    constructor(store: EnhancedStore<S>) {
        super(store);
    }

    protected render() {

        const hidden = false,
            headers = [
                i18next.t('assetName'), 
                i18next.t('attributeName'), 
                // i18next.t('valueType'), 
                i18next.t('oldestDatapoint'), 
                i18next.t('latestDatapoint')
            ],
            options = {
                stickyFirstColumn: false
            };
        
        return html`
            <div id="wrapper">
                <div id="title">
                    <or-icon icon="database-export"></or-icon> 
                    ${i18next.t("dataExport")}
                </div>
                <div class="panel">
                    <p class="panel-title">${i18next.t("dataSelection")}</p>
                    <h5 class="text-muted">${i18next.t("assetAttributeSelection")}</h5>
                    <or-table id="attribute-table" .hidden="${hidden}" .headers="${headers}" .rows="${this.tableRows}" .options="${options}"></or-table>
                    <or-mwc-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("addAssetAttribute")}" icon="plus" @click="${() => this._openDialog()}"></or-mwc-input>
                    <or-mwc-input .disabled="${this.isExportBtnDisabled}" class="button" .type="${InputType.BUTTON}" label="${i18next.t("export")}" @click="${() => this.export()}"></or-mwc-input>
                    <or-mwc-dialog id="mdc-dialog"></or-mwc-dialog>
                </div>
            </div>

        `;

    }
    
    protected _openDialog() {
        const hostElement = document.body;

        const dialog = new OrMwcAttributeSelector();
        dialog.isOpen = true;
        dialog.addEventListener(OrAddAttributeRefsEvent.NAME, async (ev: OrAddAttributeRefsEvent) => {
            const dataPointInfoPromises = ev.detail.selectedAttributes.map((attrRef: AttributeRef) => {
                return manager.rest.api.AssetDatapointResource.getDatapointPeriod({
                    assetId: attrRef.id,
                    attributeName: attrRef.name,
                });
            });
            
            Promise.all(dataPointInfoPromises).then(dataInfos => {
                
                const assetInfoPromises = dataInfos.map(result => {
                    return manager.rest.api.AssetResource.get(result.data.assetId);
                });
                
                Promise.all(assetInfoPromises).then(assetInfos => {
                    const allAssets = assetInfos.map(attr => attr.data),
                        allDatapoints = dataInfos.map(datapoints => datapoints.data);

                    this.tableRows = allDatapoints.map(dataInfo => {
                        const relevantAssetInfo = allAssets.find(asset => asset.id === dataInfo.assetId);
                        return [
                            relevantAssetInfo.name,
                            dataInfo.attributeName,
                            dataInfo.oldestTimestamp,
                            dataInfo.latestTimestamp,
                        ];
                    });
                    
                    if (allDatapoints.length > 0) {
                        this.attrRefs = allDatapoints.map(dataInfo => {
                            const relevantAssetInfo = allAssets.find(asset => asset.id === dataInfo.assetId);
                            return {
                                id: relevantAssetInfo.id,
                                name: dataInfo.attributeName
                            }
                        });
                        this.oldestTimestamp = allDatapoints[0].oldestTimestamp;
                        this.latestTimestamp = allDatapoints[0].oldestTimestamp;
                        this.isExportBtnDisabled = false;
                    }
                })
                
            });
        });
        hostElement.append(dialog);
        return dialog;
    }
    
    protected export = () => {
        manager.rest.api.AssetDatapointResource.getDatapointExport({
            attributeRefs: JSON.stringify(this.attrRefs),
            fromTimestamp: this.oldestTimestamp,
            toTimestamp: this.latestTimestamp
        });   
    }

    public stateChanged(state: S) {
    }
}
