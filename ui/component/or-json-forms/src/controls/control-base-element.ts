/*
 * Copyright 2026, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
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
