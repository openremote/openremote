import {css, html} from "lit";
import {customElement, property} from "lit/decorators.js";
import {AppStateKeyed} from "@openremote/or-app/dist/app";
import {Page, PageProvider} from "@openremote/or-app/dist/types";
import {EnhancedStore} from "@reduxjs/toolkit";

export interface SplashConfig {
    redirect: string;
    interval?: number;
    logoMobile?: HTMLTemplateElement | string;
}

export function pageMobileSplashProvider<S extends AppStateKeyed>(store: EnhancedStore<S>, config?: SplashConfig): PageProvider<S> {
    return {
        name: "splash",
        routes: [
            "splash"
        ],
        pageCreator: () => {
            const page = new PageMobileSplash(store);
            if(config) page.config = config;
            return page;
        }
    };
}

@customElement("page-mobile-splash")
class PageMobileSplash<S extends AppStateKeyed> extends Page<S> {

    static get styles() {
        // language=CSS
        return css`
            :host {
                flex: 1;
                width: 100%;     
                align-items: center;
                justify-content: center;       
            }
        `;
    }

    get name(): string {
        return "mobile-splash";
    }

    constructor(store: EnhancedStore<S>) {
        super(store);
    }

    public connectedCallback() {
        super.connectedCallback();
        if (this.config) {
            setTimeout(() => {
                window.location.href = this.config!.redirect;
            }, this.config.interval ? this.config.interval : 3000);
        }
    }
    

    public stateChanged(state: S) {
    }


    @property()
    public config?: SplashConfig;

    protected render() {
        return html`
            <div><img id="logo-mobile" src="${this.config?.logoMobile}" /></div>
        `;
    }
}
