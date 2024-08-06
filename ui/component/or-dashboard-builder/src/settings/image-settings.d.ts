import { PropertyValues, TemplateResult } from "lit";
import { ImageWidgetConfig } from "../widgets/image-widget";
import { AttributesSelectEvent } from "../panels/attributes-panel";
import { OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { AssetWidgetSettings } from "../util/or-asset-widget";
export declare class ImageSettings extends AssetWidgetSettings {
    protected readonly widgetConfig: ImageWidgetConfig;
    static get styles(): import("lit").CSSResult[];
    protected willUpdate(changedProps: PropertyValues): void;
    protected loadAssets(): void;
    protected render(): TemplateResult;
    protected onAttributesSelect(ev: AttributesSelectEvent): void;
    protected onImageUrlUpdate(ev: OrInputChangedEvent): void;
    updateCoordinateMap(config: ImageWidgetConfig): void;
    private draftCoordinateEntries;
    protected onCoordinateUpdate(index: number, coordinate: 'x' | 'y', value: number): void;
}
