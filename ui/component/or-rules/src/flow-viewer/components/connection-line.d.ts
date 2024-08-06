import { NodeConnection } from "@openremote/model";
import { SelectableElement } from "./selectable-element";
import { EditorWorkspace } from "./editor-workspace";
export declare class ConnectionLine extends SelectableElement {
    connection: NodeConnection;
    workspace: EditorWorkspace;
    private polylineId;
    private fromNodeElement;
    private toNodeElement;
    private fromElement;
    private toElement;
    private resizeObserver;
    private readonly fancyLine;
    private readonly curveIntensity;
    constructor();
    disconnectedCallback(): void;
    protected firstUpdated(): void;
    protected render(): import("lit-html").TemplateResult<1>;
    static get styles(): import("lit").CSSResult;
    private nodeChanged;
    private get isInvalid();
}
