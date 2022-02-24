import {html, css, LitElement, unsafeCSS } from "lit";
import { customElement } from "lit/decorators.js";
import { GridStack } from 'gridstack';
import 'gridstack/dist/h5/gridstack-dd-native'; // drag and drop feature

// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const gridcss = require('gridstack/dist/gridstack.min.css');

// language=CSS
const styling = css`
    .grid-stack {
        background-color: #fafad2;
        border: 1px solid #E0E0E0;
    }
    .grid-stack-item-content {
        background-color: #18bc9c;
    }
`;


@customElement("or-dashboard-builder")
export class OrDashboardBuilder extends LitElement {

    // Importing Styles; the unsafe GridStack css, and all custom css
    static get styles() {
        return [unsafeCSS(gridcss), styling]
    }

    // Main constructor; after the component is rendered/updated, we start rendering the grid.
    constructor() {
        super(); this.updateComplete.then(() => {
            if(this.shadowRoot != null) {
                const gridElement = this.shadowRoot.getElementById("gridElement");
                const gridItems = [
                    {x: 0, y: 0, w: 3, h: 3, minW: 3, minH: 3, content: '<span>First Item</span>'},
                    {x: 2, y: 3, w: 4, h: 4, minW: 3, minH: 3, content: '<span>Second Item</span>'},
                    {x: 6, y: 3, w: 3, h: 3, minW: 3, minH: 3, content: '<span>Third Item</span>'}
                ];
                const grid = GridStack.init({
                    draggable: {
                        appendTo: 'parent'
                    },
                    float: true
                    // @ts-ignore
                }, gridElement); // We ignore type checking on gridElement, because we can only provide an HTMLElement (which GridStackElement inherits)
                grid.load(gridItems);
            }
        });
    }

    // Rendering the page
    render(): any {
        return html`
            <div style="padding: 100px;">
                <div id="gridElement" class="grid-stack"></div>
            </div>
            <div>
                <span>Bottom Content of the or-dashboard-builder component</span>
            </div>
        `
    }
}
