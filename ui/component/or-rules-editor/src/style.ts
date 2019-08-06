import {css, unsafeCSS} from "lit-element";
import {DefaultColor1, DefaultColor2, DefaultColor5, DefaultColor3, DefaultBoxShadow, DefaultColor4, DefaultColor6, DefaultDisabledOpacity} from "@openremote/core";

// language=CSS
export const rulesEditorStyle = css`

    :host {
        display: flex;
        height: 100%;
        width: 100%;
        
        --internal-or-rules-editor-background-color: var(--or-rules-editor-background-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-rules-editor-text-color: var(--or-rules-editor-text-color, inherit);
        --internal-or-rules-editor-panel-color: var(--or-rules-editor-panel-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
        --internal-or-rules-editor-button-color: var(--or-rules-editor-button-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));
        --internal-or-rules-editor-invalid-color: var(--or-rules-editor-invalid-color, var(--or-app-color6, ${unsafeCSS(DefaultColor6)}));        
        --internal-or-rules-editor-panel-color: var(--or-rules-editor-panel-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
        --internal-or-rules-editor-line-color: var(--or-rules-editor-line-color, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));
        
        --internal-or-rules-editor-list-selected-color: var(--or-rules-editor-list-selected-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
        --internal-or-rules-editor-list-text-color: var(--or-rules-editor-list-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
        --internal-or-rules-editor-list-text-size: var(--or-rules-editor-list-text-size, 15px);

        --internal-or-rules-editor-list-button-size: var(--or-rules-editor-list-button-size, 24px);
        
        --internal-or-rules-editor-header-background-color: var(--or-rules-editor-header-background-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));
        --internal-or-rules-editor-header-height: var(--or-rules-editor-header-height, 80px);
    }

    .shadow {
        -webkit-box-shadow: ${unsafeCSS(DefaultBoxShadow)};
        -moz-box-shadow: ${unsafeCSS(DefaultBoxShadow)};
        box-shadow: ${unsafeCSS(DefaultBoxShadow)};
    }

    #rule-list-container {
        min-width: 300px;
        width: 300px;
        z-index: 2;
        display: flex;
        flex-direction: column;
        background-color: var(--internal-or-rules-editor-panel-color);
        color: var(--internal-or-rules-editor-list-text-color);

    }

    #rule-list-container #bottom-toolbar {
        display: flex;
        border-top: 1px solid var(--internal-or-rules-editor-line-color);
        --or-icon-fill: var(--internal-or-rules-editor-panel-color);
        --or-icon-width: var(--internal-or-rules-editor-list-button-size);
        --or-icon-height: var(--internal-or-rules-editor-list-button-size);        
    }
    
    #bottom-toolbar button {
        padding: 5px;
        margin: 10px;
        cursor: pointer;
        border: 1px solid var(--internal-or-rules-editor-line-color);
        border-radius: 3px;
        background-color: var(--internal-or-rules-editor-button-color);
    }

    .rule-container {
        display: flex;
        flex-direction: column;
        flex-grow: 1;
    }
    
    or-rule-header {
        min-height: var(--internal-or-rules-editor-header-height);
        height: var(--internal-or-rules-editor-header-height);
    }
    
    .rule-editor-panel {
        display: flex;
        flex-grow: 1;
        background-color: var(--internal-or-rules-editor-background-color);
    }
    
    .rule-editor-panel > * {
        display: flex;
        flex: 1 1 0;
    }

    or-rule-section {
        margin: 10px;
        flex-grow: 1;
        flex-shrink: 1;
    }
    
    .center-center {
        display: flex;
        justify-content: center;
        align-items: center;
        
        flex-direction: column;
        text-align: center;
        margin: auto;
    }
    
    @media only screen 
    and (min-device-width : 768px) 
    and (max-device-width : 1024px)  { 
        side-menu {
            min-width: 150px;
            width: 150px;
        }
    }
`;

// language=CSS
export const invalidStyle = css`
    *:invalid {
        border-bottom: 2px solid var(--internal-or-rules-editor-invalid-color);
    }
`;

