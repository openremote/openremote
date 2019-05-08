import {css} from 'lit-element';

// language=CSS
export const style = css`
    :host {
        display: block;
        height: 100%;
        width: 100%;

        --bg-white: #FFFFFF;
        --bg-grey: #F5F5F5;
        --app-lightgrey-color: #f2f2f2;
    }

    .rule-editor-container {
        display: flex;
        position: relative;
        height: 100%;
        width: 100%;
    }

    .bg-white {
        background-color: var(--bg-white);
    }

    .shadow {
        -webkit-box-shadow: 0px 1px 3px 0px rgba(0, 0, 0, 0.21);
        -moz-box-shadow: 0px 1px 3px 0px rgba(0, 0, 0, 0.21);
        box-shadow: 0px 1px 3px 0px rgba(0, 0, 0, 0.21);
    }

    side-menu {
        min-width: 300px;
        width: 300px;

        z-index: 2;
        display: flex;
        flex-direction: column;
    }

    or-rule-list {
        flex-grow: 1;
    }

    .bottom-toolbar {
        display: flex;

        border-top: 1px solid var(--app-lightgrey-color);
    }

    icon {
        cursor: pointer;
        padding: 5px;
        margin: 10px;
        border: 1px solid var(--app-lightgrey-color);
        border-radius: 3px;
    }

    icon:hover {
        background-color: var(--app-primary-color);
        color: var(--app-white-color, #FFF);
    }

    or-body,
    .content {
        display: flex;
        flex-grow: 1;
    }

    or-body {
        flex-direction: column;
    }

    .content {
        flex-direction: row;
    }

    or-rule-then,
    or-rule-when {
        display: flex;
        flex: 1 1 0;
        background-color: var(--app-lightgrey-color);
    }

    or-rule-when {
        padding-left: 10px;
    }
    
    or-rule-then {
        padding-right: 10px;
    }
    
    .small-icon {
        --or-icon-height: 16px;
    }
    
    button {
        padding: 0 20px;
        font-size: 14px;
        height: 40px;
        border: none;
        border-radius: 5px;
        background-color: var(--app-primary-color);
        margin-left: auto;
        color: var(--app-white-color, #FFF);
        font-weight: bold;
        cursor: pointer;
    }
    
    .center-center {
        display: flex;
        justify-content: center;
        align-items: center;
        
        flex-direction: column;
        text-align: center;
        margin: auto;
    }
    
    @media only screen 
    and (min-device-width : 768px) 
    and (max-device-width : 1024px)  { 
        side-menu {
            min-width: 150px;
            width: 150px;
        }
    }
    
`;