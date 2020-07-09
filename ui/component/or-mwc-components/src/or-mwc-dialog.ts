import {
    css,
    customElement,
    html,
    LitElement,
    property,
    query,
    TemplateResult,
    unsafeCSS,
    CSSResult
} from "lit-element";
import {MDCDialog} from "@material/dialog";
import "@openremote/or-translate";
import "@openremote/or-input";
import {InputType} from "@openremote/or-input";

const dialogStyle = require("!!raw-loader!@material/dialog/dist/mdc.dialog.css");
const listStyle = require("!!raw-loader!@material/list/dist/mdc.list.css");

export interface DialogConfig {
    title?: TemplateResult | string;
    content?: TemplateResult;
    actions?: DialogAction[];
    avatar?: boolean;
    styles?: TemplateResult | string;
}

export interface DialogAction {
    default?: boolean;
    content: TemplateResult | string;
    actionName: string;
    action?: () => void;
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
`;

@customElement("or-mwc-dialog")
export class OrMwcDialog extends LitElement {

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

    @property({type: Boolean})
    public avatar?: boolean;

    @property()
    public styles?: TemplateResult | string;

    @query("#dialog")
    protected _mdcElem!: HTMLElement;

    protected _mdcComponent?: MDCDialog;

    public get isOpen() {
        return this._mdcComponent ? this._mdcComponent.isOpen : false;
    }

    public open() {
        if (this._mdcElem && !this._mdcComponent) {
            this._mdcComponent = new MDCDialog(this._mdcElem);
            this._mdcComponent!.scrimClickAction = "";
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
            
            ${typeof(this.styles) === "string" ?  html`<style></style>${this.styles}<style>` : this.styles}

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
                            : html`<span class="mdc-dialog__title" id="dialog-title">${this.dialogTitle}</span>`}
                        ${this.dialogContent ? html` 
                            <div class="dialog-container mdc-dialog__content" id="dialog-content">
                                ${this.dialogContent ? this.dialogContent : html`<slot></slot>`}
                            </div>
                            <footer class="mdc-dialog__actions">
                                ${this.dialogActions ? this.dialogActions.map((action) => {
                                    return html`
                                    <div class="mdc-button mdc-dialog__button" ?data-mdc-dialog-button-default="${action.default}" data-mdc-dialog-action="${action.actionName}">
                                        ${typeof(action.content) === "string" ? html`<or-input .type="${InputType.BUTTON}" .label="${action.content}"></or-input>` : action.content}
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
        `;
    }

    protected _onDialogOpened() {
        this.dispatchEvent(new OrMwcDialogOpenedEvent());
    }

    protected _onDialogClosed(action?: string) {
        this.dispatchEvent(new OrMwcDialogClosedEvent(action));
        if (action && this.dialogActions) {
            const matchedAction = this.dialogActions.find((dialogAction) => dialogAction.actionName === action);
            if (matchedAction && matchedAction.action) {
                matchedAction.action();
            }
        }
    }
}
