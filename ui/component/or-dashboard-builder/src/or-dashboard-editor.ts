import {GridItemHTMLElement, GridStack, GridStackElement, GridStackNode} from "gridstack";
import {css, html, LitElement, PropertyValues, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import {style} from "./style";
import { DashboardGridItem, DashboardScalingPreset, DashboardTemplate, DashboardWidget, DashboardWidgetType } from "@openremote/model";

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const gridcss = require('gridstack/dist/gridstack.min.css');
const extracss = require('gridstack/dist/gridstack-extra.css');

//language=css
const editorStyling = css`
    
    #view-options {
        padding: 24px;
        display: flex;
        justify-content: center;
        align-items: center;
    }
    /* Margins on view options */
    #view-preset-select { margin-left: 20px; }
    #width-input { margin-left: 20px; }
    #height-input { margin-left: 10px; }
    #rotate-btn { margin-left: 10px; }
    
    #maingrid {
        border: 3px solid #909090;
        background: #FFFFFF;
        border-radius: 8px;
        overflow-x: hidden;
        overflow-y: auto;
        height: 500px; /* Should be set according to input */
        width: 900px; /* Should be set according to input */
        padding: 4px;
    }
    .gridItem {
        background: white;
        height: 100%;
        box-sizing: border-box;
        border: 2px solid #E0E0E0;
        border-radius: 4px;
        overflow: hidden;
    }
    .gridItem__active {
        border: 2px solid green;    
    }
    
    /* Grid lines on the background of the grid */
    .grid-stack {
        background-image:
                linear-gradient(90deg, #E0E0E0, transparent 1px),
                linear-gradient(90deg, transparent calc(100% - 1px), #E0E0E0),
                linear-gradient(#E0E0E0, transparent 1px),
                linear-gradient(transparent calc(100% - 1px), #E0E0E0 100%);
    }
`

export interface SelectOutput {
    gridItem: DashboardGridItem | undefined;
}
export interface AddOutput {
    gridStackNode: ORGridStackNode;
}
export interface ORGridStackNode extends GridStackNode {
    widgetType: DashboardWidgetType;
}

@customElement("or-dashboard-editor")
export class OrDashboardEditor extends LitElement{

    static get styles() {
        return [unsafeCSS(gridcss), unsafeCSS(extracss), editorStyling, style];
    }

    // Variables
    mainGrid: GridStack | undefined; // TODO: MAKE NOT UNDEFINED ANYMORE

    @property({type: Object})
    protected widgets: DashboardWidget[] | undefined;

    @property({type: Object})
    protected selected: DashboardWidget | undefined;


    /* ---------------- */

    constructor() {
        super();
        this.updateComplete.then(() => {
            if(this.shadowRoot != null) {

                // Setting up main center Grid
                const gridElement = this.shadowRoot.getElementById("gridElement");
                this.mainGrid = GridStack.init({
                    acceptWidgets: true,
                    animate: true,
                    cellHeight: 'initial',
                    cellHeightThrottle: 100,
                    draggable: {
                        appendTo: 'parent', // Required to work, seems to be Shadow DOM related.
                        scroll: true
                    },
                    float: true,
                    margin: 4,
                    resizable: {
                        handles: 'all'
                    },
                    // @ts-ignore typechecking, because we can only provide an HTMLElement (which GridHTMLElement inherits)
                }, gridElement);
                if(this.widgets != null) {
                    const gridItems: DashboardGridItem[] = [];
                    this.widgets.forEach((widget) => { widget.gridItem != null ? gridItems.push(widget.gridItem) : null; })
                    this.mainGrid.load(gridItems);
                }

                // Adding event listeners on every initial widget
                this.mainGrid.getGridItems().forEach((htmlElement) => {
                    const gridItem = htmlElement.gridstackNode as DashboardGridItem;
                    this.addWidgetEventListeners(gridItem, htmlElement);
                });


                // Render a CSS border raster on the background, and update it on resize.
                if(gridElement != null) {
                    gridElement.style.backgroundSize = "" + this.mainGrid.cellWidth() + "px " + this.mainGrid.getCellHeight() + "px";
                    let previousWidth = gridElement.getBoundingClientRect().width;
                    let resizeObserver = new ResizeObserver((entries) => {
                        const width = entries[0].borderBoxSize?.[0].inlineSize;
                        if (width !== previousWidth) {
                            previousWidth = width;
                            gridElement.style.backgroundSize = "" + this.mainGrid?.cellWidth() + "px " + this.mainGrid?.getCellHeight() + "px";
                        }
                    });
                    resizeObserver.observe(gridElement);
                }

                // Handling dropping of new items
                this.mainGrid.on('dropped', (event: Event, previousWidget: any, newWidget: GridStackNode | undefined) => {
                    if(this.mainGrid != null && newWidget != null) {
                        this.mainGrid.removeWidget((newWidget.el) as GridStackElement, true, false); // Removes dragged widget first
                        this.dispatchEvent(new CustomEvent("add", {detail: { gridStackNode: newWidget } as AddOutput}));
                    }
                });
            }
        });
    }


    // If ANY update happened (also the fields with a partial update)
    requestUpdate(name?: PropertyKey, oldValue?: unknown) {
        console.log("A property has been updated! [" + name?.toString() + "]");

        // When the list of widgets has updated
        if(name?.toString() === "widgets" && this.widgets != null) {
            const gridItems = this.mainGrid?.getGridItems();
            if(gridItems != null) {
                this.widgets.forEach((widget) => {
                    const foundItem = gridItems.find((item) => { return item.gridstackNode?.id == widget.gridItem?.id});
                    if(foundItem == null && widget.gridItem != null && this.mainGrid != null) {
                        const htmlElement: GridItemHTMLElement = this.mainGrid.addWidget(widget.gridItem);
                        this.addWidgetEventListeners(widget.gridItem, htmlElement);
                    }
                });
            }
        }

        // On widget selected status change (select AND deselect)
        if(name?.toString() == "selected") {
            if(this.selected == undefined) {
                this.mainGrid?.getGridItems().forEach(item => {
                    this.deselectGridItem(item);
                });
            } else {
                const foundItem = this.mainGrid?.getGridItems().find((item) => { return item.gridstackNode?.id == this.selected?.gridItem?.id});
                if(foundItem != null) {
                    this.selectGridItem(foundItem);
                }
            }
        }
        return super.requestUpdate(name, oldValue);
    }


    // Adding HTML event listeners (for example selecting/deselecting)
    addWidgetEventListeners(gridItem: DashboardGridItem, htmlElement: GridItemHTMLElement) {
        htmlElement.addEventListener("click", (event) => {
            if(this.selected?.gridItem?.id == gridItem.id) {
                this.dispatchEvent(new CustomEvent("deselect", { detail: { gridItem: gridItem }}));
            } else {
                this.dispatchEvent(new CustomEvent("select", { detail: { gridItem: gridItem } as SelectOutput}));
            }
        });
    }


    selectGridItem(gridItem: GridItemHTMLElement) {
        if(this.mainGrid != null) {
            this.mainGrid.getGridItems().forEach(item => { this.deselectGridItem(item); }); // deselecting all other items
            gridItem.querySelectorAll<HTMLElement>(".gridItem").forEach((item: HTMLElement) => {
                item.classList.add('gridItem__active'); // Apply active CSS class
            });
        }
    }
    deselectGridItem(gridItem: GridItemHTMLElement) {
        gridItem.querySelectorAll<HTMLElement>(".gridItem__active").forEach((item: HTMLElement) => {
            item.classList.remove('gridItem__active'); // Remove active CSS class
        });
    }


    // Render
    protected render() {
        return html`
            <div>
                <div id="view-options">
                    <or-mwc-input id="zoom-btn" type="${InputType.BUTTON}" outlined label="100%"></or-mwc-input>
                    <or-mwc-input id="view-preset-select" type="${InputType.SELECT}" outlined label="Preset size" value="Large" .options="${['Large', 'Medium', 'Small']}" style="min-width: 220px;"></or-mwc-input>
                    <or-mwc-input id="width-input" type="${InputType.NUMBER}" outlined label="Width" min="100" value="1920" style="width: 90px"></or-mwc-input>
                    <or-mwc-input id="height-input" type="${InputType.NUMBER}" outlined label="Height" min="100" value="1080" style="width: 90px;"></or-mwc-input>
                    <or-mwc-input id="rotate-btn" type="${InputType.BUTTON}" icon="screen-rotation"></or-mwc-input>
                </div>
                <div id="container" style="display: flex; justify-content: center; height: auto;">
                    <div id="maingrid">
                        <!-- Gridstack element on which the Grid will be rendered -->
                        <div id="gridElement" class="grid-stack"></div>
                    </div>
                </div>
            </div>
        `
    }
}
