import {css, customElement, html, LitElement, property, PropertyValues, unsafeCSS, TemplateResult} from "lit-element";
import {updateMetadata} from "pwa-helpers/metadata";
import "@openremote/or-header";
import "@openremote/or-icon";
import "@openremote/or-translate";
import manager, {DefaultColor2, DefaultColor3, DefaultHeaderHeight} from "@openremote/core";
import {router} from "../index";
import {RootState, store} from "../store";
import "../components/pages/page-map";
import "../components/pages/page-assets";
import "../components/pages/page-rules";
import "../components/or-language/src";
import i18next from "i18next";
import {connect} from "pwa-helpers/connect-mixin";

// Declare require method which we'll use for importing webpack resources (using ES6 imports will confuse typescript parser)
declare function require(name: string): any;

const logoImage = require("../../images/openremote-logo.png");
const logoMobileImage = require("../../images/logo-mobile.png");

@customElement("my-app")
class MyApp extends connect(store)(LitElement) {

    @property({type: String})
    public appTitle = "";

    @property({type: Boolean, attribute: false})
    protected resolved = false;

    @property({type: String, attribute: false})
    protected page = "";

    static get styles() {
        return css`
            :host {
                --or-app-color2: ${unsafeCSS(DefaultColor2)};
                --or-app-color3: #22211f;
                --or-app-color4: #07601F;
                color: ${unsafeCSS(DefaultColor3)};
                fill: ${unsafeCSS(DefaultColor3)};
                font-size: 14px;
            }
                
            .main-content {
                display: flex;
                padding-top: ${unsafeCSS(DefaultHeaderHeight)};
                height: 100vh;
                box-sizing: border-box;
                background-color: var(--or-app-color2);
            }
            
            .page {
                display: flex;
                flex: 1;
                position: relative;
            }
    
            .desktop-hidden {
                display: none !important;
            }
            
            @media only screen and (max-width: 780px){
                .desktop-hidden {
                    display: inline-block !important;
                }
            }
            
            /* HEADER STYLES */
            or-header a > or-icon {
                margin-right: 10px;
            }
        `;
    }

    protected render(): TemplateResult | void {

        if (!this.resolved) {
            return html``;
        }

        return html`
            <or-header logo="${logoImage}" logoMobile="${logoMobileImage}">
                <a slot="desktop-left" ?selected="${this.page === "map"}" @click="${() => router.navigate("map")}"><or-icon icon="map"></or-icon><or-translate value="map"></or-translate></a>
                <a slot="desktop-left" ?selected="${this.page === "assets"}" @click="${() => router.navigate("assets")}"><or-icon icon="sitemap"></or-icon><or-translate value="asset_plural"></or-translate></a>
                <a slot="desktop-left" ?selected="${this.page === "rules"}" @click="${() => router.navigate("rules")}"><or-icon icon="cogs"></or-icon><or-translate value="rules"></or-translate></a>
                <a slot="desktop-right"><or-language></or-language></a>
                <a slot="desktop-right" @click="${() => this.logout()}"><or-translate value="logout"></or-translate></a>
                <a slot="mobile-top" ?selected="${this.page === "map"}" @click="${() => router.navigate("map")}"><or-icon icon="map"></or-icon><or-translate value="map"></or-translate></a>
                <a slot="mobile-top" ?selected="${this.page === "assets"}" @click="${() => router.navigate("assets")}"><or-icon icon="sitemap"></or-icon><or-translate value="asset_plural"></or-translate></a>
                
                <a slot="mobile-top" @click="${() => this.logout()}"><or-icon icon="logout"></or-icon><or-translate value="logout"></or-translate></a>
            </or-header>
            
            <!-- Main content -->
            <main role="main" class="main-content d-none">
                ${this.page === "map" ? html`<page-map class="page"></page-map>` : ``}
                ${this.page === "assets" ? html`<page-assets class="page"></page-assets>` : ``}
                ${this.page === "rules" ? html`<page-rules class="page"></page-rules>` : ``}
            </main>
        `;
    }

    public logout() {
        manager.logout();
    }

    public stateChanged(state: RootState) {
        this.page = state.app!.page;
        this.resolved = state.app.resolved;
    }

    protected updated(changedProps: PropertyValues) {
        super.updated(changedProps);

        if (changedProps.has("page")) {
            const pageTitle = i18next.isInitialized ? i18next.t(this.appTitle) + " - " + i18next.t(this.page) : this.appTitle + " - " + this.page;
            updateMetadata({
                title: pageTitle,
                description: pageTitle
            });
        }
    }
}
