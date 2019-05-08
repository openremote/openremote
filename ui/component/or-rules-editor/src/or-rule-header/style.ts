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

    input {
        font-size: 18px;
        font-weight: bold;
        border: none;
        background-color: transparent;
        border-bottom: 2px solid transparent;

        transition: all 150ms ease-in;
    }

    input:hover {
        border-bottom: 2px solid #d5d5d5;
    }

    button {
        padding: 0 20px;
        font-size: 14px;
        height: 40px;

        border-radius: 5px;
        border: none;
        background-color: var(--app-primary-color);
        margin-left: auto;
        color: var(--app-white-color, #FFF);
        font-weight: bold;
        cursor: pointer;
    }
    
    button[disabled] {
        background-color: #e4e4e4;
    }
    
    .button-simple {
        border: none;
        color: var(--app-primary-color);
        background-color: var(--app-white-color);
    }

    or-input {
        padding: 10px;
    }


    /* The switch - the box around the slider */
    .switch {
        position: relative;
        display: inline-block;
        width: 36px;
        height: 14px;
        margin: 13px 30px 13px 10px;
    }

    /* Hide default HTML checkbox */
    .switch input {
        opacity: 0;
        width: 0;
        height: 0;
    }

    /* The slider */
    .slider {
        position: absolute;
        cursor: pointer;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background-color: #ccc;
        -webkit-transition: .4s;
        transition: .4s;
    }
    
    .switch[data-disabled] {
        cursor: auto;
    }
    
    .toggle-label[data-disabled] {
        color:  #e4e4e4;
    }
    
    .switch[data-disabled] .slider::before {
        background-color: #e4e4e4;
    }

    .slider:before {
        position: absolute;
        content: "";
        height: 20px;
        width: 20px;
        left: 0;
        top: -3px;
        background-color: white;
        -webkit-transition: .4s;
        transition: .4s;
        box-shadow: 0 1px 5px 0 rgba(0, 0, 0, 0.6);
    }

    input:checked + .slider:before {
        background-color: var(--app-primary-color);
    }

    input:focus + .slider {
    }

    input:checked + .slider:before {
        -webkit-transform: translateX(20px);
        -ms-transform: translateX(20px);
        transform: translateX(20px);
    }

    /* Rounded sliders */
    .slider.round {
        border-radius: 34px;
    }

    .slider.round:before {
        border-radius: 50%;
    }
`;