import { OrWidget, WidgetManifest } from "../util/or-widget";
import { PropertyValues, TemplateResult } from "lit";
import { WidgetConfig } from "../util/widget-config";
import { OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { GatewayTunnelInfo, GatewayTunnelInfoType } from "@openremote/model";
export interface GatewayWidgetConfig extends WidgetConfig {
    gatewayId?: string;
    type: GatewayTunnelInfoType;
    target: string;
    targetPort: number;
}
export declare class GatewayWidget extends OrWidget {
    protected widgetConfig: GatewayWidgetConfig;
    protected _loading: boolean;
    protected _activeTunnel?: GatewayTunnelInfo;
    protected _startedByUser: boolean;
    static getManifest(): WidgetManifest;
    refreshContent(force: boolean): void;
    static get styles(): import("lit").CSSResult[];
    disconnectedCallback(): void;
    protected willUpdate(changedProps: PropertyValues): void;
    protected render(): TemplateResult;
    /**
     * HTML callback function when 'copy address' button is pressed for a TCP tunnel.
     */
    protected _onCopyTunnelAddressClick(ev: OrInputChangedEvent): void;
    /**
     * HTML callback function when 'start' button is pressed, meant to create / start a new tunnel.
     */
    protected _onStartTunnelClick(ev: OrInputChangedEvent): void;
    /**
     * HTML callback function when 'stop' button is pressed, meant to destroy the active tunnel.
     */
    protected _onStopTunnelClick(ev: OrInputChangedEvent): void;
    /**
     * HTML callback function when 'open' button is pressed, meant to start using the tunnel.
     */
    protected _onTunnelNavigateClick(ev: OrInputChangedEvent): void;
    /**
     * Function that tries to start the tunnel. It checks the configuration beforehand,
     * and acts as a controller to call the correct functions throughout the starting process.
     */
    protected _tryStartTunnel(widgetConfig: GatewayWidgetConfig): void;
    /**
     * Internal function that starts the tunnel by communicating with the Manager API.
     */
    protected _startTunnel(info: GatewayTunnelInfo): Promise<GatewayTunnelInfo | undefined>;
    /**
     * Internal function that requests the Manager API for the active tunnel based on the {@link GatewayTunnelInfo} parameter.
     */
    protected _getActiveTunnel(info: GatewayTunnelInfo): Promise<GatewayTunnelInfo | undefined>;
    /**
     * Function that tries to destroy the currently active tunnel.
     */
    protected _tryStopTunnel(config: GatewayWidgetConfig): void;
    /**
     * Internal function that requests the Manager API to destroy a tunnel that is in line with the {@link GatewayTunnelInfo} parameter.
     */
    protected _stopTunnel(info: GatewayTunnelInfo): Promise<void>;
    /**
     * Function that navigates the user to an HTTP web page that interacts with a service through the tunnel.
     * It will open in a new browser tab automatically.
     */
    protected _navigateToTunnel(info: GatewayTunnelInfo): void;
    /**
     * Internal function to get the tunnel address based on {@link GatewayTunnelInfo}
     */
    protected _getTunnelAddress(info: GatewayTunnelInfo): string | undefined;
    /**
     * Internal function to check whether the {@link GatewayWidgetConfig} includes the necessary information to control the tunnel.
     * Uses several `undefined` or empty checks. Useful for checking the object before interacting with Manager APIs.
     */
    protected _isConfigComplete(widgetConfig: GatewayWidgetConfig): boolean;
    /**
     * Internal function that parses a {@link GatewayWidgetConfig} into a new {@link GatewayTunnelInfo}.
     */
    protected _getTunnelInfoByConfig(config: GatewayWidgetConfig): GatewayTunnelInfo;
}
