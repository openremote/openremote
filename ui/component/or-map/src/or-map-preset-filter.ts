import { css, html, LitElement, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { selectRenderer } from "@vaadin/select/lit.js";
import { AssetQuery } from "@openremote/model";
import { Util } from "@openremote/core";
import { i18next } from "@openremote/or-translate";
import "@openremote/or-vaadin-components/or-vaadin-select";
import "@openremote/or-vaadin-components/or-vaadin-item";
import "@openremote/or-vaadin-components/or-vaadin-list-box";
import { AssetWithLocation } from "./types";

export interface MapPresetFilter {
    assetQuery: AssetQuery;
}

export class OrMapPresetFilterEvent extends CustomEvent<MapPresetFilter | null> {
    public static readonly NAME = "or-map-preset-filter-changed";

    constructor(filter: MapPresetFilter | null) {
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

export function evalValuePredicate(val: any, predicate: any): boolean {
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

export function evalAttributePredicate(asset: AssetWithLocation, predicate: any): boolean {
    const attrName = predicate.name?.value;
    if (!attrName) return true;
    const attribute = asset.attributes?.[attrName];
    if (!attribute) return predicate.negated ? true : false;
    const matches = evalValuePredicate(attribute.value, predicate.value);
    return predicate.negated ? !matches : matches;
}

export function evalAttributeGroup(asset: AssetWithLocation, group: any): boolean {
    const operator: string = group.operator ?? "AND";
    const results: boolean[] = [
        ...(group.items ?? []).map((item: any) => evalAttributePredicate(asset, item)),
        ...(group.groups ?? []).map((g: any) => evalAttributeGroup(asset, g))
    ];
    if (!results.length) return true;
    return operator === "OR" ? results.some(Boolean) : results.every(Boolean);
}

export function assetMatchesFilter(asset: AssetWithLocation, filter: MapPresetFilter): boolean {
    const { types, attributes } = filter.assetQuery;
    if (types?.length && (!asset.type || !types.includes(asset.type))) return false;
    if (attributes && !evalAttributeGroup(asset, attributes)) return false;
    return true;
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
                background: #3A463A1A;
                border-radius: 10px;
                padding: 0 4px;
                font-size: 0.8em;
                width: 36px;
                height: 20px;
                line-height: 20px;
                text-align: center;
                box-sizing: border-box;
                pointer-events: none;
            }
        `;
    }

    @property({ type: Array })
    public filters: MapPresetFilter[] = [];

    @property({ type: Array })
    public assets: AssetWithLocation[] = [];

    @state()
    protected _activeIndex = 0;

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
        if (filterIndex === 0) return this.assets.length;
        const filter = this.filters[filterIndex - 1];
        return this.assets.filter(a => assetMatchesFilter(a, filter)).length;
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
                .filter-item__count {
                    background: #3A463A1A;
                    border-radius: 10px;
                    padding: 0 4px;
                    font-size: 0.8em;
                    width: 36px;
                    height: 20px;
                    line-height: 20px;
                    text-align: center;
                    flex-shrink: 0;
                    box-sizing: border-box;
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
                            <span class="filter-item__count">${opt.count > 99 ? "99+" : opt.count}</span>
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
                    ${selectRenderer(() => this._renderFilterOptions(), [this.filters, this.assets])}
                    @change="${this._onChange}"
                ></or-vaadin-select>
                <span class="filter-field-count">
                    ${currentCount > 99 ? "99+" : currentCount}
                </span>
            </div>
        `;
    }

    protected _onChange(e: Event) {
        this._activeIndex = parseInt((e.target as HTMLInputElement).value) || 0;
        const filter = this._activeIndex > 0 ? this.filters[this._activeIndex - 1] : null;
        this.dispatchEvent(new OrMapPresetFilterEvent(filter));
    }
}
