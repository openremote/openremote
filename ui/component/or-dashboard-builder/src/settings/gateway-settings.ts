import { customElement } from "lit/decorators.js";
import {WidgetSettings} from "../util/widget-settings";
import {TemplateResult, html} from "lit";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {i18next} from "@openremote/or-translate";
import {Asset, AssetQuery, AttributeRef, GatewayTunnelInfoType} from "@openremote/model";
import {GatewayWidgetConfig} from "../widgets/gateway-widget";
import {Task} from "@lit/task";
import manager from "@openremote/core";

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
        return html`
            <div>
                <settings-panel displayName="gatewayTunnels.settings" expanded="${true}">
                    <div style="display: flex; flex-direction: column; gap: 8px;">
                        <div>
                            ${this._fetchAssetsTask.render({
                                pending: () => html`
                                    <or-mwc-input .type="${InputType.SELECT}" compact style="width: 100%;" disabled
                                                  label="${i18next.t('gatewayTunnels.selectAsset')}"
                                    ></or-mwc-input>
                                `,
                                complete: (gatewayAssets) => {
                                    const options: [string, string][] = gatewayAssets.map(a => [a.id!, a.name!]);
                                    const selected = gatewayAssets.find(a => a.id === this.widgetConfig.gatewayId);
                                    const value = selected ? [selected.id, selected.name] as [string, string] : undefined;
                                    return html`
                                        <or-mwc-input .type="${InputType.SELECT}" compact style="width: 100%;"
                                                      .options="${options}" .value="${value}"
                                                      label="${i18next.t('gatewayTunnels.selectAsset')}"
                                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._onAssetSelect(ev)}"
                                        ></or-mwc-input>
                                    `;
                                },
                                error: () => html`
                                    <or-mwc-input .type="${InputType.TEXT}" compact style="width: 100%;" disabled
                                                  label="${i18next.t('gatewayTunnels.selectAsset')}" .value="${i18next.t('errorOccurred')}"
                                    ></or-mwc-input>
                                `
                            })}
                        </div>
                        <div>
                            <or-mwc-input .type="${InputType.SELECT}" compact style="width: 100%;"
                                          .disabled="${disabled}" .options="${this.GATEWAY_TUNNEL_TYPES}" .value="${this.widgetConfig.type}"
                                          label="${i18next.t('gatewayTunnels.protocol')}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._onTunnelTypeSelect(ev)}"
                            ></or-mwc-input>
                        </div>
                        <div>
                            <or-mwc-input .type="${InputType.TEXT}" style="width: 100%;"
                                          .disabled="${disabled}" .value="${this.widgetConfig.target}"
                                          label="${i18next.t('gatewayTunnels.target')}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._onTunnelTargetSelect(ev)}"
                            ></or-mwc-input>
                        </div>
                        <div>
                            <or-mwc-input .type="${InputType.NUMBER}" style="width: 100%;"
                                          .disabled="${disabled}" .value="${this.widgetConfig.targetPort}"
                                          label="${i18next.t('gatewayTunnels.targetPort')}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._onTunnelTargetPortSelect(ev)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </settings-panel>
            </div>
        `;
    }

    protected _onAssetSelect(ev: OrInputChangedEvent): void {
        this.widgetConfig.gatewayId = ev.detail.value;
        this.widgetConfig.attributeRefs = [{id: ev.detail.value, name: undefined} as AttributeRef]; // Add to attributeRefs object, so the backend is aware of asset permissions
        this.notifyConfigUpdate();
    }

    protected _onTunnelTypeSelect(ev: OrInputChangedEvent) {
        this.widgetConfig.type = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected _onTunnelTargetSelect(ev: OrInputChangedEvent) {
        this.widgetConfig.target = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected _onTunnelTargetPortSelect(ev: OrInputChangedEvent) {
        this.widgetConfig.targetPort = ev.detail.value;
        this.notifyConfigUpdate();
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
