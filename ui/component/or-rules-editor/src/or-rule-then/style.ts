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
        padding: 10px 40px;
        font-size: 20px;
        font-weight: bold;
        height: 32px;
        box-sizing: content-box;
        border: 3px dashed var(--app-lightgrey-color, #e4e4e4);
    }
    
    .rule-additional {
        height: 38px;
        line-height: 38px;
        padding: 10px 20px;
        font-size: 20px;
        font-weight: bold;
    }
`;