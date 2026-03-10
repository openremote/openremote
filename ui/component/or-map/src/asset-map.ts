import { GeoJSONFeature, GeoJSONSource, MapSourceDataEvent, Marker } from "maplibre-gl";
import { Feature, FeatureCollection, Point } from "geojson";
import { AssetWithLocation, ClusterConfig } from "./types";
import { MapWidget } from "./mapwidget";
import { OrClusterMarker, Slice } from "./markers/or-cluster-marker";
import { getMarkerIconAndColorFromAssetType } from "./util";
import { OrMapLoadedEvent, OrMapMarkersChangedEvent } from ".";

type MissingAsset = AssetWithLocation & { id: string; type: string };

/**
 * @todo consider using `this._map.setGlobalStateProperty`
 * @todo look at https://maplibre.org/maplibre-style-spec/expressions/#accumulated
 */
export class AssetMap extends MapWidget {
    protected _assets: Record<string, AssetWithLocation> = {};
    protected _assetTypeColors: any = {};
    protected _assetsOnScreen: Record<string, AssetWithLocation> = {};
    protected _cachedClusters: Record<string, Marker> = {};
    protected _clustersOnScreen: Record<string, Marker> = {};
    protected _clusterConfig?: ClusterConfig;
    protected _source?: GeoJSONSource;

    protected _onMove = () => this._updateMarkers();
    protected _onMoveEnd = (e: any) => {
        // Ensure marker updates happen after the frame
        requestAnimationFrame(() => {
            // On WebKit browsers the clicked marker may not be removed,
            // if the camera zoom level is directly between 2 levels
            if (e.marker instanceof OrClusterMarker) {
                this._clustersOnScreen[e.marker._clusterId]?.remove();
            }
            this._updateMarkers();
        });
    };

    protected _onData = (e: MapSourceDataEvent) => {
        if (this._map && e.isSourceLoaded && e.sourceId === "assets") {
            this._map.on("move", this._onMove);
            this._map.on("moveend", this._onMoveEnd);
            this._updateMarkers();
        }
    };

    constructor(
        styleParent: Node,
        mapContainer: HTMLElement,
        showGeoCodingControl = false,
        showBoundaryBox = false,
        useZoomControls = true,
        showGeoJson = true,
        clusterConfig?: ClusterConfig
    ) {
        super(styleParent, mapContainer, showGeoCodingControl, showBoundaryBox, useZoomControls, showGeoJson);
        this._clusterConfig = clusterConfig;
    }

    override async build(): Promise<void> {
        return super.build().then(() => {
            this._map?.on("load", async () => await this.load());
        });
    }

    /**
     * Add an asset as (asset) feature to the map.
     *
     * Use {@link addAssets} instead if adding multiple assets.
     * @param asset The asset to add
     */
    public async addAsset(asset: AssetWithLocation) {
        if (!this._source) return;
        const { features } = (await this._source?.getData()) as FeatureCollection<Point>;

        if (!this._hasRequired(asset) || this._isMissing(asset, features)) {
            return;
        }

        if (!(asset.type in this._assetTypeColors)) {
            this._assetTypeColors[asset.type] = getMarkerIconAndColorFromAssetType(asset.type)?.color;
        }

        this._assets[asset.id] = asset;
        this._source?.updateData({ add: [AssetMap._assetToFeature(asset)] });
    }

    /**
     * Add multiple assets as (asset) feature to the map. Optimized for adding mutliple assets at once.
     * @param assets The assets to add
     */
    public async addAssets(assets: AssetWithLocation[]) {
        if (!this._source) return;
        const { features } = (await this._source?.getData()) as FeatureCollection<Point>;

        let missing = assets.filter(this._hasRequired);
        if (features?.length) {
            missing = missing.filter((a) => this._isMissing(a, features));
        }

        if (!missing.length) {
            return;
        }

        for (const asset of missing) {
            if (!(asset.type in this._assetTypeColors)) {
                this._assetTypeColors[asset.type] = getMarkerIconAndColorFromAssetType(asset.type)?.color;
            }
            this._assets[asset.id] = asset;
        }
        this._source.workerOptions.clusterProperties = Object.fromEntries(
            Object.keys(this._assetTypeColors).map((t) => AssetMap._getClusterPropertyExpression("assetType", t))
        );

        // console.log(this._assetTypeColors, missing.map(AssetMap._assetToFeature));
        this._source.updateData({
            add: missing.map(AssetMap._assetToFeature),
        });
    }

    public clearAssets() {
        this._source?.updateData({ removeAll: true });
    }

    public updateAttribute(id: string, value: any) {
        this._source?.updateData({ update: [{ id, newGeometry: value }] });
        // console.log(id, value);
    }

