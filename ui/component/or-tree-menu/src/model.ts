/**
 * The TreeNode model for constructing a node within the tree.
 */
export interface TreeNode {
    id: string;
    children?: TreeNode[];
    label: string;
    hidden?: boolean;
    readonly?: boolean;
    selected?: boolean; // Only acts as initial state
    expandable?: boolean;
    expanded?: boolean;
}

/**
 * List of possible options for tree menu selection.
 * The order within the enum is based on restrictiveness.
 *
 * - **LEAF** - only allows a single node to be selected, and forbids the selection of any parent node.
 * - **SINGLE** - only allows a single node to be selected, but does allow the selection of the parent node.
 * - **MULTI** - allows selecting multiple nodes using keyboard controls. (control and shift)
 */
export enum TreeMenuSelection {
    LEAF = "leaf", SINGLE = "single", MULTI = "multi"
}

/**
 * List of sorting options in the header of the tree menu.
 * By default, we include alphabetically sorting, but can be expanded by class inheritors.
 */
export enum TreeMenuSorting {
    A_TO_Z = "a_to_z", Z_TO_A = "z_to_a"
}

/**
 * Model for the SELECT event that {@link OrTreeMenu} can dispatch.
 * Once a node is selected, a list of all the selected nodes will be shared with the consumer elements.
 */
export class OrTreeSelectEvent extends CustomEvent<TreeNode[]> {

    public static readonly NAME = "or-tree-select";

    constructor(nodes: TreeNode[]) {
        super(OrTreeSelectEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: nodes
        });
    }
}

/**
 * Payload object of the {@link OrTreeDragEvent}.
 */
export interface OrTreeGroupEventDetail {
    nodes: TreeNode[];
    groupNode?: TreeNode;
    newNodes: TreeNode[];
}

/**
 * Model for the DRAG event that {@link OrTreeMenu} can dispatch.
 * Once a node is dragged into (or outside) a group, we send details to consumer elements.
 */
export class OrTreeDragEvent extends CustomEvent<OrTreeGroupEventDetail> {

    public static readonly NAME = "or-tree-drag";

    constructor(nodes: TreeNode[], groupNode?: TreeNode, newNodes: TreeNode[] = []) {
        super(OrTreeDragEvent.NAME, {
            bubbles: true,
            composed: true,
            cancelable: true,
            detail: {
                nodes: nodes,
                groupNode: groupNode,
                newNodes: newNodes
            } as OrTreeGroupEventDetail
        });
    }
}
