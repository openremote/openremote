import {LitElement, PropertyValues, TemplateResult } from "lit";
import { property } from "lit/decorators.js";
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

export abstract class WidgetSettings extends LitElement {

    @property()
    protected readonly widgetConfig: WidgetConfig;

    static get styles() {
        return [style];
    }

    protected abstract render(): TemplateResult

    constructor(config: WidgetConfig) {
        super();
        this.widgetConfig = config;
    }


    // Lit lifecycle for "on every update" which triggers on every property/state change
    protected willUpdate(changedProps: PropertyValues) {
        if(changedProps.has('widgetConfig') && this.widgetConfig) {
            this.dispatchEvent(new WidgetSettingsChangedEvent(this.widgetConfig));
        }
    }

    protected notifyConfigUpdate() {
        this.requestUpdate('widgetConfig');
    }


    /* ----------------------------- */

    public getDisplayName?: () => string | undefined;

    public setDisplayName?: (name?: string) => void;

    public getEditMode?: () => boolean;

    public getWidgetLocation?: () => { x?: number, y?: number, h?: number, w?: number }
}
