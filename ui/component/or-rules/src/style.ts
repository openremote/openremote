import {DefaultColor5} from "@openremote/core";
import {css, unsafeCSS} from "lit";

// language=CSS
export const invalidStyle = css`
    *:invalid {
        border-bottom: 2px solid var(--internal-or-rules-invalid-color);
    }
`;

// language=CSS
export const buttonStyle = css`
    .button-clear {
        background: none;
        color: ${unsafeCSS(DefaultColor5)};
        --or-icon-fill: ${unsafeCSS(DefaultColor5)};
        visibility: hidden;
        display: inline-block;
        border: none;
        padding: 0;
        cursor: pointer;
    }

    .button-clear:hover {
        --or-icon-fill: var(--internal-or-rules-button-color);
    }

    .button-clear:focus {
        outline: 0;
    }

    .button-clear.hidden {
        visibility: hidden;
    }

    .plus-button {
        --or-icon-fill: var(--internal-or-rules-button-color);
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
        border-color: var(--internal-or-rules-line-color);
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
