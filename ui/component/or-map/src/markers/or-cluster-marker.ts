import { css, CSSResultGroup, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";

@customElement("or-cluster-marker")
export class OrClusterMarker extends LitElement {
    static get styles(): CSSResultGroup {
        return css``;
    }

    @property({ type: Number, reflect: true, attribute: true })
    public slices?: [number];

    constructor() {
        super();
    }

    protected render() {
        return html``;
    }
}
