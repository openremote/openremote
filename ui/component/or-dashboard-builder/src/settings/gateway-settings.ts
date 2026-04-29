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
import { customElement } from "lit/decorators.js";
import {WidgetSettings} from "../util/widget-settings";
import {TemplateResult, html} from "lit";
import {i18next} from "@openremote/or-translate";
import {Asset, AssetQuery, AttributeRef, GatewayTunnelInfoType} from "@openremote/model";
import {GatewayWidgetConfig} from "../widgets/gateway-widget";
import {Task} from "@lit/task";
import manager from "@openremote/core";
import {OrVaadinSelect, SelectItem} from "@openremote/or-vaadin-components/or-vaadin-select";
import {OrVaadinNumberField} from "@openremote/or-vaadin-components/or-vaadin-number-field";

@customElement("gateway-settings")
export class GatewaySettings extends WidgetSettings {

    protected GATEWAY_ASSET_TYPES: string[] = ["GatewayAsset"];
    protected GATEWAY_TUNNEL_TYPES: GatewayTunnelInfoType[] = [GatewayTunnelInfoType.HTTPS, GatewayTunnelInfoType.HTTP, GatewayTunnelInfoType.TCP];

    // Override of widgetConfig with extended type
    protected readonly widgetConfig!: GatewayWidgetConfig;

    connectedCallback() {
        super.connectedCallback();
    }

    protected render(): TemplateResult {
        const disabled = !this.widgetConfig.gatewayId;
        const tunnelTypeItems: SelectItem[] = this.GATEWAY_TUNNEL_TYPES.map(t => ({value: t, label: i18next.t(t)}));
        return html`
            <div>
                <settings-panel displayName="gatewayTunnels.settings" expanded="${true}">
                    <div style="display: flex; flex-direction: column; gap: 8px;">
                        <div>
                            ${this._fetchAssetsTask.render({
                                pending: () => html`
                                    <or-vaadin-select disabled>
                                        <or-translate slot="label" value="gatewayTunnels.selectAsset"></or-translate>
                                    </or-vaadin-select>
                                `,
                                complete: (gatewayAssets) => {
                                    const options: SelectItem[] = gatewayAssets.map(a => ({value: a.id!, label: a.name! }));
                                    const selected = gatewayAssets.find(a => a.id === this.widgetConfig.gatewayId);
                                    const value = selected?.id;
                                    return html`
                                        <or-vaadin-select .items=${options} value=${value} @change=${(ev: Event) => this._onAssetSelect(ev)}>
                                            <or-translate slot="label" value="gatewayTunnels.selectAsset"></or-translate>
                                        </or-vaadin-select>
                                    `;
                                },
                                error: () => html`
                                    <or-vaadin-text-field readonly value=${i18next.t("errorOccurred")}>
                                        <or-translate slot="label" value="gatewayTunnels.selectAsset"></or-translate>
                                    </or-vaadin-text-field>
                                `
                            })}
                        </div>
                        <!-- Select tunnel type (HTTP/HTTPS/TCP) -->
                        <or-vaadin-select .items=${tunnelTypeItems} .value=${this.widgetConfig.type} ?disabled=${disabled}
                                          @change=${(ev: Event) => this._onTunnelTypeSelect(ev)}>
                            <or-translate slot="label" value="gatewayTunnels.protocol"></or-translate>
                        </or-vaadin-select>
                        <!-- Select tunnel target address -->
                        <or-vaadin-text-field value=${this.widgetConfig.target} ?disabled=${disabled}
                                              @change=${(ev: Event) => this._onTunnelTargetSelect(ev)}>
                            <or-translate slot="label" value="gatewayTunnels.target"></or-translate>
                        </or-vaadin-text-field>
                        <!-- Select tunnel target port -->
                        <or-vaadin-number-field value=${this.widgetConfig.targetPort} ?disabled=${disabled} min="0"
                                                @change=${(ev: Event) => this._onTunnelTargetPortSelect(ev)}>
                            <or-translate slot="label" value="gatewayTunnels.targetPort"></or-translate>
                        </or-vaadin-number-field>
                    </div>
                </settings-panel>
            </div>
        `;
    }

    protected _onAssetSelect(ev: Event): void {
        const elem = ev.currentTarget as OrVaadinSelect;
        this.widgetConfig.gatewayId = elem.value;
        this.widgetConfig.attributeRefs = [{id: elem.value, name: undefined} as AttributeRef]; // Add to attributeRefs object, so the backend is aware of asset permissions
        this.notifyConfigUpdate();
    }

    protected _onTunnelTypeSelect(ev: Event) {
        this.widgetConfig.type = (ev.currentTarget as OrVaadinSelect).value as GatewayTunnelInfoType;
        this.notifyConfigUpdate();
    }

    protected _onTunnelTargetSelect(ev: Event) {
        this.widgetConfig.target = (ev.currentTarget as OrVaadinSelect).value;
        this.notifyConfigUpdate();
    }

    protected _onTunnelTargetPortSelect(ev: Event) {
        const elem = ev.currentTarget as OrVaadinNumberField;
        if(elem.checkValidity()) {
            this.widgetConfig.targetPort = Number(elem.value);
            this.notifyConfigUpdate();
        }
    }


    protected _fetchAssetsTask = new Task(this, {
        task: async ([], {signal}) => {
            return await this._fetchAssets(true, signal);
        },
        args: () => []
    });

    protected async _fetchAssets(requireTunnelSupport = true, signal?: AbortSignal): Promise<Asset[]> {
        const query = {
            realm: {name: manager.displayRealm},
            select: {attributes: ["tunnelingSupported"]},
            types: this.GATEWAY_ASSET_TYPES,
            attributes: requireTunnelSupport ? {
                items: [
                    {
                        name: {
                            predicateType: "string",
                            value: "tunnelingSupported"
                        },
                        value: {
                            predicateType: "boolean",
                            value: true
                        },
                    },
                    {
                        name: {
                            predicateType: "string",
                            value: "gatewayStatus"
                        },
                        value: {
                            predicateType: "string",
                            value: "CONNECTED"
                        },
                    }
                ]
            } : undefined
        } as AssetQuery;
        return (await manager.rest.api.AssetResource.queryAssets(query)).data;
    }

}
