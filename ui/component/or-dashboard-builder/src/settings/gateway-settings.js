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
import { customElement } from "lit/decorators.js";
import { WidgetSettings } from "../util/widget-settings";
import { html } from "lit";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";
import { i18next } from "@openremote/or-translate";
import { Task } from "@lit/task";
import manager from "@openremote/core";
let GatewaySettings = class GatewaySettings extends WidgetSettings {
    constructor() {
        super(...arguments);
        this.GATEWAY_ASSET_TYPES = ["GatewayAsset"];
        this.GATEWAY_TUNNEL_TYPES = ["HTTPS" /* GatewayTunnelInfoType.HTTPS */, "HTTP" /* GatewayTunnelInfoType.HTTP */, "TCP" /* GatewayTunnelInfoType.TCP */];
        this._fetchAssetsTask = new Task(this, {
            task: ([], { signal }) => __awaiter(this, void 0, void 0, function* () {
                return yield this._fetchAssets(true, signal);
            }),
            args: () => []
        });
    }
    connectedCallback() {
        super.connectedCallback();
    }
    render() {
        const disabled = !this.widgetConfig.gatewayId;
        return html `
            <div>
                <settings-panel displayName="gatewayTunnels.settings" expanded="${true}">
                    <div style="display: flex; flex-direction: column; gap: 8px;">
                        <div>
                            ${this._fetchAssetsTask.render({
            pending: () => html `
                                    <or-mwc-input .type="${InputType.SELECT}" compact style="width: 100%;" disabled
                                                  label="${i18next.t('gatewayTunnels.selectAsset')}"
                                    ></or-mwc-input>
                                `,
            complete: (gatewayAssets) => {
                const options = gatewayAssets.map(a => [a.id, a.name]);
                const selected = gatewayAssets.find(a => a.id === this.widgetConfig.gatewayId);
                const value = selected ? [selected.id, selected.name] : undefined;
                return html `
                                        <or-mwc-input .type="${InputType.SELECT}" compact style="width: 100%;"
                                                      .options="${options}" .value="${value}"
                                                      label="${i18next.t('gatewayTunnels.selectAsset')}"
                                                      @or-mwc-input-changed="${(ev) => this._onAssetSelect(ev)}"
                                        ></or-mwc-input>
                                    `;
            },
            error: () => html `
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
                                          @or-mwc-input-changed="${(ev) => this._onTunnelTypeSelect(ev)}"
                            ></or-mwc-input>
                        </div>
                        <div>
                            <or-mwc-input .type="${InputType.TEXT}" style="width: 100%;"
                                          .disabled="${disabled}" .value="${this.widgetConfig.target}"
                                          label="${i18next.t('gatewayTunnels.target')}"
                                          @or-mwc-input-changed="${(ev) => this._onTunnelTargetSelect(ev)}"
                            ></or-mwc-input>
                        </div>
                        <div>
                            <or-mwc-input .type="${InputType.NUMBER}" style="width: 100%;"
                                          .disabled="${disabled}" .value="${this.widgetConfig.targetPort}"
                                          label="${i18next.t('gatewayTunnels.targetPort')}"
                                          @or-mwc-input-changed="${(ev) => this._onTunnelTargetPortSelect(ev)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </settings-panel>
            </div>
        `;
    }
    _onAssetSelect(ev) {
        this.widgetConfig.gatewayId = ev.detail.value;
        this.notifyConfigUpdate();
    }
    _onTunnelTypeSelect(ev) {
        this.widgetConfig.type = ev.detail.value;
        this.notifyConfigUpdate();
    }
    _onTunnelTargetSelect(ev) {
        this.widgetConfig.target = ev.detail.value;
        this.notifyConfigUpdate();
    }
    _onTunnelTargetPortSelect(ev) {
        this.widgetConfig.targetPort = ev.detail.value;
        this.notifyConfigUpdate();
    }
    _fetchAssets(requireTunnelSupport = true, signal) {
        return __awaiter(this, void 0, void 0, function* () {
            const query = {
                realm: { name: manager.displayRealm },
                select: { attributes: ["tunnelingSupported"] },
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
            };
            return (yield manager.rest.api.AssetResource.queryAssets(query)).data;
        });
    }
};
GatewaySettings = __decorate([
    customElement("gateway-settings")
], GatewaySettings);
export { GatewaySettings };
//# sourceMappingURL=gateway-settings.js.map