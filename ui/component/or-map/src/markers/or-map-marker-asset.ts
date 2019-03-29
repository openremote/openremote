import {customElement, property} from "lit-element";
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

    @property({type: String})
    asset?: string;

    @property({type: Object})
    _ele!: HTMLElement;

    constructor() {
        super();
    }

    _getMarkerElement(): HTMLElement {
        let className = ("or-map-marker " + this.className).trim();
        let ele = document.createElement("div");
        ele.className = className;
        return ele;
    }

    refreshMarker() {
        this.getAsset().then((asset) => {
            const attrs = <any>asset.attributes;
            const attr: AssetAttribute | undefined = attrs.location;

            if (!attr) {
                this.visible = false;
                return;
            }

            const location: GeoJSONPoint | null = attr.value as GeoJSONPoint;

            if (!location) {
                this.visible = false;
                return;
            }
            this._createAssetMarker();
            // Model d.ts is clearly not perfect need to sort out Jackson annotations
            this.lat = (<any>location.coordinates)[1] || 0;
            this.lng = (<any>location.coordinates)[0] || 0;
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

    private _createAssetMarker() {
        // Create basic element of _ele is undefined
        if(typeof this._ele === 'undefined') {
            this._ele = document.createElement("div");
            this._ele.className = "marker";
        }
    }
}

