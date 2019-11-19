import {css, unsafeCSS} from "lit-element";
import {DefaultColor1, DefaultColor2, DefaultColor5, DefaultColor3, DefaultBoxShadow, DefaultColor4, DefaultColor6, DefaultDisabledOpacity} from "@openremote/core";

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
        margin-bottom: -10px;
        padding-top: 5px;
        border-top-width: 1px;
        border-top-style: solid;
        border-color: var(--internal-or-rules-editor-line-color);
    }

    .add-buttons-container.hidden {
        border: none;
        margin: 0;
        padding: 0;
    }
    
    .add-buttons-container > button {
        display: inline-flex;
    }

    .add-buttons-container > button > or-icon {
        margin-right: 5px;
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
