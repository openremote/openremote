import {css, customElement, html} from "lit-element";
import "@openremote/or-rules";
import {RulesConfig} from "@openremote/or-rules";
import {AssetType, RulesetLang} from "@openremote/model";
import {AppStateKeyed, Page} from "../index";
import {EnhancedStore} from "@reduxjs/toolkit";

export function pageRulesProvider<S extends AppStateKeyed>(store: EnhancedStore<S>) {
    return {
        routes: [
            "rules",
            "rules/:id"
        ],
        pageCreator: () => {
            return new PageRules(store);
        }
    };
}

const rulesConfig: RulesConfig = {
    controls: {
        allowedLanguages: [RulesetLang.JSON, RulesetLang.FLOW]
    },
    descriptors: {
        all: {
            excludeAssets: [
                AssetType.BUILDING.type,
                AssetType.CITY.type,
                AssetType.AREA.type,
                AssetType.FLOOR.type,
                AssetType.AGENT.type
            ]
        }
    },
    json: {

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

    constructor(store: EnhancedStore<S>) {
        super(store);
    }

    protected render() {
        return html`
            <or-rules .config="${rulesConfig}"></or-rules>
        `;
    }

    public stateChanged(state: S) {
    }
}
