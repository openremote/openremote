import {css, html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property, queryAll} from "lit/decorators.js";
import {map} from "lit/directives/map.js";
import {when} from "lit/directives/when.js";
import {InputType} from "@openremote/or-mwc-components/or-mwc-input";
import {getContentWithMenuTemplate} from "@openremote/or-mwc-components/or-mwc-menu";
import {showSnackbar} from "@openremote/or-mwc-components/or-mwc-snackbar";
import {ListItem} from "@openremote/or-mwc-components/or-mwc-list";
import {Util} from "@openremote/core";
import {OrTreeNode} from "./or-tree-node";
import {OrTreeGroup} from "./or-tree-group";
import {moveNodesToGroupNode} from "./util";
import {OrTreeDragEvent, OrTreeSelectEvent, TreeMenuSelection, TreeMenuSorting, TreeNode} from "./model";

import "./or-tree-group";
import "./or-tree-node";

export * from "./or-tree-group";
export * from "./or-tree-node";
export * from "./model";

const styles = css`
    * {
        box-sizing: border-box;
    }

    #tree-list {
        list-style: none;
        padding: 0;
        margin: 0;
    }

    or-tree-node > or-tree-node > * {
        pointer-events: none;
    }
    
    or-tree-node {
        transition: background-color 80ms;
    }
    
    or-tree-node[drophover] {
        background: #e9ecef;
    }
    
    #tree-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0 15px;
        min-height: 48px;
        background: var(--or-app-color4, #4d9d2a);
        color: var(--or-app-color7, white);
        --or-icon-fill: var(--or-app-color7, white);
    }
    
    #tree-header-title {
        margin: 0;
        font-weight: 500;
        font-size: 16px;
    }
    
    #tree-header-actions {
        display: flex;
        align-items: center;
    }
`;

/**
 * @event {OrTreeSelectEvent} or-tree-select - Triggers upon selecting a node, and dispatches a list of the nodes selected.
 * @event {OrTreeDragEvent} or-tree-drag - Triggers upon dragging a node to a new group, and dispatches a list of dragged nodes, the group node, and the updated list of all nodes.
 */
@customElement("or-tree-menu")
export class OrTreeMenu extends LitElement {

    /**
     * List of node items in the menu.
     * Uses the TreeNode format for rendering the OrTreeNode elements.
     */
    @property({type: Array})
    public nodes: TreeNode[] = [];

    /**
     * Changes the allowed selection method within the tree.
     * Common options are `LEAF`, `SINGLE` and `MULTI`.
     */
    @property({type: String})
    public selection: TreeMenuSelection = TreeMenuSelection.LEAF;

    /**
     * Disables and enables dragging of nodes into groups.
     */
    @property({type: Boolean})
    public draggable = false;

    /**
     * Removes the header from the tree menu.
     */
    @property({type: Boolean, attribute: "no-header"})
    public noHeader = false;

    /**
     * Adjusts the title in the menu header.
     */
    @property({type: String, attribute: "menu-title"})
    public menuTitle = "Tree Menu";

    /**
     * List of options available to sort the object with
     */
    @property({type: Array, attribute: "sort-options"})
    public sortOptions?: TreeMenuSorting[];

    /**
     * Represents the selected sorting option
     */
    @property({type: String, attribute: "sort-by", reflect: true})
    public sortBy: TreeMenuSorting = TreeMenuSorting.A_TO_Z;

    /**
     * Automatically prioritizes the groups, and positions these on top.
     */
    @property({type: Boolean, attribute: "group-first"})
    public groupFirst = false;

    @queryAll("or-tree-node")
    protected _uiNodes?: NodeListOf<OrTreeNode>;

    @queryAll("or-tree-group")
    protected _uiGroups?: NodeListOf<OrTreeGroup>;

    // A Map<TreeNode, unique generated ID> to easily identify which TreeNode has been used to render a TreeNodeComponent.
    protected _treeNodeCache: Map<TreeNode, string> = new Map<TreeNode, string>;

    // Caches the last selected component for use in multi select.
    protected _lastSelectedNode?: OrTreeNode;

    static get styles() {
        return [styles];
    }

