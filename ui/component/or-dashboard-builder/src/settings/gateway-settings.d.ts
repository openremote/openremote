import { WidgetSettings } from "../util/widget-settings";
import { TemplateResult } from "lit";
import { OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { Asset, GatewayTunnelInfoType } from "@openremote/model";
import { GatewayWidgetConfig } from "../widgets/gateway-widget";
import { Task } from "@lit/task";
export declare class GatewaySettings extends WidgetSettings {
    protected GATEWAY_ASSET_TYPES: string[];
    protected GATEWAY_TUNNEL_TYPES: GatewayTunnelInfoType[];
    protected readonly widgetConfig: GatewayWidgetConfig;
    connectedCallback(): void;
    protected render(): TemplateResult;
    protected _onAssetSelect(ev: OrInputChangedEvent): void;
    protected _onTunnelTypeSelect(ev: OrInputChangedEvent): void;
    protected _onTunnelTargetSelect(ev: OrInputChangedEvent): void;
    protected _onTunnelTargetPortSelect(ev: OrInputChangedEvent): void;
    protected _fetchAssetsTask: Task<never[], Asset[]>;
    protected _fetchAssets(requireTunnelSupport?: boolean, signal?: AbortSignal): Promise<Asset[]>;
}
