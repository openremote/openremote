import { getWcStorybookHelpers } from "wc-storybook-helpers";
import "@openremote/or-gauge";

const { events, args, argTypes, template } = getWcStorybookHelpers("or-gauge");

/** @type { import('@storybook/web-components').Meta } */
const meta = {
    title: 'Playground/or-gauge',
    component: 'or-gauge',
    args,
    argTypes,
    parameters: {
        actions: {
            handles: events,
        },
    }
};

/** @type { import('@storybook/web-components').StoryObj } */
export const Primary = {
    args: {
        min: 0,
        max: 10,
        value: 6,
        thresholds: JSON.stringify([
            [0, "#4caf50"],
            [5, "#ff9800"],
            [8, "#ef5350"],
        ])
    },
    parameters: {
        docs: {
            source: {
                code: getExampleCode()
            },
            story: {
                height: '200px'
            }
        }
    },
    render: (args) => template(args),
};

/* ------------------------------------------------------- */
/*                   UTILITY FUNCTIONS                     */
/* ------------------------------------------------------- */

function getExampleCode() {

    //language=javascript
    return `
import "@openremote/or-gauge";

// (OPTIONAL) set the thresholds;
const thresholds = [[0, "#4caf50"], [5, "#ff9800"], [8, "#ef5350"]];
// const thresholdsStr = JSON.stringify(thresholds);

// in your HTML code use this, and inject them;
<or-gauge min="0" value="6" max="10" thresholds="thresholdsStr"></or-gauge>
`
}

export default meta;
