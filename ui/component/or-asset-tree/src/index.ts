import {html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import "@openremote/or-mwc-components/or-mwc-input";
import {InputType, OrInputChangedEvent, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import "@openremote/or-icon";
import {
    Asset,
    AssetDescriptor,
    AssetEvent,
    AssetEventCause,
    AssetModelUtil,
    AssetQuery,
    AssetQueryMatch,
    AssetQueryOrderBy$Property,
    AssetsEvent,
    AssetTreeEvent, AssetTreeNode,
    Attribute,
    AttributePredicate,
    ClientRole,
    LogicGroup,
    LogicGroupOperator,
    SharedEvent,
    StringPredicate,
    WellknownAssets
} from "@openremote/model";
import "@openremote/or-translate";
import {style} from "./style";
import manager, {EventCallback, subscribe, Util} from "@openremote/core";
import Qs from "qs";
import {getAssetDescriptorIconTemplate, OrIcon} from "@openremote/or-icon";
import "@openremote/or-mwc-components/or-mwc-menu";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import "@openremote/or-mwc-components/or-mwc-list";
import {i18next} from "@openremote/or-translate";
import "@openremote/or-mwc-components/or-mwc-dialog";
import {OrMwcDialog, showDialog, showErrorDialog, showOkCancelDialog} from "@openremote/or-mwc-components/or-mwc-dialog";
import {OrAddAssetDialog, OrAddChangedEvent} from "./or-add-asset-dialog";
import "./or-add-asset-dialog";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {when} from "lit/directives/when.js";
import {debounce} from "lodash";

export interface AssetTreeTypeConfig {
    include?: string[];
    exclude?: string[];
}

export interface AssetTreeConfig {
    select?: {
        multiSelect?: boolean;
        types?: string[];
    };
    add?: {
        typesProvider?: (parent: UiAssetTreeNode | undefined) => AssetDescriptor[] | undefined;
        typesParent?: {
            default?: AssetTreeTypeConfig;
            none?: AssetTreeTypeConfig;
            assetTypes?: { [assetType: string]: AssetTreeTypeConfig }
        };
    };
}

interface AssetWithReparentId extends Asset {
    reparentId?: string | null;
}

export interface UiAssetTreeNode extends AssetTreeNode {
    selected: boolean;
    expandable: boolean;
    expanded: boolean;
    parent: UiAssetTreeNode;
    children: UiAssetTreeNode[];
    someChildrenSelected: boolean;
    allChildrenSelected: boolean;
    notMatchingFilter: boolean;
    hidden: boolean;
}

export interface NodeSelectEventDetail {
    oldNodes: UiAssetTreeNode[];
    newNodes: UiAssetTreeNode[];
}

export interface ChangeParentEventDetail {
    parentId: string | undefined;
    assetIds: string[];
}

export {style};

export class OrAssetTreeRequestSelectionEvent extends CustomEvent<Util.RequestEventDetail<NodeSelectEventDetail>> {

    public static readonly NAME = "or-asset-tree-request-selection";

    constructor(request: NodeSelectEventDetail) {
        super(OrAssetTreeRequestSelectionEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                allow: true,
                detail: request
            }
        });
    }
}

export class OrAssetTreeSelectionEvent extends CustomEvent<NodeSelectEventDetail> {

    public static readonly NAME = "or-asset-tree-selection";

    constructor(detail: NodeSelectEventDetail) {
        super(OrAssetTreeSelectionEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: detail
        });
    }
}

export class OrAssetTreeChangeParentEvent extends CustomEvent<ChangeParentEventDetail> {

    public static readonly NAME = "or-asset-tree-change-parent";

    constructor(parent: string | undefined, assetsIds: string[]) {
        super(OrAssetTreeChangeParentEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                parentId: parent,
                assetIds: assetsIds
            }
        });
    }
}
export interface ToggleExpandEventDetail {
    node: UiAssetTreeNode;
}
export class OrAssetTreeToggleExpandEvent extends CustomEvent<ToggleExpandEventDetail> {

    public static readonly NAME = "or-asset-tree-expand";

    constructor(detail: ToggleExpandEventDetail) {
        super(OrAssetTreeToggleExpandEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: detail
        });
    }
}

enum FilterElementType {
    SEARCH_FILTER, ASSET_TYPE,ATTRIBUTE_NAME, ATTRIBUTE_VALUE
}

export type AddEventDetail = {
    sourceAsset?: Asset;
    asset: Asset;
}

export class OrAssetTreeRequestAddEvent extends CustomEvent<Util.RequestEventDetail<AddEventDetail>> {

    public static readonly NAME = "or-asset-tree-request-add";

    constructor(detail: AddEventDetail) {
        super(OrAssetTreeRequestAddEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                allow: true,
                detail: detail
            }
        });
    }
}

export class OrAssetTreeAddEvent extends CustomEvent<AddEventDetail> {

    public static readonly NAME = "or-asset-tree-add";

    constructor(detail: AddEventDetail) {
        super(OrAssetTreeAddEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: detail
        });
    }
}

export class OrAssetTreeRequestDeleteEvent extends CustomEvent<Util.RequestEventDetail<UiAssetTreeNode[]>> {

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

export class OrAssetTreeAssetEvent extends CustomEvent<AssetEvent> {

    public static readonly NAME = "or-asset-tree-asset-event";

    constructor(assetEvent: AssetEvent) {
        super(OrAssetTreeAssetEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: assetEvent
        });
    }
}

export class OrAssetTreeFilter {
    asset: string | undefined;
    assetType: string[];
    attribute: string[];
    attributeValue: string[];

    constructor(asset?: string) {
        this.asset = asset;
        this.assetType = [];
        this.attribute = [];
        this.attributeValue = [];
    }
}

