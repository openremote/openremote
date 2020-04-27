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
    protected _data?: ValueDatapoint<any>[] = [{x: 1587333600000, y: 0}, {x: 1587337200000, y: 8.99}, {x: 1587340800000, y: 0}, {x: 1587344400000, y: 0}, {x: 1587348000000, y: 0}, {x: 1587351600000, y: 0}, {x: 1587355200000, y: 9.11}, {x: 1587358800000, y: 0}, {x: 1587362400000, y: 0}, {x: 1587366000000, y: 10.22}, {x: 1587369600000, y: 11.385}, {x: 1587373200000, y: 12.16875}, {x: 1587376800000, y: 0}, {x: 1587380400000, y: 0}, {x: 1587384000000, y: 0}, {x: 1587387600000, y: 0}, {x: 1587391200000, y: 0}, {x: 1587394800000, y: 0}, {x: 1587398400000, y: 0}, {x: 1587402000000, y: 0}, {x: 1587405600000, y: 0}, {x: 1587409200000, y: 0}, {x: 1587412800000, y: 0}, {x: 1587416400000, y: 0}];

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
        this.getAssetById(this.assetId)
            .then((data) => {
                    this.assetName = data.name || "";
                    if (data.id) {
                        this.getDatapointsByAttribute(data.id)
                            .then((datapoints: any) => { // todo: fix this any
                                // this._data = datapoints;
                            });
                    }
                }
            );
    };

    protected render() {
        console.log("vars ready", this.assetId, this.attributeName);

        this.getData();

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
