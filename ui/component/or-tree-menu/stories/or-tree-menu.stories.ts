import {setCustomElementsManifest, type Meta, type StoryObj } from "@storybook/web-components";
import customElements from "../custom-elements.json" with { type: "json" };
import "../src/index";

type Story = StoryObj;
setCustomElementsManifest(customElements);

const meta: Meta = {
    component: "or-tree-menu"
};

export const Primary: Story = {
    args: {
        menuTitle: "My custom title"
    }
};

export default meta;
