import {css, unsafeCSS} from "lit-element";
import {DefaultColor1, DefaultColor2, DefaultColor5, DefaultColor3, DefaultColor4, DefaultBoxShadow, DefaultDisabledOpacity,
    DefaultBoxShadowBottom} from "@openremote/core";
import mdi from "@openremote/or-icon/dist/mdi-icons";

// language=CSS
export const style = css`

    :host {
        --internal-or-asset-viewer-background-color: var(--or-asset-viewer-background-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-asset-viewer-grid-template-columns: var(--or-asset-viewer-grid-template-columns, repeat(12, 1fr));
        --internal-or-asset-viewer-grid-template-rows: var(--or-asset-viewer-grid-template-rows, repeat(auto-fill, 1fr));
        --internal-or-asset-viewer-grid-template-auto-rows: var(--or-asset-viewer-grid-template-auto-rows, minmax(min-content, 1fr));
        --internal-or-asset-viewer-panel-margin: var(--or-asset-viewer-panel-margin, 15px);
        --internal-or-asset-viewer-panel-padding: var(--or-asset-viewer-panel-padding, 20px);
        --internal-or-asset-viewer-text-color: var(--or-asset-viewer-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));       
        --internal-or-asset-viewer-title-text-color: var(--or-asset-viewer-title-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));       
        --internal-or-asset-viewer-panel-color: var(--or-asset-viewer-panel-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
        --internal-or-asset-viewer-line-color: var(--or-asset-viewer-line-color, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));
        --internal-or-asset-viewer-info-columm-start: var(--or-asset-viewer-info-columm-start, 1); 
        --internal-or-asset-viewer-info-columm-end: var(--or-asset-viewer-info-columm-end, 7);
        --internal-or-asset-viewer-info-row-start: var(--or-asset-viewer-info-row-start, 1); 
        --internal-or-asset-viewer-info-row-end: var(--or-asset-viewer-info-row-end, 2);
        --internal-or-asset-viewer-info-height: var(--or-asset-viewer-info-height, unset);
        --internal-or-asset-viewer-info-min-height: var(--or-asset-viewer-info-min-height, unset);
        --internal-or-asset-viewer-location-columm-start: var(--or-asset-viewer-location-columm-start, 7); 
        --internal-or-asset-viewer-location-columm-end: var(--or-asset-viewer-location-columm-end, 13);
        --internal-or-asset-viewer-location-row-start: var(--or-asset-viewer-location-row-start, 1); 
        --internal-or-asset-viewer-location-row-end: var(--or-asset-viewer-location-row-end, 3);
        --internal-or-asset-viewer-location-height: var(--or-asset-viewer-location-height, unset);
        --internal-or-asset-viewer-location-min-height: var(--or-asset-viewer-location-min-height, unset);
        --internal-or-asset-viewer-attributes-columm-start: var(--or-asset-viewer-attributes-columm-start, 1); 
        --internal-or-asset-viewer-attributes-columm-end: var(--or-asset-viewer-attributes-columm-end, 7);
        --internal-or-asset-viewer-attributes-row-start: var(--or-asset-viewer-attributes-row-start, 2); 
        --internal-or-asset-viewer-attributes-row-end: var(--or-asset-viewer-attributes-row-end, 4);
        --internal-or-asset-viewer-attributes-height: var(--or-asset-viewer-attributes-height, unset);
        --internal-or-asset-viewer-attributes-min-height: var(--or-asset-viewer-attributes-min-height, unset);
        --internal-or-asset-viewer-history-columm-start: var(--or-asset-viewer-history-columm-start, 7); 
        --internal-or-asset-viewer-history-columm-end: var(--or-asset-viewer-history-columm-end, 13);
        --internal-or-asset-viewer-history-row-start: var(--or-asset-viewer-history-row-start, 3); 
        --internal-or-asset-viewer-history-row-end: var(--or-asset-viewer-history-row-end, 4);
        --internal-or-asset-viewer-history-height: var(--or-asset-viewer-history-height, unset);
        --internal-or-asset-viewer-history-min-height: var(--or-asset-viewer-history-min-height, unset);
        
        height: 100%;
        width: 100%;
        background-color: var(--internal-or-asset-viewer-background-color);
    }
   
    *[hidden] {
        display: none;
    }
    
    #container {
        box-sizing: border-box;
        padding: var(--internal-or-asset-viewer-panel-margin);
        height: 100%;
        display: grid;
        grid-template-columns: var(--internal-or-asset-viewer-grid-template-columns);
        grid-template-rows: var(--internal-or-asset-viewer-grid-template-rows);
        grid-auto-rows: var(--internal-or-asset-viewer-grid-template-auto-rows);  
        -webkit-animation: fadein 0.3s; /* Safari, Chrome and Opera > 12.1 */
        -moz-animation: fadein 0.3s; /* Firefox < 16 */
        -ms-animation: fadein 0.3s; /* Internet Explorer */
        -o-animation: fadein 0.3s; /* Opera < 12.1 */
        animation: fadein 0.3s;
    }

    @keyframes fadein {
        from { opacity: 0; }
        to   { opacity: 1; }
    }

    /* Firefox < 16 */
    @-moz-keyframes fadein {
        from { opacity: 0; }
        to   { opacity: 1; }
    }

    /* Safari, Chrome and Opera > 12.1 */
    @-webkit-keyframes fadein {
        from { opacity: 0; }
        to   { opacity: 1; }
    }

    /* Internet Explorer */
    @-ms-keyframes fadein {
        from { opacity: 0; }
        to   { opacity: 1; }
    }

    /* Opera < 12.1 */
    @-o-keyframes fadein {
        from { opacity: 0; }
        to   { opacity: 1; }
    }
    
    .panel {
        display: flex;
        flex-direction: column;
        background-color: var(--internal-or-asset-viewer-panel-color);
        padding: var(--internal-or-asset-viewer-panel-padding);
        margin: var(--internal-or-asset-viewer-panel-margin);
        box-shadow: ${unsafeCSS(DefaultBoxShadow)};
    }
    
    .panel-content-wrapper {
        height: 100%;
        min-height: 0;
    }
    
    .panel-content {
        display: flex;
        flex-wrap: wrap;
        height: 100%;
    }
        
    .panel-title {
        text-transform: uppercase;
        font-weight: bolder;
        color: var(--internal-or-asset-viewer-title-text-color);
        margin-bottom: 15px;
        flex: 0 0 auto;
    }
   
    .field {
        margin: 10px 0;
        width: 100%;
        flex: 0 0 auto;
        min-height: 50px;
    }
    
    .field > * {
        width: 100%;
        box-sizing: border-box;
    }

    #info-panel {
        grid-column-start: var(--internal-or-asset-viewer-info-columm-start);
        grid-column-end: var(--internal-or-asset-viewer-info-columm-end);
        grid-row-start: var(--internal-or-asset-viewer-info-row-start);
        grid-row-end: var(--internal-or-asset-viewer-info-row-end);
        height: var(--internal-or-asset-viewer-info-height);
        min-height: var(--internal-or-asset-viewer-info-min-height);
    }
    
    #location-panel {
        grid-column-start: var(--internal-or-asset-viewer-location-columm-start);
        grid-column-end: var(--internal-or-asset-viewer-location-columm-end);
        grid-row-start: var(--internal-or-asset-viewer-location-row-start);
        grid-row-end: var(--internal-or-asset-viewer-location-row-end);
        height: var(--internal-or-asset-viewer-location-height);
        min-height: var(--internal-or-asset-viewer-location-min-height);
    }
    
    #attributes-panel {
        grid-column-start: var(--internal-or-asset-viewer-attributes-columm-start);
        grid-column-end: var(--internal-or-asset-viewer-attributes-columm-end);
        grid-row-start: var(--internal-or-asset-viewer-attributes-row-start);
        grid-row-end: var(--internal-or-asset-viewer-attributes-row-end);
        height: var(--internal-or-asset-viewer-attributes-height);
        min-height: var(--internal-or-asset-viewer-attributes-min-height);
    }
        
    #history-panel {
        grid-column-start: var(--internal-or-asset-viewer-history-columm-start);
        grid-column-end: var(--internal-or-asset-viewer-history-columm-end);
        grid-row-start: var(--internal-or-asset-viewer-history-row-start);
        grid-row-end: var(--internal-or-asset-viewer-history-row-end);
        height: var(--internal-or-asset-viewer-history-height);
        min-height: var(--internal-or-asset-viewer-history-min-height);
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
        text-transform: uppercase;
    }
    
    .modal-btns .btn + .btn {
        margin-left: 15px;
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
        border-left: 6px solid transparent;
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
        display: flex;
        justify-content: center;
        align-items: center;
        text-align: center;
        height: 100%;
    }    
`;