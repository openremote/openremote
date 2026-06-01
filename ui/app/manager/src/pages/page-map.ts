import {css, html, PropertyValues} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import {createSlice, Store, PayloadAction} from "@reduxjs/toolkit";
import "@openremote/or-map";
import "@openremote/or-vaadin-components/or-vaadin-select";
import "@openremote/or-vaadin-components/or-vaadin-item";
import "@openremote/or-vaadin-components/or-vaadin-list-box";
import { selectRenderer } from "@vaadin/select/lit.js";
import {
    MapAssetCardConfig,
    OrMap,
    OrMapAssetCardLoadAssetEvent,
    OrMapClickedEvent,
    OrMapMarkerAsset,
    OrMapMarkerClickedEvent,
    OrMapGeocoderChangeEvent,
    MapMarkerAssetConfig,
    OrMapMarkersChangedEvent,
    OrMapLegendEvent,
    OrMapLoadedEvent
} from "@openremote/or-map";
import manager, {Util} from "@openremote/core";
import {createSelector} from "reselect";
import {
    Asset,
    AssetEvent,
    AssetEventCause,
    AssetQuery,
    AttributeEvent,
    GeoJSONPoint,
    WellknownAttributes,
    WellknownMetaItems
} from "@openremote/model";
import {getAssetsRoute, getMapRoute} from "../routes";
import {AppStateKeyed, Page, PageProvider, router} from "@openremote/or-app";
import {GenericAxiosResponse} from "@openremote/rest";
import { ClusterConfig, Util as MapUtil, AssetWithLocation } from "@openremote/or-map";
import { i18next } from "@openremote/or-translate";

export interface MapState {
    locatedAssets: AssetWithLocation[];
    unlocatedAssets: Asset[];
    currentAssetId: string;
}

export interface MapStateKeyed extends AppStateKeyed {
    map: MapState;
}

const INITIAL_STATE: MapState = {
    locatedAssets: [],
    unlocatedAssets: [],
    currentAssetId: undefined
};

const pageMapSlice = createSlice({
    name: "pageMap",
    initialState: INITIAL_STATE,
    reducers: {
        assetEventReceived(state: MapState, action: PayloadAction<AssetEvent>) {
            if (action.payload.cause === AssetEventCause.CREATE) {
                if (MapUtil.isAssetWithLocation(action.payload.asset)) {
                    state.locatedAssets.push(action.payload.asset);
                } else {
                    state.unlocatedAssets.push(action.payload.asset);
                }
            } else if (action.payload.cause === AssetEventCause.DELETE) {
                const index = state.locatedAssets.findIndex(asset => asset.id === action.payload.asset.id);
                if (index > -1) state.locatedAssets.splice(index, 1);
                else {
                    const index = state.unlocatedAssets.findIndex(asset => asset.id === action.payload.asset.id);
                    if (index > -1) state.unlocatedAssets.splice(index, 1);
                }
            }
            return state;
        },
        attributeEventReceived(state: MapState, action: PayloadAction<[string[], AttributeEvent]>) {
            const attrsOfInterest = action.payload[0];
            const attrEvent = action.payload[1];
            const attrName = attrEvent.ref.name;

            // Only react if attribute is an attribute of interest
            if (!attrsOfInterest.includes(attrName)) {
                return;
            }

            const locatedAssets = state.locatedAssets;
            const unlocatedAssets = state.unlocatedAssets;
            const assetId = attrEvent.ref.id;

            const located = locatedAssets.findIndex((a) => a.id === assetId);
            if (located > -1) {
                const asset = Util.updateAsset({ ...locatedAssets[located] }, attrEvent);
                if (MapUtil.isAssetWithLocation(asset)) {
                    locatedAssets[located] = asset;
                } else {
                    locatedAssets.splice(located, 1);
                    unlocatedAssets.push(asset);
                }
            } else {
                const unlocated = unlocatedAssets.findIndex((a) => a.id === assetId);
                if (unlocated > -1) {
                    const asset = Util.updateAsset({ ...unlocatedAssets[unlocated] }, attrEvent);
                    if (MapUtil.isAssetWithLocation(asset)) {
                        unlocatedAssets.splice(unlocated, 1);
                        locatedAssets.push(asset);
                    } else {
                        unlocatedAssets[unlocated] = asset;
                    }
                }
            }

            return state;
        },
        setAssets(state, action: PayloadAction<Asset[]>) {
            const locatedAssets = [], unlocatedAssets = [];
            for (const asset of action.payload) {
                if (MapUtil.isAssetWithLocation(asset)) {
                    locatedAssets.push(asset);
                } else {
                    unlocatedAssets.push(asset);
                }
            }
            return { ...state, locatedAssets, unlocatedAssets };
        }
    }
});

