import manager, {DefaultColor2, DefaultColor3, Util} from '@openremote/core';
import {
    Asset,
    AssetModelUtil,
    Attribute,
    AttributeRef,
    DashboardWidget
} from "@openremote/model";
import {showSnackbar} from '@openremote/or-mwc-components/or-mwc-snackbar';
import {i18next} from '@openremote/or-translate';
import {css, html, LitElement, TemplateResult, unsafeCSS} from 'lit';
import {customElement, property, state} from 'lit/decorators.js';
import {OrWidgetConfig, OrWidgetEntity} from './or-base-widget';
import {when} from 'lit/directives/when.js';
import {map} from 'lit/directives/map.js';
import {style} from '../style';
import {SettingsPanelType, widgetSettingsStyling} from '../or-dashboard-settingspanel';
import {InputType, OrInputChangedEvent} from '@openremote/or-mwc-components/or-mwc-input';

export interface ImageAssetMarker {
    attributeRef: AttributeRef,
    coordinates: [number, number]
}

export interface ImageWidgetConfig extends OrWidgetConfig {
    displayName: string;
    attributeRefs: AttributeRef[];
    markers: ImageAssetMarker[];
    showTimestampControls: boolean;
    imagePath: string;
}

export class OrImageWidget implements OrWidgetEntity {

    readonly DISPLAY_MDI_ICON: string = 'file-image-marker'; // https://materialdesignicons.com;
    readonly DISPLAY_NAME: string = 'Image';
    readonly MIN_COLUMN_WIDTH: number = 2;
    readonly MIN_PIXEL_HEIGHT: number = 0;
    readonly MIN_PIXEL_WIDTH: number = 0;

    getDefaultConfig(widget: DashboardWidget): OrWidgetConfig {
        return {
            displayName: widget?.displayName,
            attributeRefs: [],
            showTimestampControls: false,
            imagePath: '',
            markers: [],
        } as unknown as ImageWidgetConfig;
    }

    getSettingsHTML(widget: DashboardWidget, realm: string) {
        return html`
            <or-image-widgetsettings .widget="${widget}" realm="${realm}"></or-image-widgetsettings>`;
    }

    getWidgetHTML(widget: DashboardWidget, editMode: boolean, realm: string) {
        return html`
            <or-image-widget .widget="${widget}" .editMode="${editMode}" realm="${realm}" style="height: 100%; overflow: hidden;"></or-image-widget>`;
    }

// Triggered every update to double check if the specification.
    // It will merge missing values, or you can add custom logic to process here.
    verifyConfigSpec(widget: DashboardWidget): ImageWidgetConfig {
        return Util.mergeObjects(this.getDefaultConfig(widget), widget.widgetConfig, false) as ImageWidgetConfig;
    }
}

const contentStyling = css`
    #img-wrapper {
        height: 100%;
        width: 100%;
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        overflow: hidden;
        z-index: 1;
    }

    #img-container {
      position: relative;
      max-height: 100%;
    }
    
    #img-content {
      height: 100%;
      max-height: 100%;
      max-width: 100%;
    }

    #overlay {
        position: absolute;
        z-index: 3;
    
        /*additional marker styling*/
        color: var(--or-app-color2, ${unsafeCSS(DefaultColor2)});
        background-color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
        border-radius: 15px;
        padding: 3px 8px 5px 8px;
        object-fit: contain;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
`;

@customElement('or-image-widget')
export class OrImageWidgetContent extends LitElement {

    @property()
    public realm?: string;

    @property()
    public readonly widget?: DashboardWidget;

    @property()
    public editMode?: boolean;

    @state()
    protected assets: Asset[] = [];

    @state()
    protected assetAttributes: [number, Attribute<any>][] = [];

    static styles = contentStyling;

