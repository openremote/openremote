import { getWcStorybookHelpers } from "wc-storybook-helpers";
import {createMdiIconSet, IconSets, OrIcon} from "@openremote/or-icon";
import "@openremote/or-icon";
import { html } from "lit";

const { events, args, argTypes, template } = getWcStorybookHelpers("or-icon");


/** @type { import('@storybook/web-components').Meta } */
const meta = {
    title: "Playground/or-icon",
    component: "or-icon",
    args: args,
    argTypes: argTypes,
    parameters: {
        actions: {
            handles: events
        }
    }
};

/** @type { import('@storybook/web-components').StoryObj } */
export const Primary = {
    args: {
        icon: "home"
    },
    loaders: [
        async (args) => ({
            orIcon: await loadOrIcon(args)
        })
    ],
    parameters: {
        docs: {
            /*readmeStr: "This is a test readmeStr ## Test",*/
            source: {
                code: getExampleCode()
            },
            story: {
                height: "60px"
            }
        }
    },
    render: (_args, { loaded: { orIcon }}) => html`${orIcon}`
};

/* ------------------------------------------------------- */
/*                   UTILITY FUNCTIONS                     */
/* ------------------------------------------------------- */

function getExampleCode() {

    //language=javascript
    return `
import "@openremote/or-icon";

// (OPTIONAL) initialize a custom icon set.
// OR if or-icon is not used in an OpenRemote (custom) app, you MUST initialize OrIconSets.
IconSets.addIconSet("mdi", createMdiIconSet('<managerUrl>'));

// in your HTML code use this, and inject them;
<or-icon icon="home"></or-icon>
`;
}

async function loadOrIcon(_args) {
    IconSets.addIconSet(
        "mdi",
        createMdiIconSet(window.location.href.includes("localhost") ? "http://localhost:8080" : window.location.origin)
    );
    const orIcon = new OrIcon();
    orIcon.icon = "home";
    return orIcon;
}

export default meta;
