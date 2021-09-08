import {DefaultColor4, DefaultColor5 } from "@openremote/core";
import { css, html, unsafeCSS } from "lit";

// language=CSS
export const baseStyle = css`
    :host {
        flex: 1;
    }
    
    .item-container {
        display: flex;
    }
    
    .delete-container, .drag-container {
        width: 30px;
        display: flex;
        vertical-align: middle;
    }
    
    .item-container:hover .button-clear, .item-wrapper:hover .button-clear {
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

    #errors {
        color: red;
        margin-right: 10px;
        flex: 1;
        display: flex;
        align-items: center;
    }

    #errors > or-icon {
        margin-right: 5px;
    }
`;

// language=CSS
export const panelStyle = css`
    #header-description {
        flex: 1;
        display: flex;
        flex-direction: row;
    }

    #header-buttons {
        flex: 0;
    }

    #content-wrapper {
        flex: 1;
        padding: 0 10px 10px 10px;
    }

    #content {
        display: flex;
        flex-direction: column;
    }

    #content > * {
        flex: 1;
    }

    .item-container + .item-container {
        padding-top: 10px;
    }

    #footer {
        margin-top: 10px;
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
