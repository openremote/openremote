import {css} from 'lit-element';

// language=CSS
export const OrHeaderStyle = css`
    or-header a, a:link, a:visited {
        text-decoration: none !important;
    }

    or-header a, a:link, a:visited {
        display: inline-block;
        color: rgba(255,255,255, 0.8);
        line-height: 60px;
        padding: 0 5px;
        text-transform: uppercase;
        font-size: 14px;
        text-decoration: none !important;
        cursor: pointer;
      }
      or-header a[selected]{
        color: var(--app-light-text-color);
        font-weight: 400;
      }
      
     or-header a[selected] svg path{
        fill: var(--app-light-text-color);
     }
     
     [slot="mobile-top"] > a,
     [slot="mobile-bottom"] > a {
        color: var(--app-drawer-text-color);
        display: block;
     }
     [slot="mobile-top"] > a[selected] svg path,
     [slot="mobile-bottom"] > a[selected] svg path {
        fill: var(--app-drawer-text-color);
     }
     
     [slot="mobile-top"] > a[selected],
     [slot="mobile-bottom"] > a[selected] {
        color: var(--app-drawer-text-color);
        /*border-bottom: 4px solid #A3BDD0;*/
     }
     
    
      or-header .svg-inline--fa {
        height: 14px;
        width: 14px;
        margin: -2px 4px;
      }

     [slot="desktop-right"] {
        margin-left: auto;
     }
     
     [slot="desktop-left"] svg path {
        fill: rgba(255,255,255, 0.8);
     }
      
     [slot="desktop-left"] > a {
        display: none;
        line-height: 60px;
        padding: 0 24px;
     }
     
    
     [slot="desktop-left"] > a[selected] {
        display:inline-block;
     }
     
    @media (min-width: 460px) {
     [slot="desktop-left"] > a {
        display:inline-block;
        line-height: 56px;
     }
     
     [slot="desktop-left"] > a[selected] {
        border-bottom: 4px solid var(--app-header-selected-border);
     }
`;
