import {css} from 'lit-element';

// language=CSS
export const style = css`    
    .rule-content-section {
        flex-grow: 1;
        flex-basis: 1;
        margin: 20px 10px;
    }

    .rule-when-container {
        display: flex;
        padding: 20px;

        background-color: var(--app-white-color, #FFF);
        flex-wrap: wrap;
        box-shadow: rgba(0, 0, 0, 0.11) 0px 1px 2px 0px;
    }
    
    .button-add {
        display: block;
        cursor: pointer;
        font-weight: bold;    
        height: 32px;
        box-sizing: content-box;

        padding: 0;
        border: none;
        font-size: 15px;
        color: var(--app-primary-color);
        
    }
    
    .rule-additional {
        height: 38px;
        line-height: 38px;
        padding: 10px 20px;
        font-size: 20px;
        font-weight: bold;
    }
`;