import {html, LitElement, property, css, query, customElement, PropertyValues} from "lit-element";
import {store} from "../../store";
import {connect} from "pwa-helpers/connect-mixin";
import "@openremote/or-rules";
import {RulesConfig} from "@openremote/or-rules";
import {RulesetLang, AssetType, AttributeType} from "@openremote/model";

const rulesConfig: RulesConfig = {
    controls: {
        allowedLanguages: [RulesetLang.JSON]
    },
    descriptors: {
        all: {
            excludeAssets: [
                AssetType.BUILDING.type,
                AssetType.CITY.type,
                AssetType.AREA.type,
                AssetType.FLOOR.type,
            ],
            assets: {
                "*": {
                    excludeAttributes: [
                        AttributeType.LOCATION.attributeName
                    ]
                }
            }
        }
    },
    json: {
        rule: {
            reset: {
                timer: "1h"
            }
        }
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
