import {GridItemHTMLElement, GridStack, GridStackElement, GridStackNode, GridStackWidget} from "gridstack";
import {css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import {InputType} from '@openremote/or-mwc-components/or-mwc-input';
import {style} from "./style";
import manager, {DefaultColor4} from "@openremote/core";
import {Attribute, AttributeRef, DashboardGridItem,
    DashboardScalingPreset, DashboardScreenPreset, DashboardTemplate, DashboardWidget, DashboardWidgetType } from "@openremote/model";
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
        overflow-y: scroll;
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
        /*pointer-events: none;*/
        position: relative;
        z-index: 0;
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
    protected readonly editMode: boolean | undefined = true;

    @property()
    protected readonly fullscreen: boolean | undefined = false;

    @property()
    protected width: number = 960;

    @property()
    protected height: number = 540;

    @property()
    protected previewSize: "Large" | "Medium" | "Small" | "Custom Size" = "Large";

    @property()
    protected readonly isLoading: boolean | undefined;

    @property()
    protected readonly rerenderPending: boolean | undefined;

    @state()
    protected activePreset: DashboardScreenPreset;

    @state()
    protected resizeObserver: ResizeObserver | undefined;


    /* ---------------- */

    constructor() {
        super();
        this.isLoading = false;
        this.activePreset = {
            id: 'defaultPresetWithRandomId',
            displayName: 'defaultPreset',
            scalingPreset: DashboardScalingPreset.RESIZE_WIDGETS,
            breakpoint: 1920,
        } as DashboardScreenPreset


        // Tasks to execute after all rendering is done.
        this.updateComplete.then(() => {

            // If fullscreen, make sure it takes all available space
            if(this.fullscreen) {
                const element = this.shadowRoot?.firstElementChild as HTMLElement;
                this.width = element.clientWidth; // - 8; // -8px of padding
                this.height = element.clientHeight; // - 8; // -8px of padding
            }

            const maingrid = this.shadowRoot?.querySelector(".maingrid");
            if(maingrid != null) {
                this.setupResizeObserver(maingrid);
            }
        })
    }

    /* ------------------------------------- */


    // Listening to property changes (main controller method)
    updated(changedProperties: Map<string, any>) {
        console.log(changedProperties);

        // Template input changes
        if(changedProperties.has("template") || changedProperties.has("editMode")) {
            this.rerenderGrid(this.activePreset, false);
        }
        if(changedProperties.has("editMode")) {
            const maingrid = this.shadowRoot?.querySelector(".maingrid");
            if(maingrid != null) {
                this.setupResizeObserver(maingrid);
            }
        }
        if(changedProperties.has("rerenderPending")) {
            if(this.rerenderPending) {
                this.rerenderGrid(this.activePreset, true);
                this.dispatchEvent(new CustomEvent("rerender"));
            }
        }

        if(changedProperties.has("fullscreen")) {
            if((changedProperties.get("fullscreen") as boolean) && this.fullscreen == false) {
                this.width = 960; this.height = 540;
            }
        }

        // Width or height input changes
        if(changedProperties.has("width") || changedProperties.has("height")) {
            if(this.shadowRoot != null) {
                const gridHTML = this.shadowRoot.querySelector(".maingrid") as HTMLElement;
                gridHTML.style.width = (this.width + 'px');
                gridHTML.style.height = (this.fullscreen ? 'auto' : (this.height + 'px'));
                /*if(this.mainGrid != null) {
                    this.updateGridSize(true);
                }*/
            }
            if(this.width == 1920 && this.height == 1080) { this.previewSize = "Large"; }
            else if(this.width == 1280 && this.height == 720) { this.previewSize = "Medium"; }
            else if(this.width == 480 && this.height == 853) { this.previewSize = "Small"; }
            else { this.previewSize = "Custom Size"; }
        }

        if(changedProperties.has("previewSize")) {
            switch (this.previewSize) {
                case "Large": { this.width = 1920; this.height = 1080; break; }
                case "Medium": { this.width = 1280; this.height = 720; break; }
                case "Small": { this.width = 480; this.height = 853; break; }
                case "Custom Size": { break; }
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
                // Checking whether the mainGrid is not destroyed and there are Items to deselect..
                if(this.mainGrid?.el != undefined && this.mainGrid?.getGridItems() != null) {
                    this.deselectGridItems(this.mainGrid.getGridItems());
                }
                this.dispatchEvent(new CustomEvent("deselected", { detail: changedProperties.get("selected") as DashboardWidget }));
            }
        }
    }

    /* -------------------------------------------------------- */

    // Main large method for rendering the Grid
    private async renderGrid(activePreset: DashboardScreenPreset): Promise<void> {
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
            let gridElement; let grid;
            if(this.template != null && this.template.widgets != null) {
                gridElement = this.shadowRoot.getElementById("gridElement");
                const gridItems: DashboardGridItem[] = [];
                for (const widget of this.template.widgets) {
                    widget.gridItem != null ? gridItems.push((await this.loadWidget(widget)).gridItem as DashboardGridItem) : null;
                }
                grid = this.createGrid(activePreset, gridElement, gridItems);
            }

            if(grid != null) {

                this.mainGrid = grid;

                // Render a CSS border raster on the background
                if(gridElement != null) {
                    gridElement.style.backgroundSize = "" + this.mainGrid.cellWidth() + "px " + this.mainGrid.getCellHeight() + "px";
                    gridElement.style.height = "100%";
                    gridElement.style.minHeight = "100%";
                    gridElement.style.maxHeight = "100%";
                    gridElement.style.overflow = "visible";
                }

            } else {
                console.log("Could not render initial Grid! Could not find gridElement!");
            }
        }
    }

    /*getColumns(activePreset: DashboardScreenPreset, columns?: number): number {
        if(activePreset.scalingPreset == DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN) {
            return 2;
        } else {
            return (columns != undefined ? columns : 12);
        }
    }*/

    createGrid(activePreset: DashboardScreenPreset, gridElement?: HTMLElement | null, gridItems?: DashboardGridItem[]): GridStack | null {
        console.log("Creating a new Grid..");
        if(gridElement == null && this.shadowRoot != null) {
            gridElement = this.shadowRoot.getElementById("gridElement");
            if(gridElement == null) {
                const maingrid = this.shadowRoot.querySelector(".maingrid");
                if(maingrid != null) {
                    if(this.fullscreen) {
                        maingrid.innerHTML = '<div id="gridElement" class="grid-stack"></div>';
                    } else {
                        maingrid.innerHTML = '<div id="gridElement" class="grid-stack grid-element"></div>';
                    }
                    gridElement = this.shadowRoot.getElementById("gridElement");
                } else {
                    console.log("Grid could not be created, because the gridElement does not exist!");
                    return null;
                }
            }
        }
        const grid = GridStack.init({
            acceptWidgets: (this.editMode),
            animate: true,
            cellHeight: (activePreset.scalingPreset == DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN ? (this.width / 4) : 'auto'),
            cellHeightThrottle: 100,
            column: (this.template?.columns != null ? this.template.columns : 12),
            disableOneColumnMode: (activePreset.scalingPreset == DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN ? false : true),
            draggable: {
                appendTo: 'parent', // Required to work, seems to be Shadow DOM related.
                scroll: true
            },
            float: true,
            margin: 4,
            minWidth: (activePreset.breakpoint),
            resizable: {
                handles: 'all'
            },
            staticGrid: (activePreset.scalingPreset == DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN ? true : (!this.editMode)),
            styleInHead: false
            // @ts-ignore typechecking, because we can only provide an HTMLElement (which GridHTMLElement inherits)
        }, gridElement)
        // console.log(grid);

        if(gridElement != null) {
            gridElement.style.backgroundSize = "" + grid.cellWidth() + "px " + grid.getCellHeight() + "px";
            gridElement.style.height = "100%";
            gridElement.style.minHeight = "100%";
        }

        if(gridItems != null) {
            grid.load(gridItems);
            if(this.editMode && activePreset.scalingPreset != DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN) {
                grid.getGridItems().forEach((htmlElement) => {
                    const gridItem = htmlElement.gridstackNode as DashboardGridItem;
                    this.addWidgetEventListeners(gridItem, htmlElement);
                });
            }
            if(activePreset.scalingPreset == DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN) {
                grid.compact();
            }
        }

        if(this.editMode) {
            grid.on('dropped', (event: Event, previousWidget: any, newWidget: GridStackNode | undefined) => {
                if(this.mainGrid != null && newWidget != null) {
                    this.mainGrid.removeWidget((newWidget.el) as GridStackElement, true, false); // Removes dragged widget first
                    this.dispatchEvent(new CustomEvent("dropped", { detail: newWidget }));
                }
            });

            // Handling changes of items (resizing, moving around etc)
            grid.on('change', (event: Event, items: any) => {
                if(this.template != null && this.template.widgets != null) {
                    (items as GridStackNode[]).forEach(node => {
                        const widget: DashboardWidget | undefined = this.template?.widgets?.find(widget => { return widget.gridItem?.id == node.id; });
                        if(widget != null && widget.gridItem != null) {
                            // console.log("Updating properties of " + widget.displayName);
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
        }

        // Making all GridStack events dispatch on this component as well.
        grid.on("added", (event: Event, items: any) => { this.dispatchEvent(new CustomEvent("added", {detail: { event: event, items: items }})); });
        grid.on("change", (event: Event, items: any) => { this.dispatchEvent(new CustomEvent("change", { detail: { event: event, items: items }})); });
        grid.on("disable", (event: Event) => { this.dispatchEvent(new CustomEvent("disable", { detail: { event: event }})); });
        grid.on("dragstart", (event: Event, el: any) => { this.dispatchEvent(new CustomEvent("dragstart", { detail: { event: event, el: el }})); });
        grid.on("drag", (event: Event, el: any) => { this.dispatchEvent(new CustomEvent("drag", { detail: { event: event, el: el }})); });
        grid.on("dragstop", (event: Event, el: any) => { this.dispatchEvent(new CustomEvent("dragstop", { detail: { event: event, el: el }})); });
        grid.on("enable", (event: Event) => { this.dispatchEvent(new CustomEvent("enable", { detail: { event: event }})); });
        grid.on("removed", (event: Event, items: any) => { this.dispatchEvent(new CustomEvent("removed", { detail: { event: event, items: items }})); });
        grid.on("resizestart", (event: Event, el: any) => { this.dispatchEvent(new CustomEvent("resizestart", { detail: { event: event, el: el }})); });
        grid.on("resize", (event: Event, el: any) => { this.dispatchEvent(new CustomEvent("resize", { detail: { event: event, el: el }})); });
        grid.on("resizestop", (event: Event, el: any) => { this.dispatchEvent(new CustomEvent("resizestop", { detail: { event: event, el: el }})); });

        return grid;
    }

    async rerenderGrid(activePreset: DashboardScreenPreset, rerender: boolean = true, force: boolean = true) {
        console.log("Rerendering the Grid..");
        if(this.mainGrid != null && rerender) {
            this.mainGrid.destroy(force);
        }
        this.selected = undefined;

        if(this.shadowRoot != null) {
            const mainGridContainer = this.shadowRoot.querySelector(".maingrid") as HTMLElement;
            if(!this.editMode) {
                mainGridContainer.classList.add("maingrid__fullscreen");
            } else {
                if(mainGridContainer.classList.contains("maingrid__fullscreen")) {
                    mainGridContainer.classList.remove("maingrid__fullscreen");
                }
            }
        }

        if(this.template?.widgets != null && this.shadowRoot != null) {
            const gridItems: DashboardGridItem[] = [];
            for (const widget of this.template.widgets) {
                widget.gridItem != null ? gridItems.push((await this.loadWidget(widget)).gridItem as DashboardGridItem) : null;
            }
            const newGrid = this.createGrid(activePreset, undefined, gridItems);
            if(newGrid != null) { this.mainGrid = newGrid; }

            const gridElement = this.shadowRoot.getElementById("gridElement");

            // Render a CSS border raster on the background
            if(gridElement != null) {
                gridElement.style.backgroundSize = "" + this.mainGrid?.cellWidth() + "px " + this.mainGrid?.getCellHeight() + "px";
                gridElement.style.height = "100%";
                gridElement.style.minHeight = "100%";
                gridElement.style.maxHeight = "100%";
                gridElement.style.overflow = "visible";
            }

        } else {
            console.log("Grid could not be destroyed, because it does not exist!");
        }
    }

    updateGridSize(activePreset: DashboardScreenPreset, doResize: boolean, size?: number) {
        if(this.shadowRoot != null) {
            const gridElement = this.shadowRoot.querySelector("#gridElement") as HTMLElement;
            if(doResize && gridElement != null && this.mainGrid != null) {
                console.log("Updating the Grid Size...");
                if(size != undefined) {
                    gridElement.style.backgroundSize = "" + size + "px " + size + "px";
                } else {
                    this.mainGrid.cellHeight(this.mainGrid.cellWidth())
                    gridElement.style.backgroundSize = "" + this.mainGrid.cellWidth() + "px " + this.mainGrid.getCellHeight() + "px";
                }
            }
        }
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
            this.deselectGridItems(this.mainGrid.getGridItems()); // deselecting all other items
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

    deselectGridItems(gridItems: GridItemHTMLElement[]) {
        gridItems.forEach(item => {
            this.deselectGridItem(item);
        })
    }


    /* ------------------------------ */

    async loadWidget(widget: DashboardWidget): Promise<DashboardWidget> {
        const _widget = Object.assign({}, widget);
        if(_widget.gridItem != null) {
            switch(_widget.widgetType) {
                case DashboardWidgetType.CHART: {
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
            <div id="buildingArea" style="display: flex; flex-direction: column; height: 100%;" @click="${(event: PointerEvent) => { if((event.composedPath()[1] as HTMLElement).id === 'buildingArea') { this.selected = undefined; }}}">
                ${this.editMode ? html`
                    <div id="view-options">
                        <or-mwc-input id="zoom-btn" type="${InputType.BUTTON}" disabled outlined label="50%"></or-mwc-input>
                        <or-mwc-input id="view-preset-select" type="${InputType.SELECT}" .disabled="${this.isLoading}" outlined label="Preset size" .value="${this.previewSize}" .options="${['Large', 'Medium', 'Small', 'Custom Size']}" style="min-width: 220px;"
                                      @or-mwc-input-changed="${(event: OrInputChangedEvent) => { this.previewSize = event.detail.value; }}"
                        ></or-mwc-input>
                        <or-mwc-input id="width-input" type="${InputType.NUMBER}" .disabled="${this.isLoading}" outlined label="Width" min="100" .value="${this.width}" style="width: 90px"
                                      @or-mwc-input-changed="${(event: OrInputChangedEvent) => { this.width = event.detail.value as number; }}"
                        ></or-mwc-input>
                        <or-mwc-input id="height-input" type="${InputType.NUMBER}" .disabled="${this.isLoading}" outlined label="Height" min="100" .value="${this.height}" style="width: 90px;"
                                      @or-mwc-input-changed="${(event: OrInputChangedEvent) => { this.height = event.detail.value as number; }}"
                        ></or-mwc-input>
                        <or-mwc-input id="rotate-btn" type="${InputType.BUTTON}" .disabled="${this.isLoading}" icon="screen-rotation"
                                      @or-mwc-input-changed="${() => { const newWidth = this.height; const newHeight = this.width; this.width = newWidth; this.height = newHeight; }}">
                        </or-mwc-input>
                        <or-mwc-input id="test-btn" type="${InputType.BUTTON}" icon="home"
                                      @or-mwc-input-changed="${() => { this.mainGrid?.getColumn() == 1 ? this.mainGrid?.column(12) : this.mainGrid?.column(1); }}">
                        </or-mwc-input>
                    </div>
                    <div id="container" style="display: flex; justify-content: center; height: 100%;">
                        <div class="maingrid">
                            <!-- Gridstack element on which the Grid will be rendered -->
                            <div id="gridElement" class="grid-stack grid-element"></div>
                        </div>
                    </div>
                ` : html`
                    <div id="container" style="display: flex; justify-content: center; height: 100%;">
                        <div class="maingrid">
                            <!-- Gridstack element on which the Grid will be rendered -->
                            <div id="gridElement" class="grid-stack"></div>
                        </div>
                    </div>
                `}
            </div>
        `
    }

    setupResizeObserver(element: Element): ResizeObserver {
        this.resizeObserver?.disconnect();
        this.resizeObserver = new ResizeObserver((entries) => {

            console.log("Noticed a Dashboard resize! Updating the grid..");

            if(this.template?.screenPresets != null) {
                // const activePresets = this.template.screenPresets.filter((preset) => { return (preset.breakpoint != null && this.width < preset.breakpoint); });
                let presets: DashboardScreenPreset[] = this.template.screenPresets;
                presets.sort((a, b) => {
                    if(a.breakpoint != null && b.breakpoint != null) {
                        if(a.breakpoint > b.breakpoint) {
                            return 1;
                        }
                        if(a.breakpoint < b.breakpoint) {
                            return -1;
                        }
                    }
                    return 0;
                });

                // console.log(presets);

                const width = (this.fullscreen ? element.clientWidth : this.width);
                const height = (this.fullscreen ? element.clientHeight : this.height);
                // console.log("New grid size is: [" + width + "x" + height + "]")

                // Getting the active Preset based on breakpoint
                let activePreset: DashboardScreenPreset | undefined;
                presets.forEach((preset) => {
                    if(activePreset == undefined && preset.breakpoint != null && width <= preset.breakpoint) {
                        activePreset = preset;
                    }
                });

                // console.log("Active preset is the following:");
                // console.log(activePreset);

                if(activePreset != undefined) {
                    this.activePreset = activePreset;
                    this.rerenderGrid(activePreset).then(() => {
                        if(activePreset != null) {
                            this.updateGridSize(activePreset, true);
                        }
                    });
                }
            }
        });
        this.resizeObserver.observe(element);
        return this.resizeObserver;
    }
}
