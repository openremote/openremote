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
import { css, html, LitElement, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { AssetQuery } from "@openremote/model";
import { Util } from "@openremote/core";
import { i18next } from "@openremote/or-translate";
import "@openremote/or-vaadin-components/or-vaadin-badge";
import "@openremote/or-vaadin-components/or-vaadin-select";
import "@openremote/or-vaadin-components/or-vaadin-item";
import "@openremote/or-vaadin-components/or-vaadin-list-box";
import { selectRenderer } from "@openremote/or-vaadin-components/or-vaadin-select"
import { AssetWithLocation } from "./types";

export class OrMapPresetFilterEvent extends CustomEvent<AssetQuery | null> {
    public static readonly NAME = "or-map-preset-filter-changed";

    constructor(filter: AssetQuery | null) {
        super(OrMapPresetFilterEvent.NAME, {
            bubbles: false,
            composed: false,
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
                display: inline-block;
            }
            or-vaadin-select {
                width: 320px;
                --vaadin-input-field-background: white;
            }
            .filter-field-count {
                position: absolute;
                right: 40px;
                top: 50%;
                transform: translateY(-50%);
                pointer-events: none;
            }
        `;
    }

    @property({ type: Array })
    public filters: AssetQuery[] = [];

    @property({ type: Array })
    public assets: AssetWithLocation[] = [];

    @state()
    protected _activeIndex = 0;

    protected _getFilterLabel(filter: AssetQuery): string {
        const types = filter.types;
        const typeLabel = types?.length
            ? types.map(t => Util.getAssetTypeLabel(t).replace(/\s*asset\s*$/i, "").trim()).join(" + ")
            : i18next.t("mapPage.filterCustom", { defaultValue: "Custom" });

        const attrValues = (filter.attributes?.items ?? [] as any[])
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
        const filter = this.filters[filterIndex - 1];
        return this.assets.filter(a => Util.assetMatchesQuery(a, filter)).length;
    }

    protected _buildOptions() {
        return [
            { value: "0", label: i18next.t("mapPage.filterAll", { defaultValue: "All" }), count: this.assets.length },
            ...this.filters.map((filter, i) => ({
                value: String(i + 1),
                label: this._getFilterLabel(filter),
                count: this._getFilterCount(i + 1)
            }))
        ];
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
            </style>
            <or-vaadin-list-box>
                ${options.map(opt => html`
                    <or-vaadin-item class="filter-option" value="${opt.value}" label="${opt.label}">
                        <div class="filter-item">
                            <span class="filter-item__label">${opt.label}</span>
                            <or-vaadin-badge class="filter-item__count">${opt.count > 99 ? "99+" : opt.count}</or-vaadin-badge>
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
                <or-vaadin-badge class="filter-field-count">${currentCount > 99 ? "99+" : currentCount}</or-vaadin-badge>
            </div>
        `;
    }

    protected _onChange(e: Event) {
        this._activeIndex = parseInt((e.target as HTMLInputElement).value) || 0;
        const filter = this._activeIndex > 0 ? this.filters[this._activeIndex - 1] : null;
        this.dispatchEvent(new OrMapPresetFilterEvent(filter));
    }
}
