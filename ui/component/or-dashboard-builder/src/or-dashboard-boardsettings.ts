import {DashboardScalingPreset, DashboardTemplate } from "@openremote/model";
import {css, html, LitElement, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {style} from './style';
import {scalingPresetToString, sortScreenPresets, stringToScalingPreset} from ".";

//language=css
const boardSettingsStyling = css`
`

@customElement("or-dashboard-boardsettings")
export class OrDashboardBoardsettings extends LitElement {

    @property()
    protected readonly template?: DashboardTemplate;

    @state()
    protected expandedPanels: string[] = ["Permissions", "Layout", "Display", "Breakpoints"];

    constructor() {
        super();
        this.updateComplete.then(() => {
            if(this.shadowRoot != null) {
                this.shadowRoot.querySelectorAll(".displayInput").forEach(element => {
                    (element.shadowRoot?.querySelector(".mdc-menu") as HTMLElement).style.minWidth = "250px"; // small fix for dropdown menu's to be the correct width
                })
            }
        })
    }
    /* ------------------------- */

    static get styles() {
        return [boardSettingsStyling, style]
    }

    forceParentUpdate() {
        this.dispatchEvent(new CustomEvent('minorupdate'));
    }
    forceParentRerender() {
        this.dispatchEvent(new CustomEvent('majorupdate'));
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

    protected render() { //nosonar
        if(this.template != null && this.template.screenPresets != null) {
            const screenPresets = sortScreenPresets(this.template.screenPresets, true);
            return html`
                <!-------------------->
                <div>${this.generateExpandableHeader('Permissions')}</div>
                <div>
                    ${this.expandedPanels.includes('Permissions') ? html`
                        <div style="padding: 24px 24px 48px 24px;">
                            <span>Not created yet.</span>
                        </div>
                    ` : null}
                </div>
                <!-------------------->
                <div>${this.generateExpandableHeader('Layout')}</div>
                <div>
                    ${this.expandedPanels.includes('Layout') ? html`
                        <div style="padding: 24px 24px 48px 24px;">
                            <!-- Number of Columns control -->
                            <div style="margin-bottom: 12px; display: flex; align-items: center;">
                                <span style="min-width: 180px;">Number of Columns</span>
                                <or-mwc-input type="${InputType.NUMBER}" compact outlined .value="${this.template.columns}" max="12" @or-mwc-input-changed="${(event: OrInputChangedEvent) => { if(this.template != null) { this.template.columns = event.detail.value as number; this.forceParentRerender(); }}}"></or-mwc-input>
                            </div>
                            <!-- Max Screen Width control-->
                            <div style="margin-bottom: 24px; display: flex; align-items: center;">
                                <span style="min-width: 150px;">Max Screen Width</span>
                                <or-mwc-input type="${InputType.NUMBER}" compact outlined .value="${this.template.maxScreenWidth}" disabled @or-mwc-input-changed="${(event: OrInputChangedEvent) => { if(this.template != null) { this.template.maxScreenWidth = event.detail.value as number; this.forceParentUpdate(); }}}"></or-mwc-input>
                                <span style="margin-left: 8px;">px</span>
                            </div>
                        </div>
                    ` : null}
                </div>
                <!-------------------->
                <div>${this.generateExpandableHeader('Display')}</div>
                <div>
                    ${this.expandedPanels.includes('Display') ? html`
                        <div style="padding: 24px 24px 48px 24px;">
                            ${screenPresets.map((preset) => {
                                const scalingPresets = [DashboardScalingPreset.KEEP_LAYOUT, DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN, DashboardScalingPreset.REDIRECT, DashboardScalingPreset.BLOCK_DEVICE];
                                return html`
                                    <div style="margin-bottom: 24px;">
                                        <div>
                                            <span>On a</span>
                                            <span style="font-weight: bold;">${preset.displayName}</span>
                                            <span>Screen my board should:</span>
                                        </div>
                                        <or-mwc-input class="displayInput" type="${InputType.SELECT}" style="width: 250px;"
                                                      .options="${scalingPresets.map((x) => scalingPresetToString(x))}"
                                                      .value="${scalingPresetToString(preset.scalingPreset)}"
                                                      @or-mwc-input-changed="${(event: OrInputChangedEvent) => { preset.scalingPreset = stringToScalingPreset(event.detail.value); this.forceParentRerender(); }}"
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
                        <div style="padding: 24px 24px 48px 24px;">
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
