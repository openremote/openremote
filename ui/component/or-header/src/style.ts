import {css, unsafeCSS} from "lit-element";
import {DefaultColor1, DefaultColor2, DefaultColor3, DefaultColor4, DefaultColor5, DefaultBoxShadowBottom, DefaultHeaderHeight} from "@openremote/core";

// language=CSS
export const style = css`
    
        :host {
            --internal-or-header-color: var(--or-header-color, var(--or-app-color1, ${unsafeCSS(DefaultColor1)}));    
            --internal-or-header-selected-color: var(--or-header-selected-color, var(--or-app-color4, ${unsafeCSS(DefaultColor4)}));    
            --internal-or-header-text-color: var(--or-header-text-color, var(--or-app-color3, inherit));
            --internal-or-header-height: var(--or-header-height, ${unsafeCSS(DefaultHeaderHeight)});
            --internal-or-header-logo-margin: var(--or-header-logo-margin, 0 40px 0 0);
            --internal-or-header-logo-height: var(--or-header-logo-height, var(--internal-or-header-height, ${unsafeCSS(DefaultHeaderHeight)}));
            --internal-or-header-item-size: var(--or-header-item-size, calc(${unsafeCSS(DefaultHeaderHeight)} - 20px));
            --internal-or-header-drawer-color: var(--or-header-drawer-color, var(--or-app-color2, ${unsafeCSS(DefaultColor2)}));
            --internal-or-header-drawer-text-color: var(--or-header-drawer-text-color, var(--or-app-color3, ${unsafeCSS(DefaultColor3)}));
            --internal-or-header-drawer-item-size: var(--or-header-drawer-item-size, 30px);
            --internal-or-header-drawer-separator-color: var(--or-header-drawer-separator-color, var(--or-app-color5, ${unsafeCSS(DefaultColor5)}));
            
            display: block;
        }
          
        #toolbar-top {
            display: flex;
            padding: 0;
        }
        
        #logo-mobile {
            margin: 8px;
            height: calc(var(--internal-or-header-logo-height) - 16px);
            display: block;
        }

        #logo {
            display: none;
        }
                                        
        #header {
            opacity: 1;
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: var(--internal-or-header-height);
            text-align: center;
            background-color: var(--internal-or-header-color);
            color: var(--internal-or-header-text-color);
            --or-icon-fill: var(--internal-or-header-text-color);
            --or-icon-height: calc(var(--internal-or-header-item-size) - 12px);
            --or-icon-width: calc(var(--internal-or-header-item-size) - 12px);
            z-index: 9999999;
        }

        .shadow {
            -webkit-box-shadow: ${unsafeCSS(DefaultBoxShadowBottom)};
            -moz-box-shadow: ${unsafeCSS(DefaultBoxShadowBottom)};
            box-shadow: ${unsafeCSS(DefaultBoxShadowBottom)};
        }
                
        #drawer {
            z-index: 999999;
            position: absolute;
            top: var(--internal-or-header-height);
            width: 100%;
            height: calc(100% - var(--internal-or-header-height));
            transition: all 300ms ease-in;
            transition-property: -webkit-transform;
            transition-property: transform;
            -webkit-transform: translate3d(0, -100%, 0);
            transform: translate3d(0, -100%, 0);
            background: var(--internal-or-header-drawer-color);
            color: var(--internal-or-header-drawer-text-color);
            --or-icon-fill: var(--internal-or-header-drawer-text-color);
            --or-icon-height: calc(var(--internal-or-header-drawer-item-size) - 10px);
            --or-icon-width: calc(var(--internal-or-header-drawer-item-size) - 10px);
        }
        
        #drawer[opened] {
            -webkit-transform: translate3d(0, 0, 0);
            transform: translate3d(0, 0, 0);
        }
                        
        #drawer > div {
            box-sizing: border-box;
            width: 100%;
            height: 100%;
            padding: 6px 24px;            
            position: relative;
        }
          
        #menu-btn {
            background: none;
            border: none;
            cursor: pointer;
            padding: 0 6px;
            margin: 10px 10px 10px auto;
        }
        
        #desktop-right {
            margin-left: auto;
            display: none;
        }
        
        #desktop-right slot {
            display: flex;
        }

        #mobile-bottom {
            margin-top: 20px;
        }
                        
        #mobile-bottom.has-children {
            border-top: 2px solid var(--internal-or-header-drawer-separator-color);
        }
         
        ::slotted(*) {
            opacity: 0.7;
            cursor: pointer;
            text-decoration: none !important;                       
        }

        ::slotted(*), ::slotted(*:link), ::slotted(*:visited) {
            color: inherit;
        }
        
        ::slotted(*[selected]), ::slotted(*:hover), #desktop-right ::slotted(*) {
            opacity: 1;
        }
                
        #desktop-left ::slotted(*), #desktop-right ::slotted(*) {
            padding: 0 20px;
            font-size: 14px;
        }
        
        #desktop-left ::slotted(*) {
            display: none;
            line-height: calc(var(--internal-or-header-height) - 4px);
        }
        
        #desktop-right ::slotted(*) {
            line-height: var(--internal-or-header-height);
        }
        
        #drawer ::slotted(*) {
            display: block;
            line-height: var(--internal-or-header-drawer-item-size);
            margin: 10px 0;
        }
        
        #desktop-left ::slotted(*[selected]) {
            display: inline-block;
            line-height: var(--internal-or-header-height);
        }
        
        /* Wide layout: when the viewport width is bigger than 780px, layout
        changes to a wide layout. */
        @media (min-width: 780px) {

            #logo {
                margin: var(--internal-or-header-logo-margin);
                height: var(--internal-or-header-logo-height);
                display: block;
            }

            #logo-mobile {
                display: none;
            }

            #menu-btn {
                display: none;
            }

            #drawer {
                display: none;
            }
            
            #desktop-right {
                display: block;
            }
            
            #desktop-left ::slotted(*) {
                display: inline-block;
            }

            #desktop-left ::slotted(*[selected]) {                
                border-bottom: 4px solid var(--internal-or-header-selected-color);
                line-height: calc(var(--internal-or-header-height) - 4px);
            }
        }
`;