// language=CSS
export const buttonStyle = css`
    .button-clear {
        background: none;
        color: var(--internal-or-rules-editor-button-color);
        --or-icon-fill: var(--internal-or-rules-editor-button-color);
        border: none;
        padding: 0;
        display: inline-block;
        cursor: pointer;
    }

    .button-clear:focus {
        outline: 0;
    }

    .add-button {
        display: inline-block;
        font-weight: bold;
        font-size: 15px;
        line-height: 24px;
    }

    .add-buttons-container {
        display: flex;
        flex-direction: column;
        margin-top: 10px;
        padding-top: 5px;
        border-top-width: 1px;
        border-top-style: solid;
        border-color: var(--internal-or-rules-editor-line-color);
    }

    .add-buttons-container > button {
        display: inline-flex;
    }

    .add-buttons-container > button > or-icon {
        margin-right: 5px;
    }
`;

// language=CSS
export const ruleSectionStyle = css`
    :host > div {
        padding: 20px;
        display: flex;
        flex-direction: column;
        background-color: var(--internal-or-rules-editor-panel-color);
        box-shadow: rgba(0, 0, 0, 0.11) 0 1px 2px 0;
    }
    
    strong {
        text-transform: capitalize;
        padding-bottom: 5px;
    }
`;

// language=CSS
export const ruleListStyle = css`
    :host {
        overflow: auto;
        flex-grow: 1;
        font-size: var(--internal-or-rules-editor-list-text-size);
    }
    
    .d-flex {
        display: -webkit-box;
        display: -moz-box;
        display: -ms-flexbox;
        display: -webkit-flex;
        display: flex;
    }

    .flex {
        -webkit-box-flex: 1;
        -moz-box-flex: 1;
        -webkit-flex: 1;
        -ms-flex: 1;
        flex: 1;
    }

    .list-title {
        display: block;
        padding: 30px 30px 5px 20px;
        text-transform: uppercase;
        color: var(, #808080);
        font-size:14px;
        font-weight: bold;
        text-align: left;
    }

    .list-item {
        text-decoration: none;
        padding: 13px 15px;
        border-left: 5px solid transparent;
        cursor: pointer;
        transition: all 200ms ease-in;
        opacity: 0.8;
    }

    .list-item:hover {
        border-left-color: var(--internal-or-rules-editor-list-selected-color);
        background-color: var(--internal-or-rules-editor-list-selected-color);
    }

    .list-item[selected] {
        border-left-color: var(--internal-or-rules-editor-button-color);
        background-color: var(--internal-or-rules-editor-list-selected-color);
        opacity: 1;
    }

    .list-item > span {
        width: 8px;
        height: 8px;
        border-radius: 8px;
        margin: 6px 10px 0 0;
    }

    .bg-green {
        background-color: #28b328;
    }

    .bg-red {
        background-color: red
    }
    
    .bg-blue {
        background-color: #3e92dc;
    }

    .bg-grey {
        background-color: #b7b7b7;
    }
`;

// language=CSS
export const headerStyle = css`
    
    ${invalidStyle}
    
    :host {
        display: block;
        width: 100%;
        z-index: 1;
        background-color: var(--internal-or-rules-editor-header-background-color);
        --or-icon-fill: var(--internal-or-rules-editor-panel-color);
    }

    #container {
        display: flex;
        height: 100%;
        flex-direction: row;
        padding: 0 20px;
    }

    #title {
        font-size: 18px;
        font-weight: bold;
        border: 2px solid transparent;
        background-color: transparent;
        transition: all 150ms ease-in;        
        width: 350px;
        max-width: 100%;
    }

    #title:hover {
        border-bottom-color: 2px solid var(--internal-or-rules-editor-line-color);
    }
    
    #title:focus {
        outline: none;
    }
    
    #controls {
        margin: 0 0 0 auto;
        display: flex;
        flex-direction: row;
        align-items: center;
        height: 100%;    
    }
    
    #save-btn {
        padding: 0 20px;
        font-size: 14px;
        height: 40px;
        border-radius: 5px;
        border: none;
        background-color: var(--internal-or-rules-editor-button-color);
        margin-left: auto;
        color: var(--internal-or-rules-editor-panel-color);
        font-weight: bold;
        cursor: pointer;
    }
    
    #save-btn[disabled] {
        opacity: ${unsafeCSS(DefaultDisabledOpacity)};
        pointer-events: none;
    }

    /* The switch - the box around the slider */
    #toggle {
        position: relative;
        display: inline-block;
        width: 36px;
        height: 14px;
        margin: 13px 30px 13px 10px;
    }

    /* The slider */
    #toggle > span {
        position: absolute;
        cursor: pointer;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background-color: #ccc;
        -webkit-transition: .4s;
        transition: .4s;
        border-radius: 34px;
    }

    #toggle-label[data-disabled], #toggle[data-disabled] {
        opacity: ${unsafeCSS(DefaultDisabledOpacity)};
        pointer-events: none;
    }

    #toggle > span:before {
        position: absolute;
        content: "";
        height: 20px;
        width: 20px;
        left: 0;
        top: -3px;
        opacity: 1;
        background-color: white;
        border-radius: 50%;
        -webkit-transition: .4s;
        transition: .4s;
        box-shadow: ${unsafeCSS(DefaultBoxShadow)};
    }

    #toggle[data-checked] > span:before {
        background-color: var(--internal-or-rules-editor-button-color);
        transform: translateX(20px);
    }
`;

