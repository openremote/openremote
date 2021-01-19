import {Node, NodeCollection, NodeDataType, NodePosition, NodeType, ValueDescriptor} from "@openremote/model";

export class NodeUtilities {
    public static getNodeFromID(id: string, nodes: Node[]) {
        const node = nodes.find((n) => n.id === id);
        if (!node) { console.warn(`Node with ID ${id} not found`); }
        return node;
    }

    public static getSocketFromID(id: string, nodes: Node[]) {
        for (const node of nodes) {
            for (const input of node.inputs!) {
                if (input.id === id) { return input; }
            }
            for (const output of node.outputs!) {
                if (output.id === id) { return output; }
            }
        }
    }

    public static convertValueTypeToSocketType(value: ValueDescriptor): NodeDataType {
        switch (value.jsonType) {
            case "boolean": return NodeDataType.BOOLEAN;
            case "number":
            case "bigint": return NodeDataType.NUMBER;
            case "string": return NodeDataType.STRING;
            default: return NodeDataType.ANY;
        }
    }

    public static backtrackFrom(collection: NodeCollection, nodeId: string): Node[] {
        if (!collection) { throw new Error("Collection has to exist"); }
        if (!collection.nodes) { throw new Error("Collection has to have existing nodes"); }
        if (!collection.connections) { throw new Error("Collection has to have existing connections"); }

        const node = this.getNodeFromID(nodeId, collection.nodes);

        if (!node) { throw new Error("Node has to exist"); }
        if (!node.inputs) { throw new Error("Node has to have existing inputs"); }
        if (!node.outputs) { throw new Error("Node has to have existing outputs"); }

        let total: Node[] = [];
        let children: Node[] = [];

        for (const s of node.inputs) {
            children = children.concat(collection.connections.
                filter((c) => c.to === s.id).
                map((c) => this.getNodeFromID(this.getSocketFromID(c.from!, collection.nodes!)!.nodeId!, collection.nodes!)!));
        }

        for (const child of children) {
            total.push(child);
            const result = this.backtrackFrom(collection, child.id!);
            total = total.concat(result);
        }

        return total;
    }

    public static validate(collection: NodeCollection) {
        if (!collection) { return false; }
        if (!collection.nodes) { return false; }
        if (!collection.connections) { return false; }
        const outputNodes = collection.nodes.filter((n) => n.type === NodeType.OUTPUT);
        if (outputNodes.length === 0) { return false; }

        for (const output of outputNodes) {
            const tree = this.backtrackFrom(collection, output.id!);
            if (tree.length === 0) { return false; }
            if (tree.filter((n) => n.type === NodeType.INPUT).length === 0) { return false; }
        }
        
        return true;
    }

    public static add(a: NodePosition, b: NodePosition): NodePosition {
        return { x: a.x! + b.x!, y: a.y! + b.y! };
    }

    public static subtract(a: NodePosition, b: NodePosition): NodePosition {
        return { x: a.x! - b.x!, y: a.y! - b.y! };
    }

    public static multiply(a: NodePosition, b: number): NodePosition {
        return { x: a.x! * b, y: a.y! * b };
    }

    public static lerpNumber(x: number, y: number, t: number) {
        return x * (1 - t) + y * t;
    }

    public static lerp(a: NodePosition, b: NodePosition, t: number): NodePosition {
        const x = this.lerpNumber(a.x!, b.x!, t);
        const y = this.lerpNumber(a.y!, b.y!, t);

        return { x, y };
    }
}
