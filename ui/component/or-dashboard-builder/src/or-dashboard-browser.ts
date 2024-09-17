import {GridStack, GridStackNode } from "gridstack";
import {css, html, LitElement, unsafeCSS } from "lit";
import { customElement, state} from "lit/decorators.js";
import {style} from "./style";
import {widgetTypes} from "./index";

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
    .grid-stack-item {
        cursor: grab;
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

    @state()
    private sidebarGrid?: GridStack;

    @state()
    private backgroundGrid?: GridStack;


    static get styles() {
        return [unsafeCSS(gridcss), unsafeCSS(extracss), browserStyling, style];
    }

    constructor() {
        super();
        this.updateComplete.then(() => {
            this.renderGrid();
        })
    }

    /* --------------------------------- */

    protected renderGrid() {
        const sidebarElement = this.shadowRoot?.getElementById("sidebarElement");
        const coords: Array<[number, number]> = new Array<[number, number]>([0,0],[2,0], [0,2], [2,2], [0,4], [2,4], [0,6], [2,6], [0,8], [2,8]) // TODO: make this unlimited possibilities with a formula
        const sidebarItems: any[] = Array.from(widgetTypes)
            .sort((a, b) => a[1].displayName.localeCompare(b[1].displayName))
            .map((typeArr, index) => {
                return {x: coords[index][0], y: coords[index][1], w: 2, h: 2, widgetTypeId: typeArr[0], locked: true, content: `<div class="sidebarItem"><or-icon icon="${typeArr[1].displayIcon}"></or-icon><span class="itemText">${typeArr[1].displayName}</span>`}
        });

        // Setting Sidebar height depending on sidebarItems
        let sidebarHeight = 0;
        sidebarItems.forEach((item) => {
            if((item.y + item.h) > sidebarHeight) {
                sidebarHeight = (item.y + item.h);
            }
        });

        let newSidebarHeight = 0;
        if (sidebarItems.length % 2 !== 0) {
            // If sidebarHeight is based on nr of widgets in array & is not even add 1
            newSidebarHeight = sidebarItems.length + 1
        } else {
            newSidebarHeight = sidebarItems.length
        }
        // Creation of the sidebarGrid. Only loads items if already existing
        if(this.sidebarGrid !== undefined) {
            this.sidebarGrid.removeAll();
            this.sidebarGrid.load(sidebarItems);
        } else {
            this.sidebarGrid = GridStack.init({
                acceptWidgets: false,
                column: 4,
                cellHeight: 67,
                disableOneColumnMode: true,
                disableResize: true,
                draggable: {
                    appendTo: 'parent'
                },
                margin: 8,
                row: newSidebarHeight

                // @ts-ignore typechecking, because we can only provide an HTMLElement (which GridHTMLElement inherits)
            }, sidebarElement);
            
            this.sidebarGrid.load(sidebarItems);

            // If an item gets dropped on the main grid, the dragged item needs to be reset to the sidebar.
            // This is done by just loading the initial/original widget back in the sidebar.
            // @ts-ignore typechecking since we assume they are not undefined
            this.sidebarGrid.on('removed', (_event: Event, items: GridStackNode[]) => {
                if(items.length === 1) {
                    const filteredItems = sidebarItems.filter(widget => {
                        return (widget.content === items[0].content);
                    });
                    this.sidebarGrid?.load([filteredItems[0]]);
                }
            });

            // On any change, recreate the grid to prevent users moving Sidebar items around.
            // @ts-ignore typechecking since we assume they are not undefined
            this.sidebarGrid.on('change', (_event: Event, _items: GridStackNode[]) => {
                this.renderGrid();
            });
        }


        // Seperate Static Background grid (to make it look like the items duplicate)
        const sidebarBgElement = this.shadowRoot?.getElementById("sidebarBgElement");
        if(this.backgroundGrid != undefined) {
            this.backgroundGrid.removeAll();
            this.backgroundGrid.load(sidebarItems);

        } else {
            this.backgroundGrid = GridStack.init({
                staticGrid: true,
                column: 4,
                cellHeight: 67,
                disableOneColumnMode: true,
                margin: 8

                // @ts-ignore typechecking, because we can only provide an HTMLElement (which GridHTMLElement inherits)
            }, sidebarBgElement);
            this.backgroundGrid.load(sidebarItems);
        }
    }

    protected render() {
        return html`
            <div id="sidebar">
                <div id="sidebarElement" class="grid-stack" style="width: 100%; z-index: 3;"></div>
                <div id="sidebarBgElement" class="grid-stack" style="width: 100%; z-index: 2"></div>
            </div>
        `
    }
}
