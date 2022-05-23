import {DashboardScalingPreset, DashboardTemplate } from "@openremote/model";
import {css, html, LitElement, TemplateResult } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import {style} from './style';

//language=css
const boardSettingsStyling = css`
    
    .expandableHeader {
        display: flex;
        align-items: center;
        padding: 12px;
        background: #F0F0F0;
        width: 100%;
        border: none;
    }
`

@customElement("or-dashboard-boardsettings")
export class OrDashboardBoardsettings extends LitElement {

    @property()
    protected readonly template?: DashboardTemplate;

    @state()
    protected expandedPanels: string[] = ["Permissions", "Layout", "Display"];

    constructor() {
        super();
        this.updateComplete.then(() => {
            if(this.shadowRoot != null) {
                console.log(this.shadowRoot);
                this.shadowRoot.querySelectorAll(".displayInput").forEach(element => {
                    (element.shadowRoot?.querySelector(".mdc-menu") as HTMLElement).style.minWidth = "250px";
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

    // UI generation of an expandable panel header.
    generateExpandableHeader(name: string): TemplateResult {
        return html`
            <button class="expandableHeader" @click="${() => { this.expandPanel(name); }}">
                <or-icon icon="${this.expandedPanels.includes(name) ? 'chevron-down' : 'chevron-right'}"></or-icon>
                <span style="margin-left: 6px;">${name}</span>
            </button>
        `
    }

    // Method when a user opens or closes a expansion panel. (UI related)
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

    scalingPresetToString(scalingPreset: DashboardScalingPreset | undefined): string {
        if(scalingPreset != null) {
            switch (scalingPreset) {
                case DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN: {
                    return "Wrap widgets to one column";
                } case DashboardScalingPreset.RESIZE_WIDGETS: {
                    return "Resize the Widgets";
                } case DashboardScalingPreset.BLOCK_DEVICE: {
                    return "Block this device";
                } case DashboardScalingPreset.REDIRECT: {
                    return "Redirect to a different Dashboard.."
                }
            }
        }
        return "undefined";
    }

    stringToScalingPreset(scalingPreset: string): DashboardScalingPreset {
        switch (scalingPreset) {
            case "Wrap widgets to one column": { return DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN; }
            case "Resize the Widgets": { return DashboardScalingPreset.RESIZE_WIDGETS; }
            case "Block this device": { return DashboardScalingPreset.BLOCK_DEVICE; }
            case "Redirect to a different Dashboard..": { return DashboardScalingPreset.REDIRECT; }
            default: { return DashboardScalingPreset.RESIZE_WIDGETS; }
        }

    }

    /* -------------------------------- */

    protected render() {
        if(this.template != null) {
            return html`
                <div>
                    ${this.generateExpandableHeader('Permissions')}
                </div>
                <div>
                    ${this.expandedPanels.includes('Permissions') ? html`
                        <div style="padding: 24px 24px 48px 24px;">
                            <span>Not created yet.</span>
                        </div>
                    ` : null}
                </div>
                <div>
                    ${this.generateExpandableHeader('Layout')}
                </div>
                <div>
                    ${this.expandedPanels.includes('Layout') ? html`
                        <div style="padding: 24px 24px 48px 24px;">
                            <span>Not created yet.</span>
                        </div>
                    ` : null}
                </div>
                <div>
                    ${this.generateExpandableHeader('Display')}
                </div>
                <div>
                    ${this.expandedPanels.includes('Display') ? html`
                        <div style="padding: 24px 24px 48px 24px;">
                            ${this.template.screenPresets?.map((preset) => {
                                const scalingPresets = [DashboardScalingPreset.WRAP_TO_SINGLE_COLUMN, DashboardScalingPreset.RESIZE_WIDGETS, DashboardScalingPreset.REDIRECT, DashboardScalingPreset.BLOCK_DEVICE];
                                return html`
                                    <div style="margin-bottom: 24px;">
                                        <div>
                                            <span>On a</span>
                                            <span style="font-weight: bold;">${preset.displayName}</span>
                                            <span>Screen my board should:</span>
                                        </div>
                                        <or-mwc-input class="displayInput" type="${InputType.SELECT}" style="width: 250px;"
                                                      .options="${scalingPresets.map((x) => this.scalingPresetToString(x))}"
                                                      .value="${this.scalingPresetToString(preset.scalingPreset)}"
                                                      @or-mwc-input-changed="${(event: OrInputChangedEvent) => { preset.scalingPreset = this.stringToScalingPreset(event.detail.value); this.forceParentRerender(); }}"
                                        ></or-mwc-input>
                                    </div>
                                `
                            })}
                        </div>
                    ` : null}
                </div>
                <div>
                    ${this.generateExpandableHeader('Breakpoints')}
                </div>
                <div>
                    ${this.expandedPanels.includes('Breakpoints') ? html`
                        <div style="padding: 24px 24px 48px 24px;">
                            <span>Not created yet.</span>
                        </div>
                    ` : null}
                </div>
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

}
