import {PropertyValues} from "lit";
import {customElement, property} from "lit/decorators.js";
import {OrMapMarker} from "./or-map-marker";
import {
    AttributeEvent,
    GeoJSONPoint,
    AssetEvent,
    AssetEventCause,
    Asset,
    SharedEvent,
    WellknownAttributes, ReadAttributeEvent,
} from "@openremote/model";
import {MapMarkerConfig, subscribe, Util} from "@openremote/core";
import manager from "@openremote/core";
import { getMarkerIconAndColorFromAssetType } from "../util";

@customElement("or-map-marker-asset")
export class OrMapMarkerAsset extends subscribe(manager)(OrMapMarker) {

    @property({type: String, reflect: true, attribute: true})
    public assetId?: string;

    @property({type: Object, attribute: true})
    public asset?: Asset;

    @property()
    public config?: MapMarkerConfig;

    public assetTypeAsIcon: boolean = true;

    constructor() {
        super();
        this.visible = false;
    }

    protected markerColor?: string;

    protected set type(type: string | undefined) {
        const iconAndColor = getMarkerIconAndColorFromAssetType(type, this.config, this.displayValue);

        if (!iconAndColor) {
            this.visible = false;
            return;
        }

        if (this.assetTypeAsIcon) {
            this.icon = iconAndColor.icon;
        }

        this.markerColor = (Array.isArray(iconAndColor.color)) ? iconAndColor.color[0].colour : iconAndColor.color || undefined;
        this.updateColor(this.markerContainer);
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

    public _onEvent(event: SharedEvent) {
        if (event.eventType === "attribute") {
            const attributeEvent = event as AttributeEvent;

            if (attributeEvent.attributeState!.ref!.name === WellknownAttributes.LOCATION) {
                this._updateLocation(attributeEvent.attributeState!.value as GeoJSONPoint);
                return;
            }

            if (this.asset) {
                this.asset = Util.updateAsset(this.asset, event as AttributeEvent);
                this.requestUpdate();
            }

            return;
        }

        if (event.eventType === "asset") {
            const assetEvent = event as AssetEvent;

            switch (assetEvent.cause) {
                case AssetEventCause.READ:
                case AssetEventCause.CREATE:
                case AssetEventCause.UPDATE:
                    this.onAssetChanged(assetEvent.asset);
                    break;
                case AssetEventCause.DELETE:
                    this.onAssetChanged(undefined);
                    break;
            }
        }
    }

    protected async onAssetChanged(asset?: Asset) {
        if (asset) {
            const attr = asset.attributes ? asset.attributes[WellknownAttributes.LOCATION] : undefined;
            this._updateLocation(attr ? attr.value as GeoJSONPoint : null);

            if (this.config && this.config[asset.type!]) {
                const attributeName = Object.keys(this.config[asset.type!])[0];
                this.displayValue = await this.getDesiredAttrValue(asset, attributeName);
                console.log('dpv', this.displayValue);
            }
            
            this.type = asset.type;
        } else {
            this.lat = undefined;
            this.lng = undefined;
        }
    }
    
    protected async getDesiredAttrValue(asset: Asset, attributeName: string): Promise<string | number | boolean | undefined> {
        const currentValue: AttributeEvent = await manager.events!.sendEventWithReply({
            event: {
                eventType: "read-asset-attribute",
                ref: {
                    id: asset.id,
                    name: attributeName
                }
            } as ReadAttributeEvent
        });
        return currentValue.attributeState?.value || undefined;
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
