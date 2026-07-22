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
import "../src/or-vaadin-text-area";

const tagName = "or-vaadin-text-area";
type Story = StoryObj;
setCustomElementsManifest(customElements);

const { events, args, argTypes, template } = getORStorybookHelpers(tagName);

const meta: Meta = {
    title: "Playground/or-vaadin-components/text-area",
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
            description: "Text Area is a multi-line text input. By default it automatically resizes with its content, bounded by `min-rows`/`max-rows`; set `manualresize` for a fixed height (starting at `min-rows`) with a native resize handle."
        }
    }
};

// Default: automatically resizes with the content.
export const Primary: Story = {
    args: {
        label: "Notes",
        value: "OpenRemote"
    },
    parameters: {
        docs: {
            story: {
                height: "120px"
            }
        }
    }
};

// Fixed height with a native vertical resize handle; overflow scrolls until dragged.
export const ManualResize: Story = {
    parameters: {
        title: "Manual resizing",
        summary: "With `manualresize` the field starts at `min-rows`, keeps a fixed height, and shows the native resize handle. Overflowing content scrolls until the handle is dragged.",
        docs: {
            story: {
                height: "180px"
            }
        }
    },
    args: {
        label: "Notes",
        minRows: 5,
        manualresize: true,
        value: "one\ntwo\nthree\nfour\nfive\nsix\nseven\neight"
    }
};

// Automatic resizing constrained to a number of rows.
export const BoundedAutoresize: Story = {
    parameters: {
        title: "Bounded automatic resizing",
        summary: "`min-rows` and `max-rows` bound the automatic resizing: the field starts at `min-rows` and grows with the content until `max-rows`, after which it scrolls.",
        docs: {
            story: {
                height: "140px"
            }
        }
    },
    args: {
        label: "Notes",
        minRows: 2,
        maxRows: 6
    }
};

export const examples: Story[] = [ManualResize, BoundedAutoresize];

export {customElements, packageJson};

export default meta;
