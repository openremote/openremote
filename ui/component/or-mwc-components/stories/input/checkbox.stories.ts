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
    title: "Playground/or-mwc-input/checkbox",
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
            subtitle: `<${tagName} type="checkbox">`,
            description: "Checkboxes allow the user to toggle an option on or off."
        }
    }
};

export const Primary: Story = {
    args: {
        type: InputType.CHECKBOX,
        label: "Checkbox"
    }
};

export {customElements, packageJson};

export default meta;
