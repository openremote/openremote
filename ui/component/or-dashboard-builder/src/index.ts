import {html, css, LitElement, unsafeCSS } from "lit";
import { customElement, state } from "lit/decorators.js";
import "./or-dashboard-tree";
import "./or-dashboard-browser";
import "./or-dashboard-editor";
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import "@openremote/or-icon";
import {GridStack, GridStackWidget } from 'gridstack';
import 'gridstack/dist/h5/gridstack-dd-native'; // drag and drop feature
import {style} from "./style";
import { MDCTabBar } from "@material/tab-bar";

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const gridcss = require('gridstack/dist/gridstack.min.css');
const extracss = require('gridstack/dist/gridstack-extra.css');
const tabStyle = require("@material/tab/dist/mdc.tab.css");
const tabbarStyle = require("@material/tab-bar/dist/mdc.tab-bar.css");
const tabIndicatorStyle = require("@material/tab-indicator/dist/mdc.tab-indicator.css");
const tabScrollerStyle = require("@material/tab-scroller/dist/mdc.tab-scroller.css");

// language=CSS
const styling = css`
    
    /* Header related styling */
    #header {
        display: table-row;
        height: 0.1%;
        background: white;
    }
    #header-wrapper {
        padding: 20px 20px 14px 20px;
        display: flex;
        flex-direction: row;
    }
    #header-title {
        font-size: 18px;
    }
    #header-title > or-icon {
        margin-right: 10px;
    }
    #header-actions {
        flex: 1 1 auto;
        text-align: right;
    }
    #header-actions-content {
        display: flex;
        flex-direction: row;
        align-items: center;
        float: right;
    }
    
    /* ----------------------------- */
    /* Editor/builder related styling */
    #builder {
        flex-grow: 2;
        align-items: stretch;
        z-index: 0;
        padding: 3vh 4vw 3vh 4vw;
    }
    
    /* ----------------------------- */
    /* Sidebar related styling (drag and drop widgets / configuration) */
    #sidebar {
        display: flex;
        flex-direction: column;
        width: 300px;
        background: white;
    }
    #browser {
        flex-grow: 1;
        align-items: stretch;
        z-index: 1;
        max-width: 300px;
    }
    
    #save-btn { margin-left: 15px; }
    #view-btn { margin-left: 15px; }

    
    /* Material Design Tab Bar overrides (for now just placed them here) */
    .mdc-tab--active .mdc-tab__text-label {
        color: white !important;
    }
    .mdc-tab .mdc-tab__text-label {
        color: rgba(255, 255, 255, 0.74);
    }
    .mdc-tab-indicator .mdc-tab-indicator__content--underline {
        border-color: white;
    }
`;

@customElement("or-dashboard-builder")
export class OrDashboardBuilder extends LitElement {

    // Importing Styles; the unsafe GridStack css, and all custom css
    static get styles() {
        return [unsafeCSS(gridcss), unsafeCSS(extracss), unsafeCSS(tabStyle), unsafeCSS(tabbarStyle), unsafeCSS(tabIndicatorStyle), unsafeCSS(tabScrollerStyle), styling, style]
    }

    @state()
    protected sidebarMenuIndex: number;


    // Main constructor; after the component is rendered/updated, we start rendering the grid.
    constructor() {
        super();
        this.sidebarMenuIndex = 0;
        this.updateComplete.then(() => {
            if(this.shadowRoot != null) {

                // Setting up tabs in sidebar.
                const tabBar = this.shadowRoot.getElementById("tab-bar");
                if (tabBar != null) {
                    const mdcTabBar = new MDCTabBar(tabBar);
                    mdcTabBar.activateTab(this.sidebarMenuIndex); // Activate initial tab
                    mdcTabBar.listen("MDCTabBar:activated", (event: CustomEvent) => {
                        this.sidebarMenuIndex = event.detail.index; // 0 = Widget Browser, and 1 = Settings menu.
                    })
                }
            }
        });
    }

    connectedCallback(): void {
        super.connectedCallback();
    }

