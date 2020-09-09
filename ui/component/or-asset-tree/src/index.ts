import {customElement, html, LitElement, property, PropertyValues, query, TemplateResult} from "lit-element";
import {styleMap} from "lit-html/directives/style-map";
import "@openremote/or-input";
import "@openremote/or-icon";
import {Asset, AssetQuery, AssetTreeNode, ClientRole, SharedEvent, AssetsEvent, AssetEvent, AssetEventCause, AssetDescriptor, AssetType} from "@openremote/model";
import "@openremote/or-translate";
import {style} from "./style";
import manager, {AssetModelUtil, Util, OREvent, EventCallback, subscribe} from "@openremote/core";
import {InputType, OrInputChangedEvent} from "@openremote/or-input";
import Qs from "qs";
import {getAssetDescriptorIconTemplate, OrIcon} from "@openremote/or-icon";
import "@openremote/or-mwc-components/dist/or-mwc-menu";
import {getContentWithMenuTemplate, MenuItem} from "@openremote/or-mwc-components/dist/or-mwc-menu";
import "@openremote/or-mwc-components/dist/or-mwc-list";
import {ListItem, OrMwcListChangedEvent} from "@openremote/or-mwc-components/dist/or-mwc-list";
import {i18next, OrTranslate} from "@openremote/or-translate";
import "@openremote/or-mwc-components/dist/or-mwc-dialog";
import {showDialog} from "@openremote/or-mwc-components/dist/or-mwc-dialog";

export interface AssetTreeTypeConfig {
    include?: string[];
    exclude?: string[];
}

export interface AssetTreeConfig {
    default?: AssetTreeTypeConfig;
    addAssetTypes?: AssetTreeTypeConfig;
}

export interface UiAssetTreeNode extends AssetTreeNode {
    selected: boolean;
    expandable: boolean;
    expanded: boolean;
    parent: UiAssetTreeNode;
    children: UiAssetTreeNode[];
}

interface RequestEventDetail<T> {
    allow: boolean;
    detail: T;
}

interface NodeClickEventDetail {
    node: UiAssetTreeNode;
    clickEvent: MouseEvent;
}

export {style};

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

export class OrAssetTreeRequestEditToggleEvent extends CustomEvent<RequestEventDetail<boolean>> {

    public static readonly NAME = "or-asset-tree-request-edit-toggle";

    constructor(edit: boolean) {
        super(OrAssetTreeRequestEditToggleEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                allow: true,
                detail: edit
            }
        });
    }
}

export class OrAssetTreeEditChangedEvent extends CustomEvent<boolean> {

    public static readonly NAME = "or-asset-tree-edit-changed";

    constructor(edit: boolean) {
        super(OrAssetTreeEditChangedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: edit
        });
    }
}

export class OrAssetTreeRequestAddEvent extends CustomEvent<RequestEventDetail<Asset>> {

    public static readonly NAME = "or-asset-tree-request-add";

    constructor(asset: Asset) {
        super(OrAssetTreeRequestAddEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                allow: true,
                detail: asset
            }
        });
    }
}

export class OrAssetTreeAssetAddedEvent extends CustomEvent<Asset> {

    public static readonly NAME = "or-asset-tree-asset-added";

    constructor(asset: Asset) {
        super(OrAssetTreeRequestAddEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: asset
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
        [OrAssetTreeRequestEditToggleEvent.NAME]: OrAssetTreeRequestEditToggleEvent;
        [OrAssetTreeEditChangedEvent.NAME]: OrAssetTreeEditChangedEvent;
    }
}

export const getAssetTypes = async () => {
    const response = await manager.rest.api.AssetResource.queryAssets({
        select: {
            excludeAttributes: true,
            excludeParentInfo: true,
            excludePath: true,
            excludeAttributeMeta: true,
            excludeAttributeTimestamp: true,
            excludeAttributeType: true,
            excludeAttributeValue: true
        },
        recursive: true
    });

    if(response && response.data) {
        return response.data.map(asset => asset.type!);
    }
}

// TODO: Add websocket support
// TODO: Make modal a standalone component
@customElement("or-asset-tree")
export class OrAssetTree extends subscribe(manager)(LitElement) {

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

    @property({type: Object})
    public assetDescriptors?: AssetDescriptor[];

    @property({type: Array})
    public _assetIdsOverride?: string[];

    @property({type: Array})
    public _assetTypeOptions?: string[];

    @property({type: Array})
    public rootAssets?: Asset[];

    @property({type: Array})
    public rootAssetIds?: string[];

    @property({type: Boolean})
    public readonly: boolean = false;

    @property({type: Boolean})
    public disabled: boolean = false;

