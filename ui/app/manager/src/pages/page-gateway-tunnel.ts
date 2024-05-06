import {AppStateKeyed, Page, PageProvider, router} from "@openremote/or-app";
import {TemplateResult, html, css, unsafeCSS} from "lit";
import {customElement, state} from "lit/decorators.js";
import {until} from "lit/directives/until.js";
import {Task} from "@lit/task";
import {Store} from "@reduxjs/toolkit";
import {i18next} from "@openremote/or-translate";
import {DefaultColor3, manager} from "@openremote/core";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {Asset, AssetQuery, GatewayTunnelInfo} from "@openremote/model";
import {TableColumn, TableRow} from "@openremote/or-mwc-components/or-mwc-table";
import {getAssetsRoute} from "../routes";
import {OrMwcDialog, showDialog, showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";

export function pageGatewayTunnelProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "gateway-tunnel",
        routes: [
            "gateway-tunnel"
        ],
        pageCreator: () => {
            return new PageGatewayTunnel(store);
        }
    };
}

const styling = css`
    #wrapper {
        height: 100%;
        width: 100%;
        display: flex;
        flex-direction: column;
    }

    #title {
        padding: 0 20px;
        font-size: 18px;
        font-weight: bold;
        width: calc(100% - 40px);
        max-width: 1360px;
        margin: 20px auto;
        align-items: center;
        display: flex;
        color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
    }

    #title > or-icon {
        margin-right: 10px;
        margin-left: 14px;
    }

    .panel {
        flex: 0;
        width: 100%;
        box-sizing: border-box;
        max-width: 1360px;
        background-color: white;
        border: 1px solid #e5e5e5;
        border-radius: 5px;
        position: relative;
        margin: 0 auto 10px;
        padding: 12px 24px 24px;
        display: flex;
        flex-direction: column;
    }

    .panel-title {
        display: flex;
        text-transform: uppercase;
        font-weight: bolder;
        color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
        line-height: 1em;
        margin-bottom: 10px;
        margin-top: 0;
        flex: 0 0 auto;
        letter-spacing: 0.025em;
        align-items: center;
        min-height: 36px;
    }

`

@customElement("page-gateway-tunnel")
export class PageGatewayTunnel extends Page<AppStateKeyed> {

    // Static value with the asset types to be used
    protected GATEWAY_TUNNEL_TYPES = ["GatewayAsset"];

    // Static value with possible protocol types
    protected GATEWAY_TUNNEL_PROTOCOL_TYPES = ["https", "http", "tcp"];

    @state()
    protected _realm = manager.displayRealm;

    @state()
    protected _loading = false;

    static get styles() {
        return [styling];
    }

    get name(): string {
        return "gateway-tunnel";
    }

    stateChanged(state: AppStateKeyed): void {
        this._realm = state.app.realm || manager.displayRealm;
    }

    protected render(): TemplateResult {
        return html`
            <div id="wrapper">
                <div id="title">
                    <or-icon icon="lan-connect"></or-icon>
                    ${i18next.t("gatewayTunnels.")}
                </div>
                <div class="panel">
                    <div class="panel-title" style="justify-content: space-between;">
                        <or-translate value="tunnels"></or-translate>
                        <or-mwc-input style="margin: 0;" type="${InputType.BUTTON}" icon="plus"
                                      label="${i18next.t('add')} ${i18next.t("tunnel")}"
                                      @or-mwc-input-changed="${(ev) => this._onAddTunnelClick(ev)}"
                        ></or-mwc-input>
                    </div>
                    ${this._fetchTunnelsTask.render({
                        pending: () => html`${i18next.t('loading')}`,
                        complete: (tunnels) => until(this.getTunnelsTable(tunnels), html`${i18next.t('loading')}`),
                        error: () => html`${i18next.t('errorOccurred')}`
                    })}
                </div>
            </div>
        `;
    }

    /**
     * Function that returns an HTML {@link TemplateResult} with a table.
     * Displays the given {@link tunnels} parameter in a nice looking format.
     * Uses the {@link OrMwcTable} component for displaying
     */
    protected async getTunnelsTable(tunnels?: GatewayTunnelInfo[]): Promise<TemplateResult> {
        const columns: TableColumn[] = [
            {title: i18next.t("gatewayTunnels.id"), isSortable: true},
            {title: i18next.t("gatewayTunnels.gatewayId")},
            {title: i18next.t("gatewayTunnels.target"), isSortable: true, hideMobile: true},
            {title: i18next.t("gatewayTunnels.targetPort"), isSortable: true, hideMobile: true},
            {title: ""}
        ];
        const rows: TableRow[] = tunnels?.map(tunnel => ({
            content: [
                tunnel.id,
                until(this._getTunnelGatewayIdTemplate(tunnel), html`${i18next.t('loading')}`),
                tunnel.target,
                tunnel.targetPort + " ",
                until(this._getTunnelActionsTemplate(tunnel), html`${i18next.t('loading')}`)
            ],
            clickable: false,
        } as TableRow)) || [];
        return html`
            <or-mwc-table .columns="${columns}" .rows="${rows}">

            </or-mwc-table>
        `
    }

