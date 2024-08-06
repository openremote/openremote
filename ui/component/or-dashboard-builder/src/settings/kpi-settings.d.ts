import { TemplateResult } from "lit";
import { AssetWidgetSettings } from "../util/or-asset-widget";
import { KpiWidgetConfig } from "../widgets/kpi-widget";
import { AttributesSelectEvent } from "../panels/attributes-panel";
import { OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
export declare class KpiSettings extends AssetWidgetSettings {
    protected widgetConfig: KpiWidgetConfig;
    static get styles(): import("lit").CSSResult[];
    protected render(): TemplateResult;
    protected onAttributesSelect(ev: AttributesSelectEvent): void;
    protected onTimeframeSelect(ev: OrInputChangedEvent): void;
    protected onTimeframeToggle(ev: OrInputChangedEvent): void;
    protected onDeltaFormatSelect(ev: OrInputChangedEvent): void;
    protected onDecimalsChange(ev: OrInputChangedEvent): void;
}
