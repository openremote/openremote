import {css} from "lit-element";

// language=CSS
export const style = css`
    :host {
        position: relative;
    }
    
    .dropdown-menu {
        position: absolute;
        top: 100%;
        box-shadow: rgba(0, 0, 0, 0.3) 0 5px 5px -2px;
        width: 120px;
        background-color: white;
        right: 0;
    }
    
    span {
        display: block;
        height: 40px;
        line-height: 40px;
    }
    
    .background-close {
        display: none;
        position: fixed;
        width: 100vw;
        height: 100vh;
        top: 0;
        left: 0;
        z-index: 0;
    }
    
    .background-close.active {
        display: block;
    }
`;