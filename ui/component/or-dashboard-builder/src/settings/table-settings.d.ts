import { TemplateResult } from "lit";
import { AssetWidgetSettings } from "../util/or-asset-widget";
import { TableWidgetConfig } from "../widgets/table-widget";
import { OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { AssetIdsSelectEvent, AssetTypeSelectEvent, AttributeNamesSelectEvent } from "../panels/assettypes-panel";
export declare class TableSettings extends AssetWidgetSettings {
    protected widgetConfig: TableWidgetConfig;
    static get styles(): import("lit").CSSResult[];
    protected render(): TemplateResult;
    protected onAssetTypeSelect(ev: AssetTypeSelectEvent): void;
    protected onAssetIdsSelect(ev: AssetIdsSelectEvent): void;
    protected onAttributesSelect(ev: AttributeNamesSelectEvent): void;
    protected onTableSizeSelect(ev: OrInputChangedEvent): void;
}
