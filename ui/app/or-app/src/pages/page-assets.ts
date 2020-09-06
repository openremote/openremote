import {css, customElement, html, property, TemplateResult, unsafeCSS, query} from "lit-element";
import "@openremote/or-asset-tree";
import "@openremote/or-asset-viewer";
import {ViewerConfig, OrAssetViewer} from "@openremote/or-asset-viewer";
import {AssetTreeConfig, OrAssetTreeSelectionChangedEvent, OrAssetTreeRequestSelectEvent, OrAssetTreeRequestEditToggleEvent, OrAssetTreeEditChangedEvent} from "@openremote/or-asset-tree";
import {DefaultBoxShadow} from "@openremote/core";
import {AppStateKeyed} from "../app";
import {Page, router} from "../types";
import {EnhancedStore} from "@reduxjs/toolkit";
import { showDialog } from "@openremote/or-mwc-components/dist/or-mwc-dialog";
import i18next from "i18next";

export interface PageAssetsConfig {
    viewer: ViewerConfig;
    tree?: AssetTreeConfig;
}

export function pageAssetsProvider<S extends AppStateKeyed>(store: EnhancedStore<S>, config?: PageAssetsConfig) {
    return {
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
                }
            }
        `;
    }

    @property()
    public config?: PageAssetsConfig;

    @property()
    protected _editMode: boolean;

    @property()
    protected _assetId;

    @query("#viewer")
    protected _viewer!: OrAssetViewer;

    get name(): string {
        return "assets";
    }

    constructor(store: EnhancedStore<S>) {
        super(store);
    }

    connectedCallback() {
        super.connectedCallback();
        this.addEventListener(OrAssetTreeSelectionChangedEvent.NAME, this._onTreeSelectionChanged);
        this.addEventListener(OrAssetTreeRequestSelectEvent.NAME, this._onTreeSelectionRequested);
        this.addEventListener(OrAssetTreeRequestEditToggleEvent.NAME, this._onTreeEditToggleRequested);
        this.addEventListener(OrAssetTreeEditChangedEvent.NAME, this._onTreeEditChanged);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener(OrAssetTreeSelectionChangedEvent.NAME, this._onTreeSelectionChanged);
        this.removeEventListener(OrAssetTreeRequestSelectEvent.NAME, this._onTreeSelectionRequested);
    }

    protected render(): TemplateResult | void {
        const selectedIds = this._assetId ? [this._assetId] : undefined;
        return html`
              <or-asset-tree id="tree" .editMode="${!!this._editMode}" .config="${this.config && this.config.tree ? this.config.tree : null}" class="${this._assetId ? "hideMobile" : ""}" .selectedIds="${selectedIds}"></or-asset-tree>
              <or-asset-viewer id="viewer" .editMode="${!!this._editMode}" class="${!this._assetId ? "hideMobile" : ""}" .config="${this.config && this.config.viewer ? this.config.viewer : undefined}"></or-asset-viewer>
        `;
    }

    stateChanged(state: S) {
        const editMode = state.app.params && state.app.params.editMode === "true";
        const id = state.app.params && state.app.params.id ? state.app.params.id : undefined;
        this._assetId = id;
        this._editMode = editMode;
    }

    protected _onTreeSelectionRequested(event: OrAssetTreeRequestSelectEvent) {
        // Block the navigation and show dialog then navigate
        event.detail.allow = false;
        const assetId = event.detail.detail.node.asset.id;
        const action = () => {
            this._assetId = assetId;
            this._updateRoute(false);
        };
        this._checkIfModified(action);
    }

    protected _onTreeSelectionChanged(event: OrAssetTreeSelectionChangedEvent) {
        const nodes = event.detail;
        const assetId = nodes[0] ? nodes[0].asset.id : undefined;
        this._assetId = assetId;
        this._viewer.assetId = assetId;
        this._updateRoute();
    }

    protected _onTreeEditToggleRequested(event: OrAssetTreeRequestEditToggleEvent) {
        // Block the request and show dialog then toggle
        event.detail.allow = false;
        const editMode = event.detail.detail;
        const action = () => {
            this._editMode = editMode;
            this._updateRoute(false);
        };
        this._checkIfModified(action);
    }

    protected _onTreeEditChanged(event: OrAssetTreeEditChangedEvent) {
        this._editMode = event.detail;
        this._updateRoute();
    }

    protected _checkIfModified(action: () => void) {
        if (this._viewer.isModified()) {
            showDialog(
                {
                    content: html`<p>${i18next.t("confirmContinueAssetModified")}</p>`,
                    actions: [
                        {
                            actionName: "ok",
                            content: "ok",
                            action: action
                        },
                        {
                            actionName: "cancel",
                            content: "cancel",
                            default: true
                        }
                    ],
                    title: "assetModified"
                }
            )
        } else {
            action();
        }
    }

    protected _updateRoute(silent: boolean = true) {
        if (silent) {
            router.pause();
        }
        router.navigate(getAssetsRoute(this._editMode, this._assetId));
        if (silent) {
            router.resume();
        }
    }
}
