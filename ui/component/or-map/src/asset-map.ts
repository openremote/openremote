import { Asset } from "@openremote/model";
import { LngLatLike, MapSourceDataEvent, Marker } from "maplibre-gl";
import { Geometry } from "geojson";
import { AssetWithLocation, ClusterConfig } from "./types";
import { MapWidget } from "./mapwidget";
import { OrClusterMarker, Slice } from "./markers/or-cluster-marker";
import { getMarkerIconAndColorFromAssetType } from "./util";
import { OrMapMarkersChangedEvent } from ".";

export class AssetMap extends MapWidget {
    protected _clusterConfig?: ClusterConfig;

    protected _assetTypeColors: any = {};
    protected _cachedMarkers: Record<string, Marker> = {};
    protected _markersOnScreen: Record<string, Marker> = {};
    protected _assetsOnScreen: Record<string, AssetWithLocation> = {};

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

    protected _onMove = () => this._updateMarkers();
    protected _onMoveEnd = (e: any) => {
        // Ensure marker updates happen after the frame
        requestAnimationFrame(() => {
            // On WebKit browsers the clicked marker may not be removed,
            // if the camera zoom level is directly between 2 levels
            if (e.marker instanceof OrClusterMarker) {
                this._removeMarker(e.marker._clusterId);
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

    /**
     * Load map sources, layers and events
     */
    public async load() {
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

        this._map.on("data", this._onData);
    }

    public addAssetMarker(
        assetId: string,
        assetName: string,
        assetType: string,
        long: number,
        lat: number,
        asset: Asset
    ) {
        this._assetTypeColors[assetType] = getMarkerIconAndColorFromAssetType(assetType)?.color;
        this._assetCollection.features.push({
            type: "Feature",
            properties: {
                name: assetName,
                id: assetId,
                assetType: assetType,
                asset: asset,
            },
            geometry: {
                type: "Point",
                coordinates: [long, lat],
            },
        });
    }

    public cleanUpAssetMarkers(): void {
        this._assetCollection = {
            type: "FeatureCollection",
            features: [],
        };
        this._assetTypeColors = {};
    }

    protected _updateMarkers() {
        if (!this._map) return;

        const newMarkers: Record<string, Marker> = {};
        const features = this._map.querySourceFeatures("assets");

        // Asset markers
        for (const feature of features) {
            if (!feature.properties.id) continue;
            const id: string = feature.properties.id;
            const geometry = feature.geometry as Geometry & { coordinates: LngLatLike };
            const coords = geometry.coordinates;

            let marker = this._cachedMarkers[id];
            if (!marker) {
                const placeholder = document.createElement("div");
                marker = this._cachedMarkers[id] = new Marker({ element: placeholder }).setLngLat(coords);
            }
            newMarkers[id] = marker;

            if (!this._markersOnScreen[id]) {
                marker.addTo(this._map);
                this._assetsOnScreen[id] = JSON.parse(feature.properties.asset);
            }
        }

        // Cluster markers
        for (const feature of features) {
            if (!feature.properties.cluster) continue;
            const id: number = feature.properties.cluster_id;
            const geometry = feature.geometry as Geometry & { coordinates: [number, number] };
            const [lng, lat] = geometry.coordinates;

            let marker = this._cachedMarkers[id];
            if (!marker) {
                const slices: Slice[] = Object.entries(feature.properties)
                    .filter(([k]) => this._assetTypeColors.hasOwnProperty(k))
                    .map(([type, count]) => [type, this._assetTypeColors[type], count]);

                marker = this._cachedMarkers[id] = new Marker({
                    element: new OrClusterMarker(slices, id, lng, lat, this._map),
                }).setLngLat([lng, lat]);
            }
            newMarkers[id] = marker;

            if (!this._markersOnScreen[id]) marker.addTo(this._map);
        }

        for (const id in this._markersOnScreen) {
            const marker = newMarkers[id];
            if (!marker || !this._hasAllAssetTypes(marker._element)) {
                this._removeMarker(id);
            }
        }
        this._markersOnScreen = newMarkers;
        this._mapContainer.dispatchEvent(new OrMapMarkersChangedEvent(Object.values(this._assetsOnScreen)));
    }

    private _removeMarker(id: string) {
        this._markersOnScreen[id].remove();
        delete this._assetsOnScreen[id];
    }

    private _hasAllAssetTypes(element: HTMLElement): boolean {
        return element instanceof OrClusterMarker && element.hasTypes(Object.keys(this._assetTypeColors));
    }
}
