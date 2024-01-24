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
    SharedEvent, ValueDatapoint,
    WellknownAttributes
} from "@openremote/model";
import manager, {subscribe, Util} from "@openremote/core";
import {getMarkerIconAndColorFromAssetType, OverrideConfigSettings} from "../util";

export type MapMarkerConfig = {
    attributeName: string;
    showLabel?: boolean;
    showUnits?: boolean;
    hideDirection?: boolean;
    colours?: MapMarkerHistoryColours;
}

export type MapMarkerHistoryColours = AttributeMarkerHistoryColours | RangeAttributeMarkerColours;

export type MapMarkerHistoryAssetConfig = {
    [assetType: string]: MapMarkerConfig
}

export type AttributeMarkerHistoryColours = {
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

export function getMarkerConfigForAssetType(config: MapMarkerHistoryAssetConfig | undefined, assetType: string | undefined): MapMarkerConfig | undefined {
    if (!config || !assetType || !config[assetType]) {
        return;
    }

    return config[assetType];
}

export function getMarkerConfigAttributeName(config: MapMarkerHistoryAssetConfig | undefined, assetType: string | undefined): string | undefined {
    const assetTypeConfig = getMarkerConfigForAssetType(config, assetType);

    if (!assetTypeConfig) {
        return;
    }

    return assetTypeConfig.attributeName;
}

@customElement("or-map-marker-location-history")
export class OrMapLocationHistoryMarker extends subscribe(manager)(OrMapMarker) {

    @property({type: String, reflect: true, attribute: true})
    public assetId?: string;

    @property({type: Object, attribute: true})
    public locationDatapoint?: ValueDatapoint<GeoJSONPoint>;
    //
    // @property({type: Object, attribute: true})
    // public previousDatapoint?: ValueDatapoint<GeoJSONPoint>;


    // @property()
    // public config?: MapMarkerAssetConfig;

    public assetTypeAsIcon: boolean = true;


    protected markerColor?: string;


    async connectedCallback() {
        super.connectedCallback();
        if(this.locationDatapoint == undefined) return;
        this._updateLocation(this.locationDatapoint!.y!)
        console.log("added " + this.locationDatapoint.x?.toString())
        this.requestUpdate();
    }

    //
    // protected set type(type: string | undefined) {
    //
    //     let overrideOpts: OverrideConfigSettings | undefined;
    //     const assetTypeConfig = getMarkerConfigForAssetType(this.config, type);
    //
    //     if (assetTypeConfig && assetTypeConfig.attributeName && this.asset && this.asset.attributes && this.asset.attributes[assetTypeConfig.attributeName] && assetTypeConfig.colours) {
    //         const currentValue = this.asset.attributes[assetTypeConfig.attributeName].value;
    //         overrideOpts = {
    //             markerConfig: assetTypeConfig.colours,
    //             currentValue: currentValue
    //         };
    //     }
    //
    //     const iconAndColour = getMarkerIconAndColorFromAssetType(type, overrideOpts);
    //
    //     if (!iconAndColour) {
    //         this.visible = false;
    //         return;
    //     }
    //
    //     if (this.assetTypeAsIcon) {
    //         this.icon = iconAndColour.icon;
    //     }
    //
    //     this.markerColor = (Array.isArray(iconAndColour.color)) ? iconAndColour.color[0].colour : iconAndColour.color || undefined;
    //     this.updateColor(this.markerContainer);
    //     this.visible = true;
    // }

    protected _updateLocation(location: GeoJSONPoint | null) {
        // console.log(location)
        this.lat = location && location.coordinates ? (location.coordinates as any)[0] : undefined;
        this.lng = location && location.coordinates ? (location.coordinates as any)[1] : undefined;
        this.icon = "mdiHistory";
        this.interactive = true;
        this.visible = true;
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
