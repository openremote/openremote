import { CircleLayerSpecification, GeoJSONFeature, GeoJSONSource, MapSourceDataEvent, Marker } from "maplibre-gl";
import { Feature, Point } from "geojson";
import { AssetWithLocation, ClusterConfig } from "./types";
import { BaseMap } from "./base-map";
import { OrClusterMarker, Slice } from "./markers/or-cluster-marker";
import { getMarkerIconAndColorFromAssetType } from "./util";
import { OrMapLoadedEvent, OrMapMarkersChangedEvent } from ".";
import { Util } from "@openremote/core";
import { AttributeEvent, WellknownAttributes } from "@openremote/model";

type IdentifiableAsset = AssetWithLocation & { id: string; type: string };

/**
 * This class handles where to display asset markers and clusters on the map.
 *
 * Currently, the standard flow to display assets as markers on the map goes as follows:
 *
 * 1. The map page adds assets to the {@link AssetMap}, which computes assets in view and then dispatches {@link OrMapMarkersChangedEvent}.
 * 2. The map page receives the event, renders the received assets as {@link OrMapMarkerAsset} by including them inside the map element's HTML body.
 * 3. The map component then receives either an event from the {@link OrMapMarkerAsset},
 * or when the map component sees a slotted {@link OrMapMarker} was added. From there the {@link OrMapMarker}s are passed to the {@link BaseMap} to be assigned to a MapLibre marker.
 *
 * @todo consider using `this._map.setGlobalStateProperty` for static cluster property
 */
export class AssetMap extends BaseMap {
    private static _clusterProperty = "assetType";

    protected _assets: Record<string, AssetWithLocation | null> = {};
    protected _assetTypeColors: Record<string, string> = {};
    protected _assetsOnScreen: Record<string, AssetWithLocation> = {};
    protected _cachedClusters: Record<string, Marker> = {};
    protected _clustersOnScreen: Record<string, Marker> = {};
    protected _clusterConfig?: ClusterConfig;
    protected _source?: GeoJSONSource;

