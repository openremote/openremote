import { GridStack } from "gridstack";
import {css, html, LitElement, unsafeCSS } from "lit";
import { customElement} from "lit/decorators.js";
import {style} from "./style";
import { DashboardWidgetType } from "@openremote/model";

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const gridcss = require('gridstack/dist/gridstack.min.css');
const extracss = require('gridstack/dist/gridstack-extra.css');

//language=css
const browserStyling = css`
    #sidebar {
        display: grid;
        padding: 16px;
    }
    #sidebarElement, #sidebarBgElement {
        grid-column: 1;
        grid-row: 1;
    }
    .sidebarItem {
        height: 100%;
        background: white;
        display: flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;
        border: 1px solid #E0E0E0;
        border-radius: 8px;
        box-sizing: border-box;
        flex-direction: column;
        font-size: 14px;
        --or-icon-width: 36px;
    }
    .itemText {
        margin-top: 10px;
    }
`

@customElement("or-dashboard-browser")
export class OrDashboardBrowser extends LitElement {

    static get styles() {
        return [unsafeCSS(gridcss), unsafeCSS(extracss), browserStyling, style];
    }

    constructor() {
        super();
        this.updateComplete.then(() => {
            if (this.shadowRoot != null) {

                // Setup of Sidebar
                const sidebarElement = this.shadowRoot.getElementById("sidebarElement");
                const sidebarItems = [
                    {x: 0, y: 0, w: 2, h: 2, autoPosition: false, widgetType: DashboardWidgetType.CHART, locked: true, content: '<div class="sidebarItem"><or-icon icon="chart-bell-curve-cumulative"></or-icon><span class="itemText">Line Chart</span></div>'},
/*                    {x: 2, y: 0, w: 2, h: 2, autoPosition: false, widgetType: DashboardWidgetType.CHART, locked: true, content: '<div class="sidebarItem"><or-icon icon="chart-bar"></or-icon><span>Bar Chart</span></div>'},
                    {x: 0, y: 2, w: 2, h: 2, autoPosition: false, widgetType: DashboardWidgetType.CHART, locked: true, content: '<div class="sidebarItem"><or-icon icon="speedometer"></or-icon><span>Gauge</span></div>'},*/
                    {x: 2, y: 0, w: 2, h: 2, autoPosition: false, widgetType: DashboardWidgetType.KPI, locked: true, content: '<div class="sidebarItem"><or-icon icon="label"></or-icon><span class="itemText">KPI</span></div>'},
                    //{x: 2, y: 3, w: 2, h: 2, locked: true, noMove: true, content: 'w'} // Invisible widget
                ];
                const sidebarGrid = GridStack.init({
                    acceptWidgets: false,
                    column: 4,
                    cellHeight: 67,
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
                    cellHeight: 67,
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
            }
        })
    }

    protected render() {
        return html`
            <div id="sidebar">
                <div id="sidebarElement" class="grid-stack" style="width: 100%; z-index: 6;"></div>
                <div id="sidebarBgElement" class="grid-stack" style="width: 100%; z-index: 5"></div>
            </div>
        `
    }
}
