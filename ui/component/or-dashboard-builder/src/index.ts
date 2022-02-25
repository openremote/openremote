import {html, css, LitElement, unsafeCSS } from "lit";
import { customElement } from "lit/decorators.js";
import {GridItemHTMLElement, GridStack, GridStackEngine, GridStackNode, GridStackWidget } from 'gridstack';
import 'gridstack/dist/h5/gridstack-dd-native'; // drag and drop feature

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const gridcss = require('gridstack/dist/gridstack.min.css');
const extracss = require('gridstack/dist/gridstack-extra.css')

// language=CSS
const styling = css`
    .grid-stack {
        background-color: #F5F5F5;
        border: 1px solid #E0E0E0;
    }
    .grid-stack-item-content {
        background-color: #18bc9c;
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
    #linechartDraggable, #barchartDraggable, #piechartDraggable {
        width: 100px;
        padding: 18px;
        background: #E0E0E0;
        text-align: center;
    }
    .sidebarItem {
        height: 100%;
        background: white;
        display: flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;
        border: 1px solid #E0E0E0;
        box-sizing: border-box;
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
                const gridElement = this.shadowRoot.getElementById("gridElement");
                const gridItems = [
                    {x: 0, y: 0, w: 2, h: 2, minW: 2, minH: 2, content: '<span>First Item</span>'},
                    {x: 2, y: 1, w: 3, h: 3, minW: 2, minH: 2, content: '<span>Second Item</span>'},
                    {x: 6, y: 2, w: 2, h: 2, minW: 2, minH: 2, content: '<span>Third Item</span>'}
                ];
                const mainGrid = GridStack.init({
                    acceptWidgets: true,
                    alwaysShowResizeHandle: true,
                    animate: true,
                    cellHeight: "initial",
                    draggable: {
                        appendTo: 'parent'
                    },
                    dragInOptions: {
                        helper: 'clone'
                    },
                    dragOut: false,
                    float: true,
                    margin: 4,
                    minRow: 5,
                    resizable: {
                        autoHide: false,
                        handles: 'se'
                    }
                    // @ts-ignore
                }, gridElement); // We ignore type checking on gridElement, because we can only provide an HTMLElement (which GridHTMLElement inherits)

                mainGrid.load(gridItems);
                mainGrid.on('added', () => { console.log("'added' event of GridStack got ."); });
                mainGrid.on('change', () => { console.log("'change' event of GridStack got ."); });
                mainGrid.on('dragstart', () => { console.log("'dragstart' event of GridStack got ."); });
                mainGrid.on('drag', () => { console.log("'drag' event of GridStack got ."); });
                mainGrid.on('dragstop', () => { console.log("'dragstop' event of GridStack got ."); });
                mainGrid.on('dropped', () => { console.log("'dropped' event of GridStack got ."); });
                mainGrid.on('removed', () => { console.log("'removed' event of GridStack got ."); });
                mainGrid.on('resizestart', () => { console.log("'resizestart' event of GridStack got ."); });
                mainGrid.on('resize', () => { console.log("'resize' event of GridStack got ."); });
                mainGrid.on('resizestop', () => { console.log("'resizestop' event of GridStack got ."); });



                const sidebarElement = this.shadowRoot.getElementById("sidebarElement");
                const sidebarItems: GridStackWidget[] = [
                    {x: 0, y: 0, w: 1, h: 1, locked: true, content: '<div class="sidebarItem"><span>Line Chart</span></div>'},
                    {x: 1, y: 0, w: 1, h: 1, locked: true, content: '<div class="sidebarItem"><span>Bar Chart</span></div>'},
                    {x: 0, y: 1, w: 1, h: 1, locked: true, content: '<div class="sidebarItem"><span>Pie Chart</span></div>'}
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
                    minRow: 5,
                    minWidth: 200,

                    // @ts-ignore
                }, sidebarElement); // We ignore type checking on gridElement, because we can only provide an HTMLElement (which GridHTMLElement inherits)

                sidebarGrid.load(sidebarItems);

                sidebarGrid.on('added', () => { console.log("'added' event of SidebarGrid got ."); });
                sidebarGrid.on('change', () => { console.log("'change' event of SidebarGrid got ."); });
                sidebarGrid.on('dragstart', () => { console.log("'dragstart' event of SidebarGrid got ."); });
                sidebarGrid.on('drag', () => { console.log("'drag' event of SidebarGrid got ."); });
                sidebarGrid.on('dragstop', () => { console.log("'dragstop' event of SidebarGrid got ."); });
                sidebarGrid.on('dropped', () => { console.log("'dropped' event of SidebarGrid got ."); });
                sidebarGrid.on('removed', () => { console.log("'removed' event of SidebarGrid got ."); });
                sidebarGrid.on('resizestart', () => { console.log("'resizestart' event of SidebarGrid got ."); });
                sidebarGrid.on('resize', () => { console.log("'resize' event of SidebarGrid got ."); });
                sidebarGrid.on('resizestop', () => { console.log("'resizestop' event of SidebarGrid got ."); });


                // @ts-ignore
                sidebarGrid.on('removed', (event: Event, items: GridStackNode[]) => {
                    const filteredItems = sidebarItems.filter(widget => { return (widget.content == items[0].content); });  // Filter the GridstackWidgets: the input (GridstackNode) extends on GridstackWidget
                    sidebarGrid.load([filteredItems[0]]);
                });
            }
        });
    }

    // Rendering the page
    render(): any {
        return html`
            <div class="flex-container">
                <div class="flex-item">
                    <div>
                        <div id="gridElement" class="grid-stack"></div>
                    </div>
                </div>
                <div class="flex-item">
                    <div class="sidebar" style="width: 320px;">
                        <div id="sidebarElement" class="grid-stack" style="width: 100%;"></div>
                    </div>
                </div>
            </div>
        `
    }
}
