import { customElement } from "lit/decorators.js";
import {OrAssetWidget} from "../util/or-asset-widget";
import {OrWidget, WidgetManifest} from "../util/or-widget";
import {WidgetSettings} from "../util/widget-settings";
import {WidgetConfig} from "../util/widget-config";
import {Attribute, AttributeRef} from "@openremote/model";
import {html, TemplateResult } from "lit";
import {KpiSettings} from "../settings/kpi-settings";
import "@openremote/or-attribute-card";

export interface KpiWidgetConfig extends WidgetConfig {
    attributeRefs: AttributeRef[];
    period?: 'year' | 'month' | 'week' | 'day' | 'hour';
    decimals: number;
    deltaFormat: "absolute" | "percentage";
    showTimestampControls: boolean;
}

function getDefaultWidgetConfig(): KpiWidgetConfig {
    return {
        attributeRefs: [],
        period: "day",
        decimals: 0,
        deltaFormat: "absolute",
        showTimestampControls: false
    };
}

@customElement("kpi-widget")
export class KpiWidget extends OrAssetWidget {

    protected widgetConfig!: KpiWidgetConfig;

    static getManifest(): WidgetManifest {
        return {
            displayName: "KPI",
            displayIcon: "label",
            minColumnWidth: 1,
            minColumnHeight: 1,
            getContentHtml(config: KpiWidgetConfig): OrWidget {
                return new KpiWidget(config);
            },
            getSettingsHtml(config: KpiWidgetConfig): WidgetSettings {
                return new KpiSettings(config);
            },
            getDefaultConfig(): KpiWidgetConfig {
                return getDefaultWidgetConfig();
            }
        }
    }

    refreshContent(force: boolean): void {
        this.loadAssets(this.widgetConfig.attributeRefs);
    }

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
            <div style="height: 100%; overflow: hidden;">
                <or-attribute-card .assets="${this.loadedAssets}" .assetAttributes="${this.assetAttributes}" .period="${this.widgetConfig.period}"
                                   .deltaFormat="${this.widgetConfig.deltaFormat}" .mainValueDecimals="${this.widgetConfig.decimals}"
                                   showControls="${this.widgetConfig?.showTimestampControls}" showTitle="${false}" style="height: 100%;">
                </or-attribute-card>
            </div>
        `;
    }

}
