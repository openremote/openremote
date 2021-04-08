import {css, customElement, html, LitElement, property, TemplateResult, unsafeCSS} from "lit-element";
import {InputType, OrInput, OrInputChangedEvent, getValueHolderInputTemplateProvider, ValueInputProviderOptions, OrInputChangedEventDetail} from "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import {Asset, Attribute, NameValueHolder} from "@openremote/model";
import {AssetModelUtil, DefaultColor5, DefaultColor3, Util} from "@openremote/core";
import "@openremote/or-mwc-components/or-mwc-input";
import {OrIcon} from "@openremote/or-icon";
import {showDialog, OrMwcDialog, DialogAction} from "@openremote/or-mwc-components/or-mwc-dialog";
import {ListItem, ListType, OrMwcList} from "@openremote/or-mwc-components/or-mwc-list";
import "./or-add-attribute-panel";
import {getField, getPanel, getPropertyTemplate} from "./index";
import {
    OrAddAttributePanelAttributeChangedEvent,
} from "./or-add-attribute-panel";
import {panelStyles} from "./style";
import { OrAssetTree, UiAssetTreeNode } from "@openremote/or-asset-tree";
import { OrAttributeInputChangedEvent } from "@openremote/or-attribute-input";

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

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

    .mdc-data-table__row {
        border-top-color: #D3D3D3;
    }

    #attribute-table {
        width: 100%;
    }

    .mdc-data-table__header-cell {
        font-weight: bold;
        color: ${unsafeCSS(DefaultColor3)};
    }

    .mdc-data-table__header-cell:first-child {
        padding-left: 36px;
    }
    .expander-cell {
        --or-icon-width: 20px;
        --or-icon-height: 20px;
        cursor: pointer;
    }
    .expander-cell > * {
        pointer-events: none;
    }
    .expander-cell > or-icon {
        vertical-align: middle;
        margin-right: 6px;
        margin-left: -5px;
    }
    .padded-cell {
        padding: 10px 16px;
    }
    .actions-cell {
        text-align: right;
        width: 40px;
        padding-right: 5px;
    }
    .meta-item-container {
        overflow: hidden;
        max-height: 0;
        transition: max-height 0.25s ease-out;
        padding: 0 16px 0 36px;
    }
    .attribute-meta-row.expanded .meta-item-container {
        max-height: 1000px;
        transition: max-height 1s ease-in;
    }
    .meta-item-container or-mwc-input {
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
    .item-add-attribute {
        margin: 10px 0px 10px 4px;
    }
    .button-clear {
        background: none;
        color: ${unsafeCSS(DefaultColor5)};
        --or-icon-fill: ${unsafeCSS(DefaultColor5)};
        visibility: hidden;
        display: inline-block;
        border: none;
        padding: 0 0 0 5px;
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

const AssetNameRegex = /^\w+$/;

@customElement("or-edit-asset-panel")
export class OrEditAssetPanel extends LitElement {

    @property({attribute: false})
    protected asset!: Asset;

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
            html`<div @or-mwc-input-changed="${(ev: OrInputChangedEvent) => updatePublicRead(ev.detail.value as boolean)}">
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
                        ${!this.asset.attributes ? `` : Object.entries(this.asset.attributes!).sort(Util.sortByString(([name, attribute]) => name.toUpperCase())).map(([name, attribute]) => {attribute.name = name; return this._getEditAttributeTemplate(this.asset.type!, attribute as Attribute<any>);})}
                        <tr class="mdc-data-table__row">
                            <td colspan="4">
                                <div class="item-add-attribute">
                                    <or-mwc-input .type="${InputType.BUTTON}" .label="${i18next.t("addAttribute")}" icon="plus" @click="${() => this._addAttribute()}"></or-mwc-input>
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

    protected _getEditAttributeTemplate(assetType: string, attribute: Attribute<any>): TemplateResult {

        const deleteAttribute = () => {
            delete this.asset.attributes![attribute.name!];
            this._onModified();
        };

        const descriptor = AssetModelUtil.getAttributeDescriptor(attribute.name!, assetType);
        const canDelete = !descriptor || descriptor.optional;

        return html`
            <tr class="mdc-data-table__row">
                <td class="padded-cell mdc-data-table__cell expander-cell"><or-icon icon="chevron-right"></or-icon><span>${attribute.name}</span></td>
                <td class="padded-cell mdc-data-table__cell">${Util.getValueDescriptorLabel(attribute.type!)}</td>
                <td class="padded-cell overflow-visible mdc-data-table__cell">
                    <or-attribute-input compact .assetType="${assetType}" .label=${null} .readonly="${false}" .attribute="${attribute}" .assetId="${this.asset.id!}" disableWrite disableSubscribe disableButton @or-attribute-input-changed="${(e: OrAttributeInputChangedEvent) => this._onAttributeModified(attribute, e.detail.value)}"></or-attribute-input>
                </td>
                <td class="padded-cell mdc-data-table__cell actions-cell">${canDelete ? html`<or-mwc-input type="${InputType.BUTTON}" icon="delete" @click="${deleteAttribute}">` : ``}</td>
            </tr>
            <tr class="attribute-meta-row">
                <td colspan="4">
                    <div class="meta-item-container">
                        <div>
                            ${!attribute.meta ? `` : Object.entries(attribute.meta).sort(Util.sortByString(([name, value]) => name!)).map(([name, value]) => this._getMetaItemTemplate(attribute, Util.getMetaItemNameValueHolder(name, value)))}
                        </div>
                        <div class="item-add">
                            <or-mwc-input .type="${InputType.BUTTON}" .label="${i18next.t("addMetaItems")}" icon="plus" @click="${() => this._addMetaItems(attribute)}"></or-mwc-input>
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

    protected _onAttributeModified(attribute: Attribute<any>, newValue: any) {
        attribute.value = newValue;
        attribute.timestamp = undefined; // Clear timestamp so server will set this
        this._onModified();
    }

    protected _onMetaItemModified(attribute: Attribute<any>, metaItem: NameValueHolder<any>, detail: OrInputChangedEventDetail) {
        metaItem.value = detail.value;
        attribute.meta![metaItem.name!] = detail.value;
        this._onModified();
    }

    protected _getMetaItemTemplate(attribute: Attribute<any>, metaItem: NameValueHolder<any>): TemplateResult {

        const removeMetaItem = () => {
            delete attribute.meta![metaItem.name!];
            this._onModified();
        };

        const descriptor = AssetModelUtil.getMetaItemDescriptor(metaItem.name);
        let valueDescriptor = descriptor ? AssetModelUtil.getValueDescriptor(descriptor.type) : undefined;
        let content: TemplateResult = html``;

        if (!valueDescriptor) {
            content = html`<p>NOT SUPPORTED</p>`;
        } else {
            const options: ValueInputProviderOptions = {
                label: Util.getMetaLabel(metaItem, descriptor!, this.asset.type!, true)
            };
            const provider = getValueHolderInputTemplateProvider(this.asset.type!, metaItem, descriptor, valueDescriptor, (detail: OrInputChangedEventDetail) => this._onMetaItemModified(attribute, metaItem, detail), options);

            if (provider.templateFunction) {
                content = provider.templateFunction(metaItem.value, false, false, false, false, undefined);
            }
        }

        return html`
                <div class="meta-item-wrapper">
                    ${content}
                    <button class="button-clear" @click="${removeMetaItem}"><or-icon icon="close-circle"></or-icon></input>
                </div>
            `;
    }

    protected _addAttribute() {

        const asset = this.asset!;
        let attr: Attribute<any>;

        const onAttributeChanged = (attribute: Attribute<any>) => {
            const addDisabled = !(attribute.name && !asset.attributes![attribute.name] && AssetNameRegex.test(attribute.name) && attribute.type);
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
                    actionName: "cancel",
                    content: i18next.t("cancel")
                },
                {
                    default: true,
                    actionName: "add",
                    action: () => {
                        this.asset.attributes![attr.name!] = attr;
                        this._onModified();
                    },
                    content: html`<or-mwc-input id="add-btn" .type="${InputType.BUTTON}" disabled .label="${i18next.t("add")}"></or-mwc-input>`
                }
            ],
            dismissAction: null
        });
    }

    protected _addMetaItems(attribute: Attribute<any>) {

        const assetTypeInfo = AssetModelUtil.getAssetTypeInfo(this.asset!.type!);
        if (!assetTypeInfo || !assetTypeInfo.metaItemDescriptors) {
            return;
        }

        const meta = attribute.meta || {};

        const metaItemList: (ListItem | null)[] = assetTypeInfo.metaItemDescriptors.map((metaName) => AssetModelUtil.getMetaItemDescriptor(metaName)!)
            .filter((descriptor) => !meta.hasOwnProperty(descriptor.name!))
            .map((descriptor) => {
                return {
                    text: Util.getMetaLabel(undefined, descriptor, this.asset!.type!, true),
                    value: descriptor.name!,
                    translate: false
                };
            }).sort(Util.sortByString((item) => item.text));

        const dialog = showDialog({
            content: html`
                <div id="meta-creator">
                    <or-mwc-list id="meta-creator-list" .type="${ListType.MULTI_CHECKBOX}" .listItems="${metaItemList}"></or-mwc-list>
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
                    actionName: "cancel",
                    content: i18next.t("cancel")
                },
                {
                    default: true,
                    actionName: "add",
                    action: () => {
                        const list = dialog!.shadowRoot!.getElementById("meta-creator-list") as OrMwcList;
                        const selectedItems = list ? list.selectedItems : undefined;
                        if (selectedItems) {
                            if (!attribute.meta) {
                                attribute.meta = {};
                            }
                            selectedItems.forEach((item) => {
                                const descriptor = AssetModelUtil.getMetaItemDescriptors().find((descriptor) => descriptor.name === item.value);
                                if (descriptor) {
                                    attribute.meta![descriptor.name!] = null;
                                    this._onModified();
                                }
                            });
                        }
                    },
                    content: i18next.t("add")
                }
            ],
            dismissAction: null
        });
    }

    protected _getParentTemplate() {
        const viewer = this;
        let dialog: OrMwcDialog;

        const setParent = () => {
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
        const dialogContent = html`<or-asset-tree id="parent-asset-tree" disableSubscribe readonly .selectedIds="${this.asset!.parentId ? [this.asset.parentId] : []}" @or-asset-tree-request-select="${blockEvent}" @or-asset-tree-selection-changed="${blockEvent}" .selector="${parentSelector}"></or-asset-tree>`;

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

        const openDialog = () => {
            dialog = showDialog(
                {
                    content: dialogContent,
                    actions: dialogActions,
                    styles: html`
                        <style>
                            .mdc-dialog__surface {
                                width: 400px;
                                height: 800px;
                                display: flex;
                                overflow: visible;
                                overflow-x: visible !important;
                                overflow-y: visible !important;
                            }
                            #dialog-content {
                                flex: 1;    
                                overflow: visible;
                                min-height: 0;
                            }
                            or-asset-tree {
                                height: 100%;
                            }
                        </style>
                    `,
                     dismissAction: null
                }
            );
        };

        return html`
            <div id="parent-edit-wrapper">
                ${getPropertyTemplate(this.asset, "parentId", this, undefined, undefined, {readonly: false, label: i18next.t("parent")})}
                <or-mwc-input id="change-parent-btn" type="${InputType.BUTTON}" raised .label="${i18next.t("edit")}" @or-mwc-input-changed="${openDialog}"></or-mwc-input>
            </div>
        `;
    }
}
