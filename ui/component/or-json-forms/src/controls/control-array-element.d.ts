import { JsonSchema } from "@jsonforms/core";
import { PropertyValues, TemplateResult } from "lit";
import { CombinatorInfo } from "../util";
import "@openremote/or-mwc-components/or-mwc-list";
import { ControlBaseElement } from "./control-base-element";
export declare class ControlArrayElement extends ControlBaseElement {
    protected minimal?: boolean;
    protected resolvedSchema: JsonSchema;
    protected itemInfos: CombinatorInfo[] | undefined;
    protected addItem: (value: any) => void;
    protected removeItem: (index: number) => void;
    protected moveItem: (fromIndex: number, toIndex: number) => void;
    static get styles(): import("lit").CSSResult[];
    shouldUpdate(_changedProperties: PropertyValues): boolean;
    render(): TemplateResult<1>;
    protected getArrayItemWrapper(elementTemplate: TemplateResult, index: number): TemplateResult<1>;
    protected _onDragStart(ev: DragEvent): void;
    protected _onDragEnd(ev: DragEvent): void;
    protected _onDragOver(ev: DragEvent): void;
    protected _showJson(ev: Event): void;
    protected doAddItem(): void;
    protected showAddDialog(): void;
}
