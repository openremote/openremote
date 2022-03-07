import {html, css, LitElement, unsafeCSS } from "lit";
import { customElement } from "lit/decorators.js";
import {GridItemHTMLElement, GridStack, GridStackNode, GridStackWidget } from 'gridstack';
import 'gridstack/dist/h5/gridstack-dd-native'; // drag and drop feature

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const gridcss = require('gridstack/dist/gridstack.min.css');
const extracss = require('gridstack/dist/gridstack-extra.css');
const buttonStyle = require("@material/button/dist/mdc.button.css");
const inputStyle = require("@material/textfield/dist/mdc.textfield.css");

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
    .sidebar {
        display: grid;
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
    #sidebarElement, #sidebarBgElement {
        grid-column: 1;
        grid-row: 1;
    }
`;

@customElement("or-dashboard-builder") // @ts-ignore
export class OrDashboardBuilder extends LitElement {

    // Importing Styles; the unsafe GridStack css, and all custom css
    static get styles() {
        return [unsafeCSS(gridcss), unsafeCSS(extracss), unsafeCSS(buttonStyle), unsafeCSS(inputStyle), styling]
    }

    // Variables
    mainGrid: GridStack | undefined;


    // Main constructor; after the component is rendered/updated, we start rendering the grid.
    constructor() {
        super(); this.updateComplete.then(() => {
            if(this.shadowRoot != null) {

                // Setting up main center Grid
                const gridElement = this.shadowRoot.getElementById("gridElement");
                const gridItems = [
                    {x: 0, y: 0, w: 2, h: 2, minW: 2, minH: 2, content: '<div class="gridItem"><span>First Item</span></div>'},
                    {x: 2, y: 1, w: 3, h: 3, minW: 2, minH: 2, content: '<div class="gridItem"><span>Second Item</span></div>'},
                    {x: 6, y: 2, w: 2, h: 2, minW: 2, minH: 2, content: '<div class="gridItem"><span>Third Item</span></div>'},
                    {x: 6, y: 5, w: 1, h: 1, minW: 1, minH: 1, content: '<div class="gridItem"><span>Fourth Item</span></div>'}
                ];
                this.mainGrid = GridStack.init({
                    acceptWidgets: true,
                    animate: true,
                    cellHeight: 'initial',
                    cellHeightThrottle: 100,
                    draggable: {
                        appendTo: 'parent'
                    },
                    float: true,
                    margin: 4,
                    resizable: {
                        handles: 'all'
                    },
                    // @ts-ignore typechecking, because we can only provide an HTMLElement (which GridHTMLElement inherits)
                }, gridElement);
                this.mainGrid.load(gridItems);


                // Setup of Sidebar
                const sidebarElement = this.shadowRoot.getElementById("sidebarElement");
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



                // Handling dropping of new items
                this.mainGrid.on('dropped', (event: Event, previousWidget: any, newWidget: any) => {
                    if(this.mainGrid != null) {
                        this.mainGrid.removeWidget(newWidget.el, true, false); // Removes dragged widget first
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
                            case 'sidebar-table': {
                                widgetToAdd = {
                                    content: '<div class="gridItem"><span>This is a new Table widget.</span></div>',
                                    w: 2, h: 2, minW: 2, minH: 2, x: newWidget.x, y: newWidget.y
                                }
                            }
                        }
                        if(widgetToAdd != null) {
                            this.mainGrid.load([widgetToAdd]);
                        }
                    }
                });
            }
        });
    }

    saveDashboard(): void {
        if(this.mainGrid != null) {
            console.log(this.mainGrid.save());
        }
    }

    compact(): void {
        if(this.mainGrid != null) {
            this.mainGrid.compact();
        }
    }

    changeColumns(a: any): void {
        if(this.mainGrid != null && a.target.value != null) {
            this.mainGrid.column(a.target.value, 'moveScale');
        }
    }

    connectedCallback(): void {
        super.connectedCallback();

        // Handling window resize for correct display of grid (perfect 1:1 squares)
        /*window.addEventListener("resize", () => {
            console.log("Resizing the Grid..");
            if(this.mainGrid != null) {
                if(this.mainGrid.getColumn() > 1) {
                    this.mainGrid.cellHeight(undefined, true); // undefined makes GridStack use the default option; perfect squares.
                    // @ts-ignore
                    let children = Array.from(this.shadowRoot.getElementById("gridElement").children) as HTMLCollectionOf<HTMLElement>;
                    for(let i in children) {
                        const gridItemHeight = children[i].getAttribute('gs-h');
                        if (gridItemHeight != null) {
                            let width: number = children[i].clientWidth;
                            children[i].style.setProperty('height', (width + 'px'), 'important');
                        }
                    }
                } else {
                    console.log("Oh you are viewing in Mobile mode..")
                    this.mainGrid.cellHeight(160, true)
                }
            }
        }, false);*/
    }

    // Rendering the page
    render(): any {
        return html`
            <div class="flex-container">
                <div class="flex-item">
                    <div style="margin-bottom: 12px; width: 100%;">
                        <button @click="${this.compact}" class="mdc-button mdc-button--outlined">Compact</button>
                        <button class="mdc-button mdc-button--outlined">Action 2</button>
                        <button class="mdc-button mdc-button--outlined">Action 3</button>
                        
                        <span style="margin-left: 24px;">Amount of Columns:</span>
                        <label class="mdc-text-field mdc-text-field--outlined mdc-text-field--no-label">
                            <span class="mdc-notched-outline">
                                <span class="mdc-notched-outline__leading"></span>
                                <span class="mdc-notched-outline__trailing"></span>
                            </span>
                            <input class="mdc-text-field__input" type="number" value="12" min="1" max="36" aria-label="Label" @change="${this.changeColumns}">
                        </label>
                        <div style="float: right">
                            <button class="mdc-button mdc-button--outlined" @click="${this.saveDashboard}">Save</button>
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
            </div>
        `
    }
}
