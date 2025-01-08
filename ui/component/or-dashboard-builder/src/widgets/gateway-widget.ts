import {customElement, state} from "lit/decorators.js";
import {OrWidget, WidgetManifest} from "../util/or-widget";
import {css, html, PropertyValues, TemplateResult} from "lit";
import {WidgetSettings} from "../util/widget-settings";
import {WidgetConfig} from "../util/widget-config";
import {GatewaySettings} from "../settings/gateway-settings";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {GatewayTunnelInfo, GatewayTunnelInfoType} from "@openremote/model";
import manager from "@openremote/core";
import {when} from "lit/directives/when.js";
import {i18next} from "@openremote/or-translate";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import moment from "moment";

const styling = css`
    #gateway-widget-wrapper {
        height: 100%;
        display: flex;
        justify-content: center;
        align-items: center;
        overflow: hidden;
    }

    #gateway-widget-container {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        max-height: 48px;
        flex-wrap: wrap;
        position: relative;
    }
`;

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
        targetPort: 443
    };
}

@customElement("gateway-widget")
export class GatewayWidget extends OrWidget {

    protected widgetConfig!: GatewayWidgetConfig;

    @state()
    protected _loading = true;

    /**
     * Cache of the active {@link GatwayTunnelInfo} we receive from the HTTP API.
     * It contains all necessary information, such as ID, assigned port, target information, and autoCloseTime.
     */
    @state()
    protected _activeTunnel?: GatewayTunnelInfo;

    protected _startedByUser = false;

