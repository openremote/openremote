import {css, html, LitElement, TemplateResult} from "lit";
import {customElement, property} from "lit/decorators.js";

const styles = css`
    * {
        box-sizing: border-box;
    }
    :host {
        width: 100%;
        height: var(--or-tree-node-height, 44px);
        padding: 6px 12px 6px var(--or-tree-node-indent, 24px);
        background: var(--or-tree-node-background, transparent);
        border-left: 4px solid transparent;
        display: flex;
        align-items: center;
        gap: var(--or-tree-node-gap, 8px);
        overflow: hidden;
        user-select: none;
        --or-icon-width: 20px;
    }

    :host(:not([readonly])) {
        cursor: pointer;
    }
    
    :host([readonly]) {
        cursor: not-allowed;
    }

    :host(:not([readonly]):hover) {
        background: var(--or-tree-node-backgrond--hovered, #f8f9fa);
    }
    
    :host([selected]) {
        background: var(--or-tree-node-background--selected, #f1f3f5);
        border-left: 4px solid var(--or-tree-node-color--selected, var(--or-app-color4, #4d9d2a));
    }

    ::slotted(*:not([slot])) {
        flex: 1;
    }
`;

/**
 * @slot prefix - Appends elements to the left hand side of the node, commonly used for icons.
 * @slot - Default slot for the main content, commonly used for text.
 * @slot suffix - Appends elements to the right hand side of the node.
 *
 * @cssprop --or-tree-node-height - Controls the height of the node
 * @cssprop --or-tree-node-indent - Controls the left padding of the node
 *
 * @cssprop --or-tree-node-background - Sets the default background
 * @cssprop --or-tree-node-background--hovered - Sets the background while hovering
 * @cssprop --or-tree-node-background--selected - Sets the background when selected
 *
 * @cssprop --or-tree-node-color--selected - Sets the primary color of the node when selected
 */
@customElement("or-tree-node")
export class OrTreeNode extends LitElement {

    /**
     * HTML attribute that only applies CSS, showing this node cannot be interacted with.
     */
    @property({type: Boolean, reflect: true})
    public readonly = false;

    /**
     * HTML attribute that only applies CSS, marking the node as 'selected'.
     */
    @property({type: Boolean, reflect: true})
    public selected = false;

    static get styles() {
        return [styles];
    }

    protected render(): TemplateResult {
        return html`
            <slot name="prefix"></slot>
            <slot></slot>
            <slot name="suffix"></slot>
        `;
    }
}
