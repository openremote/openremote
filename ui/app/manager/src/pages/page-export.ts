import {css, customElement, html, unsafeCSS, query} from "lit-element";
import "@openremote/or-rules";
import {EnhancedStore} from "@reduxjs/toolkit";
import {Page, PageProvider} from "@openremote/or-app";
import {AppStateKeyed} from "@openremote/or-app";
import {i18next} from "@openremote/or-translate";
import { DefaultColor3 } from "@openremote/core";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { OrMwcDialog } from "@openremote/or-mwc-components/or-mwc-dialog";
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

    get name(): string {
        return "export";
    }

    constructor(store: EnhancedStore<S>) {
        super(store);
    }

    protected render() {

        const hidden = false,
            headers = ['header 1', 'header 2', 'header 3', 'header 4', 'header 5'],
            rows = [['cell 1', 'cell 2', 'cell 3', 'cell 4', 'cell 5'],['cell 1', 'cell 2', 'cell 3', 'cell 4', 'cell 5'],['cell 1', 'cell 2', 'cell 3', 'cell 4', 'cell 5']],
            options = {
                stickyFirstColumn: false
            };
        
        return html`
            <div id="wrapper">
                <div id="title">
                    <or-icon icon="database-plus"></or-icon>
                    ${i18next.t("dataExport")}
                </div>
                <div class="panel">
                    <p class="panel-title">${i18next.t("dataSelection")}</p>
                    <h5 class="text-muted">${i18next.t("assetAttributeSelection")}</h5>
                    <or-table id="attribute-table" .hidden="${hidden}" .headers="${headers}" .rows="${rows}" .options="${options}"></or-table>
                    <or-mwc-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("addAssetAttribute")}" icon="plus" @click="${() => this._openDialog()}"></or-mwc-input>
                    <or-mwc-dialog id="mdc-dialog"></or-mwc-dialog>
                </div>
            </div>

        `;

    }
    
    protected _openDialog() {
        if (this._dialog) {

            this._dialog.dialogTitle = i18next.t("addAttribute");

            this._dialog.dialogActions = [
                {
                    actionName: "cancel",
                    content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("cancel")}"></or-mwc-input>`,
                    action: () => {
                        // Nothing to do here
                    }
                },
                {
                    actionName: "yes",
                    default: true,
                    content: html`<or-mwc-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("add")}" data-mdc-dialog-action="yes" data-mdc-dialog-button-default></or-mwc-input>`,
                    action: () => {
                        const dialog: OrMwcDialog = this.shadowRoot!.getElementById("mdc-dialog") as OrMwcDialog;
                        if (dialog.shadowRoot && dialog.shadowRoot.getElementById("attribute-picker")) {
                            const elm = dialog.shadowRoot.getElementById("attribute-picker") as HTMLInputElement;
                            // this.dispatchEvent(new OrAttributeCardAddAttributeEvent(elm.value));
                        }
                    }
                }
            ];

            this._dialog.dialogContent = null;

            this._dialog.dismissAction = null;
            
            this._dialog.open();
        }
    }

    public stateChanged(state: S) {
    }
}
