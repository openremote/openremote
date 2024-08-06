var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { LitElement, html } from "lit";
import { customElement, property } from "lit/decorators.js";
import { repeat } from "lit/directives/repeat.js";
import { project } from "./flow-editor";
let ConnectionContainer = class ConnectionContainer extends LitElement {
    constructor() {
        super();
        project.addListener("connectioncreated", () => {
            this.requestUpdate();
        });
        project.addListener("connectionremoved", () => {
            this.requestUpdate();
        });
        project.addListener("cleared", () => {
            this.requestUpdate();
        });
    }
    render() {
        return html `${repeat(project.connections, (c) => c.from + c.to, (c) => html `<connection-line .workspace="${this.workspace}" .connection="${c}"></connection-line>`)}`;
    }
};
__decorate([
    property({ attribute: false })
], ConnectionContainer.prototype, "workspace", void 0);
ConnectionContainer = __decorate([
    customElement("connection-container")
], ConnectionContainer);
export { ConnectionContainer };
//# sourceMappingURL=connection-container.js.map