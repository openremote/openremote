import {customElement, html, LitElement, property, PropertyValues, query, TemplateResult} from "lit-element";
import "@openremote/or-input";
import "@openremote/or-icon";
import {Asset, AssetQuery, AssetTreeNode, AssetType, Tenant} from "@openremote/model";
import "@openremote/or-translate";
import {style} from "./style";
import manager, {AssetModelUtil, OREvent, EventCallback} from "@openremote/core";
import {OrInputChangedEvent, InputType} from "@openremote/or-input";
import Qs from "qs";

export interface UiAssetTreeNode extends AssetTreeNode {
    selected: boolean;
    expandable: boolean;
    expanded: boolean;
    parent: UiAssetTreeNode;
    children: UiAssetTreeNode[];
}

interface RequestEventDetail<T> {
    allow: boolean,
    detail: T
}

interface NodeClickEventDetail {
    node: UiAssetTreeNode,
    clickEvent: MouseEvent
}

export class OrAssetTreeRequestSelectEvent extends CustomEvent<RequestEventDetail<NodeClickEventDetail>> {

    public static readonly NAME = "or-asset-tree-request-select";

    constructor(request: NodeClickEventDetail) {
        super(OrAssetTreeRequestSelectEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                allow: true,
                detail: request
            }
        });
    }
}

export class OrAssetTreeRequestAddEvent extends CustomEvent<RequestEventDetail<void>> {

    public static readonly NAME = "or-asset-tree-request-add";

    constructor() {
        super(OrAssetTreeRequestAddEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                allow: true,
                detail: undefined
            }
        });
    }
}

export class OrAssetTreeRequestDeleteEvent extends CustomEvent<RequestEventDetail<UiAssetTreeNode[]>> {

    public static readonly NAME = "or-asset-tree-request-delete";

    constructor(request: UiAssetTreeNode[]) {
        super(OrAssetTreeRequestDeleteEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                allow: true,
                detail: request
            }
        });
    }
}

export class OrAssetTreeRequestCopyEvent extends CustomEvent<RequestEventDetail<UiAssetTreeNode>> {

    public static readonly NAME = "or-asset-tree-request-copy";

    constructor(request: UiAssetTreeNode) {
        super(OrAssetTreeRequestCopyEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                allow: true,
                detail: request
            }
        });
    }
}

export class OrAssetTreeSelectionChangedEvent extends CustomEvent<UiAssetTreeNode[]> {

    public static readonly NAME = "or-asset-tree-selection-changed";

    constructor(nodes: UiAssetTreeNode[]) {
        super(OrAssetTreeSelectionChangedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: nodes
        });
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrAssetTreeRequestSelectEvent.NAME]: OrAssetTreeRequestSelectEvent;
        [OrAssetTreeRequestAddEvent.NAME]: OrAssetTreeRequestAddEvent;
        [OrAssetTreeRequestDeleteEvent.NAME]: OrAssetTreeRequestDeleteEvent;
        [OrAssetTreeRequestCopyEvent.NAME]: OrAssetTreeRequestCopyEvent;
        [OrAssetTreeSelectionChangedEvent.NAME]: OrAssetTreeSelectionChangedEvent;
    }
}

//TODO: Add websocket support
//TODO: Make modal a standalone component
@customElement("or-asset-tree")
export class OrAssetTree extends LitElement {

    static get styles() {
        return [
            style
        ];
    }

    /**
     * Allows arbitrary assets to be displayed using a tree
     */
    @property({type: Array, reflect: false})
    public assets?: Asset[];

    @property({type: Array})
    public assetIds?: string[];

    @property({type: Array})
    public rootAssets?: Asset[];

    @property({type: Array})
    public rootAssetIds?: string[];

    @property({type: String})
    public realm?: string;

    @property({type: Boolean})
    public multiSelect?: boolean = true;

    @property({type: Function, noAccessor: true, attribute: false})
    public selector?: (node: UiAssetTreeNode) => boolean;

    @property({type: Array, noAccessor: true})
    public selectTypes?: string[];

    @property({type: Array})
    public selectedIds?: string[];

    @property({type: String})
    public sortBy?: string;

    @property({attribute: false})
    protected _realms?: Tenant[];

    @property({attribute: false})
    protected _nodes?: UiAssetTreeNode[];

