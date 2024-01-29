import {css, html, LitElement, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {until} from "lit/directives/until.js";
import {customElement, property, state} from "lit/decorators.js";
import {InputType, OrMwcInput, OrInputChangedEvent, getValueHolderInputTemplateProvider, ValueInputProviderOptions, OrInputChangedEventDetail, ValueInputProvider} from "@openremote/or-mwc-components/or-mwc-input";
import i18next from "i18next";
import {Asset, Attribute, NameValueHolder, AssetModelUtil} from "@openremote/model";
import { DefaultColor5, DefaultColor3, DefaultColor2, Util} from "@openremote/core";
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
import {jsonFormsInputTemplateProvider, OrAttributeInput, OrAttributeInputChangedEvent } from "@openremote/or-attribute-input";
import {createRef, ref, Ref } from "lit/directives/ref.js";

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

    .mdc-data-table__table {
        width: 100%;
    }
    .mdc-data-table__header-cell {
        font-weight: 500;
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
    .padded-cell > or-attribute-input {
        width: 100%;
    }
    .actions-cell {
        text-align: right;
        width: 40px;
        padding-right: 4px;
    }
    .meta-item-container {
        padding: 0 4px 0 24px;
        overflow: hidden;
        max-height: 0;
        transition: max-height 0.25s ease-out;
    }
    .attribute-meta-row.expanded  .meta-item-container {
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
        padding: 0 0 0 4px;
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

export class OrEditAssetModifiedEvent extends CustomEvent<ValidatorResult[]> {

    public static readonly NAME = "or-edit-asset-modified";

    constructor(validatorResults: ValidatorResult[]) {
        super(OrEditAssetModifiedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: validatorResults
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrEditAssetModifiedEvent.NAME]: OrEditAssetModifiedEvent;
    }
}

const AssetNameRegex = /^\w+$/;

export interface ValidatorResult {
    name: string;
    valid: boolean;
    metaResults?: ValidatorResult[]; // For meta items
}

interface TemplateAndValidator {
    template: TemplateResult;
    validator: () => ValidatorResult;
}

@customElement("or-edit-asset-panel")
export class OrEditAssetPanel extends LitElement {

    @property({attribute: false})
    protected asset!: Asset;

    protected attributeTemplatesAndValidators: TemplateAndValidator[] = [];
    protected changedAttributes: string[] = [];

    public static get styles() {
        return [
            unsafeCSS(tableStyle),
            panelStyles,
            style
        ];
    }

    public attributeUpdated(attributeName: string) {
        if (!this.asset) {
            return;
        }
        this.changedAttributes.push(attributeName);

        // Request re-render
        this.requestUpdate();
    }

    shouldUpdate(changedProperties: PropertyValues): boolean {
        if (changedProperties.has("asset")) {
            this.changedAttributes = [];
        }
        return super.shouldUpdate(changedProperties);
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
            if (tdElem.className.indexOf("expander-cell") < 0 || tdElem.className.indexOf("expanding") >= 0) {
                return;
            }
            const expanderIcon = tdElem.getElementsByTagName("or-icon")[0] as OrIcon;
            const headerRow = tdElem.parentElement! as HTMLTableRowElement;
            const metaRow = (headerRow.parentElement! as HTMLTableElement).rows[headerRow.rowIndex];
            const metaContainer = metaRow.firstElementChild!.firstElementChild as HTMLElement;
            const contentHeight = Math.max(500, metaContainer.firstElementChild!.getBoundingClientRect().height);

            if (expanderIcon.icon === "chevron-right") {
                expanderIcon.icon = "chevron-down";
                metaRow.classList.add("expanded");
                metaContainer.style.maxHeight = contentHeight + "px";
                tdElem.classList.add("expanding");
                // Allow container to grow when expanded once animation has finished
                window.setTimeout(() => {
                    tdElem.classList.remove("expanding");
                    metaContainer.style.maxHeight = "unset";
                }, 1100);
            } else {
                expanderIcon.icon = "chevron-right";
                metaRow.classList.remove("expanded");
                metaContainer.style.transition = "none";
                metaContainer.style.maxHeight = Math.max(500, metaContainer.firstElementChild!.getBoundingClientRect().height) + "px";
                window.setTimeout(() => {
                    metaContainer.style.transition = "";
                    metaContainer.style.maxHeight = "";
                });
            }
        };

        this.attributeTemplatesAndValidators = !this.asset.attributes ? [] : Object.entries(this.asset.attributes!).sort(Util.sortByString(([name, attribute]) => name.toUpperCase())).map(([name, attribute]) => {attribute.name = name; return this._getAttributeTemplate(this.asset.type!, attribute as Attribute<any>);})

        const attributes = html`
            <div id="attribute-table" class="mdc-data-table">
                <table class="mdc-data-table__table" aria-label="attribute list" @click="${expanderToggle}">
                    <colgroup>
                        <col span="1" style="width: 25%;">
                        <col span="1" style="width: 25%;">
                        <col span="1" style="width: 35%;">
                        <col span="1" style="width: 15%;">
                    </colgroup>
                    <thead>
                        <tr class="mdc-data-table__header-row">
                            <th class="mdc-data-table__header-cell" role="columnheader" scope="col"><or-translate value="name"></or-translate></th>
                            <th class="mdc-data-table__header-cell" role="columnheader" scope="col"><or-translate value="type"></or-translate></th>
                            <th class="mdc-data-table__header-cell" role="columnheader" scope="col"><or-translate value="value"></or-translate></th>
                            <th class="mdc-data-table__header-cell" role="columnheader" scope="col"></or-translate></th>
                        </tr>
                    </thead>
                    <tbody class="mdc-data-table__content">
                        ${this.attributeTemplatesAndValidators.map((attrTemplateAndValidator) => attrTemplateAndValidator.template)}
                        <tr class="mdc-data-table__row">
                            <td colspan="4">
                                <div class="item-add-attribute">
                                    <or-mwc-input .type="${InputType.BUTTON}" label="addAttribute" icon="plus" @or-mwc-input-changed="${() => this._addAttribute()}"></or-mwc-input>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        `;

        return html`
            <div id="edit-wrapper">
                ${getPanel("0", {type: "info", title: "properties"}, html`${properties}`) || ``}
                ${getPanel("1", {type: "info", title: "attribute_plural"}, html`${attributes}`) || ``}
            </div>
        `;
    }

    protected _getAttributeTemplate(assetType: string, attribute: Attribute<any>): TemplateAndValidator {

        const deleteAttribute = () => {
            delete this.asset.attributes![attribute.name!];
            this._onModified();
        };

        const descriptor = AssetModelUtil.getAttributeDescriptor(attribute.name!, assetType);
        const canDelete = !descriptor || descriptor.optional;
        const attributeInputRef: Ref<OrAttributeInput> = createRef();
        const metaTemplatesAndValidators = !attribute.meta ? [] : Object.entries(attribute.meta).sort(Util.sortByString(([name, value]) => name!)).map(([name, value]) => this._getMetaItemTemplate(attribute, Util.getMetaItemNameValueHolder(name, value)));

        const validator = (): ValidatorResult => {
            let valid = false;

            if (attributeInputRef.value) {
                valid = attributeInputRef.value.checkValidity();
            }

            return {
                name: attribute.name!,
                valid: valid,
                metaResults: metaTemplatesAndValidators.map((metaTemplateAndValidator) => metaTemplateAndValidator.validator())
            };
        };

        const template = html`
            <tr class="mdc-data-table__row">
                <td class="padded-cell mdc-data-table__cell expander-cell"><or-icon icon="chevron-right"></or-icon><span>${attribute.name}</span></td>
                <td class="padded-cell mdc-data-table__cell">${Util.getValueDescriptorLabel(attribute.type!)}</td>
                <td class="padded-cell overflow-visible mdc-data-table__cell">
                    <or-attribute-input ${ref(attributeInputRef)} 
                                        .comfortable="${true}" .assetType="${assetType}" .label=${null} 
                                        .readonly="${false}" .attribute="${attribute}" .assetId="${this.asset.id!}" 
                                        disableWrite disableSubscribe disableButton compact 
                                        @or-attribute-input-changed="${(e: OrAttributeInputChangedEvent) => this._onAttributeModified(attribute, e.detail.value)}"></or-attribute-input>
                </td>
                <td class="padded-cell mdc-data-table__cell actions-cell">${canDelete ? html`<or-mwc-input type="${InputType.BUTTON}" icon="delete" @click="${deleteAttribute}">` : ``}</td>
            </tr>
            <tr class="attribute-meta-row">
                <td colspan="4">
                    <div class="meta-item-container">
                        <div>
                            <div>
                                ${metaTemplatesAndValidators.map((metaTemplateAndValidator) => metaTemplateAndValidator.template)}
                            </div>
                            <div class="item-add">
                                <or-mwc-input .type="${InputType.BUTTON}" label="addMetaItems" icon="plus" @or-mwc-input-changed="${() => this._addMetaItems(attribute)}"></or-mwc-input>
                            </div>
                        </div>
                    </div>                     
                </td>
            </tr>
        `;

        return {
            template: template,
            validator: validator
        };
    };

    protected _onModified() {
        this.dispatchEvent(new OrEditAssetModifiedEvent(this.validate()));
        this.requestUpdate();
    }

    public validate(): ValidatorResult[] {
        return this.attributeTemplatesAndValidators.map((attrTemplateAndValidator) => attrTemplateAndValidator.validator());
    }

    protected _onAttributeModified(attribute: Attribute<any>, newValue: any) {

        // Check if modification came from external change
        const index = this.changedAttributes.indexOf(attribute.name!);
        if (index > -1) {
            this.changedAttributes.splice(index, 1);
            return;
        }

        attribute.value = newValue;
        attribute.timestamp = undefined; // Clear timestamp so server will set this
        this._onModified();
    }

    protected _onMetaItemModified(attribute: Attribute<any>, metaItem: NameValueHolder<any>, detail: OrInputChangedEventDetail | undefined) {
        metaItem.value = detail ? detail.value : undefined;
        attribute.meta![metaItem.name!] = metaItem.value;
        this._onModified();
    }

    protected _getMetaItemTemplate(attribute: Attribute<any>, metaItem: NameValueHolder<any>): TemplateAndValidator {
        const descriptor = AssetModelUtil.getMetaItemDescriptor(metaItem.name);
        const valueDescriptor = descriptor ? AssetModelUtil.getValueDescriptor(descriptor.type) : undefined;
        let content: TemplateResult = html``;
        let validator: () => ValidatorResult = () => {
            return {
                name: metaItem.name!,
                valid: true
            };
        };

        if (!valueDescriptor) {
            console.log("Couldn't find value descriptor for meta item so falling back to simple JSON input: " + metaItem.name);
            content = html`<or-mwc-input @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._onMetaItemModified(attribute, metaItem, ev.detail)}" .type="${InputType.JSON}" .value="${metaItem.value}"></or-mwc-input>`;
        } else {
            const options: ValueInputProviderOptions = {
                label: Util.getMetaLabel(metaItem, descriptor!, this.asset.type!, true),
                resizeVertical: true
            };
            const standardInputProvider = getValueHolderInputTemplateProvider(this.asset.type!, metaItem, descriptor, valueDescriptor, (detail) => this._onMetaItemModified(attribute, metaItem, detail), options);
            let provider = jsonFormsInputTemplateProvider(standardInputProvider)(this.asset.type!, metaItem, descriptor, valueDescriptor, (detail) => this._onMetaItemModified(attribute, metaItem, detail), options);

            if (!provider) {
                provider = standardInputProvider;
            }

            if (provider && provider.templateFunction) {
                content = html`${until(provider.templateFunction(metaItem.value, false, false, false, false, undefined), ``)}`;
            }
            if (provider.validator) {
                validator = () => {
                    return {
                        name: metaItem.name!,
                        valid: provider.validator!()
                    };
                };
            }
        }

        const removeMetaItem = () => {
            delete attribute.meta![metaItem.name!];
            this._onModified();
        };

        const template = html`
            <div class="meta-item-wrapper">
                ${content}
                <button class="button-clear" @click="${removeMetaItem}">
                    <or-icon icon="close-circle"></or-icon>
                    </input>
            </div>
        `;

        return {
            template: template,
            validator: validator
        };
    }

    protected _addAttribute() {

        const asset = this.asset!;
        let attr: Attribute<any>;

        const isDisabled = (attribute: Attribute<any>) => {
            return !(attribute && attribute.name && !asset.attributes![attribute.name] && AssetNameRegex.test(attribute.name) && attribute.type);
        }

        const onAttributeChanged = (attribute: Attribute<any>) => {
            const addDisabled = isDisabled(attribute);
            const addBtn = dialog!.shadowRoot!.getElementById("add-btn") as OrMwcInput;
            addBtn!.disabled = addDisabled;
            attr = attribute;
        };

        const dialog = showDialog(new OrMwcDialog()
            .setContent(html`
                <or-add-attribute-panel .asset="${asset}" @or-add-attribute-panel-attribute-changed="${(ev: OrAddAttributePanelAttributeChangedEvent) => onAttributeChanged(ev.detail)}"></or-add-attribute-panel>
            `)
            .setStyles(html`
                <style>
                    .mdc-dialog__surface {
                        overflow-x: visible !important;
                        overflow-y: visible !important;
                    }
                    #dialog-content {
                        padding: 0;
                        overflow: visible;
                    }
                </style>
            `)
            .setHeading(i18next.t("addAttribute"))
            .setActions([
                {
                    actionName: "cancel",
                    content: "cancel"
                },
                {
                    default: true,
                    actionName: "add",
                    action: () => {
                        if (attr) {
                            this.asset.attributes![attr.name!] = attr;
                            this._onModified();
                        }
                    },
                    content: html`<or-mwc-input id="add-btn" .type="${InputType.BUTTON}" disabled label="add"
                                    @or-mwc-input-changed="${(ev: Event) => { if (isDisabled(attr)) { ev.stopPropagation(); return false; } } }"></or-mwc-input>`
                }
            ])
            .setDismissAction(null));
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

        const dialog = showDialog(new OrMwcDialog()
            .setContent(html`
                <div id="meta-creator">
                    <or-mwc-list id="meta-creator-list" .type="${ListType.MULTI_CHECKBOX}" .listItems="${metaItemList}"></or-mwc-list>
                </div>
            `)
            .setStyles(html`
                <style>
                    #meta-creator {
                        height: 600px;
                        max-height: 100%;
                    }
                    
                    #meta-creator > or-mwc-list {
                        height: 100%;
                    }

                    .mdc-dialog .mdc-dialog__content {
                        padding: 0 !important;
                    }
                </style>
            `)
            .setHeading(i18next.t("addMetaItems"))
            .setActions([
                {
                    actionName: "cancel",
                    content: "cancel"
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
                                    attribute.meta![descriptor.name!] = (descriptor.type === 'boolean') ? true : null;
                                    this._onModified();
                                }
                            });
                        }
                    },
                    content: "add"
                }
            ])
            .setDismissAction(null));
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
                path.unshift(parentNode.asset!.id!);
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
                content: "none",
                action: clearParent
            },
            {
                actionName: "ok",
                content: "ok",
                action: setParent
            },
            {
                default: true,
                actionName: "cancel",
                content: "cancel"
            }
        ];

        const openDialog = () => {
            dialog = showDialog(new OrMwcDialog()
                .setContent(dialogContent)
                .setActions(dialogActions)
                .setStyles(html`
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
                                padding: 0;
                            }
                            footer.mdc-dialog__actions {
                                border-top: 1px solid ${unsafeCSS(DefaultColor5)};
                            }
                            or-asset-tree {
                                height: 100%;
                            }
                        </style>
                    `)
                .setHeading(i18next.t("setParent"))
                .setDismissAction(null));
        };

        return html`
            <div id="parent-edit-wrapper">
                ${getPropertyTemplate(this.asset, "parentId", this, undefined, undefined, {readonly: true, label: i18next.t("parent")})}
                <or-mwc-input id="change-parent-btn" type="${InputType.BUTTON}" outlined label="edit" @or-mwc-input-changed="${openDialog}"></or-mwc-input>
            </div>
        `;
    }
}
