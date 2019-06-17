import {css} from "lit-element";

// TODO this is temp work, should be replaced with material design components / styling
// language=CSS
export const selectStyle = css`
    
    select:invalid {
        border-bottom: 2px solid red;
    }
    
    .mdc-select {
        font-family:
                'Roboto','Helvetica','Arial',sans-serif;
        position: relative;
        display: inline-block;
    }

    select {
        position: relative;
        font-family: inherit;
        padding: 10px 40px 10px 10px;
        border-radius: 0;
        border: none;
        -webkit-appearance: none;
        -moz-appearance: none;
        appearance: none;
        font-size: 14px;

        border-bottom: 2px solid #d5d5d5;
        background-color: #f2f2f2;

        background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24'%3E%3Cpath d='M7.41 7.84L12 12.42l4.59-4.58L18 9.25l-6 6-6-6z'/%3E%3C/svg%3E");
        background-repeat: no-repeat;
        background-size: 20px;
        background-position:  right 10px center;
    }

`;