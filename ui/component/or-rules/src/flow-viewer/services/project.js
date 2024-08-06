var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { EventEmitter } from "events";
import { SocketTypeMatcher, NodeUtilities } from "../node-structure";
import { input } from "../components/flow-editor";
export class Project extends EventEmitter {
    constructor() {
        super();
        this.nodes = [];
        this.connections = [];
        this.existingFlowRuleId = -1;
        this.existingFlowRuleName = null;
        this.existingFlowRuleDesc = null;
        this.isInUnsavedState = false;
        this.isConnecting = false;
        this.history = [];
        this.historyIndex = 0;
        this.startConnectionDrag = (e, socket, isInputNode) => {
            this.connectionStartSocket = null;
            this.connectionEndSocket = null;
            if (isInputNode) {
                this.connectionEndSocket = socket;
            }
            else {
                this.connectionStartSocket = socket;
            }
            this.isConnecting = true;
            this.emit("connectionstart", e, socket);
        };
        this.connectionDragging = (e) => {
            this.emit("connecting", e);
        };
        this.endConnectionDrag = (e, socket, isInputNode) => {
            if (!this.isConnecting) {
                return;
            }
            if (isInputNode) {
                this.connectionEndSocket = socket;
            }
            else {
                this.connectionStartSocket = socket;
            }
            this.isConnecting = false;
            this.emit("connectionend", e, socket);
            if (this.connectionEndSocket && this.connectionStartSocket) {
                this.createConnection(this.connectionStartSocket.id, this.connectionEndSocket.id);
            }
        };
        this.setMaxListeners(1024);
    }
    get isCurrentlyConnecting() {
        return this.isConnecting;
    }
    set unsavedState(state) {
        this.isInUnsavedState = state;
        this.emit("unsavedstateset", state);
    }
    get unsavedState() {
        return this.isInUnsavedState;
    }
    setCurrentProject(id, name, desc) {
        this.unsavedState = false;
        this.existingFlowRuleId = id;
        this.existingFlowRuleName = name;
        this.existingFlowRuleDesc = desc;
        this.history = [];
        this.emit("projectset", id);
    }
    notifyChange() {
        this.unsavedState = true;
        this.emit("changed");
    }
    createUndoSnapshot() {
        this.history = this.history.splice(0, this.historyIndex + 1);
        this.history.push(JSON.parse(JSON.stringify(this.toNodeCollection("", ""))));
        this.historyIndex = this.history.length - 1;
    }
    undo() {
        return __awaiter(this, void 0, void 0, function* () {
            if (this.history.length === 0 || this.historyIndex === -1) {
                return;
            }
            if (this.historyIndex === this.history.length - 1) {
                this.history.push(JSON.parse(JSON.stringify(this.toNodeCollection("", ""))));
            }
            yield this.fromNodeCollection(this.history[this.historyIndex]);
            this.historyIndex--;
            this.notifyChange();
        });
    }
    redo() {
        return __awaiter(this, void 0, void 0, function* () {
            if (this.history.length === 0 || this.historyIndex >= (this.history.length - 2)) {
                return;
            }
            this.historyIndex++;
            yield this.fromNodeCollection(this.history[this.historyIndex + 1]);
            this.notifyChange();
        });
    }
    fromNodeCollection(collection) {
        return __awaiter(this, void 0, void 0, function* () {
            yield this.clear();
            collection.nodes.forEach((node) => {
                this.addNode(node);
            });
            collection.connections.forEach((conn) => {
                this.createConnection(conn.from, conn.to);
            });
            this.emit("nodecollectionloaded", collection);
            this.unsavedState = false;
        });
    }
    toNodeCollection(name, description) {
        return {
            name,
            description,
            connections: this.connections,
            nodes: this.nodes
        };
    }
    clear(alsoResetProject = false) {
        return __awaiter(this, void 0, void 0, function* () {
            input.clearSelection();
            this.nodes.forEach((n) => {
                this.removeNode(n);
            });
            this.nodes = [];
            this.connections = [];
            this.unsavedState = !alsoResetProject;
            if (alsoResetProject) {
                this.setCurrentProject(-1, null, null);
            }
            this.emit("cleared");
        });
    }
    addNode(node) {
        if (this.nodes.filter((n) => n.id === node.id).length > 0) {
            throw new Error("Node with identical identity already exists in the project");
        }
        this.nodes.push(node);
        this.emit("nodeadded", node);
    }
    removeNode(node) {
        input.clearSelection();
        this.connections.filter((c) => {
            const from = NodeUtilities.getSocketFromID(c.from, this.nodes);
            const to = NodeUtilities.getSocketFromID(c.to, this.nodes);
            return from.nodeId === node.id || to.nodeId === node.id;
        }).forEach((c) => {
            this.removeConnection(c);
        });
        this.nodes.filter((n) => n.id === node.id).forEach((n) => {
            this.nodes.splice(this.nodes.indexOf(n), 1);
            this.emit("noderemoved", n);
        });
    }
    removeConnection(connection) {
        input.clearSelection();
        this.connections.filter((c) => c.from === connection.from && c.to === connection.to).forEach((c) => {
            const index = this.connections.indexOf(c);
            if (index === -1) {
                console.warn("attempt to delete nonexistent connection");
            }
            else {
                this.connections.splice(index, 1);
                this.emit("connectionremoved", c);
            }
        });
    }
    isValidConnection(connection) {
        const fromSocket = NodeUtilities.getSocketFromID(connection.from, this.nodes);
        const toSocket = NodeUtilities.getSocketFromID(connection.to, this.nodes);
        if (!fromSocket ||
            !toSocket) {
            return false;
        }
        if (!SocketTypeMatcher.match(fromSocket.type, toSocket.type) ||
            fromSocket.id === toSocket.id ||
            fromSocket.nodeId === toSocket.nodeId) {
            return false;
        }
        return true;
    }
    createConnection(fromSocket, toSocket) {
        const connection = {
            from: fromSocket,
            to: toSocket
        };
        if (!this.isValidConnection(connection)) {
            return false;
        }
        for (const c of this.connections.filter((j) => j.to === toSocket)) {
            this.removeConnection(c);
        }
        this.connections.push(connection);
        this.emit("connectioncreated", fromSocket, toSocket);
        return true;
    }
    removeInvalidConnections() {
        for (const c of this.connections.filter((j) => !this.isValidConnection(j))) {
            this.removeConnection(c);
        }
    }
}
//# sourceMappingURL=project.js.map