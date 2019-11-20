import {customElement, html, LitElement, property, TemplateResult, css, unsafeCSS} from "lit-element";
import {store} from "../../store";
import {connect} from "pwa-helpers/connect-mixin";
import "@openremote/or-asset-tree";
import "@openremote/or-asset-viewer";
import {ViewerConfig} from "@openremote/or-asset-viewer";
import {OrAssetTreeSelectionChangedEvent} from "@openremote/or-asset-tree";
import {DefaultBoxShadow} from "@openremote/core";
import moment from "moment";
import {router} from "../../index";

// language=CSS
const style = css`
    
    or-asset-tree {
        align-items: stretch;
        z-index: 1;
    }
    
    .hideMobile {
        display: none;
    }
        
    or-asset-viewer {
        align-items: stretch;
        z-index: 0;
    }
    
    @media only screen and (min-width: 768px){
        or-asset-tree {
            width: 300px;
            min-width: 300px;
            box-shadow: ${unsafeCSS(DefaultBoxShadow)} 
        }
        
        .hideMobile {
            display: flex;
        }
        
        or-asset-viewer,
        or-asset-viewer.hideMobile {
            display: initial;
        }
     
      
        
    }
`;

const viewerConfig: ViewerConfig = {
    default: {
        panels: {
            "attributes": {
                exclude: ["location"]
            }
        }
    },
    assetTypes: {
        "urn:openremote:asset:residence": {
            panels: {
                "attributes": {
                    exclude: ["dayScene", 
                              "daySceneEnabledFRIDAY", 
                              "daySceneEnabledMONDAY", 
                              "daySceneEnabledTUESDAY", 
                              "daySceneEnabledWEDNESDAY", 
                              "daySceneEnabledTHURSDAY",
                              "daySceneEnabledSATURDAY",
                              "daySceneEnabledSUNDAY",
                              "daySceneTimeFRIDAY",
                              "daySceneTimeMONDAY",
                              "daySceneTimeTUESDAY",
                              "daySceneTimeWEDNESDAY",
                              "daySceneTimeTHURSDAY",
                              "daySceneTimeSATURDAY",
                              "daySceneTimeSUNDAY",
                              "disableSceneTimer",
                              "enableSceneTimer",
                              "eveningScene",
                              "dayScene",
                              "eveningSceneEnabledFRIDAY",
                              "eveningSceneEnabledMONDAY",
                              "eveningSceneEnabledTUESDAY",
                              "eveningSceneEnabledWEDNESDAY",
                              "eveningSceneEnabledTHURSDAY",
                              "eveningSceneEnabledSATURDAY",
                              "eveningSceneEnabledSUNDAY",
                              "eveningSceneTimeFRIDAY",
                              "eveningSceneTimeMONDAY",
                              "eveningSceneTimeTUESDAY",
                              "eveningSceneTimeWEDNESDAY",
                              "eveningSceneTimeTHURSDAY",
                              "eveningSceneTimeSATURDAY",
                              "eveningSceneTimeSUNDAY",
                              "morningScene",
                              "nightscene",
                              "morningSceneEnabledFRIDAY",
                              "morningSceneEnabledMONDAY",
                              "morningSceneEnabledTUESDAY",
                              "morningSceneEnabledWEDNESDAY",
                              "morningSceneEnabledTHURSDAY",
                              "morningSceneEnabledSATURDAY",
                              "morningSceneEnabledSUNDAY",
                              "morningSceneTimeFRIDAY",
                              "morningSceneTimeMONDAY",
                              "morningSceneTimeTUESDAY",
                              "morningSceneTimeWEDNESDAY",
                              "morningSceneTimeTHURSDAY",
                              "morningSceneTimeSATURDAY",
                              "morningSceneTimeSUNDAY",
                              "nightSceneEnabledFRIDAY",
                              "nightSceneEnabledMONDAY",
                              "nightSceneEnabledTUESDAY",
                              "nightSceneEnabledWEDNESDAY",
                              "nightSceneEnabledTHURSDAY",
                              "nightSceneEnabledSATURDAY",
                              "nightSceneEnabledSUNDAY",
                              "nightSceneTimeFRIDAY",
                              "nightSceneTimeMONDAY",
                              "nightSceneTimeTUESDAY",
                              "nightSceneTimeWEDNESDAY",
                              "nightSceneTimeTHURSDAY",
                              "nightSceneTimeSATURDAY",
                              "nightSceneTimeSUNDAY",
                              "vacationUntil",
                              "ventilationLevel",
                              "location"]
                }
            }
        }
    }
};

@customElement("page-assets")
class PageAssets extends connect(store)(LitElement)  {

    static get styles() {
        return style;
    }

    @property()
    protected _assetId;

    constructor() {
        super();
        this.addEventListener(OrAssetTreeSelectionChangedEvent.NAME, (evt) => this._onTreeSelectionChanged(evt));
    }

    protected render(): TemplateResult | void {
        const selectedIds = [this._assetId];
        return html`
              <or-asset-tree class="${this._assetId ? "hideMobile" : ""}" .selectedIds="${selectedIds}"></or-asset-tree>
              <or-asset-viewer class="${!this._assetId ? "hideMobile" : ""}" .config="${viewerConfig}" .assetId="${this._assetId}"></or-asset-viewer>
        `;
    }

    stateChanged(state) {
        if(state.app.activeAsset && this._assetId !== state.app.activeAsset) {
            this._assetId = state.app.activeAsset
        } else if (!state.app.activeAsset) {
            this._assetId = null;
        }
    }

    protected _onTreeSelectionChanged(event: OrAssetTreeSelectionChangedEvent) {
        const nodes = event.detail;
        if(nodes[0]){
            router.navigate('assets/'+nodes[0].asset.id);
        }
    }
}
