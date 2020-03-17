import {customElement, property, PropertyValues} from "lit-element";
import {OrMapMarker} from "./or-map-marker";
import {AttributeEvent, AttributeType, GeoJSONPoint, AssetType, AssetEvent, AssetEventCause, Asset, MetaItemType} from "@openremote/model";
import {subscribe} from "@openremote/core";
import manager, {AssetModelUtil} from "@openremote/core";
import {Util} from "@openremote/core";

@customElement("or-map-marker-asset")
export class OrMapMarkerAsset extends subscribe(manager)(OrMapMarker) {

    @property({type: String, reflect: true, attribute: true})
    public assetId?: string;

    @property({type: Object, attribute: true})
    public asset?: Asset;

    public assetTypeAsIcon: boolean = true;

    constructor() {
        super();
        this.visible = false;
    }

    protected markerColor?: string;

    protected set type(type: string | undefined) {
        if (!type) {
            this.visible = false;
            return;
        }

        const descriptor = AssetModelUtil.getAssetDescriptor(type);

        if (this.assetTypeAsIcon) {
            const icon = descriptor ? descriptor.icon : AssetType.THING.icon;
            this.icon = icon;
        }

        if (descriptor && descriptor.color) {
            this.markerColor = descriptor.color;
            this.updateColor(this.markerContainer);
        }
        this.visible = true;
    }

    protected shouldUpdate(_changedProperties: PropertyValues): boolean {
        if (_changedProperties.has("assetId")) {

            this.lat = undefined;
            this.lng = undefined;
            this.type = undefined;
            this.assetIds = this.assetId && this.assetId.length > 0 ? [this.assetId] : undefined;

            if (Object.keys(_changedProperties).length === 1) {
                return false;
            }
        }

        if (_changedProperties.has("asset")) {
            this.onAssetChanged(this.asset);
        }

        return super.shouldUpdate(_changedProperties);
    }

    public onAttributeEvent(event: AttributeEvent) {
        if (event.attributeState!.attributeRef!.attributeName === AttributeType.LOCATION.attributeName) {
            this._updateLocation(event.attributeState!.value as GeoJSONPoint);
        }
    }

    public onAssetEvent(event: AssetEvent) {
        switch (event.cause) {
            case AssetEventCause.READ:
            case AssetEventCause.CREATE:
            case AssetEventCause.UPDATE:
                this.onAssetChanged(event.asset);
                break;
            case AssetEventCause.DELETE:
                this.onAssetChanged(undefined);
                break;
        }
    }

    protected onAssetChanged(asset?: Asset) {
        if (asset) {
            const attr = Util.getAssetAttribute(asset, AttributeType.LOCATION.attributeName!);
            const showOnMapMeta = Util.getFirstMetaItem(attr, MetaItemType.SHOW_ON_DASHBOARD.urn!);
            if(!showOnMapMeta || !showOnMapMeta.value) return

            this._updateLocation(attr ? attr.value as GeoJSONPoint : null);
            this.type = asset.type;
        } else {
            this.lat = undefined;
            this.lng = undefined;
        }
    }

    protected _updateLocation(location: GeoJSONPoint | null) {
        this.lat = location && location.coordinates ? (location.coordinates as any)[1] : undefined;
        this.lng = location && location.coordinates ? (location.coordinates as any)[0] : undefined;
    }

    protected getColor() {
        if (this.markerColor && !this.color) {
            return "#" + this.markerColor;
        }
        return super.getColor();
    }

    protected getActiveColor() {
        if (this.markerColor && !this.activeColor) {
            return "#" + this.markerColor;
        }
        return super.getActiveColor();
    }
}
