import {customElement, property, PropertyValues} from "lit-element";
import {OrMapMarker} from "./or-map-marker";
import rest from "@openremote/rest";
import {Asset, AssetAttribute, GeoJSONPoint} from "@openremote/model";
import {GenericAxiosResponse} from "axios";

@customElement("or-map-marker-asset")
export class OrMapMarkerAsset extends OrMapMarker {

    @property({type: String})
    public asset?: string;

    constructor() {
        super();
    }

    public connectedCallback(): void {
        super.connectedCallback();
    }

    public disconnectedCallback(): void {
        super.disconnectedCallback();
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        if (_changedProperties.has("asset")) {
            this.refreshMarker();
        }
    }

    protected shouldUpdate(_changedProperties: PropertyValues): boolean {
        if (_changedProperties.has("asset")) {
            this.refreshMarker();

            if (Object.keys(_changedProperties).length === 1) {
                return false;
            }
        }
        return super.shouldUpdate(_changedProperties);
    }

    protected refreshMarker() {

        if (this.asset && this.asset.length > 0) {
            this.getAsset().then((asset) => {
                const attrs = asset.attributes as any;
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

                // Model d.ts is clearly not perfect need to sort out Jackson annotations
                this.lat = (location.coordinates as any)[1] || 0;
                this.lng = (location.coordinates as any)[0] || 0;
                if (asset.type) {
                    // TODO: Get hold of icon from asset type
                    this.icon = "office-building";
                } else {
                    this.icon = undefined;
                }
                this.visible = true;
            }).catch(() => {
                this.visible = false;
            });
        } else {
            this.visible = false;
        }
    }

    protected async getAsset(): Promise<Asset> {
        const response: GenericAxiosResponse<Asset> = await rest.api.AssetResource.get(this.asset!);
        return response.data;
    }
}
