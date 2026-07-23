/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
import { css, html, LitElement } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import type { Map as MapGL } from "maplibre-gl";
import { OrMapBaseControl } from "./base";
import maplibregl from "maplibre-gl";
import debounce from "lodash.debounce";
import { i18next } from "@openremote/or-translate";
import "@openremote/or-vaadin-components/or-vaadin-combo-box";
import "@openremote/or-vaadin-components/or-vaadin-button";
import "@openremote/or-icon";
import type { ComboBoxDataProviderCallback, ComboBoxDataProviderParams } from "@openremote/or-vaadin-components/or-vaadin-combo-box";
import type { MapGeocoderEventDetail } from "../types";

export class OrMapGeocoderChangeEvent extends CustomEvent<MapGeocoderEventDetail> {

    public static readonly NAME = "or-map-geocoder-change";

    constructor(geocode: any) {
        super(OrMapGeocoderChangeEvent.NAME, {
            detail: { geocode },
            bubbles: true,
            composed: true,
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrMapGeocoderChangeEvent.NAME]: OrMapGeocoderChangeEvent;
    }
}

/**
 * This is a custom geocoder replacing `@maplibre/maplibre-gl-geocoder` to be able to use Vaadin
 * components. We talk directly to the Nominatim endpoint which can be configured in the
 * mapsettings.json.
 */
@customElement("or-map-geocoder")
export class OrMapGeocoder extends LitElement {

    static get styles() {
        return css`
            :host { display: block; }

            or-vaadin-button {
                background: white;
                border-radius: var(--lumo-border-radius-m);
            }

            or-icon {
                --or-icon-width: 18px;
                --or-icon-height: 18px;
                color: var(--lumo-secondary-text-color);
            }

            or-icon[slot="prefix"] {
                margin-left: 4px;
                margin-right: 8px;
            }

            or-vaadin-combo-box {
                width: 300px;
                --vaadin-input-field-background: white;
            }

            /* Hide dropdown toggle — suggestions open when typing */
            or-vaadin-combo-box::part(toggle-button) {
                display: none;
            }

            vaadin-combo-box-item.no-results-item {
                pointer-events: none;
                color: var(--lumo-disabled-text-color);
            }

            @media only screen and (max-width: 450px) {
                or-vaadin-combo-box {
                    width: 100%;
                }
            }
        `;
    }

    @property({ type: String })
    public geocodeUrl = "";

    @property({ type: Array })
    public bbox?: [west: number, south: number, east: number, north: number];

    @state()
    private _collapsed = true;

    private _hasValue = false;
    private _map?: MapGL;
    private _marker?: any;
    private _mq = window.matchMedia("(max-width: 450px)");
    private _onMqChange = (e: MediaQueryList | MediaQueryListEvent) => {
        if (e.matches) this._collapsed = false;
        else if (!this._hasValue) this._collapsed = true;
    };
    private _onLanguageChanged = () => {
        (this.shadowRoot?.querySelector("or-vaadin-combo-box") as any)?.clearCache?.();
    };

    public connectedCallback() {
        super.connectedCallback();
        i18next.on("languageChanged", this._onLanguageChanged);
        this._mq.addEventListener("change", this._onMqChange);
        this._onMqChange(this._mq);
    }

    public setMap(map: MapGL): void {
        this._map = map;
    }

    protected render() {
        if (this._collapsed) {
            return html`
                <or-vaadin-button theme="icon" .title="${i18next.t("mapPage.searchLocation")}" @click="${this._expand}">
                    <or-icon icon="mdi:magnify"></or-icon>
                </or-vaadin-button>
            `;
        }
        return html`
            <or-vaadin-combo-box
                .dataProvider="${this._dataProvider}"
                .itemClassNameGenerator="${this._itemClassNameGenerator}"
                item-label-path="place_name"
                item-value-path="place_name"
                placeholder="${i18next.t("mapPage.searchLocationPlaceholder")}"
                clear-button-visible
                @selected-item-changed="${this._onItemSelected}"
            >
                <or-icon slot="prefix" icon="mdi:magnify"></or-icon>
            </or-vaadin-combo-box>
        `;
    }

