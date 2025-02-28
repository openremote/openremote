import { getWcStorybookHelpers } from "wc-storybook-helpers";
import {OrAssetTree} from "@openremote/or-asset-tree";
import openremote, {manager} from "@openremote/core";
import { html } from "lit";

const { events, args, argTypes, template } = getWcStorybookHelpers("or-asset-tree");

/** @type { import('@storybook/web-components').Meta } */
const meta = {
    title: "Playground/or-asset-tree",
    component: "or-asset-tree",
    args,
    argTypes,
    parameters: {
        actions: {
            handles: events
        }
    }
};

/** @type { import('@storybook/web-components').Meta } */
export const Primary = {
    /*args: {
        dataProvider: async () => ([
            { id: "id1", name: "Asset 1" }
        ])
    },*/
    loaders: [
        async (args) => ({
            orAssetTree: await loadOrAssetTree(args.allArgs)
        })
    ],
    parameters: {
        docs: {
            source: {
                code: getExampleCode()
            },
            story: {
                height: "480px"
            }
        }
    },
    render: (args, { loaded: { orAssetTree }}) => html`${orAssetTree}`
};

/* ------------------------------------------------------- */
/*                   UTILITY FUNCTIONS                     */
/* ------------------------------------------------------- */

function getExampleCode() {

    //language=javascript
    return `
import openremote from "@openremote/core";
import "@openremote/or-asset-tree";

// Wait for OpenRemote JS library initialization
await openremote.init({
    managerUrl: "http://localhost:8080",
    keycloakUrl: "http://localhost:8080/auth"
});

// Prompt login screen if necessary
if(!manager.authenticated) {
    manager.login();
    return;
}

// in your HTML code use this;
<or-asset-tree style="height: 400px;"></or-asset-tree>
`;
}

async function loadOrAssetTree(args) {

    await manager.init({
        managerUrl: "http://localhost:8080",
        keycloakUrl: "http://localhost:8080/auth"
    });

    if(!manager.authenticated) {
        console.debug("Prompting login!");
        manager.login();
        return;
    }

    const newArgs = Object.fromEntries(Object.entries(args).filter(([key, value]) => (
        value != null && String(value).length > 0
    )));

    return Object.assign(new OrAssetTree(), newArgs);
}

export default meta;
