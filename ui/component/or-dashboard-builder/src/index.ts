import {html, LitElement } from "lit";
import { customElement } from "lit/decorators.js";

@customElement("or-dashboard-builder")
export class OrDashboardBuilder extends LitElement {

    render(): any {
        return html`
            <div>
                <span>Content of the or-dashboard-builder component</span>
            </div>
        `
    }
}
