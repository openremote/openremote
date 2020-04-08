import {css, customElement, html, LitElement, property, query} from "lit-element";
import {connect} from "pwa-helpers/connect-mixin";
import "@openremote/or-map";
import "@openremote/or-map/dist/or-map-asset-card";
import {ViewerConfig} from "@openremote/or-map/dist/or-map-asset-card";

import {Util} from "@openremote/core";
import {OrMap, OrMapClickedEvent, OrMapMarkerAsset, OrMapMarkerClickedEvent} from "@openremote/or-map";
import {RootState, store} from "../../store";
import {setCurrentAssetId, subscribeAssets, unsubscribeAssets} from "../../actions/map";
import map from "../../reducers/map";
import {createSelector} from "reselect";
import {Asset, AttributeType, MetaItemType} from "@openremote/model";
import {MapStyle} from "./style";
import {router} from "../../index";
store.addReducers({
    map
});

const mapCardConfig: ViewerConfig = {
    default: {
            exclude: ["userNotes"]
    },
    assetTypes: {
        "urn:openremote:asset:iems:weather": {
            exclude: ["location"]
        }
    }
};

@customElement("page-map")
class PageMap extends connect(store)(LitElement)  {

    static get styles() {
        return MapStyle;
    }

    @query("#map")
    protected _map?: OrMap;

    @property()
    protected _assets: Asset[] = [];

    @property()
    protected _currentAsset?: Asset;

    protected _assetSelector = (state: RootState) => state.map.assets;
    protected _currentAssetSelector = (state: RootState) => state.app.activeAsset;

    protected _getMapAssets = createSelector(
        [this._assetSelector],
        (assets) => {
            return assets;
        });

    protected _getCurrentAsset = createSelector(
        [this._assetSelector, this._currentAssetSelector],
        (assets, currentId) => {
            if (!currentId) {
                return null;
            }
            return assets.find((asset) => asset.id === currentId);
        });

    protected render() {

        return html`
            
            ${this._currentAsset ? html `<or-map-asset-card .config="${mapCardConfig}" .asset="${this._currentAsset}"></or-map-asset-card>` : ``}
            
            <or-map id="map" class="or-map">
                ${
                    this._assets.map((asset) => {
                        const attr = Util.getAssetAttribute(asset, AttributeType.LOCATION.attributeName!);
                        const showOnMapMeta = Util.getFirstMetaItem(attr, MetaItemType.SHOW_ON_DASHBOARD.urn!);
                        if(!showOnMapMeta || !showOnMapMeta.value) return html``;
                        return html`
                            <or-map-marker-asset ?active="${this._currentAsset && this._currentAsset.id === asset.id}" .asset="${asset}"></or-map-marker-asset>
                        `;
                    })
            }
            </or-map>
        `;
    }

    public connectedCallback() {
        super.connectedCallback();
        this.addEventListener(OrMapMarkerClickedEvent.NAME, this.onMapMarkerClick);
        this.addEventListener(OrMapClickedEvent.NAME, this.onMapClick);
        store.dispatch(subscribeAssets());
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener(OrMapMarkerClickedEvent.NAME, this.onMapMarkerClick);
        this.removeEventListener(OrMapClickedEvent.NAME, this.onMapClick);
        store.dispatch(unsubscribeAssets());
    }

    stateChanged(state: RootState) {
        this._assets = this._getMapAssets(state);
        this._currentAsset = this._getCurrentAsset(state);
    }

    protected onMapMarkerClick(e: OrMapMarkerClickedEvent) {
        const asset = (e.detail.marker as OrMapMarkerAsset).asset;
        store.dispatch(setCurrentAssetId(asset.id));

        router.navigate('map/'+asset.id);
    }

    protected onMapClick(e: OrMapClickedEvent) {
        router.navigate('map');
    }

    protected getCurrentAsset() {
        this._getCurrentAsset(store.getState());
    }
}
