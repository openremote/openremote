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
    title: "Playground/or-mwc-input/button",
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
            subtitle: `<${tagName} type="button">`,
            description: "Buttons represent actions that are available to the user."
        }
    }
};

export const Primary: Story = {
    args: {
        type: InputType.BUTTON,
        label: "Button"
    }
};

export const OutlinedExample: Story = {
    name: "Tree Example",
    parameters: {
        title: "Outlined button",
        summary: "You can outline the button using the `outlined` HTML attribute."
    },
    args: {
        type: InputType.BUTTON,
        outlined: true,
        label: "Button"
    }
};

export const examples: Story[] = [OutlinedExample];

export {customElements, packageJson};

export default meta;
