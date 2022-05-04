import { MDCTabBar } from "@material/tab-bar";
import {css, CSSResult, html, LitElement, TemplateResult, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import {DefaultColor4, DefaultColor8} from "@openremote/core";
import {unsafeHTML} from 'lit/directives/unsafe-html.js';

// Material Styling import
const tabStyle = require("@material/tab/dist/mdc.tab.css");
const tabBarStyle = require("@material/tab-bar/dist/mdc.tab-bar.css");
const tabIndicatorStyle = require("@material/tab-indicator/dist/mdc.tab-indicator.css");
const tabScrollerStyle = require("@material/tab-scroller/dist/mdc.tab-scroller.css");

export interface OrMwcTabItem {
    name?: string,
    icon?: string,
    content?: TemplateResult | string
}

//language=css
const tabStyling = css`
    .mdc-tab {
        background: ${unsafeCSS(DefaultColor4)};
    }
    .mdc-tab .mdc-tab__text-label {
        color: ${unsafeCSS(DefaultColor8)};
    }
    .mdc-tab .mdc-tab__icon {
        color: ${unsafeCSS(DefaultColor8)};
    }
    .mdc-tab-indicator .mdc-tab-indicator__content--underline {
        border-color: ${unsafeCSS(DefaultColor8)};
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
`

/* ----------------- */

@customElement("or-mwc-tabs")
export class OrMwcTabs extends LitElement {

    static get styles() {
        return [unsafeCSS(tabStyle), unsafeCSS(tabBarStyle), unsafeCSS(tabIndicatorStyle), unsafeCSS(tabScrollerStyle), tabStyling];
    }

    @property({type: Number}) // Selected tab index from the items array
    protected index?: number;

    @property({type: Array})
    protected items?: OrMwcTabItem[];

    @property({type: String})
    protected iconPosition?: "left" | "top";

    @property({type: String})
    protected bgColor?: string;

    @property({type: String})
    protected color?: string;

    @property() // Custom styling that gets injected in render()
    protected styles?: CSSResult | string;

    @state()
    private mdcTabBar: MDCTabBar | undefined;


    constructor() {
        super();

        // default values
        if (this.index == null) { this.index = 0; }
        if (this.items == null) { this.items = []; }
        if (this.iconPosition == null) { this.iconPosition = "left"; }

        this.updateComplete.then(() => {
            const tabBarElement = this.shadowRoot?.querySelector('.mdc-tab-bar');
            if(tabBarElement != null) {
                this.mdcTabBar = new MDCTabBar(tabBarElement); // init of material component
                this.mdcTabBar.listen("MDCTabBar:activated", (event: CustomEvent) => {
                    this.index = event.detail.index;
                });
            }

            if(tabBarElement != null) {

                // Adding classes for Icon top position or not.
                if(this.iconPosition === "top") {
                    if(this.items?.find((item) => { return (item.name != null && item.icon != null); }) != undefined) {
                        tabBarElement.querySelectorAll('.mdc-tab').forEach((value: Element) => { value.classList.add('mdc-tab-vertical'); });
                        tabBarElement.querySelectorAll('.mdc-tab__content').forEach((value: Element) => { value.classList.add('mdc-tab-vertical__content')});
                        tabBarElement.querySelectorAll('.mdc-tab__text-label').forEach((value: Element) => { value.classList.add('mdc-tab-vertical__text-label')});
                    }
                }

                // Apply colors if set differently
                if(this.bgColor != null) {
                    tabBarElement.querySelectorAll('.mdc-tab').forEach((value: Element) => {
                        (value as HTMLElement).style.background = this.bgColor as string;
                    });
                }
                if(this.color != null) {
                    tabBarElement.querySelectorAll('.mdc-tab__text-label').forEach((value: Element) => { (value as HTMLElement).style.color = this.color as string; });
                    tabBarElement.querySelectorAll('.mdc-tab__icon').forEach((value: Element) => { (value as HTMLElement).style.color = this.color as string; })
                    tabBarElement.querySelectorAll('.mdc-tab-indicator__content--underline').forEach((value: Element) => { (value as HTMLElement).style.borderColor = this.color as string; });
                    tabBarElement.querySelectorAll('.mdc-tab__ripple').forEach((value: Element) => { (value as HTMLElement).style.setProperty('--ripplecolor', this.color as string); })
                }
            }
        })
    }

    // If an update happens on either the Index or mdcTabBar object.
    protected updated(changedProperties: Map<string, any>) {
        if(changedProperties.has('index') || changedProperties.has('mdcTabBar')) {
            if(this.mdcTabBar != null && this.index != null) {
                this.mdcTabBar.activateTab(this.index);
                this.dispatchEvent(new CustomEvent("activated", { detail: { index: this.index }}))
            }
        }
    }


    protected render() {
        let selectedItem: OrMwcTabItem | undefined;
        if(this.items != null && this.index != null) {
            selectedItem = this.items[this.index];
        }
        return html`
            ${typeof(this.styles) === "string" ? html`<style>${this.styles}</style>` : this.styles || ``}
            <div>
                <div class="mdc-tab-bar" role="tablist" id="tab-bar">
                    <div class="mdc-tab-scroller">
                        <div class="mdc-tab-scroller__scroll-area">
                            <div class="mdc-tab-scroller__scroll-content">
                                ${this.items?.map((menuItem => { return html`
                                    <button class="mdc-tab" role="tab" aria-selected="false" tabindex="${this.items?.indexOf(menuItem)}">
                                    <span class="mdc-tab__content">
                                        ${menuItem.icon != null ? html`<or-icon icon="${menuItem.icon}" class="mdc-tab__icon"></or-icon>` : null}
                                        ${menuItem.name != null ? html`<span class="mdc-tab__text-label">${menuItem.name}</span>` : null}
                                    </span>
                                        <span class="mdc-tab-indicator">
                                        <span class="mdc-tab-indicator__content mdc-tab-indicator__content--underline"></span>
                                    </span>
                                        <span class="mdc-tab__ripple"></span>
                                    </button>
                                `}))}
                            </div>
                        </div>
                    </div>
                </div>
                <div>
                    <div>
                        <!-- Content of menu Item. Using either unsafeHTML or HTML depending on input type -->
                        ${(selectedItem != null && selectedItem.content != null) ? (typeof selectedItem.content == "string" ? unsafeHTML(selectedItem.content as string) : html`${selectedItem.content}`) : undefined}
                    </div>
                </div>
            </div>
        `
    }
}
