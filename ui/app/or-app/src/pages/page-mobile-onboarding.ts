import {css, customElement, html, property} from "lit-element";
import {AppStateKeyed, Page} from "../index";
import {EnhancedStore} from "@reduxjs/toolkit";
import manager from "@openremote/core";

export interface ConsoleProvider {
    name: string;
    action: string;
}
export interface OnboardingPage {
    title: string;
    description?: string;
    enableProviders?: ConsoleProvider[];
    image?: HTMLTemplateElement | string;
    type: "default" | "bottom-image";
    next?: string;
}

export interface OnboardingConfig {
    pages: OnboardingPage[],
    redirect: string;
}

export function pageMobileOnboardingProvider<S extends AppStateKeyed>(store: EnhancedStore<S>, config?: OnboardingConfig) {
    return {
        routes: [
            "onboarding"
        ],
        pageCreator: () => {
            const page = new PageMobileOnboarding(store);
            if(config) page.config = config;
            return page;
        }
    };
}

@customElement("page-mobile-onboarding")
class PageMobileOnboarding<S extends AppStateKeyed> extends Page<S> {

    static get styles() {
        // language=CSS
        return css`
            :host {
                flex: 1;
                width: 100%; 
                flex-direction: column;           
            }
            
            .page-container {
                width: 100vw;
                height: 100vh;
                flex-direction: column;   
                display: none;
             
            }

            .page-container.active {
                display: flex;
            }

            .page-content {
                flex: 1;
                display: flex;
                flex-direction: column;    
                align-items: center;
                justify-content: center;    
            }

            .next-button {
                cursor: pointer;
                margin: 0 20px 20px auto;
                width: 42px;
                height: 42px;
                border-radius: 50%;
                background-color: var(--or-app-color4);
                justify-content: center;
                align-items: center;
                display: flex;
                --or-icon-fill: var(--or-app-color2, #F9F9F9);
            }
        `;
    }

    constructor(store: EnhancedStore<S>) {
        super(store);

       

    }

    public connectedCallback() {
        super.connectedCallback();
        if (localStorage.getItem("completedOnboarding") !== null) {
            window.location.href = this.config.redirect;
        }
    }

    public stateChanged(state: S) {
    }

    @property()
    public pageIndex: number = 0;
  
    @property()
    public config?: OnboardingConfig;

    protected render() {
        return html`
            ${this.config.pages.map((page, index) => {
                return html`
                    <div class="page-container ${this.pageIndex === index ? "active" :""}">
                        <div class="page-content">
                            ${page.type === "default" ? html`
                                <div><img src="${page.image}" /></div> 
                                <h1>${page.title}</h1>
                                <p>${page.description}</p> 
                            ` : ``}

                            ${page.type === "bottom-image" ? html`
                                <h1>${page.title}</h1>
                                <p>${page.description}</p>
                                <div><img src="${page.image}" /></div>  
                            ` : ``}
                        </div>
                     
                        <div class="next-button" @click="${() => this.nextPage(index)}">
                            <or-icon icon="chevron-right"></or-icon>
                        </div>
                    </div>
                `
            })}
        `;
    }

    private async enableProviders() {
        if(this.config.pages[this.pageIndex-1].enableProviders) {
            this.config.pages[this.pageIndex-1].enableProviders.map(provider => {
                manager.console.sendProviderMessage({provider: provider.name, action: provider.action}, true)
            })
        }
    }
    
    private nextPage(index) {
        if(this.config.pages.length-1 === index) {
            window.location.href = this.config.redirect;
            window.localStorage.setItem("completedOnboarding", "1");
        } else {
            this.pageIndex = this.pageIndex+1
        }
        this.enableProviders();
    }
}
