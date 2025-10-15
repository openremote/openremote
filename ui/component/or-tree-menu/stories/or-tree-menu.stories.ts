import {setCustomElementsManifest, type Meta, type StoryObj } from "@storybook/web-components";
import customElements from "../custom-elements.json" with { type: "json" };
import packageJson from "../package.json" with { type: "json" };
import "../src/index";

type Story = StoryObj;
setCustomElementsManifest(customElements);

const meta: Meta = {
    title: "Playground/or-tree-menu",
    component: "or-tree-menu",
    parameters: {
        docs: {
            subtitle: "<or-tree-menu>"
        }
    }
};

export const Primary: Story = {
    args: {
        menuTitle: "My custom title"
    }
};

export const TreeExample: Story = {
    name: "Building a Tree structure",
    parameters: {
        summary: "You can define the tree using `<or-tree-group>` and `<or-tree-node>` elements."
    },
    args: {
        menuTitle: "My Assets"
    }
};

export const examples: Story[] = [TreeExample];

export {customElements, packageJson};

export default meta;
