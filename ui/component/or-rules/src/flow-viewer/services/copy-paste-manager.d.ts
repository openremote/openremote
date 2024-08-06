import { NodePosition } from "@openremote/model";
import { LightNodeCollection } from "../models/light-node-collection";
export declare class CopyPasteManager {
    private clipboard;
    private copyOrigin;
    get isFull(): boolean;
    putInClipboard(obj: LightNodeCollection, origin: NodePosition): void;
    getFromClipboard(newOrigin: NodePosition): LightNodeCollection;
    cloneIsolated(obj: LightNodeCollection, positionOffset?: number): LightNodeCollection;
    copy(x: number, y: number): void;
}