    protected _onMove = () => this._updateMarkers();
    protected _onMoveEnd = (e: any) => {
        // Ensure marker updates happen after the frame
        requestAnimationFrame(() => {
            // The clicked marker may not be removed,
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
    public addAsset(asset: AssetWithLocation) {
        if (!this._source) return;
        const features = this._map!.querySourceFeatures("assets");

        if (!this._hasRequired(asset) || !this._isMissing(asset, features)) {
            return;
        }

        if (!(asset.type in this._assetTypeColors)) {
            this._assetTypeColors[asset.type] = getMarkerIconAndColorFromAssetType(asset.type)?.color as string;
            this._source.workerOptions.clusterProperties = this._getClusterProperties();
        }

        this._assets[asset.id] = asset;
        this._source.updateData({ add: [AssetMap._assetToFeature(asset)] });
    }

    /**
     * Add multiple assets as (asset) features to the map. Optimized for adding multiple assets at once.
     * @param assets The assets to add
     */
    public addAssets(assets: AssetWithLocation[]) {
        if (!this._source) return;
        const features = this._map!.querySourceFeatures("assets");

        let missing = assets.filter(this._hasRequired);
        if (features?.length) {
            missing = missing.filter((a) => this._isMissing(a, features));
        }

        if (!missing.length) {
            return;
        }

        for (const asset of missing) {
            if (!(asset.type in this._assetTypeColors)) {
                this._assetTypeColors[asset.type] = getMarkerIconAndColorFromAssetType(asset.type)?.color as string;
            }
            this._assets[asset.id] = asset;
        }
        this._source.workerOptions.clusterProperties = this._getClusterProperties();

        this._source.updateData({
            add: missing.map(AssetMap._assetToFeature),
        });
    }

    public updateAttribute(event: AttributeEvent) {
        const id = event?.ref?.id;

        if (!this._source || !id || !this._assets[id]) return;

        if (event.value == null && event.ref?.name === WellknownAttributes.LOCATION) {
            this._assets[id] = null;
            this._source?.updateData({ remove: [id] });
            return;
        }

        const asset = (this._assets[id] = Util.updateAsset(structuredClone(this._assets[id]), event));
        const newGeometry = asset.attributes.location.value;
        const newProperties = Object.entries(AssetMap._assetToFeature(asset as IdentifiableAsset).properties).map(
            ([key, value]) => ({ key, value })
        );
        this._source?.updateData({ update: [{ id, newGeometry, addOrUpdateProperties: newProperties }] });
    }

    public removeAssets(ids: string[]) {
        for (const id of ids) this._assets[id] = null;
        this._source?.updateData({ remove: ids });
    }

    public removeAllAssets() {
        this._assets = {};
        this._source?.updateData({ removeAll: true });
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
            if (this._map.getLayer("asset")) {
                this._map.removeLayer("asset");
            }
            if (this._map.getLayer("cluster")) {
                this._map.removeLayer("cluster");
            }
            this._map.removeSource("assets");
        }

        const baseLayer: Omit<CircleLayerSpecification, "id"> = {
            type: "circle",
            source: "assets",
            paint: { "circle-radius": 0 },
        };

        this._map
            .addSource("assets", {
                type: "geojson",
                cluster: this._clusterConfig?.cluster ?? true,
                clusterRadius: this._clusterConfig?.clusterRadius ?? 180,
                clusterMaxZoom: this._clusterConfig?.clusterMaxZoom ?? 17,
                clusterProperties: this._getClusterProperties(),
                promoteId: "id", // Promote the id property as feature id
                data: { type: "FeatureCollection", features: [] }, // TODO: consider preloading data to avoid tile cache misses
            })
            .addLayer({ ...baseLayer, id: "cluster", filter: ["has", "point_count"] })
            .addLayer({ ...baseLayer, id: "asset", filter: ["!", ["has", "point_count"]] });

        this._source = this._map.getSource("assets") as GeoJSONSource;

        this._map.on("data", this._onData);

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

        for (const feature of features) {
            if (!feature.properties.id) continue;
            const id = feature.properties.id as string;
            if (this._assets[id]) newAssets[id] = this._assets[id];
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
            let marker = this._cachedClusters[id] ?? this._getClusterMarker(feature);
            // Invalidate the cached cluster marker if the type counts don't match
            // If a new type is added, the marker will have a different cluster_id
            if (marker._element instanceof OrClusterMarker && !marker._element.slicesMatch(feature.properties)) {
                marker.remove();
                marker = this._getClusterMarker(feature).addTo(this._map!);
            }
            newClusters[id] = marker;
            // Add the new cluster marker if it's not already on screen
            if (!this._clustersOnScreen[id]) marker.addTo(this._map!);
        }

        // Cleanup clusters that should no longer be on screen
        for (const id in this._clustersOnScreen) {
            if (!newClusters[id]) this._clustersOnScreen[id]?.remove();
        }
        this._clustersOnScreen = newClusters;
    }

    private _getClusterMarker(feature: GeoJSONFeature) {
        const id: number = feature.properties.cluster_id;
        const geometry = feature.geometry as Point;
        const [lng, lat] = geometry.coordinates;

        const slices: Slice[] = Object.entries(feature.properties)
            .filter(([k]) => k in this._assetTypeColors)
            .map(([type, count]) => [type, this._assetTypeColors[type], count]);

        return (this._cachedClusters[id] = new Marker({
            element: new OrClusterMarker(slices, id, lng, lat, this._map!),
        }).setLngLat([lng, lat]));
    }

    private _hasRequired(asset: AssetWithLocation): asset is IdentifiableAsset {
        return Boolean(asset?.id && asset?.type);
    }

    private _isMissing(asset: IdentifiableAsset, features: Feature[]) {
        return !features?.some((f) => f.properties?.id === asset.id);
    }

    private _getClusterProperties() {
        return Object.fromEntries(Object.keys(this._assetTypeColors).map(AssetMap._getClusterPropertyExpression));
    }

    private static _getClusterPropertyExpression(value: string) {
        return [value, ["+", ["case", ["==", ["get", AssetMap._clusterProperty], value], 1, 0]]];
    }

    private static _assetToFeature({
        id,
        type,
        name,
        attributes,
    }: IdentifiableAsset): Feature<Point, { id: string; name?: string; [AssetMap._clusterProperty]: string }> {
        return {
            type: "Feature",
            geometry: attributes.location.value,
            properties: { id, name, [AssetMap._clusterProperty]: type },
        };
    }
}
