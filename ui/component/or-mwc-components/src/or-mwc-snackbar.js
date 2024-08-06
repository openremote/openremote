var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { MDCSnackbar } from "@material/snackbar";
const drawerStyle = require("@material/snackbar/dist/mdc.snackbar.css");
export class OrMwcSnackbarChangedEvent extends CustomEvent {
    constructor(value) {
        super(OrMwcSnackbarChangedEvent.NAME, {
            detail: value,
            bubbles: true,
            composed: true
        });
    }
}
OrMwcSnackbarChangedEvent.NAME = "or-mwc-snackbar-changed";
export function showSnackbar(hostElement, text, buttonText, buttonAction) {
    if (!hostElement) {
        hostElement = OrMwcSnackbar.DialogHostElement || document.body;
    }
    const snackbar = new OrMwcSnackbar();
    snackbar.text = text;
    snackbar.buttonText = buttonText;
    snackbar.buttonAction = buttonAction;
    snackbar.isOpen = true;
    snackbar.addEventListener(OrMwcSnackbarChangedEvent.NAME, (ev) => {
        ev.stopPropagation();
        if (!ev.detail.opened) {
            window.setTimeout(() => {
                if (snackbar.parentElement) {
                    snackbar.parentElement.removeChild(snackbar);
                }
            }, 0);
        }
    });
    hostElement.append(snackbar);
    return snackbar;
}
let OrMwcSnackbar = class OrMwcSnackbar extends LitElement {
    constructor() {
        super(...arguments);
        this._open = false;
    }
    static get styles() {
        return [
            css `${unsafeCSS(drawerStyle)}`,
            css `
      `
        ];
    }
    get isOpen() {
        return this._mdcComponent ? this._mdcComponent.isOpen : false;
    }
    set isOpen(isOpen) {
        this._open = true;
    }
    open() {
        if (this._mdcElem && !this._mdcComponent) {
            this._mdcComponent = new MDCSnackbar(this._mdcElem);
            this._mdcComponent.timeoutMs = this.timeout || 4000;
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
            <div id="mdc-snackbar" class="mdc-snackbar" @MDCSnackbar:opened="${() => this.onOpen()}"
                 @MDCSnackbar:closed="${(ev) => this.onClose(ev.detail.reason)}">
                <div class="mdc-snackbar__surface" role="status" aria-relevant="additions">
                    <div class="mdc-snackbar__label" aria-atomic="false">
                        <or-translate value="${this.text}"></or-translate>
                    </div>
                    ${!this.buttonText ? html `` : html `
                        <div class="mdc-snackbar__actions" aria-atomic="true">
                            <or-mwc-input type="button" class="mdc-button mdc-snackbar__action" label="${this.buttonText}">                                
                            </or-mwc-input>
                        </div>
                    `};
                </div>
            </div>
        `;
    }
    updated(_changedProperties) {
        super.updated(_changedProperties);
        if (_changedProperties.has("_open") && this._open) {
            this.open();
        }
    }
    onClose(reason) {
        if (this.buttonAction) {
            this.buttonAction();
        }
        this.dispatchChangedEvent({ opened: false, closeReason: reason });
    }
    onOpen() {
        this.dispatchChangedEvent({ opened: true });
    }
    dispatchChangedEvent(detail) {
        this.dispatchEvent(new OrMwcSnackbarChangedEvent(detail));
    }
};
__decorate([
    property({ type: String, attribute: false })
], OrMwcSnackbar.prototype, "text", void 0);
__decorate([
    property({ type: String })
], OrMwcSnackbar.prototype, "buttonText", void 0);
__decorate([
    property({ type: Object, attribute: false })
], OrMwcSnackbar.prototype, "buttonAction", void 0);
__decorate([
    property({ type: Number })
], OrMwcSnackbar.prototype, "timeout", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcSnackbar.prototype, "_open", void 0);
__decorate([
    query("#mdc-snackbar")
], OrMwcSnackbar.prototype, "_mdcElem", void 0);
OrMwcSnackbar = __decorate([
    customElement("or-mwc-snackbar")
], OrMwcSnackbar);
export { OrMwcSnackbar };
//# sourceMappingURL=or-mwc-snackbar.js.map