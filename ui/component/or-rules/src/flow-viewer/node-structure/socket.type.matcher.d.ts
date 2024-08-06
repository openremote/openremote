import { NodeDataType } from "@openremote/model";
export declare class SocketTypeMatcher {
    static match(a: NodeDataType, b: NodeDataType): boolean;
    private static readonly matches;
}