    // method to render and update the markers on the image
    private handleMarkerPlacement(config: ImageWidgetConfig) {
        if (this.assetAttributes.length && config.attributeRefs.length > 0) {

            if(config.markers.length === 0) {
                console.error("No markers found!");
                return [];
            }
            return config.attributeRefs.map((attributeRef, index) => {
                const marker = config.markers.find(m => m.attributeRef.id === attributeRef.id && m.attributeRef.name === attributeRef.name);
                const asset = this.assets.find(a => a.id === attributeRef.id);
                let value: string | undefined;
                if(asset) {
                    const attribute = asset.attributes![attributeRef.name!];
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attributeRef.name, attribute);
                    value = Util.getAttributeValueAsString(attribute, descriptors[0], asset.type, true, "-");
                }
                return html`
                    <span id="overlay" style="left: ${marker!.coordinates[0]}%; top: ${marker!.coordinates[1]}%">
                        ${value}
                    </span>
                `;
            });
        }
    }


    render() {
        const imagePath = this.widget?.widgetConfig.imagePath;
        return html`
            <div id="img-wrapper">
                ${when(imagePath, () => html`
                    <div id="img-container">
                        <img id="img-content" src="${imagePath}" alt=""/>
                        <div>
                            ${this.handleMarkerPlacement(this.widget?.widgetConfig)}
                        </div>
                    </div>
                `, () => html`
                    <span>${i18next.t('dashboard.noImageSelected')}</span>
                `)}
            </div>
        `;
    }

    updated(changedProperties: Map<string, any>) {
        if (changedProperties.has('widget') || changedProperties.has('editMode')) {
            if (this.assetAttributes.length !== this.widget!.widgetConfig.attributeRefs.length) {
                this.fetchAssets(this.widget?.widgetConfig).then(assets => {
                    this.assets = assets!;
                    this.assetAttributes = this.widget?.widgetConfig.attributeRefs.map((attrRef: AttributeRef) => {
                        const assetIndex = assets!.findIndex(asset => asset.id === attrRef.id);
                        const foundAsset = assetIndex >= 0 ? assets![assetIndex] : undefined;
                        return foundAsset && foundAsset.attributes ? [assetIndex, foundAsset.attributes[attrRef.name!]] : undefined;
                    }).filter((indexAndAttr: any) => !!indexAndAttr) as [number, Attribute<any>][];
                });
            }
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
            }).catch(reason => {
                console.error(reason);
                showSnackbar(undefined, i18next.t('errorOccurred'));
            });
            return assets;
        }
    }
}

const markerContainerStyling = css`
  #marker-container {
    display: flex;
    justify-content: flex-end;
    align-items: center;
  }
`;

@customElement('or-image-widgetsettings')
export class OrImageWidgetSettings extends LitElement {

    static get styles() {
        return [style, widgetSettingsStyling, markerContainerStyling];
    }

    private expandedPanels: string[] = [i18next.t('attributes'), i18next.t('dashboard.markerCoordinates'), i18next.t('dashboard.imageSettings')];

    @state()
    private loadedAssets?: Asset[];

    @property()
    public readonly widget?: DashboardWidget;

