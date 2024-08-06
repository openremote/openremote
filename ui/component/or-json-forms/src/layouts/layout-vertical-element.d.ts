import { GroupLayout, JsonSchema, StatePropsOfControl, VerticalLayout } from "@jsonforms/core";
import { TemplateResult } from "lit";
import { LayoutBaseElement } from "./layout-base-element";
import "@openremote/or-mwc-components/or-mwc-list";
import "@openremote/or-components/or-collapsible-panel";
export declare class LayoutVerticalElement extends LayoutBaseElement<VerticalLayout | GroupLayout> {
    protected minimal?: boolean;
    protected type?: string;
    handleChange: (path: string, data: any) => void;
    static get styles(): import("lit").CSSResult[];
    render(): TemplateResult<1>;
    protected _getDynamicContentTemplate(dynamicPropertyRegex: string, dynamicValueSchema: JsonSchema): TemplateResult | undefined;
    protected _showJson(ev: Event): void;
    protected _addParameter(rootSchema: JsonSchema, optionalProps: StatePropsOfControl[], dynamicPropertyRegex?: string, dynamicValueSchema?: JsonSchema): void;
}
