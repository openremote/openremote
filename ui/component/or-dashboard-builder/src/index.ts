import {html, css, LitElement, unsafeCSS } from "lit";
import { customElement } from "lit/decorators.js";
import "./or-dashboard-tree";
// import "../../or-mwc-components/src/or-mwc-input"; // temporarily
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import "@openremote/or-icon";
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
        box-sizing: border-box;
        border: 2px solid #E0E0E0;
        border-radius: 4px;
    }
    .flex-container {
        display: flex;
        flex-direction: row;
        flex-wrap: nowrap;
        justify-content: normal;
        align-items: normal;
        align-content: normal;
        /*padding: 64px;*/
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
    #content {
        width: 100%;
        height: 100%;
    }
    #container {
        display: flex;
        width: 100%;
        height: 100%;
    }
    #browser {
        flex-grow: 1;
        align-items: stretch;
        z-index: 1;
        max-width: 300px;
    }
    #builder {
        flex-grow: 2;
        align-items: stretch;
        z-index: 0;
    }
    #title {
        flex: 1 1 auto;
        font-size: 18px;
        font-weight: bold;
        display: flex;
        flex-direction: row;
        align-items: center;
    }

    #title > or-icon {
        margin-right: 10px;
    }
    #wrapper {
        height: 100%;
        width: 100%;
        display: flex;
        flex-direction: row;
    }
    #right-wrapper {
        flex: 1 1 auto;
        text-align: right;
    }
    #save-btn { margin-left: 20px; }
    #view-btn { margin-left: 15px; }
    #view-preset-select { margin-left: 20px; }
    #width-input { margin-left: 20px; }
    #height-input { margin-left: 10px; }
    #rotate-btn { margin-left: 10px; }
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
                        appendTo: 'parent',
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
            <div id="container" style="display: inherit;">
                <div id="header" style="background: white; padding: 20px 20px 14px 20px; border-bottom: solid 1px #e5e5e5;">
                    <div id="wrapper">
                        <div id="title">
                            <!--<or-icon icon="view-dashboard"></or-icon>-->
                            <or-mwc-input type="${InputType.TEXT}" min="1" max="1023" comfortable required outlined label="Name" value="Dashboard 1" style="min-width: 320px;"></or-mwc-input>
                        </div>
                        <div id="right-wrapper">
                            <div style="display: flex; flex-direction: row; align-items: center; float: right;">
                                <or-mwc-input id="share-btn" type="${InputType.BUTTON}" icon="share-variant"></or-mwc-input>
                                <or-mwc-input id="save-btn" type="${InputType.BUTTON}" raised label="Save"></or-mwc-input>
                                <or-mwc-input id="view-btn" type="${InputType.BUTTON}" outlined icon="eye" label="View"></or-mwc-input>
                            </div>
                        </div>
                    </div>
                </div>
                <div id="content">
                    <div id="container">
                        <div id="builder" style="padding: 3vh 4vw 3vh 4vw;">
                            <div style="padding: 24px; display: flex; justify-content: center; align-items: center;">
                                <or-mwc-input id="zoom-btn" type="${InputType.BUTTON}" outlined label="100%"></or-mwc-input>
                                <or-mwc-input id="view-preset-select" type="${InputType.SELECT}" outlined label="Preset size" value="Large" .options="${['Large', 'Medium', 'Small']}" style="min-width: 220px;"></or-mwc-input>
                                <or-mwc-input id="width-input" type="${InputType.NUMBER}" outlined label="Width" min="100" value="1920" style="width: 90px"></or-mwc-input>
                                <or-mwc-input id="height-input" type="${InputType.NUMBER}" outlined label="Height" min="100" value="1080" style="width: 90px;"></or-mwc-input>
                                <or-mwc-input id="rotate-btn" type="${InputType.BUTTON}" icon="screen-rotation"></or-mwc-input>
                            </div>
                            <div style="display: flex; width: 100%; justify-content: center;">
                                <div class="maingrid" style="border: 3px solid #909090; background: #FFFFFF; border-radius: 8px; height: 500px; overflow: auto; width: 900px; padding: 4px;">
                                    <div id="gridElement" class="grid-stack"></div>
                                </div>
                            </div>
                        </div>
                        <or-dashboard-browser id="browser"></or-dashboard-browser>
                    </div>
                </div>
            </div>
            
            
            <!--<div class="flex-container">
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
            </div>-->
        `
    }
}
