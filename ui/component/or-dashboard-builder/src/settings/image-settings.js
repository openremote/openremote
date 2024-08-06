var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html } from "lit";
import { customElement } from "lit/decorators.js";
import { i18next } from "@openremote/or-translate";
import { AssetModelUtil } from "@openremote/model";
import { map } from "lit/directives/map.js";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { Util } from "@openremote/core";
import { when } from "lit/directives/when.js";
import { AssetWidgetSettings } from "../util/or-asset-widget";
const styling = css `
  #marker-container {
    display: flex;
    justify-content: flex-end;
    align-items: center;
  }
`;
let ImageSettings = class ImageSettings extends AssetWidgetSettings {
    static get styles() {
        return [...super.styles, styling];
    }
    willUpdate(changedProps) {
        super.willUpdate(changedProps);
        if (changedProps.has('widgetConfig') && this.widgetConfig) {
            this.updateCoordinateMap(this.widgetConfig);
            this.loadAssets();
        }
    }
    loadAssets() {
        const missingAssets = this.widgetConfig.attributeRefs.filter(ref => !this.isAttributeRefLoaded(ref));
        if (missingAssets.length > 0) {
            this.fetchAssets(this.widgetConfig.attributeRefs).then((assets) => {
                if (assets === undefined) {
                    this.loadedAssets = [];
                }
                else {
                    this.loadedAssets = assets;
                }
            });
        }
    }
    render() {
        return html `
            <div>
                <!-- Attributes selector -->
                <settings-panel displayName="attributes" expanded="${true}">
                    <attributes-panel .attributeRefs="${this.widgetConfig.attributeRefs}" onlyDataAttrs="${false}" multi="${true}"
                                      @attribute-select="${(ev) => this.onAttributesSelect(ev)}"
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
                                      @or-mwc-input-changed="${(ev) => this.onImageUrlUpdate(ev)}"
                        ></or-mwc-input>
                    </div>
                </settings-panel>
            </div>
        `;
    }
    onAttributesSelect(ev) {
        this.widgetConfig.attributeRefs = ev.detail.attributeRefs;
        this.notifyConfigUpdate();
    }
    onImageUrlUpdate(ev) {
        this.widgetConfig.imagePath = ev.detail.value;
        this.notifyConfigUpdate();
    }
    /* -------------------------------------- */
    // updates coordinate map according to the attributeRef entries per id
    updateCoordinateMap(config) {
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
    draftCoordinateEntries(config) {
        const min = 0;
        const max = 100;
        if (config.markers.length > 0) {
            return config.attributeRefs.map((attributeRef) => {
                var _a, _b, _c;
                const marker = config.markers.find(m => m.attributeRef.id === attributeRef.id && m.attributeRef.name === attributeRef.name);
                if (marker === undefined) {
                    console.error("A marker could not be found during drafting coordinate entries.");
                    return html ``;
                }
                const index = config.markers.indexOf(marker);
                const coordinates = marker.coordinates;
                const asset = (_a = this.loadedAssets) === null || _a === void 0 ? void 0 : _a.find(a => a.id === attributeRef.id);
                let label;
                if (asset) {
                    const attribute = asset.attributes[attributeRef.name];
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attributeRef.name, attribute);
                    label = Util.getAttributeLabel(attribute, descriptors[0], asset.type, false);
                }
                return html `
                    <div id="marker-container">
                        <div style="flex: 1; display: flex; flex-direction: column;">
                            <span>${(_c = (_b = this.loadedAssets) === null || _b === void 0 ? void 0 : _b.find(a => a.id === attributeRef.id)) === null || _c === void 0 ? void 0 : _c.name}</span>
                            ${when(label, () => html `
                                <span style="color: gray;">${label}</span>
                            `)}
                        </div>
                        <div style="display: flex; gap: 8px;">
                            <or-mwc-input .disableSliderNumberInput="${true}" compact style="max-width: 64px;"
                                          .type="${InputType.NUMBER}" .min="${min}" .max="${max}" .value="${coordinates[0]}"
                                          @or-mwc-input-changed="${(ev) => this.onCoordinateUpdate(index, 'x', ev.detail.value)}"
                            ></or-mwc-input>

                            <or-mwc-input .disableSliderNumberInput="${true}" compact style="max-width: 64px;"
                                          .type="${InputType.NUMBER}" .min="${min}" .max="${max}" .value="${coordinates[1]}"
                                          @or-mwc-input-changed="${(ev) => this.onCoordinateUpdate(index, 'y', ev.detail.value)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                `;
            });
        }
        else {
            return [
                html `<span><or-translate value="noAttributeConnected"></or-translate></span>`
            ];
        }
    }
    onCoordinateUpdate(index, coordinate, value) {
        let coords = this.widgetConfig.markers[index].coordinates;
        if (!coords) {
            coords = [0, 0];
        }
        if (coordinate === 'x') {
            coords[0] = value;
        }
        else if (coordinate === 'y') {
            coords[1] = value;
        }
        this.widgetConfig.markers[index].coordinates = coords;
        this.notifyConfigUpdate();
    }
};
ImageSettings = __decorate([
    customElement("image-settings")
], ImageSettings);
export { ImageSettings };
//# sourceMappingURL=image-settings.js.map