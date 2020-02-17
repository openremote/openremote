import rest from "@openremote/rest";
import { Node } from "@openremote/model";
import { EventEmitter } from "events";

export class Integration extends EventEmitter {
    public nodes: Node[] = [];
    
    public async refreshNodes() {
        this.nodes = [];
        const allNodes = (await rest.api.FlowResource.getAllNodeDefinitions()).data;
        for (const n of allNodes) {
            this.nodes.push(n);
        }
    }
}
