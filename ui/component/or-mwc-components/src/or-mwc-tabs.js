var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { MDCTabBar } from "@material/tab-bar";
import { css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { DefaultColor4, DefaultColor8 } from "@openremote/core";
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
// Material Styling import
const tabStyle = require("@material/tab/dist/mdc.tab.css");
const tabBarStyle = require("@material/tab-bar/dist/mdc.tab-bar.css");
const tabIndicatorStyle = require("@material/tab-indicator/dist/mdc.tab-indicator.css");
const tabScrollerStyle = require("@material/tab-scroller/dist/mdc.tab-scroller.css");
//language=css
const tabStyling = css `
    .mdc-tab {
        background: var(--or-app-color4, ${unsafeCSS(DefaultColor4)});
    }
    .mdc-tab .mdc-tab__text-label {
        color: var(--or-app-color8, ${unsafeCSS(DefaultColor8)});
    }
    .mdc-tab .mdc-tab__icon {
        color: var(--or-app-color8, ${unsafeCSS(DefaultColor8)})
    }
    .mdc-tab-indicator .mdc-tab-indicator__content--underline {
        border-color: var(--or-app-color8, ${unsafeCSS(DefaultColor8)})
    }
    .mdc-tab__ripple::before, .mdc-tab__ripple::after {
        background-color: var(--ripplecolor, ${unsafeCSS(DefaultColor8)});
    }
    .mdc-tab-vertical {
        height: 72px !important; /* 72px is original Material spec */
    }
    .mdc-tab-vertical__content {
        flex-direction: column;
        gap: 6px; /* 6px is original Material spec */
    }
    .mdc-tab-vertical__text-label {
        padding-left: 0 !important;
    }
`;
/* ----------------- */
let OrMwcTabs = class OrMwcTabs extends LitElement {
    static get styles() {
        return [unsafeCSS(tabStyle), unsafeCSS(tabBarStyle), unsafeCSS(tabIndicatorStyle), unsafeCSS(tabScrollerStyle), tabStyling];
    }
    constructor() {
        super();
        // default values
        if (this.index == null) {
            this.index = 0;
        }
        if (this.items == null) {
            this.items = [];
        }
        if (this.iconPosition == null) {
            this.iconPosition = "left";
        }
        if (this.noScroll == null) {
            this.noScroll = false;
        }
        this.updateComplete.then(() => {
            var _a, _b;
            const tabBarElement = (_a = this.shadowRoot) === null || _a === void 0 ? void 0 : _a.querySelector('.mdc-tab-bar');
            if (tabBarElement != null) {
                this.mdcTabBar = new MDCTabBar(tabBarElement); // init of material component
                this.mdcTabBar.listen("MDCTabBar:activated", (event) => {
                    this.index = event.detail.index;
                });
            }
            if (tabBarElement != null) {
                // Adding classes for Icon top position or not.
                if (this.iconPosition === "top") {
                    if (((_b = this.items) === null || _b === void 0 ? void 0 : _b.find((item) => { return (item.name != null && item.icon != null); })) != undefined) {
                        tabBarElement.querySelectorAll('.mdc-tab').forEach((value) => { value.classList.add('mdc-tab-vertical'); });
                        tabBarElement.querySelectorAll('.mdc-tab__content').forEach((value) => { value.classList.add('mdc-tab-vertical__content'); });
                        tabBarElement.querySelectorAll('.mdc-tab__text-label').forEach((value) => { value.classList.add('mdc-tab-vertical__text-label'); });
                    }
                }
                // Apply colors if set differently
                if (this.bgColor != null) {
                    tabBarElement.querySelectorAll('.mdc-tab').forEach((value) => {
                        value.style.background = this.bgColor;
                    });
                }
                if (this.color != null) {
                    tabBarElement.querySelectorAll('.mdc-tab__text-label').forEach((value) => { value.style.color = this.color; });
                    tabBarElement.querySelectorAll('.mdc-tab__icon').forEach((value) => { value.style.color = this.color; });
                    tabBarElement.querySelectorAll('.mdc-tab-indicator__content--underline').forEach((value) => { value.style.borderColor = this.color; });
                    tabBarElement.querySelectorAll('.mdc-tab__ripple').forEach((value) => { value.style.setProperty('--ripplecolor', this.color); });
                }
            }
        });
    }
    // If an update happens on either the Index or mdcTabBar object.
    updated(changedProperties) {
        if (changedProperties.has('index') || changedProperties.has('mdcTabBar')) {
            if (this.mdcTabBar != null && this.index != null) {
                this.mdcTabBar.activateTab(this.index);
                this.dispatchEvent(new CustomEvent("activated", { detail: { index: this.index } }));
            }
        }
    }
    render() {
        var _a;
        let selectedItem;
        if (this.items != null && this.index != null) {
            selectedItem = this.items[this.index];
        }
        return html `
            ${typeof (this.styles) === "string" ? html `<style>${this.styles}</style>` : this.styles || ``}
            <div>
                <div class="mdc-tab-bar" role="tablist" id="tab-bar">
                    <div class="mdc-tab-scroller">
                        <div class="mdc-tab-scroller__scroll-area" style="overflow-x: ${this.noScroll ? 'hidden' : undefined}">
                            <div class="mdc-tab-scroller__scroll-content">
                                ${(_a = this.items) === null || _a === void 0 ? void 0 : _a.map((menuItem => {
            var _a;
            return html `
                                    <button class="mdc-tab" role="tab" aria-selected="false" tabindex="${(_a = this.items) === null || _a === void 0 ? void 0 : _a.indexOf(menuItem)}">
                                    <span class="mdc-tab__content">
                                        ${menuItem.icon != null ? html `<or-icon icon="${menuItem.icon}" class="mdc-tab__icon"></or-icon>` : null}
                                        ${menuItem.name != null ? html `<span class="mdc-tab__text-label">${menuItem.name}</span>` : null}
                                    </span>
                                        <span class="mdc-tab-indicator">
                                        <span class="mdc-tab-indicator__content mdc-tab-indicator__content--underline"></span>
                                    </span>
                                        <span class="mdc-tab__ripple"></span>
                                    </button>
                                `;
        }))}
                            </div>
                        </div>
                    </div>
                </div>
                <div>
                    <div>
                        <!-- Content of menu Item. Using either unsafeHTML or HTML depending on input type -->
                        ${(selectedItem != null && selectedItem.content != null) ? (typeof selectedItem.content == "string" ? unsafeHTML(selectedItem.content) : html `${selectedItem.content}`) : undefined}
                    </div>
                </div>
            </div>
        `;
    }
};
__decorate([
    property({ type: Number }) // Selected tab index from the items array
], OrMwcTabs.prototype, "index", void 0);
__decorate([
    property({ type: Array })
], OrMwcTabs.prototype, "items", void 0);
__decorate([
    property({ type: String })
], OrMwcTabs.prototype, "iconPosition", void 0);
__decorate([
    property({ type: Boolean })
], OrMwcTabs.prototype, "noScroll", void 0);
__decorate([
    property({ type: String })
], OrMwcTabs.prototype, "bgColor", void 0);
__decorate([
    property({ type: String })
], OrMwcTabs.prototype, "color", void 0);
__decorate([
    property() // Custom styling that gets injected in render()
], OrMwcTabs.prototype, "styles", void 0);
__decorate([
    state()
], OrMwcTabs.prototype, "mdcTabBar", void 0);
OrMwcTabs = __decorate([
    customElement("or-mwc-tabs")
], OrMwcTabs);
export { OrMwcTabs };
//# sourceMappingURL=or-mwc-tabs.js.map