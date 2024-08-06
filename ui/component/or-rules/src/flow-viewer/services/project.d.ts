/// <reference types="node" />
import { Node, NodeConnection, NodeSocket, NodeCollection } from "@openremote/model";
import { EventEmitter } from "events";
import { EditorWorkspace } from "../components/editor-workspace";
export declare class Project extends EventEmitter {
    nodes: Node[];
    connections: NodeConnection[];
    workspace: EditorWorkspace;
    existingFlowRuleId: number;
    existingFlowRuleName: string | null;
    existingFlowRuleDesc: string | null;
    private isInUnsavedState;
    private isConnecting;
    private connectionStartSocket;
    private connectionEndSocket;
    private history;
    private historyIndex;
    constructor();
    get isCurrentlyConnecting(): boolean;
    set unsavedState(state: boolean);
    get unsavedState(): boolean;
    setCurrentProject(id: number, name: string | null, desc: string | null): void;
    notifyChange(): void;
    createUndoSnapshot(): void;
    undo(): Promise<void>;
    redo(): Promise<void>;
    fromNodeCollection(collection: NodeCollection): Promise<void>;
    toNodeCollection(name: string, description: string): NodeCollection;
    clear(alsoResetProject?: boolean): Promise<void>;
    addNode(node: Node): void;
    removeNode(node: Node): void;
    startConnectionDrag: (e: MouseEvent, socket: NodeSocket, isInputNode: boolean) => void;
    connectionDragging: (e: MouseEvent) => void;
    endConnectionDrag: (e: MouseEvent, socket: NodeSocket | null, isInputNode: boolean) => void;
    removeConnection(connection: NodeConnection): void;
    isValidConnection(connection: NodeConnection): boolean;
    createConnection(fromSocket: string, toSocket: string): boolean;
    removeInvalidConnections(): void;
}
