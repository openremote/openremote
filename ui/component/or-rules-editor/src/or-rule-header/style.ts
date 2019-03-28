import {css} from 'lit-element';

// language=CSS
export const style = css`
    :host {
        display: block;
        width: 100%;

        z-index: 1;
    }
    
    .rule-container {
        padding: 20px;
    }
    
    .layout.horizontal {
        display: flex;
        flex-direction: row;
    }
    
    h1 {
        margin: 0;
    }
    
    button {
    
        padding: 0 20px;
        font-size: 14px;
        height: 40px;
        
        border-radius: 5px;
        background-color: var(--app-primary-color);
        margin-left: auto;
        color: var(--app-white-color, #FFF);
        font-weight: bold;
    }
    
    or-input {
       padding: 10px;
    }
    
   .rule-status {
        width: 8px;
        height: 8px;
        border-radius: 8px;
        margin: 15px
    }
    
    .bg-green {
        background-color: green;
    }
    
    .bg-red {
        background-color: red
    }
`;