import { LitElement, html, customElement, property } from "lit-element";
import { repeat } from "lit-html/directives/repeat";
import { EditorWorkspace } from "./editor-workspace";
import { project } from "./flow-editor";

@customElement("connection-container")
export class ConnectionContainer extends LitElement {
    @property({ attribute: false }) private workspace!: EditorWorkspace;

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

    protected render() {
        return html`${repeat(project.connections, (c) => c.from! + c.to!, (c) => html`<connection-line .workspace="${this.workspace}" .connection="${c}"></connection-line>`)}`;
    }
}
