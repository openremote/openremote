import {css} from "lit-element";

// TODO this is temp work, should be replaced with material design components / styling
// language=CSS
export const orInputStyle = css`
    input {
        position: relative;
        font-family: inherit;
        padding: 10px;
        border-radius: 0;
        border: none;
        font-size: 14px;

        border-bottom: 2px solid #d5d5d5;
        background-color: #f2f2f2;
    }

    input:focus:required:invalid { border-bottom: 2px solid red;}
`;