    protected willUpdate(changedProps: PropertyValues) {
        if((changedProps.has("nodes") || changedProps.has("sortBy") || changedProps.has("groupFirst")) && this.nodes) {
            this.nodes = this._sortNodes(this.nodes, this.sortBy, this.groupFirst);
        }
        // If the selection method has been changed, for example from LEAF to SINGLE, we deselect everything
        if(changedProps.has("selection") && changedProps.get("selection") && this.selection) {
            this._deselectAllNodes();
        }
        return super.willUpdate(changedProps);
    }

    render(): TemplateResult {
        return html`
            <div id="tree-container">
                ${when(!this.noHeader, () => this._getHeaderTemplate())}
                ${this._getTreeTemplate(this.nodes)}
                ${this._getErrorTemplate()}
            </div>
        `;
    }

    /* ------------------------------------------------------------- */
    //region Public functions for or-tree-menu
    /* ------------------------------------------------------------- */

    /**
     * Function that moves an array of {@link TreeNode} into another {@link TreeNode}, by adding them to their children.
     * The function takes care of removing the nodes from the former group, and makes sure no duplicates end up in the list.
     *
     * @param nodesToMove - The array of nodes that are moved into a group.
     * @param groupNode - The group node to insert nodesToMove in.
     */
    public moveNodesToGroup(nodesToMove: TreeNode[], groupNode?: TreeNode) {
        this.nodes = moveNodesToGroupNode(nodesToMove, groupNode, this.nodes);
    }

    //endregion

    /* ------------------------------------------------------------- */
    //region HTML Templates functions
    /* ------------------------------------------------------------- */

    /**
     * Returns a HTML template that displays the tree menu.
     * @param nodes - List of nodes to be rendered
     * @protected
     */
    protected _getTreeTemplate(nodes: TreeNode[]): TemplateResult {
        return html`
            <ol id="tree-list">
                ${map(nodes, node => this._getNodeTemplate(node))}
            </ol>
        `;
    }

    /**
     * Returns an HTML template for displaying a single node within a tree menu. This can both be a group or a solo node.
     * @param node - Node to be rendered
     * @param parent - Optional parent (group node) if the node is placed inside a group
     * @protected
     */
    protected _getNodeTemplate(node: TreeNode, parent?: TreeNode): TemplateResult {
        const isGroup = node.children;
        if (isGroup) {
            return this._getGroupNodeTemplate(node, parent);
        } else {
            return this._getSingleNodeTemplate(node, parent);
        }
    }

    /**
     * Returns an HTML template for displaying a single node
     * @param node - Node to be rendered
     * @param parent - Optional parent (group node) if the node is placed inside a group
     * @protected
     */
    protected _getSingleNodeTemplate(node: TreeNode, parent?: TreeNode): TemplateResult {
        const randomId = this._setTreeNodeId(node);
        return html`
            <li draggable=${this.draggable}
                @dragstart=${(ev: DragEvent) => this._onDragStart(ev, node)}
                @dragover=${(ev: DragEvent) => this._onDragOverSingleNode(ev, node, parent)}
                @dragleave=${(ev: DragEvent) => this._onDragLeaveSingleNode(ev, node, parent)}
                @drop=${(ev: DragEvent) => this._onDragDropSingleNode(ev, node, parent)}>
                <or-tree-node id=${randomId} ?selected=${node.selected} ?readonly=${node.readonly} @click="${this._onTreeNodeClick}">
                    ${this._getSingleNodeSlotTemplate(node)}
                </or-tree-node>
            </li>
        `;
    }

    /**
     * Returns an HTML template for the slot element inside <or-tree-node>.
     * It allows customization such as a prefix icon, adjusting the label, etc.
     * @param node - Node to be rendered
     * @protected
     */
    protected _getSingleNodeSlotTemplate(node: TreeNode): TemplateResult {
        return html`
            <or-icon slot="prefix" icon="flag"></or-icon>
            <span>${node.label}</span>
            <span slot="suffix"></span>
        `;
    }

