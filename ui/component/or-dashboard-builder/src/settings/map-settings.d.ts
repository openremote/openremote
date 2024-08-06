import { TemplateResult } from "lit";
import { AssetWidgetSettings } from "../util/or-asset-widget";
import { OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { MapWidgetConfig } from "../widgets/map-widget";
import "../panels/assettypes-panel";
import "../panels/thresholds-panel";
import { AssetTypeSelectEvent, AttributeNamesSelectEvent } from "../panels/assettypes-panel";
import { ThresholdChangeEvent } from "../panels/thresholds-panel";
export declare class MapSettings extends AssetWidgetSettings {
    protected widgetConfig: MapWidgetConfig;
    static get styles(): import("lit").CSSResult[];
    protected render(): TemplateResult;
    protected onZoomUpdate(ev: OrInputChangedEvent): void;
    protected onCenterUpdate(ev: OrInputChangedEvent): void;
    protected onGeoJsonToggle(ev: OrInputChangedEvent): void;
    protected onAssetTypeSelect(ev: AssetTypeSelectEvent): void;
    protected onAttributeNameSelect(ev: AttributeNamesSelectEvent): Promise<void>;
    protected onShowLabelsToggle(ev: OrInputChangedEvent): void;
    protected onShowUnitsToggle(ev: OrInputChangedEvent): void;
    protected onThresholdsChange(ev: ThresholdChangeEvent): void;
}
