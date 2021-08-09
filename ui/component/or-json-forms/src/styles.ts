import {DefaultColor4, DefaultColor5 } from "@openremote/core";
import { css, unsafeCSS } from "lit";

// language=CSS
export const baseStyle = css`
    :host {
        flex: 1;
    }
    
    .item-container {
        display: flex;
        margin: 10px 0 10px 10px;
    }
    
    .delete-container {
        width: 30px;
        display: flex;
    }
    
    .item-container:hover .button-clear {
        visibility: visible;                    
    }
    
    .button-clear {
        background: none;
        color: ${unsafeCSS(DefaultColor5)};
        --or-icon-fill: ${unsafeCSS(DefaultColor5)};
        visibility: hidden;
        display: inline-block;
        border: none;
        padding: 0 0 0 5px;
        cursor: pointer;
    }                
    .button-clear:hover {
        --or-icon-fill: ${unsafeCSS(DefaultColor4)};
    }                
    .button-clear:focus {
        outline: 0;
    }                
    .button-clear.hidden {
        visibility: hidden;
    }
`;
