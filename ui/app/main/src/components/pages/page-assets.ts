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
    historyConfig: {
        table: {
            attributeNames: {
                "gunshotEvent": {
                    columns: [
                        {
                            header: "area",
                            type: "prop",
                            path: "$.content.area",
                            styles: {
                                width: "20%"
                            }
                        },
                        {
                            header: "location",
                            type: "prop",
                            path: "$.content.triggers[0].position.coordinates",
                            styles: {
                                width: "30%"
                            },
                            contentProvider: (datapoint, value, config) => {
                                if (Array.isArray(value)) {
                                    return "[" + Number(value[0]).toLocaleString(undefined, {minimumFractionDigits: 5, maximumFractionDigits: 5}) + ", " + Number(value[1]).toLocaleString(undefined, {minimumFractionDigits: 5, maximumFractionDigits: 5}) + "]"
                                }
                            }
                        },
                        {
                            header: "intensity",
                            type: "prop",
                            path: "$.content.triggers[0].intensity",
                            styles: {
                                width: "25%"
                            },
                            contentProvider: (datapoint, value, config) => {
                                return Number(value).toLocaleString(undefined, {maximumFractionDigits: 0})
                            }
                        },
                        {
                            header: "timestamp",
                            type: "prop",
                            path: "$.content.time",
                            styles: {
                                width: "25%"
                            },
                            contentProvider: (datapoint, value, config) => {
                                if (value) {
                                    return moment(value).format("L HH:mm:ss");
                                }
                            }
                        }
                    ]
                }
            }
        }
    },
    default: {
        panels: {
            "attributes": {
                exclude: ["location"]
            }
        }
    },
    assetTypes: {
        "urn:openremote:asset:cdpstrijp:topic": {
            panels: {
                "attributes": {
                    exclude: ["location", "status"]
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
