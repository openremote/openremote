import {Dashboard, DashboardAccess, DashboardRefreshInterval, DashboardScalingPreset, DashboardScreenPreset} from "@openremote/model";
import {css, html, LitElement} from "lit";
import {customElement, property} from "lit/decorators.js";
import {InputType, OrInputChangedEvent} from "@openremote/or-mwc-components/or-mwc-input";
import {style} from './style';
import {i18next} from "@openremote/or-translate";
import {unsafeHTML} from 'lit/directives/unsafe-html.js';
import {when} from "lit/directives/when.js";
import {dashboardAccessToString, scalingPresetToString, sortScreenPresets} from ".";

//language=css
const boardSettingsStyling = css`
    .label {
        margin-bottom: 4px;
    }
`

@customElement("or-dashboard-boardsettings")
export class OrDashboardBoardsettings extends LitElement {

    @property()
    protected readonly dashboard!: Dashboard;

    @property()
    protected readonly showPerms?: boolean;

    /* ------------------------- */

    static get styles() {
        return [boardSettingsStyling, style]
    }

    forceParentUpdate(force: boolean = false) {
        this.dispatchEvent(new CustomEvent('update', {detail: {force: force}}));
    }

    /* -------------------------------- */

    private setViewAccess(access: DashboardAccess) {
        this.dashboard.viewAccess = access;
        if (access === DashboardAccess.PRIVATE) {
            this.dashboard.editAccess = DashboardAccess.PRIVATE; // if viewAccess changed to PRIVATE, make editAccess private as well.
        }
        this.requestUpdate();
        this.forceParentUpdate(false);
    }

