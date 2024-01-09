import {html, TemplateResult} from "lit";
import {customElement, state} from "lit/decorators.js";
import {OrAssetWidget} from "../util/or-asset-widget";
import {WidgetConfig} from "../util/widget-config";
import {Asset, Attribute, AttributeRef} from "@openremote/model";
import {OrWidget, WidgetManifest} from "../util/or-widget";
import {WidgetSettings} from "../util/widget-settings";
import {GaugeSettings} from "../settings/gauge-settings";
import {when} from "lit/directives/when.js";
import "@openremote/or-gauge";

export interface GaugeWidgetConfig extends WidgetConfig {
    attributeRefs: AttributeRef[];
    thresholds: [number, string][];
    decimals: number;
    min: number;
    max: number;
    valueType: string;
}

function getDefaultWidgetConfig(): GaugeWidgetConfig {
    return {
        attributeRefs: [],
        thresholds: [[0, "#4caf50"], [75, "#ff9800"], [90, "#ef5350"]], // colors from https://mui.com/material-ui/customization/palette/ as reference (since material has no official colors)
        decimals: 0,
        min: 0,
        max: 100,
        valueType: 'number',
    };
}

@customElement("gauge-widget")
export class GaugeWidget extends OrAssetWidget {

    // Override of widgetConfig with extended type
    protected widgetConfig!: GaugeWidgetConfig;

    static getManifest(): WidgetManifest {
        return {
            displayName: "Gauge",
            displayIcon: "gauge",
            minColumnWidth: 1,
            minColumnHeight: 1,
            getContentHtml(config: GaugeWidgetConfig): OrWidget {
                return new GaugeWidget(config);
            },
            getSettingsHtml(config: GaugeWidgetConfig): WidgetSettings {
                return new GaugeSettings(config);
            },
            getDefaultConfig(): GaugeWidgetConfig {
                return getDefaultWidgetConfig();
            }
        }
    }

    public refreshContent(force: boolean) {
        this.loadAssets(this.widgetConfig.attributeRefs);
    }

    // WebComponent lifecycle method, that occurs AFTER every state update
    protected updated(changedProps: Map<string, any>) {
        super.updated(changedProps);

        // If widgetConfig, and the attributeRefs of them have changed...
        if(changedProps.has("widgetConfig") && this.widgetConfig) {
            const attributeRefs = this.widgetConfig.attributeRefs;

            // Check if list of attributes has changed, based on the cached assets
            const loadedRefs: AttributeRef[] = attributeRefs?.filter((attrRef: AttributeRef) => this.isAttributeRefLoaded(attrRef));
            if (loadedRefs?.length !== (attributeRefs ? attributeRefs.length : 0)) {

                // Fetch the new list of assets
                this.loadAssets(attributeRefs);

            }
        }
        return super.updated(changedProps);
    }

    protected loadAssets(attributeRefs: AttributeRef[]) {
        this.fetchAssets(attributeRefs).then((assets) => {
            this.loadedAssets = assets;
            this.assetAttributes = attributeRefs?.map((attrRef: AttributeRef) => {
                const assetIndex = assets.findIndex((asset) => asset.id === attrRef.id);
                const foundAsset = assetIndex >= 0 ? assets[assetIndex] : undefined;
                return foundAsset && foundAsset.attributes ? [assetIndex, foundAsset.attributes[attrRef.name!]] : undefined;
            }).filter((indexAndAttr: any) => !!indexAndAttr) as [number, Attribute<any>][];
        });
    }


    protected render(): TemplateResult {
        return html`
            ${when(this.loadedAssets && this.assetAttributes && this.loadedAssets.length > 0 && this.assetAttributes.length > 0, () => {
                return html`
                    <or-gauge .asset="${this.loadedAssets[0]}" .assetAttribute="${this.assetAttributes[0]}" .thresholds="${this.widgetConfig.thresholds}"
                              .decimals="${this.widgetConfig.decimals}" .min="${this.widgetConfig.min}" .max="${this.widgetConfig.max}"
                              style="height: 100%; overflow: hidden;">
                    </or-gauge>
                `;
            }, () => {
                return html`
                    <div style="height: 100%; display: flex; justify-content: center; align-items: center;">
                        <span><or-translate value="noAttributeConnected"></or-translate></span>
                    </div>
                `
            })}
        `;
    }

}
