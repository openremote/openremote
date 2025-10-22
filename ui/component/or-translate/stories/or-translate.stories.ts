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
import {getStorybookHelpers} from "@wc-toolkit/storybook-helpers";
import customElements from "../custom-elements.json" with { type: "json" };
import packageJson from "../package.json" with { type: "json" };
import i18nextBackend from "i18next-http-backend";
import {i18next, OrTranslate} from "../src";
import "../src/index";

const tagName = "or-translate";
type Story = StoryObj;
setCustomElementsManifest(customElements);

const { events, args, argTypes, template } = getStorybookHelpers(tagName);

const meta: Meta = {
    title: "Playground/or-translate",
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
            description: "Useful elements for automatically translating text for in UI apps"
        }
    }
};

export const Primary: Story = {
    args: {
        value: "monday"
    },
    loaders: [
        async storyArgs => ({
            orTranslate: await loadOrTranslate(storyArgs.allArgs)
        })
    ]
};

export {customElements, packageJson};

/* ------------------------------------------------------- */
/*                   UTILITY FUNCTIONS                     */
/* ------------------------------------------------------- */

/**
 * Initialises {@link OrTranslate}, awaits initialization, and returns the HTML object.
 */
async function loadOrTranslate(storyArgs: any) {
    console.debug("Loading OrTranslate...");
    const orTranslate = Object.assign(new OrTranslate(), storyArgs);
    console.debug("Waiting for i18next initialization...");
    if(window.location.href.includes("localhost")) {
        await i18next.use(i18nextBackend).init({lng: "en", backend: {loadPath: () => "http://localhost:8080/shared/locales/{{lng}}/or.json"}});
    } else {
        await i18next.use(i18nextBackend).init({lng: "en", backend: {loadPath: () => "/shared/locales/{{lng}}/or.json"}});
    }
    console.debug("OrTranslate loaded");
    return orTranslate;
}

export default meta;