    /**
     * Returns a HTML template for rendering a group node (aka a node with children)
     * @param node - Node to be rendered
     * @param parent - Optional parent (group node) if the node is placed inside a group
     * @protected
     */
    protected _getGroupNodeTemplate(node: TreeNode, parent?: TreeNode): TemplateResult {
        const leaf = this.selection === TreeMenuSelection.LEAF;
        const randomId = this._setTreeNodeId(node);
        return html`
            <li>
                <or-tree-group ?leaf=${leaf} ?expanded=${node.expanded}>
                    <or-tree-node slot="parent" id=${randomId} ?readonly=${leaf}
                                  @click=${this._onTreeGroupClick}
                                  @dragover=${(ev: DragEvent) => this._onDragOverGroup(ev, node)}
                                  @dragleave=${(ev: DragEvent) => this._onDragLeaveGroup(ev, node)}
                                  @drop=${(ev: DragEvent) => this._onDragDropGroup(ev, node)}>
                        ${this._getGroupNodeSlotTemplate(node)}
                    </or-tree-node>
                    ${map(node.children, n => this._getNodeTemplate(n, node))}
                </or-tree-group>
            </li>
        `;
    }

    /**
     * Returns an HTML template for the parent slot element inside <or-tree-group>.
     * It allows customization such as a prefix icon, adjusting the label, etc.
     * @param node - Node to be rendered
     * @protected
     */
    protected _getGroupNodeSlotTemplate(node: TreeNode): TemplateResult {
        return html`
            <or-icon slot="prefix" icon="folder"></or-icon>
            <span>${node.label}</span>
            <span slot="suffix"></span>
        `;
    }

    /**
     * Returns a HTML template for the header
     * @protected
     */
    protected _getHeaderTemplate(): TemplateResult {
        return html`
            <div id="tree-header">
                <h3 id="tree-header-title">
                    <or-translate value=${this.menuTitle}></or-translate>
                </h3>
                <div id="tree-header-actions">
                    ${this._getSortActionTemplate(this.sortBy, this.sortOptions)}
                </div>
            </div>
        `;
    }

    /**
     * Returns a HTML template for the sorting options menu.
     * @param value - The selected sorting option
     * @param options - The available sorting options
     * @protected
     */
    protected _getSortActionTemplate(value?: string, options?: TreeMenuSorting[]): TemplateResult {
        return getContentWithMenuTemplate(
            html`<or-mwc-input type=${InputType.BUTTON} icon="sort-variant"></or-mwc-input>`,
            (options || []).map(sort => ({ value: sort, text: sort } as ListItem)),
            value,
            value => this._onSortClick(String(value))
        );
    }

    /**
     * Returns a HTML template for displaying errors
     * @protected
     */
    protected _getErrorTemplate(): TemplateResult {
        return html``;
    }

    //endregion

    /* ------------------------------------------------------------- */
    //region Event callback functions
    /* ------------------------------------------------------------- */

    /**
     * HTML callback event for selecting a sort option in the dropdown menu,.
     * @param value - The new selected sort option
     * @protected
     */
    protected _onSortClick(value: string) {
        this.sortBy = value as TreeMenuSorting;
    }

    /**
     * HTML callback event for clicking on a group node. (aka a node with children)
     * Based on the configured TreeMenuSelection, it single- or multi selects the nodes.
     * @protected
     */
    protected _onTreeGroupClick(ev: PointerEvent) {
        const elem = ev.currentTarget as OrTreeNode;
        const group = elem.parentElement as OrTreeGroup;
        const select = (treeGroup: OrTreeGroup, nodeElem: OrTreeNode) => {
            treeGroup.select();
            this._lastSelectedNode = nodeElem;
            this._notifyNodesSelect()
        }

        switch (this.selection) {
            case TreeMenuSelection.LEAF: {
                return; // Group node cannot be selected when in leaf
            }
            case TreeMenuSelection.MULTI: {

                // Shift selects all nodes between the previous selected, and this one.
                if(ev.shiftKey && this._lastSelectedNode) {
                    const nodes = Array.from(this._uiNodes || []);
                    const parentNode = group.getGroupNode();
                    if(parentNode) {
                        const indexOfClickedNode = nodes.indexOf(parentNode);
                        const indexOfPreviousNode = nodes.indexOf(this._lastSelectedNode);
                        this._selectNodesBetween(nodes, indexOfClickedNode, indexOfPreviousNode);
                        return;
                    }

                // Ctrl multi selects without deselecting the previous one.
                } else if(ev.ctrlKey) {
                    select(group, elem);
                    return;
                }
                // Otherwise, select node like normal
                this._deselectAllNodes();
                select(group, elem);
                return;
            }

            case TreeMenuSelection.SINGLE: {
                this._deselectAllNodes();
                select(group, elem);
                return;
            }
        }
    }

