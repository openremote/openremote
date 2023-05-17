import manager, {DefaultColor5, DefaultColor4, DefaultColor2, DefaultColor3, Util } from "@openremote/core";
import { Asset, Attribute, AttributeRef, DashboardWidget } from "@openremote/model";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { i18next } from "@openremote/or-translate";
import { css, html, LitElement, TemplateResult, unsafeCSS } from "lit";
import { customElement, property, state, query } from "lit/decorators.js";
import { OrWidgetConfig, OrWidgetEntity } from "./or-base-widget";
import { style } from "../style";
import {debounce} from "lodash";
import { SettingsPanelType, widgetSettingsStyling } from "../or-dashboard-settingspanel";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";

export interface ImageWidgetConfig extends OrWidgetConfig {
    displayName: string;
    attributeRefs: AttributeRef[];
    attributeCoordinates: Map<string, [number, number]>;
    xCoordinates: number;
    yCoordinates: number;
    imgSize: [number, number];
    showTimestampControls: boolean;
    imageUploaded: boolean;
    //currently uses this --> needs to use the image variable on ln 17
    imagePath: string;
    assets: Asset[];
}


export class OrImageWidget implements OrWidgetEntity {

    readonly DISPLAY_MDI_ICON: string = "file-image-marker"; // https://materialdesignicons.com;
    readonly DISPLAY_NAME: string = "Image";
    readonly MIN_COLUMN_WIDTH: number = 2;
    readonly MIN_PIXEL_HEIGHT: number = 0;
    readonly MIN_PIXEL_WIDTH: number = 0;

    getDefaultConfig(widget: DashboardWidget): OrWidgetConfig {
        return {
            displayName: widget?.displayName,
            attributeRefs: [],
            xCoordinates: 0,
            yCoordinates: 0,
            // set to 100 to avoid min and max both being 0
            imgSize: [100,100],
            attributeCoordinates: new Map<string, [number, number]>(),
            showTimestampControls: false,
            imageUploaded: false,
            imagePath: "https://home3ds.com/wp-content/uploads/2018/11/PNG.png",
            assets: []
        } as ImageWidgetConfig;
    }

    // Triggered every update to double check if the specification.
    // It will merge missing values, or you can add custom logic to process here.
    verifyConfigSpec(widget: DashboardWidget): ImageWidgetConfig {
        return Util.mergeObjects(this.getDefaultConfig(widget), widget.widgetConfig, false) as ImageWidgetConfig;
    }

    getWidgetHTML(widget: DashboardWidget, editMode: boolean, realm: string) {
        return html`<or-image-widget .widget="${widget}" .editMode="${editMode}" realm="${realm}" style="height: 100%; overflow: hidden;"></or-image-widget>`;
    }

    getSettingsHTML(widget: DashboardWidget, realm: string) {
        return html`<or-image-widgetsettings .widget="${widget}" realm="${realm}"></or-image-widgetsettings>`;
    }
}

@customElement("or-image-widget")
export class OrImageWidgetContent extends LitElement {

    @property()
    public readonly widget?: DashboardWidget;

    @property()
    public editMode?: boolean;

    @property()
    public realm?: string;

    @state()
    private attributeCoordinates: Record<string, [number, number]> = {};

    @state()
    public imageUploaded?: boolean;

    @state()
    private image?: HTMLInputElement;

    @state()
    private loadedAssets: Asset[] = [];

    @state()
    private assetAttributes: [number, Attribute<any>][] = [];



    @query("#img-container")
    private _imgSize!: HTMLElement;

    @state()
    private imageSize?: { width: number, height: number }

    private resizeObserver?: ResizeObserver;

    render() {

        const css =
            `
            #img-container {
                height: 100%;
                display: flex;
                justify-content: center; 
                align-items: center;
                position: relative;
                z-index: 1;
            }

            .img-content {
                display: flex;
                flex-direction: column;
                position: absolute;    /*added to check if elements can stack*/
                height: 100%;
                width: 100%;
                object-fit: contain;
                flex: 1;
                z-index: 2;
            }

            /*overlay element doesnt have to have be span, div works too, OG try was with span and it worked*/
            #overlay {
                position: absolute;     /*prevously relative*/
                z-index: 3;

                /*additional marker styling*/
                color: var(--or-app-color2, ${unsafeCSS(DefaultColor2)});
                background-color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
                border-radius: 15px;
                padding: 3px 8px 5px 8px;
                object-fit: contain;
                text-overflow: ellipsis;
            }
        `
        var imagePath = this.widget?.widgetConfig.imagePath;
        return html
            `
                <style>
                    ${css}
                </style>
            <div id="img-container">
                <img class="img-content" src="${imagePath}" alt=""/>

                <div>
                    ${this.handleMarkerPlacement(this.widget?.widgetConfig)}
                </div>
            </div>
            `;
    }

