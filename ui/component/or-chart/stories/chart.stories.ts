import {type Meta, setCustomElementsManifest, type StoryObj} from "@storybook/web-components";
import {getStorybookHelpers} from "@wc-toolkit/storybook-helpers";
import {AssetQuery, Attribute, WellknownMetaItems} from "@openremote/model";
import manager from "@openremote/core";
import {html} from "lit";
import customElements from "../custom-elements.json" with {type: "json"};
import packageJson from "../package.json" with {type: "json"};
import {OrChart} from "../src/index";
import "../src/index";

const tagName = "or-chart";
type Story = StoryObj;
setCustomElementsManifest(customElements);

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

export const Primary: Story = {
    render: (args: any) => html`
        ${template(args)}
        <script>
            const component = document.querySelector('or-chart');
            component.style.height = "300px";
            component.attributeControls = false;
            component.timestampControls = false;
            // component.timeframe = [new Date(Date.now() - (60000 * 60)), new Date()]
            component.dataProvider = async () => ([{
                type: 'line',
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
        </script>
    `,
    loaders: [
        async (args: any) => ({
            orChart: await loadOrChart(args.allArgs)
        })
    ]
};

export const AssetsExample: Story = {
    name: "Asset example",
    parameters: {
        title: "Using asset data",
        summary: "You can show historic asset data in the chart using the `assets`, `assetAttributes`, `attributeColors`, `datapointQuery` and a `timeframe`."
    },
    args: {
        datapointQuery: JSON.stringify({ type: "lttb", amountOfPoints: 100, fromTimestamp: Date.now() - 120000 * 60, toTimestamp: Date.now() })
    },
    render: (args, { loaded }) => {
        const query: AssetQuery = { limit: 1, attributes: { items: [{ meta: [{ name: { predicateType: "string", value: WellknownMetaItems.STOREDATAPOINTS }}]}]}};
        loaded.orManager.rest.api.AssetResource.queryAssets(query).then((response: any) => {
            const component = document.querySelector("or-chart") as OrChart;
            if(response.data.length === 0) {
                console.error("There are no attributes in the Manager that have data points.");
                return;
            }
            console.debug("Displaying asset: ", response.data[0]);
            const datapointAttr = Object.values(response.data[0].attributes).find((attr: any) => Object.keys(attr.meta).includes(WellknownMetaItems.STOREDATAPOINTS)) as Attribute<any>;
            component.assets = response.data;
            component.assetAttributes = [[0, datapointAttr]];
            component.attributeColors = [[{ id: component.assets[0].id, name: datapointAttr.name }, OrChart.DEFAULT_COLORS[0]]];
            component.timeframe = [new Date(Date.now() - (60000 * 60)), new Date()];
        });
        return html`
            ${template(args)}
            <script>
                const component = document.querySelector('or-chart');
                component.style.height = "300px";
                component.attributeControls = false;
            </script>
        `;
    },
    loaders: [
        async (args: any) => ({
            orChart: await loadOrChart(args.allArgs),
            orManager: await loadOrManager()
        })
    ]
}

export const examples: Story[] = [AssetsExample];

export {customElements, packageJson};

/* ------------------------------------------------------- */
/*                   UTILITY FUNCTIONS                     */
/* ------------------------------------------------------- */

async function loadOrChart(args: any) {
    const newArgs = Object.fromEntries(Object.entries(args).filter(([_key, value]) => (
        value != null && String(value).length > 0
    )));
    return Object.assign(new OrChart(), newArgs);
}

async function loadOrManager() {
    if(await manager.init({ managerUrl: "http://localhost:8080", realm: "smartcity" })) {
        if(!manager.authenticated) manager.login();
        return manager;
    }
    throw new Error("Manager could not be initialized");
}

export default meta;
