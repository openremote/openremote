import {html, LitElement } from "lit";
import {customElement} from "lit/decorators.js";

@customElement("or-json-forms-unknown")
export class UnknownElement extends LitElement {

    public render() {
        return html`<span>No applicable renderer found!</span>`;
    }
}
