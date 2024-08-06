var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var ImageWidget_1;
import { AssetModelUtil } from "@openremote/model";
import { customElement } from "lit/decorators.js";
import { css, html, unsafeCSS } from "lit";
import { OrAssetWidget } from "../util/or-asset-widget";
import { ImageSettings } from "../settings/image-settings";
import { when } from "lit/directives/when.js";
import { DefaultColor2, DefaultColor3, Util } from "@openremote/core";
import { styleMap } from "lit/directives/style-map.js";
const styling = css `
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

    /* additional marker styling */
    color: var(--or-app-color2, ${unsafeCSS(DefaultColor2)});
    background-color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
    border-radius: 15px;
    padding: 3px 8px 5px 8px;
    object-fit: contain;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`;
function getDefaultWidgetConfig() {
    return {
        attributeRefs: [],
        showTimestampControls: false,
        imagePath: '',
        markers: [],
    };
}
let ImageWidget = ImageWidget_1 = class ImageWidget extends OrAssetWidget {
    static getManifest() {
        return {
            displayName: "Image",
            displayIcon: "file-image-marker",
            minColumnWidth: 1,
            minColumnHeight: 1,
            getContentHtml(config) {
                return new ImageWidget_1(config);
            },
            getSettingsHtml(config) {
                return new ImageSettings(config);
            },
            getDefaultConfig() {
                return getDefaultWidgetConfig();
            }
        };
    }
    refreshContent(force) {
        this.loadAssets();
    }
    static get styles() {
        return [...super.styles, styling];
    }
    updated(changedProps) {
        if (changedProps.has('widgetConfig') && this.widgetConfig) {
            const attributeRefs = this.widgetConfig.attributeRefs;
            const missingAssets = attributeRefs === null || attributeRefs === void 0 ? void 0 : attributeRefs.filter((attrRef) => !this.isAttributeRefLoaded(attrRef));
            if (missingAssets.length > 0) {
                this.loadAssets();
            }
        }
    }
    loadAssets() {
        this.fetchAssets(this.widgetConfig.attributeRefs).then(assets => {
            this.loadedAssets = assets;
            this.assetAttributes = this.widgetConfig.attributeRefs.map((attrRef) => {
                const assetIndex = assets.findIndex(asset => asset.id === attrRef.id);
                const foundAsset = assetIndex >= 0 ? assets[assetIndex] : undefined;
                return foundAsset && foundAsset.attributes ? [assetIndex, foundAsset.attributes[attrRef.name]] : undefined;
            }).filter((indexAndAttr) => !!indexAndAttr);
        });
    }
    // method to render and update the markers on the image
    handleMarkerPlacement(config) {
        if (this.assetAttributes.length && config.attributeRefs.length > 0) {
            if (config.markers.length === 0) {
                console.error("No markers found!");
                return [];
            }
            return config.attributeRefs.map((attributeRef, index) => {
                const marker = config.markers.find(m => m.attributeRef.id === attributeRef.id && m.attributeRef.name === attributeRef.name);
                const asset = this.loadedAssets.find(a => a.id === attributeRef.id);
                let value;
                const styles = {
                    "left": `${marker.coordinates[0]}%`,
                    "top": `${marker.coordinates[1]}%`
                };
                if (asset) {
                    const attribute = asset.attributes[attributeRef.name];
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attributeRef.name, attribute);
                    value = Util.getAttributeValueAsString(attribute, descriptors[0], asset.type, true, "-");
                    if ((attribute === null || attribute === void 0 ? void 0 : attribute.type) === "colourRGB" /* WellknownValueTypes.COLOURRGB */ && value !== "-") {
                        styles.backgroundColor = value;
                        styles.minHeight = "21px";
                        styles.minWidth = "13px";
                        value = undefined;
                    }
                }
                return html `
                    <span id="overlay" style="${styleMap(styles)}">
                        ${value}
                    </span>
                `;
            });
        }
    }
    render() {
        const imagePath = this.widgetConfig.imagePath;
        return html `
            <div id="img-wrapper">
                ${when(imagePath, () => html `
                    <div id="img-container">
                        <img id="img-content" src="${imagePath}" alt=""/>
                        <div>
                            ${this.handleMarkerPlacement(this.widgetConfig)}
                        </div>
                    </div>
                `, () => html `
                    <span><or-translate value="dashboard.noImageSelected"></or-translate></span>
                `)}
            </div>
        `;
    }
};
ImageWidget = ImageWidget_1 = __decorate([
    customElement("image-widget")
], ImageWidget);
export { ImageWidget };
//# sourceMappingURL=image-widget.js.map