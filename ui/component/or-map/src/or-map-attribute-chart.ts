import {LitElement} from "lit";
import {css, html} from "lit";
import {customElement, property, state} from "lit/decorators.js";
import manager, {subscribe} from "@openremote/core";
import moment from "moment";
import {OrMap} from "./index";
import {
    ValueDatapoint, GeoJSONPoint, Asset, Attribute, AttributeRef
} from '@openremote/model';
import {GeoJSONSource} from "maplibre-gl"
import {TimePresetCallback} from '@openremote/or-chart'
//
@customElement("or-map-attribute-chart")
export class OrMapAttributeChart extends subscribe(manager)(LitElement) {
    // static get styles(){
    //     return css`
    //
    //     `
    // }

    @property({type: String, reflect: true, attribute: true})
    public assetId?: string;

    @property({type: Object, reflect: true, attribute: true})
    public asset?: Asset;
    @state()
    private assets: Asset[] = [];
    @state()
    private assetAttributes: [number, Attribute<any>][] = [];

    connectedCallback() {
        super.connectedCallback();
        // options.chart = new ChartConf

    }

    // disconnectedCallback() {
    //     super.disconnectedCallback();
    //     // this.removeLayers();
    // }
    //
    // async updated(changedProperties: Map<string, any>){
    //     // Since the element is not removed when another marker is selected,
    //     // but the Asset ID is changed if another asset is selected,
    //     // we check if the assetId has changed,
    //     // and if it has, remove the GeoJSON elements, and load the new Asset's layers.
    //     if (changedProperties.has("assetId")){
    //         // checks if the assetId is actually updated, because this is fired when the element is constructed.
    //         if(changedProperties.get("assetId") != undefined) {
    //             // this.removeLayers();
    //             //
    //             // await this._loadData();
    //             // this._addGeoJSONLayer();
    //         }
    //     }
    // }

    protected render(){
        this.assets = [this.asset!]


        if (this.assets && this.assets.length > 0) {
            let attrRefs: AttributeRef[]  =[];

            // for (const assetsKey of this.assets) {
            //     console.log(assetsKey.attributes);
            //     this.assetAttributes.push([0, assetsKey.attributes]);
            // }
        }
        const timePresetOptions = new Map<string, TimePresetCallback>([
            ["lastHour", (date: Date) => [moment(date).subtract(1, 'hour').toDate(), date]],
            ["last24Hours", (date: Date) => [moment(date).subtract(24, 'hours').toDate(), date]],
            ["last7Days", (date: Date) => [moment(date).subtract(7, 'days').toDate(), date]],
            ["last30Days", (date: Date) => [moment(date).subtract(30, 'days').toDate(), date]],
            ["last90Days", (date: Date) => [moment(date).subtract(90, 'days').toDate(), date]],
            ["last6Months", (date: Date) => [moment(date).subtract(6, 'months').toDate(), date]],
            ["lastYear", (date: Date) => [moment(date).subtract(1, 'year').toDate(), date]],
            ["thisHour", (date: Date) => [moment(date).startOf('hour').toDate(), moment(date).endOf('hour').toDate()]],
            ["thisDay", (date: Date) => [moment(date).startOf('day').toDate(), moment(date).endOf('day').toDate()]],
            ["thisWeek", (date: Date) => [moment(date).startOf('isoWeek').toDate(), moment(date).endOf('isoWeek').toDate()]],
            ["thisMonth", (date: Date) => [moment(date).startOf('month').toDate(), moment(date).endOf('month').toDate()]],
            ["thisYear", (date: Date) => [moment(date).startOf('year').toDate(), moment(date).endOf('year').toDate()]],
            ["yesterday", (date: Date) => [moment(date).subtract(24, 'hours').startOf('day').toDate(), moment(date).subtract(24, 'hours').endOf('day').toDate()]],
            ["thisDayLastWeek", (date: Date) => [moment(date).subtract(1, 'week').startOf('day').toDate(), moment(date).subtract(1, 'week').endOf('day').toDate()]],
            ["previousWeek", (date: Date) => [moment(date).subtract(1, 'week').startOf('isoWeek').toDate(), moment(date).subtract(1, 'week').endOf('isoWeek').toDate()]],
            ["previousMonth", (date: Date) => [moment(date).subtract(1, 'month').startOf('month').toDate(), moment(date).subtract(1, 'month').endOf('month').toDate()]],
            ["previousYear", (date: Date) => [moment(date).subtract(1, 'year').startOf('year').toDate(), moment(date).subtract(1, 'year').endOf('year').toDate()]]
        ])

        return html`
            <or-attribute-history 
                      
                      style="height: 100%"
                    ></or-attribute-history>
        `
    }
    //
    // async connectedCallback() {
    //     super.connectedCallback();
    //
    // }
}
