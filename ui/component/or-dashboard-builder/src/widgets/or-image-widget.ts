import manager, { Util } from "@openremote/core";
import {Asset, Attribute, AttributeRef, DashboardWidget } from "@openremote/model";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import { i18next } from "@openremote/or-translate";
import { html, LitElement, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import {OrWidgetConfig, OrWidgetEntity} from "./or-base-widget";
import {style} from "../style";
import {SettingsPanelType, widgetSettingsStyling} from "../or-dashboard-settingspanel";
import {InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {OrFileUploader} from "@openremote/or-components/or-file-uploader";
// import "@openremote/or-image-map";

export interface ImageWidgetConfig extends OrWidgetConfig {
    displayName: string;
    attributeRefs: AttributeRef[];
    displayMode?: 'icons' | 'icons with values' | 'values';
    image?: string;
    decimals: number;
    deltaFormat: "absolute" | "percentage";
    showTimestampControls: boolean;
    imageUploaded: boolean;
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
            decimals: 0,
            deltaFormat: "absolute",
            showTimestampControls: false,
            imageUploaded: false,
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
    private image?: string;

    @state()
    private assetAttributes: [number, Attribute<any>][] = [];

    

    // render() {
    //     return html`
    //         <or-attribute-card .assets="${this.loadedAssets}" .assetAttributes="${this.assetAttributes}" .displayMode="${this.widget?.widgetConfig.displayMode}"
    //                            .deltaFormat="${this.widget?.widgetConfig.deltaFormat}" .mainValueDecimals="${this.widget?.widgetConfig.decimals}" .imageUploaded="${this.widget?.widgetConfig.imageUploaded}" 
    //                            ."image" = "${this.image}"
    //                            showControls="${!this.editMode && this.widget?.widgetConfig?.showTimestampControls}" showTitle="${false}" realm="${this.realm}" style="height: 100%;">
    //         </or-attribute-card>
    //     `
    // }

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

        return html
            `
                <style>
                    ${css}
                </style>
            <div style="height: 100%; display: flex; justify-content: center; align-items: center; position: relative;">
            <span></span>
            <img class="img-content" src="https://home3ds.com/wp-content/uploads/2018/11/PNG.png" alt="">
            </div>
            `;

        
        //general logic for rendering changes in widget from settings interaction
        // if (!this.widget?.widgetConfig.imageUploaded) {
        //     return html
        //     `<div style="height: 100%; display: flex; justify-content: center; align-items: center;">
        //     <span>${i18next.t('noAttributesConnected')}</span>
        //     </div>`;
        // } else {
        //     // doesnt necessarily need a request update??? tho thats pretty strange imo
        //     // this.requestUpdate();
        //     console.log("Inside render for image widget");
        //     console.log(this.widget?.widgetConfig.image);
        //     return html
        //     `<div style="height: 100%; display: flex; justify-content: center; align-items: center; position: relative;">
        //     <span></span>
        //     <img class="img" src="https://scontent.xx.fbcdn.net/v/t1.15752-9/336973731_198428016250982_2588607385324835429_n.jpg?stp=dst-jpg_s403x403&_nc_cat=111&ccb=1-7&_nc_sid=aee45a&_nc_ohc=MHIkM7wrLoUAX_86ZpQ&_nc_ad=z-m&_nc_cid=0&_nc_ht=scontent.xx&oh=03_AdQxNZAtLZiQT-r_c6EzxWUFLWwykEY7Tgmk0VZo6i0qfA&oe=645237EC" alt="">
        //     </div>`;
            
        // }

        //tested html progress for rendering image from local url
        // `<div style="height: 100%; display: flex; justify-content: center; align-items: center; position: relative;">
            // <span></span>
            // <img class="img" src="${URL.createObjectURL(this.widget?.widgetConfig.image)}" alt="">
            // </div>`;
        
        
    }


    updated(changedProperties: Map<string, any>) {
        if(changedProperties.has("widget") || changedProperties.has("editMode")) {
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
        if(config.attributeRefs && config.attributeRefs.length > 0) {
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

    // Default values
    private expandedPanels: string[] = [i18next.t('attributes'), i18next.t('display'), i18next.t('values'), i18next.t('image settings')];
    private loadedAsset?: Asset;


    static get styles() {
        return [style, widgetSettingsStyling];
    }

    // UI Rendering
    render() {
        const config = JSON.parse(JSON.stringify(this.widget!.widgetConfig)) as ImageWidgetConfig; // duplicate to edit, to prevent parent updates. Please trigger updateConfig()
        
        return html`
            <div>
                ${this.generateExpandableHeader(i18next.t('attributes'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('attributes')) ? html`
                    <or-dashboard-settingspanel .type="${SettingsPanelType.SINGLE_ATTRIBUTE}" .widgetConfig="${this.widget!.widgetConfig}"
                                                @updated="${(event: CustomEvent) => {
                                                    this.onAttributesUpdate(event.detail.changes);
                                                    this.updateConfig(this.widget!, event.detail.changes.get('config'));
                                                }}"
                    ></or-dashboard-settingspanel>
                ` : null}
            </div>
            <div>
                ${this.generateExpandableHeader(i18next.t('display'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('display')) ? html`
                    <div style="padding: 24px 24px 48px 24px;">
                        <div>
                            <or-mwc-input .type="${InputType.SELECT}" style="width: 100%;" 
                                          .options="${displayOptions}" 
                                          .value="${config.displayMode}" label="${i18next.t('Display options')}" 
                                          @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                            config.displayMode = event.detail.value;
                                            this.updateConfig(this.widget!, config);
                                          }}"
                            ></or-mwc-input>
                        </div>
                        <div class="switchMwcInputContainer" style="margin-top: 16px;">
                            <span>${i18next.t('dashboard.allowTimerangeSelect')}</span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="margin: 0 -10px;" .value="${config.showTimestampControls}"
                                          @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                              config.showTimestampControls = event.detail.value;
                                              this.updateConfig(this.widget!, config);
                                          }}"
                            ></or-mwc-input>
                        </div>
                    </div>
                ` : null}
            </div>
            <div>
                ${this.generateExpandableHeader(i18next.t('values'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('values')) ? html`
                    <div style="padding: 24px 24px 48px 24px;">
                        <div>
                            <or-mwc-input .type="${InputType.SELECT}" style="width: 100%;" .options="${deltaOptions}" .value="${config.deltaFormat}" label="${i18next.t('dashboard.showValueAs')}"
                                          @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                              config.deltaFormat = event.detail.value;
                                              this.updateConfig(this.widget!, config);
                                          }}"
                            ></or-mwc-input>
                        </div>
                        <div style="margin-top: 18px;">
                            <or-mwc-input .type="${InputType.NUMBER}" style="width: 100%;" .value="${config.decimals}" label="${i18next.t('decimals')}"
                                          @or-mwc-input-changed="${(event: OrInputChangedEvent) => { 
                                              config.decimals = event.detail.value;
                                              this.updateConfig(this.widget!, config);
                                          }}"
                            ></or-mwc-input>
                        </div>
                    </div>
                ` : null}
            </div>
            <div>
                ${this.generateExpandableHeader(i18next.t('image settings'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('image settings')) ? html`
                    <div style="padding: 24px 24px 48px 24px;">
                        <div>
                            <or-mwc-input .type="${InputType.BUTTON}" style="width: 100%;"" .value="${config.imageUploaded}" label="${i18next.t('Upload Image')}"
                                          @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                            console.log(config);
                                            if (event.detail.value) {
                                                const input = document.createElement('input');
                                                input.type = 'file';
                                                input.click();
                                                console.log(input);
                                                console.log(event);
                                                input.addEventListener("change", function() {
                                                    if (input.files != null){
                                                        config.image = input.files[0].name;
                                                    }
                                                })
                                                
                                            }
                                            
                                              config.imageUploaded = event.detail.value;

                                              this.updateConfig(this.widget!, config);
                                          }}"
                            ></or-mwc-input>
                        </div>
                    </div>
                ` : null}
            </div>
        `
    }

    fileUpload() {
        const fileSelector = document.createElement('input');
        fileSelector.setAttribute('type', 'file');

        var selectDialogueLink = document.createElement('a');
        selectDialogueLink.setAttribute('href', '');
        selectDialogueLink.innerText = "Select file";

        selectDialogueLink.onclick = function() {
            fileSelector.click();
            return false;
        };

        document.body.appendChild(selectDialogueLink);
    }

    updateConfig(widget: DashboardWidget, config: OrWidgetConfig | any, force: boolean = false) {
        const oldWidget = JSON.parse(JSON.stringify(widget)) as DashboardWidget;
        widget.widgetConfig = config;
        this.requestUpdate("widget", oldWidget);
        this.forceParentUpdate(new Map<string, any>([["widget", widget]]), force);
    }

    onAttributesUpdate(changes: Map<string, any>) {
        if(changes.has('loadedAssets')) {
            this.loadedAsset = changes.get('loadedAssets')[0];
        }
        if(changes.has('config')) {
            const config = changes.get('config') as ImageWidgetConfig;
            if(config.attributeRefs.length > 0) {
                this.widget!.displayName = this.loadedAsset?.name + " - " + this.loadedAsset?.attributes![config.attributeRefs[0].name!].name;
            }
        }
    }

    // Method to update the Grid. For example after changing a setting.
    forceParentUpdate(changes: Map<string, any>, force: boolean = false) {
        this.requestUpdate();
        this.dispatchEvent(new CustomEvent('updated', {detail: {changes: changes, force: force}}));
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
