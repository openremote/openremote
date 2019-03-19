import {css} from 'lit-element';

// language=CSS
export const style = css`
    
    .rule-content-section {
        flex-grow: 1;
        flex-basis: 1;
        margin: 20px 10px;
    }

    .rule-then-container {
        display: flex;
        padding: 20px;

        flex-wrap: wrap;
        background-color: var(--app-white-color, #FFF);
    }
    
    .button-add {
        display: block;
        cursor: pointer;
        padding: 15px 40px;
        font-size: 20px;
        font-weight: bold;
        box-sizing: border-box;
        border: 3px dashed var(--app-lightgrey-color, #e4e4e4);
    }
    
    .rule-additional {
        padding: 15px 20px;
        font-size: 20px;
        font-weight: bold;
    }
`;