    /**
     * Function that returns an HTML {@link TemplateResult} containing the gateway asset that is linked to the {@link tunnel}.
     */
    protected async _getTunnelGatewayIdTemplate(tunnel: GatewayTunnelInfo): Promise<TemplateResult | string> {
        const onClick = () => {
            router.navigate(getAssetsRoute(false, tunnel.gatewayId));
        }
        return html`
            <div style="height: 100%; display: flex; align-items: center;">
                <span style="cursor: pointer;" @click="${() => onClick()}">${tunnel.gatewayId}</span>
            </div>
        `;
    }

    /**
     * Function that returns an HTML {@link TemplateResult} containing the actions for the given {@link tunnel}.
     */
    protected async _getTunnelActionsTemplate(tunnel: GatewayTunnelInfo): Promise<TemplateResult | string> {
        return html`
            <div style="display: flex; justify-content: end; align-items: center; gap: 12px;">
                <or-mwc-input .type="${InputType.BUTTON}" icon="stop" @or-mwc-input-changed="${(ev) => this._onStopTunnelClick(ev, tunnel)}"></or-mwc-input>
                <or-mwc-input .type="${InputType.BUTTON}" outlined label="open" @or-mwc-input-changed="${(ev) => this._onOpenTunnelClick(ev, tunnel)}"></or-mwc-input>
            </div>
        `
    }

    /**
     * HTML callback event of the 'stop' button in the tunnels table,
     * meant to stop and delete the constructed tunnel.
     */
    protected _onStopTunnelClick(ev: OrInputChangedEvent, tunnel: GatewayTunnelInfo) {
        ev.stopPropagation();
        showOkCancelDialog(
            i18next.t('areYouSure'),
            i18next.t('dashboard.deleteWidgetWarning'), // TODO: Update text
            i18next.t('delete')
        ).then((ok: boolean) => {
            if (ok) {
                this._stopTunnel(tunnel).then(success => {
                    if (success) {
                        showSnackbar(undefined, 'success'); // TODO: Update text
                    } else {
                        showSnackbar(undefined, 'errorOccurred');
                    }
                }).catch(_error => {
                    showSnackbar(undefined, 'errorOccurred');
                }).finally(() => {
                    this._fetchTunnelsTask.run();
                });
            }
        });
    }

    /**
     * Function that requests the manager to stop the {@link tunnel}.
     */
    protected async _stopTunnel(tunnel: GatewayTunnelInfo): Promise<boolean> {
        const response = await manager.rest.api.GatewayServiceResource.stopTunnel(tunnel);
        return response.status === 200;
    }

    /**
     * HTML callback event of the 'open' button in the tunnels table,
     * meant to start tunneling towards that instance
     */
    protected _onOpenTunnelClick(ev: OrInputChangedEvent, tunnel: GatewayTunnelInfo): void {
        ev.stopPropagation();
        console.log(tunnel);

        // TODO: Write logic to open tunnel
    }

