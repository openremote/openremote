import {css, html, TemplateResult, unsafeCSS} from "lit";
import {customElement, state} from "lit/decorators.js";
import manager, {DefaultColor3, DefaultColor5} from "@openremote/core";
import "@openremote/or-translate";
import {Store} from "@reduxjs/toolkit";
import {AppStateKeyed, Page, PageProvider} from "@openremote/or-app";
import {CustomAssetTypeAttributeDefinition, CustomAssetTypeDefinition} from "@openremote/model";
import {i18next} from "@openremote/or-translate";
import {OrIcon} from "@openremote/or-icon";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {OrVaadinTextField} from "@openremote/or-vaadin-components/or-vaadin-text-field";

const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

export function pageCustomAssetTypesProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "asset-types",
        routes: ["asset-types"],
        pageCreator: () => {
            return new PageCustomAssetTypes(store);
        },
    };
}

@customElement("page-custom-asset-types")
export class PageCustomAssetTypes extends Page<AppStateKeyed> {

    static get styles() {
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
                    border: 1px solid var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
                    border-radius: 5px;
                    position: relative;
                    margin: 0 auto;
                    padding: 12px 24px 24px;
                }

                .panel-title {
                    display: flex;
                    align-items: center;
                    text-transform: uppercase;
                    font-weight: bolder;
                    line-height: 1em;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                    margin-bottom: 10px;
                    margin-top: 0;
                    flex: 0 0 auto;
                    letter-spacing: 0.025em;
                    min-height: 36px;
                }

                .panel-title p {
                    margin: 0;
                }

