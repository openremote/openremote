import {css, customElement, html, LitElement, property, TemplateResult, unsafeCSS} from "lit-element";
import {InputType, OrInput, OrInputChangedEvent} from "@openremote/or-input";
import i18next from "i18next";
import {Asset, Attribute, MetaItem, ValueType} from "@openremote/model";
import {AssetModelUtil, DefaultColor5, Util} from "@openremote/core";
import "@openremote/or-input";
import {OrIcon} from "@openremote/or-icon";
import {showDialog, OrMwcDialog, DialogAction} from "@openremote/or-mwc-components/dist/or-mwc-dialog";
import {ListItem, ListType, OrMwcList} from "@openremote/or-mwc-components/dist/or-mwc-list";
import "./or-add-attribute-panel";
import {getField, getPanel, getPropertyTemplate} from "./index";
import {OrAddAttributePanelAttributeChangedEvent} from "./or-add-attribute-panel";
import {panelStyles} from "./style";
import { OrAssetTree, UiAssetTreeNode } from "@openremote/or-asset-tree";

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const tableStyle = require("!!raw-loader!@material/data-table/dist/mdc.data-table.css");

// language=CSS
const style = css`
    .panel {
        margin: 10px auto;
        max-width: 1200px;    
    }                
    #parent-edit-wrapper {
        display: flex;
        align-items: center;
    }
    
    #parent-edit-wrapper > #property-parentId {
        width: 100%;
    }
    
    #change-parent-btn {
        margin-left: 20px;
    }
    .mdc-data-table__row:hover {
        background-color: inherit !important;
    }
    #attribute-table {
        width: 100%;
    }
    .expander-cell {
        --or-icon-width: 18px;
        --or-icon-height: 18px;
        cursor: pointer;
    }
    .expander-cell > * {
        pointer-events: none;
    }
    .expander-cell > or-icon {
        margin-right: 16px;
    }
    .actions-cell {
        text-align: right;
        width: 40px;
    }
    .meta-item-container {
        overflow: hidden;
        max-height: 0;
        transition: max-height 0.25s ease-out;
        padding: 0 20px 0 50px;
    }
    .attribute-meta-row.expanded .meta-item-container {
        max-height: 1000px;
        transition: max-height 1s ease-in;
    }
    .meta-item-container or-input {
        width: 100%;
    }
    .meta-item-wrapper {
        display: flex;
        margin: 10px 0;
    }
    .meta-item-wrapper:first-child {
        margin-top: 0;
    }
    .meta-item-wrapper:hover .button-clear {
        visibility: visible;                    
    }
    .item-add {
        margin-bottom: 10px;                
    }                    
    .button-clear {
        background: none;
        color: ${unsafeCSS(DefaultColor5)};
        --or-icon-fill: ${unsafeCSS(DefaultColor5)};
        visibility: hidden;
        display: inline-block;
        border: none;
        padding: 0;
        cursor: pointer;
    }                
    .button-clear:hover {
        --or-icon-fill: var(--internal-or-asset-viewer-button-color);
    }                
    .button-clear:focus {
        outline: 0;
    }                
    .button-clear.hidden {
        visibility: hidden;
    }
    .padded-cell {
        padding: 10px 0;
    }
    .overflow-visible {
        overflow: visible;
    }
`;

export class OrEditAssetModifiedEvent extends CustomEvent<void> {

    public static readonly NAME = "or-edit-asset-modified";

    constructor() {
        super(OrEditAssetModifiedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrEditAssetModifiedEvent.NAME]: OrEditAssetModifiedEvent;
    }
}

@customElement("or-edit-asset-panel")
export class OrEditAssetPanel extends LitElement {

    @property({attribute: false})
    protected asset!: Asset;

    @property({attribute: false})
    protected attrs!: Attribute[];

    public static get styles() {
        return [
            unsafeCSS(tableStyle),
            panelStyles,
            style
        ];
    }

