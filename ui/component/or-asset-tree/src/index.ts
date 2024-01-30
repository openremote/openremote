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
    AssetsEvent,
    AssetTreeNode,
    AssetTypeInfo,
    Attribute,
    AttributePredicate,
    ClientRole,
    LogicGroup,
    LogicGroupOperator,
    SharedEvent,
    StringPredicate
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

import {
    OrMwcDialog,
    showDialog,
    showErrorDialog,
    showOkCancelDialog
} from "@openremote/or-mwc-components/or-mwc-dialog";
import {OrAddAssetDialog, OrAddChangedEvent} from "./or-add-asset-dialog";
import "./or-add-asset-dialog";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import { when } from "lit/directives/when.js";

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

export class OrAssetTreeChangeParentEvent extends CustomEvent<any> {

    public static readonly NAME = "or-asset-tree-change-parent";

    constructor(parent: string | undefined, assetsIds: string[]) {
        super(OrAssetTreeChangeParentEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: {
                parentId: parent,
                assetsIds: assetsIds
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

    constructor() {
        this.asset = undefined;
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

export const getAssetTypes = async () => {
    const response = await manager.rest.api.AssetResource.queryAssets({
        select: {
            attributes: []
        },
        recursive: true
    });

    if(response && response.data) {
        return response.data.map(asset => asset.type!);
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
    public assets?: Asset[];

    @property({type: Object})
    public assetInfos?: AssetTypeInfo[];

    @property({type: Array})
    public _assetIdsOverride?: string[];

    @property({type: Array})
    public rootAssets?: Asset[];

    @property({type: Array})
    public rootAssetIds?: string[];

    @property({type: Object})
    public dataProvider?: () => Promise<Asset[]>;

    @property({type: Boolean})
    public readonly: boolean = false;

    @property({type: Boolean})
    public disabled: boolean = false;

    @property({type: Boolean})
    public disableSubscribe: boolean = false;

    @property({type: Array})
    public selectedIds?: string[];

    @property({type: Boolean})
    public showDeselectBtn?: boolean = true;

    @property({type: Boolean})
    public showSortBtn?: boolean = true;

    @property({type: Boolean})
    public showFilter: boolean = true;

    @property({type: String})
    public sortBy?: string = "name";

    @property({type: Boolean})
    public expandAllNodes?: boolean = false;

    @property({type: Array})
    public expandedIds?: string[] = [];

    @property({type: Boolean})
    public checkboxes?: boolean = false;

    protected config?: AssetTreeConfig;

    @property({attribute: false})
    protected _nodes?: UiAssetTreeNode[];

    protected _loading: boolean = false;
    protected _connected: boolean = false;
    protected _selectedNodes: UiAssetTreeNode[] = [];
    protected _expandedNodes: UiAssetTreeNode[] = [];
    protected _initCallback?: EventCallback;

    @state()
    protected _filter: OrAssetTreeFilter = new OrAssetTreeFilter();
    protected _searchInputTimer?: number = undefined;
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
        return html`
            <div id="header">
                <div id="title-container">
                    <or-translate id="title" value="asset_plural"></or-translate>
                </div>

                <div id="header-btns">
                    <or-mwc-input ?hidden="${!this.selectedIds || this.selectedIds.length === 0 || !this.showDeselectBtn}" type="${InputType.BUTTON}" icon="close" @or-mwc-input-changed="${() => this._onDeselectClicked()}"></or-mwc-input>
                    <or-mwc-input ?hidden="${this._isReadonly() || !this.selectedIds || this.selectedIds.length !== 1}" type="${InputType.BUTTON}" icon="content-copy" @or-mwc-input-changed="${() => this._onCopyClicked()}"></or-mwc-input>
                    <or-mwc-input ?hidden="${this._isReadonly() || !this.selectedIds || this.selectedIds.length === 0}" type="${InputType.BUTTON}" icon="delete" @or-mwc-input-changed="${() => this._onDeleteClicked()}"></or-mwc-input>
                    <or-mwc-input ?hidden="${this._isReadonly() || !this._canAdd()}" type="${InputType.BUTTON}" icon="plus" @or-mwc-input-changed="${() => this._onAddClicked()}"></or-mwc-input>
                    <or-mwc-input hidden type="${InputType.BUTTON}" icon="magnify" @or-mwc-input-changed="${() => this._onSearchClicked()}"></or-mwc-input>
                    
                    ${getContentWithMenuTemplate(
                            html`<or-mwc-input type="${InputType.BUTTON}" ?hidden="${!this.showSortBtn}" icon="sort-variant"></or-mwc-input>`,
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
                                  @input="${(e: KeyboardEvent) => {
                                      // Means some input is occurring so delay filter
                                      this._onFilterInputEvent(e);
                                  }}"
                                  @or-mwc-input-changed="${(e: OrInputChangedEvent) => {
                                      // Means field has lost focus so do filter immediately
                                      this._onFilterInput((e.detail.value as string) || undefined, true);
                                  }}">
                    </or-mwc-input>
                    <or-icon id="filterSettingsIcon" icon="${this._filterSettingOpen ? "window-close" : "tune"}" @click="${() => {
                        if (this._filterSettingOpen) {
                            this._filterSettingOpen = false;
                        } else {
                            this._filterSettingOpen = true;
                            // Avoid to build again the types
                            if (this._assetTypes.length === 0) {
                                let usedTypes: string[] = [];
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

                                this._filter = new OrAssetTreeFilter();

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
            
            ${!this._nodes
                ? html`
                    <span id="loading"><or-translate value="loading"></or-translate></span>`
                : ((this._nodes.length === 0 || !this.atLeastOneNodeToBeShown())
                            ? html `<span id="noAssetsFound"><or-translate value="noAssetsFound"></or-translate></span>` 
                            : html`
                    <div id="list-container">
                        <ol id="list">
                            ${this._nodes.map((treeNode) => this._treeNodeTemplate(treeNode, 0)).filter(t => !!t)}
                            <li class="asset-list-element">    
                                <div class="end-element" node-asset-id="${''}" @dragleave=${(ev: DragEvent) => { this._onDragLeave(ev) }} @dragenter="${(ev: DragEvent) => this._onDragEnter(ev)}" @dragend="${(ev: DragEvent) => this._onDragEnd(ev)}" @dragover="${(ev: DragEvent) => this._onDragOver(ev)}"></div>
                            </li>
                        </ol>
                    </div>
                `)
            }

            <div id="footer">
            
            </div>
        `;
    }

    protected _isReadonly() {
        return this.readonly || !manager.hasRole(ClientRole.WRITE_ASSETS);
    }

    protected shouldUpdate(_changedProperties: PropertyValues): boolean {
        const result = super.shouldUpdate(_changedProperties);
        if (_changedProperties.has("assets")
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

        if (_changedProperties.has("disabledSubscribe")) {
            if (this.disableSubscribe) {
                this._removeEventSubscriptions();
            }
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

            if (this.checkboxes) {
                let parent = node.parent;
                while (parent) {
                    const allChildren: UiAssetTreeNode[] = [];
                    OrAssetTree._forEachNodeRecursive(parent.children, (child) => {
                        allChildren.push(child);
                    });
                    parent.someChildrenSelected = false;
                    parent.allChildrenSelected = false;

                    if (allChildren.every(c => actuallySelectedIds.includes(c.asset!.id!))) {
                        parent.allChildrenSelected = true;
                    } else if (allChildren.some(c => actuallySelectedIds.includes(c.asset!.id!))) {
                        parent.someChildrenSelected = true;
                    }

                    parent = parent.parent;
                }
            }
        });

        this.selectedIds = actuallySelectedIds;
        const oldSelection = this._selectedNodes;
        this._selectedNodes = selectedNodes;
        this.dispatchEvent(new OrAssetTreeSelectionEvent({
            oldNodes: oldSelection,
            newNodes: selectedNodes
        }));
    }

    protected _updateSort(nodes: UiAssetTreeNode[], sortFunction: (a: UiAssetTreeNode, b: UiAssetTreeNode) => number) {
        if (!nodes) {
            return;
        }

        nodes.sort(sortFunction);
        nodes.forEach((node) => this._updateSort(node.children, sortFunction));
    }

    protected _toggleExpander(expander: HTMLElement, node: UiAssetTreeNode | null) {
        if (node && node.expandable) {
            node.expanded = !node.expanded;

            if (node.expanded) {
                this._expandedNodes.push(node);
            } else {
                this._expandedNodes = this._expandedNodes.filter(n => n !== node);
            }

            const elem = expander.parentElement!.parentElement!.parentElement!;
            elem.toggleAttribute("data-expanded");
            this.dispatchEvent(new OrAssetTreeToggleExpandEvent({ node: node }));
            this.requestUpdate();
        }
    }

    /**
     * This method is used to avoid to re-render and erase all the this.selectedIds attribute
     *
     * @param expander
     * @param node
     * @protected
     */
    protected _toggleExpanderWithoutEventDispatch(expander: HTMLElement, node: UiAssetTreeNode | null) {
        if (node && node.expandable) {
            node.expanded = !node.expanded;

            if (node.expanded) {
                this._expandedNodes.push(node);
            } else {
                this._expandedNodes = this._expandedNodes.filter(n => n !== node);
            }

            const elem = expander.parentElement!.parentElement!.parentElement!;
            elem.toggleAttribute("data-expanded");
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

        if (isExpander) {
            this._toggleExpander((evt.target as HTMLElement), node);
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

    protected parseFromInputFilter(inputValue?: string): OrAssetTreeFilter {
        let searchValue: string | undefined = this._filterInput.value;
        if (inputValue) {
            searchValue = inputValue;
        }
        let resultingFilter: OrAssetTreeFilter = new OrAssetTreeFilter();

        if (searchValue) {
            let asset: string = searchValue;
            let matchingResult: RegExpMatchArray | null = searchValue.match(/(attribute\:)(\"[^"]+\")\S*/g);
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

            matchingResult = searchValue.match(/(type\:)\S+/g);
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

            matchingResult = searchValue.match(/(\"[^\"]+\")\:(([^\"\s]+)|(\"[^\"]+\"))/g);
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

        this._filter = filterFromSearchInputWithSettings;

        let newFilterForSearchInput: string = this.formatFilter(this._filter);

        this._filterInput.value = newFilterForSearchInput;

        this._filterSettingOpen = false;

        this._doFiltering();
    }

    protected _onFilterInputEvent(e: KeyboardEvent) {
        let value: string | undefined;

        if (e.composedPath()) {
            value = ((e.composedPath()[0] as HTMLInputElement).value) || undefined;
        }

        this._onFilterInput(value, false);
    }

    protected _onFilterInput(newValue: string | undefined, force: boolean): void {
        let currentFilter: OrAssetTreeFilter = this.parseFromInputFilter(newValue);

        if (Util.objectsEqual(this._filter, currentFilter,true)) {
            return;
        }

        this._filter = currentFilter;

        if (this._searchInputTimer) {
            clearTimeout(this._searchInputTimer);
        }

        if (!force) {
            this._searchInputTimer = window.setTimeout(() => {
                this._doFiltering();
            }, 350);
        } else {
            this._doFiltering();
        }
    }

    protected async _doFiltering() {
        // Clear timeout in case we got here from value change
        if (this._searchInputTimer) {
            clearTimeout(this._searchInputTimer);
            this._searchInputTimer = undefined;
        }

        if (this.isConnected && this._nodes) {

            if (!this._filter.asset && !this._filter.attribute && !this._filter.assetType && !this._filter.attributeValue) {
                // Clear the filter
                OrAssetTree._forEachNodeRecursive(this._nodes!, (node) => {
                    node.notMatchingFilter = false;
                    node.hidden = false;
                });
                this.requestUpdate("_nodes");
                return;
            }

            this.disabled = true;

            // Use a matcher function - this can be altered independent of the filtering logic
            // Maybe we should just filter in memory for basic matches like name
            if (this._filter.asset || this._filter.assetType || this._filter.attribute) {
                let queryRequired: boolean = false;

                if (this._filter.attribute) {
                    queryRequired = true;
                }

                this.getMatcher(queryRequired).then((matcher: (asset: Asset) => boolean) => {
                    if (this._nodes) {
                        this._nodes.forEach((node: UiAssetTreeNode) => {
                            this.filterTreeNode(node, matcher);
                        });
                        this.disabled = false;
                    }
                });
            }
        }
    }

    protected getMatcher(requireQuery: boolean): Promise<((asset: Asset) => boolean)> {
        if (requireQuery) {
            return this.getMatcherFromQuery();
        } else {
            return this.getSimpleNameMatcher();
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

    protected async getMatcherFromQuery(): Promise<((asset: Asset) => boolean)> {
        let assetCond: StringPredicate[] | undefined = undefined;
        let attributeCond: LogicGroup<AttributePredicate> | undefined = undefined;
        let assetTypeCond: string[] | undefined = undefined;

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
            }
        }

        const query: AssetQuery = {
            select: {
                attributes: attributeCond ? undefined : []
            },
            names: assetCond,
            types: assetTypeCond,
            attributes: attributeCond
        };



        let response: any;
        let foundAssetIds: string[];

       try {
           response = await manager.rest.api.AssetResource.queryAssets(query);
           foundAssetIds = response.data.map((asset: Asset) => asset.id!);
       } catch (e) {
            this._filter.assetType.forEach((assetT: string) => {
                if(this._assetTypes.findIndex((assetD: AssetDescriptor) => { return assetD.name === assetT; }) === -1) {
                    showSnackbar(undefined, "filter.assetTypeDoesNotExist", "dismiss");
                }
            });
            foundAssetIds = [];
        }

        return (asset) => {
            let attrValueCheck = true;

            if (this._filter.attribute.length > 0 && this._filter.attributeValue.length > 0 && foundAssetIds.includes(asset.id!)) {
                let attributeVal: [string, string][] = [];

                this._filter.attributeValue.forEach((attrVal: string, index: number) => {
                    if (attrVal.length > 0) {
                        attributeVal.push([this._filter.attribute[index], attrVal]);
                    }
                });

                let matchingAsset: Asset | undefined = response.data.find((a: Asset) => a.id === asset.id );

                if (matchingAsset && matchingAsset.attributes) {
                    for (let attributeValIndex = 0; attributeValIndex < attributeVal.length; attributeValIndex++ ) {
                        let currentAttributeVal = attributeVal[attributeValIndex];

                        let atLeastOneAttributeMatchValue: boolean = false;
                        Object.keys(matchingAsset.attributes).forEach((key: string) => {
                            let attr: Attribute<any> = matchingAsset!.attributes![key];

                            // attr.value check to avoid to compare with empty/non existing value
                            if (attr.name!.toLowerCase() === currentAttributeVal[0].toLowerCase() && attr.value) {
                                switch (attr.type!) {
                                    case "number":
                                    case "integer":
                                    case "long":
                                    case "bigInteger":
                                    case "bigNumber":
                                    case "positiveInteger":
                                    case "negativeInteger":
                                    case "positiveNumber":
                                    case "negativeNumber":
                                        let value: string = currentAttributeVal[1];
                                        if (currentAttributeVal[1].startsWith('=') && currentAttributeVal[1][1] !== '=') {
                                            value = '=' + value;
                                        }

                                        if (/^[0-9]+$/.test(currentAttributeVal[1])) {
                                            value = '==' + value;
                                        }

                                        const resultNumberEval: boolean = eval(attr.value + value);

                                        if (resultNumberEval) {
                                            atLeastOneAttributeMatchValue = true;
                                        }
                                        break;
                                    case "text":
                                        if (attr.value) {
                                            let unparsedValue: string = currentAttributeVal[1];
                                            const multicharString: string = '*';

                                            let parsedValue: string = unparsedValue.replace(multicharString, '.*');
                                            parsedValue = parsedValue.replace(/"/g,'');

                                            let valueFromAttribute: string = attr.value as string;

                                            if (valueFromAttribute.toLowerCase().indexOf(parsedValue.toLowerCase()) != -1) {
                                                atLeastOneAttributeMatchValue = true;
                                            }
                                        }
                                        break;
                                }
                            }
                        });

                        attrValueCheck = atLeastOneAttributeMatchValue;
                    }
                }
            }

            return foundAssetIds.includes(asset.id!) && attrValueCheck;
        };
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

    protected _onSearchClicked() {

    }

    protected _onSortClicked(sortBy: string) {
        this.sortBy = sortBy;
    }

    protected _doDelete() {

        if (!this._selectedNodes || this._selectedNodes.length === 0) {
            return;
        }

        // Get all unique descendant IDs of selected nodes
        let uniqueAssets = new Set<Asset>();
        OrAssetTree._forEachNodeRecursive(this._selectedNodes, (node) => {
            uniqueAssets.add(node.asset!);
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
                // Clear nodes to re-fetch them
                this.refresh();
                this.disabled = false;

                if (response.status !== 204) {
                    showErrorDialog(i18next.t("deleteAssetsFailed"));
                }
            }).catch((reason) => {
                this.refresh();
                this.disabled = false;
                showErrorDialog(i18next.t("deleteAssetsFailed"));
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

    protected _loadAssets() {

        const sortFunction = this._getSortFunction();

        if (!this.assets) {

            if (!this._connected) {
                return;
            }

            if (this._loading) {
                return;
            }

            this._loading = true;

            if(this.dataProvider) {
                this.dataProvider().then(assets => {
                    this._loading = false;
                    this._buildTreeNodes(assets, sortFunction);
                })

            } else {

                const query: AssetQuery = {
                    realm: {
                        name: manager.displayRealm
                    },
                    select: { // Just need the basic asset info
                        attributes: []
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
                }).then((ev) => {
                    this._loading = false;
                    this._buildTreeNodes((ev as AssetsEvent).assets!, sortFunction)
                });
            }
        } else {
            this._loading = false;
            this._buildTreeNodes(this.assets, sortFunction);
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
        this._loadAssets();
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

            // In case of filter already active, do not override the actual state of assetTree
            if ( !this._filterInput.value ) {
                this._buildTreeNodes(assets, this._getSortFunction());
            }
            this.dispatchEvent(new OrAssetTreeAssetEvent(assetEvent));
        }
    }



    protected _buildTreeNodes(assets: Asset[], sortFunction: (a: UiAssetTreeNode, b: UiAssetTreeNode) => number) {
        if (!assets || assets.length === 0) {
            this._nodes = [];
        } else {
            if (manager.isRestrictedUser()) {
                // Any assets whose parents aren't accessible need to be re-parented
                assets.forEach(asset => {
                    if (!!asset.parentId && !!asset.path && assets.find(a => a.id === asset.parentId) === undefined) {
                        let reparentId = null;
                        for (let i = 2; i < asset.path!.length; i++) {
                            const ancestorId = asset.path![i];
                            if (assets.find(a => a.id === ancestorId) !== undefined) {
                                reparentId = ancestorId;
                                break;
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
                    if (n.asset && expandedNode.asset && n.asset.id === expandedNode.asset.id) {
                        n.expanded = true;
                        newExpanded.push(n);
                    }
                });
            });
            this._expandedNodes = newExpanded;
        }

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

        if (treeNode.children.length > 0) {
            treeNode.expandable = true;
        }

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

                this._toggleExpanderWithoutEventDispatch(elem.firstElementChild!.firstElementChild! as HTMLElement, node);
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
            treeNode.expanded = false;
        }

        if (this.expandedIds && this.expandedIds.findIndex((expandId: string) => { return expandId === treeNode.asset!.id!; }) !== -1) {
            treeNode.expanded = true;
        }

        return html`
            <li class="asset-list-element" ?data-selected="${treeNode.selected}" ?data-expanded="${treeNode.expanded}" @click="${(evt: MouseEvent) => this._onNodeClicked(evt, treeNode)}">
                <div class="in-between-element" node-asset-id="${treeNode.parent ? (treeNode.parent.asset ? treeNode.parent.asset.id : '' ) : undefined}" @dragleave=${(ev: DragEvent) => { this._onDragLeave(ev) }} @dragenter="${(ev: DragEvent) => this._onDragEnter(ev)}" @dragend="${(ev: DragEvent) => this._onDragEnd(ev)}" @dragover="${(ev: DragEvent) => this._onDragOver(ev)}"></div>
                <div class="node-container draggable" node-asset-id="${treeNode.asset ? treeNode.asset.id : ''}" draggable="true" @dragleave=${(ev: DragEvent) => { this._onDragLeave(ev) }} @dragenter="${(ev: DragEvent) => this._onDragEnter(ev)}" @dragstart="${(ev: DragEvent) => this._onDragStart(ev)}" @dragend="${(ev: DragEvent) => this._onDragEnd(ev)}" @dragover="${(ev: DragEvent) => this._onDragOver(ev)}" style="padding-left: ${level * 22}px">
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