    private setEditAccess(access: DashboardAccess) {
        this.dashboard.editAccess = access;
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
            const viewAccessOptions = [DashboardAccess.PRIVATE, DashboardAccess.SHARED, DashboardAccess.PUBLIC].map((access) => ({key: access, value: dashboardAccessToString(access)}));
            const editAccessOptions = [DashboardAccess.PRIVATE, DashboardAccess.SHARED].map((access) => ({key: access, value: dashboardAccessToString(access)}));
            const refreshIntervalOptions = [DashboardRefreshInterval.OFF, DashboardRefreshInterval.ONE_MIN, DashboardRefreshInterval.FIVE_MIN, DashboardRefreshInterval.QUARTER, DashboardRefreshInterval.ONE_HOUR].map(interval => ({key: interval, value: `dashboard.interval.${interval.toLowerCase()}`}))
            const scalingPresets: { key: DashboardScalingPreset, value: string }[] = [];
            [DashboardScalingPreset.KEEP_LAYOUT, DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN, /*DashboardScalingPreset.REDIRECT,*/ DashboardScalingPreset.BLOCK_DEVICE].forEach((preset: DashboardScalingPreset) => {
                scalingPresets.push({key: preset, value: scalingPresetToString(preset)});
            });
            return html`

                <!-- Permissions panel, to set who can view/edit the selected dashboard -->
                ${when(this.showPerms, () => html`
                    <settings-panel displayName="permissions" expanded="${true}">
                        <div>
                            <div style="margin-bottom: 24px;">
                                <div class="label">
                                    ${html`<span>${unsafeHTML(i18next.t('dashboard.whoCanView').toString())}</span>`}
                                </div>
                                <or-mwc-input class="permissionInput" comfortable type="${InputType.SELECT}" style="width: 100%;"
                                              .options="${viewAccessOptions.map((access) => access.value)}"
                                              .value="${dashboardAccessToString(this.dashboard.viewAccess!)}"
                                              @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                                  this.setViewAccess(viewAccessOptions.find((access) => access.value == event.detail.value)?.key!);
                                              }}"
                                ></or-mwc-input>
                            </div>
                            <div style="margin-bottom: 24px;">
                                <div class="label">
                                    ${html`<span>${unsafeHTML(i18next.t('dashboard.whoCanEdit').toString())}</span>`}
                                </div>
                                <or-mwc-input class="permissionInput" comfortable type="${InputType.SELECT}" style="width: 100%;"
                                              .disabled="${(this.dashboard.viewAccess == DashboardAccess.PRIVATE)}"
                                              .options="${editAccessOptions.map((access) => access.value)}"
                                              .value="${dashboardAccessToString(this.dashboard.editAccess!)}"
                                              @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                                  this.setEditAccess(editAccessOptions.find((access) => access.value == event.detail.value)?.key!);
                                              }}"
                                ></or-mwc-input>
                            </div>
                        </div>
                    </settings-panel>
                `)}

                <!-- Layout panel, with options such as 'amount of columns' -->
                <settings-panel displayName="layout" expanded="${true}">
                    <div>
                        
                        <!-- Number of Columns control -->
                        <div style="margin-bottom: 24px; display: flex; align-items: center;">
                            <span style="min-width: 180px;"><or-translate value="dashboard.numberOfColumns"></or-translate></span>
                            <or-mwc-input type="${InputType.NUMBER}" comfortable .value="${this.dashboard.template.columns}" min="1" max="24" @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                if (this.dashboard.template != null && event.detail.value as number <= 24 && event.detail.value as number >= 1) {
                                    this.dashboard.template.columns = event.detail.value as number;
                                    this.forceParentUpdate(true);
                                }
                            }}"></or-mwc-input>
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
                            <div>
                                <or-mwc-input type="${InputType.NUMBER}" comfortable .value="${this.dashboard.template.maxScreenWidth}" style="width: 70px;"
                                              @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                                  if (this.dashboard.template != null) {
                                                      this.dashboard.template.maxScreenWidth = event.detail.value as number;
                                                      this.forceParentUpdate(true);
                                                  }
                                              }}">
                                </or-mwc-input>
                                <span style="margin-left: 2px;">px</span>
                            </div>
                        </div>
                        
                        <!-- "Set to mobile" preset button, for ease of use -->
                        <div>
                            <or-mwc-input type="${InputType.BUTTON}" comfortable label="dashboard.setToMobilePreset"
                                          @or-mwc-input-changed="${() => {
                                              this.setToMobilePreset();
                                          }}">
                            </or-mwc-input>
                        </div>
                    </div>
                </settings-panel>

                <!-- Data management options -->
                <settings-panel displayName="dashboard.dataManagement" expanded="${true}">
                    <div>
                        <div class="label">
                            <span><or-translate value="dashboard.defaultRefreshInterval"></or-translate></span>
                        </div>
                        <or-mwc-input type="${InputType.SELECT}" comfortable .options="${refreshIntervalOptions.map(o => o.value)}" style="width: 100%;"
                                      .value="${`dashboard.interval.${this.dashboard.template.refreshInterval?.toLowerCase() || `off`}`}"
                                      @or-mwc-input-changed="${(ev: OrInputChangedEvent) => {
                                          const intervalEntry = refreshIntervalOptions.find(o => o.value === ev.detail.value);
                                          if(intervalEntry) {
                                              this.setRefreshInterval(intervalEntry.key)
                                          }
                                      }}">
                        </or-mwc-input>
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

    scalingPresetTemplate(screenPresets: DashboardScreenPreset[], scalingPresets: { key: DashboardScalingPreset, value: string }[]) {
        return html`
            ${screenPresets.map((preset) => {
                return html`
                    <div style="margin-bottom: ${screenPresets.length > 1 ? '24px' : '16px'}">
                        <div class="label">
                            ${html`<span>${unsafeHTML(i18next.t("dashboard.onScreenMyBoardShould").replace("{{size}}", ("<b>" + i18next.t(preset.displayName!) + "</b>")))}</span>`}
                        </div>
                        <or-mwc-input class="displayInput" type="${InputType.SELECT}" comfortable style="width: 100%;"
                                      .options="${scalingPresets.map((x) => x.value)}"
                                      .value="${scalingPresets.find((p) => p.key == preset.scalingPreset)?.value}"
                                      @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                          preset.scalingPreset = scalingPresets.find((p) => p.value == event.detail.value)?.key;
                                          this.forceParentUpdate(true);
                                          this.requestUpdate();
                                      }}"
                        ></or-mwc-input>
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
                        <div>
                            <span style="margin-right: 4px;">${(screenPresets.indexOf(preset) == 0) ? '>' : '<'}</span>
                            <or-mwc-input type="${InputType.NUMBER}" comfortable .disabled="${(screenPresets.length > 1 && screenPresets.indexOf(preset) == 0)}" style="width: 70px;"
                                          .value="${(screenPresets.length > 1 && screenPresets.indexOf(preset) == 0 ? screenPresets[1].breakpoint : preset.breakpoint)}"
                                          @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                              this.setBreakpoint(screenPresets.indexOf(preset), event.detail.value)
                                          }}"
                            ></or-mwc-input>
                            <span style="margin-left: 2px;">px</span>
                        </div>
                    </div>
                `
            })}
        `
    }
}
