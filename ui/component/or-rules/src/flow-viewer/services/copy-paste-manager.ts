import { Node, NodeConnection, NodePosition } from "@openremote/model";
import { LightNodeCollection } from "../models/light-node-collection";
import { IdentityAssigner } from "../node-structure";
import { input } from "../components/flow-editor";
import { FlowNode, ConnectionLine } from "../flow-viewer";

export class CopyPasteManager {
    private clipboard!: LightNodeCollection;
    private copyOrigin!: NodePosition;

    public get isFull() { return !!!this.clipboard; }

    public putInClipboard(obj: LightNodeCollection, origin: NodePosition) {
        this.copyOrigin = origin;
        this.clipboard = obj;
    }

    public getFromClipboard(newOrigin: NodePosition) {
        const clone = this.cloneIsolated(this.clipboard, 0);
        const offset = {
            x: newOrigin.x! - this.copyOrigin.x!,
            y: newOrigin.y! - this.copyOrigin.y!,
        };
        clone.nodes.forEach((node) => {
            node.position!.x! += offset.x;
            node.position!.y! += offset.y;
        });
        return clone;
    }

    public cloneIsolated(obj: LightNodeCollection, positionOffset = 50) {
        const remapped: { [id: string]: string } = {};

        const clone: LightNodeCollection = JSON.parse(JSON.stringify(obj));

        clone.nodes.forEach((node: Node) => {
            const newNodeID = IdentityAssigner.generateIdentity();
            remapped[node.id!] = newNodeID;
            node.position!.x! += positionOffset;
            node.position!.y! += positionOffset;
            node.id = newNodeID;
            node.inputs!.forEach((inputSocket) => {
                const newSocketID = IdentityAssigner.generateIdentity();
                remapped[inputSocket.id!] = newSocketID;
                inputSocket.id = newSocketID;
                inputSocket.nodeId = newNodeID;
            });
            node.outputs!.forEach((outputSocket) => {
                const newSocketID = IdentityAssigner.generateIdentity();
                remapped[outputSocket.id!] = newSocketID;
                outputSocket.id = newSocketID;
                outputSocket.nodeId = newNodeID;
            });
        });

        clone.connections.forEach((connection: NodeConnection) => {
            connection.from = remapped[connection.from!];
            connection.to = remapped[connection.to!];
        });

        return clone;
    }

    public copy(x: number, y: number) {
        const selectedNodes = input.selected.filter((s) => s instanceof FlowNode && s.selected) as FlowNode[];
        const selectedConnections = input.selected.filter((s) => s instanceof ConnectionLine && s.selected) as ConnectionLine[];
        this.putInClipboard({
            nodes: selectedNodes.map((n) => n.node),
            connections: selectedConnections.map((c) => c.connection)
        }, { x, y });
    }
}