    @property({type: Boolean})
    public multiSelect?: boolean = true;

    @property({type: Boolean})
    public editMode?: boolean = false;

    @property({type: Function, noAccessor: true, attribute: false})
    public selector?: (node: UiAssetTreeNode) => boolean;

    @property({type: Array, noAccessor: true})
    public selectTypes?: string[];

    @property({type: Array})
    public selectedIds?: string[];

    @property({type: String})
    public sortBy?: string = "name";

    protected config?: AssetTreeConfig;

    @property({attribute: false})
    protected _nodes?: UiAssetTreeNode[];

    protected _connected: boolean = false;
    protected _selectedNodes: UiAssetTreeNode[] = [];
    protected _initCallback?: EventCallback;
    protected _addedAssetId?: string;

    public get selectedNodes(): UiAssetTreeNode[] {
        return this._selectedNodes ? [...this._selectedNodes] : [];
    }

    /**
     * Override subscribe mixin behaviour to get re-render
     */
    public set assetIds(assetIds: string[]) {
        this._assetIdsOverride = assetIds;
        super.assetIds = assetIds;
    }

    public connectedCallback() {
        super.connectedCallback();
        manager.addListener(this.onManagerEvent);
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
        manager.removeListener(this.onManagerEvent);
    }

    protected onManagerEvent = (event: OREvent) => {
        switch (event) {
            case OREvent.DISPLAY_REALM_CHANGED:
                this._nodes = undefined;
                break;
        }
    }

