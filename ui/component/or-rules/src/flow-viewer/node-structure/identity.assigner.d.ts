import { NodeSocket } from "@openremote/model";
export declare class IdentityAssigner {
    static generateIdentity(): string;
    static getSocketElementIdentity(socket: NodeSocket): any;
}
