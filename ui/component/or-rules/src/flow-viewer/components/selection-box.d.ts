import { LitElement } from "lit";
import { EditorWorkspace } from "./editor-workspace";
export declare class SelectionBox extends LitElement {
    static get styles(): import("lit").CSSResult;
    workspace: EditorWorkspace;
    readonly distanceTreshold = 20;
    private isSelecting;
    private selectionStartPosition;
    private selectionBox;
    protected firstUpdated(): void;
    protected render(): import("lit-html").TemplateResult<1>;
    private workspaceMouseDown;
    private mouseMove;
    private workspaceMouseUp;
    private selectInBounds;
}
