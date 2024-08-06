import { LitElement } from "lit";
import { Node } from "@openremote/model";
declare const NodeMenuItem_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class NodeMenuItem extends NodeMenuItem_base {
    node: Node;
    private isDragging;
    private x;
    private y;
    private workspace;
    private xOffset;
    private yOffset;
    constructor();
    static get styles(): import("lit").CSSResult;
    protected render(): import("lit-html").TemplateResult<1>;
    private get dragNodeTemplate();
    private get flowNodeName();
    private startDrag;
    private onMove;
    private stopDrag;
}
export {};