    protected _refreshTimer?: number = undefined;

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
        };
    }

    refreshContent(force: boolean): void {
        this.widgetConfig = JSON.parse(JSON.stringify(this.widgetConfig)) as GatewayWidgetConfig;
    }

    static get styles() {
        return [...super.styles, styling];
    }

    disconnectedCallback() {
        if(this._activeTunnel) {
            if(this._startedByUser) {
                this._stopTunnel(this._activeTunnel).then(() => {
                    console.warn("Stopped the active tunnel, as it was created through the widget.");
                });
            } else {
                console.warn("Keeping the active tunnel open, as it is not started through the widget.");
            }
        }

        if (this._refreshTimer) {
            clearTimeout(this._refreshTimer);
        }

        super.disconnectedCallback();
    }

    protected firstUpdated(changedProps: PropertyValues) {
        if(this.widgetConfig) {

            // Apply a timeout of 500 millis, so the tunnel has time to close upon disconnectedCallback() of a different widget.
            setTimeout(() => {

                // Check if the tunnel is already active upon widget initialization
                const tunnelInfo = this._getTunnelInfoByConfig(this.widgetConfig);
                this._getActiveTunnel(tunnelInfo).then(info => {
                    if(info) {
                        console.log("Existing tunnel found!", info);
                        this._setActiveTunnel(info, true)
                    }
                }).catch(e => {
                    console.error(e);
                }).finally(() => {
                    this._loading = false;
                });

            }, 500);
        }
        return super.firstUpdated(changedProps);
    }

    protected render(): TemplateResult {
        const disabled = this.getEditMode?.() || !this._isConfigComplete(this.widgetConfig);
        return html`
            <div id="gateway-widget-wrapper">
                <div id="gateway-widget-container">
                    ${when(this._loading, () => html`
                        <or-loading-indicator></or-loading-indicator>
                    `, () => {
                        if (this._activeTunnel) {
                            return html`
                                <div>
                                <or-mwc-input .type="${InputType.BUTTON}" icon="stop" label="${i18next.t('gatewayTunnels.stop')}" .disabled="${disabled}"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._onStopTunnelClick(ev)}"
                                ></or-mwc-input>
                                
                                ${when(this.widgetConfig.type === GatewayTunnelInfoType.TCP, () => html`
                                    <or-mwc-input .type="${InputType.BUTTON}" icon="content-copy" label="${i18next.t('gatewayTunnels.copyAddress')}" outlined .disabled="${disabled}"
                                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._onCopyTunnelAddressClick(ev)}"
                                    ></or-mwc-input>
                                `, () => html`
                                    <or-mwc-input .type="${InputType.BUTTON}" icon="open-in-new" label="${i18next.t('gatewayTunnels.open')}" outlined .disabled="${disabled}"
                                                  @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._onTunnelNavigateClick(ev, this._activeTunnel)}"
                                    ></or-mwc-input>
                                `)}
                                </div>
                                ${when(this._activeTunnel?.autoCloseTime, () => html`
                                    <div><or-translate value="gatewayTunnels.closesAt"></or-translate>: ${moment(this._activeTunnel?.autoCloseTime).format("lll")}</div>
                                `)}
                            `;
                        } else {
                            return html`
                                <or-mwc-input .type="${InputType.BUTTON}" label="${i18next.t('gatewayTunnels.start')}" outlined .disabled="${disabled}"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._onStartTunnelClick(ev)}"
                                ></or-mwc-input>
                            `;
                        }
                    })}
                </div>
            </div>
        `;
    }

    /**
     * HTML callback function when 'copy address' button is pressed for a TCP tunnel.
     */
    protected _onCopyTunnelAddressClick(ev: OrInputChangedEvent) {
        if (this._isConfigComplete(this.widgetConfig)) {
            const tunnelInfo = this._getTunnelInfoByConfig(this.widgetConfig);
            const address = this._getTunnelAddress(tunnelInfo);
            if(address) {
                navigator.clipboard.writeText(address).finally(() => {
                    showSnackbar(undefined, i18next.t('gatewayTunnels.copySuccess'));
                });
            } else {
                console.warn("Could not copy tunnel address as it could not be found.");
                showSnackbar(undefined, i18next.t('errorOccurred'));
            }
        } else {
            console.warn("Could not copy tunnel address as configuration is not complete.");
            showSnackbar(undefined, i18next.t('errorOccurred'));
        }
    }

    /**
     * HTML callback function when 'start' button is pressed, meant to create / start a new tunnel.
     */
    protected _onStartTunnelClick(ev: OrInputChangedEvent) {
        this._tryStartTunnel(this.widgetConfig);
    }

    /**
     * HTML callback function when 'stop' button is pressed, meant to destroy the active tunnel.
     */
    protected _onStopTunnelClick(ev: OrInputChangedEvent) {
        this._tryStopTunnel(this.widgetConfig);
    }

    /**
     * HTML callback function when 'open' button is pressed, meant to start using the tunnel.
     */
    protected _onTunnelNavigateClick(ev: OrInputChangedEvent, activeTunnel?: GatewayTunnelInfo) {
        if(activeTunnel) {
            this._navigateToTunnel(activeTunnel);
        } else {
            console.warn("Could not navigate to tunnel as configuration is not complete.")
        }
    }

    /**
     * Internal function to set the active tunnel.
     * Navigates the user to the tunnel after updating. This can be disabled using the {@link silent} parameter.
     */
    protected _setActiveTunnel(tunnelInfo?: GatewayTunnelInfo, silent = false) {
        this._activeTunnel = tunnelInfo;

        if (this._refreshTimer) {
            clearTimeout(this._refreshTimer);
        }

        if (tunnelInfo?.autoCloseTime) {
            const timeout = tunnelInfo?.autoCloseTime - Date.now();
            if (timeout > 0) {
                this._refreshTimer = window.setTimeout(() => {
                    this._getActiveTunnel(this._getTunnelInfoByConfig(this.widgetConfig)).then(info => {
                        if (info) {
                            this._setActiveTunnel(info, true)
                        } else {
                            this._setActiveTunnel(undefined);
                        }
                    });
                }, timeout);
            }
        }

        if(tunnelInfo && !silent) {
            this._navigateToTunnel(tunnelInfo);
        }
    }

    /**
     * Function that tries to start the tunnel. It checks the configuration beforehand,
     * and acts as a controller to call the correct functions throughout the starting process.
     */
    protected _tryStartTunnel(widgetConfig: GatewayWidgetConfig): void {
        if (this._isConfigComplete(widgetConfig)) {

            const tunnelInfo = this._getTunnelInfoByConfig(widgetConfig);
            this._loading = true;

            this._getActiveTunnel(tunnelInfo).then(activeTunnel => {

                if (activeTunnel) {
                    console.log("Found an active tunnel!", activeTunnel);
                    this._setActiveTunnel(activeTunnel);
                    this._loading = false;

                } else {
                    this._startTunnel(tunnelInfo).then(newTunnel => {

                        if (newTunnel) {
                            console.log("Started a new tunnel!", newTunnel);
                            this._setActiveTunnel(newTunnel);
                            this._startedByUser = true;

                        } else {
                            console.warn("No new tunnel!");
                        }
                    }).catch(e => {
                        console.error(e);
                        showSnackbar(undefined, i18next.t("errorOccurred"));
                    }).finally(() => {
                        this._loading = false;
                    });
                }

            }).catch(e => {
                console.error(e);
                showSnackbar(undefined, i18next.t("errorOccurred"));
                this._loading = false;
            })
        }
    }

    /**
     * Internal function that starts the tunnel by communicating with the Manager API.
     */
    protected async _startTunnel(info: GatewayTunnelInfo): Promise<GatewayTunnelInfo | undefined> {
        if (!info.realm || !info.gatewayId || !info.target || !info.targetPort) {
            throw new Error("Could not start tunnel, as some provided information was not set.");
        }
        return (await manager.rest.api.GatewayServiceResource.startTunnel(info)).data;
    }

    /**
     * Internal function that requests the Manager API for the active tunnel based on the {@link GatewayTunnelInfo} parameter.
     * Returns `undefined` if no tunnel could be found on the Manager instance.
     */
    protected async _getActiveTunnel(info: GatewayTunnelInfo): Promise<GatewayTunnelInfo | undefined> {
        if (!info.realm || !info.gatewayId || !info.target || !info.targetPort) {
            throw new Error("Could not get active tunnel, as some provided information was not set.")
        }
        const response = await manager.rest.api.GatewayServiceResource.getActiveTunnelInfo(info.realm, info.gatewayId, info.target, info.targetPort);
        if(response.status === 204) {
            return undefined;
        } else {
            return response.data;
        }
    }

    /**
     * Function that tries to destroy the currently active tunnel.
     */
    protected _tryStopTunnel(config: GatewayWidgetConfig): void {
        const tunnelInfo = this._getTunnelInfoByConfig(config);
        this._loading = true;
        this._stopTunnel(tunnelInfo)
            .then(() => {
                this._setActiveTunnel(undefined);
                this._startedByUser = false;
            })
            .catch(e => {
                console.warn(e);
            })
            .finally(() => {
                this._loading = false;
            });
    }

    /**
     * Internal function that requests the Manager API to destroy a tunnel that is in line with the {@link GatewayTunnelInfo} parameter.
     */
    protected async _stopTunnel(info: GatewayTunnelInfo): Promise<void> {
        return (await manager.rest.api.GatewayServiceResource.stopTunnel(info)).data;
    }

    /**
     * Function that navigates the user to an HTTP web page that interacts with a service through the tunnel.
     * It will open in a new browser tab automatically.
     */
    protected _navigateToTunnel(info: GatewayTunnelInfo): void {
        if (!info.id || !info.realm || !info.gatewayId || !info.target || !info.targetPort) {
            console.warn("Could not navigate to tunnel, as some provided information was not set.");
        }
        const address = this._getTunnelAddress(info);
        switch (info.type) {
            case GatewayTunnelInfoType.HTTPS:
            case GatewayTunnelInfoType.HTTP:
                window.open(address)?.focus();
                break;
            default:
                console.error("Unknown error when navigating to tunnel.");
                break;
        }
    }

    /**
     * Internal function to get the tunnel address based on {@link GatewayTunnelInfo}
     * WARNING: Could return incorrect address if not all fields are set.
     */
    protected _getTunnelAddress(info: GatewayTunnelInfo): string | undefined {
        switch (info.type) {
            case GatewayTunnelInfoType.HTTPS:
            case GatewayTunnelInfoType.HTTP:
                return "//" + info.id + "." + (info.hostname ? info.hostname : window.location.hostname);
            case GatewayTunnelInfoType.TCP:
                return (info.hostname ? info.hostname : window.location.hostname) + ":" + info.assignedPort;
        }
    }

    /**
     * Internal function to check whether the {@link GatewayWidgetConfig} includes the necessary information to control the tunnel.
     * Uses several `undefined` or empty checks. Useful for checking the object before interacting with Manager APIs.
     */
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

    /**
     * Internal function that parses a {@link GatewayWidgetConfig} into a new {@link GatewayTunnelInfo}.
     * Be aware: this does not information received from the HTTP API such as ID, assigned port, etc. Please use {@link _activeTunnel} for this.
     */
    protected _getTunnelInfoByConfig(config: GatewayWidgetConfig): GatewayTunnelInfo {
        return {
            realm: manager.displayRealm,
            gatewayId: config.gatewayId,
            type: config.type,
            target: config.target,
            targetPort: config.targetPort
        } as GatewayTunnelInfo;
    }

}
