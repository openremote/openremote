import { Node, NodeConnection } from "@openremote/model";

export interface LightNodeCollection {
    nodes: Node[];
    connections: NodeConnection[];
}
