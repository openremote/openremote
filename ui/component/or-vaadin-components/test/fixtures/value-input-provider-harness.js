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
// Plain JS on purpose: the component-test bundle compiles .ts files against ui/test's tsconfig
// (rootDir ui/test), so TS component sources outside that dir fail the build.
import { html, LitElement } from "lit";
import { getValueHolderInputTemplateProvider } from "@openremote/or-vaadin-components/value-input-provider";

/**
 * Renders the input for an attribute of `valueType` the way or-attribute-input does: the provider is built once
 * per descriptor and its template is re-rendered with the current value. Every value the input reports is
 * re-dispatched as a `value-change` event carrying it as the detail.
 */
export class ValueInputProviderHarness extends LitElement {
  static properties = {
    valueType: { type: String },
    jsonType: { type: String },
    value: {},
    constraints: { type: Array },
    format: { type: Object },
  };

  willUpdate(changed) {
    if (changed.has("valueType") || changed.has("constraints") || changed.has("format")) {
      const meta = {};
      if (this.constraints) meta.constraints = this.constraints;
      if (this.format) meta.format = this.format;
      this._provider = getValueHolderInputTemplateProvider(
        "TestAsset",
        { name: "attribute", type: this.valueType, meta },
        undefined,
        { name: this.valueType, jsonType: this.jsonType ?? "number" },
        (value) => this.dispatchEvent(new CustomEvent("value-change", { detail: value })),
        { label: "Attribute" }
      );
    }
  }

  render() {
    return this._provider?.templateFunction?.(this.value, false, false, false, false, undefined) ?? html``;
  }
}
customElements.define("test-value-input-provider-harness", ValueInputProviderHarness);