    /**
     * Initialize asset sources, layers and data events
     */
    private async load() {
        if (!this._map) {
            console.warn("MapLibre Map not initialized!");
            return;
        }

        if (this._loaded) {
            return;
        }

        if (this._map.getSource("assets")) {
            if (this._map.getLayer("unclustered-point")) {
                this._map.removeLayer("unclustered-point");
            }
            if (this._map.getLayer("clusters")) {
                this._map.removeLayer("clusters");
            }
            if (this._map.getLayer("cluster-count")) {
                this._map.removeLayer("cluster-count");
            }
            this._map.removeSource("assets");
        }

        this._map.addSource("assets", {
            type: "geojson",
            cluster: this._clusterConfig?.cluster ?? true,
            clusterRadius: this._clusterConfig?.clusterRadius ?? 180,
            clusterMaxZoom: this._clusterConfig?.clusterMaxZoom ?? 17,
            clusterProperties: Object.fromEntries(
                Object.keys(this._assetTypeColors).map((t) => [
                    t,
                    ["+", ["case", ["==", ["get", "assetType"], t], 1, 0]],
                ])
            ),
            promoteId: "id", // Promote the id property as feature id
            data: { type: "FeatureCollection", features: [] },
        });

        if (!this._map.getLayer("unclustered-point")) {
            this._map.addLayer({
                id: "unclustered-point",
                type: "circle",
                source: "assets",
                filter: ["!", ["has", "point_count"]],
                // paint: { "circle-radius": 0 },
            });
        }

        this._source = this._map.getSource("assets") as GeoJSONSource;

        this._map.on("data", this._onData);

        console.log("Map loaded");
        this._mapContainer.dispatchEvent(new OrMapLoadedEvent());
        this._loaded = true;
    }

    protected _updateMarkers() {
        if (!this._map) return;

        const features = this._map.querySourceFeatures("assets");
        const newAssets = this._updateAssets(features);
        this._updateClusters(features);
        this._mapContainer.dispatchEvent(new OrMapMarkersChangedEvent(newAssets));
    }

    private _updateAssets(features: GeoJSONFeature[]) {
        const newAssets: Record<string, AssetWithLocation> = {};

        // const d = features.reduceRight((p: GeoJSONFeature[], c) => !p.find(f => f.properties.id === c.properties.id) ? [...p, c] : p, []);
        // console.log(d)
        for (const feature of features) {
            if (!feature.properties.id) continue;
            const id = feature.properties.id as string;
            // const [lon, lat] = (feature.geometry as Point).coordinates;
            // // console.log((feature.geometry as Point).coordinates)
            // // console.log(this._markers.get(id))
            // // console.log(this._markers.get(id)?._element)
            // // this._markers.get(id)?.setLngLat(geometry.coordinates as LngLatLike)

            // const marker = this._markers.get(id);
            // // console.log("UPDATE MARKER", marker instanceof OrMapMarker, marker?._element instanceof OrMapMarker)
            // if (marker instanceof OrMapMarker && marker.hasPosition()) {
            //     if (marker._actualMarkerElement) {
            //         marker.lng = lon;
            //         marker.lat = lat;
            //         marker.requestUpdate();
            //         this._updateMarkerPosition(marker); // Prefer geometry position?
            //     }
            // }
            newAssets[id] = this._assets[id];
        }

        for (const id in this._assetsOnScreen) {
            if (!newAssets[id]) delete this._assetsOnScreen[id];
        }

        this._assetsOnScreen = newAssets;
        return Object.values(this._assetsOnScreen);
    }

    private _updateClusters(features: GeoJSONFeature[]) {
        const newClusters: Record<string, Marker> = {};

        // Create missing cluster markers
        for (const feature of features) {
            if (!feature.properties.cluster) continue;
            const id: number = feature.properties.cluster_id;
            const geometry = feature.geometry as Point;
            const [lng, lat] = geometry.coordinates;

            let marker = this._cachedClusters[id];
            if (!marker) {
                const slices: Slice[] = Object.entries(feature.properties)
                    .filter(([k]) => k in this._assetTypeColors)
                    .map(([type, count]) => [type, this._assetTypeColors[type], count]);

                marker = this._cachedClusters[id] = new Marker({
                    element: new OrClusterMarker(slices, id, lng, lat, this._map!),
                }).setLngLat([lng, lat]);
            }
            newClusters[id] = marker;

            if (!this._clustersOnScreen[id]) marker.addTo(this._map!);
        }

        // Cleanup clusters that should no longer be on screen
        for (const id in this._clustersOnScreen) {
            const marker = newClusters[id];
            if (!marker || this._hasAllAssetTypes(marker._element)) {
                this._clustersOnScreen[id]?.remove();
            }
        }

        this._clustersOnScreen = newClusters;
    }

    private _hasAllAssetTypes(element: HTMLElement): boolean {
        return element instanceof OrClusterMarker && !element.hasTypes(Object.keys(this._assetTypeColors));
    }

    private _hasRequired(asset: AssetWithLocation): asset is MissingAsset {
        return Boolean(asset.id && asset.type);
    }

    private _isMissing(asset: MissingAsset, features: Feature[]) {
        return !features?.some((f) => f.properties?.id === asset.id);
    }

    private _createClusterGeoJSON() {
        const clusterProperties = Object.fromEntries(
            Object.keys(this._assetTypeColors).map((t) => AssetMap._getClusterPropertyExpression("assetType", t))
        );
        return {
            type: "geojson",
            cluster: this._clusterConfig?.cluster ?? true,
            clusterRadius: this._clusterConfig?.clusterRadius ?? 180,
            clusterMaxZoom: this._clusterConfig?.clusterMaxZoom ?? 17,
            clusterProperties,
            promoteId: "id", // Promote the id property as feature id
            data: { type: "FeatureCollection", features: [] },
        };
    }

    private static _getClusterPropertyExpression(key: string, value: string) {
        return [value, ["+", ["case", ["==", ["get", key], value], 1, 0]]];
    }

    private static _assetToFeature({ id, type: assetType, name, attributes }: MissingAsset): Feature {
        return {
            type: "Feature",
            geometry: attributes.location.value,
            properties: { id, name, assetType },
        };
    }
}
