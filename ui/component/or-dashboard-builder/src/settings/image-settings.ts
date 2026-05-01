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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import {css, html, PropertyValues, TemplateResult } from "lit";
import { customElement} from "lit/decorators.js";
import {ImageWidgetConfig} from "../widgets/image-widget";
import {AttributesSelectEvent} from "../panels/attributes-panel";
import {AssetModelUtil} from "@openremote/model";
import { map } from "lit/directives/map.js";
import {Util} from "@openremote/core";
import { when } from "lit/directives/when.js";
import {AssetWidgetSettings} from "../util/or-asset-widget";
import {OrVaadinTextField} from "@openremote/or-vaadin-components/or-vaadin-text-field";
import {OrVaadinNumberField} from "@openremote/or-vaadin-components/or-vaadin-number-field";

const styling = css`
  #marker-container {
    display: flex;
    justify-content: flex-end;
    align-items: center;
  }
`;

@customElement("image-settings")
export class ImageSettings extends AssetWidgetSettings {

    // Override of widgetConfig with extended type
    protected readonly widgetConfig!: ImageWidgetConfig;

    static get styles() {
        return [...super.styles, styling]
    }

    protected willUpdate(changedProps: PropertyValues) {
        super.willUpdate(changedProps);
        if(changedProps.has('widgetConfig') && this.widgetConfig) {
            this.updateCoordinateMap(this.widgetConfig);
            this.loadAssets();
        }
    }

    protected loadAssets() {
        const missingAssets = this.widgetConfig.attributeRefs.filter(ref => !this.isAttributeRefLoaded(ref));
        if(missingAssets.length > 0) {
            this.fetchAssets(this.widgetConfig.attributeRefs).then((assets) => {
                if(assets === undefined) {
                    this.loadedAssets = [];
                } else {
                    this.loadedAssets = assets;
                }
            });
        }
    }

    protected render(): TemplateResult {
        return html`
            <div>
                <!-- Attributes selector -->
                <settings-panel displayName="attributes" expanded="${true}">
                    <attributes-panel .attributeRefs="${this.widgetConfig.attributeRefs}" onlyDataAttrs="${false}" multi="${true}"
                                      @attribute-select="${(ev: AttributesSelectEvent) => this.onAttributesSelect(ev)}"
                    ></attributes-panel>
                </settings-panel>
                
                <!-- Marker coordinates -->
                <settings-panel displayName="dashboard.markerCoordinates" expanded="${true}">
                    <div style="display: flex; flex-direction: column; gap: 8px;">
                        ${map(this.draftCoordinateEntries(this.widgetConfig), template => template)}
                    </div>
                </settings-panel>
                
                <!-- Image settings -->
                <settings-panel displayName="dashboard.imageSettings" expanded="${true}">
                    <or-vaadin-text-field value=${this.widgetConfig.imagePath} @change=${(ev: Event) => this.onImageUrlUpdate(ev)}>
                        <or-translate slot="label" value="dashboard.imageUrl"></or-translate>
                    </or-vaadin-text-field>
                </settings-panel>
            </div>
        `;
    }

    protected onAttributesSelect(ev: AttributesSelectEvent) {
        this.widgetConfig.attributeRefs = ev.detail.attributeRefs;
        this.notifyConfigUpdate();
    }

    protected onImageUrlUpdate(ev: Event) {
        const elem = ev.currentTarget as OrVaadinTextField;
        if(elem.checkValidity()) {
            this.widgetConfig.imagePath = elem.value;
            this.notifyConfigUpdate();
        }
    }


    /* -------------------------------------- */

    // updates coordinate map according to the attributeRef entries per id
    updateCoordinateMap(config: ImageWidgetConfig) {
        for (let i = 0; i < config.attributeRefs.length; i++) {
            const attributeRef = config.attributeRefs[i];
            if (attributeRef === undefined) {
                console.error('attributeRef is undefined');
                return;
            }
            const index = config.markers.findIndex(m => m.attributeRef.id === attributeRef.id && m.attributeRef.name === attributeRef.name);
            if (index === -1) {
                config.markers.push({
                    attributeRef: attributeRef,
                    coordinates: [50, 50]
                });
            }
        }
    }

    private draftCoordinateEntries(config: ImageWidgetConfig): TemplateResult[] {
        const min = 0;
        const max = 100;

        if (config.markers.length > 0) {
            return config.attributeRefs.map((attributeRef) => {
                const marker = config.markers.find(m => m.attributeRef.id === attributeRef.id && m.attributeRef.name === attributeRef.name);
                if(marker === undefined) {
                    console.error("A marker could not be found during drafting coordinate entries.");
                    return html``;
                }
                const index = config.markers.indexOf(marker);
                const coordinates = marker.coordinates;
                const asset = this.loadedAssets?.find(a => a.id === attributeRef.id);
                let label: string | undefined;
                if(asset) {
                    const attribute = asset.attributes![attributeRef.name!];
                    const descriptors = AssetModelUtil.getAttributeAndValueDescriptors(asset.type, attributeRef.name, attribute);
                    label = Util.getAttributeLabel(attribute, descriptors[0], asset.type, false);
                }
                return html`
                    <div id="marker-container">
                        <div style="flex: 1; display: flex; flex-direction: column;">
                            <span>${this.loadedAssets?.find(a => a.id === attributeRef.id)?.name}</span>
                            ${when(label, () => html`
                                <span style="color: gray;">${label}</span>
                            `)}
                        </div>
                        <div style="display: flex; gap: 8px;">
                            <or-vaadin-number-field value=${coordinates[0]} min=${min} max=${max} style="max-width: 64px;"
                                                    @change=${(ev: Event) => this.onCoordinateUpdate(index, "x", ev)}>
                                <span slot="suffix">%</span>
                            </or-vaadin-number-field>
                            <or-vaadin-number-field value=${coordinates[1]} min=${min} max=${max} style="max-width: 64px;"
                                                    @change=${(ev: Event) => this.onCoordinateUpdate(index, "y", ev)}>
                                <span slot="suffix">%</span>
                            </or-vaadin-number-field>
                        </div>
                    </div>
                `;
            });
        } else {
            return [
                html`<span><or-translate value="noAttributeConnected"></or-translate></span>`
            ];
        }
    }

    protected onCoordinateUpdate(index: number, coordinate: 'x' | 'y', ev: Event) {
        const elem = ev.currentTarget as OrVaadinNumberField;
        if(!elem.checkValidity()) {
            return;
        }
        let coords = this.widgetConfig.markers[index].coordinates;
        if(!coords) {
            coords = [0, 0];
        }
        if(coordinate === 'x') {
            coords[0] = Number(elem.value);
        } else if(coordinate === 'y') {
            coords[1] = Number(elem.value);
        }
        this.widgetConfig.markers[index].coordinates = coords;
        this.notifyConfigUpdate();
    }

}
