import manager, { Util } from "@openremote/core";
import { Asset, Attribute, AttributeRef, DashboardWidget } from "@openremote/model";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { i18next } from "@openremote/or-translate";
import { html, LitElement, TemplateResult } from "lit";
import { customElement, property, state, query } from "lit/decorators.js";
import { OrWidgetConfig, OrWidgetEntity } from "./or-base-widget";
import { style } from "../style";
import { SettingsPanelType, widgetSettingsStyling } from "../or-dashboard-settingspanel";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { OrFileUploader } from "@openremote/or-components/or-file-uploader";

export interface ImageWidgetConfig extends OrWidgetConfig {
    displayName: string;
    attributeRefs: AttributeRef[];
    displayMode?: 'icons' | 'icons with values' | 'values';
    image?: string;
    xCoordinates: number;
    yCoordinates: number;
    deltaFormat: "absolute" | "percentage";
    showTimestampControls: boolean;
    imageUploaded: boolean;
    imagePath: string;
}

const deltaOptions = new Array<string>('absolute', 'percentage');
const displayOptions = new Array<string>('icons', 'icons with values', 'values');

export class OrImageWidget implements OrWidgetEntity {

    readonly DISPLAY_MDI_ICON: string = "file-image-marker"; // https://materialdesignicons.com;
    readonly DISPLAY_NAME: string = "Image";
    readonly MIN_COLUMN_WIDTH: number = 2;
    readonly MIN_PIXEL_HEIGHT: number = 0;
    readonly MIN_PIXEL_WIDTH: number = 0;

    getDefaultConfig(widget: DashboardWidget): OrWidgetConfig {
        return {
            displayName: widget.displayName,
            attributeRefs: [],
            displayMode: "icons",
            xCoordinates: 0,
            yCoordinates: 0,
            deltaFormat: "absolute",
            showTimestampControls: false,
            imageUploaded: false,
            imagePath: "https://home3ds.com/wp-content/uploads/2018/11/PNG.png"
        } as ImageWidgetConfig;
    }

    // Triggered every update to double check if the specification.
    // It will merge missing values, or you can add custom logic to process here.
    verifyConfigSpec(widget: DashboardWidget): ImageWidgetConfig {
        return Util.mergeObjects(this.getDefaultConfig(widget), widget.widgetConfig, false) as ImageWidgetConfig;
    }


    getSettingsHTML(widget: DashboardWidget, realm: string) {
        return html`<or-image-widgetsettings .widget="${widget}" realm="${realm}"></or-image-widgetsettings>`;
    }

    getWidgetHTML(widget: DashboardWidget, editMode: boolean, realm: string) {
        return html`<or-image-widget .widget="${widget}" .editMode="${editMode}" realm="${realm}" style="height: 100%; overflow: hidden;"></or-image-widget>`;
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
    private loadedAssets: Asset[] = [];

    @state()
    public imageUploaded?: boolean;

    @state()
    private image?: HTMLInputElement;

    @state()
    private assetAttributes: [number, Attribute<any>][] = [];

    render() {

        const css =
            `
            .img-content {
                display: flex;
                flex-direction: column;
                height: 100%;
                width: 100%;
                object-fit: contain;
                flex: 1;
            }
        `
        var imagePath = this.widget?.widgetConfig.imagePath;
        return html
            `
                <style>
                    ${css}
                </style>
            <div style="height: 100%; display: flex; justify-content: center; align-items: center; position: relative;">
            <span></span>
            <img class="img-content" src="${imagePath}" alt=""/>
            </div>
            `;
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


    private _fileElem!: HTMLInputElement;

    // Default values
    private expandedPanels: string[] = [i18next.t('attributes'), i18next.t('values'), i18next.t('image settings')];
    private loadedAssets?: Asset[];


    static get styles() {
        return [style, widgetSettingsStyling];
    }

    // UI Rendering
    render() {
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
            ${this.generateExpandableHeader(i18next.t('values'))}
        </div>
        <div>
            ${this.prepareCoordinateEntries(config, i18next.t('values'))}
        </div>
            <div>
                ${this.generateExpandableHeader(i18next.t('image settings'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('image settings')) ? html`
                    <div style="padding: 24px 24px 48px 24px;">
                        <div>
                        <input type="file" @change="${this.handleFileInputChange}" /> 
                        </div>
                    </div>
                ` : null}
            </div>
        `
        return output;
    }

    prepareCoordinateEntries(config: ImageWidgetConfig, name: string){
        if (config.attributeRefs && config.attributeRefs.length > 0) {
            return config.attributeRefs.map((name) => 
            (html`<div>
            <or-mwc-input .type="${InputType.NUMBER}" style="width: 50%; float: left;" .value="${config.xCoordinates}" label="${i18next.t('xCoordinates')}"
                @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                config.xCoordinates = event.detail.value;
                this.updateConfig(this.widget!, config);
            }}"
            ></or-mwc-input>
        </div>
        <div>
            <or-mwc-input .type="${InputType.NUMBER}" style="width: 50%; float: left;" .value="${config.yCoordinates}" label="${i18next.t('yCoordinates')}"
                @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                config.yCoordinates = event.detail.value;
                this.updateConfig(this.widget!, config);
            }}"
            ></or-mwc-input>
        </div>`))

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

    isImage(file: File): boolean {
        const allowedExtensions = /(\.jpg|\.jpeg|\.png)$/i;
        const fileName = file.name;

        if (allowedExtensions.exec(fileName) !== null) {
            return true;
        }
        return false;
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
