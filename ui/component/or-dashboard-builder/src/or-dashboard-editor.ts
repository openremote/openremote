import {GridItemHTMLElement, GridStack, GridStackElement, GridStackNode} from "gridstack";
import {css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import {style} from "./style";
import manager, {DefaultColor4} from "@openremote/core";
import {Attribute, AttributeRef, DashboardGridItem, DashboardTemplate, DashboardWidget, DashboardWidgetType } from "@openremote/model";
import { OrInputChangedEvent } from "../../or-mwc-components/lib/or-mwc-input";

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
    
    .maingrid {
        border: 3px solid #909090;
        background: #FFFFFF;
        border-radius: 8px;
        overflow-x: hidden;
        overflow-y: auto;
        height: 540px; /* TODO: Should be set according to input */
        width: 960px; /* TODO: Should be set according to input */
        padding: 4px;
        position: absolute;
        z-index: 0;
    }
    .maingrid__fullscreen {
        border: none;
        background: transparent;
        border-radius: 0;
        overflow-x: hidden;
        overflow-y: auto;
        height: auto;
        width: 100%;
        padding: 4px;
        pointer-events: none;
    }
    .maingrid__disabled {
        pointer-events: none;
        opacity: 40%;
    }
    .grid-stack-item-content {
        background: white;
        box-sizing: border-box;
        border: 2px solid #E0E0E0;
        border-radius: 4px;
        overflow: hidden;
    }
    .grid-stack-item-content__active {
        border: 2px solid ${unsafeCSS(DefaultColor4)};    
    }
    .gridItem {
        height: 100%;
        overflow: hidden;
    }
    
    /* Grid lines on the background of the grid */
    .grid-element {
        background-image:
                linear-gradient(90deg, #E0E0E0, transparent 1px),
                linear-gradient(90deg, transparent calc(100% - 1px), #E0E0E0),
                linear-gradient(#E0E0E0, transparent 1px),
                linear-gradient(transparent calc(100% - 1px), #E0E0E0 100%);
    }
`

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

    @property() // required to work!
    protected readonly template: DashboardTemplate | undefined;

    @property({type: Object})
    protected selected: DashboardWidget | undefined;

    @property()
    protected readonly editMode: boolean | undefined;

    @property()
    protected width: number = 960;

    @property()
    protected height: number = 540;

    @property()
    protected readonly isLoading: boolean | undefined;

    @state()
    private previewHeight: number | undefined;

    @state()
    private previewWidth: number | undefined;


    /* ---------------- */

    constructor() {
        super();
        if(this.editMode == undefined) { this.editMode = true; } // default
        this.isLoading = false;

        if(this.editMode) {
            if(this.previewHeight == undefined) { this.previewHeight = 540; }
            if(this.previewWidth == undefined) { this.previewWidth = 940; }
        }
    }


    updated(changedProperties: Map<string, any>) {
        console.log(changedProperties);

        // Template input changes
        if(changedProperties.has("template") || changedProperties.has("editMode")) {
            this.renderGrid();
        }

        // Width or height input changes
        if(changedProperties.has("width") || changedProperties.has("height")) {
            if(this.shadowRoot != null) {
                const gridHTML = this.shadowRoot.querySelector(".maingrid") as HTMLElement;
                gridHTML.style.width = (this.width + 'px');
                gridHTML.style.height = (this.height + 'px');
                this.renderGrid();
                // this.updateGridSize(gridHTML);
            }
        }

        // When the Loading State changes
        if(changedProperties.has("isLoading") && this.mainGrid != null && this.shadowRoot != null) {
            if(this.isLoading) {
                this.mainGrid.disable();
                this.shadowRoot.getElementById("maingrid")?.classList.add("maingrid__disabled");
            } else {
                this.mainGrid.enable();
                this.shadowRoot.getElementById("maingrid")?.classList.remove("maingrid__disabled");
            }
        }

        // User selected a Widget
        if(changedProperties.has("selected")) {
            if(this.selected != undefined) {
                if(changedProperties.get("selected") != undefined) { // if previous selected state was a different widget
                    this.dispatchEvent(new CustomEvent("deselected", { detail: changedProperties.get("selected") as DashboardWidget }));
                }
                const foundItem = this.mainGrid?.getGridItems().find((item) => { return item.gridstackNode?.id == this.selected?.gridItem?.id});
                if(foundItem != null) {
                    this.selectGridItem(foundItem);
                }
                this.dispatchEvent(new CustomEvent("selected", { detail: this.selected }));

            } else {
                this.mainGrid?.getGridItems().forEach(item => {
                    this.deselectGridItem(item);
                });
                this.dispatchEvent(new CustomEvent("deselected", { detail: changedProperties.get("selected") as DashboardWidget }));
            }
        }
    }

    /* -------------------------------------------------------- */

    // Main large method for rendering the Grid
    private async renderGrid(): Promise<void> {
        if(this.shadowRoot != null) {

            console.log("Rendering the Grid...");

            const mainGridContainer = this.shadowRoot.querySelector(".maingrid") as HTMLElement;
            if(!this.editMode) {
                mainGridContainer.classList.add("maingrid__fullscreen");
            } else {
                if(mainGridContainer.classList.contains("maingrid__fullscreen")) {
                    mainGridContainer.classList.remove("maingrid__fullscreen");
                }
            }

            // Setting up main center Grid
            const gridElement = this.shadowRoot.getElementById("gridElement");
            this.mainGrid = GridStack.init({
                acceptWidgets: (this.editMode),
                animate: true,
                cellHeight: 'auto',
                cellHeightThrottle: 100,
                disableDrag: (!this.editMode),
                disableResize: (!this.editMode),
                draggable: {
                    appendTo: 'parent', // Required to work, seems to be Shadow DOM related.
                    scroll: true
                },
                float: true,
                margin: 4,
                resizable: {
                    handles: 'all'
                },
                minRow: (this.editMode ? 10 : undefined)
                // @ts-ignore typechecking, because we can only provide an HTMLElement (which GridHTMLElement inherits)
            }, gridElement);

            // Set amount of columns if different from default (which is 12)
            if(this.template?.columns != null) {
                this.mainGrid.column(this.template.columns);
            }

            // Add widgets of template onto the Grid
            if(this.template != null && this.template.widgets != null) {
                const gridItems: DashboardGridItem[] = [];
                for (const widget of this.template.widgets) {
                    widget.gridItem != null ? gridItems.push((await this.loadWidget(widget)).gridItem as DashboardGridItem) : null;
                }
                this.mainGrid.load(gridItems);
            }

            // Add event listeners to Grid
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
                        this.updateGridSize(gridElement);
                    }
                });
                resizeObserver.observe(gridElement);
            }

            // Handling dropping of new items
            this.mainGrid.on('dropped', (event: Event, previousWidget: any, newWidget: GridStackNode | undefined) => {
                if(this.mainGrid != null && newWidget != null) {
                    this.mainGrid.removeWidget((newWidget.el) as GridStackElement, true, false); // Removes dragged widget first
                    this.dispatchEvent(new CustomEvent("dropped", { detail: newWidget }));
                }
            });

            // Handling changes of items (resizing, moving around etc)
            this.mainGrid.on('change', (event: Event, items: any) => {
                if(this.template != null && this.template.widgets != null) {
                    (items as GridStackNode[]).forEach(node => {
                        const widget: DashboardWidget | undefined = this.template?.widgets?.find(widget => { return widget.gridItem?.id == node.id; });
                        if(widget != null && widget.gridItem != null) {
                            console.log("Updating properties of " + widget.displayName);
                            widget.gridItem.x = node.x;
                            widget.gridItem.y = node.y;
                            widget.gridItem.w = node.w;
                            widget.gridItem.h = node.h;
                            widget.gridItem.content = node.content;
                        }
                    });
                    this.dispatchEvent(new CustomEvent("changed", {detail: { template: this.template }}));
                }
            });

            // Making all GridStack events dispatch on this component as well.
            this.mainGrid.on("added", (event: Event, items: any) => { this.dispatchEvent(new CustomEvent("added", {detail: { event: event, items: items }})); });
            this.mainGrid.on("change", (event: Event, items: any) => { this.dispatchEvent(new CustomEvent("change", { detail: { event: event, items: items }})); });
            this.mainGrid.on("disable", (event: Event) => { this.dispatchEvent(new CustomEvent("disable", { detail: { event: event }})); });
            this.mainGrid.on("dragstart", (event: Event, el: any) => { this.dispatchEvent(new CustomEvent("dragstart", { detail: { event: event, el: el }})); });
            this.mainGrid.on("drag", (event: Event, el: any) => { this.dispatchEvent(new CustomEvent("drag", { detail: { event: event, el: el }})); });
            this.mainGrid.on("dragstop", (event: Event, el: any) => { this.dispatchEvent(new CustomEvent("dragstop", { detail: { event: event, el: el }})); });
            this.mainGrid.on("enable", (event: Event) => { this.dispatchEvent(new CustomEvent("enable", { detail: { event: event }})); });
            this.mainGrid.on("removed", (event: Event, items: any) => { this.dispatchEvent(new CustomEvent("removed", { detail: { event: event, items: items }})); });
            this.mainGrid.on("resizestart", (event: Event, el: any) => { this.dispatchEvent(new CustomEvent("resizestart", { detail: { event: event, el: el }})); });
            this.mainGrid.on("resize", (event: Event, el: any) => { this.dispatchEvent(new CustomEvent("resize", { detail: { event: event, el: el }})); });
            this.mainGrid.on("resizestop", (event: Event, el: any) => { this.dispatchEvent(new CustomEvent("resizestop", { detail: { event: event, el: el }})); });
        }
    }

    updateGridSize(gridElement: HTMLElement) {
        gridElement.style.backgroundSize = "" + this.mainGrid?.cellWidth() + "px " + this.mainGrid?.getCellHeight() + "px";
    }


    /* --------------------- */


    // Adding HTML event listeners (for example selecting/deselecting)
    addWidgetEventListeners(gridItem: DashboardGridItem, htmlElement: GridItemHTMLElement) {
        if(htmlElement.onclick == null) {
            htmlElement.onclick = (event) => {
                if(this.selected?.gridItem?.id == gridItem.id) {
                    this.selected = undefined;
                } else {
                    this.selected = this.template?.widgets?.find(widget => { return widget.gridItem?.id == gridItem.id; });
                }
            };
        }
    }


    selectGridItem(gridItem: GridItemHTMLElement) {
        if(this.mainGrid != null) {
            this.mainGrid.getGridItems().forEach(item => { this.deselectGridItem(item); }); // deselecting all other items
            gridItem.querySelectorAll<HTMLElement>(".grid-stack-item-content").forEach((item: HTMLElement) => {
                item.classList.add('grid-stack-item-content__active'); // Apply active CSS class
            });
        }
    }
    deselectGridItem(gridItem: GridItemHTMLElement) {
        gridItem.querySelectorAll<HTMLElement>(".grid-stack-item-content").forEach((item: HTMLElement) => {
            item.classList.remove('grid-stack-item-content__active'); // Remove active CSS class
        });
    }


    /* ------------------------------ */

    async loadWidget(widget: DashboardWidget): Promise<DashboardWidget> {
        const _widget = Object.assign({}, widget);
        if(_widget.gridItem != null) {
            switch(_widget.widgetType) {
                case DashboardWidgetType.CHART: {
/*                    const chartConfig: OrChartConfig = {
                        chart: {
                            xLabel: "X Label",
                            yLabel: "Y Label"
                        },
                        realm: manager.displayRealm,
                        views: {
                            ["dashboards"]: {
                                [widget.displayName as string]: {
                                    attributeRefs: widget.dataConfig?.attributes
                                }
                            }
                        }
                    }*/
                    const response = await manager.rest.api.AssetResource.queryAssets({
                        ids: widget.widgetConfig?.attributeRefs?.map((x: AttributeRef) => { return x.id; }) as string[]
                    })
                    const assets = response.data;
                    const attributes = widget.widgetConfig?.attributeRefs?.map((attrRef: AttributeRef) => {
                        const assetIndex = assets.findIndex((asset) => asset.id === attrRef.id);
                        const asset = assetIndex >= 0 ? assets[assetIndex] : undefined;
                        return asset && asset.attributes ? [assetIndex!, asset.attributes[attrRef.name!]] : undefined;
                    }).filter((indexAndAttr: any) => !!indexAndAttr) as [number, Attribute<any>][];
                    _widget.gridItem.content = "<div class='gridItem'><or-chart" +
                        " assets='" + JSON.stringify(assets) +
                        "' activeAsset='" + JSON.stringify(assets[0]) +
                        "' assetAttributes='" + JSON.stringify(attributes) +
                        "' period='" + widget.widgetConfig?.period +
                        "' showLegend='" + JSON.stringify(widget.widgetConfig?.showLegend) +
                        "' realm='" + manager.displayRealm + "' showControls='false' style='height: 100%;'></or-chart></div>";
                    break;
                }

                // TODO: Should depend on custom properties set in widgetsettings.
                case DashboardWidgetType.MAP: {
                    _widget.gridItem.content = "<div class='gridItem'><or-map center='5.454250, 51.445990' zoom='5' style='height: 100%; width: 100%;'></or-map></div>";
                    break;
                }
            }
        }
        return _widget;
    }

    // Render
    protected render() {
        return html`
            <div style="display: flex; flex-direction: column; height: 100%;">
                ${this.editMode ? html`
                    <div id="view-options">
                        <or-mwc-input id="zoom-btn" type="${InputType.BUTTON}" .disabled="${this.isLoading}" outlined label="50%"></or-mwc-input>
                        <or-mwc-input id="view-preset-select" type="${InputType.SELECT}" .disabled="${this.isLoading}" outlined label="Preset size" value="Large" .options="${['Large', 'Medium', 'Small']}" style="min-width: 220px;"></or-mwc-input>
                        <or-mwc-input id="width-input" type="${InputType.NUMBER}" .disabled="${this.isLoading}" outlined label="Width" min="100" .value="${this.width}" style="width: 90px"
                                      @or-mwc-input-changed="${(event: OrInputChangedEvent) => { this.width = event.detail.value as number; }}"
                        ></or-mwc-input>
                        <or-mwc-input id="height-input" type="${InputType.NUMBER}" .disabled="${this.isLoading}" outlined label="Height" min="100" .value="${this.height}" style="width: 90px;"
                                      @or-mwc-input-changed="${(event: OrInputChangedEvent) => { this.height = event.detail.value as number; }}"
                        ></or-mwc-input>
                        <or-mwc-input id="rotate-btn" type="${InputType.BUTTON}" .disabled="${this.isLoading}" icon="screen-rotation"></or-mwc-input>
                    </div>
                    <div id="container" style="display: flex; justify-content: center; height: auto;">
                        <div class="maingrid">
                            <!-- Gridstack element on which the Grid will be rendered -->
                            <div id="gridElement" class="grid-stack grid-element"></div>
                        </div>
                    </div>
                ` : html`
                    <div id="container" style="display: flex; justify-content: center; height: auto;">
                        <div class="maingrid">
                            <!-- Gridstack element on which the Grid will be rendered -->
                            <div id="gridElement" class="grid-stack"></div>
                        </div>
                    </div>
                `}
            </div>
        `
    }
}
