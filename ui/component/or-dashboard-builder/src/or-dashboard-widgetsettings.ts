import {css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import {style} from './style';

//language=css
const widgetSettingsStyling = css`
    
`

@customElement("or-dashboard-widgetsettings")
export class OrDashboardWidgetsettings extends LitElement {

    static get styles() {
        return [widgetSettingsStyling, style]
    }

    @property({})
    protected selectedWidget: any;

    protected render() {
        return html`
            <div>
                <span></span>
            </div>
        `
    }
}
