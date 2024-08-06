var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import manager, { DefaultColor4, DefaultColor5 } from "@openremote/core";
import { css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { style } from "./style";
import "./or-dashboard-widgetcontainer";
import { debounce } from "lodash";
import { getActivePreset } from "./index";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-components/or-loading-indicator";
import { repeat } from 'lit/directives/repeat.js';
import { GridStack } from "gridstack";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { i18next } from "@openremote/or-translate";
import { when } from "lit/directives/when.js";
import { cache } from "lit/directives/cache.js";
import { guard } from "lit/directives/guard.js";
import { OrDashboardEngine } from "./or-dashboard-engine";
import { WidgetService } from "./service/widget-service";
import { OrDashboardWidgetContainer } from "./or-dashboard-widgetcontainer";
// TODO: Add webpack/rollup to build so consumers aren't forced to use the same tooling
const gridcss = require('gridstack/dist/gridstack.min.css');
const extracss = require('gridstack/dist/gridstack-extra.css');
//language=css
const editorStyling = css `
    
    #loadingContainer {
        position: absolute;
        width: 100%;
        height: 100%;
        display: flex;
        justify-content: center;
        align-items: center;
    }
    
    #view-options {
        padding: 24px;
        display: flex;
        justify-content: center;
        align-items: center;
    }
    /* Margins on view options */
    #fit-btn { margin-right: 10px; }
    #view-preset-select { margin-left: 20px; }
    #width-input { margin-left: 20px; }
    #height-input { margin-left: 10px; }
    #rotate-btn { margin-left: 10px; }
    
    .maingridContainer {
        position: absolute;
        padding-bottom: 32px;
    }
    .maingridContainer__fullscreen {
        width: 100%;
    }
    
    .maingrid {
        border: 3px solid #909090;
        background: #FFFFFF;
        border-radius: 8px;
        overflow-x: hidden;
        overflow-y: scroll;
        padding: 4px;
        z-index: 0;
    }
    .maingrid__fullscreen {
        border: none;
        background: transparent;
        border-radius: 0;
        overflow-x: hidden;
        overflow-y: scroll;
        height: 100% !important; /* To override .maingrid */
        width: 100% !important; /* To override .maingrid */
        padding: 0;
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
        border: 1px solid var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
        border-radius: 4px;
    }
    .grid-stack > .grid-stack-item > .grid-stack-item-content {
        overflow: visible;
    }
    .grid-stack-item-content__active {
        border: 2px solid var(--or-app-color4, ${unsafeCSS(DefaultColor4)});
        margin: -1px !important; /* to compromise with the extra pixel of border. */
    }
    
    /* Grid lines on the background of the grid */
    .grid-element {
        background-image:
                linear-gradient(90deg, #E0E0E0, transparent 1px),
                linear-gradient(90deg, transparent calc(100% - 1px), #E0E0E0),
                linear-gradient(#E0E0E0, transparent 1px),
                linear-gradient(transparent calc(100% - 1px), #E0E0E0 100%);
    }
`;
/* ------------------------------------------------------------ */
let OrDashboardPreview = class OrDashboardPreview extends LitElement {
    // Monitoring the changes in the template, save the changes to this.latestChanges,
    // so we can check afterwards which changes are made. Used for
    set template(newValue) {
        const currentValue = this._template;
        if (currentValue != undefined) {
            const changes = {
                changedKeys: Object.keys(newValue).filter(key => (JSON.stringify(newValue[key]) !== JSON.stringify(currentValue[key]))),
                oldValue: currentValue,
                newValue: newValue
            };
            this._template = JSON.parse(JSON.stringify(newValue));
            this.latestChanges = changes;
            this.requestUpdate("template", currentValue);
            // If there is no value yet, do initial setup:
        }
        else if (newValue != undefined) {
            this._template = newValue;
            this.setupGrid(false, false);
        }
    }
    get template() {
        return this._template;
    }
    /* ------------------------------------------- */
    // Using constructor to set initial values
    constructor() {
        super();
        this.editMode = false;
        this.readonly = true;
        this.previewZoom = 1;
        this.fullscreen = true;
        this.rerenderActive = false;
        this.isLoading = false;
        this.cachedGridstackCSS = new Map();
        this.resizeObserverCallback = (entries) => {
            var _a;
            if ((((_a = this.previousObserverEntry) === null || _a === void 0 ? void 0 : _a.contentRect.width) + "px") !== (entries[0].contentRect.width + "px")) {
                this._onGridResize();
            }
            this.previousObserverEntry = entries[0];
        };
        if (!this.realm) {
            this.realm = manager.displayRealm;
        }
        if (!this.availablePreviewSizes) {
            this.availablePreviewSizes = [
                { displayName: "4k Television", width: 3840, height: 2160 },
                { displayName: "Desktop", width: 1920, height: 1080 },
                { displayName: "Small desktop", width: 1280, height: 720 },
                { displayName: "Phone", width: 360, height: 800 },
                { displayName: "Custom" }
            ];
        }
        // Defaulting to a Phone view
        if (!this.previewSize) {
            this.previewSize = this.availablePreviewSizes[3];
        }
        // Register custom override functions for GridStack
        GridStack.registerEngine(OrDashboardEngine);
    }
    static get styles() {
        return [unsafeCSS(gridcss), unsafeCSS(extracss), editorStyling, style];
    }
    /* ------------------------------ */
    // Checking whether actual changes have been made; if not, prevent updating.
    shouldUpdate(changedProperties) {
        var _a, _b, _c;
        const changed = changedProperties;
        if (changedProperties.has('latestChanges')
            && ((_a = this.latestChanges) === null || _a === void 0 ? void 0 : _a.changedKeys.length) == 0
            && (JSON.stringify((_b = changedProperties.get('latestChanges')) === null || _b === void 0 ? void 0 : _b.oldValue)) == (JSON.stringify((_c = changedProperties.get('latestChanges')) === null || _c === void 0 ? void 0 : _c.newValue))) {
            changed.delete('latestChanges');
        }
        // Do not update UI if the preview size has changed while being fullscreen,
        // since it is only used when in "responsive mode".
        if (this.fullscreen && changedProperties.has('previewWidth')) {
            changed.delete('previewWidth');
        }
        if (this.fullscreen && changedProperties.has('previewHeight')) {
            changed.delete('previewHeight');
        }
        return (changed.size === 0 ? false : super.shouldUpdate(changedProperties));
    }
    // Main method for executing actions after property changes
    updated(changedProperties) {
        var _a, _b, _c, _d, _e, _f;
        super.updated(changedProperties);
        if (this.realm == undefined) {
            this.realm = manager.displayRealm;
        }
        // Setup template (list of widgets and properties)
        if (!this.template && this.dashboardId) {
            manager.rest.api.DashboardResource.get(this.realm, this.dashboardId)
                .then((response) => { this.template = response.data.template; })
                .catch((reason) => { console.error(reason); showSnackbar(undefined, "errorOccurred"); });
        }
        else if (this.template == null && this.dashboardId == null) {
            console.warn("Neither the template nor dashboardId attributes have been specified!");
        }
        // If changes to the template have been made
        if (changedProperties.has("latestChanges")) {
            if (this.latestChanges) {
                this.processTemplateChanges(this.latestChanges);
                this.latestChanges = undefined;
            }
        }
        if (changedProperties.has("selectedWidget")) {
            if (this.selectedWidget) {
                if (changedProperties.get("selectedWidget") != undefined) { // if previous selected state was a different widget, dispatch event as well
                    this.dispatchEvent(new CustomEvent("deselected", { detail: changedProperties.get("selectedWidget") }));
                }
                if (((_a = this.grid) === null || _a === void 0 ? void 0 : _a.el) != null) {
                    const foundItem = (_b = this.grid) === null || _b === void 0 ? void 0 : _b.getGridItems().find((item) => { var _a, _b, _c; return ((_a = item.gridstackNode) === null || _a === void 0 ? void 0 : _a.id) == ((_c = (_b = this.selectedWidget) === null || _b === void 0 ? void 0 : _b.gridItem) === null || _c === void 0 ? void 0 : _c.id); });
                    if (foundItem != null) {
                        this.selectGridItem(foundItem);
                    }
                    this.dispatchEvent(new CustomEvent("selected", { detail: this.selectedWidget }));
                }
            }
            else {
                // Checking whether the mainGrid is not destroyed and there are Items to deselect...
                if (((_c = this.grid) === null || _c === void 0 ? void 0 : _c.el) != undefined && ((_d = this.grid) === null || _d === void 0 ? void 0 : _d.getGridItems()) != null) {
                    this.deselectGridItems(this.grid.getGridItems());
                }
                this.dispatchEvent(new CustomEvent("deselected", { detail: changedProperties.get("selectedWidget") }));
            }
        }
        // Switching edit/view mode needs recreation of Grid
        if (changedProperties.has("editMode")) {
            if (changedProperties.get('editMode') != undefined) {
                this.setupGrid(true, true);
            }
        }
        // Adjusting previewSize when manual pixels control changes
        if (changedProperties.has("previewWidth") || changedProperties.has("previewHeight")) {
            if ((_e = this.template) === null || _e === void 0 ? void 0 : _e.screenPresets) {
                this.previewSize = (_f = this.availablePreviewSizes) === null || _f === void 0 ? void 0 : _f.find(s => ((s.width + "px" == this.previewWidth) && (s.height + "px" == this.previewHeight)));
            }
        }
        // Adjusting pixels control when previewSize changes.
        if (changedProperties.has('previewSize')) {
            if (this.previewSize) {
                this.previewWidth = this.previewSize.width + "px";
                this.previewHeight = this.previewSize.height + "px";
            }
        }
        // When parent component requests a forced rerender
        if (changedProperties.has("rerenderActive")) {
            if (this.rerenderActive) {
                this.rerenderActive = false;
            }
        }
    }
    /* ---------------------------------------- */
    // Main setup Grid method (often used)
    setupGrid(recreate, force = false) {
        var _a, _b, _c, _d, _e, _f, _g, _h, _j, _k, _l, _m;
        return __awaiter(this, void 0, void 0, function* () {
            this.isLoading = true;
            yield this.updateComplete;
            let gridElement = (_a = this.shadowRoot) === null || _a === void 0 ? void 0 : _a.getElementById("gridElement");
            if (gridElement != null) {
                if (recreate && this.grid != null) {
                    this.grid.destroy(false);
                    if (force) { // Fully rerender the grid by switching rerenderActive on and off, and continue after that.
                        this.rerenderActive = true;
                        yield this.updateComplete;
                        yield this.waitUntil((_) => !this.rerenderActive);
                        gridElement = (_b = this.shadowRoot) === null || _b === void 0 ? void 0 : _b.getElementById("gridElement");
                        this.grid = undefined;
                    }
                }
                const width = (this.fullscreen ? this.clientWidth : (+((_c = this.previewWidth) === null || _c === void 0 ? void 0 : _c.replace(/\D/g, ""))));
                const newPreset = getActivePreset(width, this.template.screenPresets);
                if ((newPreset === null || newPreset === void 0 ? void 0 : newPreset.scalingPreset) != ((_d = this.activePreset) === null || _d === void 0 ? void 0 : _d.scalingPreset)) {
                    if (!(recreate && force)) { // Fully rerender the grid by switching rerenderActive on and off, and continue after that.
                        if (!recreate) { // If not destroyed yet, destroy first.
                            (_e = this.grid) === null || _e === void 0 ? void 0 : _e.destroy(false);
                        }
                        this.rerenderActive = true;
                        yield this.updateComplete;
                        yield this.waitUntil((_) => !this.rerenderActive);
                        gridElement = (_f = this.shadowRoot) === null || _f === void 0 ? void 0 : _f.getElementById("gridElement");
                        this.grid = undefined;
                    }
                }
                this.activePreset = newPreset;
                // If grid got reset, setup the ResizeObserver again.
                if (this.grid == null) {
                    const gridHTML = (_g = this.shadowRoot) === null || _g === void 0 ? void 0 : _g.querySelector(".maingrid");
                    this.setupResizeObserver(gridHTML);
                }
                gridElement.style.maxWidth = this.template.maxScreenWidth + "px";
                this.grid = GridStack.init({
                    acceptWidgets: (this.editMode),
                    animate: true,
                    cellHeight: (((_h = this.activePreset) === null || _h === void 0 ? void 0 : _h.scalingPreset) === "WRAP_TO_SINGLE_COLUMN" /* DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN */ ? (width / (((_j = this.template) === null || _j === void 0 ? void 0 : _j.columns) ? (this.template.columns / 4) : 2)) : 'initial'),
                    column: (_k = this.template) === null || _k === void 0 ? void 0 : _k.columns,
                    disableOneColumnMode: (((_l = this.activePreset) === null || _l === void 0 ? void 0 : _l.scalingPreset) !== "WRAP_TO_SINGLE_COLUMN" /* DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN */),
                    oneColumnModeDomSort: true,
                    draggable: {
                        appendTo: 'parent', // Required to work, seems to be Shadow DOM related.
                    },
                    float: true,
                    margin: 5,
                    resizable: {
                        handles: 'all'
                    },
                    staticGrid: (((_m = this.activePreset) === null || _m === void 0 ? void 0 : _m.scalingPreset) === "WRAP_TO_SINGLE_COLUMN" /* DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN */ ? true : (!this.editMode)),
                    styleInHead: false
                }, gridElement);
                gridElement.style.backgroundSize = "" + this.grid.cellWidth() + "px " + this.grid.getCellHeight() + "px";
                gridElement.style.height = "100%";
                gridElement.style.minHeight = "100%";
                // When an item gets dropped ontop of the grid. GridStack docs say:
                // "called when an item has been dropped and accepted over a grid. If the item came from another grid, the previous widget node info will also be sent (but dom item long gone)."
                this.grid.on('dropped', (ev, prevWidget, newWidget) => this.onWidgetDrop(ev, prevWidget, newWidget));
                this.grid.on('change', (_event, items) => {
                    if (this.template != null && this.template.widgets != null) {
                        items.forEach(node => {
                            var _a, _b;
                            const foundWidget = (_b = (_a = this.template) === null || _a === void 0 ? void 0 : _a.widgets) === null || _b === void 0 ? void 0 : _b.find(widget => { var _a; return ((_a = widget.gridItem) === null || _a === void 0 ? void 0 : _a.id) == node.id; });
                            foundWidget.gridItem.x = node.x;
                            foundWidget.gridItem.y = node.y;
                            foundWidget.gridItem.w = node.w;
                            foundWidget.gridItem.h = node.h;
                        });
                        this.dispatchEvent(new CustomEvent("changed", { detail: { template: this.template } }));
                    }
                });
                this.grid.on('resizestart', (_event) => {
                    this.latestDragWidgetStart = new Date();
                });
                this.grid.on('resizestop', (_event) => {
                    setTimeout(() => { this.latestDragWidgetStart = undefined; }, 200);
                });
            }
            this.isLoading = false;
        });
    }
    /* ------------------------------- */
    refreshPreview() {
        this.setupGrid(true, true);
    }
    refreshWidgets() {
        var _a;
        (_a = this.grid) === null || _a === void 0 ? void 0 : _a.getGridItems().forEach(gridItem => {
            const widgetContainer = gridItem.querySelector(OrDashboardWidgetContainer.tagName);
            if (widgetContainer) {
                widgetContainer.refreshContent(false);
            }
        });
    }
    selectGridItem(gridItem) {
        if (this.grid != null) {
            this.deselectGridItems(this.grid.getGridItems()); // deselecting all other items
            gridItem.querySelectorAll(".grid-stack-item-content").forEach((item) => {
                item.classList.add('grid-stack-item-content__active'); // Apply active CSS class
            });
        }
    }
    deselectGridItem(gridItem) {
        gridItem.querySelectorAll(".grid-stack-item-content").forEach((item) => {
            item.classList.remove('grid-stack-item-content__active'); // Remove active CSS class
        });
    }
    deselectGridItems(gridItems) {
        gridItems.forEach(item => {
            this.deselectGridItem(item);
        });
    }
    onGridItemClick(gridItem) {
        var _a, _b, _c, _d, _e;
        if (!this.latestDragWidgetStart && !((_a = this.grid) === null || _a === void 0 ? void 0 : _a.opts.staticGrid)) {
            if (!gridItem) {
                this.selectedWidget = undefined;
            }
            else if (((_c = (_b = this.selectedWidget) === null || _b === void 0 ? void 0 : _b.gridItem) === null || _c === void 0 ? void 0 : _c.id) != gridItem.id) {
                this.selectedWidget = (_e = (_d = this.template) === null || _d === void 0 ? void 0 : _d.widgets) === null || _e === void 0 ? void 0 : _e.find(widget => { var _a; return ((_a = widget.gridItem) === null || _a === void 0 ? void 0 : _a.id) == gridItem.id; });
            }
        }
    }
    onFitToScreenClick() {
        var _a;
        const container = (_a = this.shadowRoot) === null || _a === void 0 ? void 0 : _a.querySelector('#container');
        if (container) {
            const zoomWidth = +((0.95 * container.clientWidth) / +this.previewWidth.replace('px', '')).toFixed(2);
            this.previewZoom = (zoomWidth > 1 ? 1 : zoomWidth);
        }
    }
    isPreviewVisible() {
        var _a;
        return !this.isLoading && ((_a = this.activePreset) === null || _a === void 0 ? void 0 : _a.scalingPreset) != "BLOCK_DEVICE" /* DashboardScalingPreset.BLOCK_DEVICE */;
    }
    // Render
    render() {
        var _a, _b, _c, _d, _e, _f, _g, _h;
        try { // to correct the list of gridItems each render (Hopefully temporarily since it's quite compute heavy)
            if (((_a = this.grid) === null || _a === void 0 ? void 0 : _a.el) && ((_b = this.grid) === null || _b === void 0 ? void 0 : _b.getGridItems())) {
                (_c = this.grid) === null || _c === void 0 ? void 0 : _c.getGridItems().forEach((gridItem) => {
                    var _a, _b, _c;
                    if (((_b = (_a = this.template) === null || _a === void 0 ? void 0 : _a.widgets) === null || _b === void 0 ? void 0 : _b.find((widget) => widget.id == gridItem.id)) == undefined) {
                        (_c = this.grid) === null || _c === void 0 ? void 0 : _c.removeWidget(gridItem);
                    }
                });
            }
        }
        catch (e) {
            console.error(e);
        }
        const customPreset = "Custom";
        let screenPresets = (_e = (_d = this.template) === null || _d === void 0 ? void 0 : _d.screenPresets) === null || _e === void 0 ? void 0 : _e.map(s => s.displayName);
        screenPresets === null || screenPresets === void 0 ? void 0 : screenPresets.push(customPreset);
        return html `
            <div id="buildingArea" style="display: flex; flex-direction: column; height: 100%; position: relative;" @click="${(event) => { if (event.composedPath()[1].id === 'buildingArea') {
            this.onGridItemClick(undefined);
        } }}">
                ${this.editMode && !this.fullscreen ? html `
                    <div id="view-options">
                        <or-mwc-input id="fit-btn" type="${InputType.BUTTON}" icon="fit-to-screen"
                                      @or-mwc-input-changed="${() => this.onFitToScreenClick()}">
                        </or-mwc-input>
                        <or-mwc-input id="zoom-input" type="${InputType.NUMBER}" outlined label="${i18next.t('dashboard.zoomPercent')}" min="25" .value="${(this.previewZoom * 100)}" style="width: 90px"
                                      @or-mwc-input-changed="${debounce((event) => { this.previewZoom = event.detail.value / 100; }, 50)}"
                        ></or-mwc-input>
                        <or-mwc-input id="view-preset-select" type="${InputType.SELECT}" outlined label="${i18next.t('dashboard.presetSize')}" style="min-width: 220px;"
                                      .value="${this.previewSize == undefined ? customPreset : this.previewSize.displayName}" .options="${(_f = this.availablePreviewSizes) === null || _f === void 0 ? void 0 : _f.map((x) => x.displayName)}"
                                      @or-mwc-input-changed="${(event) => { var _a; this.previewSize = (_a = this.availablePreviewSizes) === null || _a === void 0 ? void 0 : _a.find(s => s.displayName == event.detail.value); }}"
                        ></or-mwc-input>
                        <or-mwc-input id="width-input" type="${InputType.NUMBER}" outlined label="${i18next.t('width')}" min="100" .value="${(_g = this.previewWidth) === null || _g === void 0 ? void 0 : _g.replace('px', '')}" style="width: 90px"
                                      @or-mwc-input-changed="${debounce((event) => { this.previewWidth = event.detail.value + 'px'; }, 550)}"
                        ></or-mwc-input>
                        <or-mwc-input id="height-input" type="${InputType.NUMBER}" outlined label="${i18next.t('height')}" min="100" .value="${(_h = this.previewHeight) === null || _h === void 0 ? void 0 : _h.replace('px', '')}" style="width: 90px;"
                                      @or-mwc-input-changed="${(event) => { this.previewHeight = event.detail.value + 'px'; }}"
                        ></or-mwc-input>
                        <or-mwc-input id="rotate-btn" type="${InputType.BUTTON}" icon="screen-rotation"
                                      @or-mwc-input-changed="${() => { const newWidth = this.previewHeight; const newHeight = this.previewWidth; this.previewWidth = newWidth; this.previewHeight = newHeight; }}">
                        </or-mwc-input>
                    </div>
                ` : undefined}
                ${this.rerenderActive ? html `
                    <div id="container" style="justify-content: center; align-items: center;">
                        <span><or-translate value="dashboard.renderingGrid"></or-translate></span>
                    </div>
                ` : html `
                    <div id="container" style="justify-content: center; position: relative;">
                        ${when(this.isLoading, () => html `
                            <div style="position: absolute; z-index: 3; height: 100%; display: flex; align-items: center;">
                                <or-loading-indicator></or-loading-indicator>
                            </div>
                        `, () => {
            var _a;
            return html `
                            ${((_a = this.activePreset) === null || _a === void 0 ? void 0 : _a.scalingPreset) == "BLOCK_DEVICE" /* DashboardScalingPreset.BLOCK_DEVICE */ ? html `
                                <div style="position: absolute; z-index: 3; height: 100%; display: flex; align-items: center;">
                                    <span><or-translate value="dashboard.deviceNotSupported"></or-translate></span>
                                </div>
                            ` : undefined}
                        `;
        })}
                        <!-- The grid itself. Will also show during isLoading, but will be invisible through CSS -->
                        <div class="${this.fullscreen ? 'maingridContainer__fullscreen' : 'maingridContainer'}" style="${this.isLoading ? 'visibility: hidden;' : ''}">
                            <div class="maingrid ${this.fullscreen ? 'maingrid__fullscreen' : undefined}"
                                 style="width: ${this.previewWidth}; height: ${this.previewHeight}; visibility: ${this.isPreviewVisible() ? 'visible' : 'hidden'}; zoom: ${this.editMode && !this.fullscreen ? this.previewZoom : 'normal'}; ${this.editMode && !this.fullscreen ? ('-moz-transform: scale(' + this.previewZoom + ')') : undefined}; transform-origin: top;"
                                 @click="${(ev) => {
            if (ev.composedPath()[0].id == 'gridElement') {
                this.onGridItemClick(undefined);
            }
        }}">
                                ${guard([this.editMode, this.template], () => {
            var _a;
            return html `
                                    <!-- Gridstack element on which the Grid will be rendered -->
                                    <div id="gridElement" class="grid-stack ${this.editMode ? 'grid-element' : undefined}" style="margin: auto;">
                                        ${((_a = this.template) === null || _a === void 0 ? void 0 : _a.widgets) ? repeat(this.template.widgets, (item) => item.id, (widget) => {
                var _a, _b, _c, _d, _e, _f, _g;
                return html `
                                                    <div class="grid-stack-item" id="${widget.id}" gs-id="${(_a = widget.gridItem) === null || _a === void 0 ? void 0 : _a.id}" gs-x="${(_b = widget.gridItem) === null || _b === void 0 ? void 0 : _b.x}" gs-y="${(_c = widget.gridItem) === null || _c === void 0 ? void 0 : _c.y}"
                                                         gs-w="${(_d = widget.gridItem) === null || _d === void 0 ? void 0 : _d.w}" gs-h="${(_e = widget.gridItem) === null || _e === void 0 ? void 0 : _e.h}" gs-min-w="${(_f = widget.gridItem) === null || _f === void 0 ? void 0 : _f.minW}" gs-min-h="${(_g = widget.gridItem) === null || _g === void 0 ? void 0 : _g.minH}"
                                                         @click="${() => {
                    this.onGridItemClick(widget.gridItem);
                }}">
                                                        <div class="grid-stack-item-content" style="display: flex;">
                                                            <or-dashboard-widget-container .widget="${widget}" .editMode="${this.editMode}" style="width: 100%; height: auto; border-radius: 4px;"></or-dashboard-widget-container>
                                                        </div>
                                                    </div>
                                                `;
            }) : undefined}
                                    </div>
                                `;
        })}
                            </div>
                        </div>
                    </div>
                `}
            </div>
            <style>
                ${cache(when(this.isExtraLargeGrid(), () => this.applyCustomGridstackGridCSS(this.getGridstackColumns(this.grid) ? this.getGridstackColumns(this.grid) : this.template.columns)))}
            </style>
        `;
    }
    getGridstackColumns(grid) {
        try {
            return grid === null || grid === void 0 ? void 0 : grid.getColumn();
        }
        catch (e) {
            return undefined;
        }
    }
    isExtraLargeGrid() {
        var _a;
        return !!this.grid && ((this.getGridstackColumns(this.grid) && this.getGridstackColumns(this.grid) > 12)
            || !!(((_a = this.template) === null || _a === void 0 ? void 0 : _a.columns) && this.template.columns > 12));
    }
    // Provides support for > 12 columns in GridStack (which requires manual css edits)
    //language=html
    applyCustomGridstackGridCSS(columns) {
        if (this.cachedGridstackCSS.has(columns)) {
            return html `${this.cachedGridstackCSS.get(columns).map((x) => x)}`;
        }
        else {
            const htmls = [];
            for (let i = 0; i < (columns + 1); i++) {
                htmls.push(html `
                    <style>
                        .grid-stack > .grid-stack-item[gs-w="${i}"]:not(.ui-draggable-dragging):not(.ui-resizable-resizing) { width: ${100 - (columns - i) * (100 / columns)}% !important; }
                        .grid-stack > .grid-stack-item[gs-x="${i}"]:not(.ui-draggable-dragging):not(.ui-resizable-resizing) { left: ${100 - (columns - i) * (100 / columns)}% !important; }                    
                    </style>
                `);
            }
            this.cachedGridstackCSS.set(columns, htmls);
            return html `${htmls.map((x) => x)}`;
        }
    }
    disconnectedCallback() {
        var _a;
        super.disconnectedCallback();
        (_a = this.resizeObserver) === null || _a === void 0 ? void 0 : _a.disconnect();
    }
    // Triggering a Grid rerender on every time the element resizes.
    // In fullscreen, debounce (only trigger after 550ms of no changes) to limit amount of rerenders.
    setupResizeObserver(element) {
        var _a;
        (_a = this.resizeObserver) === null || _a === void 0 ? void 0 : _a.disconnect();
        if (this.fullscreen) {
            this.resizeObserver = new ResizeObserver(debounce(this.resizeObserverCallback, 200));
        }
        else {
            this.resizeObserver = new ResizeObserver(this.resizeObserverCallback);
        }
        this.resizeObserver.observe(element);
        return this.resizeObserver;
    }
    _onGridResize() {
        this.setupGrid(true, false);
    }
    /* --------------------------------------- */
    processTemplateChanges(changes) {
        var _a, _b, _c;
        // If only columns property changed, change columns through the framework and then recreate grid.
        if (changes.changedKeys.length == 1 && changes.changedKeys.includes('columns') && this.grid) {
            this.grid.column(changes.newValue.columns);
            let maingrid = (_a = this.shadowRoot) === null || _a === void 0 ? void 0 : _a.querySelector(".maingrid");
            let gridElement = (_b = this.shadowRoot) === null || _b === void 0 ? void 0 : _b.getElementById("gridElement");
            gridElement.style.backgroundSize = "" + this.grid.cellWidth() + "px " + this.grid.getCellHeight() + "px";
            gridElement.style.height = maingrid.scrollHeight + 'px';
            this.setupGrid(true, false);
        }
        // If multiple properties changed, just force rerender all of it.
        else if (changes.changedKeys.length > 1) {
            this.setupGrid(true, true);
        }
        // On widgets change, check whether they are programmatically added to GridStack. If not, adding them.
        else if (changes.changedKeys.includes('widgets')) {
            if (((_c = this.grid) === null || _c === void 0 ? void 0 : _c.el) != null) {
                this.grid.getGridItems().forEach((gridElement) => {
                    var _a;
                    if (!gridElement.classList.contains('ui-draggable')) {
                        (_a = this.grid) === null || _a === void 0 ? void 0 : _a.makeWidget(gridElement);
                    }
                });
            }
        }
        else if (changes.changedKeys.includes('screenPresets')) {
            this.setupGrid(true, true);
        }
    }
    // Wait until function that waits until a boolean returns differently
    // TODO: Remove this, and replace 'waiting' functionality with observer pattern principles.
    waitUntil(conditionFunction) {
        const poll = (resolve) => {
            if (conditionFunction())
                resolve();
            else
                setTimeout(_ => poll(resolve), 400);
        };
        return new Promise(poll);
    }
    // Callback method for GridStack Grid 'dropped' event. GridStack docs say:
    // called when an item has been dropped and accepted over a grid. If the item came from another grid, the previous widget node info will also be sent (but dom item long gone).
    onWidgetDrop(_ev, _prevWidget, newWidget) {
        // When a "Widget Card" gets dropped onto the grid, we create a new widget on those coordinates.
        if (this.grid && newWidget) {
            this.grid.removeWidget((newWidget.el), true, false); // Removes dragged widget first
            WidgetService.placeNew(newWidget.widgetTypeId, newWidget.x, newWidget.y).then((widget) => {
                this.dispatchEvent(new CustomEvent("created", { detail: widget }));
            });
        }
    }
};
__decorate([
    property({ hasChanged(oldValue, newValue) { return JSON.stringify(oldValue) != JSON.stringify(newValue); } })
], OrDashboardPreview.prototype, "template", null);
__decorate([
    property() // Optional alternative for template
], OrDashboardPreview.prototype, "dashboardId", void 0);
__decorate([
    property() // Normally manager.displayRealm
], OrDashboardPreview.prototype, "realm", void 0);
__decorate([
    property({ type: Object })
], OrDashboardPreview.prototype, "selectedWidget", void 0);
__decorate([
    property()
], OrDashboardPreview.prototype, "editMode", void 0);
__decorate([
    property() // For example when no permission
], OrDashboardPreview.prototype, "readonly", void 0);
__decorate([
    property()
], OrDashboardPreview.prototype, "previewWidth", void 0);
__decorate([
    property()
], OrDashboardPreview.prototype, "previewHeight", void 0);
__decorate([
    property()
], OrDashboardPreview.prototype, "previewZoom", void 0);
__decorate([
    property()
], OrDashboardPreview.prototype, "previewSize", void 0);
__decorate([
    property()
], OrDashboardPreview.prototype, "availablePreviewSizes", void 0);
__decorate([
    property()
], OrDashboardPreview.prototype, "fullscreen", void 0);
__decorate([
    state() // State where the changes of the template are saved temporarily (for comparison with incoming data)
], OrDashboardPreview.prototype, "latestChanges", void 0);
__decorate([
    state()
], OrDashboardPreview.prototype, "activePreset", void 0);
__decorate([
    state()
], OrDashboardPreview.prototype, "rerenderActive", void 0);
__decorate([
    state()
], OrDashboardPreview.prototype, "isLoading", void 0);
OrDashboardPreview = __decorate([
    customElement("or-dashboard-preview")
], OrDashboardPreview);
export { OrDashboardPreview };
//# sourceMappingURL=or-dashboard-preview.js.map