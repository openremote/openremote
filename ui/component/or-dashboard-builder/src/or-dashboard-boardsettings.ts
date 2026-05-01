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
import {Dashboard, DashboardAccess, DashboardRefreshInterval, DashboardScalingPreset, DashboardScreenPreset} from "@openremote/model";
import {css, html, LitElement} from "lit";
import {customElement, property} from "lit/decorators.js";
import {style} from './style';
import {i18next} from "@openremote/or-translate";
import {unsafeHTML} from 'lit/directives/unsafe-html.js';
import {when} from "lit/directives/when.js";
import {dashboardAccessToString, scalingPresetToString, sortScreenPresets} from ".";
import {OrVaadinSelect, SelectItem} from "@openremote/or-vaadin-components/or-vaadin-select";
import {OrVaadinNumberField} from "@openremote/or-vaadin-components/or-vaadin-number-field";

//language=css
const boardSettingsStyling = css`
    .label {
        margin-bottom: 4px;
    }
`

@customElement("or-dashboard-boardsettings")
export class OrDashboardBoardsettings extends LitElement {

    @property({ type: Object })
    public readonly dashboard!: Dashboard;

    @property({ type: Boolean })
    public readonly showPerms?: boolean;

    /* ------------------------- */

    static get styles() {
        return [boardSettingsStyling, style]
    }

    forceParentUpdate(force: boolean = false) {
        this.dispatchEvent(new CustomEvent('update', {detail: {force: force}}));
    }

    /* -------------------------------- */

    private setAccess(access: DashboardAccess) {
        this.dashboard.access = access;
        this.requestUpdate();
        this.forceParentUpdate(false);
    }

    private setBreakpoint(presetIndex: number, value: number) {
        this.dashboard.template!.screenPresets![presetIndex].breakpoint = value;
        this.requestUpdate();
        this.forceParentUpdate(true);
    }

    private setRefreshInterval(interval: DashboardRefreshInterval) {
        this.dashboard.template!.refreshInterval = interval;
        this.requestUpdate();
        this.forceParentUpdate(false);
    }

    protected render() {
        if (this.dashboard.template?.screenPresets != null) {
            const screenPresets = sortScreenPresets(this.dashboard.template.screenPresets, true);
            const accessOptions: SelectItem[] = [DashboardAccess.PRIVATE, DashboardAccess.SHARED, DashboardAccess.PUBLIC].map((access) => ({value: access, label: dashboardAccessToString(access)}));
            const refreshIntervalOptions: SelectItem[] = [DashboardRefreshInterval.OFF, DashboardRefreshInterval.ONE_MIN, DashboardRefreshInterval.FIVE_MIN, DashboardRefreshInterval.QUARTER, DashboardRefreshInterval.ONE_HOUR].map(interval => ({value: interval, label: i18next.t(`dashboard.interval.${interval.toLowerCase()}`)}))
            const scalingPresets: SelectItem[] = [DashboardScalingPreset.KEEP_LAYOUT, DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN, /*DashboardScalingPreset.REDIRECT,*/ DashboardScalingPreset.BLOCK_DEVICE].map((preset) => ({value: preset, label: scalingPresetToString(preset)}));
            return html`

                <!-- Permissions panel, to set who can view/edit the selected dashboard -->
                ${when(this.showPerms, () => html`
                    <settings-panel displayName="permissions" expanded="${true}">
                        <div>
                            <div class="label">
                                ${html`<span>${unsafeHTML(i18next.t('dashboard.whoCanView').toString())}</span>`}
                            </div>
                            <or-vaadin-select .items=${accessOptions} value=${this.dashboard.access}
                                              @change=${(ev: Event) => this.setAccess((ev.currentTarget as OrVaadinSelect).value as DashboardAccess)}
                            ></or-vaadin-select>
                        </div>
                    </settings-panel>
                `)}

                <!-- Layout panel, with options such as 'amount of columns' -->
                <settings-panel displayName="layout" expanded="${true}">
                    <div>
                        
                        <!-- Number of Columns control -->
                        <div style="margin-bottom: 24px; display: flex; align-items: center;">
                            <span style="min-width: 180px;"><or-translate value="dashboard.numberOfColumns"></or-translate></span>
                            <or-vaadin-number-field value=${this.dashboard.template.columns} min="1" max="24"
                                                    @change=${(ev: Event) => {
                                                        const elem = ev.currentTarget as OrVaadinNumberField;
                                                        if (elem.checkValidity() && this.dashboard.template != null) {
                                                            this.dashboard.template.columns = Number(elem.value);
                                                            this.forceParentUpdate(true);
                                                        }
                                                    }}
                            ></or-vaadin-number-field>
                        </div>
                        
                        <!-- Scaling preset -->
                        ${screenPresets.length === 1 ? html`
                            ${this.scalingPresetTemplate(screenPresets, scalingPresets)}
                        ` : undefined}
                        
                        <!-- Screen preset -->
                        ${screenPresets.length === 1 ? html`
                            ${this.screenPresetTemplate(screenPresets, ['Mobile breakpoint'])}
                        ` : undefined}
                        
                        <!-- Max Screen Width control-->
                        <div style="margin-bottom: 24px; display: flex; align-items: center; justify-content: space-between;">
                            <span style="min-width: 150px;"><or-translate value="dashboard.maxScreenWidth"></or-translate></span>
                            <or-vaadin-number-field value=${this.dashboard.template.maxScreenWidth} min="0" style="width: 110px;" @change=${(ev: Event) => {
                                const elem = ev.currentTarget as OrVaadinNumberField;
                                if (elem.checkValidity() && this.dashboard.template != null) {
                                    this.dashboard.template.maxScreenWidth = Number(elem.value);
                                    this.forceParentUpdate(true);
                                }
                            }}>
                                <span slot="suffix">px</span>
                            </or-vaadin-number-field>
                        </div>
                        
                        <!-- "Set to mobile" preset button, for ease of use -->
                        <or-vaadin-button @click=${() => this.setToMobilePreset()}>
                            <or-translate value="dashboard.setToMobilePreset"></or-translate>
                        </or-vaadin-button>
                    </div>
                </settings-panel>

                <!-- Data management options -->
                <settings-panel displayName="dashboard.dataManagement" expanded="${true}">
                    <div>
                        <div class="label">
                            <span><or-translate value="dashboard.defaultRefreshInterval"></or-translate></span>
                        </div>
                        <or-vaadin-select .items=${refreshIntervalOptions} value=${this.dashboard.template.refreshInterval}
                                          @change=${(ev: Event) => {
                                              const elem = ev.currentTarget as OrVaadinSelect;
                                              const intervalEntry = refreshIntervalOptions.find(o => o.value === elem.value)?.value;
                                              console.debug(intervalEntry);
                                              if(intervalEntry) {
                                                  this.setRefreshInterval(intervalEntry as DashboardRefreshInterval);
                                              }
                                          }}>
                        </or-vaadin-select>
                    </div>
                </settings-panel>
                
            `;
        } else {
            return html`
                <div style="padding: 24px;">
                    <span><or-translate value="errorOccurred"></or-translate></span><br/>
                    <span><or-translate value="noDashboardFound"></or-translate></span>
                </div>
            `
        }
    }

