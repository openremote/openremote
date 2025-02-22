import { getWcStorybookHelpers } from "wc-storybook-helpers";
import openremote, {manager} from "@openremote/core";
import { html } from "lit";
import {OrAssetViewer} from "@openremote/or-asset-viewer";

const { events, args, argTypes, template } = getWcStorybookHelpers("or-asset-viewer");

/** @type { import('@storybook/web-components').Meta } */
const meta = {
    title: "Playground/or-asset-viewer",
    component: "or-asset-viewer",
    args,
    argTypes: {
        ...argTypes,
    },
    parameters: {
        actions: {
            handles: events
        }
    }
};

/** @type { import('@storybook/web-components').StoryObj } */
export const Primary = {
    loaders: [
        async (args) => ({
            orAssetViewer: await loadOrAssetViewer(args.allArgs)
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
    render: (args, { loaded: { orAssetViewer }}) => html`${orAssetViewer}`
};

/* ------------------------------------------------------- */
/*                   UTILITY FUNCTIONS                     */
/* ------------------------------------------------------- */

function getExampleCode() {

    //language=javascript
    return `
import openremote from "@openremote/core";
import "@openremote/or-asset-viewer";

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

// (optional) Query an asset ID to show
const assets = (await manager.rest.api.AssetResource.queryAssets({
    realm: { name: "master" }, // or any other realm
    limit: 1
})).data;
const assetIds = assets.map(a => a.id);

// in your HTML code use this, and inject the ids;
<or-asset-viewer assetIds="{assetIds}"></or-asset-viewer>
`;
}

async function loadOrAssetViewer(args) {

    await manager.init({
        managerUrl: "http://localhost:8080",
        keycloakUrl: "http://localhost:8080/auth"
    });

    if(!manager.authenticated) {
        console.debug("Prompting login!");
        manager.login();
        return;
    }

    const assets = (await manager.rest.api.AssetResource.queryAssets({
        realm: { name: "master" },
        limit: 1
    })).data;
    const assetIds = assets.map(a => a.id);

    const newArgs = Object.fromEntries(Object.entries(args).filter(([key, value]) => (
        value != null && String(value).length > 0
    )));

    const orAssetViewer = Object.assign(new OrAssetViewer(), newArgs);
    if(!orAssetViewer.id) {
        orAssetViewer.id = assetIds[0];
    }

    return orAssetViewer;
}

export default meta;