const {assetEventReceived, attributeEventReceived, setAssets} = pageMapSlice.actions;
export const pageMapReducer = pageMapSlice.reducer;

export interface MapPresetFilter {
    assetQuery: AssetQuery;
}

export interface PageMapConfig {
    legend?: {
      show: boolean
    },
    clustering?: ClusterConfig,
    card?: MapAssetCardConfig,
    assetQuery?: AssetQuery,
    markers?: MapMarkerAssetConfig,
    filters?: MapPresetFilter[]
}

export function pageMapProvider(store: Store<MapStateKeyed>, config?: PageMapConfig): PageProvider<MapStateKeyed> {
    return {
        name: "map",
        routes: [
            "map",
            "map/:id"
        ],
        pageCreator: () => {
            const page = new PageMap(store);
            page.config = config || {};
            return page
        }
    };
}


@customElement("page-map")
export class PageMap extends Page<MapStateKeyed> {

    static get styles() {
        // language=CSS
        return css`
          :host {
              display: flex;
          }

           or-map-asset-card {
                height: 35vh;
                position: absolute;
                bottom: 0;
                right: 0;
                width: 100vw;
                z-index: 3;
            }

            #filter-select {
                position: absolute;
                top: 10px;
                left: 10px;
                z-index: 3;
                width: 320px;
                --vaadin-input-field-background: white;
            }

           or-map-legend {
               position: absolute;
               bottom: 10px;
               right: 10px;
               width: 254px;
               margin: 10px 0;
               z-index: 1;
           }

            or-map {
                flex: 1 1 auto;
            }

            @media only screen and (min-width: 40em){
                or-map-asset-card {
                    position: absolute;
                    top: 56px;
                    left: 10px;
                    width: 320px;
                    margin: 0;
                    height: 400px; /* fallback for IE */
                    height: max-content;
                    max-height: calc(100vh - 150px);
                    z-index: 2;
                }

                or-map-legend {
                    position: absolute;
                    bottom: 10px;
                    right: 10px;
                    width: calc(100%);
                    max-width: 254px;
                    margin: 0;
                    height: max-content;
                    max-height: calc(100vh - 150px);
                }
            }
        `;
    }

    @property()
    public config?: PageMapConfig;

    @query("#map")
    protected _map?: OrMap;

    @state()
    protected _assets: AssetWithLocation[] = [];

    @state()
    protected _currentAsset?: Asset;

    @state()
    protected _assetTypes: string[] = [];

    @state()
    protected _excludedTypes: string[] = [];

    @state()
    protected _assetsOnScreen: AssetWithLocation[] = [];

    @state()
    protected _activeFilterIndex = 0;

    protected _locatedAssetSelector = (state: MapStateKeyed) => state.map.locatedAssets;
    protected _unlocatedAssetSelector = (state: MapStateKeyed) => state.map.unlocatedAssets;
    protected _paramsSelector = (state: MapStateKeyed) => state.app.params;
    protected _realmSelector = (state: MapStateKeyed) => state.app.realm || manager.displayRealm;

