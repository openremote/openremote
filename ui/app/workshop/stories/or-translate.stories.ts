import type { Meta, StoryObj } from '@storybook/web-components';
import { getWcStorybookHelpers } from "wc-storybook-helpers";
import {i18next, OrTranslate} from "@openremote/or-translate";
import "@openremote/or-translate";
import i18nextBackend from "i18next-http-backend";
import { html } from 'lit';
import {getMarkdownString, splitLines} from "./util/util";
import ReadMe from "./temp/or-translate-README.md";

const { events, args, argTypes, template } = getWcStorybookHelpers("or-translate");

const meta: Meta<OrTranslate> = {
    title: "Components/or-translate",
    component: "or-translate",
    args,
    argTypes,
    parameters: {
        actions: {
            handles: events
        }
    }
};

type Story = StoryObj<OrTranslate & typeof args>;

export const Primary: Story = {
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
            readmeStr: splitLines(getMarkdownString(ReadMe), 1),
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
async function loadOrTranslate(args: any): Promise<OrTranslate> {
    console.debug("Loading OrTranslate...");
    const orTranslate = Object.assign(new OrTranslate(), args) as OrTranslate;
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