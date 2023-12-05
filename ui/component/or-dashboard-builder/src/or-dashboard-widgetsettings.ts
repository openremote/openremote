import {Asset, DashboardWidget } from "@openremote/model";
import {html, LitElement, TemplateResult, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {style} from './style';
import { i18next } from "@openremote/or-translate";
import { until } from "lit/directives/until.js";
import {WidgetService} from "./service/widget-service";
import {WidgetSettings, WidgetSettingsChangedEvent} from "./util/widget-settings";
import {WidgetManifest} from "./util/or-widget";
import { guard } from "lit/directives/guard.js";

const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

/* ------------------------------------ */

@customElement("or-dashboard-widgetsettings")
export class OrDashboardWidgetsettings extends LitElement {

    static get styles() {
        return [unsafeCSS(tableStyle), style]
    }

    @property({type: Object})
    protected selectedWidget!: DashboardWidget;

    @state()
    protected settingsElem?: WidgetSettings;

    /* ------------------------------------ */


    // Method to update the Grid. For example after changing a setting.
    forceParentUpdate(changes: Map<string, any>, force: boolean = false) {
        this.requestUpdate();
        this.dispatchEvent(new CustomEvent('update', { detail: { changes: changes, force: force }}));
    }

    protected render() {
        return html`
            <div style="padding: 12px;">
                <div>
                    <or-mwc-input .type="${InputType.TEXT}" style="width: 100%;" .value="${this.selectedWidget?.displayName}" label="${i18next.t('name')}"
                                  @or-mwc-input-changed="${(event: OrInputChangedEvent) => this.setDisplayName(event.detail.value)}"
                    ></or-mwc-input>
                </div>
            </div>
            <div>
                ${guard([this.selectedWidget], () => html`
                    ${until(this.generateContent(this.selectedWidget!.widgetTypeId!), html`Loading...`)}
                `)}
            </div>
        `;
    }

    protected setDisplayName(name?: string) {
        this.selectedWidget!.displayName = name;
        this.forceParentUpdate(new Map<string, any>([['widget', this.selectedWidget]]));
    }

    protected async generateContent(widgetTypeId: string): Promise<TemplateResult> {
        if(!this.settingsElem || this.settingsElem.id !== this.selectedWidget.id) {
            const manifest = WidgetService.getManifest(widgetTypeId);
            this.settingsElem = this.initSettings(manifest);
        }
        return html`
            ${this.settingsElem}
        `;
    }

    protected initSettings(manifest: WidgetManifest): WidgetSettings {
        const settingsElem =  manifest.getSettingsHtml(this.selectedWidget!.widgetConfig);
        settingsElem.id = this.selectedWidget.id!;
        settingsElem.getDisplayName = () => this.selectedWidget.displayName;
        settingsElem.setDisplayName = (name?: string) => this.setDisplayName(name);
        settingsElem.getEditMode = () => true;
        settingsElem.getWidgetLocation = () => ({
            x: this.selectedWidget.gridItem?.x,
            y: this.selectedWidget.gridItem?.y,
            w: this.selectedWidget.gridItem?.w,
            h: this.selectedWidget.gridItem?.h
        });
        settingsElem.addEventListener(WidgetSettingsChangedEvent.NAME, (ev: any) => this.onWidgetConfigChange(ev));
        return settingsElem;
    }

    protected onWidgetConfigChange(ev: WidgetSettingsChangedEvent) {
        this.selectedWidget!.widgetConfig = ev.detail;
        this.forceParentUpdate(new Map<string, any>([['widget', this.selectedWidget]]));
    }
}
