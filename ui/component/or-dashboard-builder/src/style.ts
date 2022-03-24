import { css, unsafeCSS } from "lit";
import {DefaultColor1, DefaultColor2, DefaultColor4, DefaultColor5} from "@openremote/core";


// language=CSS
export const style = css`

    :host {       
        --internal-or-asset-tree-header-color: var(--or-asset-tree-header-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));     
        --internal-or-asset-tree-header-text-color: var(--or-asset-tree-header-text-color, var(--or-app-color7, ${unsafeCSS(DefaultColor1)}));
        --internal-or-asset-tree-header-menu-background-color: var(--or-asset-tree-header-menu-background-color, var(--internal-or-asset-tree-header-text-color));
        --internal-or-asset-tree-header-menu-text-color: var(--or-asset-tree-header-menu-text-color, inherit);
        --internal-or-asset-tree-header-height: var(--or-asset-tree-header-height, 48px);
        --internal-or-asset-tree-background-color: var(--or-asset-tree-background-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
        --internal-or-asset-tree-text-color: var(--or-asset-tree-text-color, inherit);
        --internal-or-asset-tree-item-height: var(--or-asset-tree-item-height, 24px);
        --internal-or-asset-tree-item-padding: var(--or-asset-tree-item-padding, 10px);
        --internal-or-asset-tree-selected-background-color: var(--or-asset-tree-selected-background-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-asset-tree-selected-color: var(--or-asset-tree-selected-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
        --internal-or-asset-tree-button-color: var(--or-asset-tree-button-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
        --internal-or-asset-tree-line-color: var(--or-asset-tree-line-color, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));
            
        display: flex;
        flex-direction: column;
        height: 100%;
        width: 100%;
        background-color: var(--internal-or-asset-tree-background-color);
    }
        
    #header {
        background-color: var(--internal-or-asset-tree-header-color);
        display: flex;
        align-items: center;
        width: 100%;
        height: var(--internal-or-asset-tree-header-height);
        border-bottom: 1px solid ${unsafeCSS(DefaultColor5)};
        z-index: 1000;
        line-height: var(--internal-or-asset-tree-header-height);
        color: var(--internal-or-asset-tree-header-text-color);
        --or-icon-fill: var(--internal-or-asset-tree-header-text-color);
    }

    #title-container {
        flex: 1 0 auto;
        flex-direction: row;
        text-transform: capitalize;
        padding-left: 15px;
    }
    
    #title {
        font-weight: 500;
        font-size: 16px;
    }

    #sidebarElement, #sidebarBgElement {
        grid-column: 1;
        grid-row: 1;
    }

    #sidebar {
        display: grid;
    }

    #item {
        display: block;
        flex-basis: auto;
        align-self: auto;
        order: 0;
        border: 1px solid #E0E0E0;
        padding: 32px;
    }

    .sidebarItem {
        height: 100%;
        background: white;
        display: flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;
        border: 1px solid #E0E0E0;
        border-radius: 8px;
        box-sizing: border-box;
        flex-direction: column;
        font-size: 14px;
    }
`
