import {css, html, TemplateResult, unsafeCSS, PropertyValues} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import "@openremote/or-asset-viewer";
import {
    OrAssetViewer,
    OrAssetViewerEditToggleEvent,
    OrAssetViewerRequestEditToggleEvent,
    OrAssetViewerSaveEvent,
    OrAssetViewerLoadUserEvent,
    saveAsset,
    SaveResult,
    ViewerConfig, OrAssetViewerLoadAlarmEvent
} from "@openremote/or-asset-viewer";
import {
    AssetTreeConfig, ChangeParentEventDetail,
    OrAssetTree,
    OrAssetTreeAddEvent,
    OrAssetTreeAssetEvent,
    OrAssetTreeChangeParentEvent,
    OrAssetTreeRequestSelectionEvent,
    OrAssetTreeSelectionEvent,
    OrAssetTreeToggleExpandEvent,
} from "@openremote/or-asset-tree";
import manager, {DefaultBoxShadow, DefaultColor5, Util} from "@openremote/core";
import {AppStateKeyed, Page, PageProvider, router} from "@openremote/or-app";
import {createSlice, Store, createSelector, PayloadAction} from "@reduxjs/toolkit";
import {DialogAction, OrMwcDialog, showDialog, showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import i18next from "i18next";
import {AssetEventCause, WellknownAssets} from "@openremote/model";
import "@openremote/or-json-forms";
import {getAlarmsRoute, getAssetsRoute, getUsersRoute} from "../routes";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";

export interface PageAssetsConfig {
    viewer?: ViewerConfig;
    tree?: AssetTreeConfig;
}
export interface AssetsState {
    expandedParents: {realm: string, ids: string[]}[]
}
export interface AssetsStateKeyed extends AppStateKeyed {
    assets: AssetsState;
}
const INITIAL_STATE: AssetsState = {
    expandedParents: []
};
const pageAssetsSlice = createSlice({
    name: "pageAssets",
    initialState: INITIAL_STATE,
    reducers: {
        updateExpandedParents(state, action: PayloadAction<[string, boolean]>) {
            const expandedParents = JSON.parse(JSON.stringify(state.expandedParents)); // copy state to prevent issues inserting it back
            const expanded = expandedParents.find(x => x.realm == manager.displayRealm);
            if(!expanded) {
                expandedParents.push({ realm: manager.displayRealm, ids: []});
            }
            const expandedId = expandedParents.findIndex(x => x.realm == manager.displayRealm);
            if(!action.payload[1] && expanded && expanded.ids && expanded.ids.includes(action.payload[0])) {
                expandedParents[expandedId].ids = expanded.ids.filter((parent) => parent != action.payload[0]); // filter out collapsed ones
            } else if(!expanded || (action.payload[1] && expanded && expanded.ids && !expanded.ids.includes(action.payload[0]))) {
                expandedParents[expandedId].ids.push(action.payload[0]); // add new extended ones
            }
            return { ...state, expandedParents: expandedParents }
        }
    }
})
const {updateExpandedParents} = pageAssetsSlice.actions;
export const pageAssetsReducer = pageAssetsSlice.reducer;

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
export function pageAssetsProvider(store: Store<AssetsStateKeyed>, config?: PageAssetsConfig): PageProvider<AssetsStateKeyed> {
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

@customElement("page-assets")
export class PageAssets extends Page<AssetsStateKeyed>  {

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

            .multipleAssetsView {
                display: flex;
                justify-content: center;
                align-items: center;
                text-align: center;
                height: 100%;
                width: 100%;
            }

            .multipleAssetsView > div {
                display: flex;
                flex-direction: column;
            }

            .multipleAssetsView > div > *:first-child {
                margin: 30px;                
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

    @property() // selected asset ids
    protected _assetIds?: string[];

    @query("#tree")
    protected _tree!: OrAssetTree;

    @query("#viewer")
    protected _viewer?: OrAssetViewer;

    protected _addedAssetId?: string;
    protected _realmSelector = (state: AssetsStateKeyed) => state.app.realm || manager.displayRealm;

    get name(): string {
        return "assets";
    }

    protected getRealmState = createSelector(
        [this._realmSelector],
        async (realm: string) => {
            this._assetIds = undefined;
            if (this._viewer) {
                this._viewer.assetId = undefined;
            }
            if (this._tree) this._tree.refresh();
            this._updateRoute(true);
        }
    )

    constructor(store: Store<AssetsStateKeyed>) {
        super(store);
        this.addEventListener(OrAssetTreeRequestSelectionEvent.NAME, this._onAssetSelectionRequested);
        this.addEventListener(OrAssetTreeSelectionEvent.NAME, (ev) => this._onAssetSelectionChanged(ev.detail.newNodes.map((node) => node.asset.id!)));
        this.addEventListener(OrAssetViewerRequestEditToggleEvent.NAME, this._onEditToggleRequested);
        this.addEventListener(OrAssetViewerEditToggleEvent.NAME, this._onEditToggle);
        this.addEventListener(OrAssetTreeAddEvent.NAME, this._onAssetAdd);
        this.addEventListener(OrAssetViewerSaveEvent.NAME, (ev) => this._onAssetSave(ev.detail));
        this.addEventListener(OrAssetTreeAssetEvent.NAME, this._onAssetTreeAssetEvent);
        this.addEventListener(OrAssetTreeChangeParentEvent.NAME, (ev) => this._onAssetParentChange(ev.detail));
        this.addEventListener(OrAssetTreeToggleExpandEvent.NAME, this._onAssetExpandToggle);
        this.addEventListener(OrAssetViewerLoadUserEvent.NAME, this._onLoadUserEvent);
        this.addEventListener(OrAssetViewerLoadAlarmEvent.NAME,(ev) =>  this._onLoadAlarmEvent(ev));
    }

    protected render(): TemplateResult | void {

        let viewerHTML: TemplateResult;
        const multiSelection = this._assetIds && this._assetIds.length > 1;

        if (multiSelection) {
            viewerHTML = html`
                <div class="multipleAssetsView hideMobile">
                    <div>
                        <or-translate value="multiAssetSelected" .options="${ { assetNbr: this._assetIds.length } }"></or-translate>
                        <or-mwc-input .type="${InputType.BUTTON}" label="changeParent" @click="${() => this._onParentChangeClick()}" outlined></or-mwc-input>
                    </div>
                </div>
            `;
        } else {
            const assetId = this._assetIds && this._assetIds.length === 1 ? this._assetIds[0] : undefined;
            viewerHTML = html`
                <or-asset-viewer id="viewer"
                                 .config="${this.config && this.config.viewer ? this.config.viewer : undefined}"
                                 class="${!assetId ? "hideMobile" : ""}"
                                 .editMode="${this._editMode}"
                ></or-asset-viewer>
            `;
        }

        return html`
            <or-asset-tree id="tree" .config="${this.config && this.config.tree ? this.config.tree : PAGE_ASSETS_CONFIG_DEFAULT.tree}"
                           class="${this._assetIds && this._assetIds.length === 1 ? "hideMobile" : ""}"
                           .selectedIds="${this._assetIds}"
            ></or-asset-tree>
            ${viewerHTML}
        `;
    }

    // State is only utilised for initial loading, and for changes within the store.
    // On the assets page, we shouldn't change editMode nor assetIds if the URL/state hasn't changed.
    stateChanged(state: AssetsStateKeyed) {
        this.getRealmState(state); // Order is important here!
        this._editMode = !!(state.app.params && state.app.params.editMode === "true");
        if (!this._assetIds || this._assetIds.length === 0) {
            this._assetIds = state.app.params && state.app.params.id ? [state.app.params.id as string] : undefined;
        }
    }

    protected _onParentChangeClick() {
        let dialog: OrMwcDialog;

        const blockEvent = (ev: Event) => {
            ev.stopPropagation();
        };

        const dialogContent = html`
            <or-asset-tree id="parent-asset-tree" disableSubscribe readonly .selectedIds="${[]}"
                           @or-asset-tree-request-select="${blockEvent}"
                           @or-asset-tree-selection-changed="${blockEvent}"></or-asset-tree>`;

        const setParent = () => {
            const assetTree = dialog.shadowRoot!.getElementById("parent-asset-tree") as OrAssetTree;
            let idd = assetTree.selectedIds!.length === 1 ? assetTree.selectedIds![0] : undefined;
            this._onAssetParentChange({parentId: idd, assetIds: this._assetIds});
        };

        const clearParent = () => {
            this._onAssetParentChange({parentId: undefined, assetIds: this._assetIds});
        };

        const dialogActions: DialogAction[] = [
            {
                actionName: "clear",
                content: "none",
                action: clearParent
            },
            {
                actionName: "ok",
                content: "ok",
                action: setParent
            },
            {
                default: true,
                actionName: "cancel",
                content: "cancel"
            }
        ];

        dialog = showDialog(new OrMwcDialog()
            .setContent(dialogContent)
            .setActions(dialogActions)
            .setStyles(html`
                <style>
                    .mdc-dialog__surface {
                        width: 400px;
                        height: 800px;
                        display: flex;
                        overflow: visible;
                        overflow-x: visible !important;
                        overflow-y: visible !important;
                    }

                    #dialog-content {
                        flex: 1;
                        overflow: visible;
                        min-height: 0;
                        padding: 0;
                    }

                    footer.mdc-dialog__actions {
                        border-top: 1px solid ${unsafeCSS(DefaultColor5)};
                    }

                    or-asset-tree {
                        height: 100%;
                    }
                </style>
            `)
            .setHeading(i18next.t("setParent"))
            .setDismissAction(null));
    }

    protected _onAssetSelectionRequested(event: OrAssetTreeRequestSelectionEvent) {
        const isModified = this._viewer && this._viewer.isModified();

        if (!isModified) {
            return;
        }

        // Prevent the request and check if user wants to lose changes
        event.detail.allow = false;

        this._confirmContinue(() => {
            this._onAssetSelectionChanged(event.detail.detail.newNodes.map((node) => node.asset.id!));
        });
    }

    protected _onAssetSelectionChanged(assetIds: string[] | undefined) {
        if (Util.objectsEqual(this._assetIds, assetIds)) {
            // User has clicked the same node(s)
            if (assetIds.length === 1) {
                // force refresh the selected asset
                if (this._viewer) {
                    this._viewer.assetId = undefined;
                    setTimeout(() => {
                        if (this._viewer) {
                            this._viewer.assetId = assetIds[0];
                        }
                    }, 0);
                }
            }
        } else {
            this._assetIds = assetIds;
            this._updateRoute(true);
        }
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
        this._updateRoute(false);
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

    protected async _onAssetParentChange(parentChange: ChangeParentEventDetail) {
        let parentId: string | undefined = parentChange.parentId;
        let assetsIds: string[] = parentChange.assetIds;

        try {
            if (parentId) {
                if ( !assetsIds.includes(parentId) ) {
                    await manager.rest.api.AssetResource.updateParent(parentId, { assetIds : assetsIds });
                } else {
                    showSnackbar(undefined, "moveAssetFailed", "dismiss");
                }
            } else {
                //So need to remove parent from all the selected assets
                await manager.rest.api.AssetResource.updateNoneParent({ assetIds : assetsIds });
            }
        } catch (e) {
            showSnackbar(undefined, "moveAssetFailed", "dismiss");
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

    protected _onLoadUserEvent(event: OrAssetViewerLoadUserEvent, silent: boolean = false) {
        router.navigate(getUsersRoute(event.detail), {
            callHooks: !silent,
            callHandler: !silent
        });
    }

    protected _onLoadAlarmEvent(event: OrAssetViewerLoadAlarmEvent, silent: boolean = false) {
        router.navigate(getAlarmsRoute(event.detail.toString()), {
            callHooks: !silent,
            callHandler: !silent
        });
    }
}
