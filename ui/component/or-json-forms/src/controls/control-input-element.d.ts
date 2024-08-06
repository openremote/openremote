import { InputType, OrInputChangedEvent } from "@openremote/or-mwc-components/or-mwc-input";
import { ControlBaseElement } from "./control-base-element";
export declare class ControlInputElement extends ControlBaseElement {
    protected inputType: InputType;
    static get styles(): import("lit").CSSResult[];
    render(): import("lit-html").TemplateResult<1>;
    protected onValueChanged(e: OrInputChangedEvent): void;
}
