import {setCustomElementsManifest, type Meta, type StoryObj } from "@storybook/web-components";
import { getStorybookHelpers, setStorybookHelpersConfig } from "@wc-toolkit/storybook-helpers";
import customElements from "../../custom-elements.json" with { type: "json" };
import packageJson from "../../package.json" with { type: "json" };
import {InputType} from "../../src/or-mwc-input";
import "../../src/or-mwc-input";

const tagName = "or-mwc-input";
type Story = StoryObj;
setCustomElementsManifest(customElements);
setStorybookHelpersConfig({});

const { events, args, argTypes, template } = getStorybookHelpers(tagName);

const meta: Meta = {
    title: "Playground/or-mwc-input/select",
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
            subtitle: `<${tagName} type="select">`,
            description: "Selects allow you to choose items from a menu of predefined options.",
            story: {
                height: "240px"
            }
        }
    }
};

export const Primary: Story = {
    args: {
        type: InputType.SELECT,
        label: "Select amount",
        placeHolder: "Select amount",
        options: JSON.stringify(["One", "Two", "Three"]),
        fullWidth: true
    }
};

export {customElements, packageJson};

export default meta;