    protected _assetSubscriptionId: string;
    protected _attributeSubscriptionId: string;

    protected getAttributesOfInterest(): (string | WellknownAttributes)[] {
        // Extract all label attributes configured in marker config
        let markerLabelAttributes = [];

        if (this.config && this.config.markers) {
            markerLabelAttributes = Object.values(this.config.markers)
              .filter(assetTypeMarkerConfig => assetTypeMarkerConfig.attributeName)
              .map(assetTypeMarkerConfig => assetTypeMarkerConfig.attributeName);
        }

        const filterAttributes = (this.config?.filters ?? [])
            .flatMap(f => f.assetQuery.attributes?.items ?? [])
            .map((item: any) => item.name?.value)
            .filter((name): name is string => !!name);

        return [
            ...markerLabelAttributes,
            ...filterAttributes,
            WellknownAttributes.LOCATION,
            WellknownAttributes.DIRECTION
        ];
    }

    protected subscribeAssets = async (realm: string) => {
        let response: GenericAxiosResponse<Asset[]>;
        const attrsOfInterest = this.getAttributesOfInterest();
        const assetQuery: AssetQuery = this.config && this.config.assetQuery ? this.config.assetQuery : {
            realm: {
                name: realm
            },
            select: {
                attributes: attrsOfInterest,
            },
            attributes: {
                items: [
                    {
                        name: {
                            predicateType: "string",
                            value: WellknownAttributes.LOCATION
                        },
                        meta: [
                            {
                                name: {
                                    predicateType: "string",
                                    value: WellknownMetaItems.SHOWONDASHBOARD
                                },
                                negated: true
                            },
                            {
                                name: {
                                    predicateType: "string",
                                    value: WellknownMetaItems.SHOWONDASHBOARD
                                },
                                value: {
                                    predicateType: "boolean",
                                    value: true
                                }
                            }
                        ]
                    }
                ]
            }
        };

        try {
            response = await manager.rest.api.AssetResource.queryAssets(assetQuery);

            if (!this.isConnected || realm !== this._realmSelector(this.getState())) {
                // No longer connected or realm has changed
                return;
            }

            if (response.data) {
                const assets = response.data;

                this._store.dispatch(setAssets(assets));
                this._map?.addAssets(this._assets);

                const assetSubscriptionId = await manager.events.subscribeAssetEvents(undefined, false, (event) => {
                    this._store.dispatch(assetEventReceived(event));
                    switch (event.cause) {
                        case "DELETE": this._map?.removeAssets([event.asset.id]); break;
                        case "CREATE":
                            if (MapUtil.isAssetWithLocation(event.asset) && this._isAssetVisible(event.asset)) {
                                this._map?.addAsset(event.asset);
                            }
                            break;
                    }
                });

                if (!this.isConnected || realm !== this._realmSelector(this.getState())) {
                    manager.events.unsubscribe(assetSubscriptionId);
                    return;
                }

                this._assetSubscriptionId = assetSubscriptionId;

                const attributeSubscriptionId = await manager.events.subscribeAttributeEvents(undefined, false, (event) => {
                    const interested = attrsOfInterest.includes(event.ref.name);
                    let asset: Asset | undefined;
                    if (interested) {
                        const assets = this._unlocatedAssetSelector(this.getState());
                        asset = assets.find(asset => asset.id === event.ref.id);
                        // Update the attribute if the asset already has a location
                        // assuming the asset is in the located asset state
                        if (!asset) {
                            this._map?.updateAttribute(event);
                        }
                    }
                    this._store.dispatch(attributeEventReceived([attrsOfInterest, event]));
                    // Add the asset after map state has been updated
                    if (interested && asset) {
                        const located = this._assets.find(a => a.id === event.ref.id);
                        if (located && this._isAssetVisible(located)) {
                            this._map?.addAsset(located);
                        }
                    }
                });

                if (!this.isConnected || realm !== this._realmSelector(this.getState())) {
                    this._assetSubscriptionId = undefined;
                    manager.events.unsubscribe(assetSubscriptionId);
                    manager.events.unsubscribe(attributeSubscriptionId);
                    return;
                }

                this._attributeSubscriptionId = attributeSubscriptionId;
            }
        } catch (e) {
            console.error("Failed to subscribe to assets", e);
        }
    };

