import { customElement } from "lit/decorators.js";
import {WidgetSettings} from "../util/widget-settings";
import {TemplateResult, html} from "lit";

@customElement("gateway-settings")
export class GatewaySettings extends WidgetSettings {

    protected render(): TemplateResult {
        return html`
            <span>Gateway widget settings</span>
        `;
    }

}