    // Rendering the page
    render(): any {
        return html`
            <div id="container" style="display: table;">
                <div id="header">
                    <div id="header-wrapper">
                        <div id="header-title">
                            <!--<or-icon icon="view-dashboard"></or-icon>-->
                            <or-mwc-input type="${InputType.TEXT}" min="1" max="1023" comfortable required outlined label="Name" value="Dashboard 1" style="min-width: 320px;"></or-mwc-input>
                        </div>
                        <div id="header-actions">
                            <div id="header-actions-content">
                                <or-mwc-input id="share-btn" type="${InputType.BUTTON}" icon="share-variant"></or-mwc-input>
                                <or-mwc-input id="save-btn" type="${InputType.BUTTON}" raised label="Save"></or-mwc-input>
                                <or-mwc-input id="view-btn" type="${InputType.BUTTON}" outlined icon="eye" label="View"></or-mwc-input>
                            </div>
                        </div>
                    </div>
                </div>
                <div id="content">
                    <div id="container">
                        <div id="builder">
                            <or-dashboard-editor style="background: transparent;"></or-dashboard-editor>
                        </div>
                        <div id="sidebar">
                            <div id="menu-header" style="display: table-row;">
                                <div class="mdc-tab-bar" role="tablist" id="tab-bar">
                                    <div class="mdc-tab-scroller">
                                        <div class="mdc-tab-scroller__scroll-area">
                                            <div class="mdc-tab-scroller__scroll-content">
                                                <button class="mdc-tab" role="tab" aria-selected="false" tabindex="0">
                                                    <span class="mdc-tab__content">
                                                        <span class="mdc-tab__text-label">WIDGETS</span>
                                                    </span>
                                                    <span class="mdc-tab-indicator">
                                                        <span class="mdc-tab-indicator__content mdc-tab-indicator__content--underline"></span>
                                                    </span>
                                                    <span class="mdc-tab__ripple"></span>
                                                </button>
                                                <button class="mdc-tab" role="tab" aria-selected="false" tabindex="1">
                                                    <span class="mdc-tab__content">
                                                        <span class="mdc-tab__text-label">SETTINGS</span>
                                                    </span>
                                                    <span class="mdc-tab-indicator">
                                                        <span class="mdc-tab-indicator__content mdc-tab-indicator__content--underline"></span>
                                                    </span>
                                                    <span class="mdc-tab__ripple"></span>
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div id="content" style="border: 1px solid #E0E0E0; height: 100%; display: contents;">
                                <or-dashboard-browser id="browser" style="${this.sidebarMenuIndex != 0 ? css`display: none` : null}"></or-dashboard-browser>
                                <div id="item" style="${this.sidebarMenuIndex != 1 ? css`display: none` : null}"> <!-- Setting display to none instead of not rendering it. -->
                                    <span>Settings to display here.</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            
            <!-- Commented code containing logic from the first impleentation, in case we need it. -->
            
            <!--<div class="flex-container">
                <div class="flex-item">
                    <div style="margin-bottom: 12px; width: 100%;">
                        <button @click="{this.compact}" class="mdc-button mdc-button--outlined">Compact</button>
                        <button class="mdc-button mdc-button--outlined">Action 2</button>
                        <button class="mdc-button mdc-button--outlined">Action 3</button>
                        
                        <span style="margin-left: 24px;">Amount of Columns:</span>
                        <label class="mdc-text-field mdc-text-field--outlined mdc-text-field--no-label">
                            <span class="mdc-notched-outline">
                                <span class="mdc-notched-outline__leading"></span>
                                <span class="mdc-notched-outline__trailing"></span>
                            </span>
                            <input class="mdc-text-field__input" type="number" value="12" min="1" max="36" aria-label="Label" @change="{this.changeColumns}">
                        </label>
                        <div style="float: right">
                            <button class="mdc-button mdc-button--outlined" @click="{this.saveDashboard}">Save</button>
                        </div>
                    </div>
                    <div class="maingrid">
                        <div id="gridElement" class="grid-stack"></div>
                    </div>
                </div>
                <div class="flex-item">
                    <div class="sidebar" style="width: 320px; height: 100%;">
                        <div id="sidebarElement" class="grid-stack" style="width: 100%; z-index: 1001"></div>
                        <div id="sidebarBgElement" class="grid-stack" style="width: 100%; z-index: 1000"></div>
                    </div>
                </div>
            </div>-->
        `
    }
}
