import { Node, NodeCollection, NodeDataType, NodePosition, ValueDescriptor } from "@openremote/model";
export declare class NodeUtilities {
    static getNodeFromID(id: string, nodes: Node[]): Node | undefined;
    static getSocketFromID(id: string, nodes: Node[]): import("@openremote/model").NodeSocket | undefined;
    static convertValueTypeToSocketType(value: ValueDescriptor): NodeDataType;
    static backtrackFrom(collection: NodeCollection, nodeId: string): Node[];
    static validate(collection: NodeCollection): boolean;
    static add(a: NodePosition, b: NodePosition): NodePosition;
    static subtract(a: NodePosition, b: NodePosition): NodePosition;
    static multiply(a: NodePosition, b: number): NodePosition;
    static lerpNumber(x: number, y: number, t: number): number;
    static lerp(a: NodePosition, b: NodePosition, t: number): NodePosition;
}
