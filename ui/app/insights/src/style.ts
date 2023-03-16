import { DefaultColor3 } from "@openremote/core";
import { css, unsafeCSS } from "lit";

// language=CSS
export const style = css`

    @media only screen and (max-width: 640px){
        .hideMobile {
            display: none !important;
        }
        .fullWidthOnMobile {
            flex: 1 !important;
        }
    }
    @media only screen and (min-width: 641px){
        .showMobile {
            display: none !important;
        }
    }
    
    /* Header related styling */
    #fullscreen-header-wrapper {
        min-height: 36px;
        padding: 20px 30px 15px;
        display: flex;
        flex-direction: row;
        align-items: center;
    }
    #fullscreen-header-title {
        font-size: 18px;
        font-weight: bold;
        color: var(--or-app-color3, ${unsafeCSS(DefaultColor3)});
    }
    #fullscreen-header-title > or-mwc-input {
        margin-right: 4px;
        --or-icon-fill: ${unsafeCSS(DefaultColor3)};
    }
    #fullscreen-header-actions {
        flex: 1 1 auto;
        text-align: right;
    }
    #fullscreen-header-actions-content {
        display: flex;
        flex-direction: row;
        align-items: center;
        float: right;
    }

    .small-btn {
        height: 36px;
        margin-top: -12px;
    }
`