    protected unsubscribeAssets = () => {
        if (this._assetSubscriptionId) {
            manager.events.unsubscribe(this._assetSubscriptionId);
            this._assetSubscriptionId = undefined;
        }
        if (this._attributeSubscriptionId) {
            manager.events.unsubscribe(this._attributeSubscriptionId);
            this._attributeSubscriptionId = undefined;
        }
        this._map?.removeAllAssets();
    };

    protected getRealmState = createSelector(
      [this._realmSelector],
      async (realm) => {
          this._excludedTypes = [];
          this.unsubscribeAssets();
          this.subscribeAssets(realm);
          this._map?.refresh();
      }
    )

    protected _getMapAssets = createSelector(
      [this._locatedAssetSelector],
      (assets) => {
          return assets;
      });

    protected _getCurrentAsset = createSelector(
      [this._locatedAssetSelector, this._paramsSelector],
      (assets, params) => {
          const currentId = params ? params.id : undefined;

          if (!currentId) {
              return null;
          }

          return assets.find((asset) => asset.id === currentId);
      });

    protected _setCenter(geocode: any) {
        this._map!.center = [geocode.geometry.coordinates[0], geocode.geometry.coordinates[1]];
    }

    get name(): string {
        return "map";
    }

    constructor(store: Store<MapStateKeyed>) {
        super(store);
        this.addEventListener(OrMapAssetCardLoadAssetEvent.NAME, this._onLoadAssetEvent);
    }

    shouldUpdate(_changedProperties: PropertyValues): boolean {
        if (_changedProperties.has('_assets')) {
            const newTypes = [];
            for (const { type } of this._assets) {
                if (!newTypes.includes(type)) newTypes.push(type);
            }
            this._assetTypes = newTypes;
        }
        return super.shouldUpdate(_changedProperties);
    }

    protected render() {
        const showLegend = this.config?.legend?.show !== false && this._assetTypes.length > 1;
        const filters = this.config?.filters;
        const filterOptions = filters?.length ? [
            { value: "0", label: i18next.t("mapPage.filterAll", { defaultValue: "All" }), count: this._assets.length },
            ...filters.map((filter, i) => ({
                value: String(i + 1),
                label: this._getFilterLabel(filter),
                count: this._getFilterCount(i + 1)
            }))
        ] : undefined;

        return html`
            ${this._currentAsset ? html `<or-map-asset-card .config="${this.config?.card}" .assetId="${this._currentAsset.id}" .markerconfig="${this.config?.markers}"></or-map-asset-card>` : ``}

            ${filterOptions ? html`
                <or-vaadin-select id="filter-select"
                    .value="${String(this._activeFilterIndex)}"
                    ${selectRenderer(() => html`
                        <or-vaadin-list-box>
                            ${filterOptions.map(opt => html`
                                <or-vaadin-item value="${opt.value}" label="${opt.label}">
                                    <div style="display:flex;align-items:center;gap:8px;">
                                        <span style="flex:1;">${opt.label}</span>
                                        <span style="background:#3A463A1A;border-radius:10px;padding:1px 8px;font-size:0.8em;">${opt.count}</span>
                                    </div>
                                </or-vaadin-item>
                            `)}
                        </or-vaadin-list-box>
                    `, filterOptions)}
                    @change="${this._onFilterChanged}"
                ></or-vaadin-select>
            ` : null}

            ${showLegend ? html`<or-map-legend .assetTypes="${this._assetTypes}" .excludedTypes="${this._excludedTypes}" @or-map-legend-changed="${this._onMapLegendChanged}"></or-map-legend>` : null}

            <or-map id="map" class="or-map" .cluster="${this.config.clustering}" showGeoCodingControl @or-map-geocoder-change="${(ev: OrMapGeocoderChangeEvent) => {this._setCenter(ev.detail.geocode);}}">
                ${this._assetsOnScreen.sort((a,b) => {
                    const pointA = a.attributes[WellknownAttributes.LOCATION].value as GeoJSONPoint;
                    const pointB = b.attributes[WellknownAttributes.LOCATION].value as GeoJSONPoint;
                    if (pointA && pointB){
                        return pointB.coordinates[1] - pointA.coordinates[1];
                    }
                }).map(asset => html`
                    <or-map-marker-asset ?active="${this._currentAsset && this._currentAsset.id === asset.id}" .asset="${asset}" .config="${this.config.markers}"></or-map-marker-asset>
                `)}
            </or-map>
        `;
    }

