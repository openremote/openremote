import { IdentityAssigner } from "./identity.assigner";
export class CopyMachine {
    static copy(node) {
        const minimalNode = {};
        minimalNode.inputs = (node.inputs || []).map((i) => {
            return {
                name: i.name, type: i.type
            };
        });
        minimalNode.internals = node.internals || [];
        minimalNode.name = node.name;
        minimalNode.displayCharacter = node.displayCharacter;
        minimalNode.outputs = (node.outputs || []).map((i) => {
            return {
                name: i.name, type: i.type
            };
        });
        minimalNode.type = node.type;
        minimalNode.position = { x: 0, y: 0 };
        minimalNode.size = { x: 0, y: 0 };
        const clone = JSON.parse(JSON.stringify(minimalNode));
        clone.id = IdentityAssigner.generateIdentity();
        clone.inputs.forEach((socket) => {
            socket.nodeId = clone.id;
            socket.id = IdentityAssigner.generateIdentity();
        });
        clone.outputs.forEach((socket) => {
            socket.nodeId = clone.id;
            socket.id = IdentityAssigner.generateIdentity();
        });
        return clone;
    }
}
//# sourceMappingURL=copy.machine.js.map