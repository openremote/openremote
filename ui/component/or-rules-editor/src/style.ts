import {css} from "lit-element";

// language=CSS
export const rulesEditorStyle = css`

    :host {
        display: flex;
        height: 100%;
        width: 100%;
        
        --internal-or-rule-section-background-color: var(--or-rule-section-background-color, white);
        --internal-or-rule-list-background-color: var(--or-rule-list-background-color, white);
        --internal-or-rule-header-background-color: var(--or-rule-header-background-color, white);
        --internal-or-rule-toolbar-background-color: var(--or-rule-toolbar-background-color, white);
        --internal-or-rule-editor-background-color: var(--or-rule-editor-background-color, #f5f5f5);
        --internal-or-rule-foreground-color: var(--or-rule-foreground-color, green);
    }

    .shadow {
        -webkit-box-shadow: 0px 1px 3px 0px rgba(0, 0, 0, 0.21);
        -moz-box-shadow: 0px 1px 3px 0px rgba(0, 0, 0, 0.21);
        box-shadow: 0px 1px 3px 0px rgba(0, 0, 0, 0.21);
    }

    side-menu {
        min-width: 300px;
        width: 300px;
        z-index: 2;
        display: flex;
        flex-direction: column;
    }

    or-rule-list {
        flex-grow: 1;
        background-color: var(--internal-or-rule-list-background-color);
    }
    
    or-rule-header {
        background-color: var(--internal-or-rule-header-background-color);
    }

    .bottom-toolbar {
        display: flex;
        border-top: 1px solid var(--app-lightgrey-color);
        background-color: var(--internal-or-rule-toolbar-background-color);
    }
    
    .bottom-toolbar or-icon {
        --or-icon-height: 16px;
    }
    
    icon {
        cursor: pointer;
        padding: 5px;
        margin: 10px;
        border: 1px solid var(--app-lightgrey-color);
        border-radius: 3px;
    }

    icon:hover {
        background-color: var(--app-primary-color);
        color: var(--app-white-color, #FFF);
    }

    .rule-container {
        display: flex;
        flex-direction: column;
        flex-grow: 1;
    }
    
    .rule-editor-panel {
        display: flex;
        flex-grow: 1;
        background-color: var(--internal-or-rule-editor-background-color);
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
        border-bottom: 2px solid red;
    }
`;

// language=CSS
export const buttonStyle = css`
    .button-clear {
        background: none;
        color: var(--internal-or-rule-foreground-color);
        border: none;
        padding: 0;
        display: inline-block;
        cursor: pointer;
    }

    .button-clear:focus {
        outline: 0;
    }
    
    .button-clear > or-icon {
        fill: var(--internal-or-rule-foreground-color);
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
        border-color: var(--internal-or-rule-foreground-color);
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
        background-color: var(--internal-or-rule-section-background-color);
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
        color: var(--app-grey-color, #808080);
        font-size:14px;
        font-weight: bold;
        text-align: left;
    }

    .list-item {
        text-decoration: none;
        font-size: 14px;
        padding: 13px 15px;
        border-left: 5px solid transparent;
        color: var(--app-grey-color, #808080);
        cursor: pointer;

        transition: all 200ms ease-in;
    }

    .list-item:hover {
        border-left-color: transparent;
        background-color: #f7f7f7;
        color: #000000;
    }

    .list-item[selected] {
        border-left-color: var(--app-primary-color);
        background-color: #f7f7f7;
        color: #000000;
    }

    .list-item:hover {
        opacity: 0.77;
    }

    .list-item > span {
        font-size: 18px;
    }

    .rule-status {
        width: 8px;
        height: 8px;
        border-radius: 8px;
        margin: 6px 10px 0 0;
    }

    .bg-green {
        background-color: green;
    }

    .bg-red {
        background-color: red
    }
`;

// language=CSS
export const headerStyle = css`
    
    ${invalidStyle}
    
    :host {
        display: block;
        width: 100%;

        z-index: 1;
    }

    .rule-container {
       padding: 20px 30px;
    }

    .layout.horizontal {
        display: flex;
        flex-direction: row;
    }

    input {
        font-size: 18px;
        font-weight: bold;
        border: none;
        background-color: transparent;
        border-bottom: 2px solid transparent;
        transition: all 150ms ease-in;
        
        width: 350px;
        max-width: 100%;
    }

    input:hover {
        border-bottom: 2px solid #d5d5d5;
    }

    button {
        padding: 0 20px;
        font-size: 14px;
        height: 40px;

        border-radius: 5px;
        border: none;
        background-color: var(--internal-or-rule-foreground-color);
        margin-left: auto;
        color: var(--app-white-color, #FFF);
        font-weight: bold;
        cursor: pointer;
    }
    
    button[disabled] {
        background-color: #e4e4e4;
    }
    
    .button-simple {
        border: none;
        color: var(--internal-or-rule-foreground-color);
        background-color: var(--internal-or-rule-header-background-color);
    }

    or-input {
        padding: 10px;
    }


    /* The switch - the box around the slider */
    .switch {
        position: relative;
        display: inline-block;
        width: 36px;
        height: 14px;
        margin: 13px 30px 13px 10px;
    }

    /* Hide default HTML checkbox */
    .switch input {
        opacity: 0;
        width: 0;
        height: 0;
    }

    /* The slider */
    .slider {
        position: absolute;
        cursor: pointer;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background-color: #ccc;
        -webkit-transition: .4s;
        transition: .4s;
    }
    
    .switch[data-disabled] {
        cursor: auto;
    }
    
    .toggle-label[data-disabled] {
        color:  #e4e4e4;
    }
    
    .switch[data-disabled] .slider::before {
        background-color: #e4e4e4;
    }

    .slider:before {
        position: absolute;
        content: "";
        height: 20px;
        width: 20px;
        left: 0;
        top: -3px;
        background-color: white;
        -webkit-transition: .4s;
        transition: .4s;
        box-shadow: 0 1px 5px 0 rgba(0, 0, 0, 0.6);
    }

    input:checked + .slider:before {
        background-color: var(--internal-or-rule-foreground-color);
    }

    input:focus + .slider {
    }

    input:checked + .slider:before {
        -webkit-transform: translateX(20px);
        -ms-transform: translateX(20px);
        transform: translateX(20px);
    }

    /* Rounded sliders */
    .slider.round {
        border-radius: 34px;
    }

    .slider.round:before {
        border-radius: 50%;
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
        border-color: var(--internal-or-rule-foreground-color);    
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
        border: 2px solid var(--internal-or-rule-foreground-color);
        border-radius: 3px;
        font-weight: bold;
    }
    
    .operator * {
        font-size: small;
        color: var(--internal-or-rule-foreground-color);    
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
        border: 2px solid var(--internal-or-rule-foreground-color);
        border-radius: 3px;
        font-weight: bold;
    }
    
    .operator * {
        font-size: small;
        color: var(--internal-or-rule-foreground-color);    
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
