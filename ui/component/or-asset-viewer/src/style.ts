import {css, unsafeCSS} from "lit-element";
import {DefaultBoxShadow, DefaultColor1, DefaultColor2, DefaultColor3, DefaultColor5} from "@openremote/core";

// language=CSS
export const style = css`

    :host {
        --internal-or-asset-viewer-background-color: var(--or-asset-viewer-background-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-asset-viewer-panel-margin: var(--or-asset-viewer-panel-margin, 20px);
        --internal-or-asset-viewer-panel-padding: var(--or-asset-viewer-panel-padding, 24px);
        --internal-or-asset-viewer-text-color: var(--or-asset-viewer-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-asset-viewer-title-text-color: var(--or-asset-viewer-title-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));       
        --internal-or-asset-viewer-panel-color: var(--or-asset-viewer-panel-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
        --internal-or-asset-viewer-line-color: var(--or-asset-viewer-line-color, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));
        
        height: 100%;
        width: 100%;
        background-color: var(--internal-or-asset-viewer-background-color);
    }
   
    *[hidden] {
        display: none;
    }
    
    #container {
        box-sizing: border-box;
        padding: 20px 30px;
        column-gap: var(--internal-or-asset-viewer-panel-margin);
        row-gap: var(--internal-or-asset-viewer-panel-margin);  
        height: 100%;
        display: grid;
        overflow-y: auto;
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

    #asset-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        grid-column-start: 1;
        grid-column-end: 13;
        grid-row-start: 1;
        grid-row-end: 2;
    }

    #asset-header > .title {
        flex: 1 0 auto;
        font-size: 18px;
        font-weight: bold;
    }

    #asset-header .title > or-icon {
        margin-right: 10px;
    }
    
    #asset-header > .created {
        text-align: right;
        flex: 1 0 auto;
        font-size: 12px;
        font-color: #ccc;
    }
    
    .panel {
        display: flex;
        flex-direction: column;
        background-color: var(--internal-or-asset-viewer-panel-color);        
        margin: 0;
        padding: var(--internal-or-asset-viewer-panel-padding);
        border: 1px solid #e5e5e5;
        border-radius: 5px;
    }
    
    .panel-content-wrapper {
        flex: 1;
        height: 100%;
    }
    
    .panel-content {
        display: flex;
        flex-wrap: wrap;
        height: 100%;
    }
        
    .panel-title {
        text-transform: uppercase;
        font-weight: bolder;
        line-height: 1em;
        color: var(--internal-or-asset-viewer-title-text-color);
        margin-bottom: 12px;
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
    
    .msg {
        display: flex;
        justify-content: center;
        align-items: center;
        text-align: center;
        height: 100%;
    }
    
    /* TODO move this to mobilePanelStyle */
    @media screen and (max-width: 768px) {
        .panel,
        #asset-header {
            grid-area: auto!important;
        }
        
        #container { 
            grid-template-columns: 1fr!important;
        }
    }
`;