import {css, customElement, html, property} from "lit-element";
import "@openremote/or-rules";
import {RulesConfig} from "@openremote/or-rules";
import {AssetType, RulesetLang, NotificationTargetType} from "@openremote/model";
import {AppStateKeyed, Page} from "../index";
import {EnhancedStore} from "@reduxjs/toolkit";

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
            hideNotificationTargetType: {
                email: [NotificationTargetType.TENANT, NotificationTargetType.ASSET],
                push: [NotificationTargetType.TENANT, NotificationTargetType.CUSTOM],
            }
        },
        descriptors: {
            all: {
                excludeAssets: [
                    AssetType.BUILDING.type!,
                    AssetType.CITY.type!,
                    AssetType.AREA.type!,
                    AssetType.FLOOR.type!,
                    AssetType.AGENT.type!
                ]
            }
        },
        json: {}
    }
};

@customElement("page-rules")
class PageRules<S extends AppStateKeyed> extends Page<S>  {

    @property()
    public config?: PageRulesConfig;
    
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
