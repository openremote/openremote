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
import customElements from "../../custom-elements.json" with { type: "json" };
import packageJson from "../../package.json" with { type: "json" };
import {InputType} from "../../src/or-mwc-input";
import "../../src/or-mwc-input";

const tagName = "or-mwc-input";
type Story = StoryObj;
setCustomElementsManifest(customElements);

const { events, args, argTypes, template } = getStorybookHelpers(tagName);

const meta: Meta = {
    title: "Playground/or-mwc-input/input",
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
            subtitle: `<${tagName} type="text | number | password">`,
            description: "Inputs collect data from the user."
        }
    }
};

export const Primary: Story = {
    args: {
        type: InputType.TEXT,
        label: "Enter text"
    }
};

export const NumberExample: Story = {
    name: "Number Example",
    parameters: {
        title: "Only allow number input",
        summary: "Require users to select a number, instead of writing a piece of text."
    },
    args: {
        type: InputType.NUMBER,
        label: "Pick a number"
    }
};

export const PasswordExample: Story = {
    name: "Password Example",
    parameters: {
        title: "Using password visibility",
        summary: "If the input needs to be secret, you can use `type='password'` to make the text masked."
    },
    args: {
        type: InputType.PASSWORD,
        label: "Write down your secret",
        value: "password"
    }
};

export const examples: Story[] = [NumberExample, PasswordExample];

export {customElements, packageJson};

export default meta;