    handleMarkerPlacement(config: ImageWidgetConfig) {
        var xCoordinate = this.widget?.widgetConfig.xCoordinates;
        var yCoordinate = this.widget?.widgetConfig.yCoordinates;

        if (this.assetAttributes && config.attributeRefs.length > 0) {

            return this.assetAttributes.map((attribute) => 
            (
                html`
                <span id="overlay" style="top: ${yCoordinate}%; left: ${xCoordinate}%;">${attribute[1].value}</span>
                ${console.log(attribute[1])}
                `
            ));
        }
    }

    // handleMarkerAttributeMapping(config: ImageWidgetConfig) {
    //     if (this.assetAttributes && config.attributeRefs.length > 0) {
    //         this.assetAttributes.map((attribute) => 
    //         (
    //             this.attributeCoordinates.attribute[1].value = {
    //                 x: 0,
    //                 y: 0
    //             }

    //         ));
    //     }
    // }

    handleContainerSizing(config: ImageWidgetConfig){
        this.updateComplete.then(() => {
            this.resizeObserver = new ResizeObserver(debounce((entries: ResizeObserverEntry[]) => {
                const size = entries[0].contentRect;
                this.imageSize = {
                    width: size.width,
                    height: size.height
                }
                this.updateComplete.then(() => {
                    //not ideal at all --> this shouldn't be in the config
                    //exponentially slows down the entire dashboard
                    // this.widget!.widgetConfig.imgSize[0] = size.width;
                    // this.widget!.widgetConfig.imgSize[0] = size.height;
                });
            }, 200))
            this.resizeObserver.observe(this._imgSize);
        })
    }

    calculateMarkerPosition(markerValue: Number, maxValue: Number) {
        var percentage: Number = 0;
        // .valueOf() is added to avoid TS2362 error
        percentage = (markerValue.valueOf() / maxValue.valueOf()) * 100;

        return percentage;
    }

    updated(changedProperties: Map<string, any>) {
        if (changedProperties.has("widget") || changedProperties.has("editMode")) {
            this.fetchAssets(this.widget?.widgetConfig).then((assets) => {
                this.loadedAssets = (assets ? assets : []);
                this.assetAttributes = this.widget?.widgetConfig.attributeRefs.map((attrRef: AttributeRef) => {
                    const assetIndex = assets!.findIndex((asset) => asset.id === attrRef.id);
                    const foundAsset = assetIndex >= 0 ? assets![assetIndex] : undefined;
                    return foundAsset && foundAsset.attributes ? [assetIndex, foundAsset.attributes[attrRef.name!]] : undefined;
                }).filter((indexAndAttr: any) => !!indexAndAttr) as [number, Attribute<any>][];
                this.handleContainerSizing(this.widget?.widgetConfig);
                this.requestUpdate();
            });
        }
    }

    // Fetching the assets according to the AttributeRef[] input in DashboardWidget if required. TODO: Simplify this to only request data needed for attribute list
    async fetchAssets(config: OrWidgetConfig | any): Promise<Asset[] | undefined> {
        if (config.attributeRefs && config.attributeRefs.length > 0) {
            let assets: Asset[] = [];
            await manager.rest.api.AssetResource.queryAssets({
                ids: config.attributeRefs?.map((x: AttributeRef) => x.id) as string[],
                realm: {
                    name: this.realm
                },
                select: {
                    attributes: config.attributeRefs?.map((x: AttributeRef) => x.name) as string[]
                }
            }).then(response => {
                assets = response.data;
            }).catch((reason) => {
                console.error(reason);
                showSnackbar(undefined, i18next.t('errorOccurred'));
            });
            return assets;
        }
    }
}

@customElement("or-image-widgetsettings")
export class OrImageWidgetSettings extends LitElement {

    @property()
    public readonly widget?: DashboardWidget;

    // same implementation as /or-components/src/or-file-uploader.ts
    // file explorer dialogue opens with supported file types
    @property({ attribute: false })
    public accept: string = "image/png,image/jpeg,image/vnd.microsoft.icon,image/svg+xml";

    private _fileElem!: HTMLInputElement;

    private expandedPanels: string[] = [i18next.t('attributes'), i18next.t('marker coordinates'), i18next.t('image settings')];
    private loadedAssets?: Asset[];


    static get styles() {
        return [style, widgetSettingsStyling];
    }

