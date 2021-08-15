import {
    ControlElement,
    ControlProps,
    createId,
    isControl,
    JsonSchema,
    mapDispatchToControlProps,
    OwnPropsOfControl,
    removeId
} from "@jsonforms/core";
import {PropertyValues} from "lit";
import {property} from "lit/decorators.js";
import {BaseElement} from "../base-element";

export abstract class ControlBaseElement extends BaseElement<ControlElement, ControlProps> implements OwnPropsOfControl, ControlProps {

    @property()
    public description?: string | undefined;

    @property()
    public rootSchema!: JsonSchema;

    public handleChange!: (path: string, data: any) => void;

    constructor() {
        super();
    }

    updated(_changedProperties: PropertyValues) {
        super.updated(_changedProperties);

        if (_changedProperties.has("state")) {
            const { handleChange } = mapDispatchToControlProps(this.state.dispatch);
            this.handleChange = handleChange;
        }
    }

    public shouldUpdate(changedProperties: PropertyValues): boolean {

        if (changedProperties.has("uischema")) {
            if (isControl(this.uischema)) {
                const oldSchemaValue = (changedProperties.get("uischema") as ControlElement);
                if (oldSchemaValue?.scope !== this.uischema?.scope) {
                    if (this.id) {
                        removeId(this.id);
                    }
                    this.id = createId(this.uischema.scope);
                }
            }
        }

        return true;
    }

    public disconnectedCallback() {
        if (isControl(this.uischema)) {
            removeId(this.id);
        }
    }
}
