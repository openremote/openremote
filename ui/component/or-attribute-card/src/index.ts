import {css, customElement, html, LitElement, property, PropertyValues} from "lit-element";
import {OrTranslate, translate} from "@openremote/or-translate";
import {classMap} from "lit-html/directives/class-map";

import i18next from "i18next";
import {Asset, AssetAttribute} from "@openremote/model";
import manager from "@openremote/core";

// language=CSS
const style = css`
    
    :host {
        width: 100%
    }
    
    :host([hidden]) {
        display: none;
    }
    
    .panel {
        background-color: var(--internal-or-asset-viewer-panel-color);     
        border: 1px solid #e5e5e5;
        border-radius: 5px;
        max-width: 100%;
        position: relative;
    }
    
    .panel-content-wrapper {
        padding: var(--internal-or-asset-viewer-panel-padding);
    }
    
    .panel-content {
        display: flex;
        flex-wrap: wrap;
    }
        
    .panel-title {
        text-transform: uppercase;
        font-weight: bolder;
        line-height: 1em;
        color: var(--internal-or-asset-viewer-title-text-color);
        margin-bottom: 25px;
        flex: 0 0 auto;
    }
`;

@customElement("or-attribute-card")
export class OrAttributeCard extends LitElement {

    static get styles() {
        return [
            style
        ];
    }

    @property()
    public assetId?: string;

    @property()
    public attributeName?: string;

    @property()
    private cardTitle: string = "";

    private getData = () => {
        if (this.assetId) {
            this.getAssetById(this.assetId)
                .then((data) => {
                    this.cardTitle = data.name || "";
                    console.log("set", this.cardTitle);
                }
            );
        }
    };

    protected render() {
        console.log("vars ready", this.assetId, this.attributeName);

        this.getData();

        if (!this.assetId || !this.attributeName) {
            // return html`
            //     <div class="panel">
            //         <div class="panel-content-wrapper">
            //             <div class="panel-title">
            //                 ${this.asdf}
            //             </div>
            //             <div class="panel-content">
            //                 <p>no attribute found</p>
            //             </div>
            //         </div>
            //     </div>
            // `;
        }

        return html`
            <div class="panel" id="attribute-card">
                <div class="panel-content-wrapper">
                    <div class="panel-title">
                        this.cardTitle: ${this.cardTitle} 
                    </div>
                    <div class="panel-title">
                        ${this.assetId} 
                    </div>
                    <div class="panel-title">
                        ${this.attributeName} 
                    </div>
                    <div class="panel-content">
                        <p>person</p>
                    </div>
                </div>
            </div>
        `;
    }

    private async getAssetById(id: string): Promise<Asset> {
        const response = await manager.rest.api.AssetResource.queryAssets({
            ids: [id],
            recursive: false
        });

        // if (response.status !== 200 || !response.data) {
        //     return;
        // }

        return response.data[0];
    }

}
