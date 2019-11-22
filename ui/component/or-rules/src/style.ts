import {css} from "lit-element";

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
        color: var(--internal-or-rules-button-color);
        --or-icon-fill: var(--internal-or-rules-button-color);
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
