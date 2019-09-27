import {css, unsafeCSS} from "lit-element";
import {DefaultColor1, DefaultColor2, DefaultColor5, DefaultColor3, DefaultColor4, DefaultBoxShadow, DefaultDisabledOpacity,
    DefaultBoxShadowBottom} from "@openremote/core";

// TODO this is temp work, should be replaced with material design components / styling
// language=CSS
export const orInputStyle = css`
    
    :host {
        --internal-or-input-color: var(--or-input-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));    
        --internal-or-input-text-color: var(--or-input-text-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));    
        
        --mdc-theme-primary: var(--internal-or-input-color);
        --mdc-theme-on-primary: var(--internal-or-input-text-color);
        --mdc-theme-secondary: var(--internal-or-input-color);
    }
       
    .or-input--rounded {
        border-radius: 50% !important;
    }
    
    /* MDC TEXT FIELD DOESN'T USE THEME VARS */
    .mdc-text-field--focused:not(.mdc-text-field--disabled) .mdc-floating-label {
      color: var(--mdc-theme-primary);
    }
    .mdc-text-field--focused .mdc-text-field__input:required ~ .mdc-floating-label::after,
    .mdc-text-field--focused .mdc-text-field__input:required ~ .mdc-notched-outline .mdc-floating-label::after {
      color: var(--mdc-theme-primary);
    }
    
    .mdc-text-field, .mdc-text-field-helper-line {
        width: inherit;
    }
    
    .mdc-text-field--dense:not(.mdc-text-field--no-label):not(.mdc-text-field--outlined) {
        height: 52px;
    }
    
    .mdc-text-field--dense.mdc-text-field--no-label {
        height: 40px;
    }
    
    .mdc-switch {
        margin: 18px;
    }
`;
