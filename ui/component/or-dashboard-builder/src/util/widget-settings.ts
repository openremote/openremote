import {css, LitElement, PropertyValues, TemplateResult } from "lit";
import { property } from "lit/decorators.js";
import {AttributeRef, DashboardWidget} from "@openremote/model";
import {SettingsPanel} from "./settings-panel";
import {WidgetConfig} from "./widget-config";
import {style} from "../style";

export class WidgetSettingsChangedEvent extends CustomEvent<WidgetConfig> {

    public static readonly NAME = "settings-changed";

    constructor(widgetConfig: WidgetConfig) {
        super(WidgetSettingsChangedEvent.NAME, {
            bubbles: true,
            composed: true,
            detail: widgetConfig
        });
    }

}

const styling = css`

`

export abstract class WidgetSettings extends LitElement {

    @property()
    protected readonly widgetConfig: WidgetConfig;

    static get styles() {
        return [styling, style];
    }

    protected abstract render(): TemplateResult

    constructor(config: WidgetConfig) {
        super();
        this.widgetConfig = config;
    }


    // Lit lifecycle for "on every update" which triggers on every property/state change
    protected willUpdate(changedProps: PropertyValues) {
        console.log(changedProps);
        if(changedProps.has('widgetConfig') && this.widgetConfig) {
            this.dispatchEvent(new WidgetSettingsChangedEvent(this.widgetConfig));
        }
    }

    protected notifyConfigUpdate() {
        this.requestUpdate('widgetConfig');
    }
}
