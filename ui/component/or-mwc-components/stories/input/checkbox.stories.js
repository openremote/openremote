import { getWcStorybookHelpers } from "wc-storybook-helpers";
import {InputType, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import { html } from 'lit';

const { events, args, argTypes, template } = getWcStorybookHelpers("or-mwc-input");

/** @type { import('@storybook/web-components').Meta } */
const meta = {
  title: "Playground/or-mwc-input/checkbox",
  component: "or-mwc-input",
  args: args,
  argTypes: {
    ...argTypes,
    type: {
      options: Object.values(InputType)
    }
  },
  parameters: {
    actions: {
      handles: [...events, "or-mwc-input-changed"]
    },
    docs: {
      subtitle: "<or-mwc-input>",
    }
  }
};

/** @type { import('@storybook/web-components').StoryObj } */
export const Primary = {
  render: (args) => template(args),
  args: {
    type: InputType.CHECKBOX,
    label: "Checkbox"
  }
};

export default meta;