    protected updated(changedProps: Map<string, any>) {
        if(this.loadedAssets === undefined) {
            this.fetchAssets(this.widget?.widgetConfig).then((assets) => {
                if(assets === undefined) {
                    this.loadedAssets = [];
                } else {
                    this.loadedAssets = assets;
                }
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
                    name: manager.displayRealm // TODO: Improve this
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

    // UI Rendering
    render() {
        const config = JSON.parse(JSON.stringify(this.widget!.widgetConfig)) as ImageWidgetConfig; // duplicate to edit, to prevent parent updates. Please trigger updateConfig()
        return html`
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
                ${this.generateExpandableHeader(i18next.t('dashboard.markerCoordinates'))}
            </div>
            <div>
                ${when(this.expandedPanels.includes(i18next.t('dashboard.markerCoordinates')), () => html`
                    <div style="display: flex; flex-direction: column; gap: 8px; padding: 8px 16px 32px 16px;">
                        ${map(this.draftCoordinateEntries(config), template => template)}
                    </div>
                `)}
            </div>
            <div>
                ${this.generateExpandableHeader(i18next.t('dashboard.imageSettings'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('dashboard.imageSettings')) ? html`
                    <div style="padding: 8px 16px 32px 16px;">
                        <or-mwc-input style="width: 100%;" type="${InputType.TEXT}" label="${i18next.t('dashboard.imageUrl')}" .value="${config.imagePath}"
                                      @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                          config.imagePath = event.detail.value;
                                          this.updateConfig(this.widget!, config);
                                      }}"
                        ></or-mwc-input>
                    </div>
                ` : null}
            </div>
        `;
    }


    private draftCoordinateEntries(config: ImageWidgetConfig): TemplateResult[] {
        const min = 0;
        const max = 100;

        if (config.markers.length > 0) {
            return config.attributeRefs.map((attributeRef) => {
                const marker = config.markers.find(m => m.attributeRef.id === attributeRef.id && m.attributeRef.name === attributeRef.name);
                if(marker === undefined) {
                    console.error("A marker could not be found during drafting coordinate entries.");
                    return html``;
                }
                const index = config.markers.indexOf(marker);
                const coordinates = marker.coordinates;
                const asset = this.loadedAssets?.find(a => a.id === attributeRef.id);
                let label: string | undefined;
                if(asset) {
                    const attribute = asset.attributes![attributeRef.name!];
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attributeRef.name, attribute);
                    label = Util.getAttributeLabel(attribute, descriptors[0], asset.type, false);
                }
                return html`
                    <div id="marker-container">
                        <div style="flex: 1; display: flex; flex-direction: column;">
                            <span>${this.loadedAssets?.find(a => a.id === attributeRef.id)?.name}</span>
                            ${when(label, () => html`
                                <span style="color: gray;">${label}</span>
                            `)}
                        </div>
                        <div style="display: flex; gap: 8px;">
                            <or-mwc-input .disableSliderNumberInput="${true}" compact style="max-width: 64px;"
                                          .type="${InputType.NUMBER}" .min="${min}" .max="${max}" .value="${coordinates[0]}"
                                          @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                              config.markers[index].coordinates = [event.detail.value, coordinates[1]];
                                              this.updateConfig(this.widget!, config);
                                          }}"
                            ></or-mwc-input>

                            <or-mwc-input .disableSliderNumberInput="${true}" compact style="max-width: 64px;"
                                          .type="${InputType.NUMBER}" .min="${min}" .max="${max}" .value="${coordinates[1]}"
                                          @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                              config.markers[index].coordinates = [coordinates[0], event.detail.value];
                                              this.updateConfig(this.widget!, config);
                                          }}"
                            ></or-mwc-input>
                        </div>
                    </div>
                `;
            });
        } else {
            return [
                html`<span>${i18next.t('noAttributeConnected')}</span>`
            ];
        }
    }

    // updates coordinate map according to the attributeRef entries per id
    updateCoordinateMap(config: ImageWidgetConfig) {
        for (let i = 0; i < config.attributeRefs.length; i++) {
            const attributeRef = config.attributeRefs[i];
            if (attributeRef === undefined) {
                console.error('attributeRef is undefined');
                return;
            }
            const index = config.markers.findIndex(m => m.attributeRef.id === attributeRef.id && m.attributeRef.name === attributeRef.name);
            if (index === -1) {
                config.markers.push({
                    attributeRef: attributeRef,
                    coordinates: [50, 50]
                });
            }
        }
    }

    private updateConfig(widget: DashboardWidget, config: OrWidgetConfig | any, force = false) {
        const oldWidget = JSON.parse(JSON.stringify(widget)) as DashboardWidget;
        widget.widgetConfig = config;
        this.requestUpdate('widget', oldWidget);
        this.forceParentUpdate(new Map<string, any>([['widget', widget]]), force);
    }


    private onAttributesUpdate(changes: Map<string, any>) {
        if (changes.has('loadedAssets')) {
            this.loadedAssets = changes.get('loadedAssets');
        }
        if (changes.has('config')) {
            const config = changes.get('config') as ImageWidgetConfig;
            this.updateCoordinateMap(config);
        }
    }


    // Method to update the Grid. For example after changing a setting.
    private forceParentUpdate(changes: Map<string, any>, force = false) {
        this.requestUpdate();
        this.dispatchEvent(new CustomEvent('updated', {detail: {changes: changes, force: force}}));
    }


    private generateExpandableHeader(name: string): TemplateResult {
        return html`
            <span class="expandableHeader panel-title" @click="${() => {
                this.expandPanel(name);
            }}">
                <or-icon icon="${this.expandedPanels.includes(name) ? 'chevron-down' : 'chevron-right'}"></or-icon>
                <span style="margin-left: 6px; height: 25px; line-height: 25px;">${name}</span>
            </span>
        `;
    }


    private expandPanel(panelName: string): void {
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
