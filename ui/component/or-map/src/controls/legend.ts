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
import { css, CSSResultGroup, html, LitElement, PropertyValues, TemplateResult } from "lit";
import { countBadgeStyle } from "./styles";
import { customElement, property, state } from "lit/decorators.js";
import { AssetModelUtil } from "@openremote/model";
import { formatCount, getMarkerIconAndColorFromAssetType } from "../util";
import { Util } from "@openremote/core";
import type { Map as MapGL } from "maplibre-gl";
import { OrMapBaseControl } from "./base";
import "@openremote/or-vaadin-components/or-vaadin-checkbox";
import "@openremote/or-vaadin-components/or-vaadin-badge";
import "@openremote/or-icon";
import "@openremote/or-vaadin-components/or-vaadin-list-box";
import "@openremote/or-vaadin-components/or-vaadin-item";

export class OrMapLegendEvent extends CustomEvent<string[]> {
    public static readonly NAME = "or-map-legend-changed";

    constructor(assetTypes: string[]) {
        super(OrMapLegendEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: assetTypes,
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrMapLegendEvent.NAME]: OrMapLegendEvent;
    }
}

@customElement("or-map-legend")
export class OrMapLegend extends LitElement {

    static get styles(): CSSResultGroup {
        return [countBadgeStyle, css`
            :host {
                display: block;
            }

            #legend {
                display: flex;
                flex-direction: column;
                min-width: 220px;
            }

            #legend-title {
                height: 38px;
                line-height: 38px;
                display: flex;
                justify-content: space-between;
                align-items: center;
                border-radius: var(--lumo-border-radius-m);
                font-family: var(--lumo-font-family);
                font-size: 16px;
                color: black;
                padding: 0 20px;
            }
            #legend-title.expanded {
                border-radius: var(--lumo-border-radius-m) var(--lumo-border-radius-m) 0 0;
            }

            or-icon {
                --or-icon-width: 18px;
                --or-icon-height: 18px;
                margin-right: 4px;
            }

            or-vaadin-item {
                min-height: 30px;
                padding: 4px 16px;
                margin: 0 4px;
                box-sizing: border-box;
                font-size: var(--lumo-font-size-m);
                /* Remove hover highlight */
                --lumo-primary-color-10pct: transparent;
                cursor: default;
            }
            or-vaadin-item:last-child {
                margin-bottom: 4px;
            }

            or-vaadin-item::part(checkmark) {
                display: none;
            }

            /* List: flat top connecting to title, rounded bottom */
            #legend-list {
                min-width: 220px;
                margin: 0;
                background: var(--lumo-base-color, white);
                border-radius: 0 0 var(--lumo-border-radius-m) var(--lumo-border-radius-m);
                max-height: 0;
                overflow: hidden;
                opacity: 0;
                transition: max-height 0.14s ease, opacity 0.1s ease;
            }

            #legend-list.expanded {
                max-height: 600px;
                opacity: 1;
            }
        `];
    }

    @property({ type: Array })
    public assetTypes: string[] = [];

    @property({ type: Array })
    public excludedTypes: string[] = [];

    @property({ type: Object })
    public assetCounts: Record<string, number> = {};

    @state()
    private _contentHidden = true;

    protected _assetTypesInfo: any = {};

    protected shouldUpdate(changedProperties: PropertyValues): boolean {
        if (changedProperties.has("assetTypes")) {
            this._assetTypesInfo = {};
            this.assetTypes.forEach((assetType: string) => {
                const descriptor = AssetModelUtil.getAssetDescriptor(assetType);
                this._assetTypesInfo[assetType] = {
                    icon: getMarkerIconAndColorFromAssetType(descriptor)?.icon,
                    color: getMarkerIconAndColorFromAssetType(descriptor)?.color,
                    label: Util.getAssetTypeLabel(descriptor),
                };
            });
        }
        return super.shouldUpdate(changedProperties);
    }

    protected render(): TemplateResult | typeof html {
        if (this.assetTypes.length < 2) return html``;
        const sortedTypes = [...this.assetTypes].sort((a, b) =>
            (this._assetTypesInfo[a]?.label ?? a).localeCompare(this._assetTypesInfo[b]?.label ?? b)
        );
        return html`
            <div id="legend">
                <div id="legend-title" class="${!this._contentHidden ? 'expanded' : ''}" @click="${() => { this._contentHidden = !this._contentHidden; }}">
                    <or-translate value="mapPage.legendTitle"></or-translate>
                    <or-icon style="cursor: pointer" icon="${this._contentHidden ? 'mdi:chevron-up' : 'mdi:chevron-down'}"></or-icon>
                </div>
                <or-vaadin-list-box id="legend-list" class="${this._contentHidden ? '' : 'expanded'}">
                    ${sortedTypes.map((assetType) => html`
                        <or-vaadin-item>
                            <div style="display: flex; align-items: center; width: 100%; gap: 6px; min-width: 200px;">
                                <or-icon
                                    icon="${this._assetTypesInfo[assetType].icon}"
                                    style="color: #${this._assetTypesInfo[assetType].color}; flex-shrink: 0; --or-icon-width: 20px; --or-icon-height: 20px;"
                                ></or-icon>
                                <span style="flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                                    ${this._assetTypesInfo[assetType].label}
                                </span>
                                <or-vaadin-badge style="flex-shrink: 0;">${formatCount(this.assetCounts[assetType] ?? 0)}</or-vaadin-badge>
                                <or-vaadin-checkbox
                                    .checked="${!this.excludedTypes.includes(assetType)}"
                                    @click="${(e: Event) => e.stopPropagation()}"
                                    @change="${(ev: Event) => {
                                        const checked = (ev.currentTarget as any).checked;
                                        if (checked) {
                                            this.excludedTypes = this.excludedTypes.filter(t => t !== assetType);
                                        } else {
                                            this.excludedTypes = [...this.excludedTypes, assetType];
                                        }
                                        this.dispatchEvent(new OrMapLegendEvent(this.excludedTypes));
                                    }}"
                                ></or-vaadin-checkbox>
                            </div>
                        </or-vaadin-item>
                    `)}
                </or-vaadin-list-box>
            </div>
        `;
    }
}

export class OrMapLegendControl extends OrMapBaseControl {
    private _component?: OrMapLegend;

    constructor(
        private _assetTypes: string[],
        private _excludedTypes: string[],
        private _assetCounts: Record<string, number>
    ) { super(); }

    onAdd(_map: MapGL): HTMLElement {
        this._createContainer();
        this._container!.classList.add("legend-control");
        this._component = document.createElement("or-map-legend") as OrMapLegend;
        this._component.assetTypes = this._assetTypes;
        this._component.excludedTypes = this._excludedTypes;
        this._component.assetCounts = this._assetCounts;
        this._container!.appendChild(this._component);
        return this._container!;
    }

    onRemove(): void {
        super.onRemove();
        this._component = undefined;
    }

    set assetTypes(types: string[]) {
        this._assetTypes = types;
        if (this._component) this._component.assetTypes = types;
    }

    set excludedTypes(types: string[]) {
        this._excludedTypes = types;
        if (this._component) this._component.excludedTypes = types;
    }

    set assetCounts(counts: Record<string, number>) {
        this._assetCounts = counts;
        if (this._component) this._component.assetCounts = counts;
    }

    get element(): OrMapLegend | undefined {
        return this._component;
    }
}