    private _expand() {
        this._collapsed = false;
        this.updateComplete.then(() => {
            (this.shadowRoot?.querySelector("or-vaadin-combo-box") as HTMLElement)?.focus?.();
        });
    }

    private _dataProvider = (params: ComboBoxDataProviderParams, callback: ComboBoxDataProviderCallback<any>) => {
        this._fetchSuggestions(params.filter, callback);
    };

    private _fetchSuggestions = debounce(async (query: string, callback: ComboBoxDataProviderCallback<any>) => {
        if (!query || query.length < 2) {
            callback([], 0);
            return;
        }
        const features: any[] = [];
        try {
            const params = new URLSearchParams({
                q: query,
                format: "geojson",
                polygon_geojson: "1",
                addressdetails: "1",
                "accept-language": i18next.language || "en",
            });
            if (this.bbox) {
                const [west, south, east, north] = this.bbox;
                params.set("viewbox", `${west},${north},${east},${south}`);
                params.set("bounded", "1");
            }
            const response = await fetch(`${this.geocodeUrl}/search?${params.toString()}`);
            if (!response.ok) {
                callback([], 0);
                return;
            }
            const geojson = await response.json();
            for (const feature of geojson.features) {
                const center: [number, number] = feature.bbox
                    ? [feature.bbox[0] + (feature.bbox[2] - feature.bbox[0]) / 2,
                       feature.bbox[1] + (feature.bbox[3] - feature.bbox[1]) / 2]
                    : feature.geometry.coordinates;
                features.push({ place_name: feature.properties.display_name, center });
            }
        } catch (e) {
            console.error("Failed to forwardGeocode:", e);
            callback([], 0);
            return;
        }
        if (features.length) {
            callback(features, features.length);
        } else {
            const noResults = [{ place_name: i18next.t("mapPage.searchNoResults"), noResults: true }];
            callback(noResults, noResults.length);
        }
    }, 300);

    private _itemClassNameGenerator = (item: any) => item?.noResults ? "no-results-item" : "";

    private _onItemSelected(e: CustomEvent) {
        const item = e.detail.value;
        if (item?.noResults) return;
        if (item) {
            this._hasValue = true;
            this._marker?.remove();
            this._map?.flyTo({ center: item.center, zoom: 14 });
            this._marker = new maplibregl.Marker().setLngLat(item.center).addTo(this._map!);
            this.dispatchEvent(new OrMapGeocoderChangeEvent({
                type: "Feature",
                geometry: { type: "Point", coordinates: item.center },
            }));
        } else if (this._hasValue) {
            // Clear button was clicked — collapse back (unless small screen keeps it expanded)
            this._hasValue = false;
            this._marker?.remove();
            this._marker = undefined;
            this._fetchSuggestions.cancel();
            if (!this._mq.matches) this._collapsed = true;
        }
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
        i18next.off("languageChanged", this._onLanguageChanged);
        this._mq.removeEventListener("change", this._onMqChange);
        this._fetchSuggestions.cancel();
        this._marker?.remove();
    }
}

export class OrMapGeocoderControl extends OrMapBaseControl {
    constructor(private _geocodeUrl: string, private _bbox?: [number, number, number, number]) { super(); }

    onAdd(map: MapGL): HTMLElement {
        this._createContainer();
        this._container!.classList.add("geocoder-control");
        const component = document.createElement("or-map-geocoder") as OrMapGeocoder;
        component.geocodeUrl = this._geocodeUrl;
        if (this._bbox) component.bbox = this._bbox;
        component.setMap(map);
        this._container!.appendChild(component);
        return this._container!;
    }
}
