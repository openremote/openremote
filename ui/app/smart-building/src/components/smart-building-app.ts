import {css, html, LitElement, property, PropertyValues} from "lit-element";
import {connect} from "pwa-helpers/connect-mixin";
import {setPassiveTouchGestures} from "@polymer/polymer/lib/utils/settings";
import {installMediaQueryWatcher} from "pwa-helpers/media-query";
import {installRouter} from "pwa-helpers/router";
import {updateMetadata} from "pwa-helpers/metadata";
import {icon} from "@fortawesome/fontawesome-svg-core";
import {faMap, faSignOutAlt, faTable} from "@fortawesome/free-solid-svg-icons";
import "@openremote/or-header";
import "@openremote/or-bottom-navigation";

import {OrHeaderStyle} from "@openremote/or-header/dist/or-header-style";
import { localize } from "../localize-mixin";
import { i18next } from "../i18next";

// This element is connected to the Redux store.
import {store} from "../store";

// These are the actions needed by this element.
import {navigate, updateDrawerState, updateOffline} from "../actions/app";


// Declare MANAGER_URL
declare var MANAGER_URL: string;

class SmartBuildingApp extends connect(store)(localize(i18next)(LitElement)) {

    @property({type: String})
    appTitle = '';

    @property({type: String})
    _page = "scenes";

    constructor() {
        super();
        // To force all event listeners for gestures to be passive.
        // See https://www.polymer-project.org/3.0/docs/devguide/settings#setting-passive-touch-gestures
        setPassiveTouchGestures(true);
    }

    protected invalidate() {
        this.requestUpdate();
    }

    protected render() {
        return html`
            <or-header>
                <div slot="desktop-left">
                    <a ?selected="${this._page === 'scenes'}" href="#scenes">${i18next.t('scenes')}</a>
                    <a ?selected="${this._page === 'messages'}" href="#scenes">${i18next.t('receivedMessages')}</a>
                    <a ?selected="${this._page === 'contact'}" href="#scenes">${i18next.t('sendMessage')}</a>
                    <a ?selected="${this._page === 'languages'}" href="#scenes">${i18next.t('selectLanguage')}</a>
                </div>
                <div slot="desktop-right">
                    <a @click="${this.logout}">${icon(faSignOutAlt).node.item(0)} ${i18next.t('logOut')}</a>
                </div>
                 <div slot="mobile-top">
                    <a ?selected="${this._page === 'scenes'}" href="#scenes">${i18next.t('scenes')}</a>
                    <a ?selected="${this._page === 'messages'}" href="#scenes">${i18next.t('receivedMessages')}</a>
                    <a ?selected="${this._page === 'contact'}" href="#scenes">${i18next.t('sendMessage')}</a>
                    <a ?selected="${this._page === 'languages'}" href="#scenes">${i18next.t('selectLanguage')}</a>
                    <a @click="${this.logout}">${icon(faSignOutAlt).node.item(0)} ${i18next.t('logOut')}</a>
                </div>
            </or-header>
                          
            <!-- Main content -->
            <main role="main" class="main-content">
                <page-scenes class="page" ?active="${this._page === "scenes"}"></page-scenes>
                <page-messages class="page" ?active="${this._page === "messages"}"></page-messages>
                <page-contact class="page" ?active="${this._page === "contact"}"></page-contact>
                <page-languages class="page" ?active="${this._page === "languages"}"></page-languages>
            </main>
            
            <or-bottom-navigation>
                <or-navigation-item icon="tv" text="TV"></or-navigation-item>
                <span class="mdc-bottom-navigation__list-item">
                  <span class="material-icons mdc-bottom-navigation__list-item__icon">tv</span>
                  <span class="mdc-bottom-navigation__list-item__text">TV</span>
                </span>
                <span class="mdc-bottom-navigation__list-item">
                      <span class="material-icons mdc-bottom-navigation__list-item__icon">music_note</span>
                      <span class="mdc-bottom-navigation__list-item__text">Music</span>
                    </span>
                <span class="mdc-bottom-navigation__list-item">
                      <span class="material-icons mdc-bottom-navigation__list-item__icon">book</span>
                      <span class="mdc-bottom-navigation__list-item__text">Books</span>
                    </span>
                <span class="mdc-bottom-navigation__list-item">
                  <span class="material-icons mdc-bottom-navigation__list-item__icon">
                    <svg style="width:24px;height:24px" viewBox="0 0 24 24">
                      <path fill="#000000" d="M20,11H4V8H20M20,15H13V13H20M20,19H13V17H20M11,19H4V13H11M20.33,4.67L18.67,3L17,4.67L15.33,3L13.67,4.67L12,3L10.33,4.67L8.67,3L7,4.67L5.33,3L3.67,4.67L2,3V19A2,2 0 0,0 4,21H20A2,2 0 0,0 22,19V3L20.33,4.67Z" />
                    </svg>
                  </span>
                  <span class="mdc-bottom-navigation__list-item__text">Newstand</span>
                </span>            
            </or-bottom-navigation>
        `;
    }

