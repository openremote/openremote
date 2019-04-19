import {LitElement, property} from "lit-element";

export abstract class PageElement extends LitElement {
    @property({type: Boolean})
    active = false;

    // Only render this page if it's actually visible.
    protected shouldUpdate() {
        return this.active;
    }
}