    protected render() {

        const updatePublicRead = (publicRead: boolean) => {
            this.asset.accessPublicRead = publicRead;
            this._onModified();
        };

        // Properties panel fields
        const properties: TemplateResult[] = [
            getField("type", undefined, getPropertyTemplate(this.asset, "type", this, undefined, undefined, {readonly: true, label: i18next.t("assetType")})),
            getField("parent", undefined, this._getParentTemplate()),
            html`<div @or-input-changed="${(ev: OrInputChangedEvent) => updatePublicRead(ev.detail.value as boolean)}">
                    ${getField("accessPublicRead", undefined, getPropertyTemplate(this.asset, "accessPublicRead", this, undefined, undefined, {readonly: false, label: i18next.t("accessPublicRead")}))}
                </div>`
        ];

        const expanderToggle = (ev: MouseEvent) => {
            const tdElem = ev.target as HTMLElement;
            if (tdElem.className.indexOf("expander-cell") < 0) {
                return;
            }
            const expanderIcon = tdElem.getElementsByTagName("or-icon")[0] as OrIcon;
            const headerRow = tdElem.parentElement! as HTMLTableRowElement;
            const metaRow = (headerRow.parentElement! as HTMLTableElement).rows[headerRow.rowIndex];

            if (expanderIcon.icon === "chevron-right") {
                expanderIcon.icon = "chevron-down";
                metaRow.classList.add("expanded");
            } else {
                expanderIcon.icon = "chevron-right";
                metaRow.classList.remove("expanded");
            }
        };

        const attributes = html`
            <div id="attribute-table" class="mdc-data-table">
                <table class="mdc-data-table__table" aria-label="attribute list" @click="${expanderToggle}">
                    <thead>
                        <tr class="mdc-data-table__header-row">
                            <th class="mdc-data-table__header-cell" role="columnheader" scope="col"><or-translate value="name"></or-translate></th>
                            <th class="mdc-data-table__header-cell" role="columnheader" scope="col"><or-translate value="type"></or-translate></th>
                            <th class="mdc-data-table__header-cell" role="columnheader" scope="col"><or-translate value="value"></or-translate></th>
                            <th class="mdc-data-table__header-cell" role="columnheader" scope="col"></or-translate></th>
                        </tr>
                    </thead>
                    <tbody class="mdc-data-table__content">
                        ${this.attrs!.sort(Util.sortByString((attr: Attribute) => attr.name!.toUpperCase())).map((attr) => this._getEditAttributeTemplate(this.asset.type!, attr))}
                        <tr>
                            <td colspan="4">
                                <div class="item-add">
                                    <or-input .type="${InputType.BUTTON}" .label="${i18next.t("addAttribute")}" icon="plus" @click="${() => this._addAttribute()}"></or-input>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        `;

        return html`
            <div id="edit-wrapper">
                ${getPanel("properties", {}, html`${properties}`) || ``}
                ${getPanel("attributes", {}, html`${attributes}`) || ``}
            </div>
        `;
    }

    protected _getEditAttributeTemplate(assetType: string, attribute: Attribute): TemplateResult {

        const deleteAttribute = () => {
            this.attrs!.splice(this.attrs!.indexOf(attribute), 1);
            this._onModified();
        };

        return html`
            <tr class="mdc-data-table__row">
                <td class="padded-cell mdc-data-table__cell expander-cell"><or-icon icon="chevron-right"></or-icon><span>${attribute.name}</span></td>
                <td class="padded-cell mdc-data-table__cell">${attribute.type}</td>
                <td class="padded-cell overflow-visible mdc-data-table__cell">
                    <or-attribute-input compact .assetType="${assetType}" .label=${null} .readonly="${false}" .attribute="${attribute}" disableWrite disableSubscribe disableButton></or-attribute-input>
                </td>
                <td class="padded-cell mdc-data-table__cell actions-cell"><or-input type="${InputType.BUTTON}" icon="delete" @click="${deleteAttribute}"></td>
            </tr>
            <tr class="attribute-meta-row">
                <td colspan="4">
                    <div class="meta-item-container">
                        <div>
                            ${!attribute.meta ? `` : attribute.meta.sort(Util.sortByString((metaItem) => metaItem.name!)).map((metaItem) => this._getMetaItemTemplate(attribute, metaItem))}
                        </div>
                        <div class="item-add">
                            <or-input .type="${InputType.BUTTON}" .label="${i18next.t("addMetaItems")}" icon="plus" @click="${() => this._addMetaItems(attribute)}"></or-input>
                        </div>
                    </div>                     
                </td>
            </tr>
        `;
    };

