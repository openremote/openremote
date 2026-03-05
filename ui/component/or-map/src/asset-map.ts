import { GeoJSONFeature, GeoJSONSource, LngLatLike, MapSourceDataEvent, Marker } from "maplibre-gl";
import { FeatureCollection, Point } from "geojson";
import { AssetWithLocation, ClusterConfig } from "./types";
import { MapWidget } from "./mapwidget";
import { OrClusterMarker, Slice } from "./markers/or-cluster-marker";
import { getMarkerIconAndColorFromAssetType } from "./util";
import { OrMapMarkersChangedEvent } from ".";

export class AssetMap extends MapWidget {
    protected _assets: Record<string, AssetWithLocation> = {};
    protected _assetCollection: FeatureCollection = { type: "FeatureCollection", features: [] }; // REMOVE
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

    public addAsset(asset: AssetWithLocation) {
        const { type: assetType, name, id, attributes } = asset;
        const coordinates = attributes!.location.value!.coordinates!;

        if (id && assetType && !(id in this._assets)) {
            this._assets[id] = asset;
            this._assetTypeColors[assetType] = getMarkerIconAndColorFromAssetType(assetType)?.color;
            this._assetCollection.features.push({
                type: "Feature",
                geometry: { type: "Point", coordinates },
                properties: { id, name, assetType },
            });
        }
    }

    // public clearAsset() {
    //     this._assetCollection.features = [];
    // }

    public updateAttribute(id: string, value: any) {
        this._source?.updateData({ update: [{ id, newGeometry: value }] });
        console.log(id, value);
    }

    /**
     * Initialize asset sources, layers and data events
     */
    private async load() {
        if (!this._map || !this._loaded) {
            console.warn("MapLibre Map not initialized!");
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
            data: this._assetCollection,
        });

        if (!this._map.getLayer("unclustered-point")) {
            this._map.addLayer({
                id: "unclustered-point",
                type: "circle",
                source: "assets",
                filter: ["!", ["has", "point_count"]],
                paint: { "circle-radius": 0 },
            });
        }

        this._source = this._map.getSource("assets") as GeoJSONSource;

        this._map.on("data", this._onData);
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

        for (const feature of features) {
            if (!feature.properties.id) continue;
            const id = feature.properties.id as string;
            const geometry = feature.geometry as Point;
            this._markers.get(id)?.setLngLat(geometry.coordinates as LngLatLike)
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
}
