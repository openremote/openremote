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
import { css, html, LitElement, PropertyValues, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { Util } from "@openremote/core";
import { i18next } from "@openremote/or-translate";
import "@openremote/or-vaadin-components/or-vaadin-badge";
import "@openremote/or-vaadin-components/or-vaadin-select";
import "@openremote/or-vaadin-components/or-vaadin-item";
import "@openremote/or-vaadin-components/or-vaadin-list-box";
import { selectRenderer } from "@openremote/or-vaadin-components/or-vaadin-select"
import type { Map as MapGL } from "maplibre-gl";
import { OrMapBaseControl } from "./base";
import { AssetWithLocation, MapFilter } from "../types";
import { formatCount } from "../util";

import { AssetQuery } from "@openremote/model";

export class OrMapPresetFilterEvent extends CustomEvent<AssetQuery | null> {
    public static readonly NAME = "or-map-preset-filter-changed";

    constructor(filter: AssetQuery | null) {
        super(OrMapPresetFilterEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: filter
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrMapPresetFilterEvent.NAME]: OrMapPresetFilterEvent;
    }
}

@customElement("or-map-preset-filter")
export class OrMapPresetFilter extends LitElement {

    static get styles() {
        return css`
            :host {
                display: block;
            }
            .filter-container {
                position: relative;
                display: block;
            }
            or-vaadin-select {
                width: 320px;
                --vaadin-input-field-background: white;
            }
            @media only screen and (max-width: 40em) {
                :host {
                    width: 100%;
                }
                or-vaadin-select {
                    width: 100%;
                }
            }
            .filter-field-count {
                position: absolute;
                right: 40px;
                top: 50%;
                transform: translateY(-50%);
                pointer-events: none;
                --vaadin-badge-background: var(--shades-contrast-10, #3A463A0D);
                border-radius: calc(var(--lumo-border-radius-m) + 2px);
                font-size: 12px;
                color: var(--lumo-secondary-text-color);
                padding: 0 calc(var(--lumo-space-m) - 4px);
            }
        `;
    }

    @property({ type: Array })
    public filters: MapFilter[] = [];

    @property({ type: Array })
    public assets: AssetWithLocation[] = [];

    @state()
    protected _activeIndex = 0;

    protected _getFilterLabel(filter: MapFilter): string {
        if (filter.label) return filter.label;
        const { query } = filter;
        const types = query.types;
        const typeLabel = types?.length
            ? types.map(t => Util.getAssetTypeLabel(t).replace(/\s*asset\s*$/i, "").trim()).join(" + ")
            : i18next.t("mapPage.filterCustom");
        const attrValues = (query.attributes?.items ?? [] as any[])
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
        if (filterIndex === 0) return this.assets.length;
        const query = new Util.AssetQueryHelper(this.filters[filterIndex - 1].query);
        return this.assets.filter(a => query.matches(a)).length;
    }

    protected _buildOptions() {
        const all = { value: "0", label: i18next.t("mapPage.filterAll"), count: this.assets.length };
        const filters = this.filters
            .map((filter, i) => ({
                value: String(i + 1),
                label: this._getFilterLabel(filter),
                count: this._getFilterCount(i + 1)
            }))
            .sort((a, b) => a.label.localeCompare(b.label));
        return [all, ...filters];
    }

    protected _renderFilterOptions(): TemplateResult {
        const options = this._buildOptions();
        return html`
            <style>
                .filter-item {
                    display: flex;
                    align-items: center;
                    gap: calc(var(--lumo-space-l) + var(--lumo-border-radius-m) / 4);
                    padding-right: 4px;
                }
                .filter-item__label {
                    flex: 1;
                }
                or-vaadin-item.filter-option {
                    padding: unset;
                }
                or-vaadin-badge {
                    --vaadin-badge-background: var(--shades-contrast-10, #3A463A0D);
                    border-radius: calc(var(--lumo-border-radius-m) + 2px);
                    font-size: 12px;
                    color: var(--lumo-secondary-text-color);
                    padding: 0 calc(var(--lumo-space-m) - 4px);
                }
            </style>
            <or-vaadin-list-box>
                ${options.map(opt => html`
                    <or-vaadin-item class="filter-option" value="${opt.value}" label="${opt.label}">
                        <div class="filter-item">
                            <span class="filter-item__label">${opt.label}</span>
                            <or-vaadin-badge class="filter-item__count">${formatCount(opt.count)}</or-vaadin-badge>
                        </div>
                    </or-vaadin-item>
                `)}
            </or-vaadin-list-box>
        `;
    }

    protected render() {
        const currentCount = this._getFilterCount(this._activeIndex);
        return html`
            <div class="filter-container">
                <or-vaadin-select
                    .value="${String(this._activeIndex)}"
                    ${selectRenderer(this._renderFilterOptions, [this.filters, this.assets])}
                    @change="${this._onChange}"
                ></or-vaadin-select>
                <or-vaadin-badge class="filter-field-count">${formatCount(currentCount)}</or-vaadin-badge>
            </div>
        `;
    }

    protected updated(changedProperties: PropertyValues) {
        if (changedProperties.has("filters") && this.filters.length) {
            const defaultIndex = this.filters.findIndex(f => f.default);
            if (defaultIndex >= 0) {
                this._activeIndex = defaultIndex + 1;
                this.dispatchEvent(new OrMapPresetFilterEvent(this.filters[defaultIndex].query));
            }
        }
    }

    protected _onChange(e: Event) {
        this._activeIndex = parseInt((e.target as HTMLInputElement).value) || 0;
        const filter = this._activeIndex > 0 ? this.filters[this._activeIndex - 1].query : null;
        this.dispatchEvent(new OrMapPresetFilterEvent(filter));
    }
}

export class OrMapPresetFilterControl extends OrMapBaseControl {
    private _component?: OrMapPresetFilter;

    constructor(private _filters: MapFilter[], private _assets: AssetWithLocation[]) { super(); }

    onAdd(_map: MapGL): HTMLElement {
        this._createContainer();
        this._container!.classList.add("preset-filter-control");
        this._component = document.createElement("or-map-preset-filter") as OrMapPresetFilter;
        this._component.filters = this._filters;
        this._component.assets = this._assets;
        this._container!.appendChild(this._component);
        return this._container!;
    }

    onRemove(): void {
        super.onRemove();
        this._component = undefined;
    }

    set assets(assets: AssetWithLocation[]) {
        this._assets = assets;
        if (this._component) this._component.assets = assets;
    }

    /** Returns the initial filter synchronously by reading localStorage — call before flushing the asset buffer. */
    getInitialFilter(): AssetQuery | null {
        const key = `or-map-filter:${manager.displayRealm}:${manager.username}`;
        const stored = localStorage.getItem(key);
        const storedIndex = stored !== null ? parseInt(stored) : -1;
        const isValid = storedIndex >= 0 && storedIndex <= this._filters.length;
        if (isValid && storedIndex > 0) return this._filters[storedIndex - 1].query;
        const defaultIndex = this._filters.findIndex(f => f.default);
        if (defaultIndex >= 0) return this._filters[defaultIndex].query;
        return null;
    }

    get element(): OrMapPresetFilter | undefined {
        return this._component;
    }
}
