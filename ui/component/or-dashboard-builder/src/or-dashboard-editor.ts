import { GridStack, GridStackWidget } from "gridstack";
import {css, html, LitElement, unsafeCSS } from "lit";
import { customElement, state } from "lit/decorators.js";
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import {style} from "./style";

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

@customElement("or-dashboard-editor")
export class OrDashboardEditor extends LitElement{

    static get styles() {
        return [unsafeCSS(gridcss), unsafeCSS(extracss), editorStyling, style];
    }

    // Variables
    mainGrid: GridStack | undefined;

    @state()
    protected selectedWidgetId: number | undefined;

    constructor() {
        super();
        this.updateComplete.then(() => {
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
                this.mainGrid.load(gridItems);

                if(gridElement != null) {
                    gridElement.style.backgroundSize = "" + this.mainGrid.getCellHeight() + "px " + this.mainGrid.cellWidth() + "px";
                }

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


    // Render
    protected render() {
        return html`
            <div id="view-options">
                <or-mwc-input id="zoom-btn" type="${InputType.BUTTON}" outlined label="100%"></or-mwc-input>
                <or-mwc-input id="view-preset-select" type="${InputType.SELECT}" outlined label="Preset size" value="Large" .options="${['Large', 'Medium', 'Small']}" style="min-width: 220px;"></or-mwc-input>
                <or-mwc-input id="width-input" type="${InputType.NUMBER}" outlined label="Width" min="100" value="1920" style="width: 90px"></or-mwc-input>
                <or-mwc-input id="height-input" type="${InputType.NUMBER}" outlined label="Height" min="100" value="1080" style="width: 90px;"></or-mwc-input>
                <or-mwc-input id="rotate-btn" type="${InputType.BUTTON}" icon="screen-rotation"></or-mwc-input>
            </div>
            <div id="container" style="display: flex; justify-content: center; height: auto;">
                <div id="maingrid">
                    <div id="gridElement" class="grid-stack"></div>
                </div>
            </div>
        `
    }
}
