import ShortID from "shortid";
export class IdentityAssigner {
    static generateIdentity() {
        return ShortID.generate();
    }
    static getSocketElementIdentity(socket) {
        return socket.id;
    }
}
//# sourceMappingURL=identity.assigner.js.map