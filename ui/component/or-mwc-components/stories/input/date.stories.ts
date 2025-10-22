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
    title: "Playground/or-mwc-input/date",
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
            subtitle: `<${tagName} type="date | time | datetime">`,
            description: "Date pickers allow the user to select a date."
        }
    }
};

export const Primary: Story = {
    args: {
        type: InputType.DATE,
        label: "Date picker"
    }
};

export const TimeExample: Story = {
    name: "Time Example",
    parameters: {
        title: "Select a time",
        summary: "Instead of selecting a date, you could require the user to select a time."
    },
    args: {
        type: InputType.TIME,
        label: "Time picker"
    }
};

export const DateTimeExample: Story = {
    name: "Date time Example",
    parameters: {
        title: "Select a date and time",
        summary: "If you require users to set both a date and time, you can combine them in a 'date time picker'."
    },
    args: {
        type: InputType.DATETIME,
        label: "Date time picker"
    }
};

export const examples: Story[] = [TimeExample, DateTimeExample];

export {customElements, packageJson};

export default meta;
