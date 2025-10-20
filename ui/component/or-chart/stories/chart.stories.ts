import {setCustomElementsManifest, type Meta, type StoryObj } from "@storybook/web-components";
import { getStorybookHelpers, setStorybookHelpersConfig } from "@wc-toolkit/storybook-helpers";
import customElements from "../custom-elements.json" with { type: "json" };
import packageJson from "../package.json" with { type: "json" };
import {OrChart} from "../src/index";
import "../src/index";

const tagName = "or-chart";
type Story = StoryObj;
setCustomElementsManifest(customElements);
setStorybookHelpersConfig({});

const { events, args, argTypes, template } = getStorybookHelpers(tagName);

const meta: Meta = {
    title: "Playground/or-chart",
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
            description: "These line charts can be used as a graphical representation for data visualization.",
            story: {
                height: "370px"
            }
        }
    }
};

export const Primary = {
    args: {
        dataProvider: async () => ([{
            data: [
                {x: new Date(Date.now() - 60000 * 60).getTime(), y: 5},
                {x: new Date(Date.now() - 50000 * 60).getTime(), y: 8},
                {x: new Date(Date.now() - 40000 * 60).getTime(), y: 3},
                {x: new Date(Date.now() - 30000 * 60).getTime(), y: 4},
                {x: new Date(Date.now() - 20000 * 60).getTime(), y: 10},
                {x: new Date(Date.now() - 10000 * 60).getTime(), y: 2},
                {x: new Date().getTime(), y: 4}
            ]
        }])
    },
    loaders: [
        async (args: any) => ({
            orChart: await loadOrChart(args.allArgs)
        })
    ]
};

export const examples: Story[] = [];

export {customElements, packageJson};

/* ------------------------------------------------------- */
/*                   UTILITY FUNCTIONS                     */
/* ------------------------------------------------------- */

async function loadOrChart(args: any) {

    const newArgs = Object.fromEntries(Object.entries(args).filter(([key, value]) => (
        value != null && String(value).length > 0
    )));

    const orChart = Object.assign(new OrChart(), newArgs);

    return orChart;
}

export default meta;
