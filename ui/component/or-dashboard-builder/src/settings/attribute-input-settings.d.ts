import { TemplateResult } from "lit";
import { AttributeInputWidgetConfig } from "../widgets/attribute-input-widget";
import { AttributesSelectEvent } from "../panels/attributes-panel";
import { OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { AssetWidgetSettings } from "../util/or-asset-widget";
export declare class AttributeInputSettings extends AssetWidgetSettings {
    protected readonly widgetConfig: AttributeInputWidgetConfig;
    static get styles(): import("lit").CSSResult[];
    protected render(): TemplateResult;
    protected onAttributesSelect(ev: AttributesSelectEvent): void;
    protected onReadonlyToggle(ev: OrInputChangedEvent): void;
    protected onHelperTextToggle(ev: OrInputChangedEvent): void;
}