    protected render() {
        const editMode = this.editMode;

        return html`
            <div id="header">
                <div id="title-container">
                    <or-translate id="title" value="asset_plural"></or-translate>
                </div>

                <div id="header-btns">                
                    <or-input style="display: none;" ?hidden="${this._isReadonly() || !editMode || !this.selectedIds || this.selectedIds.length === 0}" type="${InputType.BUTTON}" icon="content-copy" @click="${() => this._onCopyClicked()}"></or-input>
                    <or-input ?hidden="${this._isReadonly() || !editMode || !this.selectedIds || this.selectedIds.length === 0}" type="${InputType.BUTTON}" icon="delete" @click="${() => this._onDeleteClicked()}"></or-input>
                    <or-input ?hidden="${this._isReadonly() || !editMode}" type="${InputType.BUTTON}" icon="plus" @click="${() => this._onAddClicked()}"></or-input>
                    <or-input ?hidden="${this._isReadonly()}" type="${InputType.BUTTON_TOGGLE}" icon="eye" .value="${this.editMode}" iconOn="pencil" @or-input-changed="${(ev: OrInputChangedEvent) => this._onEditToggled(ev.detail.value)}"></or-input>
                    <or-input hidden type="${InputType.BUTTON}" icon="magnify" @click="${() => this._onSearchClicked()}"></or-input>
                    
                    ${getContentWithMenuTemplate(
                            html`<or-input type="${InputType.BUTTON}" icon="sort-variant"></or-input>`,
                            ["name", "type", "createdOn", "status"].map((sort) => { return {value: sort, text: i18next.t(sort)} as MenuItem; }),
                            this.sortBy,
                            (v) => this._onSortClicked(v as string))}
                </div>
            </div>

            ${!this._nodes
                ? html`
                    <span id="loading"><or-translate value="loading"></or-translate></span>`
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

    protected _isReadonly() {
        return this.readonly || !manager.hasRole(ClientRole.WRITE_ASSETS);
    }

    protected shouldUpdate(_changedProperties: PropertyValues): boolean {
        if (this.editMode && this._isReadonly()) {
            this.editMode = false;
        }

        const result = super.shouldUpdate(_changedProperties);
        if (_changedProperties.has("_assetIdsOverride")
            || _changedProperties.has("assets")
            || _changedProperties.has("rootAssets")
            || _changedProperties.has("rootAssetIds")) {
            this._nodes = undefined;
        }

        if (!this._nodes) {
            this._loadAssets();
            return true;
        }

        if (_changedProperties.has("selectedIds")) {
            if (!Util.objectsEqual(_changedProperties.get("selectedIds"), this.selectedIds)) {
                this._updateSelectedNodes();
            }
        }

        if (_changedProperties.has("sortBy")) {
            this._updateSort(this._nodes!, this._getSortFunction());
        }

        return result;
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
        this._doRequest(new OrAssetTreeRequestDeleteEvent(this._selectedNodes), (nodes) => this._doDelete());
    }

    protected _onAddClicked() {

        const includedAssetTypes = this.config && this.config.addAssetTypes && this.config.addAssetTypes.include ? this.config.addAssetTypes.include : undefined;
        const excludedAssetTypes = this.config && this.config.addAssetTypes && this.config.addAssetTypes.exclude ? this.config.addAssetTypes.exclude : undefined;

        const listItems: ListItem[] = AssetModelUtil.getAssetDescriptors()
            .filter((descriptor) => (!includedAssetTypes || includedAssetTypes.some((inc) => Util.stringMatch(inc, descriptor.type!)))
                && (!excludedAssetTypes || !excludedAssetTypes.some((exc) => Util.stringMatch(exc, descriptor.type!))))
            .sort(Util.sortByString((descriptor) => descriptor.name!))
            .map((descriptor) => {
                return {
                    styleMap: {
                        "--or-icon-fill": descriptor.color ? "#" + descriptor.color : "unset"
                    },
                    icon: descriptor.icon,
                    text: i18next.t(descriptor.name!, {defaultValue: descriptor.name!.replace(/_/g, " ").toLowerCase()}),
                    value: descriptor.type!,
                    data: descriptor
                }
            });

        let descriptor: AssetDescriptor;

        const onAssetTypeChanged = (listItem: ListItem) => {
            descriptor = listItem.data as AssetDescriptor;
            const icon = dialog.shadowRoot!.getElementById("type-icon") as OrIcon;
            const description = dialog.shadowRoot!.getElementById("type-description") as OrTranslate;
            icon.icon = descriptor.icon;
            description.value = listItem.text;
        };

        const dialog = showDialog(
            {
                title: i18next.t("addAsset"),
                content: html`
                    <form id="mdc-dialog-form-add">
                        <div id="type-list">
                            <or-mwc-list @or-mwc-list-changed="${(evt: OrMwcListChangedEvent) => onAssetTypeChanged(evt.detail[0] as ListItem)}" .listItems="${listItems}" id="menu"></or-mwc-list>
                        </div>
                        <div id="asset-type-option-container">
                            <or-icon id="type-icon" class="hidden"></or-icon>
                            <or-translate id="type-description" class="hidden"></or-translate>
                        </div>
                    </form>`,
                actions: [
                    {
                        actionName: "cancel",
                        content: html`<or-input class="button" .type="${InputType.BUTTON}" .label="${i18next.t("cancel")}"></or-input>`
                    },
                    {
                        actionName: "yes",
                        default: true,
                        content: html`<or-input class="button" .type="${InputType.BUTTON}" label="${i18next.t("add")}" data-mdc-dialog-action="yes"></or-input>`,
                        action: () => {
                            // TODO change this to open edit page
                            if (!descriptor) {
                                return;
                            }

                            const asset: Asset = {
                                name: "New Asset",
                                type: descriptor.type,
                                realm: manager.getRealm()
                            };
                            if(this.selectedIds) {
                                asset['parentId'] = this.selectedIds[0];
                            }
                            this._doRequest(new OrAssetTreeRequestAddEvent(asset), () => this._doAdd(asset));
                        }
                    }
                ],
                styles: html`
                    <style>
                        #mdc-dialog-form-add {
                            display: flex;
                            height:400px;
                            border-top: 1px solid var(--or-app-color2);
                        }
                        #asset-type-option-container {
                            background-color: var(--or-app-color2);
                            padding: 15px;
                            width: 260px;
                            max-width: 100%;
                            font-size: 16px;
                        }
                        #type-list {
                            overflow-y: scroll;
                            text-transform: capitalize;
                        }
                        .hidden {
                            visibility: hidden;
                        }
                    </style>
                `
            }
        )
    }

    protected _onEditToggled(edit: boolean) {
        this._doRequest(new OrAssetTreeRequestEditToggleEvent(edit), () => this._doEditToggle(edit));
    }

    protected _doEditToggle(edit: boolean) {
        this.editMode = edit;
    }

    protected _onSearchClicked() {

    }

    protected _onSortClicked(sortBy: string) {
        this.sortBy = sortBy;
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

    protected _doDelete() {

        if (!this._selectedNodes) {
            return;
        }

        // Check each selected node has no children
        if (this._selectedNodes.find((node) => node.children && node.children.length > 0)) {
            window.alert(i18next.t("deleteAssetsNoChildrenAllowed"));
            return;
        }

        if (!this._okToDelete()) {
            return;
        }

        this.disabled = true;
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
            // Clear nodes to re-fetch them
            this._nodes = undefined;
        });
        this.disabled = true;
    }

    protected _okToDelete() {
        return window.confirm(i18next.t("confirmDeleteAssets"));
    }

    protected async _doAdd(asset: Asset) {
        const response = await manager.rest.api.AssetResource.create(asset);
        this._addedAssetId = response.data.id!;
        this.dispatchEvent(new OrAssetTreeAssetAddedEvent(response.data));
    }

    protected _doCopy(node: UiAssetTreeNode) {

    }

    protected _getSortFunction(): (a: UiAssetTreeNode, b: UiAssetTreeNode) => number {
        return (a, b) => (a.asset as any)![this.sortBy!] < (b.asset as any)![this.sortBy!] ? -1 : (a.asset as any)![this.sortBy!] > (b.asset as any)![this.sortBy!] ? 1 : 0;
    }

    protected _loadAssets() {

        const sortFunction = this._getSortFunction();

        if (!this.assets) {

            if (!this._connected) {
                return;
            }

            const query: AssetQuery = {
                tenant: {
                    realm: manager.isSuperUser() ? manager.displayRealm : manager.getRealm()
                },
                select: { // Just need the basic asset info
                    excludeAttributes: true,
                    excludePath: true,
                    excludeParentInfo: true
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
            this._sendEventWithReply({
                event: {
                    eventType: "read-assets",
                    assetQuery: query
                }
            })
                .then((ev) =>
                    this._buildTreeNodes((ev as AssetsEvent).assets!, sortFunction));
        } else {
            this._buildTreeNodes(this.assets, sortFunction);
        }
    }

    /* Subscribe mixin overrides */

    public async _addEventSubscriptions(): Promise<void> {
        // Subscribe to asset events for all assets in the realm
        this._subscriptionIds = [await manager.getEventProvider()!.subscribeAssetEvents(null, false, (event) => this._onEvent(event))];
    }

    public onEventsConnect() {
        this._connected = true;
        this._loadAssets();
    }

    public onEventsDisconnect() {
        this._connected = false;
        this._nodes = undefined;
    }

    public _onEvent(event: SharedEvent) {

        if (event.eventType === "assets") {
            const assetsEvent = event as AssetsEvent;
            this._buildTreeNodes(assetsEvent.assets!, this._getSortFunction());
            return;
        }

        if (event.eventType === "asset" && this._nodes && this._nodes.length > 0) {

            const assetEvent = event as AssetEvent;
            if (assetEvent.cause === AssetEventCause.READ) {
                return;
            }
            if (assetEvent.cause === AssetEventCause.UPDATE
                && !(assetEvent.updatedProperties!.includes("name")
                    || assetEvent.updatedProperties!.includes("parentId"))) {
                return;
            }

            // Extract all assets, update and rebuild tree
            const assets: Asset[] = [];
            if (assetEvent.cause !== AssetEventCause.DELETE) {
                assets.push(assetEvent.asset!);
            }
            OrAssetTree._forEachNodeRecursive(this._nodes, (node) => {
                if (node.asset!.id !== assetEvent.asset!.id) {
                    assets.push(node.asset!);
                }
            });
            this._buildTreeNodes(assets, this._getSortFunction());
            if (this._addedAssetId) {
                this.selectedIds = [this._addedAssetId];
                this._addedAssetId = undefined;
            }
        }
    }

    protected _buildTreeNodes(assets: Asset[], sortFunction: (a: UiAssetTreeNode, b: UiAssetTreeNode) => number) {
        if (!assets || assets.length === 0) {
            this._nodes = [];
        } else {
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
        }

        if (this.selectedIds && this.selectedIds.length > 0) {
            this._updateSelectedNodes();
        }
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

    protected _treeNodeTemplate(treeNode: UiAssetTreeNode, level: number): TemplateResult | string {

        const descriptor = AssetModelUtil.getAssetDescriptor(treeNode.asset!.type!);

        return html`
            <li ?data-selected="${treeNode.selected}" ?data-expanded="${treeNode.expanded}" @click="${(evt: MouseEvent) => this._onNodeClicked(evt, treeNode)}">
                <div class="node-container" style="padding-left: ${level * 22}px">
                    <div class="node-name">
                        <div class="expander" ?data-expandable="${treeNode.expandable}"></div>
                        ${getAssetDescriptorIconTemplate(descriptor)}
                        <span>${treeNode.asset!.name}</span>
                    </div>
                </div>
                <ol>
                    ${!treeNode.children ? `` : treeNode.children.map((childNode) => this._treeNodeTemplate(childNode, level + 1))}
                </ol>
            </li>
        `;
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

    protected _doRequest<T>(event: CustomEvent<RequestEventDetail<T>>, handler: (detail: T) => void) {
        this.dispatchEvent(event);
        window.setTimeout(() => {
            if (event.detail.allow) {
                handler(event.detail.detail);
            }
        });
    }
}
