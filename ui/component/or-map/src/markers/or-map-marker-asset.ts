import {customElement, property, PropertyValues} from "lit-element";
import {OrMapMarker} from "./or-map-marker";
import {AttributeEvent, Attribute, AttributeType, GeoJSONPoint, AssetType, AssetEvent, AssetEventCause} from "@openremote/model";
import {subscribe} from "@openremote/core/dist/asset-mixin";
import openremote, {AssetModelUtil} from "@openremote/core";

@customElement("or-map-marker-asset")
export class OrMapMarkerAsset extends subscribe(openremote)(OrMapMarker) {

    @property({type: String, reflect: true, attribute: true})
    public asset?: string;

    public assetTypeAsIcon: boolean = true;

    constructor() {
        super();
        this.visible = false;
    }

    protected set type(type: string | undefined) {
        if (!type) {
            this.visible = false;
            return;
        }

        if (this.assetTypeAsIcon) {
            const descriptor = AssetModelUtil.getAssetDescriptor(type);
            const icon = descriptor ? descriptor.icon : AssetType.THING.icon;
            this.icon = icon;
        }
        this.visible = true;
    }

    protected shouldUpdate(_changedProperties: PropertyValues): boolean {
        if (_changedProperties.has("asset")) {

            this.lat = undefined;
            this.lng = undefined;
            this.type = undefined;
            this.assetIds = this.asset && this.asset.length > 0 ? [this.asset] : undefined;

            if (Object.keys(_changedProperties).length === 1) {
                return false;
            }
        }
        return super.shouldUpdate(_changedProperties);
    }

    public onAttributeEvent(event: AttributeEvent) {
        if (event.attributeState!.attributeRef!.attributeName !== AttributeType.LOCATION.attributeName) {
            return;
        }

        this._updateLocation(event.attributeState!.value as GeoJSONPoint);

    }

    public onAssetEvent(event: AssetEvent) {
        switch (event.cause) {
            case AssetEventCause.READ:
                const attr = event.asset && event.asset.attributes && event.asset.attributes.hasOwnProperty("location") ? event.asset.attributes.location as Attribute : null;
                this._updateLocation(attr ? attr.value as GeoJSONPoint : null);
            case AssetEventCause.CREATE:
            case AssetEventCause.UPDATE:
                this.type = event.asset!.type;
                break;
            case AssetEventCause.DELETE:
                this.type = undefined;
                this.lat = undefined;
                this.lng = undefined;
                break;
        }
    }

    protected _updateLocation(location: GeoJSONPoint | null) {
        this.lat = location && location.coordinates ? (location.coordinates as any)[1] : undefined;
        this.lng = location && location.coordinates ? (location.coordinates as any)[0] : undefined;
    }
}