    /**
     * HTML callback event for when a child node of the tree gets clicked on.
     * @protected
     */
    protected _onTreeNodeClick(ev: PointerEvent) {
        const node = ev.currentTarget as OrTreeNode;
        if (node) {
            switch (this.selection) {
                case TreeMenuSelection.MULTI: {

                    // Shift selects all nodes between the previous selected, and this one.
                    if(ev.shiftKey && this._lastSelectedNode) {
                        const nodes = Array.from(this._uiNodes || []);
                        const prevIndex = nodes.indexOf(this._lastSelectedNode);
                        const clickedIndex = nodes.indexOf(node);
                        if (prevIndex > -1 && clickedIndex > -1) {
                            this._selectNodesBetween(nodes, prevIndex, clickedIndex);
                            return;
                        }

                    // Ctrl multi selects without deselecting the previous one.
                    } else if(ev.ctrlKey) {
                        this._selectNode(node);
                        return;
                    }
                    // Otherwise select the node like normal
                    this._deselectAllNodes();
                    this._selectNode(node);
                    return;
                }

                default: {
                    this._deselectAllNodes();
                    this._selectNode(node);
                    return;
                }
            }
        }
    }

    //endregion

    /* ------------------------------------------------------------- */
    //region Drag-and-drop callback functions
    /* ------------------------------------------------------------- */

    /** HTML callback event for 'dragstart' (the moment when a drag gesture is started) */
    protected _onDragStart(ev: DragEvent, node: TreeNode) {
        if(ev.target) {
            ev.dataTransfer?.setData("treeNode", JSON.stringify(node));
        } else {
            ev.preventDefault();
        }
    }

    /** HTML callback event for 'dragover', so while a node is dragged over a single node */
    protected _onDragOverSingleNode(ev: DragEvent, node: TreeNode, parent?: TreeNode) {
        if(this.draggable) {
            ev.preventDefault(); // allows dropping the node on this node
            if(parent) {
                const elem = this._getUiNodeFromTree(parent);
                elem?.setAttribute("drophover", "true");
            }
        }
    }

    /** HTML callback event for 'dragover', so while a node is dragged over a group node */
    protected _onDragOverGroup(ev: DragEvent, groupNode: TreeNode) {
        if(this.draggable) {
            ev.preventDefault(); // allows dropping the node on the group
            (ev.currentTarget as HTMLElement).setAttribute("drophover", "true");
        }
    }

    /** HTML callback event for 'dragover', so while a node is dragged over a single node */
    protected _onDragLeaveSingleNode(ev: DragEvent, node: TreeNode, parent?: TreeNode) {
        if(parent && this.draggable) {
            ev.preventDefault(); // allows dropping the node on this node
            const elem = this._getUiNodeFromTree(parent);
            elem?.removeAttribute("drophover");
        }
    }

    /** HTML callback event for 'dragleave', so after a node has been dragged over a group node */
    protected _onDragLeaveGroup(ev: DragEvent, groupNode: TreeNode) {
        if(this.draggable) (ev.currentTarget as HTMLElement).removeAttribute("drophover");
    }

    /** HTML callback event for when a node is dropped onto a single node, after dragging it over */
    protected _onDragDropSingleNode(ev: DragEvent, groupNode?: TreeNode, parent?: TreeNode) {
        if(this.draggable) {
            ev.preventDefault(); // allows dropping the node on this node
            this._onDragDropGroup(ev, parent);
        }
    }

    /** HTML callback event for when a node is dropped onto a group node, after dragging it over */
    protected _onDragDropGroup(ev: DragEvent, groupNode?: TreeNode) {
        if(this.draggable) {
            ev.preventDefault();
            (ev.currentTarget as HTMLElement).removeAttribute("drophover");

            const nodesToMove: TreeNode[] = [];

            // Get the dragged element from the event payload data
            const data = ev.dataTransfer?.getData("treeNode");
            if(data) {
                const node = JSON.parse(data) as TreeNode;
                if(node) nodesToMove.push(node);
            }
            // If selection is multi, also add all selected ones
            if(this.selection === TreeMenuSelection.MULTI) {
                let selected = this._findSelectedTreeNodes();
                const anyGroupSelected = !!selected.find(n => n.children);
                if(anyGroupSelected) {
                    showSnackbar(undefined, "ruleGroupDragError")
                    return; // cannot select any group
                }
                if(selected.length > 0) {
                    this._deselectAllNodes();
                    nodesToMove.push(...selected.filter(node => !nodesToMove.find(n => JSON.stringify(n) === JSON.stringify(node))));
                }
            }
            // Finally, move the nodes
            if(nodesToMove.length > 0) {
                this._dispatchCancellableDragEvent(nodesToMove, groupNode, this.nodes).then(() => {
                    this.nodes = moveNodesToGroupNode(nodesToMove, groupNode, this.nodes);
                }).catch(ignored => {});
            }
        }
    }

