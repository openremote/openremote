import type { Meta, StoryObj } from '@storybook/web-components';
import { getWcStorybookHelpers } from "wc-storybook-helpers";
import {OrAssetTree} from "@openremote/or-asset-tree";
import openremote, {manager} from "@openremote/core";
import { html } from "lit";
import {getMarkdownString, splitLines} from "./util/util";
import ReadMe from "./temp/or-asset-tree-README.md";
import {OrApp} from "@openremote/or-app";

const { events, args, argTypes, template } = getWcStorybookHelpers("or-asset-tree");

const meta: Meta<OrAssetTree> = {
    title: "Components/or-asset-tree",
    component: "or-asset-tree",
    args,
    argTypes,
    parameters: {
        actions: {
            handles: events
        }
    }
};

type Story = StoryObj<OrAssetTree & typeof args>;

export const Primary: Story = {
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
            readmeStr: splitLines(getMarkdownString(ReadMe), 1),
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

async function loadOrAssetTree(args: any): Promise<HTMLElement | undefined> {

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

    return Object.assign(new OrAssetTree(), newArgs) as OrAssetTree;
}

export default meta;
