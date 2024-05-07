import { customElement } from "lit/decorators.js";
import {OrWidget, WidgetManifest} from "../util/or-widget";
import {TemplateResult, html} from "lit";
import {WidgetSettings} from "../util/widget-settings";
import {WidgetConfig} from "../util/widget-config";
import {GatewaySettings} from "../settings/gateway-settings";
import { InputType } from "@openremote/or-mwc-components/or-mwc-input";

export interface GatewayWidgetConfig extends WidgetConfig {

}

function getDefaultWidgetConfig(): GatewayWidgetConfig {
    return {};
}

@customElement("gateway-widget")
export class GatewayWidget extends OrWidget {

    static getManifest(): WidgetManifest {
        return {
            displayName: "Gateway",
            displayIcon: "lan-connect",
            minColumnWidth: 1,
            minColumnHeight: 1,
            getContentHtml(config: GatewayWidgetConfig): OrWidget {
                return new GatewayWidget(config);
            },
            getSettingsHtml(config: GatewayWidgetConfig): WidgetSettings {
                return new GatewaySettings(config);
            },
            getDefaultConfig(): GatewayWidgetConfig {
                return getDefaultWidgetConfig();
            }
        }
    }

    refreshContent(force: boolean): void {
    }

    protected render(): TemplateResult {
        return html`
            <div>
                <or-mwc-input .type="${InputType.BUTTON}" label="Open"></or-mwc-input>
            </div>
        `;
    }

}
