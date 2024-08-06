import type { Meta, StoryObj } from '@storybook/web-components';
import { getWcStorybookHelpers } from "wc-storybook-helpers";
import {OrGauge} from "@openremote/or-gauge";
import "@openremote/or-gauge";

const { events, args, argTypes, template } = getWcStorybookHelpers("or-gauge");

const meta: Meta<OrGauge> = {
    title: 'Components/or-gauge',
    component: 'or-gauge',
    args,
    argTypes,
    parameters: {
        actions: {
            handles: events,
        },
    }
};

type Story = StoryObj<OrGauge & typeof args>;

export const Primary: Story = {
    args: {
        min: 0,
        max: 10,
        value: 6,
        thresholds: JSON.stringify([
            [0, "#4caf50"],
            [5, "#ff9800"],
            [8, "#ef5350"],
        ]) as any
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
