/*import { Asset, Attribute, AttributeRef, DashboardWidget } from "@openremote/model";
import { html, TemplateResult } from "lit";
import { customElement, state } from "lit/decorators.js";
import {OrBaseWidget, OrBaseWidgetConfig, OrBaseWidgetSettings} from "./or-base-widget";
import {i18next} from "@openremote/or-translate";
import { until } from "lit/directives/until.js";*/

/*export interface GaugeWidgetConfig extends OrBaseWidgetConfig {
    displayName: string;
    attributeRefs: AttributeRef[];
}

@customElement('or-gaugewidget')
export class OrGaugeWidget extends OrBaseWidget {

    override readonly DISPLAY_NAME: string = "Gauge";
    override readonly DISPLAY_MDI_ICON: string = "speedometer"; // https://materialdesignicons.com;
    override readonly MIN_COLUMN_WIDTH: number = 2;
    override readonly MIN_PIXEL_WIDTH: number = 200;
    override readonly MIN_PIXEL_HEIGHT: number = 200;

    @state()
    public asset?: Asset;

    @state()
    public assetAttribute?: [number, Attribute<any>];

    /!* ------------------ *!/

    override getDefaultConfig(): GaugeWidgetConfig {
        return {
            displayName: this.widget?.displayName,
            attributeRefs: []
        } as GaugeWidgetConfig;
    }

    override getWidgetHTML(widget: DashboardWidget, editMode: boolean, realm: string) {
        return html`<or-gaugewidget .widget="${widget}" .editMode="${editMode}" realm="${realm}" style="height: 100%;"></or-gaugewidget>`
    }

    override getSettingsHTML(widget: DashboardWidget): TemplateResult {
        return html`
            <or-gaugewidget-settings .widget="${widget}"></or-gaugewidget-settings>
        `;
    }

    /!*override render() {
        return html`
            <or-gauge .attrRef="${this.widget?.widgetConfig.attributeRefs ? this.widget?.widgetConfig.attributeRefs[0] : undefined}"></or-gauge>
        `
    }*!/
}




@customElement("or-gaugewidget-settings")
class OrGaugeWidgetSettings extends OrBaseWidgetSettings {

    // Default values
    override expandedPanels: string[] = [i18next.t('attributes')];

    override render() {
        const config = this.widget?.widgetConfig;
        return html`
            <div>
                ${this.generateExpandableHeader(i18next.t('attributes'))}
            </div>
            <div>
                ${this.expandedPanels.includes(i18next.t('attributes')) ? html`
                    ${until(this.generateAttributeHTML(config, false), html`<span>${i18next.t('loading')}</span>`)}
                ` : null}
            </div>
        `
    }
}*/
