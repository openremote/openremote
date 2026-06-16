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
import "@openremote/or-vaadin-components/or-vaadin-combo-box";
import "@openremote/or-vaadin-components/or-vaadin-button";
import "@openremote/or-vaadin-components/or-vaadin-icon";
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

            or-vaadin-icon {
                width: 14px;
                height: 14px;
                color: var(--lumo-secondary-text-color);
            }

            or-vaadin-icon[slot="prefix"] {
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
        `;
    }

    @property({ type: String })
    public geocodeUrl = "";

    @state()
    private _collapsed = true;

    @state()
    private _suggestions: any[] = [];

    private _hasValue = false;
    private _map?: MapGL;
    private _marker?: any;

    public setMap(map: MapGL): void {
        this._map = map;
    }

    protected render() {
        if (this._collapsed) {
            return html`
                <or-vaadin-button theme="icon" title="Search location" @click="${this._expand}">
                    <or-vaadin-icon icon="vaadin:search"></or-vaadin-icon>
                </or-vaadin-button>
            `;
        }
        return html`
            <or-vaadin-combo-box
                .filteredItems="${this._suggestions}"
                item-label-path="place_name"
                item-value-path="place_name"
                placeholder="Search location..."
                clear-button-visible
                @filter-changed="${(e: CustomEvent) => this._fetchSuggestions(e.detail.value)}"
                @selected-item-changed="${this._onItemSelected}"
            >
                <or-vaadin-icon slot="prefix" icon="vaadin:search"></or-vaadin-icon>
            </or-vaadin-combo-box>
        `;
    }

    private _expand() {
        this._collapsed = false;
        this.updateComplete.then(() => {
            (this.shadowRoot?.querySelector("or-vaadin-combo-box") as HTMLElement)?.focus?.();
        });
    }

    private _fetchSuggestions = debounce(async (query: string) => {
        if (!query || query.length < 2) {
            this._suggestions = [];
            return;
        }
        const features: any[] = [];
        try {
            const response = await fetch(
                `${this.geocodeUrl}/search?q=${encodeURIComponent(query)}&format=geojson&polygon_geojson=1&addressdetails=1`
            );
            const geojson = await response.json();
            for (const feature of geojson.features) {
                const center = [
                    feature.bbox[0] + (feature.bbox[2] - feature.bbox[0]) / 2,
                    feature.bbox[1] + (feature.bbox[3] - feature.bbox[1]) / 2,
                ];
                features.push({ place_name: feature.properties.display_name, center });
            }
        } catch (e) {
            console.error("Failed to forwardGeocode:", e);
        }
        this._suggestions = features;
    }, 300);

    private _onItemSelected(e: CustomEvent) {
        const item = e.detail.value;
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
            // Clear button was clicked — collapse back
            this._hasValue = false;
            this._marker?.remove();
            this._marker = undefined;
            this._suggestions = [];
            this._collapsed = true;
        }
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
        this._marker?.remove();
    }
}

export class OrMapGeocoderControl extends OrMapBaseControl {
    constructor(private _geocodeUrl: string) { super(); }

    onAdd(map: MapGL): HTMLElement {
        this._createContainer();
        const component = document.createElement("or-map-geocoder") as OrMapGeocoder;
        component.geocodeUrl = this._geocodeUrl;
        component.setMap(map);
        this._container!.appendChild(component);
        return this._container!;
    }
}
