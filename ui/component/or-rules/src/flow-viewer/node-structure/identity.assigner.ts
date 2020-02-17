import ShortID from "shortid";
import { NodeSocket } from "@openremote/model";

export class IdentityAssigner {

    public static generateIdentity(): string {
        return ShortID.generate();
    }

    public static getSocketElementIdentity(socket: NodeSocket) {
        return socket.id;
    }
}
