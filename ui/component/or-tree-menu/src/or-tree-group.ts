import {css, html, LitElement, PropertyValues, TemplateResult} from "lit";
import {customElement, property, query, queryAssignedElements} from "lit/decorators.js";
import { when } from "lit/directives/when.js";
import {OrTreeNode} from "./or-tree-node";

const getStyles = () => css`
    * {
        box-sizing: border-box;
    }
    
    :host {
        position: relative;
    }

    ol {
        list-style: none;
        padding: 0;
        margin: 0;
    }
    
    ::slotted(*[slot="parent"]) {
        cursor: pointer;
    }
    
    #chevron {
        position: absolute;
        height: 44px;
        display: flex;
        align-items: center;
        aspect-ratio: 1/1.25;
        padding-left: 2px;
    }

    :host(:not([readonly])) > #chevron {
        cursor: pointer;
    }
`;

/**
 * @slot - Default slot for child nodes within the group
 * @slot parent - Slot for inserting a parent node
 * @csspart chevron - The chevron icon element for expanding/collapsing the group.
 */
@customElement("or-tree-group")
export class OrTreeGroup extends LitElement {

    /**
     * Determines the visibility of child nodes.
     * Setting this to `false` hides them, and acts as a 'collapsed' state.
     */
    @property({type: Boolean, reflect: true})
    public expanded = false;

    /**
     * Only allows child nodes to be selected, making the expander (parent node) readonly.
     * If this is set to false, only the chevron icon can be used to collapse/expand the list.
     */
    @property({type: Boolean})
    public readonly leaf = false;

    /**
     * Makes the group readonly
     */
    @property({type: Boolean})
    public readonly readonly = false;

    @queryAssignedElements({slot: undefined})
    protected _childNodes?: Array<HTMLLIElement>;

    @queryAssignedElements({slot: "parent"})
    protected _parentNodes?: Array<OrTreeNode>;

    /** A click event listener on the component, used for selecting, expanding, and collapsing the group */
    protected _slotClickListener = (e: MouseEvent) => this.leaf ? this._onExpandToggle(e) : null;

    /** A click event listener on the chevron, used for expanding/collapsing the group */
    protected _chevronClickListener = (e: MouseEvent) => this._onExpandToggle(e);

    static get styles() {
        return [getStyles()];
    }

    /** Selects the group (parent) node. */
    public select() {
        const groupNode = this.getGroupNode();
        if(!this.leaf && groupNode) {
            groupNode.selected = true;
        }
    }

    /** Selects the group node itself, and all children nodes within that group. */
    public selectAll() {
        this.select();
        this.getChildNodes().forEach(node => node.selected = true);
    }

    /** Deselects the group (parent) node. */
    public deselect() {
        const groupNode = this.getGroupNode();
        if(groupNode) {
            groupNode.selected = false;
        }
    }

    /** Deselects the group node itself, and all children nodes within that group. */
    public deselectAll() {
        this.deselect();
        this.getChildNodes().forEach(node => node.selected = false);
    }

    /** Returns the group (parent) node ({@link OrTreeNode}) using a query selector. */
    public getGroupNode(): OrTreeNode | undefined {
        return this._parentNodes?.[0];
    }

    /** Returns a list of all children nodes ({@link OrTreeNode}) within the group, using a query selector. */
    public getChildNodes(): OrTreeNode[] {
        return Array.from(this._childNodes || [])
            .map(n => n.querySelector('or-tree-node') as OrTreeNode | null)
            .filter(n => n != null) as OrTreeNode[];
    }

    firstUpdated(changedProps: PropertyValues) {
        this._parentNodes?.forEach(elem => {
            elem.addEventListener("click", this._slotClickListener);
        });
        return super.firstUpdated(changedProps);
    }

    disconnectedCallback() {
        this._parentNodes?.forEach(elem => {
            elem.removeEventListener("click", this._slotClickListener);
        });
        super.disconnectedCallback();
    }

    render(): TemplateResult {
        return html`
            ${when(!this.readonly, () => this._getIconTemplate(this.expanded))}
            <slot name="parent"></slot>
            <ol ?hidden=${!this.expanded}>
                <slot @slotchange=${this._onSlotChange}></slot>
            </ol>
        `;
    }

    /**
     * Returns an HTML template that represents the icon for this group.
     * @param expanded State of the group
     * @protected
     */
    protected _getIconTemplate(expanded = false): TemplateResult {
        return html`
            <or-icon id="chevron" part="chevron" icon="${expanded ? "chevron-down" : "chevron-right"}"
                     @click="${this._chevronClickListener}"
            ></or-icon>
        `;
    }

    /**
     * Function that expands/collapses the group, changing the visibility of the child nodes.
     * @protected
     */
    protected _onExpandToggle(_ev: MouseEvent) {
        this.expanded = !this.expanded;
    }

    /**
     * Event listener for 'slotchange' of the default slot.
     * Normally triggers when <or-tree-node> elements are added or removed.
     * @protected
     */
    protected _onSlotChange(_ev: Event) {
        this._applyIndentToChildren();
    }

    /**
     * Function that applies CSS to TreeNode based on the group indentation.
     * It loops through all parent elements, and detects the amount of or-tree-group elements it is in.
     * The more nested in the tree, the more lefthanded padding is applied.
     * @param children Node elements to apply padding to.
     * @protected
     */
    protected _applyIndentToChildren(children = this.getChildNodes()) {
        const countGroups = (elem: HTMLElement | null) => {
            let count = 0;
            while(elem) {
                if(elem.tagName.toLowerCase() === "or-tree-group") {
                    count++;
                }
                elem = elem.parentElement;
            }
            return count;
        };
        children.forEach(child => {
            let groupAmount = countGroups(child);
            if(child.slot === "parent") {
                groupAmount--; // Parent slot is the group itself, so remove 1
            }
            // Apply indent as padding to the node
            child.style.setProperty("--or-tree-node-indent", `${24 + (groupAmount * 12)}px`);
        });
    }
}
