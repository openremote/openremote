import {css, html, PropertyValues, TemplateResult } from "lit";
import { customElement, state } from "lit/decorators.js";
import {ImageWidgetConfig} from "../widgets/image-widget";
import {i18next} from "@openremote/or-translate";
import {AttributesSelectEvent} from "../panels/attributes-panel";
import {Asset, AssetModelUtil, AttributeRef} from "@openremote/model";
import { map } from "lit/directives/map.js";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {Util} from "@openremote/core";
import { when } from "lit/directives/when.js";
import {AssetWidgetSettings} from "../util/or-asset-widget";

const styling = css`
  #marker-container {
    display: flex;
    justify-content: flex-end;
    align-items: center;
  }
`;

@customElement("image-settings")
export class ImageSettings extends AssetWidgetSettings {

    // Override of widgetConfig with extended type
    protected readonly widgetConfig!: ImageWidgetConfig;

    static get styles() {
        return [...super.styles, styling]
    }

    protected willUpdate(changedProps: PropertyValues) {
        super.willUpdate(changedProps);
        if(changedProps.has('widgetConfig') && this.widgetConfig) {
            this.updateCoordinateMap(this.widgetConfig);
            this.loadAssets();
        }
    }

    protected loadAssets() {
        const missingAssets = this.widgetConfig.attributeRefs.filter(ref => !this.isAttributeRefLoaded(ref));
        if(missingAssets.length > 0) {
            this.fetchAssets(this.widgetConfig.attributeRefs).then((assets) => {
                if(assets === undefined) {
                    this.loadedAssets = [];
                } else {
                    this.loadedAssets = assets;
                }
            });
        }
    }

    protected render(): TemplateResult {
        return html`
            <div>
                <!-- Attributes selector -->
                <settings-panel displayName="'attributes" expanded="${true}">
                    <attributes-panel .attributeRefs="${this.widgetConfig.attributeRefs}" onlyDataAttrs="${false}" multi="${true}"
                                      @attribute-select="${(ev: AttributesSelectEvent) => this.onAttributesSelect(ev)}"
                    ></attributes-panel>
                </settings-panel>
                
                <!-- Marker coordinates -->
                <settings-panel displayName="dashboard.markerCoordinates" expanded="${true}">
                    <div style="display: flex; flex-direction: column; gap: 8px;">
                        ${map(this.draftCoordinateEntries(this.widgetConfig), template => template)}
                    </div>
                </settings-panel>
                
                <!-- Image settings -->
                <settings-panel displayName="dashboard.imageSettings" expanded="${true}">
                    <div>
                        <or-mwc-input style="width: 100%;" type="${InputType.TEXT}" label="${i18next.t('dashboard.imageUrl')}" .value="${this.widgetConfig.imagePath}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onImageUrlUpdate(ev)}"
                        ></or-mwc-input>
                    </div>
                </settings-panel>
            </div>
        `;
    }

    protected onAttributesSelect(ev: AttributesSelectEvent) {
        this.widgetConfig.attributeRefs = ev.detail.attributeRefs;
        this.notifyConfigUpdate();
    }

    protected onImageUrlUpdate(ev: OrInputChangedEvent) {
        this.widgetConfig.imagePath = ev.detail.value;
        this.notifyConfigUpdate();
    }


    /* -------------------------------------- */

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
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onCoordinateUpdate(index, 'x', ev.detail.value)}"
                            ></or-mwc-input>

                            <or-mwc-input .disableSliderNumberInput="${true}" compact style="max-width: 64px;"
                                          .type="${InputType.NUMBER}" .min="${min}" .max="${max}" .value="${coordinates[1]}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onCoordinateUpdate(index, 'y', ev.detail.value)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                `;
            });
        } else {
            return [
                html`<span><or-translate value="noAttributeConnected"></or-translate></span>`
            ];
        }
    }

    protected onCoordinateUpdate(index: number, coordinate: 'x' | 'y', value: number) {
        let coords = this.widgetConfig.markers[index].coordinates;
        if(!coords) {
            coords = [0, 0];
        }
        if(coordinate === 'x') {
            coords[0] = value;
        } else if(coordinate === 'y') {
            coords[1] = value;
        }
        this.widgetConfig.markers[index].coordinates = coords;
        this.notifyConfigUpdate();
    }

}