                #table-custom-asset-types,
                #table-custom-asset-types table {
                    width: 100%;
                    white-space: nowrap;
                }

                .mdc-data-table__row {
                    cursor: pointer;
                    border-top-color: #d3d3d3;
                }

                .mdc-data-table__header-cell {
                    font-weight: bold;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                }

                .mdc-data-table__header-cell:first-child {
                    padding-left: 36px;
                }

                td, th {
                    width: 20%;
                    border: none;
                }

                td.large, th.large {
                    width: 40%;
                }

                .padded-cell {
                    overflow-wrap: break-word;
                    word-wrap: break-word;
                }

                .details-row td {
                    padding: 0;
                }

                .details-container {
                    overflow: hidden;
                    max-height: 0;
                    transition: max-height 0.25s ease-out;
                    padding: 0 16px;
                }

                .details-row.expanded .details-container {
                    max-height: 1000px;
                    transition: max-height 0.4s ease-in;
                }

                .details-grid {
                    display: grid;
                    grid-template-columns: repeat(2, minmax(0, 1fr));
                    gap: 16px;
                    padding: 14px 0 18px;
                }

                .field-label {
                    font-weight: 600;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                    margin-bottom: 3px;
                }

                .attribute-list {
                    display: flex;
                    flex-direction: column;
                    gap: 6px;
                }

                .attribute-row {
                    display: grid;
                    grid-template-columns: minmax(140px, 1fr) minmax(110px, 160px) 90px;
                    gap: 12px;
                    align-items: center;
                    min-height: 28px;
                }

                .swatch {
                    display: inline-block;
                    width: 14px;
                    height: 14px;
                    border-radius: 2px;
                    border: 1px solid var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
                    vertical-align: middle;
                    margin-right: 8px;
                }

                or-icon {
                    vertical-align: middle;
                    --or-icon-width: 20px;
                    --or-icon-height: 20px;
                    margin-right: 2px;
                    margin-left: -5px;
                }

                .empty {
                    padding: 22px 16px;
                    color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                }

                @media screen and (max-width: 768px) {
                    #title {
                        padding: 0;
                        width: 100%;
                    }

                    .panel {
                        border-left: 0;
                        border-right: 0;
                        width: calc(100% - 48px);
                        border-radius: 0;
                    }

                    .hide-mobile {
                        display: none;
                    }

                    td, th {
                        width: 50%;
                    }

                    .details-grid,
                    .attribute-row {
                        grid-template-columns: 1fr;
                    }
                }
            `,
        ];
    }

    @state()
    protected _definitions?: CustomAssetTypeDefinition[];

    @state()
    protected _definitionFilter: (definitions: CustomAssetTypeDefinition[]) => CustomAssetTypeDefinition[] = (definitions) => definitions;

    @state()
    protected _usageByName: Record<string, number> = {};

    get name(): string {
        return "customAssetTypes";
    }

    public connectedCallback(): void {
        super.connectedCallback();
        this._loadDefinitions();
    }

    public stateChanged(state: AppStateKeyed): void {
    }

    protected render(): TemplateResult | void {
        if (!manager.authenticated) {
            return html`
                <or-translate value="notAuthenticated"></or-translate>
            `;
        }

        if (!manager.isSuperUser()) {
            return html`
                <or-translate value="notSupported"></or-translate>
            `;
        }

        if (!this._definitions) {
            return html``;
        }

        const filteredDefinitions = this._definitionFilter(this._definitions);

        return html`
            <div id="wrapper">
                <div id="title">
                    <or-icon icon="shape-outline"></or-icon>
                    ${i18next.t("customAssetTypes")}
                </div>

                <div class="panel">
                    <div class="panel-title" style="justify-content: space-between;">
                        <p><or-translate value="customAssetTypes"></or-translate></p>
                        <or-vaadin-text-field placeholder=${i18next.t("search")} style="width: 240px;"
                                              @input=${(ev: InputEvent) => this._onDefinitionSearch(ev)}>
                            <or-icon slot="suffix" icon="magnify"></or-icon>
                        </or-vaadin-text-field>
                    </div>
                    <div id="table-custom-asset-types" class="mdc-data-table">
                        <table class="mdc-data-table__table" aria-label="custom asset type list">
                            <thead>
                            <tr class="mdc-data-table__header-row">
                                <th class="mdc-data-table__header-cell" role="columnheader" scope="col">
                                    <or-translate value="name"></or-translate>
                                </th>
                                <th class="mdc-data-table__header-cell hide-mobile large" role="columnheader" scope="col">
                                    <or-translate value="displayName"></or-translate>
                                </th>
                                <th class="mdc-data-table__header-cell hide-mobile" role="columnheader" scope="col">
                                    <or-translate value="attribute_plural"></or-translate>
                                </th>
                                <th class="mdc-data-table__header-cell hide-mobile" role="columnheader" scope="col">
                                    <or-translate value="usage"></or-translate>
                                </th>
                                <th class="mdc-data-table__header-cell" role="columnheader" scope="col">
                                    <or-translate value="status"></or-translate>
                                </th>
                            </tr>
                            </thead>
                            <tbody class="mdc-data-table__content">
                            ${filteredDefinitions.length === 0 ? html`
                                <tr>
                                    <td colspan="100%">
                                        <div class="empty"><or-translate value="noCustomAssetTypes"></or-translate></div>
                                    </td>
                                </tr>
                            ` : filteredDefinitions.map((definition, index) => this._getDefinitionTemplate(definition, index))}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;
    }

    protected async _loadDefinitions(): Promise<void> {
        this._definitions = undefined;
        this._usageByName = {};

        if (!manager.authenticated || !manager.isSuperUser()) {
            this._definitions = [];
            return;
        }

        try {
            const response = await manager.rest.api.CustomAssetTypeResource.getAll();
            if (!this.isConnected) {
                return;
            }

            this._definitions = [...response.data].sort((a, b) => (a.name || "").localeCompare(b.name || ""));
            this._loadUsageCounts(this._definitions);
        } catch (e) {
            console.error("Failed to load custom asset type definitions", e);
            showSnackbar(undefined, i18next.t("loadFailedCustomAssetTypes"), "dismiss");
            this._definitions = [];
        }
    }

    protected async _loadUsageCounts(definitions: CustomAssetTypeDefinition[]): Promise<void> {
        const usageEntries = await Promise.all(definitions
            .filter(definition => !!definition.name)
            .map(async definition => {
                try {
                    const response = await manager.rest.api.CustomAssetTypeResource.getUsage(definition.name!);
                    return [definition.name!, response.data] as [string, number];
                } catch (e) {
                    console.error("Failed to load custom asset type usage count", definition.name, e);
                    return [definition.name!, 0] as [string, number];
                }
            }));

        if (this.isConnected) {
            this._usageByName = Object.fromEntries(usageEntries);
        }
    }

    protected _getDefinitionTemplate(definition: CustomAssetTypeDefinition, index: number): TemplateResult {
        const rowId = "custom-asset-type-row-" + index;
        const attributeCount = definition.attributes?.length || 0;
        const usageCount = definition.name ? this._usageByName[definition.name] : undefined;

        return html`
            <tr class="mdc-data-table__row" @click="${() => this._toggleDefinitionExpand(index)}">
                <td class="padded-cell mdc-data-table__cell">
                    <or-icon id="${rowId}-icon" icon="chevron-right"></or-icon>
                    <span>${definition.name}</span>
                </td>
                <td class="padded-cell hide-mobile mdc-data-table__cell large">
                    ${definition.displayName}
                </td>
                <td class="padded-cell hide-mobile mdc-data-table__cell">
                    ${attributeCount}
                </td>
                <td class="padded-cell hide-mobile mdc-data-table__cell">
                    ${usageCount === undefined ? "" : usageCount}
                </td>
                <td class="padded-cell mdc-data-table__cell">
                    <or-translate value=${definition.enabled === false ? "disabled" : "enabled"}></or-translate>
                </td>
            </tr>
            <tr id="${rowId}-details" class="details-row">
                <td colspan="100%">
                    <div class="details-container">
                        <div class="details-grid">
                            <div>
                                <div class="field-label"><or-translate value="description"></or-translate></div>
                                <div>${definition.description || i18next.t("assetTypes.noDescription")}</div>
                            </div>
                            <div>
                                <div class="field-label"><or-translate value="icon"></or-translate></div>
                                <div>
                                    <or-icon .icon=${definition.icon}></or-icon>
                                    ${definition.icon || ""}
                                </div>
                            </div>
                            <div>
                                <div class="field-label"><or-translate value="colour"></or-translate></div>
                                <div>
                                    ${definition.colour ? html`
                                        <span class="swatch" style="background: ${this._normaliseColour(definition.colour)}"></span>
                                    ` : html``}
                                    ${definition.colour || ""}
                                </div>
                            </div>
                            <div>
                                <div class="field-label"><or-translate value="attribute_plural"></or-translate></div>
                                <div class="attribute-list">
                                    ${attributeCount === 0 ? html`<span><or-translate value="noAttributesToShow"></or-translate></span>` : html`
                                        ${(definition.attributes || [])
                                            .slice()
                                            .sort((a, b) => (a.position ?? 0) - (b.position ?? 0) || (a.name || "").localeCompare(b.name || ""))
                                            .map(attribute => this._getAttributeTemplate(attribute))}
                                    `}
                                </div>
                            </div>
                        </div>
                    </div>
                </td>
            </tr>
        `;
    }

    protected _getAttributeTemplate(attribute: CustomAssetTypeAttributeDefinition): TemplateResult {
        return html`
            <div class="attribute-row">
                <span>${attribute.name}</span>
                <span>${attribute.type}</span>
                <span><or-translate value=${attribute.optional ? "optional" : "required"}></or-translate></span>
            </div>
        `;
    }

    protected _toggleDefinitionExpand(index: number): void {
        const detailsRow = this.shadowRoot!.getElementById("custom-asset-type-row-" + index + "-details");
        const expanderIcon = this.shadowRoot!.getElementById("custom-asset-type-row-" + index + "-icon") as OrIcon;

        if (!detailsRow || !expanderIcon) {
            return;
        }

        if (detailsRow.classList.contains("expanded")) {
            detailsRow.classList.remove("expanded");
            expanderIcon.icon = "chevron-right";
        } else {
            detailsRow.classList.add("expanded");
            expanderIcon.icon = "chevron-down";
        }
    }

    protected _onDefinitionSearch(ev: InputEvent): void {
        const value = (ev.target as OrVaadinTextField).value?.toLowerCase();
        if (!value) {
            this._definitionFilter = (definitions) => definitions;
        } else {
            this._definitionFilter = (definitions) => definitions.filter(definition =>
                (definition.name || "").toLowerCase().includes(value) ||
                (definition.displayName || "").toLowerCase().includes(value) ||
                (definition.description || "").toLowerCase().includes(value) ||
                (definition.attributes || []).some(attribute => (attribute.name || "").toLowerCase().includes(value))
            );
        }
    }

    protected _normaliseColour(colour: string): string {
        return colour.startsWith("#") ? colour : "#" + colour;
    }
}
