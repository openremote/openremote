import {css, html, TemplateResult, unsafeCSS} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import "@openremote/or-asset-tree";
import "@openremote/or-asset-viewer";
import {
    OrAssetViewer,
    OrAssetViewerEditToggleEvent,
    OrAssetViewerRequestEditToggleEvent,
    OrAssetViewerSaveEvent,
    saveAsset,
    SaveResult,
    ViewerConfig
} from "@openremote/or-asset-viewer";
import {
    AssetTreeConfig,
    OrAssetTree,
    OrAssetTreeAddEvent,
    OrAssetTreeAssetEvent,
    OrAssetTreeRequestSelectionEvent,
    OrAssetTreeSelectionEvent
} from "@openremote/or-asset-tree";
import manager, {DefaultBoxShadow, Util} from "@openremote/core";
import {AppStateKeyed, Page, PageProvider, router} from "@openremote/or-app";
import {EnhancedStore} from "@reduxjs/toolkit";
import {showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import i18next from "i18next";
import {AssetEventCause, WellknownAssets} from "@openremote/model";
import "@openremote/or-json-forms";
import {createSelector} from "reselect";

export interface PageAssetsConfig {
    viewer?: ViewerConfig;
    tree?: AssetTreeConfig;
}

export function pageAssetsProvider<S extends AppStateKeyed>(store: EnhancedStore<S>, config?: PageAssetsConfig): PageProvider<S> {
    return {
        name: "assets",
        routes: [
            "assets",
            "assets/:editMode",
            "assets/:editMode/:id"
        ],
        pageCreator: () => {
            const page = new PageAssets(store);
            if (config) {
                page.config = config;
            }
            return page;
        }
    };
}

export const PAGE_ASSETS_CONFIG_DEFAULT: PageAssetsConfig = {
    tree: {
        add: {
            typesParent: {
                default: {
                    exclude: [
                        WellknownAssets.TRADFRILIGHTASSET,
                        WellknownAssets.TRADFRIPLUGASSET,
                        WellknownAssets.ARTNETLIGHTASSET,
                        WellknownAssets.CONSOLEASSET
                    ]
                }
            }
        }
    }
};

export function getAssetsRoute(editMode?: boolean, assetId?: string) {
    let route = "assets/" + (editMode ? "true" : "false");
    if (assetId) {
        route += "/" + assetId;
    }

    return route;
}

@customElement("page-assets")
class PageAssets<S extends AppStateKeyed> extends Page<S>  {

    static get styles() {
        // language=CSS
        return css`

            or-asset-tree {
                align-items: stretch;
                z-index: 1;
            }

            .hideMobile {
                display: none;
            }

            or-asset-viewer {
                align-items: stretch;
                z-index: 0;
            }

            @media only screen and (min-width: 768px){
                or-asset-tree {
                    width: 300px;
                    min-width: 300px;
                    box-shadow: ${unsafeCSS(DefaultBoxShadow)}
                }

                .hideMobile {
                    display: flex;
                }

                or-asset-viewer,
                or-asset-viewer.hideMobile {
                    display: initial;
                    max-width: calc(100vw - 300px);
                }
            }
        `;
    }

    @property()
    public config?: PageAssetsConfig;

    @property()
    protected _editMode: boolean = false;

    @property()
    protected _assetIds?: string[];

    @query("#tree")
    protected _tree!: OrAssetTree;

    @query("#viewer")
    protected _viewer!: OrAssetViewer;

    protected _addedAssetId?: string;
    protected _realmSelector = (state: S) => state.app.realm || manager.displayRealm;

    get name(): string {
        return "assets";
    }

    protected getRealmState = createSelector(
        [this._realmSelector],
        async () => {
            this._assetIds = undefined;
            if (this._viewer && this._viewer.assetId) this._viewer.assetId = undefined;
            if (this._tree) this._tree.refresh();
            this._updateRoute(true);
        }
    )

    constructor(store: EnhancedStore<S>) {
        super(store);
        this.addEventListener(OrAssetTreeRequestSelectionEvent.NAME, this._onAssetSelectionRequested);
        this.addEventListener(OrAssetTreeSelectionEvent.NAME, this._onAssetSelectionChanged);
        this.addEventListener(OrAssetViewerRequestEditToggleEvent.NAME, this._onEditToggleRequested);
        this.addEventListener(OrAssetViewerEditToggleEvent.NAME, this._onEditToggle);
        this.addEventListener(OrAssetTreeAddEvent.NAME, this._onAssetAdd);
        this.addEventListener(OrAssetViewerSaveEvent.NAME, (ev) => this._onAssetSave(ev.detail));
        this.addEventListener(OrAssetTreeAssetEvent.NAME, this._onAssetTreeAssetEvent);
    }

    protected render(): TemplateResult | void {
        return html`
            <or-asset-tree id="tree" .config="${this.config && this.config.tree ? this.config.tree : PAGE_ASSETS_CONFIG_DEFAULT.tree}" class="${this._assetIds && this._assetIds.length === 1 ? "hideMobile" : ""}" .selectedIds="${this._assetIds}"></or-asset-tree>
            <or-asset-viewer id="viewer" .config="${this.config && this.config.viewer ? this.config.viewer : undefined}" class="${!this._assetIds || this._assetIds.length !== 1 ? "hideMobile" : ""}" .editMode="${this._editMode}"></or-asset-viewer>
        `;
    }

    stateChanged(state: S) {
        // State is only utilised for initial loading
        this.getRealmState(state); // Order is important here!
        this._editMode = !!(state.app.params && state.app.params.editMode === "true");
        this._assetIds = state.app.params && state.app.params.id ? [state.app.params.id as string] : undefined;
    }

    protected _onAssetSelectionRequested(event: OrAssetTreeRequestSelectionEvent) {
        const isModified = this._viewer.isModified();

        if (!isModified) {
            return;
        }

        // Prevent the request and check if user wants to lose changes
        event.detail.allow = false;

        this._confirmContinue(() => {
            const nodes = event.detail.detail.newNodes;

            if (Util.objectsEqual(nodes, event.detail.detail.oldNodes)) {
                // User has clicked the same node so let's force reload it
                this._viewer.reloadAsset();
            } else {
                this._assetIds = nodes.map((node) => node.asset.id!);
                this._viewer.assetId = nodes.length === 1 ? nodes[0].asset!.id : undefined;
                this._updateRoute(true);
            }
        });
    }

    protected _onAssetSelectionChanged(event: OrAssetTreeSelectionEvent) {
        const nodes = event.detail.newNodes;
        this._assetIds = event.detail.newNodes.map((node) => node.asset.id!);
        this._viewer.assetId = nodes.length === 1 ? nodes[0].asset!.id : undefined;
        this._updateRoute(true);
    }

    protected _onEditToggleRequested(event: OrAssetViewerRequestEditToggleEvent) {
        // Block the request if current asset is modified then show dialog then navigate
        const isModified = this._viewer.isModified();

        if (!isModified) {
            return;
        }

        event.detail.allow = false;

        this._confirmContinue(() => {
            this._editMode = event.detail.detail;
        });
    }

    protected _onEditToggle(event: OrAssetViewerEditToggleEvent) {
        this._editMode = event.detail;
        this._updateRoute(true);
    }

    protected _confirmContinue(action: () => void) {
        if (this._viewer.isModified()) {
            showOkCancelDialog(i18next.t("loseChanges"), i18next.t("confirmContinueAssetModified"), i18next.t("discard"))
                .then((ok) => {
                    if (ok) {
                        action();
                    }
                });
        } else {
            action();
        }
    }

    protected async _onAssetAdd(ev: OrAssetTreeAddEvent) {
        if (this._editMode) {
            // Just load the asset into the viewer
            this._viewer.asset = ev.detail.asset;
        } else {
            // Auto save and load
            const result = await saveAsset(ev.detail.asset);
            result.isCopy = true;
            this.dispatchEvent(new OrAssetViewerSaveEvent(result));
            this._onAssetSave(result);
        }
    }

    protected _onAssetSave(result: SaveResult) {

        if (!result.success) {
            return;
        }

        if (result.isNew) {
            this._addedAssetId = result.assetId!;
        }
    }

    protected _onAssetTreeAssetEvent(ev: OrAssetTreeAssetEvent) {
        // Check if the new asset just saved has been created in the asset tree and if so select it
        if (ev.detail.cause === AssetEventCause.CREATE && this._addedAssetId) {
            if (this._addedAssetId === ev.detail.asset.id) {
                this._assetIds = [ev.detail.asset.id];
                this._addedAssetId = undefined;
                this._viewer.assetId = ev.detail.asset.id;
                this._updateRoute(true);
            }
        }
    }


    protected _updateRoute(silent: boolean = true) {
        const assetId = this._assetIds && this._assetIds.length === 1 ? this._assetIds[0] : undefined;
        router.navigate(getAssetsRoute(this._editMode, assetId), {
            callHooks: !silent,
            callHandler: !silent
        });
    }
}
