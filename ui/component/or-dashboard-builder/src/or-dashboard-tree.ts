import {css, html, LitElement, unsafeCSS } from "lit";
import { customElement, state } from "lit/decorators.js";
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import "@openremote/or-icon";
import {style} from "./style";
import { GridStack } from "gridstack";
import 'gridstack/dist/h5/gridstack-dd-native';
import { MDCTabBar } from "@material/tab-bar";

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const gridcss = require('gridstack/dist/gridstack.min.css');
const extracss = require('gridstack/dist/gridstack-extra.css');
const tabStyle = require("@material/tab/dist/mdc.tab.css");
const tabbarStyle = require("@material/tab-bar/dist/mdc.tab-bar.css");
const tabIndicatorStyle = require("@material/tab-indicator/dist/mdc.tab-indicator.css");
const tabScrollerStyle = require("@material/tab-scroller/dist/mdc.tab-scroller.css")

@customElement("or-dashboard-tree")
export class OrDashboardTree extends LitElement {

    // Importing Styles; the unsafe GridStack css, and all custom css
    static get styles() {
        return [style];
    }

    protected render() {
        return html`
            <div id="header">
                <div id="title-container">
                    <span id="title">Dashboards</span>
                </div>
                <div id="header-btns">
                    <or-mwc-input type="${InputType.BUTTON}" icon="delete"></or-mwc-input>
                    <or-mwc-input type="${InputType.BUTTON}" icon="plus"></or-mwc-input>
                </div>
            </div>
            <div id="content">
                <div style="padding: 16px;">
                    <span>Dashboard 1</span>
                </div>
                <div style="padding: 16px;">
                    <span>Dashboard 2</span>
                </div>
                <div style="padding: 16px;">
                    <span>Dashboard 3</span>
                </div>
            </div>
        `
    }
}


@customElement("or-dashboard-browser")
export class OrDashboardBrowser extends LitElement {

    static get styles() {
        return [unsafeCSS(tabStyle), unsafeCSS(tabbarStyle), unsafeCSS(tabIndicatorStyle), unsafeCSS(tabScrollerStyle), unsafeCSS(gridcss), unsafeCSS(extracss), style];
    }

    @state()
    protected sidebarMenuIndex: number;


    constructor() {
        super();
        this.sidebarMenuIndex = 0;
        this.updateComplete.then(() => {
            if (this.shadowRoot != null) {

                // Setup of Sidebar
                const sidebarElement = this.shadowRoot.getElementById("sidebarElement");
                console.log(sidebarElement);
                const sidebarItems = [
                    {x: 0, y: 0, w: 2, h: 2, autoPosition: false, widgetId: 'sidebar-linechart', locked: true, content: '<div class="sidebarItem"><or-icon icon="chart-bell-curve-cumulative"></or-icon><span>Line Chart</span></div>'},
                    {x: 2, y: 0, w: 2, h: 2, autoPosition: false, widgetId: 'sidebar-barchart', locked: true, content: '<div class="sidebarItem"><or-icon icon="chart-bar"></or-icon><span>Bar Chart</span></div>'},
                    {x: 0, y: 2, w: 2, h: 2, autoPosition: false, widgetId: 'sidebar-gauge', locked: true, content: '<div class="sidebarItem"><or-icon icon="speedometer"></or-icon><span>Gauge</span></div>'},
                    {x: 2, y: 2, w: 2, h: 2, autoPosition: false, widgetId: 'sidebar-table', locked: true, content: '<div class="sidebarItem"><or-icon icon="table-large"></or-icon><span>Table</span></div>'},
                    //{x: 2, y: 3, w: 2, h: 2, locked: true, noMove: true, content: 'w'} // Invisible widget
                ];
                const sidebarGrid = GridStack.init({
                    acceptWidgets: false,
                    column: 4,
                    disableOneColumnMode: true,
                    disableResize: true,
                    draggable: {
                        appendTo: 'parent'
                    },
                    margin: 8,
                    maxRow: 4

                    // @ts-ignore typechecking, because we can only provide an HTMLElement (which GridHTMLElement inherits)
                }, sidebarElement);
                sidebarGrid.load(sidebarItems);


                // Seperate Static Background grid (to make it look like the items duplicate)
                const sidebarBgElement = this.shadowRoot.getElementById("sidebarBgElement");
                const backgroundGrid = GridStack.init({
                    staticGrid: true,
                    column: 4,
                    disableOneColumnMode: true,
                    margin: 8

                    // @ts-ignore typechecking, because we can only provide an HTMLElement (which GridHTMLElement inherits)
                }, sidebarBgElement);
                backgroundGrid.load(sidebarItems);


                // If an item gets dropped on the main grid, the dragged item needs to be reset to the sidebar.
                // This is done by just loading the initial/original widget back in the sidebar.
                // @ts-ignore typechecking since we assume they are not undefined
                sidebarGrid.on('removed', (event: Event, items: GridStackNode[]) => {
                    const filteredItems = sidebarItems.filter(widget => {
                        return (widget.content == items[0].content);
                    });  // Filter the GridstackWidgets: the input (GridstackNode) extends on GridstackWidget
                    sidebarGrid.load([filteredItems[0]]);
                });


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
        })
    }

    protected render() {
        return html`
            <div id="header">
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
            <div id="content" style="border: 1px solid #E0E0E0; height: 100%;">
                <div id="item" style="padding: 16px; ${this.sidebarMenuIndex != 0 ? css`display: none` : css``}"> <!-- Setting display to none instead of not rendering it; to prevent rerendering GridStack every time. -->
                    <div id="sidebar">
                        <div id="sidebarElement" class="grid-stack" style="width: 100%; z-index: 1001"></div>
                        <div id="sidebarBgElement" class="grid-stack" style="width: 100%; z-index: 1000"></div>
                    </div>
                </div>  
                <div id="item" style="${this.sidebarMenuIndex != 1 ? css`display: none` : css``}"> <!-- Setting display to none instead of not rendering it. -->
                    <span>Settings to display here.</span>
                </div>
            </div>
        `
    }
}
