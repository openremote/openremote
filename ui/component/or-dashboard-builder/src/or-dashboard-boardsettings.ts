import {Dashboard, DashboardAccess, DashboardScalingPreset } from "@openremote/model";
import {css, html, LitElement, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {style} from './style';
import {
    dashboardAccessToString,
    scalingPresetToString,
    sortScreenPresets
} from ".";

//language=css
const boardSettingsStyling = css`
    .label {
        margin-bottom: 4px;
    }
`

@customElement("or-dashboard-boardsettings")
export class OrDashboardBoardsettings extends LitElement {

    @property()
    protected readonly dashboard?: Dashboard;

    /*@property()
    protected readonly template?: DashboardTemplate;*/

    @state()
    protected expandedPanels: string[] = ["Permissions", "Layout", "Display", "Breakpoints"];

    constructor() {
        super();
        this.updateComplete.then(() => {
            if(this.shadowRoot != null) {
                this.shadowRoot.querySelectorAll(".displayInput").forEach(element => {
                    (element.shadowRoot?.querySelector(".mdc-menu") as HTMLElement).style.minWidth = "250px"; // small fix for dropdown menu's to be the correct width
                })
                this.shadowRoot.querySelectorAll(".permissionInput").forEach(element => {
                    (element.shadowRoot?.querySelector(".mdc-menu") as HTMLElement).style.minWidth = "250px"; // small fix for dropdown menu's to be the correct width
                })
            }
        })
    }
    /* ------------------------- */

    static get styles() {
        return [boardSettingsStyling, style]
    }

    forceParentUpdate(force: boolean = false) {
        this.dispatchEvent(new CustomEvent('update', { detail: { force: force }}));
    }

    /* ------------------------- */

    // Method when a user opens or closes an expansion panel. (UI related)
    expandPanel(panelName: string): void {
        if(this.expandedPanels.includes(panelName)) {
            const indexOf = this.expandedPanels.indexOf(panelName, 0);
            if(indexOf > -1) { this.expandedPanels.splice(indexOf, 1); }
        } else {
            this.expandedPanels.push(panelName);
        }
        this.requestUpdate();
    }

    /* -------------------------------- */

    private setViewAccess(access: DashboardAccess) {
        this.dashboard!.viewAccess = access;
        if(access == DashboardAccess.PRIVATE) {
            this.dashboard!.editAccess = DashboardAccess.PRIVATE;
        } else if(this.dashboard?.editAccess == DashboardAccess.PRIVATE) {
            this.dashboard.editAccess = access;
        }
        this.requestUpdate();
    }
    private setEditAccess(access: DashboardAccess) {
        this.dashboard!.editAccess = access;
        this.requestUpdate();
    }

    protected render() { //nosonar
        if(this.dashboard?.template?.screenPresets != null) {
            const screenPresets = sortScreenPresets(this.dashboard.template.screenPresets, true);
            const accessOptions: {key: DashboardAccess, value: string}[] = [];
            [DashboardAccess.PRIVATE, DashboardAccess.SHARED, DashboardAccess.PUBLIC].forEach((access) => {
                accessOptions.push({key: access, value: dashboardAccessToString(access)})
            })
            const scalingPresets: {key: DashboardScalingPreset, value: string}[] = [];
            [DashboardScalingPreset.KEEP_LAYOUT, DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN, /*DashboardScalingPreset.REDIRECT,*/ DashboardScalingPreset.BLOCK_DEVICE].forEach((preset: DashboardScalingPreset) => {
                scalingPresets.push({key: preset, value: scalingPresetToString(preset) });
            });
            return html`
                <!-------------------->
                <div>${this.generateExpandableHeader('Permissions')}</div>
                <div>
                    ${this.expandedPanels.includes('Permissions') ? html`
                        <div style="padding: 24px 24px 24px 24px;">
                            <div style="margin-bottom: 24px;">
                                <div class="label">
                                    <span>Who can</span>
                                    <span style="font-weight: bold;">view</span>
                                    <span>the dashboard?</span>
                                </div>
                                <or-mwc-input class="permissionInput" compact outlined type="${InputType.SELECT}" style="width: 250px;"
                                              .options="${accessOptions.map((access) => access.value)}"
                                              .value="${dashboardAccessToString(this.dashboard.viewAccess!)}"
                                              @or-mwc-input-changed="${(event: OrInputChangedEvent) => { this.setViewAccess(accessOptions.find((access) => access.value == event.detail.value)?.key!); }}"
                                ></or-mwc-input>
                            </div>
                            <div style="margin-bottom: 24px;">
                                <div class="label">
                                    <span>Who can</span>
                                    <span style="font-weight: bold;">edit</span>
                                    <span>the dashboard?</span>
                                </div>
                                <or-mwc-input class="permissionInput" compact outlined type="${InputType.SELECT}" style="width: 250px;"
                                              .disabled="${(this.dashboard.viewAccess == DashboardAccess.PRIVATE)}" 
                                              .options="${accessOptions.map((access) => access.value)}" 
                                              .value="${dashboardAccessToString(this.dashboard.editAccess!)}" 
                                              @or-mwc-input-changed="${(event: OrInputChangedEvent) => { this.setEditAccess(accessOptions.find((access) => access.value == event.detail.value)?.key!); }}"
                                ></or-mwc-input>
                            </div>
                        </div>
                    ` : null}
                </div>
                <!-------------------->
                <div>${this.generateExpandableHeader('Layout')}</div>
                <div>
                    ${this.expandedPanels.includes('Layout') ? html`
                        <div style="padding: 24px 24px 24px 24px;">
                            <!-- Number of Columns control -->
                            <div style="margin-bottom: 12px; display: flex; align-items: center;">
                                <span style="min-width: 180px;">Number of Columns</span>
                                <or-mwc-input type="${InputType.NUMBER}" compact outlined .value="${this.dashboard.template.columns}" min="1" max="12" @or-mwc-input-changed="${(event: OrInputChangedEvent) => { if(this.dashboard?.template != null) { this.dashboard.template.columns = event.detail.value as number; this.forceParentUpdate(true); }}}"></or-mwc-input>
                            </div>
                            <!-- Max Screen Width control-->
                            <div style="margin-bottom: 24px; display: flex; align-items: center;">
                                <span style="min-width: 150px;">Max Screen Width</span>
                                <or-mwc-input type="${InputType.NUMBER}" compact outlined .value="${this.dashboard.template.maxScreenWidth}" disabled @or-mwc-input-changed="${(event: OrInputChangedEvent) => { if(this.dashboard?.template != null) { this.dashboard.template.maxScreenWidth = event.detail.value as number; this.forceParentUpdate(false); }}}"></or-mwc-input>
                                <span style="margin-left: 8px;">px</span>
                            </div>
                        </div>
                    ` : null}
                </div>
                <!-------------------->
                <div>${this.generateExpandableHeader('Display')}</div>
                <div>
                    ${this.expandedPanels.includes('Display') ? html`
                        <div style="padding: 24px 24px 24px 24px;">
                            ${screenPresets.map((preset) => {
                                return html`
                                    <div style="margin-bottom: 24px;">
                                        <div class="label">
                                            <span>On a</span>
                                            <span style="font-weight: bold;">${preset.displayName}</span>
                                            <span>Screen my board should:</span>
                                        </div>
                                        <or-mwc-input class="displayInput" type="${InputType.SELECT}" outlined style="width: 250px;"
                                                      .options="${scalingPresets.map((x) => x.value)}"
                                                      .value="${scalingPresets.find((p) => p.key == preset.scalingPreset)?.value}"
                                                      @or-mwc-input-changed="${(event: OrInputChangedEvent) => {
                                                          preset.scalingPreset = scalingPresets.find((p) => p.value == event.detail.value)?.key;
                                                          this.forceParentUpdate(true);
                                                      }}"
                                        ></or-mwc-input>
                                    </div>
                                `
                            })}
                        </div>
                    ` : null}
                </div>
                <!-------------------->
                <div>${this.generateExpandableHeader('Breakpoints')}</div>
                <div>
                    ${this.expandedPanels.includes('Breakpoints') ? html`
                        <div style="padding: 24px 24px 24px 24px;">
                            ${screenPresets.map((preset) => {
                                return html`
                                    <div style="margin-bottom: 12px; display: flex; align-items: center;">
                                        <span style="min-width: 140px;">${preset.displayName} screen</span>
                                        <span style="margin-right: 8px;">${(screenPresets.indexOf(preset) == 0) ? '>' : '<'}</span>
                                        <or-mwc-input type="${InputType.NUMBER}" compact outlined .value="${(screenPresets.indexOf(preset) == 0 ? screenPresets[1].breakpoint : preset.breakpoint)}" .disabled="${true}"></or-mwc-input>
                                        <span style="margin-left: 8px;">px</span>
                                    </div>
                                `
                            })}
                        </div>
                    ` : null}
                </div>
                <!-------------------->
            `
        } else {
            return html`
                <div style="padding: 24px;">
                    <span>Something went wrong.</span><br/>
                    <span>No dashboard could be found.</span><br/><br/>
                    <span>Try to reopen the tab.</span>
                </div>
            `
        }
    }




    /* ======================== */

    // UI generation of an expandable panel header.
    generateExpandableHeader(name: string): TemplateResult {
        return html`
            <button class="expandableHeader" @click="${() => { this.expandPanel(name); }}">
                <or-icon icon="${this.expandedPanels.includes(name) ? 'chevron-down' : 'chevron-right'}"></or-icon>
                <span style="margin-left: 6px;">${name}</span>
            </button>
        `
    }


    /* ------------------------- */

}
