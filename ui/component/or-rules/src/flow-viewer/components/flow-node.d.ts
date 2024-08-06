import { Node } from "@openremote/model";
import { SelectableElement } from "./selectable-element";
import { EditorWorkspace } from "./editor-workspace";
export declare class FlowNode extends SelectableElement {
    node: Node;
    workspace: EditorWorkspace;
    frozen: boolean;
    private minimal;
    private isBeingDragged;
    constructor();
    static get styles(): import("lit").CSSResult;
    disconnectedCallback(): void;
    bringToFront(): void;
    protected firstUpdated(): Promise<void>;
    protected updated(): Promise<void>;
    protected render(): import("lit-html").TemplateResult<1>;
    private forceUpdate;
    private setTranslate;
    private startDrag;
    private onDrag;
    private stopDrag;
    private linkIdentity;
}
