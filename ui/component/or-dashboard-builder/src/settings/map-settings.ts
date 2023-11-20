import {html, TemplateResult } from "lit";
import { customElement } from "lit/decorators.js";
import {AssetWidgetSettings} from "../util/or-asset-widget";
import {i18next} from "@openremote/or-translate";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {MapWidgetConfig} from "../widgets/map-widget";
import { LngLatLike } from "@openremote/or-map";
import "../panels/assettypes-panel";
import "../panels/thresholds-panel";
import {LngLat} from "maplibre-gl"; // TODO: Replace this import
import { when } from "lit/directives/when.js";
import {AssetTypeSelectEvent, AttributeNameSelectEvent, ShowLabelsToggleEvent, ShowUnitsToggleEvent} from "../panels/assettypes-panel";
import manager from "@openremote/core";
import { showSnackbar } from "@openremote/or-mwc-components/or-mwc-snackbar";
import {ThresholdChangeEvent} from "../panels/thresholds-panel";

@customElement("map-settings")
export class MapSettings extends AssetWidgetSettings {

    protected widgetConfig!: MapWidgetConfig;

    protected render(): TemplateResult {
        const allowedValueTypes = ["boolean", "number", "positiveInteger", "positiveNumber", "negativeInteger", "negativeNumber", "text"];
        return html`
            <div>
                
                <!-- Map settings -->
                <settings-panel displayName="${i18next.t('configuration.mapSettings')}" expanded="${true}">
                    <div style="display: flex; flex-direction: column; gap: 8px;">
                        <div>
                            <or-mwc-input .type="${InputType.NUMBER}" style="width: 100%;"
                                          .value="${this.widgetConfig.zoom}" label="${i18next.t('dashboard.zoom')}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onZoomUpdate(ev)}"
                            ></or-mwc-input>
                        </div>
                        <div style="display: flex; gap: 8px;">
                            <or-mwc-input .type="${InputType.TEXT}" style="width: 100%;"
                                          .value="${this.widgetConfig.center ? (Object.values(this.widgetConfig.center))[0] + ', ' + (Object.values(this.widgetConfig.center))[1] : undefined}"
                                          label="${i18next.t('dashboard.center')}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onCenterUpdate(ev)}"
                            ></or-mwc-input>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <span>${i18next.t('dashboard.showGeoJson')}</span>
                            <or-mwc-input .type="${InputType.SWITCH}" style="width: 70px;"
                                          .value="${this.widgetConfig.showGeoJson}"
                                          @or-mwc-input-changed="${(ev: OrInputChangedEvent) => this.onGeoJsonToggle(ev)}"
                            ></or-mwc-input>
                        </div>
                    </div>
                </settings-panel>
                
                <!-- Panel where Asset type and the selected attribute can be customized -->
                <settings-panel displayName="${i18next.t('attributes')}" expanded="${true}">
                    <assettypes-panel .assetType="${this.widgetConfig.assetType}" .attributeName="${this.widgetConfig.attributeName}"
                                      .showLabels="${this.widgetConfig.showLabels}" .showUnits="${this.widgetConfig.showUnits}"
                                      .valueTypes="${allowedValueTypes}"
                                      @assettype-select="${(ev: AssetTypeSelectEvent) => this.onAssetTypeSelect(ev)}"
                                      @attributename-select="${(ev: AttributeNameSelectEvent) => this.onAttributeNameSelect(ev)}"
                                      @showlabels-toggle="${(ev: ShowLabelsToggleEvent) => this.onShowLabelsToggle(ev)}"
                                      @showunits-toggle="${(ev: ShowUnitsToggleEvent) => this.onShowUnitsToggle(ev)}"
                    ></assettypes-panel>
                </settings-panel>
                
                <!-- List of customizable thresholds -->
                ${when(this.widgetConfig.assetIds.length > 0, () => html`
                    <settings-panel displayName="${i18next.t('thresholds')}" expanded="${true}">
                        <thresholds-panel .thresholds="${this.widgetConfig.thresholds}" .valueType="${this.widgetConfig.valueType}" style="padding-bottom: 12px;"
                                          .min="${this.widgetConfig.min}" .max="${this.widgetConfig.max}"
                                          @threshold-change="${(ev: ThresholdChangeEvent) => this.onThresholdsChange(ev)}">
                        </thresholds-panel>
                    </settings-panel>
                `)}
            </div>
        `;
    }

    protected onZoomUpdate(ev: OrInputChangedEvent) {
        this.widgetConfig.zoom = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onCenterUpdate(ev: OrInputChangedEvent) {
        if (ev.detail.value) {
            const lngLatArr = (ev.detail.value as string).split(/[, ]/).filter(v => !!v);
            if (lngLatArr.length === 2) {
                const value = new LngLat(
                    Number.parseFloat(lngLatArr[0]),
                    Number.parseFloat(lngLatArr[1])
                );
                this.widgetConfig.center = value as LngLatLike;
                this.notifyConfigUpdate();
            }
        }
    }

    protected onGeoJsonToggle(ev: OrInputChangedEvent) {
        this.widgetConfig.showGeoJson = ev.detail.value;
        this.notifyConfigUpdate();
    }

    protected onAssetTypeSelect(ev: AssetTypeSelectEvent) {
        if (this.widgetConfig.assetType !== ev.detail) {
            this.widgetConfig.attributeName = undefined;
            this.widgetConfig.assetIds = [];
            this.widgetConfig.showLabels = false;
            this.widgetConfig.showUnits = false;
            this.widgetConfig.boolColors = {type: 'boolean', 'false': '#ef5350', 'true': '#4caf50'};
            this.widgetConfig.textColors = [['example', "#4caf50"], ['example2', "#ff9800"]];
            this.widgetConfig.thresholds = [[0, "#4caf50"], [75, "#ff9800"], [90, "#ef5350"]];
            this.widgetConfig.assetType = ev.detail;
            this.notifyConfigUpdate();
        }
    }

    protected async onAttributeNameSelect(ev: AttributeNameSelectEvent) {
        this.widgetConfig.attributeName = ev.detail;
        await manager.rest.api.AssetResource.queryAssets({
            realm: {
                name: manager.displayRealm
            },
            select: {
                attributes: [ev.detail, 'location']
            },
            types: [this.widgetConfig.assetType!],
        }).then(response => {
            this.widgetConfig.assetIds = response.data.map((a) => a.id!);
            this.widgetConfig.valueType = (response.data.length > 0) ? response.data[0].attributes![ev.detail].type : "text"; // sometimes no asset exists of that assetType, so using 'text' as fallback.
        }).catch((reason) => {
            console.error(reason);
            showSnackbar(undefined, i18next.t('errorOccurred'));
        });

        this.notifyConfigUpdate()
    }

    protected onShowLabelsToggle(ev: ShowLabelsToggleEvent) {
        this.widgetConfig.showLabels = ev.detail;
        this.notifyConfigUpdate();
    }

    protected onShowUnitsToggle(ev: ShowUnitsToggleEvent) {
        this.widgetConfig.showUnits = ev.detail;
        this.notifyConfigUpdate();
    }

    protected onThresholdsChange(ev: ThresholdChangeEvent) {
        this.widgetConfig.thresholds = ev.detail;
        this.notifyConfigUpdate();
    }

}
