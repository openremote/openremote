import {css, unsafeCSS} from "lit-element";
import {DefaultColor1, DefaultColor2, DefaultColor5, DefaultColor3, DefaultColor4, DefaultColor7, DefaultBoxShadow, DefaultDisabledOpacity,
    DefaultBoxShadowBottom} from "@openremote/core";
import mdi from "@openremote/or-icon/dist/mdi-icons";

// language=CSS
export const style = css`

    :host {       
        --internal-or-asset-tree-header-color: var(--or-asset-tree-header-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));     
        --internal-or-asset-tree-header-text-color: var(--or-asset-tree-header-text-color, var(--or-app-color7, ${unsafeCSS(DefaultColor7)}));
        --internal-or-asset-tree-header-menu-background-color: var(--or-asset-tree-header-menu-background-color, var(--internal-or-asset-tree-header-text-color));
        --internal-or-asset-tree-header-menu-text-color: var(--or-asset-tree-header-menu-text-color, inherit);
        --internal-or-asset-tree-header-height: var(--or-asset-tree-header-height, 70px);
        --internal-or-asset-tree-background-color: var(--or-asset-tree-background-color, var(--or-app-color7, ${unsafeCSS(DefaultColor7)}));
        --internal-or-asset-tree-text-color: var(--or-asset-tree-text-color, inherit);
        --internal-or-asset-tree-item-height: var(--or-asset-tree-item-height, 28px);
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
    
    *[hidden] {
        display: none;
    }
    
    button {
        background-color: var(--internal-or-asset-tree-button-color);
        color: var(--internal-or-asset-tree-background-color);
        --or-icon-width: 26px;
        --or-icon-height: 26px;
        --or-icon-fill: var(--internal-or-asset-tree-background-color);
        margin: 0 8px;
        border: none;
        padding: 0;
        display: inline-block;
        cursor: pointer;
    }

    button:focus {
        outline: 0;
    }
    
    #header {
        background-color: var(--internal-or-asset-tree-header-color);
        display: flex;
        width: 100%;        
        height: var(--internal-or-asset-tree-header-height);
        border-bottom: 1px solid ${unsafeCSS(DefaultColor5)};
        -webkit-box-shadow: ${unsafeCSS(DefaultBoxShadowBottom)};
        -moz-box-shadow: ${unsafeCSS(DefaultBoxShadowBottom)};
        box-shadow: ${unsafeCSS(DefaultBoxShadowBottom)};
        z-index: 1000;
    }
    
    #header > * {
        line-height: var(--internal-or-asset-tree-header-height);
    }
   
    #title-container {
        flex: 1 0 auto;
        flex-direction: row;
        text-transform: capitalize;
        padding-left: 30px;
        
        --or-select-text-color: var(--internal-or-asset-tree-header-text-color);
        --or-select-option-text-color: var(--internal-or-asset-tree-header-menu-text-color);
        --or-select-option-background-color: var(--internal-or-asset-tree-header-menu-background-color);            
        --or-select-background-color: none;
        --or-select-border-color: none;
        --or-select-padding: 5px 20px 5px 5px;
    }
    
    #title {
        font-weight: bold;
        font-size: 20px;
        color: var(--internal-or-asset-tree-header-text-color);
    }
    
    #realm-picker {
        outline: none;
        margin-left: 5px;
        text-transform: none;
        font-size: 14px;
    }
    
    #header-btns {
        display: flex;
        flex-direction: row;
        padding-right: 10px;
    }
    
    .modal-container {
        position: relative;
        line-height: normal;
    }
    
    .modal {
        position: absolute;
        height: 0;
        overflow: hidden;
    }
    
    .modal[data-visible] {
        transition: all 0.5s ease-in;
        height: auto;
        overflow: visible;
    }
    
    .modal-content {
        border: 1px solid var(--internal-or-asset-tree-line-color);
        box-shadow: ${unsafeCSS(DefaultBoxShadow)};
        -moz-box-shadow: ${unsafeCSS(DefaultBoxShadow)};
        -webkit-box-shadow: ${unsafeCSS(DefaultBoxShadow)};
        color: var(--internal-or-asset-tree-header-menu-text-color);
        background-color: var(--internal-or-asset-tree-header-menu-background-color);
        padding: 20px 25px 10px 25px;
    }

    .modal-content > ul {
        list-style-type: none;
        padding: 0;
        margin-bottom: 0;
        white-space: nowrap;
    }
    
    .modal-content > ul > li {
        font-weight: bolder;
        line-height: 40px;
        cursor: pointer;
    }
    
    .modal-content > ul > li > or-icon {
        visibility: hidden;
        width: 18px;
        height: 18px;
        margin-right: 10px;
        --or-icon-fill: var(--internal-or-asset-tree-header-color);
        --or-icon-stroke: var(--internal-or-asset-tree-header-color);
        --or-icon-stroke-width: 2px;
    }
    
    .modal-content > ul > li[data-selected] > or-icon {
        visibility: visible;
    }
    
    .modal-content > h2 {
        opacity: 0.6;
        font-size: 0.9em;
        margin: 0;
    }
    
    .modal-btns {
        text-align: right;
    }
    
    .modal-btns .btn {
        font-weight: bolder;
        cursor: pointer;
    }
    
    .modal-btns .btn + .btn {
        margin-left: 30px;
    }    
            
    #sort-menu {
        left: -173px;
        top: var(--internal-or-asset-tree-header-height);
    }
    
    #delete-not-allowed-modal {
        left: -400px;
        top: var(--internal-or-asset-tree-header-height);
    }
    
    #delete-confirm-modal {
        left: -300px;
        top: var(--internal-or-asset-tree-header-height);
    }
    
    #list-container {
        flex: 1 0 auto;
        overflow: auto;
    }
    
    #list {
        margin: 0;
        color: var(--internal-or-asset-tree-text-color);
        padding: 15px 0;
        font-weight: bolder;
    }
    
    #list, ol {
        list-style-type: none;
    }
        
    li ol {
        padding: 0;
    }
    
    #list li:not([data-expanded]) > ol {
        display: none;
    }
    
    #list li[data-selected] > .node-container, #list li > .node-container:hover {
        background-color: var(--internal-or-asset-tree-selected-background-color);
    }
    
    #list li[data-selected] > .node-container {
        border-left-color: var(--internal-or-asset-tree-selected-color);
    }
            
    .node-container {
        display: flex;
        flex-direction: row;
        border-left: 4px solid transparent;
        user-select: none;
        cursor: pointer;
        height: var(--internal-or-asset-tree-item-height);
        line-height: var(--internal-or-asset-tree-item-height);
        padding-top: var(--internal-or-asset-tree-item-padding);
        padding-bottom: var(--internal-or-asset-tree-item-padding);
    }

    .node-container > * {
        flex: 0 0 auto;
    }
    
    .expander {
        width: 26px;
        height: 100%;
        display: inline-block;
        background-repeat: no-repeat;                
        background-size: 100%;
        background-position: center;
    }
    
    .expander[data-expandable] {
        background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath d='${unsafeCSS(mdi.icons["chevron-right"])}'/%3E%3C/svg%3E");
    }
    
    li[data-expanded] > .node-container .expander {
        background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath d='${unsafeCSS(mdi.icons["chevron-down"])}'/%3E%3C/svg%3E") !important;
    }
    
    .node-name {
        margin: -4px 0;
        flex: 1 0 auto;
        display: flex;
        align-items: center;
    }
    
    .node-name > span {
        vertical-align: middle;
    }
    
    .node-name > or-icon {
        --or-icon-width: var(--internal-or-asset-tree-item-height);
        --or-icon-height: var(--internal-or-asset-tree-item-height);
        margin-right: 10px;
    }
    
    #loading {
        flex: 1 0 auto;
        display: inline-flex;
        align-items: center;
        text-align: center;
        margin: 0 auto;
    }    

`;