import { LitElement } from "lit";
import { Node } from "@openremote/model";
import { OrMwcDrawer } from "@openremote/or-mwc-components/or-mwc-drawer";
import { FlowEditor } from "./flow-editor";
declare const NodePanel_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class NodePanel extends NodePanel_base {
    nodes: Node[];
    static get styles(): import("lit").CSSResult;
    drawer: OrMwcDrawer;
    application: FlowEditor;
    protected firstUpdated(): void;
    protected render(): import("lit-html").TemplateResult<1>;
    private nodeTemplate;
    private get listTemplate();
}
export {};
