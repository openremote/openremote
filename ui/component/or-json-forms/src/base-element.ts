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
import {LitElement} from "lit";
import {property} from "lit/decorators.js";
import {
    JsonFormsCellRendererRegistryEntry,
    JsonFormsRendererRegistryEntry,
    JsonFormsUISchemaRegistryEntry,
    JsonSchema,
    OwnPropsOfRenderer,
    UISchemaElement
} from '@jsonforms/core';
import {JsonFormsStateContext} from "./index";

/**
 * Adds additional props for layouts which are normally only available for controls
 */
export interface AdditionalProps {
    label?: string;
    required?: boolean;
    errors?: string;
    minimal?: boolean;
    type?: string;
}

export abstract class BaseElement<T extends UISchemaElement, P extends OwnPropsOfRenderer> extends LitElement implements OwnPropsOfRenderer, AdditionalProps {

    @property({type: Object})
    public state!: JsonFormsStateContext;

    @property({type: Object})
    public uischema!: T;

    @property({type: Object})
    public schema!: JsonSchema;

    @property({type: String, attribute: false})
    public data: any;

    @property({type: Array})
    public renderers?: JsonFormsRendererRegistryEntry[];

    @property({type: Array})
    public cells?: JsonFormsCellRendererRegistryEntry[];

    @property({type: String, attribute: false})
    public config: any;

    @property({type: Array})
    public uischemas?: JsonFormsUISchemaRegistryEntry[];

    @property({type: Boolean})
    public enabled!: boolean;

    @property({type: Boolean})
    public visible!: boolean;

    @property({type: String})
    public path!: string;

    @property({type: String})
    public label!: string;

    @property({type: Boolean})
    public required!: boolean;

    @property()
    public errors!: string;

    public set props(props: P) {
        delete (props as any).id;
        Object.assign(this, props);
    }
}
