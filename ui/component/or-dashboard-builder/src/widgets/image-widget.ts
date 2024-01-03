import {WidgetConfig} from "../util/widget-config";
import {AssetModelUtil, Attribute, AttributeRef, WellknownValueTypes} from "@openremote/model";
import {OrWidget, WidgetManifest} from "../util/or-widget";
import { customElement } from "lit/decorators.js";
import {WidgetSettings} from "../util/widget-settings";
import {css, CSSResult, html, PropertyValues, TemplateResult, unsafeCSS } from "lit";
import {OrAssetWidget} from "../util/or-asset-widget";
import {ImageSettings} from "../settings/image-settings";
import { when } from "lit/directives/when.js";
import {DefaultColor2, DefaultColor3, Util} from "@openremote/core";
import { styleMap } from "lit/directives/style-map.js";

const styling = css`
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

export interface ImageAssetMarker {
    attributeRef: AttributeRef,
    coordinates: [number, number]
}

export interface ImageWidgetConfig extends WidgetConfig {
    attributeRefs: AttributeRef[];
    markers: ImageAssetMarker[];
    showTimestampControls: boolean;
    imagePath: string;
}

function getDefaultWidgetConfig(): ImageWidgetConfig {
    return {
        attributeRefs: [],
        showTimestampControls: false,
        imagePath: '',
        markers: [],
    };
}

@customElement("image-widget")
export class ImageWidget extends OrAssetWidget {

    // Override of widgetConfig with extended type
    protected readonly widgetConfig!: ImageWidgetConfig;

    static getManifest(): WidgetManifest {
        return {
            displayName: "Image",
            displayIcon: "file-image-marker",
            minColumnWidth: 1,
            minColumnHeight: 1,
            getContentHtml(config: ImageWidgetConfig): OrWidget {
                return new ImageWidget(config);
            },
            getSettingsHtml(config: ImageWidgetConfig): WidgetSettings {
                return new ImageSettings(config);
            },
            getDefaultConfig(): ImageWidgetConfig {
                return getDefaultWidgetConfig();
            }
        }
    }

    public refreshContent(force: boolean) {
        this.loadAssets();
    }

    static get styles(): CSSResult[] {
        return [...super.styles, styling];
    }

    updated(changedProps: PropertyValues) {

        if(changedProps.has('widgetConfig') && this.widgetConfig) {
            const attributeRefs = this.widgetConfig.attributeRefs;
            const missingAssets = attributeRefs?.filter((attrRef: AttributeRef) => !this.isAttributeRefLoaded(attrRef));
            if (missingAssets.length > 0) {
                this.loadAssets();
            }
        }
    }

    protected loadAssets() {
        this.fetchAssets(this.widgetConfig.attributeRefs).then(assets => {
            this.loadedAssets = assets!;
            this.assetAttributes = this.widgetConfig.attributeRefs.map((attrRef: AttributeRef) => {
                const assetIndex = assets!.findIndex(asset => asset.id === attrRef.id);
                const foundAsset = assetIndex >= 0 ? assets![assetIndex] : undefined;
                return foundAsset && foundAsset.attributes ? [assetIndex, foundAsset.attributes[attrRef.name!]] : undefined;
            }).filter((indexAndAttr: any) => !!indexAndAttr) as [number, Attribute<any>][];
        });
    }

    // method to render and update the markers on the image
    protected handleMarkerPlacement(config: ImageWidgetConfig) {
        if (this.assetAttributes.length && config.attributeRefs.length > 0) {

            if(config.markers.length === 0) {
                console.error("No markers found!");
                return [];
            }
            return config.attributeRefs.map((attributeRef, index) => {
                const marker = config.markers.find(m => m.attributeRef.id === attributeRef.id && m.attributeRef.name === attributeRef.name);
                const asset = this.loadedAssets.find(a => a.id === attributeRef.id);
                let value: string | undefined;
                const styles: any = {
                    "left": `${marker!.coordinates[0]}%`,
                    "top": `${marker!.coordinates[1]}%`
                };
                if(asset) {
                    const attribute = asset.attributes![attributeRef.name!];
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attributeRef.name, attribute);
                    value = Util.getAttributeValueAsString(attribute, descriptors[0], asset.type, true, "-");
                    if(attribute?.type === WellknownValueTypes.COLOURRGB && value !== "-") {
                        styles.backgroundColor = value;
                        styles.minHeight = "21px";
                        styles.minWidth = "13px";
                        value = undefined;
                    }
                }
                return html`
                    <span id="overlay" style="${styleMap(styles)}">
                        ${value}
                    </span>
                `;
            });
        }
    }

    protected render(): TemplateResult {
        const imagePath = this.widgetConfig.imagePath;
        return html`
            <div id="img-wrapper">
                ${when(imagePath, () => html`
                    <div id="img-container">
                        <img id="img-content" src="${imagePath}" alt=""/>
                        <div>
                            ${this.handleMarkerPlacement(this.widgetConfig)}
                        </div>
                    </div>
                `, () => html`
                    <span><or-translate value="dashboard.noImageSelected"></or-translate></span>
                `)}
            </div>
        `;
    }

}