    @property()
    protected _showLoading: boolean = true;

    protected _selectedNodes: UiAssetTreeNode[] = [];

    @query("#sort-menu")
    protected _sortMenu!: HTMLDivElement;

    @query("#delete-not-allowed-modal")
    protected _deleteNotAllowedModal!: HTMLDivElement;

    @query("#delete-confirm-modal")
    protected _deleteConfirmModal!: HTMLDivElement;

    protected _initCallback?: EventCallback;
    protected _ready = false;

    protected _onReady() {
        this._ready = true;
        this._loadRealms();
        this._loadAssets();
    }

    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);

        if (!manager.ready) {
            // Defer until openremote is initialised
            this._initCallback = (initEvent: OREvent) => {
                if (initEvent === OREvent.READY) {
                    this._onReady();
                    manager.removeListener(this._initCallback!);
                }
            };
            manager.addListener(this._initCallback);
        } else {
            this._onReady();
        }

        this.addEventListener("click", (evt) => {
            if (this._sortMenu.hasAttribute("data-visible") && !this._sortMenu.contains(evt.target as Node)) {
                this._sortMenu.toggleAttribute("data-visible");
            }
            if (this._deleteNotAllowedModal.hasAttribute("data-visible") && !this._deleteNotAllowedModal.contains(evt.target as Node)) {
                this._deleteNotAllowedModal.toggleAttribute("data-visible");
            }
            if (this._deleteConfirmModal.hasAttribute("data-visible") && !this._deleteConfirmModal.contains(evt.target as Node)) {
                this._deleteConfirmModal.toggleAttribute("data-visible");
            }
        });
    }

    protected render() {

        return html`
            <div id="header">
                <div id="title-container">
                    <or-translate id="title" value="asset_plural"></or-translate>
                    ${manager.isSuperUser() ? html `<or-input id="realm-picker" type="${InputType.SELECT}" .value="${this._getRealm()}" .options="${this._realms ? this._realms.map((tenant) => [tenant.realm, tenant.displayName]) : []}" @or-input-changed="${(evt: OrInputChangedEvent) => this._onRealmChanged(evt)}"></or-input>` : ``}
                </div>

                <div id="header-btns">
                    <button ?hidden="${!this.selectedIds || this.selectedIds.length === 0}" @click="${() => this._onCopyClicked()}"><or-icon icon="content-copy"></or-icon></button>
                    <button ?hidden="${!this.selectedIds || this.selectedIds.length === 0}" @click="${() => this._onDeleteClicked()}"><or-icon icon="delete"></or-icon></button>
                    <button @click="${() => this._onAddClicked()}"><or-icon icon="plus"></or-icon></button>
                    <button @click="${() => this._onSearchClicked()}"><or-icon icon="magnify"></or-icon></button>
                    <button @click="${() => this._onSortClicked()}"><or-icon icon="sort-variant"></or-icon></button>
                    <div class="modal-container">
                        <div class="modal" id="sort-menu">
                            <div class="modal-content">
                                <ul>
                                    <li @click="${() => this.sortBy = "name"}" ?data-selected="${!this.sortBy || this.sortBy === "name"}"><or-icon icon="check"></or-icon><or-translate value="name"></or-translate></li>
                                    <li @click="${() => this.sortBy = "type"}" ?data-selected="${this.sortBy === "type"}"><or-icon icon="check"></or-icon><or-translate value="assetType"></or-translate></li>
                                    <li @click="${() => this.sortBy = "created"}" ?data-selected="${this.sortBy === "created"}"><or-icon icon="check"></or-icon><or-translate value="creationDate"></or-translate></li>
                                    <li @click="${() => this.sortBy = "status"}" ?data-selected="${this.sortBy === "status"}"><or-icon icon="check"></or-icon><or-translate value="status"></or-translate></li>
                                </ul>
                            </div>
                        </div>
                    </div>
                    <div class="modal-container">
                        <div class="modal" id="delete-not-allowed-modal">
                            <div class="modal-content">
                                <p>
                                    <or-translate value="deleteAssetsNoChildrenAllowed"></or-translate>
                                </p>
                                <div class="modal-btns">
                                    <span class="btn" @click="${() => this._deleteNotAllowedModal.toggleAttribute("data-visible")}"><or-translate value="ok"></or-translate></span>                                
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="modal-container">
                        <div class="modal" id="delete-confirm-modal">
                            <div class="modal-content">
                                <p>
                                    <or-translate value="confirmDeleteAssets"></or-translate>
                                </p>
                                <div class="modal-btns">
                                    <span class="btn" @click="${() => this._deleteConfirmModal.toggleAttribute("data-visible")}"><or-translate value="cancel"></or-translate></span>
                                    <span class="btn" @click="${() => this._doDelete()}"><or-translate value="ok"></or-translate></span>                                
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            ${!this._nodes || this._showLoading
                ? html`
                    <span id="loading">LOADING</span>` 
                : html`
                    <div id="list-container">
                        <ol id="list">
                            ${this._nodes.map((treeNode) => this._treeNodeTemplate(treeNode, 0))}
                        </ol>
                    </div>
                `
            }

            <div id="footer">
            
            </div>
        `;
    }

    protected _treeNodeTemplate(treeNode: UiAssetTreeNode, level: number): TemplateResult | string {

        const descriptor = AssetModelUtil.getAssetDescriptor(treeNode.asset!.type!);

        return html`
            <li ?data-selected="${treeNode.selected}" ?data-expanded="${treeNode.expanded}" @click="${(evt: MouseEvent) => this._onNodeClicked(evt, treeNode)}">
                <div class="node-container" style="padding-left: ${level*22}px">
                    <div class="node-name">
                        <div class="expander" ?data-expandable="${treeNode.expandable}"></div>
                        <or-icon style="--or-icon-fill: ${descriptor && descriptor.color ? "#" + descriptor.color : "unset"}" icon="${descriptor && descriptor.icon ? descriptor.icon : AssetType.THING.icon}"></or-icon>
                        <span>${treeNode.asset!.name}</span>
                    </div>
                </div>
                <ol>
                    ${!treeNode.children ? `` : treeNode.children.map((childNode) => this._treeNodeTemplate(childNode, level + 1))}
                </ol>
            </li>
        `;
    }

    protected shouldUpdate(_changedProperties: PropertyValues): boolean {
        const result = super.shouldUpdate(_changedProperties);

        if (_changedProperties.has("assetIds")
        || _changedProperties.has("rootAssets")
        || _changedProperties.has("rootAssetIds")
        || _changedProperties.has("realm")) {
            this._nodes = undefined;
        }

        if (!this._nodes) {
            this._loadAssets();
            return true;
        }

        if (_changedProperties.has("selectedIds")) {
            this._updateSelectedNodes();
        }

        if (_changedProperties.has("sortBy")) {
            this._updateSort(this._nodes!, this._getSortFunction());
        }

        return result;
    }

    protected _loadRealms() {
        if (manager.isSuperUser()) {
            manager.rest.api.TenantResource.getAll().then((response) => {
                this._realms = response.data;
            });
        }
    }

    public get selectedNodes(): UiAssetTreeNode[] {
        return this._selectedNodes ? [...this._selectedNodes] : [];
    }

    protected _updateSelectedNodes() {
        const actuallySelectedIds: string[] = [];
        const selectedNodes: UiAssetTreeNode[] = [];
        OrAssetTree._forEachNodeRecursive(this._nodes!, (node) => {
            if (this.selectedIds && this.selectedIds.indexOf(node.asset!.id!) >= 0) {
                actuallySelectedIds.push(node.asset!.id!);
                selectedNodes.push(node);
                node.selected = true;
                // Expand every ancestor
                let parent = node.parent;
                while (parent) {
                    parent.expanded = true;
                    parent = parent.parent;
                }
            } else {
                node.selected = false;
            }
        });

        this.selectedIds = actuallySelectedIds;
        this._selectedNodes = selectedNodes;
        this.dispatchEvent(new OrAssetTreeSelectionChangedEvent(this._selectedNodes));
    }


    protected _updateSort(nodes: UiAssetTreeNode[], sortFunction: (a: UiAssetTreeNode, b: UiAssetTreeNode) => number) {
        if (!nodes) {
            return;
        }

        nodes.sort(sortFunction);
        nodes.forEach((node) => this._updateSort(node.children, sortFunction));
    }

    protected _onNodeClicked(evt: MouseEvent, node: UiAssetTreeNode) {
        if (evt.defaultPrevented) {
            return;
        }

        evt.preventDefault();

        const isExpander = (evt.target as HTMLElement).className.indexOf("expander") >= 0;

        if (isExpander) {
            if (node.expandable) {
                node.expanded = !node.expanded;
                const elem = (evt.target as HTMLElement).parentElement!.parentElement!.parentElement!;
                elem.toggleAttribute("data-expanded");
            }
        } else {
            let canSelect = true;

            if (this.selector) {
                canSelect = this.selector(node);
            } else if (this.selectTypes) {
                canSelect = this.selectTypes.indexOf(node.asset!.type!) >= 0;
            }

            if (canSelect) {
                this._doRequest(new OrAssetTreeRequestSelectEvent({
                    node: node,
                    clickEvent: evt
                }), (detail) => this._doSelect(detail));
            }
        }
    }

    protected _onCopyClicked() {
        if (this._selectedNodes.length > 0) {
            this._doRequest(new OrAssetTreeRequestCopyEvent(this._selectedNodes[0]), (node) => this._doCopy(node));
        }
    }

    protected _onDeleteClicked() {
        this._doRequest(new OrAssetTreeRequestDeleteEvent(this._selectedNodes), (nodes) => this._showDeleteModal());
    }

    protected _onAddClicked() {
        this._doRequest(new OrAssetTreeRequestAddEvent(), () => this._doAdd());
    }

    protected _onSearchClicked() {

    }

    protected _onSortClicked() {
        // Do open on next task to prevent click handler closing it immediately
        if (!this._sortMenu.hasAttribute("data-visible")) {
            window.setTimeout(() => {
                this._sortMenu.toggleAttribute("data-visible");
            });
        }
    }

    protected _doSelect(detail: NodeClickEventDetail) {
        const node = detail.node;
        const evt = detail.clickEvent;

        let selectedIds = this.selectedIds ? this.selectedIds : [];
        const index = selectedIds.indexOf(node.asset!.id!);
        let select = true;
        let deselectOthers = true;

        if (this.multiSelect) {
            if (evt.ctrlKey || evt.metaKey) {
                deselectOthers = false;
                if (index >= 0 && selectedIds.length > 1) {
                    select = false;
                }
            }
        }

        if (deselectOthers) {
            selectedIds = [node.asset!.id!];
        } else if (select) {
            if (index < 0) {
                selectedIds = [...selectedIds];
                selectedIds.push(node.asset!.id!);
            }
        } else {
            if (index >= 0) {
                selectedIds.splice(index, 1);
                selectedIds = [...selectedIds];
            }
        }

        this.selectedIds = selectedIds;
    }

    protected _showDeleteModal() {

        if (!this._selectedNodes) {
            return;
        }

        // Check each selected node has no children
        if (this._selectedNodes.find((node) => node.children && node.children.length > 0)) {
            // Do open on next task to prevent click handler closing it immediately
            window.setTimeout(() => {
                this._deleteNotAllowedModal.toggleAttribute("data-visible");
            });
            return;
        }

        // Do open on next task to prevent click handler closing it immediately
        window.setTimeout(() => {
            this._deleteConfirmModal.toggleAttribute("data-visible");
        });
    }

    protected _doDelete() {
        if (!this._selectedNodes) {
            return;
        }

        manager.rest.api.AssetResource.delete({
            assetId: this._selectedNodes.map((node) => node.asset!.id!)
        }, {
            paramsSerializer: params => Qs.stringify(params, {arrayFormat: 'repeat'})
        }).then((response) => {
            if (response.status !== 200) {
                // TODO: Error announcement
            }

            // Clear nodes to refetch them
            this._nodes = undefined;
        }).catch((reason) => {
            // TODO: Error announcement
            // Clear nodes to refetch them
            this._nodes = undefined;
        });
        this._showLoading = true;
    }

    protected _doAdd() {

    }

    protected _doCopy(node: UiAssetTreeNode) {

    }

    protected _getSortFunction(): (a: UiAssetTreeNode, b: UiAssetTreeNode) => number {

        const nameSort = (a: UiAssetTreeNode, b: UiAssetTreeNode) => { return a.asset!.name! < b.asset!.name! ? -1 : a.asset!.name! > b.asset!.name! ? 1 : 0 };

        switch (this.sortBy) {
            case "created":
                return (a, b) => { return a.asset!.createdOn! < b.asset!.createdOn! ? -1 : a.asset!.createdOn! > b.asset!.createdOn! ? 1 : nameSort(a, b) };
            case "status":
                return (a, b) => { return a.asset!.name! < b.asset!.name! ? -1 : a.asset!.name! > b.asset!.name! ? 1 : nameSort(a, b) };
            case "type":
                return (a, b) => { return a.asset!.type! < b.asset!.type! ? -1 : a.asset!.type! > b.asset!.type! ? 1 : nameSort(a, b) };
            default:
                return nameSort;
        }
    }

    protected _loadAssets() {

        if (!this._ready) {
            return;
        }

        const sortFunction = this._getSortFunction();

        if (!this.assets) {
            let query: AssetQuery = {
                tenant: {
                    realm: this._getRealm()
                },
                select: {
                    excludePath: true,
                    excludeRealm: true,
                    excludeAttributes: true,
                    excludeParentInfo: false
                }
            };

            if (this.assetIds) {
                query.ids = this.assetIds;
                query.recursive = true;
            } else if (this.rootAssets) {
                query.ids = this.rootAssets.map((asset) => asset.id!);
                query.recursive = true;
            } else if (this.rootAssetIds) {
                query.ids = this.rootAssetIds;
                query.recursive = true;
            }

            manager.rest.api.AssetResource.queryAssets(query).then((response) => {
                this._buildTreeNodes(response.data, sortFunction);
            });
        } else {
            this._buildTreeNodes(this.assets, sortFunction);
        }
    }

    protected _buildTreeNodes(assets: Asset[], sortFunction: (a: UiAssetTreeNode, b: UiAssetTreeNode) => number) {
        if (!assets || assets.length === 0) {
            this._nodes = [];
        }

        let rootAssetIds: string[] | undefined;

        if (this.rootAssetIds) {
            rootAssetIds = this.rootAssetIds;
        } else if (this.rootAssets) {
            rootAssetIds = this.rootAssets.map((ra) => ra.id!);
        }

        let rootAssets: UiAssetTreeNode[];

        if (rootAssetIds) {
            rootAssets = assets.filter((asset) => rootAssetIds!.indexOf(asset.id!) >= 0).map((asset) => {
                return {
                    asset: asset
                } as UiAssetTreeNode;
            });
        } else {
            rootAssets = assets.filter((asset) => !asset.parentId).map((asset) => {
                return {
                    asset: asset
                } as UiAssetTreeNode;
            });
        }

        rootAssets.sort(sortFunction);
        rootAssets.forEach((rootAsset) => this._buildChildTreeNodes(rootAsset, assets, sortFunction));

        this._nodes = rootAssets;
        this._showLoading = false;
    }

    protected _buildChildTreeNodes(treeNode: UiAssetTreeNode, assets: Asset[], sortFunction: (a: UiAssetTreeNode, b: UiAssetTreeNode) => number) {
        treeNode.children = assets.filter((asset) => asset.parentId === treeNode.asset!.id).map((asset) => {
            return {
                asset: asset
            } as UiAssetTreeNode;
        }).sort(sortFunction);

        if (treeNode.children.length > 0) {
            treeNode.expandable = true;
        }

        treeNode.children.forEach((childNode) => {
            childNode.parent = treeNode;
            this._buildChildTreeNodes(childNode, assets, sortFunction);
        });
    }

    protected _getRealm(): string | undefined {
        if (manager.isSuperUser() && this.realm) {
            return this.realm;
        }

        return manager.getRealm();
    }

    protected _onRealmChanged(evt: OrInputChangedEvent) {
        this.realm = evt.detail.value;
        this.assets = undefined;
    }

    protected _doRequest<T>(event: CustomEvent<RequestEventDetail<T>>, handler: (detail: T) => void) {
        this.dispatchEvent(event);
        window.setTimeout(() => {
            if (event.detail.allow) {
                handler(event.detail.detail);
            }
        })
    }

    protected static _forEachNodeRecursive(nodes: UiAssetTreeNode[], fn: (node: UiAssetTreeNode) => void) {
        if (!nodes) {
            return;
        }

        nodes.forEach((node) => {
            fn(node);
            this._forEachNodeRecursive(node.children, fn);
        });
    }
}
