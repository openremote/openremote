export class NodeUtilities {
    static getNodeFromID(id, nodes) {
        const node = nodes.find((n) => n.id === id);
        if (!node) {
            console.warn(`Node with ID ${id} not found`);
        }
        return node;
    }
    static getSocketFromID(id, nodes) {
        for (const node of nodes) {
            for (const input of node.inputs) {
                if (input.id === id) {
                    return input;
                }
            }
            for (const output of node.outputs) {
                if (output.id === id) {
                    return output;
                }
            }
        }
    }
    static convertValueTypeToSocketType(value) {
        switch (value.jsonType) {
            case "boolean": return "BOOLEAN" /* NodeDataType.BOOLEAN */;
            case "number":
            case "bigint": return "NUMBER" /* NodeDataType.NUMBER */;
            case "string": return "STRING" /* NodeDataType.STRING */;
            default: return "ANY" /* NodeDataType.ANY */;
        }
    }
    static backtrackFrom(collection, nodeId) {
        if (!collection) {
            throw new Error("Collection has to exist");
        }
        if (!collection.nodes) {
            throw new Error("Collection has to have existing nodes");
        }
        if (!collection.connections) {
            throw new Error("Collection has to have existing connections");
        }
        const node = this.getNodeFromID(nodeId, collection.nodes);
        if (!node) {
            throw new Error("Node has to exist");
        }
        if (!node.inputs) {
            throw new Error("Node has to have existing inputs");
        }
        if (!node.outputs) {
            throw new Error("Node has to have existing outputs");
        }
        let total = [];
        let children = [];
        for (const s of node.inputs) {
            children = children.concat(collection.connections.
                filter((c) => c.to === s.id).
                map((c) => this.getNodeFromID(this.getSocketFromID(c.from, collection.nodes).nodeId, collection.nodes)));
        }
        for (const child of children) {
            total.push(child);
            const result = this.backtrackFrom(collection, child.id);
            total = total.concat(result);
        }
        return total;
    }
    static validate(collection) {
        if (!collection) {
            return false;
        }
        if (!collection.nodes) {
            return false;
        }
        if (!collection.connections) {
            return false;
        }
        const outputNodes = collection.nodes.filter((n) => n.type === "OUTPUT" /* NodeType.OUTPUT */);
        if (outputNodes.length === 0) {
            return false;
        }
        for (const output of outputNodes) {
            const tree = this.backtrackFrom(collection, output.id);
            if (tree.length === 0) {
                return false;
            }
            if (tree.filter((n) => n.type === "INPUT" /* NodeType.INPUT */).length === 0) {
                return false;
            }
        }
        return true;
    }
    static add(a, b) {
        return { x: a.x + b.x, y: a.y + b.y };
    }
    static subtract(a, b) {
        return { x: a.x - b.x, y: a.y - b.y };
    }
    static multiply(a, b) {
        return { x: a.x * b, y: a.y * b };
    }
    static lerpNumber(x, y, t) {
        return x * (1 - t) + y * t;
    }
    static lerp(a, b, t) {
        const x = this.lerpNumber(a.x, b.x, t);
        const y = this.lerpNumber(a.y, b.y, t);
        return { x, y };
    }
}
//# sourceMappingURL=utils.js.map