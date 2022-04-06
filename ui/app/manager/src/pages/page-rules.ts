import {css, html} from "lit";
import {customElement, property, query} from "lit/decorators.js";
import "@openremote/or-rules";
import {OrRules, RulesConfig} from "@openremote/or-rules";
import {NotificationTargetType, RulesetLang, WellknownAssets} from "@openremote/model";
import {Store} from "@reduxjs/toolkit";
import {Page, PageProvider} from "@openremote/or-app";
import {AppStateKeyed} from "@openremote/or-app";
import manager from "@openremote/core";
import {createSelector} from "reselect";

export interface PageRulesConfig {
    rules: RulesConfig;
}

export function pageRulesProvider(store: Store<AppStateKeyed>, config: PageRulesConfig = PAGE_RULES_CONFIG_DEFAULT): PageProvider<AppStateKeyed> {
    return {
        name: "rules",
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
                    email: [NotificationTargetType.USER, NotificationTargetType.CUSTOM],
                    push: [NotificationTargetType.USER, NotificationTargetType.ASSET],
                }
            }
        },
        descriptors: {
            all: {
                excludeAssets: [
                    WellknownAssets.TRADFRILIGHTASSET,
                    WellknownAssets.TRADFRIPLUGASSET,
                    WellknownAssets.ARTNETLIGHTASSET,
                    WellknownAssets.UNKNOWNASSET
                ]
            }
        },
        json: {}
    }
};

@customElement("page-rules")
export class PageRules extends Page<AppStateKeyed>  {

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

    @query("#rules")
    protected _orRules!: OrRules;

    protected _realmSelector = (state: AppStateKeyed) => state.app.realm || manager.displayRealm;

    protected getRealmState = createSelector(
        [this._realmSelector],
        async (realm) => {
            if (this._orRules) {
                this._orRules.refresh();
            }
        }
    )

    get name(): string {
        return "rules";
    }

    constructor(store: Store<AppStateKeyed>) {
        super(store);
    }

    protected render() {
        return html`
            <or-rules id="rules" .config="${this.config && this.config.rules ? this.config.rules : PAGE_RULES_CONFIG_DEFAULT.rules}"></or-rules>
        `;
    }

    public stateChanged(state: AppStateKeyed) {
        this.getRealmState(state);
    }
}
