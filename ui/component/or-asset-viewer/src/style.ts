import {css, unsafeCSS} from "lit-element";
import {DefaultColor1, DefaultColor2, DefaultColor3, DefaultColor5, DefaultColor4, DefaultHeaderHeight} from "@openremote/core";

// language=CSS
export const panelStyles = css`
    .panel {
        background-color: var(--internal-or-asset-viewer-panel-color);
        border: 1px solid #e5e5e5;
        border-radius: 5px;
        max-width: 100%;
        position: relative;
    }

    .panel-content-wrapper {
        padding: var(--internal-or-asset-viewer-panel-padding);
    }

    .panel-content {
        display: flex;
        flex-wrap: wrap;
    }

    .panel-title {
        text-transform: uppercase;
        font-weight: bolder;
        line-height: 1em;
        color: var(--internal-or-asset-viewer-title-text-color);
        margin-bottom: 20px;
        flex: 0 0 auto;
        letter-spacing: 0.025em;
    }

    .field {
        margin: 10px 0;
        width: 100%;
        flex: 0 0 auto;
    }

    .field > * {
        width: 100%;
        box-sizing: border-box;
    }

    .panel-content > :first-child {
        margin-top: 0;
    }

    .panel-content > :last-child {
        margin-bottom: 0;
    }
`;

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
        --internal-or-asset-viewer-button-color: var(--or-asset-viewer-button-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
        --internal-or-header-height: var(--or-header-height, ${unsafeCSS(DefaultHeaderHeight)});
                
        height: 100%;
        width: 100%;
        background-color: var(--internal-or-asset-viewer-background-color);
    }
   
    *[hidden] {
        display: none;
    }
    
    #wrapper {
        height: 100%;
        width: 100%;
        display: flex;
        flex-direction: column;
    }
    
    #wrapper.saving {
        opacity: 0.5;
        pointer-events: none;
    }

    #view-container, #edit-container {
        flex: 0 1 auto;
        padding-top: 10px;
        overflow: auto;
        min-height: calc(100vh - 88px - var(--internal-or-header-height));
    }
    
    #view-container {
        margin-top: 0;
        box-sizing: border-box;
        display: grid;
        padding: 0 20px 20px;
        grid-gap: 10px;
        grid-template-columns: repeat(auto-fill, minmax(calc(50% - 5px),1fr));
        grid-auto-rows: 5px;

        -webkit-animation: fadein 0.3s; /* Safari, Chrome and Opera > 12.1 */
        -moz-animation: fadein 0.3s; /* Firefox < 16 */
        -ms-animation: fadein 0.3s; /* Internet Explorer */
        -o-animation: fadein 0.3s; /* Opera < 12.1 */
        animation: fadein 0.3s;
    }
    
    #name-input {
        width: 300px;
    }
    
    @media only screen and (max-width: 767px) {
        #wrapper {
            position: absolute;
            left: 0;
            right: 0;
        }
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
        padding: 20px 30px;
        display: flex;
        flex: 0 0 auto;
        align-items: center;
        justify-content: space-between;
    }

    #asset-header.editmode {
        padding: 14px 30px;
        background-color: var(--internal-or-asset-viewer-panel-color);
        border-bottom: solid 1px #e5e5e5;
    }

    #title {
        flex: 1 1 0%;
        font-size: 18px;
        font-weight: bold;
    }

    #title > or-icon {
        margin-right: 10px;
    }
   
    #created-time {
        font-size: 12px;
    }
    
    #right-wrapper {
        flex: 1 1 0%;
        text-align: right;
    }
    
    #edit-wrapper {
        flex: 0 0 auto;
    }

    #edit-wrapper > or-translate {
        margin-right: 10px;
    }

    #save-btn {
        margin-left: 20px;
    }

    #edit-btn {
        margin-left: 15px;
    }
    
    #location-panel .panel-content {
        height: 100%;
    }
    
    #history-panel .panel-content {
        position: relative;
    }
    
    .msg {
        display: flex;
        justify-content: center;
        align-items: center;
        text-align: center;
        height: 100%;
    }
    
    .back-navigation {
        display: none;
        cursor: pointer;
    }

    @media screen and (max-width: 769px) {
        #wrapper {
            position: absolute;
            left: 0;
            right: 0;
        }

        .back-navigation {
            display: block;
        }
        
        .mobileHidden {
            display: none;
        }
        
        #asset-header {
            grid-area: auto!important;
            padding: 20px 15px 0;
        }

        .panel {
            border-radius: 0;
            border-right: none;
            border-left: none;
        }

        #chart-panel {
            grid-row-start: 1;
        }

        #attributes-panel {
            grid-row-start: 2;
        }
        
        #location-panel {         
            grid-row-start: 3;
        }
        
        #history-panel {
            grid-row-start: 4;
        }
        
        #view-container { 
            grid-auto-rows: auto;
            grid-template-columns: 100% !important;
            padding: 10px 0;
            min-height: unset;
        }
    }
`;