    // Button to switch to a mobile preset of layout settings.
    // TODO: Make these values not hardcoded anymore, and depend on the getDefault functions in or-dashboard-tree (that probably should move to somewhere else)
    setToMobilePreset() {
        this.dashboard.template!.columns = 4;
        this.dashboard.template!.screenPresets![0].scalingPreset = DashboardScalingPreset.KEEP_LAYOUT;
        this.dashboard.template!.maxScreenWidth = 700;
        this.requestUpdate();
        this.forceParentUpdate(true);
    }


    /* ======================== */

    scalingPresetTemplate(screenPresets: DashboardScreenPreset[], scalingPresets: SelectItem[]) {
        return html`
            ${screenPresets.map((preset) => {
                return html`
                    <div style="margin-bottom: ${screenPresets.length > 1 ? '24px' : '16px'}">
                        <div class="label">
                            ${html`<span>${unsafeHTML(i18next.t("dashboard.onScreenMyBoardShould").replace("{{size}}", ("<b>" + i18next.t(preset.displayName!) + "</b>")))}</span>`}
                        </div>
                        <or-vaadin-select .items=${scalingPresets} value=${preset.scalingPreset}
                                          @change=${(ev: Event) => {
                                              preset.scalingPreset = (ev.currentTarget as OrVaadinSelect).value as DashboardScalingPreset;
                                              this.forceParentUpdate(true);
                                              this.requestUpdate();
                                          }}
                        ></or-vaadin-select>
                    </div>
                `
            })}
        `
    }

    screenPresetTemplate(screenPresets: DashboardScreenPreset[], customLabels?: string[]) {
        return html`
            ${screenPresets.map((preset, index) => {
                return html`
                    <div style="margin-bottom: 12px; display: flex; align-items: center; justify-content: space-between;">
                        <span style="min-width: 140px;">${customLabels ? customLabels[index] : (preset.displayName + " " + i18next.t('screen'))}</span>
                        <or-vaadin-number-field value=${(screenPresets.length > 1 && screenPresets.indexOf(preset) == 0 ? screenPresets[1].breakpoint : preset.breakpoint)} 
                                                ?disabled=${(screenPresets.length > 1 && screenPresets.indexOf(preset) == 0)}
                                                min="0" style="width: 110px;" @change=${(ev: Event) => {
                                                    const elem = ev.currentTarget as OrVaadinNumberField;
                                                    if(elem.checkValidity()) this.setBreakpoint(screenPresets.indexOf(preset), Number(elem.value))
                                                }}>
                            <span slot="prefix">></span>
                            <span slot="suffix">px</span>
                        </or-vaadin-number-field>
                    </div>
                `
            })}
        `
    }
}