    public connectedCallback() {
        super.connectedCallback();
        this.addEventListener(OrMapMarkerClickedEvent.NAME, this._onMapMarkerClick);
        this.addEventListener(OrMapClickedEvent.NAME, this._onMapClick);
        this.addEventListener(OrMapMarkersChangedEvent.NAME, this._onMapMarkersChanged);
        this.addEventListener(OrMapLoadedEvent.NAME, this._onMapLoaded);
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener(OrMapMarkerClickedEvent.NAME, this._onMapMarkerClick);
        this.removeEventListener(OrMapClickedEvent.NAME, this._onMapClick);
        this.removeEventListener(OrMapMarkersChangedEvent.NAME, this._onMapMarkersChanged);
        this.removeEventListener(OrMapLoadedEvent.NAME, this._onMapLoaded);
        this.unsubscribeAssets();
    }

    stateChanged(state: MapStateKeyed) {
        this._assets = this._getMapAssets(state);
        this._currentAsset = this._getCurrentAsset(state);
        this.getRealmState(state);
    }

    protected _onMapMarkerClick(e: OrMapMarkerClickedEvent) {
        const asset = (e.detail.marker as OrMapMarkerAsset).asset;
        router.navigate(getMapRoute(asset.id));
    }

    protected _onMapClick(e: OrMapClickedEvent) {
        router.navigate(getMapRoute());
    }

    protected _onMapMarkersChanged(e: OrMapMarkersChangedEvent) {
        this._assetsOnScreen = e.detail;
    }

    protected _onLoadAssetEvent(loadAssetEvent: OrMapAssetCardLoadAssetEvent) {
        router.navigate(getAssetsRoute(false, loadAssetEvent.detail));
    }

    protected _onMapLoaded(e: OrMapLoadedEvent) {
        this._map?.addAssets(this._getVisibleAssets());
    }

    protected _onMapLegendChanged(e: OrMapLegendEvent) {
        this._excludedTypes = e.detail;
        this._applyVisibilityFilters();
    }

    protected _onFilterChanged(e: Event) {
        this._activeFilterIndex = parseInt((e.target as HTMLInputElement).value) || 0;
        this._applyVisibilityFilters();
    }

    protected _assetMatchesFilter(asset: AssetWithLocation, filter: MapPresetFilter): boolean {
        const { types, attributes } = filter.assetQuery;
        if (types?.length && !types.includes(asset.type)) return false;
        if (attributes && !this._evalAttributeGroup(asset, attributes)) return false;
        return true;
    }

     protected _isAssetVisible(asset: AssetWithLocation): boolean {
        if (this._activeFilterIndex > 0) {
            const filter = this.config.filters![this._activeFilterIndex - 1];
            if (!this._assetMatchesFilter(asset, filter)) return false;
        }
        return !this._excludedTypes.includes(asset.type);
    }

