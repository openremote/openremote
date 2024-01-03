import {css, html, LitElement, PropertyValues, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import {MDCDialog} from "@material/dialog";
import "@openremote/or-translate";
import "./or-mwc-input";
import {InputType, OrMwcInput} from "./or-mwc-input";
import {i18next} from "@openremote/or-translate";
import {Util} from "@openremote/core";

const dialogStyle = require("@material/dialog/dist/mdc.dialog.css");
const listStyle = require("@material/list/dist/mdc.list.css");

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
    const title = "error";
    const content = html`
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
                </div>`;

    return showOkDialog(title, content, undefined, hostElement);
}

export async function showOkCancelDialog(title: string, content: string | TemplateResult, okText?: string, hostElement?: HTMLElement) {

    const deferred = new Util.Deferred<boolean>();

    showDialog(
        new OrMwcDialog()
            .setContent(typeof(content) === "string" ? html`<p>${content}</p>` : content)
            .setActions(
                [
                    {
                        actionName: "cancel",
                        content: "cancel",
                        default: true,
                        action: () => deferred.resolve(false)
                    },
                    {
                        actionName: "ok",
                        content: okText ? okText : i18next.t("ok"),
                        action: () => deferred.resolve(true)
                    }
                ]
            )
            .setHeading(title)
            .setStyles(html`
                    <style>
                        .mdc-dialog__content {
                            white-space: pre-wrap
                        }
                    </style>
                `),
        hostElement);

    return await deferred.promise;
}

export async function showOkDialog(title: string, content: string | TemplateResult, okText?: string, hostElement?: HTMLElement) {

    const deferred = new Util.Deferred<boolean>();

    showDialog(
        new OrMwcDialog()
            .setContent(typeof(content) === "string" ? html`<p>${content}</p>` : content)
            .setActions(
                [
                    {
                        actionName: "ok",
                        default: true,
                        content: okText ? okText : i18next.t("ok"),
                        action: () => deferred.resolve(true)
                    }
                ]
            )
            .setHeading(title),
        hostElement);

    return await deferred.promise;
}

export function showDialog<T extends OrMwcDialog>(dialog: T, hostElement?: HTMLElement): T {
    if (!hostElement) {
        hostElement = OrMwcDialog.DialogHostElement || document.body;
    }

    dialog.setOpen(true);
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

    .mdc-dialog .mdc-dialog__surface {
        outline: none;
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

    @property({type: String})
    public heading?: string | TemplateResult;

    @property({type: Object, attribute: false})
    public content?: TemplateResult | (() => TemplateResult);

    @property({type: Array, attribute: false})
    public actions?: DialogAction[];

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

    public setOpen(isOpen: boolean): OrMwcDialog  {
        this._open = true;
        return this;
    }

    public setHeading(heading: TemplateResult | string | undefined): OrMwcDialog {
        this.heading = heading;
        return this;
    }

    public setContent(content: TemplateResult | (() => TemplateResult) | undefined): OrMwcDialog {
        this.content = content;
        return this;
    }

    public setActions(actions: DialogAction[] | undefined): OrMwcDialog {
        this.actions = actions;
        return this;
    }

    public setDismissAction(action: DialogActionBase | null | undefined): OrMwcDialog {
        this.dismissAction = action;
        return this;
    }

    public setStyles(styles: string | TemplateResult | undefined): OrMwcDialog {
        this.styles = styles;
        return this;
    }

    public setAvatar(avatar: boolean | undefined): OrMwcDialog {
        this.avatar = avatar;
        return this;
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
                    <div class="mdc-dialog__surface" tabindex="0">
						${typeof(this.heading) === "string" ? html`<h2 class="mdc-dialog__title" id="dialog-title"><or-translate value="${this.heading}"></or-translate></h2>`
                            : this.heading ? html`<span class="mdc-dialog__title" id="dialog-title">${this.heading}</span>` : ``}
                        ${this.content ? html` 
                            <div class="dialog-container mdc-dialog__content" id="dialog-content">
                                ${typeof this.content === "function" ? this.content() : this.content}
                            </div>
                            <footer class="mdc-dialog__actions">
                                ${this.actions ? this.actions.map((action) => {
                                    return html`
                                    <div class="mdc-button mdc-dialog__button" ?data-mdc-dialog-button-default="${action.default}" data-mdc-dialog-action="${action.disabled ? undefined : action.actionName}">
                                        ${typeof(action.content) === "string" ? html`<or-mwc-input .type="${InputType.BUTTON}" @or-mwc-input-changed="${(ev: Event) => {if ((ev.currentTarget as OrMwcInput).disabled) ev.stopPropagation()}}" .disabled="${action.disabled}" .label="${action.content}"></or-mwc-input>` : action.content}
                                    </div>`;
                                }) : ``}
                            </footer>
                        ` : html`
                            <ul class="mdc-list ${this.avatar ? "mdc-list--avatar-list" : ""}">
                                ${!this.actions ? `` : this.actions!.map((action, index) => {
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
        } else if (action && this.actions) {
            const matchedAction = this.actions.find((dialogAction) => dialogAction.actionName === action);
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
