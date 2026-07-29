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
import {setCustomElementsManifest, type Meta, type StoryObj } from "@storybook/web-components";
import {getORStorybookHelpers} from "../../storybook-utils";
import customElements from "../custom-elements.json" with { type: "json" };
import packageJson from "../package.json" with { type: "json" };
import {html} from "lit";
import {OrVaadinConfirmDialog} from "../src/or-vaadin-confirm-dialog";
import "../src/or-vaadin-button";
import "../src/or-vaadin-confirm-dialog";

const tagName = "or-vaadin-confirm-dialog";
type Story = StoryObj;
setCustomElementsManifest(customElements);

const { events, args, argTypes, template } = getORStorybookHelpers(tagName);

const meta: Meta = {
    title: "Playground/or-vaadin-components/confirm-dialog",
    component: tagName,
    args: args,
    argTypes: argTypes,
    render: storyArgs => template(storyArgs),
    excludeStories: /^[a-z].*/,
    parameters: {
        actions: {
            handles: events
        },
        docs: {
            subtitle: `<${tagName}>`,
            description: "Confirm Dialog is a modal Dialog used to confirm user actions. Compared to other components, users can not dismiss this dialog."
        }
    }
};

export const Primary: Story = {
    render: (_args) => {
        return html`
            <or-vaadin-confirm-dialog cancel-button-visible>
                <span slot="header">Header</span>
                <span>Content</span>
                <vaadin-button slot="confirm-button" theme="primary">Confirm</vaadin-button>
            </or-vaadin-confirm-dialog>
            <or-vaadin-button @click="${() => (document.querySelector('or-vaadin-confirm-dialog') as OrVaadinConfirmDialog).open()}">Show dialog</or-vaadin-button>
        `;
    },
    parameters: {
        docs: {
            story: {
                height: "360px"
            }
        }
    }
};

export const examples: Story[] = [];

export {customElements, packageJson};


export default meta;
