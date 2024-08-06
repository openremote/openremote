import { TemplateResult } from "lit";
import { WidgetSettings } from "../util/widget-settings";
import { GaugeWidgetConfig } from "../widgets/gauge-widget";
import { AttributesSelectEvent } from "../panels/attributes-panel";
import { OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { ThresholdChangeEvent } from "../panels/thresholds-panel";
export declare class GaugeSettings extends WidgetSettings {
    protected widgetConfig: GaugeWidgetConfig;
    protected render(): TemplateResult;
    protected onAttributesSelect(ev: AttributesSelectEvent): void;
    protected onMinMaxValueChange(type: 'min' | 'max', ev: OrInputChangedEvent): void;
    protected onDecimalsChange(ev: OrInputChangedEvent): void;
    protected onThresholdChange(ev: ThresholdChangeEvent): void;
}