    // UI Rendering
    render() {
        
        //why do some methods use the config variable and other refernce the widget directly??????
        //what is the reason to do it one way or the other --> unclear
        const config = JSON.parse(JSON.stringify(this.widget!.widgetConfig)) as ImageWidgetConfig; // duplicate to edit, to prevent parent updates. Please trigger updateConfig()


        var output = html`
            <div>
                ${this.generateExpandableHeader(i18next.t('attributes'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('attributes')) ? html`
                    <or-dashboard-settingspanel .type="${SettingsPanelType.MULTI_ATTRIBUTE}" .widgetConfig="${this.widget!.widgetConfig}"
                    @updated="${(event: CustomEvent) => {
                        this.onAttributesUpdate(event.detail.changes);
                        this.updateConfig(this.widget!, event.detail.changes.get('config'));
                }}"
                    ></or-dashboard-settingspanel>
                ` : null}
        </div>
        <div>
            ${this.generateExpandableHeader(i18next.t('marker coordinates'))}
        </div>
        <div style="display: flex; justify-content: start; flex-direction: row; flex-wrap: wrap; align-items: flex-start; min-width: 150px;">
            ${ this.expandedPanels.includes(i18next.t('marker coordinates')) ? this.prepareCoordinateEntries(config, i18next.t('marker coordinates')): null}
        </div>
            <div>
                ${this.generateExpandableHeader(i18next.t('image settings'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('image settings')) ? html`
                    <div style="padding: 24px 24px 48px 24px;">
                        <div>
                        <input type="file" @change="${this.handleFileInputChange}" accept="${this.accept}"/> 
                        </div>
                    </div>
                ` : null}
            </div>
        `
        return output;
    }


    prepareCoordinateEntries(config: ImageWidgetConfig, name: string){
        var min = 0;
        var max = 100;
        if (config.attributeRefs && config.attributeRefs.length > 0) {
            this.updateConfig(this.widget!, config);
            return config.attributeRefs.map((attr) => 
            (html`
                    <div style="margin: 5%; font-family: inherit; width: 100%;">${attr.name}</div>
                    <or-mwc-input .type="${InputType.RANGE}" .min="${min}" .max="${max}" .value="${config.xCoordinates}"
                    @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                        console.log("Log from prepareCoordinateEntries OrInputChangedEvent");
                        console.log(config);
                        config.xCoordinates = event.detail.value;
                        this.updateConfig(this.widget!, config);
                    }}"
                    ></or-mwc-input>
                    <or-mwc-input .type="${InputType.RANGE}" .min="${min}" .max="${max}" .value="${config.yCoordinates}"
                    @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                        config.yCoordinates = event.detail.value;
                        this.updateConfig(this.widget!, config);
                    }}"
                    ></or-mwc-input>`));

        }
    }

    handleFileInputChange(event: Event) {
        const config = JSON.parse(JSON.stringify(this.widget!.widgetConfig)) as ImageWidgetConfig;
        const input = event.target as HTMLInputElement;
        if (input.files && input.files[0]) {
            
            this._fileElem = input;
            config.imagePath = URL.createObjectURL(input.files[0]);
            this.updateConfig(this.widget!, config);
            this.requestUpdate();
        }
    }

    updateConfig(widget: DashboardWidget, config: OrWidgetConfig | any, force: boolean = false) {
        const oldWidget = JSON.parse(JSON.stringify(widget)) as DashboardWidget;
        widget.widgetConfig = config;
        this.requestUpdate("widget", oldWidget);
        this.forceParentUpdate(new Map<string, any>([["widget", widget]]), force);
    }

    onAttributesUpdate(changes: Map<string, any>) {
        if (changes.has('loadedAssets')) {
            this.loadedAssets = changes.get('loadedAssets');
        }
    }

    // Method to update the Grid. For example after changing a setting.
    forceParentUpdate(changes: Map<string, any>, force: boolean = false) {
        this.requestUpdate();
        this.dispatchEvent(new CustomEvent('updated', { detail: { changes: changes, force: force } }));
    }

    generateExpandableHeader(name: string): TemplateResult {
        return html`
            <span class="expandableHeader panel-title" @click="${() => { this.expandPanel(name); }}">
                <or-icon icon="${this.expandedPanels.includes(name) ? 'chevron-down' : 'chevron-right'}"></or-icon>
                <span style="margin-left: 6px; height: 25px; line-height: 25px;">${name}</span>
            </span>
        `
    }

    expandPanel(panelName: string): void {
        if (this.expandedPanels.includes(panelName)) {
            const indexOf = this.expandedPanels.indexOf(panelName, 0);
            if (indexOf > -1) {
                this.expandedPanels.splice(indexOf, 1);
            }
        } else {
            this.expandedPanels.push(panelName);
        }
        this.requestUpdate();
    }
}

@customElement('dynamic-slider')
class DynamicSlider extends LitElement {

    @property() 
    public min?: number;

    @property() 
    public max?: number;

    @property()
    public label?: string;
    
    @property()
    public value?: number;
    
    render() {
        const css = `
        
        `;
        return html`
        <style>
            ${css}
        </style>
        <label for="dynamic-slider">${this.label ? this.label : ""}</label>
        <input type="range" id="range" name="dynamic-slider" min="${this.min ? this.min : 0}" max="${this.max ? this.max : 100}" value="${this.value ? this.value : 0}">
        `;
    }


    
}

