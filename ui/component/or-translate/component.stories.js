import { getWcStorybookHelpers } from "wc-storybook-helpers";
import {i18next, OrTranslate} from "@openremote/or-translate";
import "@openremote/or-translate";
import i18nextBackend from "i18next-http-backend";
import { html } from 'lit';

const { events, args, argTypes, template } = getWcStorybookHelpers("or-translate");

/** @type { import('@storybook/web-components').Meta } */
const meta = {
    title: "Playground/or-translate",
    component: "or-translate",
    args,
    argTypes,
    parameters: {
        actions: {
            handles: events
        }
    }
};

/** @type { import('@storybook/web-components').Story } */
export const Primary = {
    args: {
        value: "monday"
    },
    loaders: [
        async (args) => ({
            orTranslate: await loadOrTranslate(args)
        })
    ],
    parameters: {
        docs: {
            source: {
                code: getExampleCode()
            },
            story: {
                height: '60px'
            }
        }
    },
    render: (args) => html`
        ${template(args)}
        ${template({value: "tuesday"})}
        ${template({value: "wednesday"})}
        ${template({value: "thursday"})}
        ${template({value: "friday"})}
        ${template({value: "saturday"})}
        ${template({value: "sunday"})}
    `
};

/* ------------------------------------------------------- */
/*                   UTILITY FUNCTIONS                     */
/* ------------------------------------------------------- */

function getExampleCode() {

    //language=javascript
    return `
import i18nextBackend from "i18next-http-backend"; // optional
import "@openremote/or-translate";

// (OPTIONAL) If not i18next is not initialized yet;
i18next.use(i18nextBackend).init({lng: 'en', backend: {loadPath: () => "/shared/locales/{{lng}}/{{ns}}.json"}});

// in your HTML code use this;
<or-translate value="monday"></or-translate>
`
}

/**
 * Initialises {@link OrTranslate}, awaits initialization, and returns the HTML object.
 */
async function loadOrTranslate(args) {
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
