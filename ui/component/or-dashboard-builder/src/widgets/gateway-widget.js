var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var GatewayWidget_1;
import { customElement, state } from "lit/decorators.js";
import { OrWidget } from "../util/or-widget";
import { css, html } from "lit";
import { GatewaySettings } from "../settings/gateway-settings";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import manager from "@openremote/core";
import { when } from "lit/directives/when.js";
import { i18next } from "@openremote/or-translate";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
const styling = css `
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
function getDefaultWidgetConfig() {
    return {
        type: "HTTPS" /* GatewayTunnelInfoType.HTTPS */,
        target: "localhost",
        targetPort: 443,
    };
}
let GatewayWidget = GatewayWidget_1 = class GatewayWidget extends OrWidget {
    constructor() {
        super(...arguments);
        this._loading = false;
        this._startedByUser = false;
    }
    static getManifest() {
        return {
            displayName: "Gateway",
            displayIcon: "lan-connect",
            minColumnWidth: 1,
            minColumnHeight: 1,
            getContentHtml(config) {
                return new GatewayWidget_1(config);
            },
            getSettingsHtml(config) {
                return new GatewaySettings(config);
            },
            getDefaultConfig() {
                return getDefaultWidgetConfig();
            }
        };
    }
    refreshContent(force) {
        this.widgetConfig = JSON.parse(JSON.stringify(this.widgetConfig));
    }
    static get styles() {
        return [...super.styles, styling];
    }
    disconnectedCallback() {
        if (this._activeTunnel) {
            if (this._startedByUser) {
                this._stopTunnel(this._activeTunnel);
            }
            else {
                console.warn("Keeping the active tunnel open, as it is not started through the widget.");
            }
        }
        super.disconnectedCallback();
    }
    willUpdate(changedProps) {
        if (changedProps.has("_activeTunnel") && !!this._activeTunnel) {
            this._navigateToTunnel(this._activeTunnel);
        }
    }
    render() {
        var _a;
        const disabled = ((_a = this.getEditMode) === null || _a === void 0 ? void 0 : _a.call(this)) || !this._isConfigComplete(this.widgetConfig);
        return html `
            <div id="gateway-widget-wrapper">
                <div id="gateway-widget-container">
                    ${when(this._loading, () => html `
                        <or-loading-indicator></or-loading-indicator>
                    `, () => {
            if (this._activeTunnel) {
                return html `
                                
                                <or-mwc-input .type="${InputType.BUTTON}" icon="stop" label="${i18next.t('gatewayTunnels.stop')}"
                                              @or-mwc-input-changed="${(ev) => this._onStopTunnelClick(ev)}"
                                ></or-mwc-input>
                                
                                ${when(this.widgetConfig.type === "TCP" /* GatewayTunnelInfoType.TCP */, () => html `
                                    <or-mwc-input .type="${InputType.BUTTON}" icon="content-copy" label="${i18next.t('gatewayTunnels.copyAddress')}" outlined .disabled="${disabled}"
                                                  @or-mwc-input-changed="${(ev) => this._onCopyTunnelAddressClick(ev)}"
                                    ></or-mwc-input>
                                `, () => html `
                                    <or-mwc-input .type="${InputType.BUTTON}" icon="open-in-new" label="${i18next.t('gatewayTunnels.open')}" outlined .disabled="${disabled}"
                                                  @or-mwc-input-changed="${(ev) => this._onTunnelNavigateClick(ev)}"
                                    ></or-mwc-input>
                                `)}
                                
                            `;
            }
            else {
                return html `
                                <or-mwc-input .type="${InputType.BUTTON}" label="${i18next.t('gatewayTunnels.start')}" outlined .disabled="${disabled}"
                                              @or-mwc-input-changed="${(ev) => this._onStartTunnelClick(ev)}"
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
    _onCopyTunnelAddressClick(ev) {
        if (this._isConfigComplete(this.widgetConfig)) {
            const tunnelInfo = this._getTunnelInfoByConfig(this.widgetConfig);
            const address = this._getTunnelAddress(tunnelInfo);
            if (address) {
                navigator.clipboard.writeText(address).finally(() => {
                    showSnackbar(undefined, i18next.t('gatewayTunnels.copySuccess'));
                });
            }
            else {
                console.warn("Could not copy tunnel address as it could not be found.");
                showSnackbar(undefined, i18next.t('errorOccurred'));
            }
        }
        else {
            console.warn("Could not copy tunnel address as configuration is not complete.");
            showSnackbar(undefined, i18next.t('errorOccurred'));
        }
    }
    /**
     * HTML callback function when 'start' button is pressed, meant to create / start a new tunnel.
     */
    _onStartTunnelClick(ev) {
        this._tryStartTunnel(this.widgetConfig);
    }
    /**
     * HTML callback function when 'stop' button is pressed, meant to destroy the active tunnel.
     */
    _onStopTunnelClick(ev) {
        this._tryStopTunnel(this.widgetConfig);
    }
    /**
     * HTML callback function when 'open' button is pressed, meant to start using the tunnel.
     */
    _onTunnelNavigateClick(ev) {
        if (this._isConfigComplete(this.widgetConfig)) {
            this._navigateToTunnel(this._getTunnelInfoByConfig(this.widgetConfig));
        }
        else {
            console.warn("Could not navigate to tunnel as configuration is not complete.");
        }
    }
    /**
     * Function that tries to start the tunnel. It checks the configuration beforehand,
     * and acts as a controller to call the correct functions throughout the starting process.
     */
    _tryStartTunnel(widgetConfig) {
        if (this._isConfigComplete(widgetConfig)) {
            const tunnelInfo = this._getTunnelInfoByConfig(widgetConfig);
            this._loading = true;
            this._getActiveTunnel(tunnelInfo).then(activeTunnel => {
                if (activeTunnel) {
                    console.log("Found an active tunnel!", activeTunnel);
                    this._activeTunnel = activeTunnel;
                    this._loading = false;
                }
                else {
                    this._startTunnel(tunnelInfo).then(newTunnel => {
                        if (newTunnel) {
                            console.log("Started a new tunnel!", newTunnel);
                            this._activeTunnel = newTunnel;
                            this._startedByUser = true;
                        }
                        else {
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
            });
        }
    }
    /**
     * Internal function that starts the tunnel by communicating with the Manager API.
     */
    _startTunnel(info) {
        return __awaiter(this, void 0, void 0, function* () {
            if (!info.realm || !info.gatewayId || !info.target || !info.targetPort) {
                throw new Error("Could not start tunnel, as some provided information was not set.");
            }
            return (yield manager.rest.api.GatewayServiceResource.startTunnel(info)).data;
        });
    }
    /**
     * Internal function that requests the Manager API for the active tunnel based on the {@link GatewayTunnelInfo} parameter.
     */
    _getActiveTunnel(info) {
        return __awaiter(this, void 0, void 0, function* () {
            if (!info.realm || !info.gatewayId || !info.target || !info.targetPort) {
                throw new Error("Could not get active tunnel, as some provided information was not set.");
            }
            return (yield manager.rest.api.GatewayServiceResource.getActiveTunnelInfo(info.realm, info.gatewayId, info.target, info.targetPort)).data;
        });
    }
    /**
     * Function that tries to destroy the currently active tunnel.
     */
    _tryStopTunnel(config) {
        const tunnelInfo = this._getTunnelInfoByConfig(config);
        this._loading = true;
        this._stopTunnel(tunnelInfo)
            .then(() => {
            this._activeTunnel = undefined;
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
    _stopTunnel(info) {
        return __awaiter(this, void 0, void 0, function* () {
            return (yield manager.rest.api.GatewayServiceResource.stopTunnel(info)).data;
        });
    }
    /**
     * Function that navigates the user to an HTTP web page that interacts with a service through the tunnel.
     * It will open in a new browser tab automatically.
     */
    _navigateToTunnel(info) {
        var _a;
        if (!info.realm || !info.gatewayId || !info.target || !info.targetPort) {
            console.warn("Could not navigate to tunnel, as some provided information was not set.");
        }
        const address = this._getTunnelAddress(info);
        switch (info.type) {
            case "HTTPS" /* GatewayTunnelInfoType.HTTPS */:
            case "HTTP" /* GatewayTunnelInfoType.HTTP */:
                (_a = window.open(address)) === null || _a === void 0 ? void 0 : _a.focus();
                break;
            default:
                console.error("Unknown error when navigating to tunnel.");
                break;
        }
    }
    /**
     * Internal function to get the tunnel address based on {@link GatewayTunnelInfo}
     */
    _getTunnelAddress(info) {
        switch (info.type) {
            case "HTTPS" /* GatewayTunnelInfoType.HTTPS */:
            case "HTTP" /* GatewayTunnelInfoType.HTTP */:
                return "//" + info.id + "." + window.location.host;
            case "TCP" /* GatewayTunnelInfoType.TCP */:
                return info.id + "." + window.location.hostname + ":" + info.assignedPort;
        }
    }
    /**
     * Internal function to check whether the {@link GatewayWidgetConfig} includes the necessary information to control the tunnel.
     * Uses several `undefined` or empty checks. Useful for checking the object before interacting with Manager APIs.
     */
    _isConfigComplete(widgetConfig) {
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
    _getTunnelInfoByConfig(config) {
        return {
            realm: manager.displayRealm,
            gatewayId: config.gatewayId,
            type: config.type,
            target: config.target,
            targetPort: config.targetPort
        };
    }
};
__decorate([
    state()
], GatewayWidget.prototype, "_loading", void 0);
__decorate([
    state()
], GatewayWidget.prototype, "_activeTunnel", void 0);
GatewayWidget = GatewayWidget_1 = __decorate([
    customElement("gateway-widget")
], GatewayWidget);
export { GatewayWidget };
//# sourceMappingURL=gateway-widget.js.map