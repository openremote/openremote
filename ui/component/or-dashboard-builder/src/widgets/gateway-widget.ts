import {customElement, state, query} from "lit/decorators.js";
import {OrWidget, WidgetManifest} from "../util/or-widget";
import {css, html, PropertyValues, TemplateResult} from "lit";
import {WidgetSettings} from "../util/widget-settings";
import {WidgetConfig} from "../util/widget-config";
import {GatewaySettings} from "../settings/gateway-settings";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {GatewayTunnelInfo, GatewayTunnelInfoType} from "@openremote/model";
import manager from "@openremote/core";
import {when} from "lit/directives/when.js";
import moment from "moment";
import {i18next} from "@openremote/or-translate";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";

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
        gap: 4px;
        max-height: 48px;
        flex-wrap: wrap;
        position: relative;
    }
`

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

    protected _activeTunnelTimer?: NodeJS.Timeout;

    @state()
    protected _loading = false;

    @state()
    protected _activeTunnel?: GatewayTunnelInfo;

    @query("#tunnel-timer")
    protected _tunnelTimerElem?: HTMLElement;

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

    static get styles() {
        return [...super.styles, styling];
    }

    disconnectedCallback() {
        this._clearTunnelTimer();
        super.disconnectedCallback();
    }

    protected willUpdate(changedProps: PropertyValues) {
        if (changedProps.has("_activeTunnel") && !!this._activeTunnel) {
            this._navigateToTunnel(this._activeTunnel);
        }
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
                                <or-mwc-input .type="${InputType.BUTTON}" icon="stop"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._onStopTunnelClick(ev)}"
                                ></or-mwc-input>
                                <or-mwc-input .type="${InputType.BUTTON}" label="${i18next.t('gatewayTunnels.open')}" outlined .disabled="${disabled}"
                                              @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this._onTunnelNavigateClick(ev)}"
                                ></or-mwc-input>
                                <div style="margin-left: 8px;">
                                    <span id="tunnel-timer"></span>
                                </div>
                            `
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
    protected _onTunnelNavigateClick(ev: OrInputChangedEvent) {
        if (this._isConfigComplete(this.widgetConfig)) {
            this._navigateToTunnel(this._getTunnelInfoByConfig(this.widgetConfig));
        } else {
            console.warn("Could not navigate to tunnel as configuration is not complete.")
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
                    this._activeTunnel = activeTunnel;
                    this._loading = false;

                } else {
                    this._startTunnel(tunnelInfo).then(newTunnel => {

                        if (newTunnel) {
                            console.log("Started a new tunnel!", newTunnel)
                            this._activeTunnel = newTunnel;
                            this._startTunnelTimer();

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
     */
    protected async _getActiveTunnel(info: GatewayTunnelInfo): Promise<GatewayTunnelInfo | undefined> {
        if (!info.realm || !info.gatewayId || !info.target || !info.targetPort) {
            throw new Error("Could not get active tunnel, as some provided information was not set.")
        }
        return (await manager.rest.api.GatewayServiceResource.getActiveTunnelInfo(info.realm, info.gatewayId, info.target, info.targetPort)).data;
    }

    /**
     * Function that tries to destroy the currently active tunnel.
     */
    protected _tryStopTunnel(config: GatewayWidgetConfig): void {
        const tunnelInfo = this._getTunnelInfoByConfig(config);
        this._loading = true;
        this._stopTunnel(tunnelInfo)
            .then(() => {
                this._activeTunnel = undefined;
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
        if (!info.realm || !info.gatewayId || !info.target || !info.targetPort) {
            console.warn("Could not navigate to tunnel, as some provided information was not set.");
        }
        switch (info.type) {
            case GatewayTunnelInfoType.HTTPS:
            case GatewayTunnelInfoType.HTTP:
                window.open("https://" + info.id + "." + info.target)?.focus();
                break;
            default:
                console.error("Unknown error when navigating to tunnel.");
                break;
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

    /**
     * Function that starts a {@link NodeJS.Timer} containing the current active tunnel session.
     * Important: This is processed locally within the browser, so it won't be in line with the remote instance nor the Manager backend.
     */
    protected _startTunnelTimer(time = moment().startOf("day")) {
        this._activeTunnelTimer = setInterval(() => {
            time = time.add(1, "second");
            if (this._tunnelTimerElem) {
                this._tunnelTimerElem.textContent = (time.hours() > 0) ? time.format("HH:mm:ss") : time.format("mm:ss");
            }
        }, 1000)
    }

    /**
     * Function that clears the {@link NodeJS.Timer} containing the current active tunnel session.
     */
    protected _clearTunnelTimer(): void {
        clearInterval(this._activeTunnelTimer);
    }

}