    protected firstUpdated() {
        installRouter((location) => store.dispatch(navigate(decodeURIComponent(location.pathname))));
        installMediaQueryWatcher(`(min-width: 460px)`,
            () => store.dispatch(updateDrawerState(false)));
    }

    protected updated(changedProps: PropertyValues) {
        if (changedProps.has("_page")) {
            const pageTitle = this.appTitle + " - " + this._page;
            updateMetadata({
                title: pageTitle,
                description: pageTitle
                // This object also takes an image property, that points to an img src.
            });
        }
    }

    protected logout() {

    }

    static styles = css`
        :host {
            --app-header-background-color: #bcbcbc;
        }
    `;
    //static styles = OrHeaderStyle;
  //   static styles = css`
  //   :host {
  //     display: block;
  //
  //     --app-drawer-width: 256px;
  //
  //     --app-primary-color: #E91E63;
  //     --app-secondary-color: #293237;
  //     --app-dark-text-color: var(--app-secondary-color);
  //     --app-light-text-color: white;
  //     --app-section-even-color: #f7f7f7;
  //     --app-section-odd-color: white;
  //
  //     --app-header-background-color: white;
  //     --app-header-text-color: var(--app-dark-text-color);
  //     --app-header-selected-color: var(--app-primary-color);
  //
  //     --app-drawer-background-color: var(--app-secondary-color);
  //     --app-drawer-text-color: var(--app-light-text-color);
  //     --app-drawer-selected-color: #78909C;
  //   }
  //
  //   app-header {
  //     position: fixed;
  //     top: 0;
  //     left: 0;
  //     width: 100%;
  //     text-align: center;
  //     background-color: var(--app-header-background-color);
  //     color: var(--app-header-text-color);
  //     border-bottom: 1px solid #eee;
  //   }
  //
  //   .toolbar-top {
  //     background-color: var(--app-header-background-color);
  //   }
  //
  //   [main-title] {
  //     font-family: "Pacifico";
  //     text-transform: lowercase;
  //     font-size: 30px;
  //     /* In the narrow layout, the toolbar is offset by the width of the
  //     drawer button, and the text looks not centered. Add a padding to
  //     match that button */
  //     padding-right: 44px;
  //   }
  //
  //   .toolbar-list {
  //     display: none;
  //   }
  //
  //   .toolbar-list > a {
  //     display: inline-block;
  //     color: var(--app-header-text-color);
  //     text-decoration: none;
  //     line-height: 30px;
  //     padding: 4px 24px;
  //   }
  //
  //   .toolbar-list > a[selected] {
  //     color: var(--app-header-selected-color);
  //     border-bottom: 4px solid var(--app-header-selected-color);
  //   }
  //
  //   .menu-btn {
  //     background: none;
  //     border: none;
  //     fill: var(--app-header-text-color);
  //     cursor: pointer;
  //     height: 44px;
  //     width: 44px;
  //   }
  //
  //   .drawer-list {
  //     box-sizing: border-box;
  //     width: 100%;
  //     height: 100%;
  //     padding: 24px;
  //     background: var(--app-drawer-background-color);
  //     position: relative;
  //   }
  //
  //   .drawer-list > a {
  //     display: block;
  //     text-decoration: none;
  //     color: var(--app-drawer-text-color);
  //     line-height: 40px;
  //     padding: 0 24px;
  //   }
  //
  //   .drawer-list > a[selected] {
  //     color: var(--app-drawer-selected-color);
  //   }
  //
  //   /* Workaround for IE11 displaying <main> as inline */
  //   main {
  //     display: block;
  //   }
  //
  //   .main-content {
  //     padding-top: 64px;
  //     min-height: 100vh;
  //   }
  //
  //   .page {
  //     display: none;
  //   }
  //
  //   .page[active] {
  //     display: block;
  //   }
  //
  //   footer {
  //     padding: 24px;
  //     background: var(--app-drawer-background-color);
  //     color: var(--app-drawer-text-color);
  //     text-align: center;
  //   }
  //
  //   /* Wide layout: when the viewport width is bigger than 460px, layout
  //     changes to a wide layout */
  //   @media (min-width: 460px) {
  //     .toolbar-list {
  //       display: block;
  //     }
  //
  //     .menu-btn {
  //       display: none;
  //     }
  //
  //     .main-content {
  //       padding-top: 107px;
  //     }
  //
  //     /* The drawer button isn't shown in the wide layout, so we don't
  //     need to offset the title */
  //     [main-title] {
  //       padding-right: 0px;
  //     }
  //   }
  // `;
}

window.customElements.define("smart-building-app", SmartBuildingApp);