    protected _onModified() {
        this.dispatchEvent(new OrEditAssetModifiedEvent());
        this.requestUpdate();
    }

    protected _onMetaItemModified(metaItem: MetaItem, value: any) {
        metaItem.value = value;
        this._onModified();
    }

    protected _getMetaItemTemplate(attribute: Attribute, metaItem: MetaItem): TemplateResult {

        const removeMetaItem = () => {
            attribute.meta!.splice(attribute.meta!.indexOf(metaItem), 1);
            this._onModified();
        };

        const descriptor = AssetModelUtil.getMetaItemDescriptor(metaItem.name);
        let valueType = descriptor ? descriptor .valueType : undefined;
        let inputType = InputType.TEXT;
        if (!valueType && metaItem.value !== undefined && metaItem.value !== null) {
            switch (typeof metaItem.value) {
                case "object":
                    if (Array.isArray(metaItem.value)) {
                        valueType = ValueType.ARRAY;
                    } else {
                        valueType = ValueType.OBJECT;
                    }
                    break;
                case "boolean":
                    valueType = ValueType.BOOLEAN;
                    break;
                case "number":
                    valueType = ValueType.NUMBER;
                    break;
                case "string":
                    valueType = ValueType.STRING;
                    break;
            }
        }

        if (valueType) {
            switch (valueType) {
                case ValueType.NUMBER:
                    inputType = InputType.NUMBER;
                    break;
                case ValueType.BOOLEAN:
                    inputType = InputType.CHECKBOX;
                    break;
                case ValueType.ARRAY:
                case ValueType.OBJECT:
                    inputType = InputType.JSON;
                    break;
            }
        }
        const readonly = descriptor && !!descriptor.valueFixed;
        return html`
                <div class="meta-item-wrapper">
                    <or-input .label=${Util.getMetaItemLabel(metaItem.name!)} .type="${inputType}" .value="${metaItem.value}" @or-input-changed="${(e: OrInputChangedEvent) => this._onMetaItemModified(metaItem, e.detail.value)}" ?disabled="${readonly}"></or-input>
                    <button class="button-clear" @click="${removeMetaItem}"><or-icon icon="close-circle"></or-icon></input>
                </div>
            `;
    }

    protected _addAttribute() {

        const asset = this.asset!;
        let attr: Attribute;

        const onAttributeChanged = (attribute: Attribute) => {
            const addDisabled = !(attribute.name && !asset.attributes![attribute.name] && attribute.type);
            const addBtn = dialog!.shadowRoot!.getElementById("add-btn") as OrInput;
            addBtn!.disabled = addDisabled;
            attr = attribute;
        };

        const dialog = showDialog({
            content: html`
                <or-add-attribute-panel .asset="${asset}" @or-add-attribute-panel-attribute-changed="${(ev: OrAddAttributePanelAttributeChangedEvent) => onAttributeChanged(ev.detail)}"></or-add-attribute-panel>
            `,
            styles: html`
                <style>
                    .mdc-dialog__surface {
                        overflow-x: visible !important;
                        overflow-y: visible !important;
                    }
                    #dialog-content {
                        border-color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
                        border-top-width: 1px;
                        border-top-style: solid;
                        border-bottom-width: 1px;
                        border-bottom-style: solid;
                        padding: 0;
                        overflow: visible;
                    }
                </style>
            `,
            title: i18next.t("addAttribute"),
            actions: [
                {
                    default: true,
                    actionName: "add",
                    action: () => {
                        this.attrs!.push(attr);
                        this._onModified();
                    },
                    content: html`<or-input id="add-btn" .type="${InputType.BUTTON}" disabled .label="${i18next.t("add")}"></or-input>`
                },
                {
                    actionName: "cancel",
                    content: i18next.t("cancel")
                }
            ],
            dismissAction: null
        });
    }

