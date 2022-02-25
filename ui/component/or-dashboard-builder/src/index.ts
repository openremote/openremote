import {html, css, LitElement, unsafeCSS } from "lit";
import { customElement } from "lit/decorators.js";
import {GridStack, GridStackNode, GridStackWidget } from 'gridstack';
import 'gridstack/dist/h5/gridstack-dd-native'; // drag and drop feature

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const gridcss = require('gridstack/dist/gridstack.min.css');
const extracss = require('gridstack/dist/gridstack-extra.css')

// language=CSS
const styling = css`
    .maingrid, .sidebar {
        background-color: #F5F5F5;
        border: 1px solid #E0E0E0;
    }
    .gridItem {
        background: white;
        height: 100%;
    }
    .flex-container {
        display: flex;
        flex-direction: row;
        flex-wrap: nowrap;
        justify-content: normal;
        align-items: normal;
        align-content: normal;
        padding: 64px;
    }
    .flex-item {
        display: block;
        flex-basis: auto;
        align-self: auto;
        order: 0;
        border: 1px solid #E0E0E0;
        padding: 32px;
    }
    .flex-item:nth-child(1) {
        flex-grow: 1;
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
    }
    .sidebarItem > or-icon {
        --or-icon-height: 58px;
        --or-icon-width: 58px;
        margin-bottom: 12px;
    }
`;

@customElement("or-dashboard-builder") // @ts-ignore
export class OrDashboardBuilder extends LitElement {

    // Importing Styles; the unsafe GridStack css, and all custom css
    static get styles() {
        return [unsafeCSS(gridcss), unsafeCSS(extracss), styling]
    }

    // Main constructor; after the component is rendered/updated, we start rendering the grid.
    constructor() {
        super(); this.updateComplete.then(() => {
            if(this.shadowRoot != null) {

                // Setting up main center Grid
                const gridElement = this.shadowRoot.getElementById("gridElement");
                const gridItems = [
                    {x: 0, y: 0, w: 2, h: 2, minW: 2, minH: 2, content: '<div class="gridItem"><span>First Item</span></div>'},
                    {x: 2, y: 1, w: 3, h: 3, minW: 2, minH: 2, content: '<div class="gridItem"><span>Second Item</span></div>'},
                    {x: 6, y: 2, w: 2, h: 2, minW: 2, minH: 2, content: '<div class="gridItem"><span>Third Item</span></div>'}
                ];
                const mainGrid = GridStack.init({
                    acceptWidgets: true,
                    alwaysShowResizeHandle: true,
                    animate: true,
                    cellHeight: "initial",
                    draggable: {
                        appendTo: 'parent'
                    },
                    dragOut: false,
                    float: true,
                    margin: 4,
                    minRow: 7,
                    resizable: {
                        autoHide: false,
                        handles: 'se'
                    }
                    // @ts-ignore typechecking, because we can only provide an HTMLElement (which GridHTMLElement inherits)
                }, gridElement);
                mainGrid.load(gridItems);


                // Setup of Sidebar
                const sidebarElement = this.shadowRoot.getElementById("sidebarElement");
                const sidebarItems = [
                    {x: 0, y: 0, w: 1, h: 1, widgetId: 'sidebar-linechart', locked: true, content: '<div class="sidebarItem"><or-icon icon="chart-bell-curve-cumulative"></or-icon><span>Line Chart</span></div>'},
                    {x: 1, y: 0, w: 1, h: 1, widgetId: 'sidebar-barchart', locked: true, content: '<div class="sidebarItem"><or-icon icon="chart-bar"></or-icon><span>Bar Chart</span></div>'},
                    {x: 0, y: 1, w: 1, h: 1, widgetId: 'sidebar-gauge', locked: true, content: '<div class="sidebarItem"><or-icon icon="speedometer"></or-icon><span>Gauge</span></div>'},
                    {x: 1, y: 1, w: 1, h: 1, locked: true, content: ''} // Invisible widget
                ];
                const sidebarGrid = GridStack.init({
                    acceptWidgets: false,
                    animate: true,
                    cellHeight: "initial",
                    column: 2,
                    disableDrag: false,
                    disableOneColumnMode: true,
                    disableResize: true,
                    draggable: {
                        appendTo: 'parent'
                    },
                    float: true,
                    margin: 8,
                    minWidth: 200,

                    // @ts-ignore typechecking, because we can only provide an HTMLElement (which GridHTMLElement inherits)
                }, sidebarElement);
                sidebarGrid.load(sidebarItems);



                // If an item gets dropped on the main grid, the dragged item needs to be reset to the sidebar.
                // This is done by just loading the initial/original widget back in the sidebar.
                // @ts-ignore typechecking since we assume they are not undefined
                sidebarGrid.on('removed', (event: Event, items: GridStackNode[]) => {
                    const filteredItems = sidebarItems.filter(widget => { return (widget.content == items[0].content); });  // Filter the GridstackWidgets: the input (GridstackNode) extends on GridstackWidget
                    sidebarGrid.load([filteredItems[0]]);
                });



                // Handling dropping of new items
                mainGrid.on('dropped', (event: Event, previousWidget: any, newWidget: any) => {
                    mainGrid.removeWidget(newWidget.el, true, false); // Removes dragged widget first
                    let widgetToAdd: GridStackWidget | undefined;
                    switch(newWidget.widgetId) {
                        case 'sidebar-linechart': {
                            widgetToAdd = {
                                content: '<div class="gridItem"><span>This is a new Line Chart widget.</span></div>',
                                w: 2, h: 2, minW: 2, minH: 2, x: newWidget.x, y: newWidget.y
                            }; break;
                        }
                        case 'sidebar-barchart': {
                            widgetToAdd = {
                                content: '<div class="gridItem"><span>This is a new Bar Chart widget.</span></div>',
                                w: 2, h: 2, minW: 2, minH: 2, x: newWidget.x, y: newWidget.y
                            }; break;
                        }
                        case 'sidebar-gauge': {
                            widgetToAdd = {
                                content: '<div class="gridItem"><span>This is a new Gauge widget.</span></div>',
                                w: 2, h: 2, minW: 2, minH: 2, x: newWidget.x, y: newWidget.y
                            }; break;
                        }
                    }
                    if(widgetToAdd != null) {
                        mainGrid.load([widgetToAdd]);
                    }
                });
            }
        });
    }

    // Rendering the page
    render(): any {
        return html`
            <div class="flex-container">
                <div class="flex-item">
                    <div class="maingrid">
                        <div id="gridElement" class="grid-stack"></div>
                    </div>
                </div>
                <div class="flex-item">
                    <div class="sidebar" style="width: 320px; height: 100%;">
                        <div id="sidebarElement" class="grid-stack" style="width: 100%; height: 100%;"></div>
                    </div>
                </div>
            </div>
        `
    }
}
