import { LitElement } from "lit";
import { NodeSocket } from "@openremote/model";
declare const FlowNodeSocket_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class FlowNodeSocket extends FlowNodeSocket_base {
    get connectionPosition(): {
        x: number;
        y: number;
    };
    static get styles(): import("lit").CSSResult;
    get socketTypeString(): string;
    socket: NodeSocket;
    side: "input" | "output";
    renderLabel: boolean;
    private identityDeleted;
    private circleElem;
    disconnectedCallback(): void;
    protected firstUpdated(): void;
    protected updated(): void;
    protected render(): import("lit-html").TemplateResult<1>;
    private forceUpdate;
    private linkIdentity;
}
export {};
