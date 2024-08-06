import { PropertyValues, TemplateResult } from "lit";
import { OrAssetWidget } from "../util/or-asset-widget";
import { WidgetConfig } from "../util/widget-config";
import { AttributeRef } from "@openremote/model";
import { WidgetManifest } from "../util/or-widget";
import "@openremote/or-attribute-input";
export interface AttributeInputWidgetConfig extends WidgetConfig {
    attributeRefs: AttributeRef[];
    readonly: boolean;
    showHelperText: boolean;
}
export declare class AttributeInputWidget extends OrAssetWidget {
    protected widgetConfig: AttributeInputWidgetConfig;
    protected widgetWrapperElem?: HTMLElement;
    protected attributeInputElems?: NodeList;
    protected resizeObserver?: ResizeObserver;
    static getManifest(): WidgetManifest;
    refreshContent(force: boolean): void;
    static get styles(): import("lit").CSSResult[];
    disconnectedCallback(): void;
    protected updated(changedProps: PropertyValues): void;
    protected loadAssets(attributeRefs: AttributeRef[]): void;
    protected render(): TemplateResult;
}