    /**
     * Dispatches a cancellable tree drag event.
     */
    protected _dispatchCancellableDragEvent(nodes: TreeNode[], groupNode?: TreeNode, allNodes: TreeNode[] = []): Promise<void> {
        return new Promise((resolve, reject) => {
            const success = this.dispatchEvent(new OrTreeDragEvent(nodes, groupNode, allNodes));
            success ? resolve() : reject();
        });
    }

    //endregion

    /* ------------------------------------------------------------- */

    /**
     * Selects the node using the HTML attribute 'selected' of OrTreeNode
     * @param node - Node to be selected
     * @param notify - Boolean whether to notify the HTML parents of an or-tree-select.
     * @protected
     */
    protected _selectNode(node?: OrTreeNode, notify = true) {
        if(node) {
            if(notify) {
                const selected = [...this._findSelectedTreeNodes()];
                const treeNode = this._getTreeNodeFromTree(node);
                if(treeNode) selected.push(treeNode);
                const success = this._dispatchSelectEvent(selected); // Dispatch a "select" event, and return when cancelled by a consumer
                if(!success) return;
            }
            node.selected = true;
            this._lastSelectedNode = node;
        }
    }

    /**
     * Multi-selects the nodes between two indexes in a list of OrTreeNode.
     * @param nodes - List of nodes in the tree menu
     * @param index1 - Start index of the nodes to select
     * @param index2 - End index of the nodes to select
     * @param notify - Boolean whether to notify the HTML parents of an or-tree-select.
     * @protected
     */
    protected _selectNodesBetween(nodes: OrTreeNode[], index1: number, index2: number, notify = true) {
        const selectedNodes: OrTreeNode[] = [];
        if(index1 < index2) {
            for(let x = index1; x <= index2; x++) {
                selectedNodes.push(nodes[x]);
            }
        } else if(index1 > index2) {
            for(let x = index2; x <= index1; x++) {
                selectedNodes.push(nodes[x]);
            }
        }
        // Dispatch a "select" event with the new nodes. If cancelled, we will not select the nodes.
        if(notify) {
            const nodes = selectedNodes.map(n => this._getTreeNodeFromTree(n)).filter(n => n) as TreeNode[];
            const success = this._notifyNodesSelect(nodes);
            if(!success) return;
        }
        // Select the nodes
        selectedNodes.forEach((node) => this._selectNode(node));
    }

    /**
     * Function that notifies parent HTMLElements that a tree node got selected.
     * It dispatches the OrTreeSelectEvent, which includes a list of the selected nodes.
     * @returns a 'success' boolean whether the event was cancelled or not
     * @protected
     */
    protected async _notifyNodesSelect(selectedNodes?: TreeNode[]) {
        await this.getUpdateComplete(); // await render to include the latest changes.
        if(!selectedNodes) {
            selectedNodes = this._findSelectedTreeNodes();
        }
        return this._dispatchSelectEvent(selectedNodes);
    }

    /**
     * Utility function to detect the selected tree nodes u
     * @param uiNodes
     * @param cache
     * @protected
     */
    protected _findSelectedTreeNodes(uiNodes = Array.from(this._uiNodes || []), cache = this._treeNodeCache): TreeNode[] {

        // Get the list of selected TreeNodeComponent elements in the UI.
        const selectedUiNodes = uiNodes.filter(n => n.selected);

        // Get the list of cached generated IDs (for tracking which TreeNode belongs to which TreeNodeComponent)
        const treeNodeEntries = Array.from(cache.entries());

        // Find the generated IDs in the list of TreeNodeComponents, and compare the element ID
       return selectedUiNodes.map(component => treeNodeEntries
            .find(v => v[1] === component.id))
            .map(x => x?.[0])
            .filter(n => n !== undefined) as TreeNode[];
    }

