import {PropertyValues} from "lit";
import {customElement, property} from "lit/decorators.js";
import {OrMapMarker} from "./or-map-marker";
import {
    Asset,
    AssetEvent,
    AssetEventCause,
    AttributeEvent,
    GeoJSONPoint,
    ReadAttributeEvent,
    SharedEvent,
    WellknownAttributes,
    AssetModelUtil
} from "@openremote/model";
import manager, {subscribe, Util} from "@openremote/core";
import { getMarkerIconAndColorFromAssetType } from "../util";

export type BaseMapMarkerConfig = {
    attributeName: string;
    showLabel?: boolean;
    showUnits?: boolean;
    showDirection?: boolean;
}

export type MapMarkerConfig = {
    [assetType: string]: AttributeMarkerColours[] | RangeAttributeMarkerColours[];
}

export type AttributeMarkerColours = BaseMapMarkerConfig & {
    type: "string" | "boolean";
    [value: string]: string;
}

export type RangeAttributeMarkerColours = BaseMapMarkerConfig & {
    type: "range";
    ranges: AttributeMarkerColoursRange[];
}

export type AttributeMarkerColoursRange = {
    max: number;
    colour: string;
}

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

        let overrideOpts;
        if (this.config && type && this.config[type]) {
            const markerConfig = this.config[type][0] || undefined;

            if (this.displayValue) {
                overrideOpts = {markerConfig: markerConfig, attributeValue: this.displayValue};
            }
        }

        const iconAndColour = getMarkerIconAndColorFromAssetType(type, overrideOpts);

        if (!iconAndColour) {
            this.visible = false;
            return;
        }

        if (this.assetTypeAsIcon) {
            this.icon = iconAndColour.icon;
        }

        this.markerColor = (Array.isArray(iconAndColour.color)) ? iconAndColour.color[0].colour : iconAndColour.color || undefined;
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

            if (asset.attributes && asset.attributes[WellknownAttributes.DIRECTION]) {
                this.direction = (await this.getAttrValue(asset, WellknownAttributes.DIRECTION))!.toString() || undefined;
            }

            if (this.config && asset.type && this.config[asset.type]) {
                const assetTypeConfig = this.config[asset.type][0] || undefined;

                if (assetTypeConfig && assetTypeConfig.showLabel) {
                    if (assetTypeConfig.showLabel) {
                        const attrVal = await this.getAttrValue(asset, assetTypeConfig.attributeName);
                        if (attrVal === undefined) return;

                        this.displayValue = attrVal.toString();

                        if (assetTypeConfig.showUnits !== false && attr) {
                            const attributeDescriptor = AssetModelUtil.getAttributeDescriptor(assetTypeConfig.attributeName, asset.type);
                            const unit = Util.resolveUnits(Util.getAttributeUnits(attr, attributeDescriptor, asset.type));
                            this.displayValue = `${this.displayValue} ${unit}`;
                        }
                    }
                }

                if (assetTypeConfig && assetTypeConfig.showDirection === false) {
                    this.direction = undefined;
                }
            }

            this.type = asset.type;
        } else {
            this.lat = undefined;
            this.lng = undefined;
        }
    }
    
    protected async getAttrValue(asset: Asset, attributeName: string): Promise<string | number | boolean | undefined> {
        const currentValue: AttributeEvent = await manager.events!.sendEventWithReply({
            event: {
                eventType: "read-asset-attribute",
                ref: {
                    id: asset.id,
                    name: attributeName
                }
            } as ReadAttributeEvent
        });
        return currentValue.attributeState?.value;
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
