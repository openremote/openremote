import {OrHeaderStyle} from "@openremote/or-header/src/or-header-style";
import {css, html, LitElement, property, customElement} from "lit-element";

const menuIcon = html`<svg height="24" viewBox="0 0 24 24" width="24"><path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"></path></svg>`;

const menu = html`<svg version="1.1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" xmlns:xlink="http://www.w3.org/1999/xlink" enable-background="new 0 0 24 24">
  <g>
    <path d="M24,3c0-0.6-0.4-1-1-1H1C0.4,2,0,2.4,0,3v2c0,0.6,0.4,1,1,1h22c0.6,0,1-0.4,1-1V3z"/>
    <path d="M24,11c0-0.6-0.4-1-1-1H1c-0.6,0-1,0.4-1,1v2c0,0.6,0.4,1,1,1h22c0.6,0,1-0.4,1-1V11z"/>
    <path d="M24,19c0-0.6-0.4-1-1-1H1c-0.6,0-1,0.4-1,1v2c0,0.6,0.4,1,1,1h22c0.6,0,1-0.4,1-1V19z"/>
  </g>
</svg>
`;

const close = html`<svg version="1.1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 21.9 21.9" xmlns:xlink="http://www.w3.org/1999/xlink" enable-background="new 0 0 21.9 21.9">
  <path d="M14.1,11.3c-0.2-0.2-0.2-0.5,0-0.7l7.5-7.5c0.2-0.2,0.3-0.5,0.3-0.7s-0.1-0.5-0.3-0.7l-1.4-1.4C20,0.1,19.7,0,19.5,0  c-0.3,0-0.5,0.1-0.7,0.3l-7.5,7.5c-0.2,0.2-0.5,0.2-0.7,0L3.1,0.3C2.9,0.1,2.6,0,2.4,0S1.9,0.1,1.7,0.3L0.3,1.7C0.1,1.9,0,2.2,0,2.4  s0.1,0.5,0.3,0.7l7.5,7.5c0.2,0.2,0.2,0.5,0,0.7l-7.5,7.5C0.1,19,0,19.3,0,19.5s0.1,0.5,0.3,0.7l1.4,1.4c0.2,0.2,0.5,0.3,0.7,0.3  s0.5-0.1,0.7-0.3l7.5-7.5c0.2-0.2,0.5-0.2,0.7,0l7.5,7.5c0.2,0.2,0.5,0.3,0.7,0.3s0.5-0.1,0.7-0.3l1.4-1.4c0.2-0.2,0.3-0.5,0.3-0.7  s-0.1-0.5-0.3-0.7L14.1,11.3z"/>
</svg>`;

@customElement("or-header")
class OrHeader extends LitElement {

    @property({type: Boolean})
    private _drawerOpened = false;

    @property({type: String})
    private logo = "";

    static styles = css`
                  #app-header {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 60px;
                    text-align: center;
                    background-color: var(--app-header-background-color);
                    color: var(--app-header-text-color);
                    z-index: 9999999;
                    box-shadow: 0px 0px 5px 0px rgba(0,0,0,0.57);
                  }
                  
                  .toolbar-top {
                    display: flex;
                    background-color: var(--app-header-background-color);
                    padding: 0;
                  }
                 
                  #logo {
                    margin: var(--header-logo-margin, 0);
                    height: var(--header-logo-height, 60px);
                    display: block;
                  }
                  
                  slot[name="desktop-right"] {
                        display: none;
                  }
                  
                  slot[name="desktop-right"]{
                    margin-left: auto;
                    margin-right: 20px;
                  }
                                  
                  app-drawer {
                    z-index: 999999;
                    position: absolute;
                    top: 60px;
                    width: 100%;
                    height: calc(100% - 60px);
                    transition: all 300ms ease-in;
                    transition-property: -webkit-transform;
                    transition-property: transform;
                    -webkit-transform: translate3d(0, -100%, 0);
                    transform: translate3d(0, -100%, 0);
                  }
                  
                  app-drawer[opened] {
                    -webkit-transform: translate3d(0, 0, 0);
                    transform: translate3d(0, 0, 0);
                  }
                  
                 .menu-btn {
                    background: none;
                    border: none;
                    fill: var(--app-header-text-color);
                    cursor: pointer;
                    height: 40px;
                    width: 40px;
                    margin: 10px 0 10px auto;
                  }
                  
                  .menu-btn svg {
                    width: 24px;
                    height: 24px;
                  }
                  .menu-btn svg,
                  .menu-btn svg path {
                    fill: #FFF;
                  }
                  
                  .drawer-list {
                    box-sizing: border-box;
                    width: 100%;
                    height: 100%;
                    padding: 24px;
                    background: var(--app-drawer-background-color);
                    position: relative;
                  }
                   slot[name="desktop-right"] {
                    margin-left: auto;
                   }
                  /* Wide layout: when the viewport width is bigger than 460px, layout
                  changes to a wide layout. */
                  @media (min-width: 460px) {
                    .menu-btn {
                      display: none;
                    }
                    slot[name="desktop-left"],
                    slot[name="desktop-right"] {
                        display: block;
                    }
                  }
    `;

    protected render() {
        return html`
           <!-- Header -->
            <div id="app-header">
                <div class="toolbar-top">
                        <div><img id="logo" src="${this.logo}" /></div>
                        <!-- This gets hidden on a small screen-->
                        
                        <nav class="toolbar-list">
                         <slot name="desktop-left"></slot>
                        </nav>
                    
                         <slot name="desktop-right"></slot>
                    
                        <button class="menu-btn" title="Menu" @click="${this._menuButtonClicked}">${this._drawerOpened ? close : menu}</button>
                    </div>
            </div>
            
            <app-drawer ?opened="${this._drawerOpened}" @click="${this._close}">
                <nav class="drawer-list">
                     <slot name="mobile-top"></slot>
                     
                     <slot name="mobile-bottom"></slot>
                </nav>
            </app-drawer>
            
        `;
    }

    private _close() {
        this._drawerOpened = false;
    }

    private _menuButtonClicked() {
        this._drawerOpened = !this._drawerOpened;
    }
}
