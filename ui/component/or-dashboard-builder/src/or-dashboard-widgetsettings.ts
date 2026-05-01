/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
import {DashboardWidget} from "@openremote/model";
import {html, LitElement, TemplateResult, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import {style} from './style';
import { until } from "lit/directives/until.js";
import {WidgetService} from "./service/widget-service";
import {WidgetSettings, WidgetSettingsChangedEvent} from "./util/widget-settings";
import {WidgetManifest} from "./util/or-widget";
import { guard } from "lit/directives/guard.js";
import {OrVaadinTextField} from "@openremote/or-vaadin-components/or-vaadin-text-field";

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
            <div style="padding: 12px; border-bottom: 1px solid #E0E0E0">
                <or-vaadin-text-field value=${this.selectedWidget?.displayName} required minlength="1" @click=${(ev: Event) => {
                    const elem = ev.currentTarget as OrVaadinTextField;
                    if(elem.checkValidity()) this.setDisplayName(elem.value);
                }}>
                    <or-translate slot="label" value="name"></or-translate>
                </or-vaadin-text-field>
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
