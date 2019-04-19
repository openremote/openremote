import {css} from "lit-element";

// TODO this is temp work, should be replaced with material design components / styling
// language=CSS
export const selectStyle = css`
    .mdc-select {
        font-family:
                'Roboto','Helvetica','Arial',sans-serif;
        position: relative;
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

        background-image: url('https://cdn3.iconfinder.com/data/icons/google-material-design-icons/48/ic_keyboard_arrow_down_48px-128.png');
        background-repeat: no-repeat;
        background-size: 20px;
        background-position:  right 10px center;
    }

`;