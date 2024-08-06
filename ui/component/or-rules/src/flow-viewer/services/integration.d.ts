/// <reference types="node" />
import { Node } from "@openremote/model";
import { EventEmitter } from "events";
export declare class Integration extends EventEmitter {
    nodes: Node[];
    refreshNodes(): Promise<void>;
}