    protected _evalAttributeGroup(asset: AssetWithLocation, group: any): boolean {
        const operator: string = group.operator ?? "AND";
        const results: boolean[] = [
            ...(group.items ?? []).map((item: any) => this._evalAttributePredicate(asset, item)),
            ...(group.groups ?? []).map((g: any) => this._evalAttributeGroup(asset, g))
        ];
        if (!results.length) return true;
        return operator === "OR" ? results.some(Boolean) : results.every(Boolean);
    }

    protected _evalAttributePredicate(asset: AssetWithLocation, predicate: any): boolean {
        const attrName = predicate.name?.value;
        if (!attrName) return true;
        const attribute = asset.attributes?.[attrName];
        if (!attribute) return predicate.negated ? true : false;
        const matches = this._evalValuePredicate(attribute.value, predicate.value);
        return predicate.negated ? !matches : matches;
    }

    protected _evalValuePredicate(val: any, predicate: any): boolean {
        if (!predicate) return true;
        switch (predicate.predicateType) {
            case "string": {
                if (val === null || val === undefined) return false;
                const haystack = predicate.caseSensitive !== false ? String(val) : String(val).toLowerCase();
                const needle = predicate.caseSensitive !== false ? predicate.value : predicate.value?.toLowerCase();
                let m: boolean;
                switch (predicate.match) {
                    case "BEGIN":    m = haystack.startsWith(needle); break;
                    case "END":      m = haystack.endsWith(needle); break;
                    case "CONTAINS": m = haystack.includes(needle); break;
                    default:         m = haystack === needle;
                }
                return predicate.negate ? !m : m;
            }
            case "boolean":
                return val === predicate.value;
            case "number": {
                if (typeof val !== "number") return false;
                let m: boolean;
                switch (predicate.operator) {
                    case "GREATER_THAN":   m = val > predicate.value; break;
                    case "GREATER_EQUALS": m = val >= predicate.value; break;
                    case "LESS_THAN":      m = val < predicate.value; break;
                    case "LESS_EQUALS":    m = val <= predicate.value; break;
                    case "BETWEEN":        m = val >= predicate.value && val <= predicate.rangeValue; break;
                    default:               m = val === predicate.value;
                }
                return predicate.negate ? !m : m;
            }
            default:
                return true;
        }
    }

    protected _getVisibleAssets(): AssetWithLocation[] {
        return this._assets.filter(a => this._isAssetVisible(a));
    }

    protected _applyVisibilityFilters() {
        if (!this._map) return;
        const visible = this._getVisibleAssets();
        const visibleIds = new Set(visible.map(a => a.id));
        this._map.removeAssets(this._assets.filter(a => !visibleIds.has(a.id)).map(a => a.id));
        this._map.addAssets(visible);
    }

    protected _getFilterLabel(filter: MapPresetFilter): string {
        const types = filter.assetQuery.types;
        const typeLabel = types?.length
            ? types.map(t => Util.getAssetTypeLabel(t).replace(/\s*asset\s*$/i, "").trim()).join(" + ")
            : i18next.t("mapPage.filterCustom", { defaultValue: "Custom" });

        const attrValues = (filter.assetQuery.attributes?.items ?? [] as any[])
            .map((item: any) => {
                const val = item.value?.value;
                if (val === undefined || val === null) return null;
                if (typeof val === "string") {
                    return val.replace(/_/g, " ").toLowerCase().replace(/^\w/, c => c.toUpperCase());
                }
                return String(val);
            })
            .filter(Boolean);

        return attrValues.length ? `${typeLabel}: ${attrValues.join(", ")}` : typeLabel;
    }

    protected _getFilterCount(filterIndex: number): number {
        if (filterIndex === 0) return this._assets.length;
        const filter = this.config.filters![filterIndex - 1];
        return this._assets.filter(a => this._assetMatchesFilter(a, filter)).length;
    }
}
