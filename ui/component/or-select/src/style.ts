import {css, unsafeCSS} from "lit-element";
import ORCore from "@openremote/core";

// TODO this is temp work, should be replaced with material design components / styling

export const optionColorVar = "--internal-or-select-option-text-color";

// language=CSS
export const selectStyle = css`
    
    :host {
        display: inline-block;
        
        --internal-or-select-background-color: var(--or-select-background-color, var(--or-app-color1, ${unsafeCSS(ORCore.DefaultColor2)}));     
        --internal-or-select-border-color: var(--or-select-border-color, var(--or-app-color1, ${unsafeCSS(ORCore.DefaultColor5)}));
        --internal-or-select-text-color: var(--or-select-text-color, inherit);
        --internal-or-select-padding: var(--or-select-padding, 10px 40px 10px 10px);
        --internal-or-select-option-text-color: var(--or-select-option-text-color, inherit);
        --internal-or-select-option-background-color: var(--or-select-option-background-color, var(--or-app-color7, ${unsafeCSS(ORCore.DefaultColor7)}));
    }
    
    select:invalid {
        border-bottom: 2px solid red;
    }
    
    .mdc-select {
        font-family: 'Roboto','Helvetica','Arial',sans-serif;
        position: relative;
        display: inline-block;
        fill: var(--internal-or-select-text-color);
    }

    select {
        cursor: pointer;
        position: relative;
        font-family: inherit;
        padding: var(--internal-or-select-padding);
        border-radius: 0;
        border: none;
        -webkit-appearance: none;
        -moz-appearance: none;
        appearance: none;
        font-size: 14px;
        outline: none;
        color: var(--internal-or-select-text-color);
        border-bottom: 2px solid var(--internal-or-select-border-color);
        background-color: var(--internal-or-select-background-color);
    }
    
    #width_tmp_select {
        position: absolute;
        visibility: hidden;
        pointer-events: none;
    }
    
    select > option {
        background-color: var(--internal-or-select-option-background-color);
    }
    
    svg {
        width: 20px;
        height: 100%;
        position: absolute;
        right: 0;
        top: 0;
        pointer-events: none;
    }

`;