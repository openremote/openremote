/*
 * Copyright 2026, OpenRemote Inc.
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
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import {css, html, PropertyValues, TemplateResult } from "lit";
import {OrAssetWidget} from "../util/or-asset-widget";
import {AssetWidgetConfig} from "../util/widget-config";
import {AttributeRef, WellknownMetaItems} from "@openremote/model";
import {WidgetManifest} from "../util/or-widget";
import {WidgetSettings} from "../util/widget-settings";
import { customElement, query, queryAll, state } from "lit/decorators.js";
import {AttributeInputSettings} from "../settings/attribute-input-settings";
import { when } from "lit/directives/when.js";
import {throttle} from "lodash";
import {Util} from "@openremote/core";
import "@openremote/or-attribute-input";

export interface AttributeInputWidgetConfig extends AssetWidgetConfig {
    readonly: boolean,
    showHelperText: boolean
}

function getDefaultWidgetConfig() {
    return {
        attributeRefs: [],
        readonly: false,
        showHelperText: true
    } as AttributeInputWidgetConfig;
}

const styling = css`
  #widget-wrapper {
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    overflow: hidden;
  }
    
  #error-txt {
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    text-align: center;
  }

  .attr-input {
    width: 100%;
    box-sizing: border-box;
  }
`

@customElement("attribute-input-widget")
export class AttributeInputWidget extends OrAssetWidget {

    protected widgetConfig!: AttributeInputWidgetConfig;

    @state()
    protected _loading = false;

    @query("#widget-wrapper")
    protected widgetWrapperElem?: HTMLElement;

    @queryAll(".attr-input")
    protected attributeInputElems?: NodeList;

    protected resizeObserver?: ResizeObserver;

    static getManifest(): WidgetManifest {
        return {
            displayName: "Attribute",
            displayIcon: "form-textbox",
            getContentHtml(config: AttributeInputWidgetConfig): OrAssetWidget {
                return new AttributeInputWidget(config);
            },
            getDefaultConfig(): AttributeInputWidgetConfig {
                return getDefaultWidgetConfig();
            },
            getSettingsHtml(config: AttributeInputWidgetConfig): WidgetSettings {
                return new AttributeInputSettings(config);
            }

        }
    }

    // TODO: Improve this to be more efficient
    refreshContent(force: boolean): void {
        this.widgetConfig = JSON.parse(JSON.stringify(this.widgetConfig)) as AttributeInputWidgetConfig;
    }

    static get styles() {
        return [...super.styles, styling];
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.resizeObserver?.disconnect();
        delete this.resizeObserver;
    }

    protected willUpdate(changedProps: PropertyValues) {

        // If widgetConfig, and the attributeRefs of them have changed...
        if(changedProps.has("widgetConfig") && this.widgetConfig) {
            const attributeRefs = this.widgetConfig.attributeRefs;
            if(attributeRefs.length === 0) {
                this._error = "noAttributesConnected";
            } else if(attributeRefs.length > 0 && !this.isAttributeRefLoaded(attributeRefs[0])) {
                this.loadAssets(attributeRefs);
            }
        }

        // Workaround for an issue with scalability of or-attribute-input when using 'display: flex'.
        // The percentage slider doesn't scale properly, causing the dragging knob to glitch.
        // Why? Because the Material Design element listens to a window resize, not a container resize.
        // So we manually trigger this event when the attribute-input-widget changes in size.
        if(!this.resizeObserver && this.widgetWrapperElem) {
            this.resizeObserver = new ResizeObserver(throttle(() => {
                window.dispatchEvent(new Event('resize'));
            }, 200));
            this.resizeObserver.observe(this.widgetWrapperElem);
        }

        return super.willUpdate(changedProps);
    }

    protected loadAssets(attributeRefs: AttributeRef[]) {
        if(attributeRefs.length === 0) {
            this._error = "noAttributesConnected";
            return;
        }
        this._loading = true;
        this._error = undefined;
        this.fetchAssets(attributeRefs).then((assets) => {
            this.loadedAssets = assets;
        }).catch(e => {
            this._error = e.message;
        }).finally(() => {
            this._loading = false;
        });
    }

    protected render(): TemplateResult {
        const config = this.widgetConfig;
        const attribute = (config.attributeRefs.length > 0 && this.loadedAssets[0]?.attributes) ? this.loadedAssets[0].attributes[config.attributeRefs[0].name!] : undefined;
        const readOnlyMetaItem = Util.getMetaValue(WellknownMetaItems.READONLY, attribute);
        return html`
            
            ${when(this._loading || this._error, () => {
                if(this._loading) {
                    return html`<or-loading-indicator></or-loading-indicator>`;
                } else {
                    return html`<or-translate id="error-txt" .value="${this._error}"></or-translate>`;
                }
            }, () => when(config.attributeRefs.length > 0 && attribute && this.loadedAssets && this.loadedAssets.length > 0, () => {
                return html`
                    <div id="widget-wrapper">
                        <or-attribute-input class="attr-input" fullWidth
                                            .assetType="${this.loadedAssets[0]?.type}"
                                            .attribute="${attribute}"
                                            .assetId="${this.loadedAssets[0]?.id}"
                                            .disabled="${!this.loadedAssets}"
                                            .readonly="${config.readonly || readOnlyMetaItem || this.getEditMode!()}"
                                            .hasHelperText="${config.showHelperText}"
                        ></or-attribute-input>
                    </div>
                `;
            }))}
        `;
    }
}
