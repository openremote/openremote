import {css, customElement, html, LitElement} from "lit-element";
import {store} from "../../store";
import {connect} from "pwa-helpers/connect-mixin";
import "@openremote/or-rules";
import {RulesConfig} from "@openremote/or-rules";
import {AssetType, AttributeType, RulesetLang} from "@openremote/model";

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
class PageRules extends connect(store)(LitElement)  {

    static get styles() {
        return css`
            :host {
            
            }
            
            or-rules {
                width: 100%;
                height: 100%;
            }
        `;
    }

    protected render() {
        return html`
            <or-rules .config="${rulesConfig}"></or-rules>
        `;
    }
}
