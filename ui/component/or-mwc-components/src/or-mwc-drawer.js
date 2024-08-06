var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, query } from "lit/decorators.js";
import { MDCDrawer } from "@material/drawer";
import { classMap } from "lit/directives/class-map.js";
const drawerStyle = require("@material/drawer/dist/mdc.drawer.css");
export class OrMwcDrawerChangedEvent extends CustomEvent {
    constructor(value) {
        super(OrMwcDrawerChangedEvent.NAME, {
            detail: value,
            bubbles: true,
            composed: true
        });
    }
}
OrMwcDrawerChangedEvent.NAME = "or-mwc-drawer-changed";
let OrMwcDrawer = class OrMwcDrawer extends LitElement {
    constructor() {
        super(...arguments);
        this.dismissible = false;
        this.rightSided = false;
        this.transparent = false;
        this.open = false;
    }
    static get styles() {
        return [
            css `${unsafeCSS(drawerStyle)}`,
            css `
      .transparent{
        background: none;
      }`
        ];
    }
    toggle() {
        this.open = !this.open;
    }
    disconnectedCallback() {
        super.disconnectedCallback();
        if (this.drawer) {
            this.drawer.destroy();
            this.drawer = undefined;
        }
    }
    render() {
        const isModal = !this.dismissible;
        const classes = {
            "mdc-drawer--dismissible": this.dismissible,
            "mdc-drawer--modal": isModal,
            "transparent": this.transparent
        };
        return html `
      <aside class="mdc-drawer ${classMap(classes)}" dir="${(this.rightSided ? "rtl" : "ltr")}">
        ${this.header}
        <div class="mdc-drawer__content" dir="ltr">
          <slot></slot>
        </div>
      </aside>`;
    }
    updated() {
        if (this.drawer.open !== this.open) {
            this.drawer.open = this.open;
        }
    }
    dispatchChangedEvent(value) {
        this.dispatchEvent(new OrMwcDrawerChangedEvent(value));
    }
    firstUpdated() {
        this.drawer = MDCDrawer.attachTo(this.drawerElement);
        const openHandler = () => { this.dispatchChangedEvent(true); };
        const closeHandler = () => { this.dispatchChangedEvent(false); };
        this.drawer.listen("MDCDrawer:opened", openHandler);
        this.drawer.listen("MDCDrawer:closed", closeHandler);
        if (this.appContent) {
            this.appContent.classList.add("mdc-drawer-app-content");
        }
        if (this.topBar) {
            this.topBar.classList.add("mdc-top-app-bar");
        }
    }
};
__decorate([
    property({ attribute: false })
], OrMwcDrawer.prototype, "header", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcDrawer.prototype, "dismissible", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcDrawer.prototype, "rightSided", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcDrawer.prototype, "transparent", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcDrawer.prototype, "open", void 0);
__decorate([
    property({ attribute: false })
], OrMwcDrawer.prototype, "appContent", void 0);
__decorate([
    property({ attribute: false })
], OrMwcDrawer.prototype, "topBar", void 0);
__decorate([
    query(".mdc-drawer")
], OrMwcDrawer.prototype, "drawerElement", void 0);
OrMwcDrawer = __decorate([
    customElement("or-mwc-drawer")
], OrMwcDrawer);
export { OrMwcDrawer };
//# sourceMappingURL=or-mwc-drawer.js.map