    /**
     * HTML callback event of the 'add' button above the tunnels table,
     * meant to start a tunnel towards that instance.
     */
    protected _onAddTunnelClick(ev: OrInputChangedEvent) {
        const tunnel = this._getDefaultTunnelToAdd();
        let dialog: OrMwcDialog | undefined;

        const onAddClick = () => {
            this._tryStartTunnel(tunnel);
        }
        const updateActions = () => {
            dialog.setActions([
                {actionName: "cancel", content: "cancel"},
                {actionName: "add", disabled: (!tunnel.gatewayId || !tunnel.target || !tunnel.targetPort), content: "add", action: () => onAddClick()},
            ])
        }
        const gatewayListTemplate = async (): Promise<TemplateResult> => {
            const gatewayAssets = await this._fetchGatewayAssets();
            const items = gatewayAssets.map((g) => [g.id, g.name]);
            return html`
                <div style="display: flex; flex-direction: column; gap: 20px; width: 360px;">
                    <or-mwc-input .type="${InputType.SELECT}" .options="${items}" .value="${tunnel.gatewayId}" label="${i18next.t('gatewayTunnels.selectAsset')}" style="width: 100%;"
                                  @or-mwc-input-changed="${(ev) => {
                                      tunnel.gatewayId = ev.detail.value;
                                      updateActions();
                                  }}"
                    ></or-mwc-input>
                    <or-mwc-input .type="${InputType.SELECT}" label="${i18next.t('gatewayTunnels.protocol')}" .value="${tunnel.protocol}" .options="${this.GATEWAY_TUNNEL_PROTOCOL_TYPES}"
                                  style="width: 100%"
                                  @or-mwc-input-changed="${(ev) => {
                                      tunnel.protocol = ev.detail.value;
                                      updateActions();
                                  }}"
                    ></or-mwc-input>
                    <or-mwc-input .type="${InputType.TEXT}" label="${i18next.t('host')}" .value="${tunnel.target}" style="width: 100%;"
                                  @or-mwc-input-changed="${(ev) => {
                                      tunnel.target = ev.detail.value;
                                      updateActions();
                                  }}"
                    ></or-mwc-input>
                    <or-mwc-input .type="${InputType.NUMBER}" label="${i18next.t('port')}" .value="${tunnel.targetPort}" style="width: 100%"
                                  @or-mwc-input-changed="${(ev) => {
                                      tunnel.targetPort = ev.detail.value;
                                      updateActions();
                                  }}"
                    ></or-mwc-input>
                </div>
            `
        }
        dialog = new OrMwcDialog()
            .setHeading("test")
            .setContent(html`
                ${until(gatewayListTemplate(), html`${i18next.t('loading')}`)}
            `);

        updateActions();
        showDialog(dialog);
    }

    /**
     * Function that attempts to start a new {@link tunnel}, but requires checking of its fields beforehand,
     * making sure the correct details are sent towards the {@link _startTunnel()} method.
     */
    protected _tryStartTunnel(tunnel: GatewayTunnelInfo) {
        if (!tunnel) {
            console.warn("Tried to add tunnel, but failed.")
            return;
        }
        if (!tunnel.gatewayId || !tunnel.target || !tunnel.targetPort) {
            console.warn("Tried to add tunnel, but not all fields were filled in!");
            return;
        }
        if (!tunnel.realm) {
            tunnel.realm = manager.displayRealm;
        }
        this._startTunnel(tunnel).finally(() => {
            this._fetchTunnelsTask.run();
        });
    }

    /**
     * Function that requests the manager to start a new {@link tunnel}.
     */
    protected async _startTunnel(tunnel: GatewayTunnelInfo): Promise<boolean> {
        try {
            const response = await manager.rest.api.GatewayServiceResource.startTunnel(tunnel);
            return response?.status === 200;

        } catch (ex) {
            console.error(ex);
            return false;
        }
    }


    /* ------------------------------------- */

    /**
     * Internal Lit {@link Task} meant for fetching the list of tunnels.
     * It acts as a reactive controller, and keeps its value cached.
     */
    protected _fetchTunnelsTask = new Task(this, {
        task: async ([realm], {signal}) => {
            return await this._fetchTunnels(realm, signal);
        },
        args: () => [this._realm]
    });

    /**
     * Internal asynchronous function to fetch the list of tunnels.
     */
    protected async _fetchTunnels(realm: string = manager.displayRealm, _signal?: AbortSignal): Promise<GatewayTunnelInfo[]> {
        const response = await manager.rest.api.GatewayServiceResource.getActiveTunnelInfos(realm);
        if (response.status !== 200) {
            throw new Error(response.statusText);
        }

        if (response.data.length > 0) {
            return response.data;
        } else {
            console.warn("No tunnels were received from the manager.")
        }
    }


    /* ------------------------------ */

    /**
     * Utility function to fetch the available gateway assets to select from
     */
    protected async _fetchGatewayAssets(requireTunnelSupport = true): Promise<Asset[]> {
        const query = {
            realm: {name: manager.displayRealm},
            select: {attributes: ["tunnelingSupported"]},
            types: this.GATEWAY_TUNNEL_TYPES,
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
                    }
                ]
            } : undefined
        } as AssetQuery;
        return (await manager.rest.api.AssetResource.queryAssets(query)).data;
    }

    /**
     * Utility function that returns the default values of a new tunnel.
     */
    protected _getDefaultTunnelToAdd(): GatewayTunnelInfo {
        return {
            target: "localhost",
            targetPort: 443,
            protocol: this.GATEWAY_TUNNEL_PROTOCOL_TYPES?.[0]
        }
    }

}
