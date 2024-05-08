import {customElement} from "lit/decorators.js";
import {OrWidget, WidgetManifest} from "../util/or-widget";
import {html, PropertyValues, TemplateResult} from "lit";
import {WidgetSettings} from "../util/widget-settings";
import {WidgetConfig} from "../util/widget-config";
import {GatewaySettings} from "../settings/gateway-settings";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {GatewayTunnelInfo, GatewayTunnelInfoType} from "@openremote/model";
import manager from "@openremote/core";
import {when} from "lit/directives/when.js";

export interface GatewayWidgetConfig extends WidgetConfig {
    gatewayId?: string;
    type: GatewayTunnelInfoType;
    target: string;
    targetPort: number;
}

function getDefaultWidgetConfig(): GatewayWidgetConfig {
    return {
        type: GatewayTunnelInfoType.HTTPS,
        target: "localhost",
        targetPort: 443,
    };
}

@customElement("gateway-widget")
export class GatewayWidget extends OrWidget {

    protected widgetConfig!: GatewayWidgetConfig;

    protected _loading = false;
    protected _activeTunnel?: GatewayTunnelInfo;

    static getManifest(): WidgetManifest {
        return {
            displayName: "Gateway",
            displayIcon: "lan-connect",
            minColumnWidth: 1,
            minColumnHeight: 1,
            getContentHtml(config: GatewayWidgetConfig): OrWidget {
                return new GatewayWidget(config);
            },
            getSettingsHtml(config: GatewayWidgetConfig): WidgetSettings {
                return new GatewaySettings(config);
            },
            getDefaultConfig(): GatewayWidgetConfig {
                return getDefaultWidgetConfig();
            }
        }
    }

    refreshContent(force: boolean): void {
        this.widgetConfig = JSON.parse(JSON.stringify(this.widgetConfig)) as GatewayWidgetConfig;
    }

    protected willUpdate(changedProps: PropertyValues) {
        if(changedProps.has("_activeTunnel") && !!this._activeTunnel) {
            this._navigateToTunnel(this._activeTunnel);
        }
    }

    protected render(): TemplateResult {
        const disabled = this.getEditMode?.() || !this._isConfigComplete(this.widgetConfig);
        return html`
            <div style="height: 100%; display: flex; justify-content: center; align-items: center;">
                ${when(this._loading, () => html`
                    <or-loading-indicator></or-loading-indicator>
                `, () => {
                    return html`
                        <or-mwc-input .type="${InputType.BUTTON}" label="Open" outlined .disabled="${disabled}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._onStartTunnelClick(ev)}"
                        ></or-mwc-input>
                    `;
                })}
            </div>
        `;
    }

    protected _onStartTunnelClick(ev: OrInputChangedEvent) {
        this._tryStartTunnel(this.widgetConfig);
    }

    protected _isConfigComplete(widgetConfig: GatewayWidgetConfig): boolean {
        if (!widgetConfig.gatewayId) {
            console.error("Could not start tunnel, since no gateway asset was configured.");
            return false;
        }
        if (!widgetConfig.type) {
            console.error("Could not start tunnel, since no gateway type / protocol was configured.");
            return false;
        }
        if (!widgetConfig.target) {
            console.error("Could not start tunnel, since no gateway target was configured.");
            return false;
        }
        if (!widgetConfig.targetPort) {
            console.error("Could not start tunnel, since no gateway target port was configured.");
            return false;
        }
        return true;
    }

    protected _tryStartTunnel(widgetConfig: GatewayWidgetConfig): void {
        if (this._isConfigComplete(widgetConfig)) {

            const tunnelInfo = {
                realm: manager.displayRealm,
                gatewayId: widgetConfig.gatewayId,
                type: widgetConfig.type,
                target: widgetConfig.target,
                targetPort: widgetConfig.targetPort
            } as GatewayTunnelInfo;

            this._loading = true;

            this._getActiveTunnel(tunnelInfo).then(activeTunnel => {

                if (activeTunnel) {
                    console.log("Active tunnel!", activeTunnel);
                    this._activeTunnel = activeTunnel;
                    this._loading = false;

                } else {
                    this._startTunnel(tunnelInfo).then(newTunnel => {

                        if (newTunnel) {
                            console.log("New tunnel!", newTunnel)
                            this._activeTunnel = newTunnel;

                        } else {
                            console.warn("No new tunnel!");
                        }
                    }).catch(e => {
                        console.warn(e);
                    }).finally(() => {
                        this._loading = false;
                    });
                }

            }).catch(e => {
                console.warn(e);
                this._loading = false;
            })
        }
    }

    protected async _getActiveTunnel(info: GatewayTunnelInfo): Promise<GatewayTunnelInfo | undefined> {
        if (!info.realm || !info.gatewayId || !info.target || !info.targetPort) {
            throw new Error("Could not get active tunnel, as some provided information was not set.")
        }
        return (await manager.rest.api.GatewayServiceResource.getActiveTunnelInfo(info.realm, info.gatewayId, info.target, info.targetPort)).data;
    }

    protected async _startTunnel(info: GatewayTunnelInfo): Promise<GatewayTunnelInfo | undefined> {
        if (!info.realm || !info.gatewayId || !info.target || !info.targetPort) {
            throw new Error("Could not start tunnel, as some provided information was not set.");
        }
        return (await manager.rest.api.GatewayServiceResource.startTunnel(info)).data;
    }

    protected _navigateToTunnel(info: GatewayTunnelInfo): void {
        if (!info.realm || !info.gatewayId || !info.target || !info.targetPort) {
            console.warn("Could not navigate to tunnel, as some provided information was not set.");
        }
        switch (info.type) {
            case GatewayTunnelInfoType.HTTPS:
            case GatewayTunnelInfoType.HTTP:
                window.open("https://" + info.id + "." + info.target)?.focus();
                break;
            case GatewayTunnelInfoType.TCP:
                console.log("TCP support WIP");
                break;
            default:
                console.log("Unknown error.");
                break;
        }
    }

}
