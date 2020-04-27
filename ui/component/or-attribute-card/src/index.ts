import {css, customElement, html, LitElement, property, PropertyValues} from "lit-element";
import {OrTranslate, translate} from "@openremote/or-translate";
import {classMap} from "lit-html/directives/class-map";

import i18next from "i18next";
import {Asset, AssetAttribute, DatapointInterval, ValueDatapoint} from "@openremote/model";
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

    @property()
    public assetId: string = "";

    @property()
    public attributeName: string = "";

    @property()
    protected _data?: ValueDatapoint<any>[] = [];

    @property()
    private cardTitle: string = "";

    @property()
    private assetName: string = "";

    static get styles() {
        return [
            style
        ];
    }

    private getData = () => {
    connectedCallback() {
        super.connectedCallback();
        this.getData();
    }

        this.getAssetById(this.assetId)
            .then((data) => {
                console.log(data);
                this.assetName = data.name || "";
                // return data;
                return this.getDatapointsByAttribute(data.id!);
            })
            .then((datapoints: any) => {
                console.log("datapoints", datapoints);
                // if (data.id) {
                //     this.getDatapointsByAttribute(data.id)
                //         .then((datapoints: any) => { // todo: fix this any
                //             // this._data = datapoints;
                //         });
                // }
            });
    };

    protected render() {
        console.log("vars ready", this.assetId, this.attributeName);

        if (this.assetId === "" || this.attributeName === "") {
            return html`
                <div class="panel">
                    <div class="panel-content-wrapper">
                        <div class="panel-title">
                            <or-translate value="error"></or-translate>
                        </div>
                        <div class="panel-content">
                            <or-translate value="attributeNotFound"></or-translate>
                        </div>
                    </div>
                </div>
            `;
        }

        return html`
            <div class="panel" id="attribute-card">
                <div class="panel-content-wrapper">
                    <div class="panel-title">
                        ${this.assetName} - ${i18next.t(this.attributeName)}
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

        if (response.status !== 200 || !response.data) {
            return {};
        }

        return response.data[0];
    }

    // todo: get rid of this <any>
    private async getDatapointsByAttribute(id: string): Promise<ValueDatapoint<any>[]> {
        const response = await manager.rest.api.AssetDatapointResource.getDatapoints(
            id,
            this.attributeName,
            {
                interval: DatapointInterval.DAY,
                fromTimestamp: 1585692000000,
                toTimestamp: 1588283999999
            }
        );
        // vars 2U48805AfxblLl1P5dCifA light1PowerConsumption 1585692000000 1588283999999 DAY string
        // 1585692000000 1588283999999 DAY
        console.log("data", response);

        if (response.status !== 200 || !response.data) {
            return [];
        }

        return response.data;
    }

}