// language=CSS
export const whenStyle = css`
    
    ${buttonStyle}
    
    .rule-group {
        display: flex;
        flex-direction: column;
        align-items: stretch;
        position: relative;
    }

    .rule-group.visible {
        padding: 20px 20px 10px 20px;
        border-width: 2px;
        border-style: solid;
        border-color: var(--internal-or-rules-editor-line-color);    
    }
        
    .rule-group > .remove-button {
        position: absolute;
        right: 0;
        top: 0;
        width: 24px;
        height: 24px;
        transform: translate(50%, -50%);
    }
    
    .rule-group > .remove-button > div {
        position: absolute;
        left: 2px;
        right: 2px;
        bottom: 2px;
        top: 2px;
        background-color: #FFF;
    }
    
    .rule-group-items {
        display: flex;
        flex-direction: column;
    }
    
    .rule-group .rule-group-items > div {
        margin: 10px 0;
    }
    
    .rule-group-item {
        position: relative;
        padding-left: 40px;
    }
    
    .rule-condition {
        display: flex;
    }

    .operator {
        text-transform: uppercase;
        position: absolute;
        left: 0;
        top: 100%;
        padding: 2px;
        border: 2px solid var(--internal-or-rules-editor-button-color);
        border-radius: 3px;
        font-weight: bold;
        font-size: small;
    }    
        
    .rule-condition > * {
        flex-grow: 0;
    }
    
    .rule-condition > or-rule-condition {
        flex-grow: 1;
    }
    
    .rule-group .rule-group-items div:last-child > .operator {
        display: none;
    }
`;

// language=CSS
export const actionStyle = css`
    
    ${buttonStyle}
    
    :host {
        display: flex;
        width: 100%;
        flex-direction: column;
    }
    
    .rule-action {
        display: flex;
        margin: 10px 0;
    }
    
    .rule-action-action {
        flex-grow: 1;
    }
    
    .rule-action-action > * {
        margin-right: 20px;
    }
    
    .rule-action > button {
        flex-grow: 0;
    }
`;

// language=CSS
export const conditionEditorStyle = css`
    :host {
        display: flex;
        flex-direction: row;
    }
    
    :host > * {
        margin-right: 20px;
    }
    
    or-rule-asset-query {
        flex-grow: 1;
    }
`;

// language=CSS
export const assetQueryEditorStyle = css`
    
    ${buttonStyle}
    
    :host {
        display: flex;
        flex-direction: row;
    }
    
    :host > * {
        margin-right: 20px;
    }
    
    .attribute-group {
        flex-grow: 1;
        display: flex;
        flex-direction: column;
        align-items: stretch;
        position: relative;
    }
    
    .attribute-items {
        display: flex;
        flex-direction: column;
    }
    
    .attribute-items > div {
        margin: 10px 0;
    }
    
    .attribute-item {
        position: relative;
        padding-left: 40px;
    }
    
    .attribute {
        display: flex;
    }

    .operator {
        text-transform: uppercase;
        position: absolute;
        left: 0;
        top: 100%;
        padding: 2px;
        border: 2px solid var(--internal-or-rules-editor-button-color);
        border-radius: 3px;
        font-weight: bold;
    }
    
    .operator * {
        font-size: small;
        color: var(--internal-or-rules-editor-button-color);    
    }
    
    .attribute > * {
        flex-grow: 0;
    }
    
    .attribute > .attribute-editor {
        flex-grow: 1;
    }
    
    .attribute-items div:last-child > .operator {
        display: none;
    }
`;
