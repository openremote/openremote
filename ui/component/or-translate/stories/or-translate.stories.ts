import {setCustomElementsManifest, type Meta, type StoryObj } from "@storybook/web-components";
import { getStorybookHelpers, setStorybookHelpersConfig } from "@wc-toolkit/storybook-helpers";
import customElements from "../custom-elements.json" with { type: "json" };
import packageJson from "../package.json" with { type: "json" };
import i18nextBackend from "i18next-http-backend";
import {i18next, OrTranslate} from "../src";
import "../src/index";

const tagName = "or-translate";
type Story = StoryObj;
setCustomElementsManifest(customElements);
setStorybookHelpersConfig({});

const { events, args, argTypes, template } = getStorybookHelpers(tagName);

const meta: Meta = {
    title: "Playground/or-translate",
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
            description: "Useful elements for automatically translating text for in UI apps",
        }
    }
};

export const Primary = {
    args: {
        value: "monday"
    },
    loaders: [
        async (args: any) => ({
            orTranslate: await loadOrTranslate(args.allArgs)
        })
    ]
};

export {customElements, packageJson};

/* ------------------------------------------------------- */
/*                   UTILITY FUNCTIONS                     */
/* ------------------------------------------------------- */

/**
 * Initialises {@link OrTranslate}, awaits initialization, and returns the HTML object.
 */
async function loadOrTranslate(args: any) {
    console.debug("Loading OrTranslate...");
    const orTranslate = Object.assign(new OrTranslate(), args);
    console.debug("Waiting for i18next initialization...");
    if(window.location.href.includes("localhost")) {
        await i18next.use(i18nextBackend).init({lng: 'en', backend: {loadPath: () => "http://localhost:8080/shared/locales/{{lng}}/or.json"}});
    } else {
        await i18next.use(i18nextBackend).init({lng: 'en', backend: {loadPath: () => "/shared/locales/{{lng}}/or.json"}});
    }
    console.debug("OrTranslate loaded");
    return orTranslate;
}

export default meta;
