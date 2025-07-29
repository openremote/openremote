import type { Meta, StoryObj } from "@storybook/web-components";
import "@openremote/or-tree-menu";

type Story = StoryObj;

const meta: Meta = {
    component: "or-tree-menu"
}

export const Primary: Story = {
    args: {
        menuTitle: "My custom title"
    }
}

export default meta;
