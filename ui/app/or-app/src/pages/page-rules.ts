import {css, customElement, html, property} from "lit-element";
import "@openremote/or-rules";
import {ActionTargetType, RulesConfig} from "@openremote/or-rules";
import {NotificationTargetType, RulesetLang, WellknownAssets} from "@openremote/model";
import {EnhancedStore} from "@reduxjs/toolkit";
import {AppStateKeyed} from "../app";
import {Page} from "../types";

export interface PageRulesConfig {
    rules: RulesConfig;
}

export function pageRulesProvider<S extends AppStateKeyed>(store: EnhancedStore<S>, config: PageRulesConfig = PAGE_RULES_CONFIG_DEFAULT) {
    return {
        routes: [
            "rules",
            "rules/:id"
        ],
        pageCreator: () => {
            const page = new PageRules(store);
            if (config) {
                page.config = config;
            }
            return page;
        }
    };
}

export const PAGE_RULES_CONFIG_DEFAULT: PageRulesConfig = {

    rules: {
        controls: {
            allowedLanguages: [RulesetLang.JSON, RulesetLang.FLOW, RulesetLang.GROOVY],
            allowedActionTargetTypes: {
                actions: {
                    wait: [],
                    attribute: [],
                    email: [ActionTargetType.USER, ActionTargetType.CUSTOM],
                    push: [NotificationTargetType.USER, NotificationTargetType.ASSET],
                }
            }
        },
        descriptors: {
            all: {
                excludeAssets: [
                    WellknownAssets.CITYASSET
                ]
            }
        },
        json: {}
    }
};

@customElement("page-rules")
class PageRules<S extends AppStateKeyed> extends Page<S>  {

    static get styles() {
        // language=CSS
        return css`
            :host {
            
            }
            
            or-rules {
                width: 100%;
                height: 100%;
            }
        `;
    }

    @property()
    public config?: PageRulesConfig;

    get name(): string {
        return "rules";
    }

    constructor(store: EnhancedStore<S>) {
        super(store);
    }

    protected render() {
        return html`
            <or-rules .config="${this.config && this.config.rules ? this.config.rules : PAGE_RULES_CONFIG_DEFAULT.rules}"></or-rules>
        `;
    }

    public stateChanged(state: S) {
    }
}