    /**
     * Utility function that gets a {@link TreeNode} based on an {@link OrTreeNode} HTML element in the menu.
     * @param uiNode - The tree node HTML element
     * @param cache - Optional cache to get the TreeNode from
     * @protected
     */
    protected _getTreeNodeFromTree(uiNode: OrTreeNode, cache = this._treeNodeCache): TreeNode | undefined {
        const treeNodeEntries = Array.from(cache.entries()); // Get the list of cached generated IDs (for tracking which TreeNode belongs to which TreeNodeComponent)
        return treeNodeEntries.find(entry => entry[1] === uiNode.id)?.[0];
    }

    /**
     * Utility function that gets an {@link OrTreeNode} HTML element based on the {@link TreeNode} input object.
     * @param treeNode - The tree node object
     * @param cache - Optional cache to get the HTML element from
     * @protected
     */
    protected _getUiNodeFromTree(treeNode: TreeNode, cache = this._treeNodeCache): OrTreeNode | undefined {
        const treeNodeEntries = Array.from(cache.entries()); // Get the list of cached generated IDs (for tracking which TreeNode belongs to which TreeNodeComponent)
        const treeNodeJSON = JSON.stringify(treeNode);

        const elementId = treeNodeEntries.find(entry => JSON.stringify(entry[0]) === treeNodeJSON)?.[1];
        return elementId ? this.shadowRoot!.getElementById(elementId) as OrTreeNode : undefined;
    }

    /**
     * Utility function for sending a "select" event, so consumers of this component are aware a new node has been selected.
     * @param selectedNodes - List of selected nodes to include in the event payload.
     * @returns a 'success' boolean whether the event was cancelled or not.
     * @protected
     */
    protected _dispatchSelectEvent(selectedNodes?: TreeNode[]): boolean {
        return this.dispatchEvent(new OrTreeSelectEvent(selectedNodes || []));
    }

    /**
     * Utility function that deselects all tree nodes.
     * @protected
     */
    protected _deselectAllNodes() {
        (this._uiGroups || []).forEach(uiGroup => uiGroup.deselect());
        (this._uiNodes || []).forEach(uiNode => uiNode.selected = false);
    }

    /**
     * Utility function that sorts the list of {@link nodes} based on the given {@link sortBy} method.
     * @param nodes - List of nodes to be sorted
     * @param sortBy - Sorting option
     * @param groupFirst - Whether to prioritize group nodes, and place them on the top of the list.
     */
    protected _sortNodes(nodes: TreeNode[], sortBy?: TreeMenuSorting, groupFirst = false): TreeNode[] {
        console.debug("Sorting nodes in the tree menu...");

        const grouped = nodes.filter(node => node.children !== undefined);

        // TODO: Apply recursive sorting if nested deeper inside the tree
        grouped.forEach(g => g.children?.sort(this._getSortFunction(sortBy)));

        // Optionally, prioritize group nodes, and place all groups on top of the menu
        if(groupFirst) {
            const ungrouped = nodes.filter(node => node.children === undefined);
            grouped.sort(this._getSortFunction(sortBy));
            ungrouped.sort(this._getSortFunction(sortBy));
            return [...grouped, ...ungrouped];
        }

        return nodes.sort(this._getSortFunction(sortBy));
    }

    /**
     * Function that caches a random ID into a key-value storage, linking the TreeNode with a generated ID.
     * This generated ID can be used somewhere else, for example in an HTMLElement ID as a unique identifier.
     * @protected
     */
    protected _setTreeNodeId(node: TreeNode, randomId = Math.random().toString(36).substring(2, 11)): string {
        this._treeNodeCache.set(node, randomId);
        return randomId;
    }

    /**
     * Function for retrieving the sorting for TreeNodes based on a sortBy parameter.
     * The sortBy parameter represents a key in the TreeNode object like 'label'.
     * @protected
     */
    protected _getSortFunction(sortBy?: TreeMenuSorting): (a: TreeNode, b: TreeNode) => number {
        switch (sortBy) {
            case TreeMenuSorting.Z_TO_A:
                return (a, b) => b.label.localeCompare(a.label);
            default:
                return Util.sortByString(node => node.label);
        }
    }
}
