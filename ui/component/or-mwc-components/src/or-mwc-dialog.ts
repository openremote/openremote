import {css, html, LitElement, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import {MDCDialog} from "@material/dialog";
import "@openremote/or-translate";
import "./or-mwc-input";
import {InputType, OrInputChangedEvent, OrMwcInput} from "./or-mwc-input";
import {i18next} from "@openremote/or-translate";
import manager, {DefaultColor2, DefaultColor4, DefaultColor5, Util} from "@openremote/core";
import {Attribute, AttributeRef, WellknownMetaItems} from "@openremote/model";
import "@openremote/or-asset-tree";
import {OrAssetTree, OrAssetTreeSelectionEvent} from "@openremote/or-asset-tree";
import {ListItem, ListType, OrMwcListChangedEvent} from "./or-mwc-list";

const dialogStyle = require("@material/dialog/dist/mdc.dialog.css");
const listStyle = require("@material/list/dist/mdc.list.css");

export interface DialogConfig {
    title?: TemplateResult | string;
    content?: TemplateResult;
    actions?: DialogAction[];
    avatar?: boolean;
    styles?: TemplateResult | string;
    dismissAction?: DialogActionBase | null;
}
export interface DialogActionBase {
    actionName: string;
    action?: (dialog: OrMwcDialog) => void;
}

export interface DialogAction extends DialogActionBase {
    default?: boolean;
    content: TemplateResult | string;
    disabled?: boolean;
}

export class OrMwcDialogOpenedEvent extends CustomEvent<void> {

    public static readonly NAME = "or-mwc-dialog-opened";

    constructor() {
        super(OrMwcDialogOpenedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}

export class OrMwcDialogClosedEvent extends CustomEvent<string | undefined> {

    public static readonly NAME = "or-mwc-dialog-closed";

    constructor(action?: string) {
        super(OrMwcDialogClosedEvent.NAME, {
            detail: action,
            bubbles: true,
            composed: true
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrMwcDialogOpenedEvent.NAME]: OrMwcDialogOpenedEvent;
        [OrMwcDialogClosedEvent.NAME]: OrMwcDialogClosedEvent;
    }
}

export async function showErrorDialog(errorMessage: string, hostElement?: HTMLElement) {
    const deferred = new Util.Deferred<void>();

    showDialog({
        title: "error",
        content: html`
                <div>
                    <p><or-translate value="errorOccurred"></or-translate>
                    ${errorMessage ? html`
                        :</p>
                        <p>
                            <or-translate value="error"></or-translate>
                            <span> = </span> 
                            <or-translate .value="${errorMessage}"></or-translate>
                    ` : ``}
                    </p>
                </div>`,
        actions: [{
            actionName: "ok",
            content: i18next.t("ok"),
            default: true,
            action: (dialog) => deferred.resolve()
        }]
    }, hostElement);

    await deferred.promise;
}

export async function showOkCancelDialog(title: string, content: string | TemplateResult, okText?: string) {

    const deferred = new Util.Deferred<boolean>();

    showDialog(
        {
            content: typeof(content) === "string" ? html`<p>${content}</p>` : content,
            actions: [
                {
                    actionName: "cancel",
                    content: "cancel",
                    default: true,
                    action: () => deferred.resolve(false)
                },
                {
                    actionName: "ok",
                    content: okText ? okText : "ok",
                    action: () => deferred.resolve(true)
                }
            ],
            title: title
        }
    );

    return await deferred.promise;
}

export function showDialog(config: DialogConfig, hostElement?: HTMLElement): OrMwcDialog {
    if (!hostElement) {
        hostElement = OrMwcDialog.DialogHostElement || document.body;
    }

    const dialog = new OrMwcDialog();
    dialog.isOpen = true;
    dialog.addEventListener(OrMwcDialogOpenedEvent.NAME, (ev) => {
        ev.stopPropagation();
    });
    dialog.addEventListener(OrMwcDialogClosedEvent.NAME, (ev) => {
        ev.stopPropagation();
        window.setTimeout(() => {
            if (dialog.parentElement) {
                dialog.parentElement.removeChild(dialog);
            }
        }, 0);
    });
    dialog.config = config;
    hostElement.append(dialog);
    return dialog;
}

// language=CSS
const style = css`
    :host {
        position: relative;
    }

    .dialog-container {
        display: flex;
        flex-direction: row;
    }

    .dialog-container > * {
        flex: 1 1 0;
    }
    
    .mdc-list {
        padding: 0 24px
    }

    @media (min-width: 1280px) {
        .mdc-dialog .mdc-dialog__surface {
            max-width: 1024px;
        }
    }
`;

@customElement("or-mwc-dialog")
export class OrMwcDialog extends LitElement {

    /**
     * Can be set by apps to control where in the DOM dialogs are added
     */
    public static DialogHostElement: HTMLElement;

    static get styles() {
        return [
            css`${unsafeCSS(dialogStyle)}`,
            css`${unsafeCSS(listStyle)}`,
            style
        ];
    }

    public set config(config: DialogConfig) {
        if (config) {
            this.dialogTitle = config.title;
            this.dialogContent = config.content;
            this.dialogActions = config.actions;
            this.dismissAction = config.dismissAction;
            this.avatar = config.avatar;
            this.styles = config.styles;
        }
    };

    @property({type: String})
    public dialogTitle?: string | TemplateResult;

    @property({type: Object, attribute: false})
    public dialogContent?: TemplateResult;

    @property({type: Array, attribute: false})
    public dialogActions?: DialogAction[];

    @property({type: Object, attribute: false})
    public dismissAction: DialogActionBase | null | undefined;

    @property({type: Boolean})
    public avatar?: boolean;

    @property()
    public styles?: TemplateResult | string;

    @property({attribute: false})
    protected _open: boolean = false;

    @query("#dialog")
    protected _mdcElem!: HTMLElement;

    protected _mdcComponent?: MDCDialog;

    public get isOpen() {
        return this._mdcComponent ? this._mdcComponent.isOpen : false;
    }

    public set isOpen(isOpen: boolean) {
        this._open = true;
    }

    public open() {
        if (this._mdcElem && !this._mdcComponent) {
            this._mdcComponent = new MDCDialog(this._mdcElem);
            this._mdcComponent!.scrimClickAction = this.dismissAction || this.dismissAction === null ? "close" : "";
        }
        if (this._mdcComponent) {
            this._mdcComponent.open();
        }
    }

    public close(action?: string) {
        if (this._mdcComponent) {
            this._mdcComponent.close(action);
        }
    }

    disconnectedCallback(): void {
        super.disconnectedCallback();
        if (this._mdcComponent) {
            this._mdcComponent.destroy();
            this._mdcComponent = undefined;
        }
    }

    protected render() {

        return html`
            ${typeof(this.styles) === "string" ?  html`<style>${this.styles}</style>` : this.styles || ``}
            
            <div id="dialog"
                class="mdc-dialog"
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="dialog-title"
                aria-describedby="dialog-content"
                @MDCDialog:opened="${() => this._onDialogOpened()}"
                @MDCDialog:closed="${(evt: any) => this._onDialogClosed(evt.detail.action)}">
                <div class="mdc-dialog__container">
                    <div class="mdc-dialog__surface">
						${typeof(this.dialogTitle) === "string" ? html`<h2 class="mdc-dialog__title" id="dialog-title"><or-translate value="${this.dialogTitle}"></or-translate></h2>`
                            : this.dialogTitle ? html`<span class="mdc-dialog__title" id="dialog-title">${this.dialogTitle}</span>` : ``}
                        ${this.dialogContent ? html` 
                            <div class="dialog-container mdc-dialog__content" id="dialog-content">
                                ${this.dialogContent ? this.dialogContent : html`<slot></slot>`}
                            </div>
                            <footer class="mdc-dialog__actions">
                                ${this.dialogActions ? this.dialogActions.map((action) => {
                                    return html`
                                    <div class="mdc-button mdc-dialog__button" ?data-mdc-dialog-button-default="${action.default}" data-mdc-dialog-action="${action.actionName}">
                                        ${typeof(action.content) === "string" ? html`<or-mwc-input .type="${InputType.BUTTON}" .disabled="${action.disabled}" .label="${action.content}"></or-mwc-input>` : action.content}
                                    </div>`;
                                }) : ``}
                            </footer>
                        ` : html`
                            <ul class="mdc-list ${this.avatar ? "mdc-list--avatar-list" : ""}">
                                ${!this.dialogActions ? `` : this.dialogActions!.map((action, index) => {
                                    return html`<li class="mdc-list-item" data-mdc-dialog-action="${action.actionName}"><span class="mdc-list-item__text">${action.content}</span></li>`;                    
                                })}
                            </ul>
                        `}
                    </div>
                </div>
                <div class="mdc-dialog__scrim"></div>
            </div>
        `;
    }

    protected updated(_changedProperties: PropertyValues) {
        super.updated(_changedProperties);
        if (_changedProperties.has("_open") && this._open) {
            this.open();
        }
    }

    protected _onDialogOpened() {
        this.dispatchEvent(new OrMwcDialogOpenedEvent());
    }

    protected _onDialogClosed(action?: string) {
        if (action === "close" && this.dismissAction && this.dismissAction.action) {
            this.dismissAction.action(this);
        } else if (action && this.dialogActions) {
            const matchedAction = this.dialogActions.find((dialogAction) => dialogAction.actionName === action);
            if (matchedAction && matchedAction.action) {
                matchedAction.action(this);
            }
        }
        if (this._mdcComponent) {
            this._mdcComponent.destroy();
            this._mdcComponent = undefined;
        }
        this.dispatchEvent(new OrMwcDialogClosedEvent(action));
    }
}

export type AddAttrRefsEventDetail = {
    selectedAttributes?: AttributeRef[];
}
export class OrAttributeRefsAddRequestEvent extends CustomEvent<Util.RequestEventDetail<AddAttrRefsEventDetail>> {

    public static readonly NAME = "or-attribute-refs-request-add";

    constructor(detail: AddAttrRefsEventDetail) {
        super(OrAttributeRefsAddRequestEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                allow: true,
                detail: detail
            }
        });
    }
}
export class OrAddAttributeRefsEvent extends CustomEvent<AddAttrRefsEventDetail> {

    public static readonly NAME = "or-attribute-refs-add";

    constructor(detail: AddAttrRefsEventDetail) {
        super(OrAddAttributeRefsEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: detail
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrAddAttributeRefsEvent.NAME]: OrAddAttributeRefsEvent;
    }
}

@customElement("or-mwc-attribute-selector")
export class OrMwcAttributeSelector extends OrMwcDialog {

    @property({type: Boolean})
    public showOnlyDatapointAttrs?: boolean = false;
    
    @property({type: Boolean})
    public showOnlyRuleStateAttrs?: boolean = false;

    @property({type: Boolean})
    public multiSelect?: boolean = false;

    @state()
    protected assetAttributes?: Attribute<any>[];
    protected selectedAttributes: AttributeRef[] = [];
    @query("#add-btn")
    protected addBtn!: OrMwcInput;
    
    constructor() {
        super();
        
        this.dialogTitle = "Add attributes";
        this.setDialogContent();
        this.setDialogActions();
        this.dismissAction = null;

        this.styles = `
            .attributes-header {
                line-height: 48px;
                padding: 0 15px;
                background-color: ${unsafeCSS(DefaultColor2)};
                font-weight: bold;
                border-bottom: 1px solid ${unsafeCSS(DefaultColor2)};
            }
            footer.mdc-dialog__actions {
                border-top: 1px solid ${unsafeCSS(DefaultColor5)};
            }
            #header {
                background-color: ${unsafeCSS(DefaultColor4)} !important;
            }
            #dialog-content {
                padding: 0;
            }
        `;
    }

    protected setDialogActions(): void {
        this.dialogActions = [
            {
                actionName: "cancel",
                content: i18next.t("cancel")
            },
            {
                actionName: "add",
                content: html`<or-mwc-input id="add-btn" class="button" .type="${InputType.BUTTON}" label="${i18next.t("add")}" ?disabled="${!this.selectedAttributes.length}"></or-mwc-input>`,
                action: () => {

                    if (!this.selectedAttributes.length) {
                        return;
                    }
                    
                    const detail: AddAttrRefsEventDetail = {
                        selectedAttributes: this.selectedAttributes
                    };
                    Util.dispatchCancellableEvent(this, new OrAttributeRefsAddRequestEvent(detail))
                        .then((detail) => {
                            if (detail.allow) {
                                this.dispatchEvent(new OrAddAttributeRefsEvent(detail.detail));
                            }
                        });
                }
            }
        ];
    }
    
    protected setDialogContent(): void {

        const getListItems: () => ListItem[] = () => {
            if (!this.assetAttributes) {
                return [];
            }

            return this.assetAttributes.map((attribute: Attribute<any>) => {
                return {
                    text: Util.getAttributeLabel(undefined, attribute, undefined, true),
                    value: attribute.name,
                    data: attribute
                } as ListItem
            });
        };

        this.dialogContent = html`
            <div class="row" style="display: flex;height: 600px;width: 800px;border-top: 1px solid ${unsafeCSS(DefaultColor5)};">
                <div class="col" style="width: 260px;overflow: auto;border-right: 1px solid ${unsafeCSS(DefaultColor5)};">
                    <or-asset-tree id="chart-asset-tree" readonly
                                   @or-asset-tree-selection="${(ev: OrAssetTreeSelectionEvent) => this._onAssetSelectionChanged(ev)}">
                    </or-asset-tree>
                </div>
                <div class="col" style="flex: 1 1 auto;width: 260px;overflow: auto;">
                ${this.assetAttributes && this.assetAttributes.length > 0 ? html`
                    <div class="attributes-header">
                        <or-translate value="attribute_plural"></or-translate>
                    </div>
                    ${this.multiSelect
                        ?
                            html`<div style="display: grid">
                                <or-mwc-list 
                                        id="attribute-selector" .type="${ListType.MULTI_CHECKBOX}" .listItems="${getListItems()}"
                                        .values="${this.selectedAttributes.map(attribute => attribute.name!)}"
                                        @or-mwc-list-changed="${(ev: OrMwcListChangedEvent) => this._onAttributeSelectionChanged(ev.detail.map(li => li.data as Attribute<any>))}"></or-mwc-list>
                            </div>`
                        :
                            html`<or-mwc-input id="attribute-selector"
                                    style="display:flex;"
                                    .label="${i18next.t("attribute")}"
                                    .type="${InputType.LIST}"
                                    .options="${getListItems().map(item => ([item, item.text]))}"
                                    @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._onAttributeSelectionChanged([(ev.detail.value as ListItem).data as Attribute<any>])}"
                                    }"></or-mwc-input>`
                    }
                ` : html`<div style="display: flex;align-items: center;text-align: center;height: 100%;padding: 0 20px;"><span style="width:100%">
                            <or-translate value="${
                                (this.assetAttributes && this.assetAttributes.length === 0) ?
                                    ((this.showOnlyDatapointAttrs && this.showOnlyRuleStateAttrs) ? "noDatapointsOrRuleStateAttributes" :
                                        this.showOnlyDatapointAttrs ? "noDatapointsAttributes" :
                                            this.showOnlyRuleStateAttrs ? "noRuleStateAttributes" : "noAttributesToShow"
                                    ) : "selectAssetOnTheLeft"}">
                            </or-translate></span></div>`}
                </div>
        `;
    }

    protected async _onAssetSelectionChanged(event: OrAssetTreeSelectionEvent) {
        this.assetAttributes = undefined;
        this.selectedAttributes = [];
        const assetTree = event.target as OrAssetTree;
        assetTree.disabled = true;

        let selectedAsset = event.detail.newNodes.length === 0 ? undefined : event.detail.newNodes[0].asset;

        if (selectedAsset) {
            // Load the asset attributes
            const assetResponse = await manager.rest.api.AssetResource.get(selectedAsset.id!);
            selectedAsset = assetResponse.data;

            if (selectedAsset) {
                this.assetAttributes = Object.values(selectedAsset.attributes!) as Attribute<any>[];

                if (this.showOnlyDatapointAttrs && this.showOnlyRuleStateAttrs) {
                    this.assetAttributes = this.assetAttributes
                        .filter(e => e.meta && (e.meta[WellknownMetaItems.STOREDATAPOINTS] || e.meta[WellknownMetaItems.RULESTATE] || e.meta[WellknownMetaItems.AGENTLINK]));
                } else if (this.showOnlyDatapointAttrs) {
                    this.assetAttributes = this.assetAttributes
                        .filter(e => e.meta && (e.meta[WellknownMetaItems.STOREDATAPOINTS] || e.meta[WellknownMetaItems.AGENTLINK]));
                } else if (this.showOnlyRuleStateAttrs) {
                    this.assetAttributes = this.assetAttributes
                        .filter(e => e.meta && (e.meta[WellknownMetaItems.RULESTATE] || e.meta[WellknownMetaItems.AGENTLINK]));
                }
            }
        }

        assetTree.disabled = false;
    }

    protected _onAttributeSelectionChanged(attributes: Attribute<any>[]) {
        this.selectedAttributes = attributes;
        this.addBtn.disabled = this.selectedAttributes.length > 0;
    }
}
