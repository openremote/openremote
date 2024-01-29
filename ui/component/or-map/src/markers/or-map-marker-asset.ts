import {PropertyValues} from "lit";
import {customElement, property} from "lit/decorators.js";
import {OrMapMarker} from "./or-map-marker";
import {
    Asset,
    AssetEvent,
    AssetEventCause,
    AssetModelUtil,
    AttributeEvent,
    GeoJSONPoint,
    SharedEvent,
    WellknownAttributes
} from "@openremote/model";
import manager, {subscribe, Util} from "@openremote/core";
import {getMarkerIconAndColorFromAssetType, OverrideConfigSettings} from "../util";

export type MapMarkerConfig = {
    attributeName: string;
    showLabel?: boolean;
    showUnits?: boolean;
    hideDirection?: boolean;
    colours?: MapMarkerColours;
}

export type MapMarkerColours = AttributeMarkerColours | RangeAttributeMarkerColours;

export type MapMarkerAssetConfig = {
    [assetType: string]: MapMarkerConfig
}

export type AttributeMarkerColours = {
    type: "string" | "boolean";
    [value: string]: string;
}

export type RangeAttributeMarkerColours = {
    type: "range";
    ranges: AttributeMarkerColoursRange[];
}

export type AttributeMarkerColoursRange = {
    min: number;
    colour: string;
}

export function getMarkerConfigForAssetType(config: MapMarkerAssetConfig | undefined, assetType: string | undefined): MapMarkerConfig | undefined {
    if (!config || !assetType || !config[assetType]) {
        return;
    }

    return config[assetType];
}

export function getMarkerConfigAttributeName(config: MapMarkerAssetConfig | undefined, assetType: string | undefined): string | undefined {
    const assetTypeConfig = getMarkerConfigForAssetType(config, assetType);

    if (!assetTypeConfig) {
        return;
    }

    return assetTypeConfig.attributeName;
}

@customElement("or-map-marker-asset")
export class OrMapMarkerAsset extends subscribe(manager)(OrMapMarker) {

    @property({type: String, reflect: true, attribute: true})
    public assetId?: string;

    @property({type: Object, attribute: true})
    public asset?: Asset;

    @property()
    public config?: MapMarkerAssetConfig;

    public assetTypeAsIcon: boolean = true;

    constructor() {
        super();
        this.visible = false;
    }

    protected markerColor?: string;

    protected set type(type: string | undefined) {

        let overrideOpts: OverrideConfigSettings | undefined;
        const assetTypeConfig = getMarkerConfigForAssetType(this.config, type);

        if (assetTypeConfig && assetTypeConfig.attributeName && this.asset && this.asset.attributes && this.asset.attributes[assetTypeConfig.attributeName] && assetTypeConfig.colours) {
            const currentValue = this.asset.attributes[assetTypeConfig.attributeName].value;
            overrideOpts = {
                markerConfig: assetTypeConfig.colours,
                currentValue: currentValue
            };
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
            this.direction = undefined;
            this.displayValue = undefined;
            this.assetIds = this.assetId && this.assetId.length > 0 ? [this.assetId] : undefined;

            if (Object.keys(_changedProperties).length === 1) {
                return false;
            }
        }

        if (_changedProperties.has("asset")) {
            try {
                this.onAssetChanged(this.asset);
            } catch (e) {
                console.error(e);
            }
        }

        return super.shouldUpdate(_changedProperties);
    }

    /**
     * This will only get called when assetId is set; if asset is set then it is expected that attribute changes are
     * handled outside this component and the asset should be replaced when attributes change that require the marker
     * to re-render
     */
    public _onEvent(event: SharedEvent) {
        if (event.eventType === "attribute") {
            const attributeEvent = event as AttributeEvent;

            if (attributeEvent.ref!.name === WellknownAttributes.LOCATION) {
                this._updateLocation(attributeEvent.value as GeoJSONPoint);
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
            this.direction = undefined;
            this.displayValue = undefined;

            const locAttr = asset.attributes ? asset.attributes[WellknownAttributes.LOCATION] : undefined;
            this._updateLocation(locAttr ? locAttr.value as GeoJSONPoint : null);

            const assetTypeConfig = getMarkerConfigForAssetType(this.config, asset.type);
            const showDirection = !assetTypeConfig || !assetTypeConfig.hideDirection;
            const showLabel = assetTypeConfig && assetTypeConfig.showLabel === true && !!assetTypeConfig.attributeName;
            const showUnits = !!(assetTypeConfig && assetTypeConfig.showUnits !== false);

            if (showLabel && asset.attributes && asset.attributes[assetTypeConfig?.attributeName]) {
                const attr = asset.attributes[assetTypeConfig.attributeName];
                const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attr.name, attr);
                this.displayValue = Util.getAttributeValueAsString(attr, descriptors[0], asset.type, showUnits, "-");
            }

            if (showDirection) {
                if (asset.attributes && asset.attributes[WellknownAttributes.DIRECTION]) {
                    const directionVal = asset.attributes[WellknownAttributes.DIRECTION].value as number;
                    if (directionVal !== undefined && directionVal !== null) {
                        this.direction = directionVal.toString();
                    }
                }
            }

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
