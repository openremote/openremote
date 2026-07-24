/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
// Plain JS on purpose: the component-test bundle compiles .ts files against ui/test's tsconfig
// (rootDir ui/test), so TS component sources outside that dir fail the build. jsonFormsInputTemplateProvider
// pulls in browser-only modules, so it can only run in the browser via a mounted fixture, not the Node test body.
import { html, LitElement } from "lit";
import { jsonFormsInputTemplateProvider } from "@openremote/or-attribute-input";

/**
 * Reports which template jsonFormsInputTemplateProvider picks for a value descriptor:
 * "json-forms" when it builds its own editor, "fallback" when it defers to the fallback provider.
 */
export class ProviderProbe extends LitElement {
    static properties = { descriptor: { type: Object } };

    render() {
        if (!this.descriptor) {
            return html``;
        }

        // A sentinel fallback: an identity check against it tells the two branches apart
        // without rendering either template (so no schema request is made).
        const fallback = {
            templateFunction: () => html``,
            supportsHelperText: true,
            supportsLabel: true,
            supportsSendButton: true,
        };
        const provider = jsonFormsInputTemplateProvider(fallback)(
            "TestAsset", undefined, undefined, this.descriptor, () => {}, {}
        );
        const result = provider === fallback ? "fallback" : "json-forms";
        return html`<div role="status">${result}</div>`;
    }
}
customElements.define("test-provider-probe", ProviderProbe);
