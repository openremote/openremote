import { LitElement } from "lit";
import { Node } from "@openremote/model";
import { FlowEditor } from "./flow-editor";
import { Camera } from "../models/camera";
declare const EditorWorkspace_base: (new (...args: any[]) => {
    _i18nextJustInitialized: boolean;
    connectedCallback(): void;
    disconnectedCallback(): void;
    shouldUpdate(changedProps: Map<PropertyKey, unknown> | import("lit").PropertyValueMap<any>): any;
    initCallback: (options: import("i18next").InitOptions) => void;
    langChangedCallback: () => void;
    readonly isConnected: boolean;
}) & typeof LitElement;
export declare class EditorWorkspace extends EditorWorkspace_base {
    get clientRect(): ClientRect;
    static get styles(): import("lit").CSSResult;
    get halfSize(): {
        x: number;
        y: number;
    };
    private get isCameraInDefaultPosition();
    camera: Camera;
    topNodeZindex: number;
    scrollSensitivity: number;
    zoomLowerBound: number;
    zoomUpperBound: number;
    application: FlowEditor;
    private connectionDragging;
    private connectionFrom;
    private connectionTo;
    private isPanning;
    private cachedClientRect;
    constructor();
    resetCamera(): void;
    fitCamera(nodes: Node[], padding?: number): void;
    offsetToWorld(point: {
        x?: number;
        y?: number;
    }): {
        x: number;
        y: number;
    };
    worldToOffset(point: {
        x?: number;
        y?: number;
    }): {
        x: number;
        y: number;
    };
    pageToOffset(point: {
        x?: number;
        y?: number;
    }): {
        x: number;
        y: number;
    };
    pasteAt(x: number, y: number): Promise<void>;
    protected firstUpdated(): void;
    protected render(): import("lit-html").TemplateResult<1>;
    private startPan;
    private onMove;
    private onZoom;
    private onEmptyConnectionRelease;
    private stopPan;
}
export {};
