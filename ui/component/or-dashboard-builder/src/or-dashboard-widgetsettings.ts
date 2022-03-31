import {css, html, LitElement, unsafeCSS } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { InputType } from "../../or-mwc-components/lib/or-mwc-input";
import {style} from './style';
import {AddOutput} from "./or-dashboard-editor";

const tableStyle = require("@material/data-table/dist/mdc.data-table.css");

//language=css
const widgetSettingsStyling = css`
    
`

@customElement("or-dashboard-widgetsettings")
export class OrDashboardWidgetsettings extends LitElement {

    static get styles() {
        return [unsafeCSS(tableStyle), widgetSettingsStyling, style]
    }

    @property({type: Object})
    protected selectedWidget: any;

    @state()
    protected expandedPanels: string[];

    constructor() {
        super();
        this.expandedPanels = ['Attributes', 'Display'];
    }

    expandPanel(panelName: string): void {
        if(this.expandedPanels.includes(panelName)) {
            const indexOf = this.expandedPanels.indexOf(panelName, 0);
            if(indexOf > -1) { this.expandedPanels.splice(indexOf, 1); }
        } else {
            this.expandedPanels.push(panelName);
        }
        this.requestUpdate();
    }

    deleteSelected() {
        this.dispatchEvent(new CustomEvent("delete", {detail: { widget: this.selectedWidget }}));
    }

    protected render() {
        return html`
            <div>
                <div id="settings">
                    <div id="settings-panels">
                        <div>
                            <button style="display: flex; align-items: center; padding: 12px; background: #F0F0F0; width: 100%; border: none;" @click="${() => { this.expandPanel('Attributes'); }}">
                                <or-icon icon="${this.expandedPanels.includes('Attributes') ? 'chevron-down' : 'chevron-right'}"></or-icon>
                                <span style="margin-left: 6px;">Attributes</span>
                            </button>
                        </div>
                        <div>
                            ${this.expandedPanels.includes('Attributes') ? html`
                                <div style="padding: 12px;">
                                    <span>Setting 1</span>
                                </div>
                            ` : null}
                        </div>
                        <div>
                            <button style="display: flex; align-items: center; padding: 12px; background: #F0F0F0; width: 100%; border: none;" @click="${() => { this.expandPanel('Display'); }}">
                                <or-icon icon="${this.expandedPanels.includes('Display') ? 'chevron-down' : 'chevron-right'}"></or-icon>
                                <span style="margin-left: 6px;">Display</span>
                            </button>
                        </div>
                        <div>
                            ${this.expandedPanels.includes('Display') ? html`
                            <div style="padding: 12px;">
                                <span>Setting 2</span>
                            </div>
                        ` : null}
                        </div>
                        <div>
                            <div style="display: flex; align-items: center; padding: 12px; background: #F0F0F0;">
                                <or-icon icon="chevron-right"></or-icon>
                                <span style="margin-left: 6px;">Settings</span>
                            </div>
                        </div>
                    </div>
                </div>
                <div id="actions" style="position: absolute; bottom: 20px; right: 20px;">
                    <or-mwc-input type="${InputType.BUTTON}" outlined icon="delete" label="Delete Component" @click="${() => { this.deleteSelected(); }}"></or-mwc-input>
                </div>
            </div>
        `
    }
}
