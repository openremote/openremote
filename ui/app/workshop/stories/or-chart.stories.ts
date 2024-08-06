import type { Meta, StoryContext, StoryObj } from '@storybook/web-components';
import { getWcStorybookHelpers } from "wc-storybook-helpers";
import {OrChart} from "@openremote/or-chart";
import "@openremote/or-chart";
import {OrAssetViewer} from "@openremote/or-asset-viewer";
import { html } from 'lit';

const { events, args, argTypes, template } = getWcStorybookHelpers("or-chart");

const meta: Meta<OrChart> = {
    title: "Components/or-chart",
    component: "or-chart",
    args,
    argTypes: {
        ...argTypes,
        datapointQuery: {
            control: {
                type: "object"
            },
            description: "Query object to get data"
        }
    },
    parameters: {
        actions: {
            handles: events
        }
    }
};

type Story = StoryObj<OrChart & typeof args>;

export const Primary: Story = {
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
        async (args) => ({
            orChart: await loadOrChart(args.allArgs)
        })
    ],
    parameters: {
        docs: {
            source: {
                code: getExampleCode(),
                /*transform: (code: string, storyContext: StoryContext) => {
                    return code;
                }*/
            },
            story: {
                height: "370px"
            }
        }
    },
    render: (args, { loaded: { orChart }}) => html`${orChart}`
};

/* ------------------------------------------------------- */
/*                   UTILITY FUNCTIONS                     */
/* ------------------------------------------------------- */

function getExampleCode() {

    //language=javascript
    return `
import "@openremote/or-chart";

// If using Lit; you can use the dataProvider attribute to manually insert data;
const dataProvider = async () => ([{
    data: [
        {x: new Date(Date.now() - 10000).getTime(), y: 2},
        {x: new Date().getTime(), y: 4}
    ]
}])

// ----
// OR you can insert (already fetched) assets
const assets = [];

// And, specify what attributes of these assets to include in the chart.
// It uses the 'index within the assets array' and the attribute object.
const assetAttributes = [
    [0, {name: "temperature", type: "positiveInteger"}],
    [0, {name: "humidity", type: "positiveInteger"}],
    [1, {name: "temperature", type: "positiveInteger"}],
    [1, {name: "humidity", type: "positiveInteger"}]
];

// And use the datapointQuery attribute to specify what asset data to fetch
const datapointQuery = {
    type: "lttb", // algorithm to apply
    fromTimestamp: new Date(Date.now() - 10000).getTime(),
    toTimestamp: new Date().getTime(),
    amountOfPoints: 100
}
// -----

// in your HTML code use this, and inject them like this;
<or-chart dataProvider="{dataProvider}"></or-chart>


// (IF NOT USING LIT; you should parse the objects to JSON strings)
// const datapointQueryStr = JSON.stringify(datapointQuery);
// const assetsStr = JSON.stringify(assets);
// const assetAttributesStr = JSON.stringify(assetAttributes);

// ... and inject the JSON like this
// <or-chart assets="assetsStr" assetAttributes="assetAttributeStr" datapointQuery="datapointQuery" />
`
}

async function loadOrChart(args: any): Promise<HTMLElement | undefined> {

    const newArgs = Object.fromEntries(Object.entries(args).filter(([key, value]) => (
        value != null && String(value).length > 0
    )));

    const orChart = Object.assign(new OrChart(), newArgs) as OrChart;

    return orChart;
}

export default meta;