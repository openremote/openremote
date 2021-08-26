import {DefaultColor4, DefaultColor5 } from "@openremote/core";
import { css, html, unsafeCSS } from "lit";

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
    
    .any-of-picker {
        width: 100%;
        min-width: 200px;
    }
`;

// language=HTML
export const addItemOrParameterDialogStyle = html`
    <style>
        .mdc-dialog__surface {
            width: 800px;
            overflow-x: visible !important;
            overflow-y: visible !important;
        }
        #dialog-content {
            border-color: var(--or-app-color5, ${unsafeCSS(DefaultColor5)});
            border-top-width: 1px;
            border-top-style: solid;
            border-bottom-width: 1px;
            border-bottom-style: solid;
            padding: 0;
            overflow: visible;
        }
        form {
            display: flex;
        }
        #type-list {
            overflow: auto;
            min-width: 150px;
            max-width: 300px;
            flex: 0 0 40%;
            border-right: 1px solid var(--or-app-color5, #CCC);
        }
        #parameter-list {
            display: block;
        }
        #parameter-desc {
            padding: 5px;
            flex: 1;
        }
    </style>
`;
