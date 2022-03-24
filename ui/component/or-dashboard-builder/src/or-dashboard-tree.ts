import {html, LitElement, unsafeCSS } from "lit";
import { customElement } from "lit/decorators.js";
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import "@openremote/or-icon";
import {style} from "./style";
import { GridStack } from "gridstack";
import 'gridstack/dist/h5/gridstack-dd-native';

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const gridcss = require('gridstack/dist/gridstack.min.css');
const extracss = require('gridstack/dist/gridstack-extra.css');

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
        return [unsafeCSS(gridcss), unsafeCSS(extracss), style];
    }

    constructor() {
        super();
        this.updateComplete.then(() => {
            if(this.shadowRoot != null) {

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
                backgroundGrid.load(sidebarItems); // Loading the same items


                // If an item gets dropped on the main grid, the dragged item needs to be reset to the sidebar.
                // This is done by just loading the initial/original widget back in the sidebar.
                // @ts-ignore typechecking since we assume they are not undefined
                sidebarGrid.on('removed', (event: Event, items: GridStackNode[]) => {
                    const filteredItems = sidebarItems.filter(widget => { return (widget.content == items[0].content); });  // Filter the GridstackWidgets: the input (GridstackNode) extends on GridstackWidget
                    sidebarGrid.load([filteredItems[0]]);
                });
            }
        })
    }

    protected render() {
        return html`
            <div id="header">
                <div>
                    <span>Widgets | Settings</span>
                </div>
            </div>
            <div id="content">
                <div id="item" style="padding: 16px;">
                    <div id="sidebar" style="width: 100%;">
                        <div id="sidebarElement" class="grid-stack" style="width: 100%; z-index: 1001"></div>
                        <div id="sidebarBgElement" class="grid-stack" style="width: 100%; z-index: 1000"></div>
                    </div>
                </div>
            </div>
        `
    }
}
