import {getWcStorybookHelpers} from "wc-storybook-helpers";
import {InputType, OrMwcInput} from "@openremote/or-mwc-components/or-mwc-input";
import {html} from 'lit';

const {events, args, argTypes, template} = getWcStorybookHelpers("or-mwc-input");

/** @type { import('@storybook/web-components').Meta } */
const meta = {
  title: "Playground/or-mwc-input/general",
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
    type: InputType.NUMBER,
    value: 100
  }
};

/** @type { import('@storybook/web-components').StoryObj } */
export const Select = {
  render: (_args) => {
    const options = [
      ["one", "One"],
      ["two", "Two"],
      ["three", "Three"],
      ["four", "Four"],
      ["five", "Five"],
      ["six", "Six"],
      ["seven", "Seven"],
      ["eight", "Eight"],
      ["nine", "Nine"],
      ["ten", "Ten"],
      ["eleven", "Eleven"],
      ["twelve", "Twelve"],
      ["thirteen", "Thirteen"],
      ["fourteen", "Fourteen"],
      ["fifteen", "Fifteen"],
      ["sixteen", "Sixteen"],
      ["seventeen", "Seventeen"],
      ["eighteen", "Eighteen"],
      ["nineteen", "Nineteen"],
      ["twenty", "Twenty"],
      ["twenty-one", "Twenty One"],
      ["twenty-two", "Twenty Two"],
      ["twenty-three", "Twenty Three"],
      ["twenty-four", "Twenty Four"],
      ["twenty-five", "Twenty Five"],
      ["twenty-six", "Twenty Six"],
      ["twenty-seven", "Twenty Seven"],
      ["twenty-eight", "Twenty Eight"],
      ["twenty-nine", "Twenty Nine"],
      ["thirty", "Thirty"]
    ];
    const searchProviderAsync = async (search) => {
      console.log(search);
      return options;
    }
    const searchProvider = (search) => {
      console.log(search);
      return options;
    }
    return html`
      <div style="display: flex; gap: 12px;">
        <or-mwc-input .type="${InputType.SELECT}" .value="${options[0][0]}" .options="${options}"
                      label="Search for an async value"
                      outlined rounded .searchProvider=${searchProviderAsync} style="width: 300px;"
        ></or-mwc-input>
        <or-mwc-input .type="${InputType.SELECT}" .value="${options[0][0]}" .options="${options}"
                      label="Search for a value"
                      outlined rounded .searchProvider=${searchProvider} style="width: 300px;"
        ></or-mwc-input>
        <or-mwc-input .type="${InputType.SELECT}" .value="${options[0][0]}" .options="${options}" label="Select a value"
                      outlined rounded style="width: 300px;"
        ></or-mwc-input>
      </div>
    `;
  }
};

/** @type { import('@storybook/web-components').StoryObj } */
export const Example = {
  render: (args) => html`
    <div style="display: flex; gap: 16px; align-items: center;">
      ${template(args[0])}
      ${template(args[1])}
    </div>
  `,
  args: [{
    type: InputType.NUMBER,
    value: 100,
    outlined: true,
    comfortable: true,
    compact: true
  }, {
    type: InputType.BUTTON,
    label: "submit",
    outlined: true
  }]
};

export default meta;
