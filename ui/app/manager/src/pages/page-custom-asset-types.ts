import {css, html, TemplateResult, unsafeCSS} from "lit";
import {customElement, state} from "lit/decorators.js";
import manager, {DefaultColor3, DefaultColor5} from "@openremote/core";
import "@openremote/or-translate";
import {Store} from "@reduxjs/toolkit";
import {AppStateKeyed, Page, PageProvider} from "@openremote/or-app";
import {
    AssetModelUtil,
    CustomAssetTypeAttributeDefinition,
    CustomAssetTypeDefinition,
    WellknownMetaItems,
    WellknownValueTypes
} from "@openremote/model";
import {i18next} from "@openremote/or-translate";
import {OrIcon} from "@openremote/or-icon";
import {showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {OrVaadinTextField} from "@openremote/or-vaadin-components/or-vaadin-text-field";
import {OrVaadinTextArea} from "@openremote/or-vaadin-components/or-vaadin-text-area";
import {OrVaadinCheckbox} from "@openremote/or-vaadin-components/or-vaadin-checkbox";
import {OrVaadinSelect, SelectItem} from "@openremote/or-vaadin-components/or-vaadin-select";
import "@openremote/or-vaadin-components/or-vaadin-button";

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

                .title-actions {
                    display: flex;
                    gap: 10px;
                    align-items: center;
                }

                .form-grid {
                    display: grid;
                    grid-template-columns: repeat(2, minmax(0, 1fr));
                    gap: 14px 16px;
                    padding: 16px 0 8px;
                }

                .full-width {
                    grid-column: 1 / -1;
                }

                .actions {
                    display: flex;
                    justify-content: flex-end;
                    gap: 10px;
                    margin: 14px 0 8px;
                }

                .attribute-editor {
                    border-top: 1px solid var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
                    margin-top: 4px;
                    padding-top: 14px;
                }

                .attribute-editor-heading {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    gap: 12px;
                    margin-bottom: 10px;
                }

                .attribute-editor-list {
                    display: flex;
                    flex-direction: column;
                    gap: 10px;
                }

                .attribute-editor-row {
                    display: grid;
                    grid-template-columns: repeat(5, minmax(120px, 1fr)) 42px;
                    gap: 12px;
                    align-items: end;
                }

                .attribute-editor-row or-vaadin-button {
                    align-self: center;
                }

                .attribute-options {
                    grid-column: 1 / 6;
                    display: flex;
                    flex-wrap: wrap;
                    gap: 8px 18px;
                    min-height: 36px;
                    align-items: center;
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
                    max-height: 3000px;
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

                .details-actions {
                    display: flex;
                    justify-content: flex-end;
                    gap: 10px;
                }

                .attribute-list {
                    display: flex;
                    flex-direction: column;
                    gap: 6px;
                }

                .attribute-row {
                    display: grid;
                    grid-template-columns: minmax(140px, 1fr) minmax(110px, 160px) 90px minmax(140px, 1fr);
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
                    .form-grid,
                    .attribute-row,
                    .attribute-editor-row {
                        grid-template-columns: 1fr;
                    }

                    .attribute-editor-heading {
                        align-items: stretch;
                        flex-direction: column;
                    }

                    .attribute-editor-row or-vaadin-button {
                        justify-self: flex-start;
                    }

                    .attribute-options {
                        grid-column: auto;
                        align-items: flex-start;
                        flex-direction: column;
                    }

                    .title-actions {
                        align-items: stretch;
                        flex-direction: column-reverse;
                    }

                    .title-actions or-vaadin-text-field {
                        width: 100% !important;
                    }
                }
            `,
        ];
    }

    protected static readonly TYPE_NAME_PATTERN = /^\w+$/;

    protected static readonly VALUE_TYPE_OPTIONS: SelectItem[] = [
        {value: WellknownValueTypes.BOOLEAN, label: "Boolean"},
        {value: WellknownValueTypes.INTEGER, label: "Integer"},
        {value: WellknownValueTypes.LONG, label: "Long"},
        {value: WellknownValueTypes.NUMBER, label: "Number"},
        {value: WellknownValueTypes.TEXT, label: "Text"},
        {value: WellknownValueTypes.DATEANDTIME, label: "Date and time"},
        {value: WellknownValueTypes.TIMESTAMP, label: "Timestamp"},
        {value: WellknownValueTypes.TIMESTAMPISO8601, label: "Timestamp ISO 8601"},
        {value: WellknownValueTypes.GEOJSONPOINT, label: "GeoJSON point"}
    ];

    protected static readonly VALUE_TYPES = new Set(
        PageCustomAssetTypes.VALUE_TYPE_OPTIONS.map(option => option.value as string)
    );

    @state()
    protected _definitions?: CustomAssetTypeDefinition[];

    @state()
    protected _definitionFilter: (definitions: CustomAssetTypeDefinition[]) => CustomAssetTypeDefinition[] = (definitions) => definitions;

    @state()
    protected _usageByName: Record<string, number> = {};

    @state()
    protected _draftDefinition?: CustomAssetTypeDefinition;

    @state()
    protected _editingDefinitionName?: string;

    @state()
    protected _savingDefinition: boolean = false;

    @state()
    protected _deletingDefinitionName?: string;

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
                        <div class="title-actions">
                            <or-vaadin-button theme="primary" ?disabled=${!!this._draftDefinition}
                                              @click=${() => this._addDraftDefinition()}>
                                <or-icon icon="plus"></or-icon>
                                <or-translate value="addCustomAssetType"></or-translate>
                            </or-vaadin-button>
                            <or-vaadin-text-field placeholder=${i18next.t("search")} style="width: 240px;"
                                                  @input=${(ev: InputEvent) => this._onDefinitionSearch(ev)}>
                                <or-icon slot="suffix" icon="magnify"></or-icon>
                            </or-vaadin-text-field>
                        </div>
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
                            ${this._draftDefinition ? this._getDraftTemplate(this._draftDefinition) : html``}
                            ${filteredDefinitions.length === 0 && !this._draftDefinition ? html`
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

    protected _addDraftDefinition(): void {
        this._editingDefinitionName = undefined;
        this._draftDefinition = {
            name: "",
            displayName: "",
            icon: "cube-outline",
            enabled: true,
            attributes: []
        };
    }

    protected async _loadDefinitions(): Promise<void> {
        this._definitions = undefined;
        this._usageByName = {};
        this._draftDefinition = undefined;
        this._editingDefinitionName = undefined;
        this._deletingDefinitionName = undefined;

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
            this._usageByName = {
                ...this._usageByName,
                ...Object.fromEntries(usageEntries)
            };
        }
    }

    protected _getDefinitionTemplate(definition: CustomAssetTypeDefinition, index: number): TemplateResult {
        const rowId = "custom-asset-type-row-" + index;
        const attributeCount = definition.attributes?.length || 0;
        const usageCount = definition.name ? this._usageByName[definition.name] : undefined;
        const deleteDisabled = !!this._draftDefinition || !!this._deletingDefinitionName || usageCount === undefined || usageCount > 0;

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
                            <div class="details-actions full-width">
                                <or-vaadin-button ?disabled=${!!this._draftDefinition}
                                                  @click=${() => this._editDefinition(definition)}>
                                    <or-icon icon="pencil"></or-icon>
                                    <or-translate value="edit"></or-translate>
                                </or-vaadin-button>
                                <or-vaadin-button theme="error" ?disabled=${deleteDisabled}
                                                  title=${this._getDeleteDefinitionTitle(usageCount)}
                                                  @click=${() => this._deleteDefinition(definition)}>
                                    <or-icon icon="delete"></or-icon>
                                    <or-translate value="delete"></or-translate>
                                </or-vaadin-button>
                            </div>
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

    protected _getDraftTemplate(definition: CustomAssetTypeDefinition): TemplateResult {
        const editing = !!this._editingDefinitionName;
        return html`
            <tr class="details-row expanded">
                <td colspan="100%">
                    <div class="details-container">
                        <div class="form-grid">
                            <or-vaadin-text-field required minlength="1" maxlength="255" pattern="\\w+"
                                                  manual-validation ?readonly=${editing} value=${definition.name || ""}
                                                  @input=${(ev: InputEvent) => this._updateDraft({name: (ev.currentTarget as OrVaadinTextField).value})}>
                                <or-translate slot="label" value="technicalName"></or-translate>
                            </or-vaadin-text-field>
                            <or-vaadin-text-field required minlength="1" maxlength="255" value=${definition.displayName || ""}
                                                  @input=${(ev: InputEvent) => this._updateDraft({displayName: (ev.currentTarget as OrVaadinTextField).value})}>
                                <or-translate slot="label" value="displayName"></or-translate>
                            </or-vaadin-text-field>
                            <or-vaadin-text-field maxlength="255" value=${definition.icon || ""}
                                                  @input=${(ev: InputEvent) => this._updateDraft({icon: (ev.currentTarget as OrVaadinTextField).value})}>
                                <or-translate slot="label" value="icon"></or-translate>
                            </or-vaadin-text-field>
                            <or-vaadin-text-field maxlength="7" value=${definition.colour || ""}
                                                  @input=${(ev: InputEvent) => this._updateDraft({colour: (ev.currentTarget as OrVaadinTextField).value})}>
                                <or-translate slot="label" value="colour"></or-translate>
                            </or-vaadin-text-field>
                            <or-vaadin-text-area class="full-width" maxlength="1024" value=${definition.description || ""}
                                                 @input=${(ev: InputEvent) => this._updateDraft({description: (ev.currentTarget as OrVaadinTextArea).value})}>
                                <or-translate slot="label" value="description"></or-translate>
                            </or-vaadin-text-area>
                            <or-vaadin-checkbox ?checked=${definition.enabled !== false}
                                                @change=${(ev: Event) => this._updateDraft({enabled: (ev.currentTarget as OrVaadinCheckbox).checked})}>
                                <label slot="label"><or-translate value="enabled"></or-translate></label>
                            </or-vaadin-checkbox>
                            ${this._getDraftAttributesTemplate(definition)}
                        </div>
                        <div class="actions">
                            <or-vaadin-button @click=${() => this._cancelDraftDefinition()}>
                                <or-translate value="cancel"></or-translate>
                            </or-vaadin-button>
                            <or-vaadin-button theme="primary" ?disabled=${!this._canSaveDefinition(definition)}
                                              @click=${() => this._saveDefinition(definition)}>
                                <or-translate value=${editing ? "save" : "create"}></or-translate>
                            </or-vaadin-button>
                        </div>
                    </div>
                </td>
            </tr>
        `;
    }

    protected _getDraftAttributesTemplate(definition: CustomAssetTypeDefinition): TemplateResult {
        return html`
            <div class="attribute-editor full-width">
                <div class="attribute-editor-heading">
                    <div class="field-label"><or-translate value="attribute_plural"></or-translate></div>
                    <or-vaadin-button @click=${() => this._addDraftAttribute()}>
                        <or-icon icon="plus"></or-icon>
                        <or-translate value="addAttribute"></or-translate>
                    </or-vaadin-button>
                </div>
                <div class="attribute-editor-list">
                    ${(definition.attributes || []).map((attribute, index) => this._getDraftAttributeTemplate(attribute, index))}
                </div>
            </div>
        `;
    }

    protected _getDraftAttributeTemplate(attribute: CustomAssetTypeAttributeDefinition, index: number): TemplateResult {
        return html`
            <div class="attribute-editor-row">
                <or-vaadin-text-field required minlength="1" maxlength="255" pattern="\\w+"
                                      manual-validation value=${attribute.name || ""}
                                      @input=${(ev: InputEvent) => this._updateDraftAttribute(index, {name: (ev.currentTarget as OrVaadinTextField).value})}>
                    <or-translate slot="label" value="attributeName"></or-translate>
                </or-vaadin-text-field>
                <or-vaadin-select required
                                  .items=${PageCustomAssetTypes.VALUE_TYPE_OPTIONS}
                                  value=${attribute.type || WellknownValueTypes.NUMBER}
                                  @change=${(ev: Event) => this._updateDraftAttribute(index, {type: (ev.currentTarget as OrVaadinSelect).value})}>
                    <or-translate slot="label" value="valueType"></or-translate>
                </or-vaadin-select>
                <or-vaadin-text-field maxlength="255" value=${this._getAttributeMetaValue<string>(attribute, WellknownMetaItems.LABEL) || ""}
                                      @input=${(ev: InputEvent) => this._updateDraftAttributeMeta(index, WellknownMetaItems.LABEL, (ev.currentTarget as OrVaadinTextField).value)}>
                    <or-translate slot="label" value="attributeLabel"></or-translate>
                </or-vaadin-text-field>
                <or-vaadin-text-field maxlength="255" value=${this._getAttributeDefaultValueText(attribute)}
                                      @input=${(ev: InputEvent) => this._updateDraftAttributeDefaultValue(index, (ev.currentTarget as OrVaadinTextField).value)}>
                    <or-translate slot="label" value="attributeDefaultValue"></or-translate>
                </or-vaadin-text-field>
                <or-vaadin-text-field maxlength="255" value=${this._getAttributeUnitsText(attribute)}
                                      @input=${(ev: InputEvent) => this._updateDraftAttributeUnits(index, (ev.currentTarget as OrVaadinTextField).value)}>
                    <or-translate slot="label" value="attributeUnits"></or-translate>
                </or-vaadin-text-field>
                <or-vaadin-button theme="icon" title=${i18next.t("delete")} @click=${() => this._removeDraftAttribute(index)}>
                    <or-icon icon="close-circle"></or-icon>
                </or-vaadin-button>
                <div class="attribute-options">
                    <or-vaadin-checkbox ?checked=${attribute.optional === true}
                                        @change=${(ev: Event) => this._updateDraftAttribute(index, {optional: (ev.currentTarget as OrVaadinCheckbox).checked})}>
                        <label slot="label"><or-translate value="optional"></or-translate></label>
                    </or-vaadin-checkbox>
                    <or-vaadin-checkbox ?checked=${this._getAttributeMetaValue<boolean>(attribute, WellknownMetaItems.READONLY) === true}
                                        @change=${(ev: Event) => this._updateDraftAttributeMeta(index, WellknownMetaItems.READONLY, (ev.currentTarget as OrVaadinCheckbox).checked)}>
                        <label slot="label"><or-translate value="readOnly"></or-translate></label>
                    </or-vaadin-checkbox>
                    <or-vaadin-checkbox ?checked=${this._getAttributeMetaValue<boolean>(attribute, WellknownMetaItems.STOREDATAPOINTS) === true}
                                        @change=${(ev: Event) => this._updateDraftAttributeMeta(index, WellknownMetaItems.STOREDATAPOINTS, (ev.currentTarget as OrVaadinCheckbox).checked)}>
                        <label slot="label"><or-translate value="storeDataPoints"></or-translate></label>
                    </or-vaadin-checkbox>
                </div>
            </div>
        `;
    }

    protected _getAttributeTemplate(attribute: CustomAssetTypeAttributeDefinition): TemplateResult {
        return html`
            <div class="attribute-row">
                <span>${attribute.name}</span>
                <span>${attribute.type}</span>
                <span><or-translate value=${attribute.optional ? "optional" : "required"}></or-translate></span>
                <span>${this._getAttributeMetadataSummary(attribute)}</span>
            </div>
        `;
    }

    protected _updateDraft(patch: Partial<CustomAssetTypeDefinition>): void {
        if (this._draftDefinition) {
            this._draftDefinition = {...this._draftDefinition, ...patch};
        }
    }

    protected _editDefinition(definition: CustomAssetTypeDefinition): void {
        this._editingDefinitionName = definition.name;
        this._draftDefinition = this._copyDefinitionForDraft(definition);
    }

    protected _copyDefinitionForDraft(definition: CustomAssetTypeDefinition): CustomAssetTypeDefinition {
        return {
            ...definition,
            attributes: (definition.attributes || []).map(attribute => ({
                ...attribute,
                units: attribute.units ? [...attribute.units] : undefined,
                constraints: attribute.constraints ? [...attribute.constraints] : undefined,
                meta: attribute.meta ? {...attribute.meta} : undefined
            }))
        };
    }

    protected _cancelDraftDefinition(): void {
        this._draftDefinition = undefined;
        this._editingDefinitionName = undefined;
    }

    protected _addDraftAttribute(): void {
        const attributes = [...(this._draftDefinition?.attributes || [])];
        attributes.push({
            name: "",
            type: WellknownValueTypes.NUMBER,
            optional: false,
            position: this._nextDraftAttributePosition(attributes)
        });
        this._updateDraft({attributes});
    }

    protected _nextDraftAttributePosition(attributes: CustomAssetTypeAttributeDefinition[]): number {
        return attributes.reduce((position, attribute) => Math.max(position, attribute.position ?? 0), -10) + 10;
    }

    protected _updateDraftAttribute(index: number, patch: Partial<CustomAssetTypeAttributeDefinition>): void {
        const attributes = [...(this._draftDefinition?.attributes || [])];
        if (!attributes[index]) {
            return;
        }

        attributes[index] = {...attributes[index], ...patch};
        this._updateDraft({attributes});
    }

    protected _removeDraftAttribute(index: number): void {
        const attributes = [...(this._draftDefinition?.attributes || [])];
        attributes.splice(index, 1);
        this._updateDraft({attributes});
    }

    protected _updateDraftAttributeMeta(index: number, name: WellknownMetaItems, value: string | boolean): void {
        const attributes = [...(this._draftDefinition?.attributes || [])];
        const attribute = attributes[index];
        if (!attribute) {
            return;
        }

        const meta = {...(attribute.meta || {})};
        const cleanedValue = typeof value === "string" ? this._optionalText(value) : value;
        if (cleanedValue === undefined || cleanedValue === false) {
            delete meta[name];
        } else {
            meta[name] = cleanedValue;
        }

        attributes[index] = {
            ...attribute,
            meta: Object.keys(meta).length > 0 ? meta : undefined
        };
        this._updateDraft({attributes});
    }

    protected _updateDraftAttributeUnits(index: number, unitsText: string): void {
        this._updateDraftAttribute(index, {units: this._parseAttributeUnits(unitsText)});
    }

    protected _updateDraftAttributeDefaultValue(index: number, defaultValueText: string): void {
        const attribute = this._draftDefinition?.attributes?.[index];
        this._updateDraftAttribute(index, {
            defaultValue: this._parseAttributeDefaultValue(defaultValueText, attribute?.type)
        });
    }

    protected _canSaveDefinition(definition: CustomAssetTypeDefinition): boolean {
        const name = (definition.name || "").trim();
        const displayName = (definition.displayName || "").trim();
        return !this._savingDefinition
            && !!displayName
            && PageCustomAssetTypes.TYPE_NAME_PATTERN.test(name)
            && (!this._editingDefinitionName || name === this._editingDefinitionName)
            && !(this._definitions || []).some(existing => existing.name === name && existing.name !== this._editingDefinitionName)
            && this._areDraftAttributesValid(definition.attributes || []);
    }

    protected _areDraftAttributesValid(attributes: CustomAssetTypeAttributeDefinition[]): boolean {
        const names = new Set<string>();

        return attributes.every(attribute => {
            const name = (attribute.name || "").trim();
            const type = attribute.type || "";
            if (!PageCustomAssetTypes.TYPE_NAME_PATTERN.test(name) || !PageCustomAssetTypes.VALUE_TYPES.has(type)) {
                return false;
            }

            if (names.has(name)) {
                return false;
            }

            names.add(name);
            return true;
        });
    }

    protected async _saveDefinition(definition: CustomAssetTypeDefinition): Promise<void> {
        if (this._editingDefinitionName) {
            await this._updateDefinition(definition);
        } else {
            await this._createDefinition(definition);
        }
    }

    protected async _createDefinition(definition: CustomAssetTypeDefinition, confirmExistingAssets = false): Promise<void> {
        if (!this._canSaveDefinition(definition) && !confirmExistingAssets) {
            return;
        }

        this._savingDefinition = true;
        const definitionToCreate = this._toDefinitionPayload(definition);

        try {
            const response = await manager.rest.api.CustomAssetTypeResource.create(
                definitionToCreate,
                {confirmExistingAssets}
            );

            if (!this.isConnected) {
                return;
            }

            this._draftDefinition = undefined;
            this._editingDefinitionName = undefined;
            this._definitions = [
                ...(this._definitions || []),
                response.data
            ].sort((a, b) => (a.name || "").localeCompare(b.name || ""));
            await this._loadUsageCounts([response.data]);
            await this._refreshAssetModel();
            showSnackbar(undefined, i18next.t("saveCustomAssetTypeSucceeded"));
        } catch (e) {
            if (this._getErrorStatus(e) === 409 && !confirmExistingAssets) {
                const ok = await showOkCancelDialog(
                    i18next.t("confirmCustomAssetTypeFallbackTitle"),
                    i18next.t("confirmCustomAssetTypeFallback"),
                    i18next.t("create")
                );
                if (ok) {
                    await this._createDefinition(definition, true);
                }
            } else {
                console.error("Failed to create custom asset type definition", e);
                showSnackbar(undefined, i18next.t("saveCustomAssetTypeFailed"), "dismiss");
            }
        } finally {
            if (this.isConnected) {
                this._savingDefinition = false;
            }
        }
    }

    protected async _updateDefinition(definition: CustomAssetTypeDefinition): Promise<void> {
        if (!this._editingDefinitionName || !this._canSaveDefinition(definition)) {
            return;
        }

        this._savingDefinition = true;
        const definitionToUpdate = this._toDefinitionPayload(definition);

        try {
            const response = await manager.rest.api.CustomAssetTypeResource.update(
                this._editingDefinitionName,
                definitionToUpdate
            );

            if (!this.isConnected) {
                return;
            }

            this._draftDefinition = undefined;
            this._editingDefinitionName = undefined;
            this._definitions = (this._definitions || [])
                .map(existing => existing.name === response.data.name ? response.data : existing)
                .sort((a, b) => (a.name || "").localeCompare(b.name || ""));
            await this._loadUsageCounts([response.data]);
            await this._refreshAssetModel();
            showSnackbar(undefined, i18next.t("saveCustomAssetTypeSucceeded"));
        } catch (e) {
            console.error("Failed to update custom asset type definition", e);
            showSnackbar(undefined, i18next.t("saveCustomAssetTypeFailed"), "dismiss");
        } finally {
            if (this.isConnected) {
                this._savingDefinition = false;
            }
        }
    }

    protected async _deleteDefinition(definition: CustomAssetTypeDefinition): Promise<void> {
        const name = definition.name;
        if (!name || this._draftDefinition || this._deletingDefinitionName || this._usageByName[name] !== 0) {
            return;
        }

        const ok = await showOkCancelDialog(
            i18next.t("deleteCustomAssetType"),
            i18next.t("deleteCustomAssetTypeConfirm", {name}),
            i18next.t("delete")
        );
        if (!ok) {
            return;
        }

        this._deletingDefinitionName = name;

        try {
            await manager.rest.api.CustomAssetTypeResource.delete(name);

            if (!this.isConnected) {
                return;
            }

            this._definitions = (this._definitions || []).filter(existing => existing.name !== name);
            const usageByName = {...this._usageByName};
            delete usageByName[name];
            this._usageByName = usageByName;
            await this._refreshAssetModel();
            showSnackbar(undefined, i18next.t("deleteCustomAssetTypeSucceeded"));
        } catch (e) {
            console.error("Failed to delete custom asset type definition", e);
            const message = this._getErrorStatus(e) === 409 ? "deleteCustomAssetTypeInUse" : "deleteCustomAssetTypeFailed";
            showSnackbar(undefined, i18next.t(message), "dismiss");
        } finally {
            if (this.isConnected) {
                this._deletingDefinitionName = undefined;
            }
        }
    }

    protected _toDefinitionPayload(definition: CustomAssetTypeDefinition): CustomAssetTypeDefinition {
        return {
            name: definition.name!.trim(),
            displayName: definition.displayName!.trim(),
            icon: this._optionalText(definition.icon),
            colour: this._optionalColour(definition.colour),
            description: this._optionalText(definition.description),
            enabled: definition.enabled !== false,
            attributes: (definition.attributes || []).map((attribute, index) => ({
                name: attribute.name!.trim(),
                type: attribute.type,
                optional: attribute.optional === true,
                defaultValue: this._normaliseAttributeDefaultValue(attribute),
                units: this._normaliseAttributeUnits(attribute.units),
                format: attribute.format,
                constraints: attribute.constraints,
                meta: this._normaliseAttributeMeta(attribute.meta),
                position: attribute.position ?? index * 10
            }))
        };
    }

    protected _optionalText(value?: string): string | undefined {
        const trimmed = value?.trim();
        return trimmed ? trimmed : undefined;
    }

    protected _optionalColour(value?: string): string | undefined {
        const colour = this._optionalText(value);
        return colour?.startsWith("#") ? colour.substring(1) : colour;
    }

    protected _getAttributeMetaValue<T>(attribute: CustomAssetTypeAttributeDefinition, name: WellknownMetaItems): T | undefined {
        return attribute.meta?.[name] as T | undefined;
    }

    protected _getAttributeUnitsText(attribute: CustomAssetTypeAttributeDefinition): string {
        return (attribute.units || []).join(", ");
    }

    protected _getAttributeDefaultValueText(attribute: CustomAssetTypeAttributeDefinition): string {
        const defaultValue = attribute.defaultValue;
        if (defaultValue === undefined || defaultValue === null) {
            return "";
        }
        if (typeof defaultValue === "object") {
            return JSON.stringify(defaultValue);
        }
        return String(defaultValue);
    }

    protected _parseAttributeDefaultValue(defaultValueText?: string, type?: string): any {
        const text = this._optionalText(defaultValueText);
        if (text === undefined) {
            return undefined;
        }

        if (type === WellknownValueTypes.BOOLEAN) {
            const lowerText = text.toLowerCase();
            if (lowerText === "true") {
                return true;
            }
            if (lowerText === "false") {
                return false;
            }
        }

        if (type === WellknownValueTypes.INTEGER || type === WellknownValueTypes.LONG) {
            return /^-?\d+$/.test(text) ? Number(text) : text;
        }

        if (type === WellknownValueTypes.NUMBER) {
            const numberValue = Number(text);
            return Number.isFinite(numberValue) ? numberValue : text;
        }

        if (type === WellknownValueTypes.GEOJSONPOINT && (text.startsWith("{") || text.startsWith("["))) {
            try {
                return JSON.parse(text);
            } catch (e) {
                return text;
            }
        }

        return text;
    }

    protected _parseAttributeUnits(unitsText?: string): string[] | undefined {
        const units = (unitsText || "")
            .split(",")
            .map(unit => unit.trim())
            .filter(unit => !!unit);
        return units.length > 0 ? units : undefined;
    }

    protected _normaliseAttributeUnits(units?: string[]): string[] | undefined {
        return this._parseAttributeUnits((units || []).join(","));
    }

    protected _normaliseAttributeDefaultValue(attribute: CustomAssetTypeAttributeDefinition): any {
        return this._parseAttributeDefaultValue(this._getAttributeDefaultValueText(attribute), attribute.type);
    }

    protected _normaliseAttributeMeta(meta?: {[index: string]: any}): {[index: string]: any} | undefined {
        const normalisedMeta: {[index: string]: any} = {};
        const label = this._optionalText(meta?.[WellknownMetaItems.LABEL]);
        if (label) {
            normalisedMeta[WellknownMetaItems.LABEL] = label;
        }
        if (meta?.[WellknownMetaItems.READONLY] === true) {
            normalisedMeta[WellknownMetaItems.READONLY] = true;
        }
        if (meta?.[WellknownMetaItems.STOREDATAPOINTS] === true) {
            normalisedMeta[WellknownMetaItems.STOREDATAPOINTS] = true;
        }
        return Object.keys(normalisedMeta).length > 0 ? normalisedMeta : undefined;
    }

    protected _getAttributeMetadataSummary(attribute: CustomAssetTypeAttributeDefinition): string {
        const summary = [];
        const label = this._getAttributeMetaValue<string>(attribute, WellknownMetaItems.LABEL);
        const units = this._getAttributeUnitsText(attribute);
        if (label) {
            summary.push(i18next.t("attributeLabel") + ": " + label);
        }
        if (units) {
            summary.push(i18next.t("attributeUnits") + ": " + units);
        }
        if (attribute.defaultValue !== undefined && attribute.defaultValue !== null) {
            summary.push(i18next.t("attributeDefaultValue") + ": " + this._getAttributeDefaultValueText(attribute));
        }
        if (this._getAttributeMetaValue<boolean>(attribute, WellknownMetaItems.READONLY) === true) {
            summary.push(i18next.t("readOnly"));
        }
        if (this._getAttributeMetaValue<boolean>(attribute, WellknownMetaItems.STOREDATAPOINTS) === true) {
            summary.push(i18next.t("storeDataPoints"));
        }
        return summary.join(" | ");
    }

    protected _getDeleteDefinitionTitle(usageCount?: number): string {
        if (usageCount === undefined) {
            return i18next.t("loading");
        }
        if (usageCount > 0) {
            return i18next.t("deleteCustomAssetTypeInUse");
        }
        return i18next.t("delete");
    }

    protected _getErrorStatus(error: any): number | undefined {
        return error?.response?.status;
    }

    protected async _refreshAssetModel(): Promise<void> {
        const [assetInfosResponse, metaItemDescriptorResponse, valueDescriptorResponse] = await Promise.all([
            manager.rest.api.AssetModelResource.getAssetInfos(),
            manager.rest.api.AssetModelResource.getMetaItemDescriptors(),
            manager.rest.api.AssetModelResource.getValueDescriptors()
        ]);

        AssetModelUtil._assetTypeInfos = assetInfosResponse.data;
        AssetModelUtil._metaItemDescriptors = Object.values(metaItemDescriptorResponse.data);
        AssetModelUtil._valueDescriptors = Object.values(valueDescriptorResponse.data);
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
