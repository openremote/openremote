import {html} from "@polymer/polymer";
import {customElement, property, observe} from "@polymer/decorators";
import {OrMapMarker} from "./or-map-marker";
import rest from "@openremote/rest";
import {Asset, AssetAttribute, GeoJSONPoint} from "@openremote/model";
import {GenericAxiosResponse} from "axios";

/**
 * `or-map-marker-asset`
 * Asset linked map marker
 */

@customElement('or-map-marker-asset')
export class OrMapMarkerAsset extends OrMapMarker {

    @property({type: String, observer: "_assetChanged"})
    asset?: string;

    constructor() {
        super();
    }

    _createMarkerElement(): HTMLElement {
        return document.createElement("div");
    }

    refreshMarker() {
        this.getAsset().then((asset) => {
            const attrs: Array<AssetAttribute> = <Array<AssetAttribute>>asset.attributes;
            const attr: AssetAttribute | undefined = attrs.find(attr => {
                return attr.name === "location"
            });

            if (!attr) {
                this.visible = false;
                return;
            }

            const location: GeoJSONPoint | null = attr.valueAsObject as GeoJSONPoint;

            if (!location) {
                this.visible = false;
                return;
            }

            this._createAssetMarker();
            this.lat = location.y || 0;
            this.lng = location.x || 0;
            this.visible = true;
        }).catch(() => {
            this.visible = false;
        });
    }

    async getAsset(): Promise<Asset> {
        const response: GenericAxiosResponse<Asset> = await rest.api.AssetResource.get(this.asset!);
        return response.data;
    }

    _assetChanged() {
        this.visible = false;

        if (this.asset) {
            this.refreshMarker();
        }
    }

    _elementChanged() {
        this._updateMarker();
    }

    private _createAssetMarker() {
        this._ele = document.createElement("div");
        this._ele.className = "marker";
    }
}

