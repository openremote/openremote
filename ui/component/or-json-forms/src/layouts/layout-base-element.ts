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
import {Layout, LayoutProps, mapStateToJsonFormsRendererProps, OwnPropsOfLayout, OwnPropsOfRenderer,
    Resolve, UISchemaElement} from "@jsonforms/core";
import {property} from "lit/decorators.js";
import {BaseElement} from "../base-element";

export abstract class LayoutBaseElement<T extends Layout> extends BaseElement<T, LayoutProps> implements OwnPropsOfLayout {

    @property({type: String})
    public direction: "row" | "column" = "column";

    protected getChildProps(): OwnPropsOfRenderer[]  {
        return (this.uischema && this.uischema.elements ? this.uischema.elements : []).map(
            (el: UISchemaElement) => {
                const props: OwnPropsOfRenderer = {
                    renderers: this.renderers,
                    uischema: el,
                    schema: this.schema,
                    path: this.path
                }
                return props;
            }
        );
    }
}
