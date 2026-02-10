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
import {setCustomElementsManifest, type Meta, type StoryObj } from "@storybook/web-components";
import {getORStorybookHelpers} from "../../storybook-utils";
import customElements from "../custom-elements.json" with { type: "json" };
import packageJson from "../package.json" with { type: "json" };
import {html} from "lit";
import {dialogFooterRenderer, dialogHeaderRenderer, dialogRenderer, OrVaadinDialog} from "../src/or-vaadin-dialog";
import manager from "@openremote/core";
import "../src/or-vaadin-button";
import "../src/or-vaadin-dialog";
import "../src/or-vaadin-numberfield";
import "@openremote/or-map";

const tagName = "or-vaadin-dialog";
type Story = StoryObj;
setCustomElementsManifest(customElements);

const { events, args, argTypes, template } = getORStorybookHelpers(tagName);

const meta: Meta = {
    title: "Playground/or-vaadin-components/dialog",
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
            description: "Dialog is a small window that can be used to present information and user interface elements in an overlay."
        }
    }
};

export const Primary: Story = {
    render: (_args) => {
        return html`
            <or-vaadin-dialog 
                    ${dialogHeaderRenderer(() => html`<h2>Dialog title</h2>`)}
                    ${dialogRenderer(() => html`<p>Content</p>`)}
            ></or-vaadin-dialog>
            <or-vaadin-button @click="${() => (document.querySelector('or-vaadin-dialog') as OrVaadinDialog).open()}">Show dialog</or-vaadin-button>
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

export const MapExample: Story = {
    render: (_args) => {
        const footer = () => html`
            <or-vaadin-button theme="tertiary">Cancel</or-vaadin-button>
            <or-vaadin-button theme="primary">Save</or-vaadin-button>
        `;
        return html`
            <or-vaadin-dialog header-title="Configure area" ${dialogFooterRenderer(footer)}>
                <div>
                    <or-map style="aspect-ratio: 1/1;"></or-map>
                    <or-vaadin-numberfield label="Radius (min. 100m)" min="100" value="100"></or-vaadin-numberfield>
                </div>
            </or-vaadin-dialog>
            <or-vaadin-button @click="${() => (document.querySelector('or-vaadin-dialog') as OrVaadinDialog).open()}">Show dialog</or-vaadin-button>
        `;
    },
    parameters: {
        docs: {
            story: {
                height: "640px"
            }
        }
    },
    loaders: [
        async storyArgs => ({
            orManager: await loadOrManager()
        })
    ]
};

export const examples: Story[] = [MapExample];

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