declare global {
    export interface HTMLElementEventMap {
        [OrAssetTreeRequestSelectionEvent.NAME]: OrAssetTreeRequestSelectionEvent;
        [OrAssetTreeSelectionEvent.NAME]: OrAssetTreeSelectionEvent;
        [OrAssetTreeRequestAddEvent.NAME]: OrAssetTreeRequestAddEvent;
        [OrAssetTreeAddEvent.NAME]: OrAssetTreeAddEvent;
        [OrAssetTreeRequestDeleteEvent.NAME]: OrAssetTreeRequestDeleteEvent;
        [OrAssetTreeAssetEvent.NAME]: OrAssetTreeAssetEvent;
        [OrAssetTreeChangeParentEvent.NAME]: OrAssetTreeChangeParentEvent;
    }
}

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
    public readonly assets?: Asset[];

    @property({type: Array})
    public readonly rootAssets?: Asset[];

    @property({type: Array})
    public readonly rootAssetIds?: string[];

    @property({type: Object})
    public readonly dataProvider?: (offset: number, limit: number, parentId?: string) => Promise<Asset[]>;

    @property({type: Boolean})
    public readonly readonly: boolean = false;

    @property({type: Boolean})
    public disabled: boolean = false;

    @property({type: Boolean})
    public readonly disableSubscribe: boolean = false;

    @property({type: Array})
    public selectedIds?: string[];

    @property({type: Boolean})
    public showDeselectBtn?: boolean = true;

    @property({type: Boolean})
    public showSortBtn?: boolean = true;

    @property({type: Boolean})
    public showFilter: boolean = true;

    @property({type: Boolean})
    public showFilterIcon: boolean = true;

    @property({type: String})
    public sortBy?: string = "name";

    @property({type: Boolean})
    public expandAllNodes?: boolean = false;

    @property({type: Boolean})
    public checkboxes?: boolean = false;

    @property({type: Number})
    public readonly queryLimit = 100;

    protected config?: AssetTreeConfig;

    @state()
    protected _nodes?: UiAssetTreeNode[];

    protected _loading: boolean = false;
    protected _connected: boolean = false;
    protected _selectedNodes: UiAssetTreeNode[] = [];
    protected _expandedNodes: UiAssetTreeNode[] = [];
    protected _initCallback?: EventCallback;

    @state()
    protected _filter: OrAssetTreeFilter = new OrAssetTreeFilter();
    @query("#clearIconContainer")
    protected _clearIconContainer!: HTMLElement;
    @query("#filterInput")
    protected _filterInput!: OrMwcInput;
    @state()
    protected _filterSettingOpen: boolean = false;
    @state()
    protected _assetTypes: AssetDescriptor[] = [];
    @query("#attributeNameFilter")
    protected _attributeNameFilter!: OrMwcInput;
    @query("#attributeValueFilter")
    protected _attributeValueFilter!: OrMwcInput;
    @state()
    protected _assetTypeFilter!: string;
    protected _uniqueAssetTypes: string[] = [];
    @state()
    protected _hasMoreParents = false;
    @state()
    protected _incompleteParentIds: string[] = [];

    private _dragDropParentId: string | null = null;
    protected _expandTimer?: number = undefined;
    private _latestSelected: UiAssetTreeNode | undefined = undefined;

    protected assetsChildren: {
        [key: string]: UiAssetTreeNode[]
    } = {};

    public get selectedNodes(): UiAssetTreeNode[] {
        return this._selectedNodes ? [...this._selectedNodes] : [];
    }

    public set selectedNodes(nodes: UiAssetTreeNode[]) {
        this.selectedIds = nodes.map((node) => node.asset!.id!);
    }

    public connectedCallback() {
        super.connectedCallback();
    }

    public disconnectedCallback() {
        super.disconnectedCallback();
        this.requestUpdate();
    }

    public refresh() {
        // Clear nodes to re-fetch them
        this._nodes = undefined;
    }

    public isAncestorSelected(node: UiAssetTreeNode) {
        if (!this.selectedIds || !node.parent) {
            return false;
        }

        while (node.parent) {
            node = node.parent;
            if (this.selectedIds.includes(node.asset!.id!)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A setter function for updating the list of nodes (assets) shown in the UI.
     * @param filter - The {@link OrAssetTreeFilter} to apply.
     * @param reflect - If the changes should be reflected in the filtering UI. (default: false)
     */
    public applyFilter(filter?: OrAssetTreeFilter | string, reflect = false) {
        if(!filter || typeof filter === "string") {
            filter = this.parseFromInputFilter(filter);
        }
        if (Util.objectsEqual(this._filter, filter)) {
            console.debug("Tried to apply filter to the asset tree, but it was the same.", filter);
            return;
        }
        console.debug("Applying filter to the asset tree:", filter);
        this._filter = filter;
        if(reflect) {
            this.updateComplete.finally(() => this._filterInput.value = this.formatFilter(filter));
        }
    }

    protected mapDescriptors(descriptors: (AssetDescriptor)[], withNoneValue?: ListItem): ListItem[] {
        let items: ListItem[] = descriptors.map((descriptor) => {
            return {
                styleMap: {
                    "--or-icon-fill": descriptor.colour ? "#" + descriptor.colour : "unset"
                },
                icon: descriptor.icon,
                text: Util.getAssetTypeLabel(descriptor),
                value: descriptor.name!,
                data: descriptor
            }
        }).sort(Util.sortByString((listItem) => listItem.text));

        if (withNoneValue) {
            items.splice(0,0, withNoneValue);
        }

        return items;
    }



    protected getSelectHeader(): TemplateResult {
        return html `<or-mwc-input style="width:100%;" ?disabled="${this._loading}" type="${InputType.TEXT}" .label="${i18next.t("filter.assetTypeLabel")}" iconTrailing="menu-down" iconColor="rgba(0, 0, 0, 0.87)" icon="selection-ellipse" value="${i18next.t("filter.assetTypeNone")}"></or-mwc-input>`;
    }

    protected getSelectedHeader(descriptor: AssetDescriptor): TemplateResult {
        return html `<or-mwc-input style="width:100%;" ?disabled="${this._loading}" type="${InputType.TEXT}" .label="${i18next.t("filter.assetTypeLabel")}" .iconColor="${descriptor.colour}" iconTrailing="menu-down" icon="${descriptor.icon}" value="${Util.getAssetTypeLabel(descriptor)}"></or-mwc-input>`;
    }

    protected assetTypeSelect(): TemplateResult {
        if (this._assetTypeFilter) {
            const descriptor: AssetDescriptor | undefined = this._assetTypes.find((at: AssetDescriptor) => { return at.name === this._assetTypeFilter });
            if (descriptor) {
                return this.getSelectedHeader(descriptor);
            } else {
                return this.getSelectHeader();
            }
        } else {
            return this.getSelectHeader();
        }
    }


    protected atLeastOneNodeToBeShown(): boolean {
        let atLeastOne: boolean = false;
        this._nodes?.forEach((value: UiAssetTreeNode) => {
            if (!value.hidden) {
                atLeastOne = true;
            }
        });
        return atLeastOne;
    }

    protected render() {

        const canAdd = this._canAdd();

        return html`
            <div id="header">
                <div id="title-container">
                    <or-translate id="title" value="asset_plural"></or-translate>
                </div>

                <div id="header-btns">
                    <or-mwc-input ?hidden="${!this.selectedIds || this.selectedIds.length === 0 || !this.showDeselectBtn}" type="${InputType.BUTTON}" icon="close" title="${i18next.t("deselect")}" @or-mwc-input-changed="${() => this._onDeselectClicked()}"></or-mwc-input>
                    <or-mwc-input ?hidden="${this._isReadonly() || !this.selectedIds || this.selectedIds.length !== 1 || !canAdd}" type="${InputType.BUTTON}" icon="content-copy" title="${i18next.t("duplicate")}" @or-mwc-input-changed="${() => this._onCopyClicked()}"></or-mwc-input>
                    <or-mwc-input ?hidden="${this._isReadonly() || !this.selectedIds || this.selectedIds.length === 0 || this._gatewayDescendantIsSelected()}" type="${InputType.BUTTON}" icon="delete" title="${i18next.t("delete")}" @or-mwc-input-changed="${() => this._onDeleteClicked()}"></or-mwc-input>
                    <or-mwc-input ?hidden="${this._isReadonly() || !canAdd}" type="${InputType.BUTTON}" icon="plus" title="${i18next.t("addAsset")}" @or-mwc-input-changed="${() => this._onAddClicked()}"></or-mwc-input>
                    
                    ${getContentWithMenuTemplate(
                            html`<or-mwc-input type="${InputType.BUTTON}" ?hidden="${!this.showSortBtn}" icon="sort-variant" title="${i18next.t("sort")}" ></or-mwc-input>`,
                            ["name", "type", "createdOn"].map((sort) => { return {value: sort, text: i18next.t(sort)} as ListItem; }),
                            this.sortBy,
                            (v) => this._onSortClicked(v as string))}
                </div>
            </div>
            
            ${when(this.showFilter, () => html`
                <div id="asset-tree-filter">
                    <or-mwc-input id="filterInput"
                                  ?disabled="${this._loading}"
                                  style="width: 100%;"
                                  type="${InputType.TEXT}"
                                  placeholder="${i18next.t("filter.filter")}..."
                                  compact="true"
                                  outlined="true"
                                  @input="${debounce(() => {
                                      // Means some input is occurring so delay filter
                                      this._onFilterInput(this._filterInput.nativeValue);
                                  }, 200)}">
                    </or-mwc-input>
                    ${when(this.showFilterIcon, () => html`
                        <or-icon id="filterSettingsIcon" icon="${this._filterSettingOpen ? "window-close" : "tune"}" title="${i18next.t(this._filterSettingOpen ? "filter.close" : "filter.open")}" @click="${() => {
                            if (this._filterSettingOpen) {
                                this._filterSettingOpen = false;
                            } else {
                                this._filterSettingOpen = true;
                                // Avoid to build again the types
                                if (this._assetTypes.length === 0) {
                                    const types = this._getAllowedChildTypes(this._selectedNodes[0]);
                                    this._assetTypes = types.filter((t) => t.descriptorType === "asset");
                                }

                                if (this._filter.attribute.length > 0) {
                                    this._attributeNameFilter.value = this._filter.attribute[0];
                                }

                                if (this._filter.attributeValue.length > 0 && this._filter.attribute.length > 0) {
                                    this._attributeValueFilter.disabled = false;
                                    this._attributeValueFilter.value = this._filter.attributeValue[0];
                                }

                                if (this._filter.assetType.length > 0) {
                                    this._assetTypeFilter = this._filter.assetType[0];
                                } else {
                                    this._assetTypeFilter = '';
                                }
                            }
                        }}"></or-icon>
                    `)}
                </div>
                <div id="asset-tree-filter-setting" class="${this._filterSettingOpen ? "visible" : ""}">
                    <div class="advanced-filter">
                        ${this._assetTypes.length > 0 ? getContentWithMenuTemplate(
                                this.assetTypeSelect(),
                                this.mapDescriptors(this._assetTypes, {text: i18next.t("filter.assetTypeMenuNone"), value: "", icon: "selection-ellipse"}),
                                undefined,
                                (v: string[] | string) => {
                                    this._assetTypeFilter = (v as string);
                                },
                                undefined,
                                false,
                                true,
                                true) : html``
                        }
                        <or-mwc-input id="attributeNameFilter" .label="${i18next.t("filter.attributeLabel")}"

                                      .type="${InputType.TEXT}"
                                      style="margin-top: 10px;"
                                      ?disabled="${this._loading}"
                                      @input="${(e: KeyboardEvent) => {
                                          this._shouldEnableAttrTypeEvent(e);
                                      }}"
                                      @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                          this._shouldEnableAttrType((e.detail.value as string) || undefined);
                                      }}"></or-mwc-input>
                        <or-mwc-input id="attributeValueFilter" .label="${i18next.t("filter.attributeValueLabel")}"

                                      .type="${InputType.TEXT}"
                                      style="margin-top: 10px;"
                                      disabled></or-mwc-input>
                        <div style="margin-top: 10px;">
                            <or-mwc-input style="float:left;" type="${InputType.BUTTON}" label="filter.clear" @or-mwc-input-changed="${() => {
                                // Wipe the current value and hide the clear button
                                this._filterInput.value = undefined;

                                this._attributeValueFilter.value = undefined;
                                this._attributeNameFilter.value = undefined;

                                this._attributeValueFilter.disabled = true;

                                this._assetTypeFilter = '';

                                this.applyFilter(new OrAssetTreeFilter());

                                // Call filtering
                                this._doFiltering();
                            }}"></or-mwc-input>
                            <or-mwc-input style="float: right;" type="${InputType.BUTTON}" label="filter.action" raised @or-mwc-input-changed="${() => {
                                this._filterFromSettings();
                            }}"></or-mwc-input>
                        </div>
                    </div>
                </div>
            `)}

            ${when(!this._nodes, () => html`
                <span id="loading"><or-translate value="loading"></or-translate></span>
            `, () => html`
                ${when(this._nodes!.length === 0 || !this.atLeastOneNodeToBeShown(),
                    () => html`<span id="noAssetsFound"><or-translate value="noAssetsFound"></or-translate></span>`,
                    () => html`
                        <div id="list-container">
                            <ol id="list">
                                ${this._nodes?.filter(n => n && !n.hidden).map(node => this._treeNodeTemplate(node, 0))}
                                ${when(this._hasMoreParents, () => html`
                                    <li class="asset-list-element">
                                        <div class="end-element loadmore-element" node-asset-id="${''}" @dragleave=${(ev: DragEvent) => { this._onDragLeave(ev) }}
                                             @dragenter="${(ev: DragEvent) => this._onDragEnter(ev)}" @dragend="${(ev: DragEvent) => this._onDragEnd(ev)}"
                                             @dragover="${(ev: DragEvent) => this._onDragOver(ev)}">
                                            <or-mwc-input type=${InputType.BUTTON} label="loadMore" outlined compact @or-mwc-input-changed=${() => {
                                                const cache: Asset[] = [];
                                                OrAssetTree._forEachNodeRecursive(this._nodes ?? [], n => n.asset && cache.push(n.asset));
                                                this._loadAssets(undefined, this._nodes?.length ?? 0, cache);
                                            }}></or-mwc-input>
                                        </div>
                                    </li>
                                `)}
                            </ol>
                        </div>
                    `
                )}
            `)}

            <div id="footer">
            
            </div>
        `;
    }

    protected _isReadonly() {
        return this.readonly || !manager.authenticated || !manager.hasRole(ClientRole.WRITE_ASSETS);
    }

    protected shouldUpdate(_changedProperties: PropertyValues): boolean {
        const result = super.shouldUpdate(_changedProperties);
        if (_changedProperties.has("assets")
            || _changedProperties.has("rootAssets")
            || _changedProperties.has("rootAssetIds")
            || _changedProperties.has("queryLimit")
        ) {
            this.refresh();
        }

        if (!this._nodes) {
            this._loadAssets().catch(ex => console.log(ex.message));
            return true;
        }

        if (_changedProperties.has("selectedIds") && this.selectedIds !== undefined) {
            const previous: string[] | undefined = _changedProperties.get("selectedIds");
            if (!Util.objectsEqual(previous, this.selectedIds)) {
                this._updateSelectedNodes();

                // When selecting a different node than present in the Asset ID filter, it should be removed
                if (previous?.length === 1 && this._filter.asset === previous[0]) {
                    this.applyFilter(new OrAssetTreeFilter(), true);
                    if (!this.selectedIds?.length) {
                        this._doFiltering(); // Only update UI if there is no selection, to prevent flickering
                    }
                }
            }
        }

        if (_changedProperties.has("sortBy")) {
            this._updateSort(this._nodes!, this._getSortFunction());
        }

        if (_changedProperties.has("disabledSubscribe")) {
            if (this.disableSubscribe) {
                this._removeEventSubscriptions();
            }
        }

        return result;
    }

    protected _updateSelectedNodes() {
        let actuallySelectedIds: string[] | undefined;
        let selectedNodes: UiAssetTreeNode[] | undefined;
        OrAssetTree._forEachNodeRecursive(this._nodes!, (node) => {
            if (this.selectedIds && this.selectedIds.indexOf(node.asset!.id!) >= 0) {
                actuallySelectedIds ??= [];
                actuallySelectedIds.push(node.asset!.id!);
                selectedNodes ??= [];
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

            if (this.checkboxes) {
                let parent = node.parent;
                while (parent) {
                    const allChildren: UiAssetTreeNode[] = [];
                    OrAssetTree._forEachNodeRecursive(parent.children, (child) => {
                        allChildren.push(child);
                    });
                    parent.someChildrenSelected = false;
                    parent.allChildrenSelected = false;

                    if (allChildren.every(c => actuallySelectedIds?.includes(c.asset!.id!))) {
                        parent.allChildrenSelected = true;
                    } else if (allChildren.some(c => actuallySelectedIds?.includes(c.asset!.id!))) {
                        parent.someChildrenSelected = true;
                    }

                    parent = parent.parent;
                }
            }
        });

        if(actuallySelectedIds?.length) {
            this.selectedIds = actuallySelectedIds;
        }
        if(selectedNodes !== undefined || this.selectedNodes !== undefined) {
            const oldSelection = this._selectedNodes;
            this._selectedNodes = selectedNodes ?? [];
            this.dispatchEvent(new OrAssetTreeSelectionEvent({
                oldNodes: oldSelection,
                newNodes: selectedNodes ?? []
            }));
        }
    }

    protected _updateSort(nodes: UiAssetTreeNode[], sortFunction: (a: UiAssetTreeNode, b: UiAssetTreeNode) => number) {
        if (!nodes) {
            return;
        }

        nodes.sort(sortFunction);
        nodes.forEach((node) => this._updateSort(node.children, sortFunction));
    }

    protected async _toggleExpander(expander: HTMLElement, node: UiAssetTreeNode | null, silent: boolean = false) {
        if (node && node.expandable) {
            node.expanded = !node.expanded;

            if (node.expanded) {
                this._expandedNodes.push(node);

                // Load children (from cache or using WebSocket) of the now 'expanded' node
                const hasCachedChildren = node.children?.length;
                if(hasCachedChildren) {
                    console.debug(`Reusing cache for loading children of asset ${node.asset?.id}...`);
                } else {
                    const cache: Asset[] = [];
                    OrAssetTree._forEachNodeRecursive(this._nodes ?? [], n => n.asset && cache.push(n.asset));
                    await this._loadAssets(node.asset?.id, 0, cache).catch(ex => console.log(ex.message));
                }

            } else {
                this._expandedNodes = this._expandedNodes.filter(n => n !== node);
            }

            // Update HTML attributes of the now 'expanded' node
            const elem = expander.parentElement!.parentElement!.parentElement!;
            elem.toggleAttribute("data-expanded", node.expanded);
            if (!silent) {
                this.dispatchEvent(new OrAssetTreeToggleExpandEvent({node: node}));
            }
            this.requestUpdate();
        }
    }

    private _buildPaths(node: UiAssetTreeNode): string[] {
        let paths: string[] = [];

        if (node.asset) {
            if (node.asset.id) {
                paths.push(node.asset.id);

                if (node.children.length > 0 && node.expanded) {
                    node.children.forEach((child: UiAssetTreeNode) => {
                        paths = paths.concat(this._buildPaths(child));
                    });
                }

                return paths;
            }

            return [];
        }

        return [];
    }

    private _findNode(n: UiAssetTreeNode, assetId: string): UiAssetTreeNode | undefined {
        if (n.asset && n.asset.id) {
            if (n.asset.id === assetId) {
                return n;
            } else if (n.children.length > 0 && n.expanded) {
                let foundNode: UiAssetTreeNode | undefined = undefined;
                n.children.forEach((n: UiAssetTreeNode) => {
                    if (!foundNode) {
                        foundNode = this._findNode(n, assetId);
                    }
                });
                return foundNode;
            }

            return undefined;
        }
    }

    private _findNodeFromAssetId(assetId: string) : UiAssetTreeNode | undefined {
        if (this._nodes) {
            let foundNode: UiAssetTreeNode | undefined = undefined;

            this._nodes.forEach((n: UiAssetTreeNode) => {
                if (!foundNode) {
                    foundNode = this._findNode(n, assetId);
                }
            });

            return foundNode;
        } else {
            return undefined;
        }

    }

    protected _onNodeClicked(evt: MouseEvent | null, node: UiAssetTreeNode | null) {
        if (evt && evt.defaultPrevented) {
            return;
        }

        if (evt) {
            evt.preventDefault();
        }

        const isExpander = evt && (evt.target as HTMLElement).className.indexOf("expander") >= 0;
        const isParentCheckbox = evt && (evt.target as OrIcon)?.icon?.includes("checkbox-multiple");
        const isLoadMoreButton = evt && (evt.target as OrMwcInput)?.parentElement?.classList.contains("loadmore-element");

        if (isExpander) {
            this._toggleExpander((evt.target as HTMLElement), node);

        } else if (isLoadMoreButton) {
            if(node) {
                const cache: Asset[] = [];
                OrAssetTree._forEachNodeRecursive(this._nodes ?? [], n => n.asset && cache.push(n.asset));
                this._loadAssets(node.asset?.id, node.children?.length ?? 0, cache);
            }
        } else {
            let canSelect = true;

            if (node && this.config && this.config.select?.types) {
                canSelect = this.config.select.types.indexOf(node.asset!.type!) >= 0;
            }

            // If node cannot be selected, and it is not the 'select all of this parent'-checkbox, cancel it.
            if (!canSelect && !isParentCheckbox) {
                return;
            }

            let selectedNodes: UiAssetTreeNode[] = [];

            if (node) {
                const index = this.selectedNodes.indexOf(node);
                let select = true;
                let deselectOthers = true;
                const multiSelect = !this._isReadonly() && (!this.config || !this.config.select || !this.config.select.multiSelect);

                // determine if node was already selected
                if (this.checkboxes || (multiSelect && evt && (evt.ctrlKey || evt.shiftKey || evt.metaKey))) {
                    deselectOthers = false;
                    if (index >= 0 && this.selectedIds && this.selectedIds.length > 1) {
                        select = false;
                    }
                }

                // handle selected state
                if (isParentCheckbox) {
                    selectedNodes = [...this.selectedNodes];

                    const childNodes: UiAssetTreeNode[] = [];
                    OrAssetTree._forEachNodeRecursive(node.children, (childNode) => {
                        let canSelectChild = true;
                        // If not of required asset type, cancel selection of the child
                        if(childNode && this.config?.select?.types) {
                            canSelectChild = this.config.select.types.indexOf(childNode.asset!.type!) >= 0;
                        }
                        if(canSelectChild) {
                            childNodes.push(childNode);
                        }
                    });

                    // based on multiple-box already selected, remove or add to array of selected nodes
                    selectedNodes = (!node.allChildrenSelected)
                        ? selectedNodes.concat(childNodes)
                        : selectedNodes.filter(n => !childNodes.map(cn => cn.asset!.id).includes(n.asset!.id));

                } else if (deselectOthers) {
                    this._latestSelected = Object.assign({}, node);
                    selectedNodes = [node];
                } else if (select) {
                    if (index < 0) {
                        if (evt && evt.shiftKey) {
                            let hierarchy: string[] = [];
                            this._nodes?.forEach((n: UiAssetTreeNode) => {
                                hierarchy = hierarchy.concat(this._buildPaths(n));
                            });

                            if (this._latestSelected && this._latestSelected.asset && this._latestSelected.asset.id && node.asset && node.asset.id) {
                                let latestSelectedAssetId: string = this._latestSelected.asset.id;
                                let newlySelectedAssetId: string = node.asset.id;

                                let previousIndex: number = hierarchy.findIndex((val: string) => { return val.includes(latestSelectedAssetId); });
                                let newIndex: number = hierarchy.findIndex((val: string) => { return val.includes(newlySelectedAssetId); });

                                let startIndex: number = -1;
                                let endIndex: number = -1;

                                if (previousIndex > newIndex) {
                                    startIndex = newIndex;
                                    endIndex = previousIndex;
                                } else {
                                    startIndex = previousIndex;
                                    endIndex = newIndex;
                                }

                                let assetIdsToSelect: string[] = hierarchy.slice(startIndex, endIndex + 1 );

                                let foundNodes: UiAssetTreeNode[] = [];

                                assetIdsToSelect.forEach((assetIdToSelect: string) => {
                                    let foundNode: UiAssetTreeNode | undefined = this._findNodeFromAssetId(assetIdToSelect);

                                    if (foundNode) {
                                        foundNodes.push(foundNode);
                                    }
                                });

                                selectedNodes = [...this.selectedNodes];
                                selectedNodes = selectedNodes.concat(foundNodes);
                            }
                        } else {
                            selectedNodes = [...this.selectedNodes];
                            selectedNodes.push(node);
                        }

                        this._latestSelected = Object.assign({}, node);
                    }
                } else if (index >= 0) {
                    selectedNodes = [...this.selectedNodes];
                    if (selectedNodes.length === 1) {
                        this._latestSelected = undefined;
                    }
                    selectedNodes.splice(index, 1);
                }
            }

            Util.dispatchCancellableEvent(this, new OrAssetTreeRequestSelectionEvent({
                oldNodes: this.selectedNodes,
                newNodes: selectedNodes
            })).then((detail) => {
                if (detail.allow) {
                    this.selectedNodes = detail.detail.newNodes
                }
            });
        }
    }

    protected _onDeselectClicked() {
        this._onNodeClicked(null, null);
    }

    protected parseFromInputFilter(inputValue = this._filterInput?.value): OrAssetTreeFilter {
        let resultingFilter: OrAssetTreeFilter = new OrAssetTreeFilter();

        if (inputValue) {
            let asset: string = inputValue;
            let matchingResult: RegExpMatchArray | null = inputValue.match(/(attribute\:)(\"[^"]+\")\S*/g);
            if (matchingResult) {
                if (matchingResult.length > 0) {
                    matchingResult.forEach((value: string, index: number) => {
                        asset = asset.replace(value, '');

                        const startIndex: number = value.toString().indexOf('attribute:');

                        const matchingVal: string = value.toString().substring(startIndex + 'attribute:'.length + 1, value.toString().length-1);

                        resultingFilter.attribute.push(matchingVal);
                        resultingFilter.attributeValue.push('');
                    });
                }

                this._attributeValueFilter.disabled = false;
            }

            matchingResult = inputValue.match(/(type\:)\S+/g);
            if (matchingResult) {
                if (matchingResult.length > 0) {
                    matchingResult.forEach((value: string, index: number) => {
                        asset = asset.replace(value, '');

                        const startIndex: number = value.toString().indexOf('type:');

                        const matchingVal: string = value.toString().substring(startIndex + 'type:'.length);

                        resultingFilter.assetType.push(matchingVal);
                    });
                }
            }

            matchingResult = inputValue.match(/(\"[^\"]+\")\:(([^\"\s]+)|(\"[^\"]+\"))/g);
            if (matchingResult) {
                if (matchingResult.length > 0) {
                    matchingResult.forEach((value: string, index: number) => {
                        asset = asset.replace(value, '');

                        const startIndex: number = value.toString().indexOf('":');
                        // Adding 2 to remove the ": matched before
                        let matchingVal: string = value.toString().substring(startIndex + 2);
                        // Starting from position 1 to remove first "
                        const matchingName: string = value.toString().substring(1, startIndex);

                        if (matchingVal[0] === '"' && matchingVal[matchingVal.length-1] === '"') {
                            matchingVal = matchingVal.substring(1,matchingVal.length-1);
                        }

                        resultingFilter.attribute.push(matchingName);
                        resultingFilter.attributeValue.push(matchingVal);
                    });
                }
            }
            resultingFilter.asset = (asset && asset.length > 0) ? asset.trim() : undefined;
        }

        return resultingFilter;
    }

    protected formatFilter(newFilter: OrAssetTreeFilter): string {
        let searchInput: string = newFilter.asset ? newFilter.asset : '';

        let prefix: string = newFilter.asset ? ' ' : '';

        let handledAttributeForValues: string[] = [];

        if (newFilter.assetType.length > 0) {
            newFilter.assetType.forEach((assetType: string) => {
                searchInput += prefix + 'type:' + assetType;
                prefix = ' ';
            });
        }

        if (newFilter.attribute.length > 0 && newFilter.attributeValue.length > 0) {
            newFilter.attributeValue.forEach((attributeValue: string, index: number) => {
                handledAttributeForValues.push(newFilter.attribute[index]);
                searchInput += prefix + '"' + newFilter.attribute[index] + '":' + attributeValue;
                prefix = ' ';
            });
        }

        if (newFilter.attribute.length > 0 && newFilter.attributeValue.length === 0) {
            newFilter.attribute.forEach((attributeName: string) => {
                if (!handledAttributeForValues.includes(attributeName)) {
                    searchInput += prefix + 'attribute:"' + attributeName + '"';
                    prefix = ' ';
                }
            });
        }

        return searchInput;
    }

    protected _shouldEnableAttrTypeEvent(e: KeyboardEvent): void {
        let value: string | undefined;

        if (e.composedPath()) {
            value = ((e.composedPath()[0] as HTMLInputElement).value) || undefined;
        }

        this._shouldEnableAttrType(value);
    }

    protected _shouldEnableAttrType(value: string | undefined): void {
        if (value) {
            this._attributeValueFilter.disabled = false;
        } else {
            this._attributeValueFilter.disabled = true;
        }
    }

    protected applySettingFields(filter: OrAssetTreeFilter): OrAssetTreeFilter {
        if ( this._assetTypeFilter ) {
            filter.assetType = [ this._assetTypeFilter ];
        } else {
            filter.assetType = [];
        }

        if ( this._attributeNameFilter.value ) {
            filter.attribute = [ this._attributeNameFilter.value ];
        } else {
            filter.attribute = [];
        }

        if ( this._attributeNameFilter.value && this._attributeValueFilter.value ) {
            let attributeValueValue: string = this._attributeValueFilter.value;
            if (attributeValueValue.includes(' ')) {
                attributeValueValue = '"' + attributeValueValue + '"';
            }
            filter.attributeValue = [ attributeValueValue ];
        } else {
            filter.attributeValue = [];
        }

        return filter;
    }

    protected _filterFromSettings(): void {
        let filterFromSearchInput: OrAssetTreeFilter = this.parseFromInputFilter();

        let filterFromSearchInputWithSettings: OrAssetTreeFilter = this.applySettingFields(filterFromSearchInput);

        this.applyFilter(filterFromSearchInputWithSettings);

        let newFilterForSearchInput: string = this.formatFilter(this._filter);

        this._filterInput.value = newFilterForSearchInput;

        this._filterSettingOpen = false;

        this._doFiltering();
    }

    protected _onFilterInput(newValue: string | undefined): void {
        this.applyFilter(newValue);
        this._doFiltering();
    }

    protected async _doFiltering() {

        if (this.isConnected && this._nodes) {

            // Clear filter if everything is not set anymore
            if (!this._filter.asset?.length && !this._filter.attribute?.length && !this._filter.assetType?.length && !this._filter.attributeValue?.length) {
                console.debug("Clearing asset tree filter...");
                OrAssetTree._forEachNodeRecursive(this._nodes!, (node) => {
                    node.notMatchingFilter = false;
                    node.hidden = false;
                });
                this.refresh(); // Clear cache, and refetch the assets
                return;
            }

            console.debug("Filtering asset tree using filter:", this._filter);
            this.disabled = true;

            // Use a matcher function - this can be altered independent of the filtering logic
            // Maybe we should just filter in memory for basic matches like name
            if (this._filter.asset || this._filter.assetType || this._filter.attribute) {
                let queryRequired: boolean = false;

                if (this._filter.attribute) {
                    queryRequired = true;
                }

                this.getMatcher(queryRequired).then(({ matcher, assets }) => {
                    assets ??= [];
                    if (this._nodes) {

                        // Add nodes to the tree if not done yet
                        const cache: Asset[] = [];
                        OrAssetTree._forEachNodeRecursive(this._nodes ?? [], n => n.asset && cache.push(n.asset));
                        const assetsWithoutDuplicates = new Map([...cache, ...assets].map(item => [item.id, item])).values();
                        this._buildTreeNodes([...assetsWithoutDuplicates]);

                        // Filter out nodes that should not be visible
                        const visibleNodes = new Map<string, boolean>();
                        OrAssetTree._forEachNodeRecursive(this._nodes ?? [], n => {
                            const visible = this.filterTreeNode(n, matcher);
                            if(visible && !n.notMatchingFilter && n.asset?.id) {
                                visibleNodes.set(n.asset.id, visible);
                            }
                        });
                        // If only 1 asset is shown, automatically select it
                        if(visibleNodes.size === 1) {
                            console.debug("Only 1 asset is shown, automatically selecting it...");
                            this.selectedIds = Array.from(visibleNodes.keys());
                        }
                        this.disabled = false;
                    }
                });
            }
        }
    }

    protected async getMatcher(requireQuery: boolean): Promise<{ matcher: ((asset: Asset) => boolean), assets?: Asset[]}> {
        if (requireQuery) {
            return this.getMatcherFromQuery();
        } else {
            return { matcher: await this.getSimpleNameMatcher() };
        }
    }

    protected async getSimpleNameMatcher(): Promise<((asset: Asset) => boolean)> {
        return (asset) => {
            let match: boolean = true;
            if (this._filter.asset) {
                match = match && asset.name!.toLowerCase().includes(this._filter.asset.toLowerCase());
            }

            if (this._filter.assetType.length > 0) {
                match = match && (asset.type!.toLowerCase() === this._filter.assetType[0].toLowerCase());
            }
            return match;
        };
    }

    protected async getMatcherFromQuery(): Promise<{ assets: Asset[], matcher: ((asset: Asset) => boolean)}> {
        let assetCond: StringPredicate[] | undefined;
        let attributeCond: LogicGroup<AttributePredicate> | undefined;
        let assetTypeCond: string[] | undefined;
        const assetQueries: AssetQuery[] = [];

        if (this._filter.asset) {
            assetCond = [{
                predicateType: "string",
                match: AssetQueryMatch.CONTAINS,
                value: this._filter.asset,
                caseSensitive: false
            }];
        }

        if (this._filter.assetType.length > 0) {
            assetTypeCond = this._filter.assetType;
        }

        if (this._filter.attribute.length > 0) {
            attributeCond = {
                operator: LogicGroupOperator.AND,
                items:
                    this._filter.attribute.map((attributeName: string) => {
                        return {
                            name: {
                                predicateType: "string",
                                match: AssetQueryMatch.EXACT,
                                value: Util.sentenceCaseToCamelCase(attributeName),
                                caseSensitive: false
                            }
                        };
                    })
            };
        }

        assetQueries.push({
            realm: {
                name: manager.displayRealm
            },
            select: {
                attributes: attributeCond ? undefined : []
            },
            orderBy: {
                property: this._getOrderBy(this.sortBy)
            },
            names: assetCond,
            types: assetTypeCond,
            attributes: attributeCond,
            limit: Math.max(this.queryLimit, 1)
        });

        // If the "Asset string input" is 22 characters long, we also query for the asset id
        if(this._filter.asset && this._filter.asset.length === 22) {
            assetQueries.push({
                realm: {
                    name: manager.displayRealm
                },
                select: {
                    attributes: attributeCond ? undefined : []
                },
                types: assetTypeCond,
                attributes: attributeCond,
                ids: [this._filter.asset],
                limit: 1
            });
        }

        let foundAssets: Asset[] = [];
        let foundAssetIds: string[];

        try {
            console.debug(`Querying assets using filter '${this._filterInput.nativeValue}'...`);
            const promises = assetQueries.map(q => manager.rest.api.AssetResource.queryAssetTree(q));
            const responses = await Promise.all(promises);
            foundAssets = responses.flatMap(r => r.data.assets ?? []);
            foundAssetIds = foundAssets.map(a => a.id!);
            console.debug(`The filter query found ${foundAssets.length} assets!`);

        } catch (e) {
            console.error("Error querying Asset Tree assets with filter:", e);
            this._filter.assetType.forEach((assetT: string) => {
                if (this._assetTypes.findIndex((assetD: AssetDescriptor) => assetD.name === assetT) === -1) {
                    showSnackbar(undefined, "filter.assetTypeDoesNotExist", "dismiss");
                }
            });
            foundAssetIds = [];
        }

        // Query parents of the found assets if not cached yet.
        const parentIds = new Set(foundAssets.filter(a => a.path && a.path.length > 1).flatMap(a => a.path!.slice(0, -1)));
        const unknownParentIds = Array.from(parentIds).filter(id => id && !this.assets?.some(a => a.id === id));
        if (unknownParentIds.length > 0) {
            try {
                console.debug(`Querying parents of ${unknownParentIds.length} assets...`);
                const parentAssets = await manager.rest.api.AssetResource.queryAssets({
                    select: { attributes: attributeCond ? undefined : [] },
                    ids: unknownParentIds
                });
                console.debug(`The filter query found ${parentAssets.data.length} parents!`);
                foundAssetIds = foundAssets.map(a => a.id!);
                foundAssets = [...foundAssets, ...parentAssets.data];
            } catch (e) {
                console.error("Error querying Asset Tree parents of found assets:", e);
            }
        }

        return { assets: foundAssets, matcher: (asset) => {
            let attrValueCheck = true;

            if (this._filter.attribute.length > 0 && this._filter.attributeValue.length > 0 && foundAssetIds.includes(asset.id!)) {
                let attributeVal: [string, string][] = [];

                this._filter.attributeValue.forEach((attrVal: string, index: number) => {
                    if (attrVal.length > 0) {
                        attributeVal.push([this._filter.attribute[index], attrVal]);
                    }
                });

                const matchingAsset: Asset | undefined = foundAssets.find((a: Asset) => a.id === asset.id );

                if (matchingAsset && matchingAsset.attributes) {
                    for (let attributeValIndex = 0; attributeValIndex < attributeVal.length; attributeValIndex++) {
                        let currentAttributeVal = attributeVal[attributeValIndex];

                        let atLeastOneAttributeMatchValue: boolean = false;
                        Object.keys(matchingAsset.attributes).forEach((key: string) => {
                            let attr: Attribute<any> = matchingAsset!.attributes![key];

                            // attr.value check to avoid to compare with empty/non existing value
                            if (attr.name!.toLowerCase() === currentAttributeVal[0].toLowerCase()) {
                                switch (attr.type!) {
                                    case "number":
                                    case "integer":
                                    case "long":
                                    case "bigInteger":
                                    case "bigNumber":
                                    case "positiveInteger":
                                    case "negativeInteger":
                                    case "positiveNumber":
                                    case "negativeNumber": {
                                        let normalizedValue: string = currentAttributeVal[1]?.replace(",", ".");
                                        if (!isNaN(Number(normalizedValue))) {
                                            if ((attr.value ?? 0) === Number(normalizedValue)) {
                                                atLeastOneAttributeMatchValue = true;
                                            }
                                        } else if (/\d/.test(normalizedValue)) {
                                            if (normalizedValue.endsWith("%")) {
                                                normalizedValue = normalizedValue?.replace("%", "");
                                            }
                                            // If filter starts with a number, append '==' in front of it.
                                            if (/^[0-9]/.test(normalizedValue)) {
                                                normalizedValue = "==" + normalizedValue;
                                            }
                                            const func = attr.value + normalizedValue.replace(/[a-z]/gi, "");

                                            // Execute the function
                                            try {
                                                const resultNumberEval: boolean = eval(func);
                                                if (resultNumberEval) {
                                                    atLeastOneAttributeMatchValue = true;
                                                }
                                            } catch (_ignored) {
                                                console.warn("Could not process filter on attribute number value;", func);
                                            }
                                        }
                                        break;
                                    }
                                    case "boolean": {
                                        let value: string = currentAttributeVal[1];
                                        if ((value === "false" || value === "true") && value === (attr.value ?? false).toString()) {
                                            atLeastOneAttributeMatchValue = true;
                                        }
                                        break;
                                    }
                                    case "text": {
                                        if (attr.value) {
                                            let unparsedValue: string = currentAttributeVal[1];
                                            const multicharString: string = '*';

                                            let parsedValue: string = unparsedValue.replace(multicharString, '.*');
                                            parsedValue = parsedValue.replace(/"/g, '');

                                            let valueFromAttribute: string = attr.value as string;

                                            if (valueFromAttribute.toLowerCase().indexOf(parsedValue.toLowerCase()) != -1) {
                                                atLeastOneAttributeMatchValue = true;
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        });

                        attrValueCheck = atLeastOneAttributeMatchValue;
                    }
                }
            }

            return foundAssetIds.includes(asset.id!) && attrValueCheck;
        }};
    }

    protected isAnyFilter(): boolean {
        return this._filter.asset !== undefined || this._filter.assetType.length > 0 || this._filter.attribute.length > 0;
    }

    protected filterTreeNode(currentNode: UiAssetTreeNode, matcher: (asset: Asset) => boolean, parentMatching: boolean = false): boolean {
        let nodeOrDescendantMatches = matcher(currentNode.asset!);
        currentNode.notMatchingFilter = !nodeOrDescendantMatches;

        const childOrDescendantMatches = currentNode.children.map((childNode) => {
            return this.filterTreeNode(childNode, matcher, nodeOrDescendantMatches);
        });

        let childMatches: boolean = childOrDescendantMatches.some(m => m);
        nodeOrDescendantMatches = nodeOrDescendantMatches || childMatches;
        currentNode.expanded = childMatches && currentNode.children.length > 0 && this.isAnyFilter();
        currentNode.hidden = !nodeOrDescendantMatches && !parentMatching;
        return nodeOrDescendantMatches;
    }

    protected async _onCopyClicked() {
        if (this._selectedNodes.length !== 1) {
            return;
        }

        try {
            // Need to fully load the source asset
            const response = await manager.rest.api.AssetResource.get(this._selectedNodes[0].asset!.id!);
            if (!response.data) {
                throw new Error("API returned an invalid response when retrieving the source asset");
            }
            const asset = JSON.parse(JSON.stringify(response.data)) as Asset;
            asset.name += " copy";
            delete asset.id;
            delete asset.path;
            delete asset.createdOn;
            delete asset.version;

            Util.dispatchCancellableEvent(this, new OrAssetTreeRequestAddEvent(
                {
                    sourceAsset: this._selectedNodes[0].asset!,
                    asset: asset
                })).then((detail) => {
                    if (detail.allow) {
                        this.dispatchEvent(new OrAssetTreeAddEvent(detail.detail));
                    }
            });
        } catch (e) {
            console.error("Failed to copy asset", e);
            showErrorDialog("Failed to copy asset");
        }
    }

    protected _onAddClicked() {

        const types = this._getAllowedChildTypes(this._selectedNodes[0]);
        const agentTypes = types.filter((t) => t.descriptorType === "agent");
        const assetTypes = types.filter((t) => t.descriptorType === "asset");
        const parent = this._selectedNodes && this._selectedNodes.length === 1 ? this._selectedNodes[0].asset : undefined;
        let dialog: OrMwcDialog;

        const onAddChanged = (ev: OrAddChangedEvent) => {
            const nameValid = !!ev.detail.name && ev.detail.name.trim().length > 0 && ev.detail.name.trim().length < 1024;
            const addBtn = dialog.shadowRoot!.getElementById("add-btn") as OrMwcInput;
            addBtn.disabled = !ev.detail.descriptor || !nameValid;
        };

        dialog = showDialog(new OrMwcDialog()
            .setHeading(i18next.t("addAsset"))
            .setContent(html`
                    <or-add-asset-dialog id="add-panel" .config="${this.config}" .agentTypes="${agentTypes}" .assetTypes="${assetTypes}" .parent="${parent}" @or-add-asset-changed="${onAddChanged}"></or-add-asset-dialog>
                `)
            .setActions([
                    {
                        actionName: "cancel",
                        content: "cancel"
                    },
                    {
                        actionName: "add",
                        content: html`<or-mwc-input id="add-btn" class="button" .type="${InputType.BUTTON}" label="add" disabled></or-mwc-input>`,
                        action: () => {

                            const addAssetDialog = dialog.shadowRoot!.getElementById("add-panel") as OrAddAssetDialog;
                            const descriptor = addAssetDialog.selectedType;
                            const selectedOptionalAttributes = addAssetDialog.selectedAttributes;
                            const name = addAssetDialog.name.trim();
                            const parent = addAssetDialog.parent;

                            if (!descriptor) {
                                return;
                            }

                            const asset: Asset = {
                                name: name,
                                type: descriptor.name,
                                realm: manager.displayRealm
                            };

                            // Construct attributes
                            const assetTypeInfo = AssetModelUtil.getAssetTypeInfo(descriptor.name!);

                            if (!assetTypeInfo) {
                                return;
                            }

                            if (assetTypeInfo.attributeDescriptors) {
                                asset.attributes = {};
                                assetTypeInfo.attributeDescriptors
                                    .filter((attributeDescriptor) => !attributeDescriptor.optional)
                                    .forEach((attributeDescriptor) => {
                                        asset.attributes![attributeDescriptor.name!] = {
                                            name: attributeDescriptor.name,
                                            type: attributeDescriptor.type,
                                            meta: attributeDescriptor.meta ? {...attributeDescriptor.meta} : undefined
                                        } as Attribute<any>;
                                    });
                            }

                            if (selectedOptionalAttributes) {
                                selectedOptionalAttributes?.forEach(attribute => {
                                    asset.attributes![attribute.name!] = {
                                        name: attribute.name,
                                        type: attribute.type,
                                        meta: attribute.meta ? {...attribute.meta} : undefined
                                    }
                                });
                            }

                            if (this.selectedIds) {
                                asset.parentId = parent ? parent.id : undefined;
                            }
                            const detail: AddEventDetail = {
                                asset: asset
                            };
                            Util.dispatchCancellableEvent(this, new OrAssetTreeRequestAddEvent(detail))
                                .then((detail) => {
                                    if (detail.allow) {
                                        this.dispatchEvent(new OrAssetTreeAddEvent(detail.detail));
                                    }
                                });
                        }
                    }
                ])
            .setStyles(html`
                    <style>
                        .mdc-dialog__content {
                            padding: 0 !important;
                        }
                    </style>
                `)
            .setDismissAction(null)
        );
    }

    protected _gatewayDescendantIsSelected(): boolean {
        return this._selectedNodes.some((n) => {
            let parentNode = n?.parent;
            while (parentNode) {
                if (parentNode.asset?.type === WellknownAssets.GATEWAYASSET) {
                    return true;
                }
                parentNode = parentNode.parent;
            }
            return false;
        });
    }

    protected _onDeleteClicked() {
        if (this._selectedNodes.length > 0) {
            Util.dispatchCancellableEvent(this, new OrAssetTreeRequestDeleteEvent(this._selectedNodes))
                .then((detail) => {
                    if (detail.allow) {
                        this._doDelete();
                    }
                });
        }
    }

    protected _onSortClicked(sortBy: string) {
        this.sortBy = sortBy;
    }

    protected _doDelete() {

        if (!this._selectedNodes || this._selectedNodes.length === 0) {
            return;
        }

        const uniqueAssets = new Set<Asset>();

        // Add gateway nodes first
        const nodes = this._selectedNodes.filter((node) => {
            if (node.asset?.type === WellknownAssets.GATEWAYASSET) {
                // Add gateway straight to the unique list and don't recursively select children
                uniqueAssets.add(node.asset!);
                return false;
            }
            return true;
        })

        // Iterate through descendants of selected nodes that aren't gateways
        // and add to delete list (don't recurse descendant gateway nodes)
        OrAssetTree._forEachNodeRecursive(nodes, (node) => {
            // Check no ancestor is of type gateway
            let ancestor = node.parent;
            let okToAdd = true;
            while (ancestor && okToAdd) {
                const ancestorType = ancestor?.asset?.type;
                if (ancestorType === WellknownAssets.GATEWAYASSET) {
                    okToAdd = false;
                }
                ancestor = ancestor.parent;
            }
            if (okToAdd) {
                uniqueAssets.add(node.asset!);
            }
        });
        const assetIds: string[] = Array.from(uniqueAssets).map(asset => asset.id!);
        const assetNames: string[] = Array.from(uniqueAssets).map(asset => asset.name!);

        const doDelete = () => {
            this.disabled = true;

            manager.rest.api.AssetResource.delete({
                assetId: assetIds
            }, {
                paramsSerializer: params => Qs.stringify(params, {arrayFormat: 'repeat'})
            }).then((response) => {
                this._onDeselectClicked();
                if (response.status !== 204) {
                    showErrorDialog(i18next.t("deleteAssetsFailed"));
                }
            }).catch((reason) => {
                showErrorDialog(i18next.t("deleteAssetsFailed"));
            }).finally(() => {
                this.disabled = false;
            });
        };

        // Confirm deletion request
        showOkCancelDialog(i18next.t("deleteAssets"), i18next.t("deleteAssetsConfirm", { assetNames: assetNames.join(",\n- ") }), i18next.t("delete"))
            .then((ok) => {
                if (ok) {
                    doDelete();
                }
            });
    }

    protected _canAdd(): boolean {
        if (this._selectedNodes && this._selectedNodes.length > 1) {
            return false;
        }
        const selectedNode = this._selectedNodes ? this._selectedNodes[0] : undefined;

        if (selectedNode?.asset?.type === WellknownAssets.GATEWAYASSET) {
            // Cannot add to a gateway asset
            return false;
        }

        if (this._gatewayDescendantIsSelected()) {
            // Cannot add to a descendant of a gateway asset
            return false;
        }
        return this._getAllowedChildTypes(selectedNode).length > 0;
    }

    protected _getAllowedChildTypes(selectedNode: UiAssetTreeNode | undefined): AssetDescriptor[] {
        let includedAssetTypes: string[] | undefined;
        let excludedAssetTypes: string[];

        if (this.config && this.config.add) {
            if (this.config.add.typesProvider) {
                const allowedTypes = this.config.add.typesProvider(selectedNode);
                if (allowedTypes) {
                    return allowedTypes;
                }
            }

            if (this.config.add.typesParent) {
                let config: AssetTreeTypeConfig | undefined;

                if (!selectedNode && this.config.add.typesParent.none) {
                    config = this.config.add.typesParent.none;
                } else if (selectedNode && this.config.add.typesParent.assetTypes) {
                    config = this.config.add.typesParent.assetTypes[selectedNode.asset!.type!];
                }

                if (!config) {
                    config = this.config.add.typesParent.default;
                }

                if (config) {
                    includedAssetTypes = config.include;
                    excludedAssetTypes = config.exclude || [];
                }
            }
        }

        return AssetModelUtil.getAssetDescriptors()
            .filter((descriptor) => (!includedAssetTypes || includedAssetTypes.some((inc) => Util.stringMatch(inc, descriptor.name!)))
                && (!excludedAssetTypes || !excludedAssetTypes.some((exc) => Util.stringMatch(exc, descriptor.name!))));
    }

    protected _getSortFunction(): (a: UiAssetTreeNode, b: UiAssetTreeNode) => number {
        switch (this.sortBy) {
            case "createdOn":
                return Util.sortByNumber((node: UiAssetTreeNode) => (node.asset as any)![this.sortBy!]);
            default:
                return Util.sortByString((node: UiAssetTreeNode) => (node.asset as any)![this.sortBy!]);
        }
    }

    protected _getOrderBy(sortBy?: string): AssetQueryOrderBy$Property {
        switch (sortBy) {
            case "createdOn": return AssetQueryOrderBy$Property.CREATED_ON;
            case "type": return AssetQueryOrderBy$Property.ASSET_TYPE;
            default: return AssetQueryOrderBy$Property.NAME;
        }
    }

    /**
     * Main function to load assets and populate the tree.
     * Based on the HTML attributes of this component, it either fetches using a WebSocket connection or using a dataProvider.
     * Once retrieved, these assets will be passed along to the {@link _buildTreeNodes} to construct the tree nodes.
     * If the user has applied a filter, it will also be taken into count using {@link _doFiltering}.
     *
     * @param parentId - The parent ID an asset MUST be a child of during WebSocket retrieval. This is useful for pagination.
     * @param offset - Offset number of the assets to request through WebSocket. This is useful for pagination.
     * @param cache - An array of assets to populate the tree with alongside the retrieved nodes.
     * @protected
     */
    protected async _loadAssets(parentId?: string, offset = 0, cache?: Asset[]): Promise<AssetTreeEvent | undefined> {
        console.debug(`Loading assets with ${parentId ? `parent ${parentId}` : `no parents`}...`);

        // If asset objects are provided in the HTML attribute, load these instead.
        if (this.assets) {
            console.debug(`Assets already pre-loaded using HTML attributes; reusing them to construct the tree UI...`);
            this._loading = false;
            this._buildTreeNodes(this.assets);
            return;
        }

        if (!this._connected) {
            throw new Error("Not connected to the server; cannot load assets.");
        }

        if (this._loading) {
            throw new Error("Already loading assets for asset tree; ignoring request.");
        }

        this._loading = true;

        if(this.dataProvider) {
            this.dataProvider(offset, this.queryLimit, parentId).then(assets => {
                this._loading = false;
                this._buildTreeNodes(assets);
                if(this._filterInput?.value) {
                    this._doFiltering();
                }
            });

        } else {
            const query: AssetQuery = {
                realm: {
                    name: manager.displayRealm
                },
                // parents: parentId ? [{ id: parentId }] : [], // Filters by parent ID. If parentId is null, it will only request 'top level' assets.
                select: { // Just need the basic asset info
                    attributes: []
                },
                orderBy: {
                    property: this._getOrderBy(this.sortBy)
                },
                offset: offset,
                limit: Math.max(this.queryLimit, 1)
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

            // We request the number of assets through the HTTP API, and disable pagination when there are less than 1000 assets.
            const countResponse = await manager.rest.api.AssetResource.queryCount({...query, limit: 1000});
            if (countResponse.data < 1000) {
                query.parents = undefined;
            }

            const eventPromise = this._sendEventWithReply({
                eventType: "read-asset-tree",
                assetQuery: query
            });
            eventPromise.then(ev => {
                const newAssets = (ev as AssetTreeEvent).assetTree?.assets ?? [];
                const hasMore = (ev as AssetTreeEvent).assetTree?.hasMore ?? false;
                if(!parentId) {
                    this._hasMoreParents = hasMore;
                } else if(parentId && this._incompleteParentIds.includes(parentId) && !hasMore) {
                    this._incompleteParentIds = this._incompleteParentIds.filter(id => id !== parentId);
                } else if(parentId && hasMore) {
                    this._incompleteParentIds = [...this._incompleteParentIds, parentId];
                }
                console.debug(`Received read-assets-tree event with ${newAssets.length} assets.`);
                console.debug(`Combining these assets with the cache of ${cache?.length ?? 0} assets...`);
                this._loading = false;
                if(cache) {
                    const assets = [...cache, ...newAssets.filter(a => !cache.find(c => c.id === a.id))];
                    this._buildTreeNodes(assets);
                } else {
                    this._buildTreeNodes(newAssets);
                }
                if(this._filterInput?.value) {
                    this._doFiltering();
                }
            }) as Promise<AssetTreeEvent>;

            return eventPromise as Promise<AssetTreeEvent>;
        }
    }

    /* Subscribe mixin overrides */

    public async _addEventSubscriptions(): Promise<void> {
        if (!this.disableSubscribe) {
            // Subscribe to asset events for all assets in the realm
            this._subscriptionIds = [await manager.getEventProvider()!.subscribeAssetEvents(undefined, false, (event) => this._onEvent(event))];
        }
    }

    public onEventsConnect() {
        this._connected = true;
        this._loadAssets().catch(ex => console.log(ex.message));
    }

    public onEventsDisconnect() {
        this._connected = false;
        this._nodes = undefined;
    }

    public getNodes(): UiAssetTreeNode[] {
        return this._nodes || [];
    }

    public _onEvent(event: SharedEvent) {

        if (event.eventType === "assets") {
            const assetsEvent = event as AssetsEvent;
            this._buildTreeNodes(assetsEvent.assets!);
            return;
        }

        if (event.eventType === "asset") {

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
            if (this._nodes) {
                OrAssetTree._forEachNodeRecursive(this._nodes, (node) => {
                    if (node.asset!.id !== assetEvent.asset!.id) {
                        assets.push(node.asset!);
                    }
                });
            }

            // In case of filter already active, do not override the actual state of assetTree
            this._buildTreeNodes(assets);
            if (this._filterInput?.value) {
                this._doFiltering();
            }
            this.dispatchEvent(new OrAssetTreeAssetEvent(assetEvent));
        }
    }

    /**
     * Function that creates and constructs the tree node objects to display.
     * @param assets - List of assets to display in the tree
     * @param sortFunction - Optional sorting function for ordering the nodes
     * @protected
     */
    protected _buildTreeNodes(assets: Asset[], sortFunction = this._getSortFunction()) {
        console.debug(`Building asset tree nodes for ${assets.length} assets...`);
        if (!assets || assets.length === 0) {
            this._nodes = [];
        } else {
            if (manager.isRestrictedUser()) {
                // Restricted users might have access to children, without access to the parent asset.
                // Any assets whose parents aren't accessible need to be 're-parented'.
                assets.forEach(asset => {
                    if (!!asset.parentId && !!asset.path && assets.find(a => a.id === asset.parentId) === undefined) {
                        let reparentId = null;

                        // Loop through ALL assets in the path, and check if they're present in the (restricted) asset list
                        // Once found, update its parent ID without replacing the original (that's why it's named 'reparentId').
                        for (let i = 0; i < asset.path!.length; i++) {
                            const ancestorId = asset.path![i];
                            if (asset.id !== ancestorId && assets.find(a => a.id === ancestorId) !== undefined) {
                                reparentId = ancestorId;

                                // break; No break statement here, as when an asset further down the tree has been found, it should overwrite the parent ID.
                            }
                        }
                        (asset as AssetWithReparentId).reparentId = reparentId;
                    }
                });
            }

            let rootAssetIds: string[] | undefined;

            if (this.rootAssetIds) {
                rootAssetIds = this.rootAssetIds;
            } else if (this.rootAssets) {
                rootAssetIds = this.rootAssets.map((ra) => ra.id!);
            }

            let rootAssets: UiAssetTreeNode[];

            if (rootAssetIds) {
                rootAssets = assets.filter((asset: AssetWithReparentId) => rootAssetIds!.indexOf(asset.id!) >= 0 || asset.reparentId === null).map((asset) => {
                    return {
                        asset: asset
                    } as UiAssetTreeNode;
                });
            } else {
                rootAssets = assets.filter((asset: AssetWithReparentId) => !asset.parentId || asset.reparentId === null).map((asset) => {
                    return {
                        asset: asset
                    } as UiAssetTreeNode;
                });
            }

            this.assetsChildren = {};

            assets.forEach((asset: AssetWithReparentId) => {
                if (asset.parentId) {
                    if (!this.assetsChildren[asset.parentId]) {
                        this.assetsChildren[asset.parentId] = [];
                    }
                    this.assetsChildren[asset.parentId].push({
                        asset: asset
                    } as UiAssetTreeNode);
                }

                if (asset.reparentId) {
                    if (!this.assetsChildren[asset.reparentId]) {
                        this.assetsChildren[asset.reparentId] = [];
                    }
                    this.assetsChildren[asset.reparentId].push({
                        asset: asset
                    } as UiAssetTreeNode);
                }
            });

            rootAssets.sort(sortFunction);
            rootAssets.forEach((rootAsset) => this._buildChildTreeNodes(rootAsset, assets, sortFunction));
            this._nodes = rootAssets;
            const newExpanded: UiAssetTreeNode[] = [];
            this._expandedNodes.forEach(expandedNode => {
                OrAssetTree._forEachNodeRecursive(this._nodes!, n => {
                    if (n.asset?.id && expandedNode?.asset?.id && n.asset.id === expandedNode.asset.id && this.isExpandable(expandedNode.asset.id)) {
                        n.expanded = true;
                        newExpanded.push(n);

                        // Expand every ancestor
                        let parent = n.parent;
                        while (parent) {
                            parent.expanded = true;
                            parent = parent.parent;
                            if (newExpanded.indexOf(parent) < 0) {
                                newExpanded.push(parent);
                            }
                        }
                    }
                });
            });
            this._expandedNodes = newExpanded;
        }

        console.debug(`Asset tree nodes built. Now selecting ${this.selectedIds?.length} nodes...`);
        if (this.selectedIds && this.selectedIds.length > 0) {
            this._updateSelectedNodes();
        }

        if (this.expandAllNodes) {
            OrAssetTree._forEachNodeRecursive(this._nodes, (node) => {
                if (node.children && node.children.length > 0) {
                    node.expanded = true;
                }
            });
        }
    }

    protected _buildChildTreeNodes(treeNode: UiAssetTreeNode, assets: AssetWithReparentId[], sortFunction: (a: UiAssetTreeNode, b: UiAssetTreeNode) => number) {
        let children: UiAssetTreeNode[] | undefined = this.assetsChildren[treeNode.asset!.id!];
        treeNode.children = children ? children.sort(sortFunction) : [];
        treeNode.expandable = (treeNode.asset as any)?.hasChildren || treeNode.children?.length;

        treeNode.children.forEach((childNode) => {
            childNode.parent = treeNode;
            this._buildChildTreeNodes(childNode, assets, sortFunction);
        });
    }

    public _onDragStart(ev: any): void {
        this._dragDropParentId = null;

        let currentElement = ev.currentTarget as HTMLElement;
        let selectedId: string | null = currentElement.getAttribute('node-asset-id');

        if (!this.selectedIds) {
            this.selectedIds = [];
        }

        if (selectedId && this.selectedIds && !this.selectedIds.includes(selectedId)) {
            if (!ev.ctrlKey && !ev.shiftKey) {
                this.selectedIds = [];
            }
            this.selectedIds.push(selectedId);
        }
    }

    public _onDragEnd(ev: any): void {
        const dragEndTargetX: number = ev.x;
        const dragEndTargetY: number = ev.y;

        if (this.shadowRoot !== null) {
            let listElement: HTMLElement | null = this.shadowRoot.getElementById('list');

            if (listElement) {
                const topY: number = listElement.getBoundingClientRect().top;
                const bottomY: number = listElement.getBoundingClientRect().bottom;
                const leftX: number = listElement.getBoundingClientRect().left;
                const rightX: number = listElement.getBoundingClientRect().right;

                if (dragEndTargetX < leftX || dragEndTargetX > rightX || dragEndTargetY > bottomY || dragEndTargetY < topY) {
                    return;
                }
            }
        }

        if (this.selectedIds) {
            this.dispatchEvent(new OrAssetTreeChangeParentEvent(!this._dragDropParentId ? undefined : this._dragDropParentId, this.selectedIds));
        }
    }

    protected isExpandable(assetId: string): boolean {
        if (this._nodes) {
            if (this.shadowRoot) {
                let elem: HTMLElement | null = this.shadowRoot.querySelector('[node-asset-id="' + assetId + '"] > .node-name > [data-expandable]');

                if (elem) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    public _onDragOver(ev: any): void {
        let currentElement = ev.currentTarget as HTMLElement;

        currentElement.classList.add('over');

        let assetId: string | null = currentElement.getAttribute('node-asset-id');

        if (assetId && this.isExpandable(assetId) && !this._expandTimer) {
            this._expandTimer = window.setTimeout(() => {
                this.expandNode(assetId);
            }, 1000);
        }
    }

    protected expandNode(assetId: string | null): void {
        if (this.shadowRoot && assetId && assetId === this._dragDropParentId) {
            const node = this._findNodeFromAssetId(assetId);
            let elem: HTMLElement | null = this.shadowRoot?.querySelector('[node-asset-id="' + assetId + '"]');
            if(elem && node && !node.expanded) {
                this._toggleExpander(elem.firstElementChild!.firstElementChild! as HTMLElement, node, true);
            }
        }
    }

    public _onDragEnter(ev: any): void {
        let currentElement = ev.currentTarget as HTMLElement;

        currentElement.classList.add('over');

        let enteredId: string | null = currentElement.getAttribute('node-asset-id');

        this._dragDropParentId = enteredId;
    }

    public _onDragLeave(ev: any): void {
        let currentElement = ev.currentTarget as HTMLElement;

        currentElement.classList.remove('over');

        clearTimeout(this._expandTimer);
        this._expandTimer = undefined;
    }

    /**
     * Generates the HTML TemplateResult for an individual node / tree item.
     * @param treeNode Node to display
     * @param level Level of depth in the tree from 0 to infinite. (0 = top level. If it has 2 parents, level = 2)
     * @protected
     */
    protected _treeNodeTemplate(treeNode: UiAssetTreeNode, level: number): TemplateResult | string | undefined {
        const descriptor = AssetModelUtil.getAssetDescriptor(treeNode.asset!.type!);

        let parentCheckboxIcon;
        if (treeNode.allChildrenSelected) {
            parentCheckboxIcon = 'checkbox-multiple-marked';
        } else if (treeNode.someChildrenSelected) {
            parentCheckboxIcon = 'checkbox-multiple-marked-outline';
        } else {
            parentCheckboxIcon = 'checkbox-multiple-blank-outline';
        }

        if (treeNode.hidden) {
            return html``;
        }

        let filterColorForNonMatchingAsset: boolean = false;

        if (treeNode.asset && treeNode.notMatchingFilter) {
            filterColorForNonMatchingAsset = true;
        }

        if (treeNode.expanded && treeNode.children.length === 0) {
            console.debug("Tree node has no children, collapsing it...");
            treeNode.expanded = false;
        }

        return html`
            <li class="asset-list-element" ?data-selected="${treeNode.selected}" ?data-expanded="${treeNode.expanded}" @click="${(evt: MouseEvent) => this._onNodeClicked(evt, treeNode)}">
                <div class="in-between-element" node-asset-id="${treeNode.parent ? (treeNode.parent.asset ? treeNode.parent.asset.id : '' ) : undefined}" @dragleave=${(ev: DragEvent) => { this._onDragLeave(ev) }} @dragenter="${(ev: DragEvent) => this._onDragEnter(ev)}" @dragend="${(ev: DragEvent) => this._onDragEnd(ev)}" @dragover="${(ev: DragEvent) => this._onDragOver(ev)}"></div>
                <div class="node-container draggable" node-asset-id="${treeNode.asset ? treeNode.asset.id : ''}" draggable="${!this._isReadonly()}" @dragleave=${(ev: DragEvent) => { this._onDragLeave(ev) }} @dragenter="${(ev: DragEvent) => this._onDragEnter(ev)}" @dragstart="${(ev: DragEvent) => this._onDragStart(ev)}" @dragend="${(ev: DragEvent) => this._onDragEnd(ev)}" @dragover="${(ev: DragEvent) => this._onDragOver(ev)}" style="padding-left: ${level * 22}px">
                    <div class="node-name">
                        <div class="expander" ?data-expandable="${treeNode.expandable}"></div>
                        ${getAssetDescriptorIconTemplate(descriptor, undefined, undefined, (filterColorForNonMatchingAsset ? 'd3d3d3' : undefined))}
                        <span style="color: ${filterColorForNonMatchingAsset ? '#d3d3d3;' : ''}">${treeNode.asset!.name}</span>
                        ${this.checkboxes ? html`
                            <span class="mdc-list-item__graphic">
                                ${treeNode.expandable 
                                    ? html`<div class="mdc-checkbox">
                                            <or-icon class="mdc-checkbox--parent" icon="${parentCheckboxIcon}"></or-icon>
                                        </div>`
                                    : ``}
                                <div class="mdc-checkbox">
                                    ${treeNode.selected ? html`<or-icon icon="checkbox-marked"></or-icon>`: html`<or-icon icon="checkbox-blank-outline"></or-icon>`}
                                </div>
                            </span>` 
                        : ``}
                    </div>
                </div>
                <ol>
                    ${!treeNode.children || (treeNode.expandable && !treeNode.expanded)  ? `` : treeNode.children.map((childNode) => this._treeNodeTemplate(childNode, level + 1)).filter(t => !!t)}
                    ${when(treeNode.asset?.id && this._incompleteParentIds.includes(treeNode.asset.id), () => html`
                        <li class="asset-list-element loadmore-element">
                            <or-mwc-input type=${InputType.BUTTON} outlined label="loadMore" style="padding-left: ${(level + 1) * 22}px;"></or-mwc-input>
                        </li>
                    `)}
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
}
