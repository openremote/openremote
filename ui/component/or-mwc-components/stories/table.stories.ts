import {setCustomElementsManifest, type Meta, type StoryObj } from "@storybook/web-components";
import { getStorybookHelpers, setStorybookHelpersConfig } from "@wc-toolkit/storybook-helpers";
import customElements from "../custom-elements.json" with { type: "json" };
import packageJson from "../package.json" with { type: "json" };
import "../src/or-mwc-table";

const tagName = "or-mwc-table";
type Story = StoryObj;
setCustomElementsManifest(customElements);
setStorybookHelpersConfig({});

const { events, args, argTypes, template } = getStorybookHelpers(tagName);

const meta: Meta = {
    title: "Playground/or-mwc-table",
    component: tagName,
    args: args,
    argTypes: argTypes,
    render: (args) => template(args),
    excludeStories: /^[a-z].*/,
    parameters: {
        actions: {
            handles: events
        },
        docs: {
            subtitle: `<${tagName}>`,
            description: "Used for organizing data rows and columns."
        }
    }
};

export const Primary: Story = {
    args: {
        columns: JSON.stringify([{title: "Column 1"}, {title: "Column 2"}, {title: "Column 3"}]),
        rows: JSON.stringify([
            {content: ["Row 1, Column 1", "Row 1, Column 2", "Row 1, Column 3"]},
            {content: ["Row 2, Column 1", "Row 2, Column 2", "Row 2, Column 3"]}
        ])
    },
    parameters: {
        docs: {
            story: {
                height: "240px"
            }
        }
    }
};

export {customElements, packageJson};

export default meta;
