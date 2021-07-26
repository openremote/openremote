/**
 * This adds haunted (react style hooks) to lit element
 */
import { LitElement, PropertyValues } from "lit";
import { State } from "haunted";

const defer = Promise.resolve().then.bind(Promise.resolve());

export class HauntedLitElement extends LitElement {

    protected haunted: State;

    constructor() {
        super();
        this.haunted = new State(() => this.requestUpdate(), this);
    }

    update(_changedProperties: PropertyValues) {
        this.haunted.run(() => {
            super.update(_changedProperties);
        });
    }

    updated(_changedProperties: PropertyValues) {
        super.updated(_changedProperties);
        this.haunted.runLayoutEffects();
        defer(() => this.haunted.runEffects());
    }

    disconnectedCallback() {
        this.haunted.teardown();
        super.disconnectedCallback();
    }
}