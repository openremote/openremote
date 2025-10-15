import {setCustomElementsManifest, type Meta, type StoryObj } from "@storybook/web-components";
import { getStorybookHelpers, setStorybookHelpersConfig } from "@wc-toolkit/storybook-helpers";
import customElements from "../custom-elements.json" with { type: "json" };
import packageJson from "../package.json" with { type: "json" };
import "../src/index";

const tagName = "or-tree-menu";
type Story = StoryObj;
setCustomElementsManifest(customElements);
setStorybookHelpersConfig({});

const { events, args, argTypes, template } = getStorybookHelpers(tagName);

const meta: Meta = {
    title: "Playground/or-tree-menu",
    component: tagName,
    args: args,
    argTypes: argTypes,
    render: (args) => template(args),
    parameters: {
        actions: {
            handles: events
        },
        docs: {
            subtitle: `<${tagName}>`
        }
    }
};

export const Primary: Story = {
    args: {
        menuTitle: "My custom title",
        nodes: JSON.stringify([])
    }
};

export const TreeExample: Story = {
    name: "Building a Tree structure",
    parameters: {
        summary: "You can define the tree using `<or-tree-group>` and `<or-tree-node>` elements."
    },
    args: {
        "menu-title": "My Assets",
        "nodes": JSON.stringify([])
    }
};

export const examples: Story[] = [TreeExample];

export {customElements, packageJson};

export default meta;
