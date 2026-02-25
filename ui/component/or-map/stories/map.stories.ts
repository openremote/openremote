/*
 * Copyright 2025, OpenRemote Inc.
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
import {type Meta, setCustomElementsManifest, type StoryObj} from "@storybook/web-components";
import {getORStorybookHelpers} from "../../storybook-utils";
import manager from "@openremote/core";
import {html} from "lit";
import customElements from "../custom-elements.json" with {type: "json"};
import packageJson from "../package.json" with {type: "json"};
import "../src/index";
import "../src/markers/or-map-marker";

const tagName = "or-map";
type Story = StoryObj;
setCustomElementsManifest(customElements);

const { events, args, argTypes, template } = getORStorybookHelpers(tagName);

const meta: Meta = {
    title: "Playground/or-map",
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
            description: "Map is a component for displaying geographical maps, with support for custom layers."
        }
    }
};

const parameters = {
    docs: {
        story: {
            height: "360px"
        }
    }
};

const loaders = [
    async storyArgs => ({
        orManager: await loadOrManager()
    })
];

export const Primary: Story = {
    loaders,
    parameters,
    render: (args) => template(args, html`
        <or-map-marker colour="#FF0000" icon="information" lat="51.915767" lng="4.483118" title="Marker"></or-map-marker>
    `)
};

export const WithRadius: Story = {
    loaders,
    name: "Radius example",
    parameters: {
        ...parameters,
        title: "Applying a marker radius",
        summary: "Using the `radius` HTML attribute, you can surround your marker with a circle. For example a radius of 100 meters:",
    },
    render: (args) => template(args, html`
        <or-map-marker colour="#FF0000" icon="information" lat="51.915767" lng="4.483118" radius="100" title="Radius"></or-map-marker>
    `)
};

export const examples = [WithRadius];

export {customElements, packageJson};

/* ------------------------------------------------------- */
/*                   UTILITY FUNCTIONS                     */
/* ------------------------------------------------------- */

async function loadOrManager() {
    if(await manager.init({ managerUrl: "http://localhost:8080", realm: "smartcity" })) {
        if(!manager.authenticated) {
            manager.login();
        }
        return manager;
    }
    throw new Error("Manager could not be initialized");
}

export default meta;
