var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import rest from "@openremote/rest";
import { EventEmitter } from "events";
export class Integration extends EventEmitter {
    constructor() {
        super(...arguments);
        this.nodes = [];
    }
    refreshNodes() {
        return __awaiter(this, void 0, void 0, function* () {
            this.nodes = [];
            const allNodes = (yield rest.api.FlowResource.getAllNodeDefinitions()).data;
            for (const n of allNodes) {
                this.nodes.push(n);
            }
        });
    }
}
//# sourceMappingURL=integration.js.map