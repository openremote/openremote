import { OrWidget } from "./or-widget";
import { Asset, AssetQuery, Attribute, AttributeRef } from "@openremote/model";
import { WidgetSettings } from "./widget-settings";
import { CSSResult } from "lit";
export declare abstract class OrAssetWidget extends OrWidget {
    protected loadedAssets: Asset[];
    protected assetAttributes: [number, Attribute<any>][];
    static get styles(): CSSResult[];
    protected fetchAssets(attributeRefs?: AttributeRef[]): Promise<Asset[]>;
    protected queryAssets(assetQuery: AssetQuery): Promise<Asset[]>;
    protected isAssetLoaded(assetId: string): boolean;
    protected isAttributeRefLoaded(attributeRef: AttributeRef): boolean;
}
export declare abstract class AssetWidgetSettings extends WidgetSettings {
    protected loadedAssets: Asset[];
    protected fetchAssets(attributeRefs?: AttributeRef[]): Promise<Asset[]>;
    protected queryAssets(assetQuery: AssetQuery): Promise<Asset[]>;
    protected isAssetLoaded(assetId: string): boolean;
    protected isAttributeRefLoaded(attributeRef: AttributeRef): boolean;
}
