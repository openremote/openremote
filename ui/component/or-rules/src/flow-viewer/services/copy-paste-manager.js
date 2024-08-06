import { IdentityAssigner } from "../node-structure";
import { input } from "../components/flow-editor";
import { FlowNode, ConnectionLine } from "../flow-viewer";
export class CopyPasteManager {
    get isFull() { return !!!this.clipboard; }
    putInClipboard(obj, origin) {
        this.copyOrigin = origin;
        this.clipboard = obj;
    }
    getFromClipboard(newOrigin) {
        const clone = this.cloneIsolated(this.clipboard, 0);
        const offset = {
            x: newOrigin.x - this.copyOrigin.x,
            y: newOrigin.y - this.copyOrigin.y,
        };
        clone.nodes.forEach((node) => {
            node.position.x += offset.x;
            node.position.y += offset.y;
        });
        return clone;
    }
    cloneIsolated(obj, positionOffset = 50) {
        const remapped = {};
        const clone = JSON.parse(JSON.stringify(obj));
        clone.nodes.forEach((node) => {
            const newNodeID = IdentityAssigner.generateIdentity();
            remapped[node.id] = newNodeID;
            node.position.x += positionOffset;
            node.position.y += positionOffset;
            node.id = newNodeID;
            node.inputs.forEach((inputSocket) => {
                const newSocketID = IdentityAssigner.generateIdentity();
                remapped[inputSocket.id] = newSocketID;
                inputSocket.id = newSocketID;
                inputSocket.nodeId = newNodeID;
            });
            node.outputs.forEach((outputSocket) => {
                const newSocketID = IdentityAssigner.generateIdentity();
                remapped[outputSocket.id] = newSocketID;
                outputSocket.id = newSocketID;
                outputSocket.nodeId = newNodeID;
            });
        });
        clone.connections.forEach((connection) => {
            connection.from = remapped[connection.from];
            connection.to = remapped[connection.to];
        });
        return clone;
    }
    copy(x, y) {
        const selectedNodes = input.selected.filter((s) => s instanceof FlowNode && s.selected);
        const selectedConnections = input.selected.filter((s) => s instanceof ConnectionLine && s.selected);
        this.putInClipboard({
            nodes: selectedNodes.map((n) => n.node),
            connections: selectedConnections.map((c) => c.connection)
        }, { x, y });
    }
}
//# sourceMappingURL=copy-paste-manager.js.map