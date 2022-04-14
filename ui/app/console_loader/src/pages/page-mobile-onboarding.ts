import {css, html} from "lit";
import {customElement, property} from "lit/decorators.js";
import {Page, PageProvider, AppStateKeyed} from "@openremote/or-app";
import {Store} from "@reduxjs/toolkit";
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

export function pageMobileOnboardingProvider(store: Store<AppStateKeyed>, config?: OnboardingConfig): PageProvider<AppStateKeyed> {
    return {
        name: "onboarding",
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
export class PageMobileOnboarding extends Page<AppStateKeyed> {

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
                text-align: center;
                padding: 20px;
            }

            .next-button {
                cursor: pointer;
                margin: 0 20px 20px auto;
                width: 42px;
                height: 42px;
                border-radius: 50%;
                background-color: var(--or-console-primary-color, --or-app-color4);
                justify-content: center;
                align-items: center;
                display: flex;
                --or-icon-fill: var(--or-app-color2, #F9F9F9);
            }
        `;
    }

    @property()
    protected active: boolean = false;

    get name(): string {
        return "mobile-onboarding";
    }

    constructor(store: Store<AppStateKeyed>) {
        super(store);
    }

    public connectedCallback() {
        super.connectedCallback();
        if (localStorage.getItem("completedOnboarding") !== null) {
            window.location.href = manager.consoleAppConfig ? manager.consoleAppConfig.url : this.config.redirect;
        } else {
            this.active = true;
        }
    }

    public stateChanged(state: AppStateKeyed) {
    }

    @property()
    public pageIndex: number = 0;
  
    @property()
    public config?: OnboardingConfig;

    protected render() {
        if(this.active) {
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
    }

    private enableProviders(): Promise<any> {
        if(this.config.pages[this.pageIndex].enableProviders) {
            return Promise.all(this.config.pages[this.pageIndex].enableProviders.map(provider => {
                return manager.console.sendProviderMessage({provider: provider.name, action: provider.action, consoleId: manager.console.registration.id}, true);
            }));
        }
        return Promise.resolve();
    }

    private nextPage(index) {
        this.enableProviders().then(() => {
            if (this.config.pages.length - 1 === index) {
                window.location.href = manager.consoleAppConfig ? manager.consoleAppConfig.url : this.config.redirect;
                window.localStorage.setItem("completedOnboarding", "1");
            } else {
                this.pageIndex = this.pageIndex + 1
            }
        });
    }
}
