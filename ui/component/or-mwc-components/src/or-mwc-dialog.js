var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { MDCDialog } from "@material/dialog";
import "@openremote/or-translate";
import "./or-mwc-input";
import { InputType } from "./or-mwc-input";
import { i18next } from "@openremote/or-translate";
import { Util } from "@openremote/core";
const dialogStyle = require("@material/dialog/dist/mdc.dialog.css");
const listStyle = require("@material/list/dist/mdc.list.css");
export class OrMwcDialogOpenedEvent extends CustomEvent {
    constructor() {
        super(OrMwcDialogOpenedEvent.NAME, {
            bubbles: true,
            composed: true
        });
    }
}
OrMwcDialogOpenedEvent.NAME = "or-mwc-dialog-opened";
export class OrMwcDialogClosedEvent extends CustomEvent {
    constructor(action) {
        super(OrMwcDialogClosedEvent.NAME, {
            detail: action,
            bubbles: true,
            composed: true
        });
    }
}
OrMwcDialogClosedEvent.NAME = "or-mwc-dialog-closed";
export function showErrorDialog(errorMessage, hostElement) {
    return __awaiter(this, void 0, void 0, function* () {
        const title = "error";
        const content = html `
                <div>
                    <p><or-translate value="errorOccurred"></or-translate>
                    ${errorMessage ? html `
                        :</p>
                        <p>
                            <or-translate value="error"></or-translate>
                            <span> = </span> 
                            <or-translate .value="${errorMessage}"></or-translate>
                    ` : ``}
                    </p>
                </div>`;
        return showOkDialog(title, content, undefined, hostElement);
    });
}
export function showOkCancelDialog(title, content, okText, hostElement) {
    return __awaiter(this, void 0, void 0, function* () {
        const deferred = new Util.Deferred();
        showDialog(new OrMwcDialog()
            .setContent(typeof (content) === "string" ? html `<p>${content}</p>` : content)
            .setActions([
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
        ])
            .setHeading(title)
            .setStyles(html `
                    <style>
                        .mdc-dialog__content {
                            white-space: pre-wrap
                        }
                    </style>
                `), hostElement);
        return yield deferred.promise;
    });
}
export function showOkDialog(title, content, okText, hostElement) {
    return __awaiter(this, void 0, void 0, function* () {
        const deferred = new Util.Deferred();
        showDialog(new OrMwcDialog()
            .setContent(typeof (content) === "string" ? html `<p>${content}</p>` : content)
            .setActions([
            {
                actionName: "ok",
                default: true,
                content: okText ? okText : i18next.t("ok"),
                action: () => deferred.resolve(true)
            }
        ])
            .setHeading(title), hostElement);
        return yield deferred.promise;
    });
}
export function showDialog(dialog, hostElement) {
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
const style = css `
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
let OrMwcDialog = class OrMwcDialog extends LitElement {
    constructor() {
        super(...arguments);
        this._open = false;
    }
    static get styles() {
        return [
            css `${unsafeCSS(dialogStyle)}`,
            css `${unsafeCSS(listStyle)}`,
            style
        ];
    }
    get isOpen() {
        return this._mdcComponent ? this._mdcComponent.isOpen : false;
    }
    setOpen(isOpen) {
        this._open = true;
        return this;
    }
    setHeading(heading) {
        this.heading = heading;
        return this;
    }
    setContent(content) {
        this.content = content;
        return this;
    }
    setActions(actions) {
        this.actions = actions;
        return this;
    }
    setDismissAction(action) {
        this.dismissAction = action;
        return this;
    }
    setStyles(styles) {
        this.styles = styles;
        return this;
    }
    setAvatar(avatar) {
        this.avatar = avatar;
        return this;
    }
    open() {
        if (this._mdcElem && !this._mdcComponent) {
            this._mdcComponent = new MDCDialog(this._mdcElem);
            this._mdcComponent.scrimClickAction = this.dismissAction || this.dismissAction === null ? "close" : "";
        }
        if (this._mdcComponent) {
            this._mdcComponent.open();
        }
    }
    close(action) {
        if (this._mdcComponent) {
            this._mdcComponent.close(action);
        }
    }
    disconnectedCallback() {
        super.disconnectedCallback();
        if (this._mdcComponent) {
            this._mdcComponent.destroy();
            this._mdcComponent = undefined;
        }
    }
    render() {
        return html `
            ${typeof (this.styles) === "string" ? html `<style>${this.styles}</style>` : this.styles || ``}
            
            <div id="dialog"
                class="mdc-dialog"
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="dialog-title"
                aria-describedby="dialog-content"
                @MDCDialog:opened="${() => this._onDialogOpened()}"
                @MDCDialog:closed="${(evt) => this._onDialogClosed(evt.detail.action)}">
                <div class="mdc-dialog__container">
                    <div class="mdc-dialog__surface" tabindex="0">
						${typeof (this.heading) === "string" ? html `<h2 class="mdc-dialog__title" id="dialog-title"><or-translate value="${this.heading}"></or-translate></h2>`
            : this.heading ? html `<span class="mdc-dialog__title" id="dialog-title">${this.heading}</span>` : ``}
                        ${this.content ? html ` 
                            <div class="dialog-container mdc-dialog__content" id="dialog-content">
                                ${typeof this.content === "function" ? this.content() : this.content}
                            </div>
                            <footer class="mdc-dialog__actions">
                                ${this.actions ? this.actions.map((action) => {
            return html `
                                    <div class="mdc-button mdc-dialog__button" ?data-mdc-dialog-button-default="${action.default}" data-mdc-dialog-action="${action.disabled ? undefined : action.actionName}">
                                        ${typeof (action.content) === "string" ? html `<or-mwc-input .type="${InputType.BUTTON}" @or-mwc-input-changed="${(ev) => { if (ev.currentTarget.disabled)
                ev.stopPropagation(); }}" .disabled="${action.disabled}" .label="${action.content}"></or-mwc-input>` : action.content}
                                    </div>`;
        }) : ``}
                            </footer>
                        ` : html `
                            <ul class="mdc-list ${this.avatar ? "mdc-list--avatar-list" : ""}">
                                ${!this.actions ? `` : this.actions.map((action, index) => {
            return html `<li class="mdc-list-item" data-mdc-dialog-action="${action.actionName}"><span class="mdc-list-item__text">${action.content}</span></li>`;
        })}
                            </ul>
                        `}
                    </div>
                </div>
                <div class="mdc-dialog__scrim"></div>
            </div>
        `;
    }
    updated(_changedProperties) {
        super.updated(_changedProperties);
        if (_changedProperties.has("_open") && this._open) {
            this.open();
        }
    }
    _onDialogOpened() {
        this.dispatchEvent(new OrMwcDialogOpenedEvent());
    }
    _onDialogClosed(action) {
        if (action === "close" && this.dismissAction && this.dismissAction.action) {
            this.dismissAction.action(this);
        }
        else if (action && this.actions) {
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
};
__decorate([
    property({ type: String })
], OrMwcDialog.prototype, "heading", void 0);
__decorate([
    property({ type: Object, attribute: false })
], OrMwcDialog.prototype, "content", void 0);
__decorate([
    property({ type: Array, attribute: false })
], OrMwcDialog.prototype, "actions", void 0);
__decorate([
    property({ type: Object, attribute: false })
], OrMwcDialog.prototype, "dismissAction", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcDialog.prototype, "avatar", void 0);
__decorate([
    property()
], OrMwcDialog.prototype, "styles", void 0);
__decorate([
    property({ attribute: false })
], OrMwcDialog.prototype, "_open", void 0);
__decorate([
    query("#dialog")
], OrMwcDialog.prototype, "_mdcElem", void 0);
OrMwcDialog = __decorate([
    customElement("or-mwc-dialog")
], OrMwcDialog);
export { OrMwcDialog };
//# sourceMappingURL=or-mwc-dialog.js.map