    protected _addMetaItems(attribute: Attribute) {

        const metaItemList: (ListItem | null)[] = AssetModelUtil.getMetaItemDescriptors()
            .filter((descriptor) => !attribute.meta!.some((m) => m.name === descriptor.urn))
            .map((descriptor) => {
                return {
                    text: Util.getMetaItemLabel(descriptor.urn!),
                    value: descriptor.urn!,
                    translate: false
                };
            }).sort(Util.sortByString((item) => item.text));

        const dialog = showDialog({
            content: html`
                <div id="meta-creator">
                    <or-mwc-list id="meta-creator-list" .type="${ListType.CHECKBOX}" .listItems="${metaItemList}"></or-mwc-list>
                </div>
            `,
            styles: html`
                <style>
                    #dialog-content {
                        border-color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
                        border-top-width: 1px;
                        border-top-style: solid;
                        border-bottom-width: 1px;
                        border-bottom-style: solid;
                    }
                    #meta-creator {
                        height: 600px;
                        max-height: 100%;
                    }
                    
                    #meta-creator > or-mwc-list {
                        height: 100%;
                    }
                </style>
            `,
            title: i18next.t("addMetaItems"),
            actions: [
                {
                    default: true,
                    actionName: "add",
                    action: () => {
                        const list = dialog!.shadowRoot!.getElementById("meta-creator-list") as OrMwcList;
                        const selectedItems = list ? list.selectedItems : undefined;
                        if (selectedItems) {
                            if (!attribute.meta) {
                                attribute.meta = [];
                            }
                            selectedItems.forEach((item) => {
                                const descriptor = AssetModelUtil.getMetaItemDescriptors().find((descriptor) => descriptor.urn === item.value);
                                if (descriptor) {
                                    attribute.meta!.push(
                                        {
                                            name: descriptor.urn,
                                            value: descriptor.initialValue
                                        }
                                    );
                                    this._onModified();
                                }
                            });
                        }
                    },
                    content: i18next.t("add")
                },
                {
                    actionName: "cancel",
                    content: i18next.t("cancel")
                }
            ],
            dismissAction: null
        });
    }

    protected _getParentTemplate() {
        const showDialog = () => {
            const dialog = this.shadowRoot!.getElementById("asset-picker-modal") as OrMwcDialog;
            dialog.open();
        };
        const viewer = this;

        const setParent = () => {
            const dialog = this.shadowRoot!.getElementById("asset-picker-modal") as OrMwcDialog;
            const assetTree = dialog.shadowRoot!.getElementById("parent-asset-tree") as OrAssetTree;
            this.asset.parentId = assetTree.selectedIds!.length === 1 ? assetTree.selectedIds![0] : undefined;
            // Need to update the assets path as well
            const path = [this.asset.id!];
            let parentNode = assetTree.selectedNodes[0];
            while (parentNode !== undefined) {
                path.push(parentNode.asset!.id!);
                parentNode = parentNode.parent;
            }
            this.asset.path = path;
            this._onModified();
        };

        const clearParent = () => {
            this.asset.parentId = undefined;
            this.asset.path = [this.asset.id!];
            this._onModified();
        };

        const blockEvent = (ev: Event) => {
            ev.stopPropagation();
        };

        const parentSelector = (node: UiAssetTreeNode) => node.asset!.id !== this.asset.id;

        // Prevent change event from bubbling up as it will affect any ancestor listeners that are interested in a different asset tree
        const dialogContent = html`<or-asset-tree id="parent-asset-tree" .selectedIds="${this.asset!.parentId ? [this.asset.parentId] : []}" @or-asset-tree-request-select="${blockEvent}" @or-asset-tree-selection-changed="${blockEvent}" .selector="${parentSelector}"></or-asset-tree>`;

        const dialogActions: DialogAction[] = [
            {
                actionName: "clear",
                content: i18next.t("none"),
                action: clearParent
            },
            {
                actionName: "ok",
                content: i18next.t("ok"),
                action: setParent
            },
            {
                default: true,
                actionName: "cancel",
                content: i18next.t("cancel")
            }
        ];

        return html`
            <div id="parent-edit-wrapper">
                ${getPropertyTemplate(this.asset, "parentId", this, undefined, undefined, {readonly: false, label: i18next.t("parent")})}
                <or-input id="change-parent-btn" type="${InputType.BUTTON}" raised .label="${i18next.t("edit")}" @or-input-changed="${showDialog}"></or-input>
            </div>
            <or-mwc-dialog id="asset-picker-modal" .dialogContent="${dialogContent}" .dialogActions="${dialogActions}" .multiSelect="${false}"></or-mwc-dialog>
        `;
    }
}
