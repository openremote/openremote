import type { Meta, StoryObj } from '@storybook/web-components';
import { html } from "lit";
import {Store} from "@reduxjs/toolkit";
import {AppStateKeyed, OrApp, Page, PageProvider, router} from "@openremote/or-app";
import { customElement } from "lit/decorators.js";
import { getWcStorybookHelpers } from "wc-storybook-helpers";
import {DefaultStore, getManagerAppConfig, loadOrApp} from "./util/or-app-helpers";
import {getMarkdownString, splitLines} from "./util/util";
import ReadMe from "./temp/or-app-README.md";

const { events, args, argTypes, template } = getWcStorybookHelpers("or-app");

const meta: Meta<OrApp<any>> = {
    title: 'Internal/or-app',
    component: 'or-app',
    args,
    argTypes,
    parameters: {
        actions: {
            handles: events
        }
    },
};

type Story = StoryObj<any>;


/* ----------------------------------------------------------- */
/*                PRIMARY STORY w/ EXAMPLE PAGES               */
/* ----------------------------------------------------------- */

export const Primary: Story = {
    args: {
        appConfig: {
            pages: [examplePage1Provider(DefaultStore), examplePage2Provider(DefaultStore)]
        }
    },
    parameters: {
        docs: {
            readmeStr: splitLines(getMarkdownString(ReadMe), 1),
            source: {
                code: getExampleCode()
            },
            story: {
                height: '480px'
            }
        }
    },
    loaders: [
        async (args) => ({
            orApp: await loadOrApp(args.allArgs),
        })
    ],
    render: (args, { loaded: { orApp }}) => {
        return html`${orApp}`;
    }
};


/* ------------------------------------------------------- */
/*                    STORY w/ MANAGER                     */
/* ------------------------------------------------------- */

export const Manager: Story = {
    /*args: {
        appConfig: getDefaultAppConfig()
    },*/
    loaders: [
        async (args) => ({
            orApp: await loadOrApp(args.allArgs, async (app) => getManagerAppConfig(app)),
        })
    ],
    render: (args, { loaded: { orApp }}) => {
        return html`${orApp}`;
    }
};


/* ------------------------------------------------------- */
/*                   UTILITY FUNCTIONS                     */
/* ------------------------------------------------------- */

function getExampleCode() {

    //language=javascript
    return `
import {combineReducers, configureStore} from "@reduxjs/toolkit";
import {OrApp, appReducer} from "@openremote/or-app";
import "@openremote/or-app"; // this is necessary

const rootReducer = combineReducers({
    app: appReducer
});
const store = configureStore({
    reducer: rootReducer 
});
const orApp = new OrApp(store);
document.body.appendChild(orApp);
`
}

function examplePage1Provider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "page1",
        routes: ["page1"],
        pageCreator: () => {
            return new ExamplePage1(store);
        }
    };
}

function examplePage2Provider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "page2",
        routes: ["page2"],
        pageCreator: () => {
            return new ExamplePage2(store);
        }
    };
}

@customElement("page-1")
class ExamplePage1 extends Page<AppStateKeyed> {
    get name(): string { return ""; }
    stateChanged(state: AppStateKeyed): void {}
    render() {
        return html`
            <div style="display: flex; gap: 12px; align-items: center; height: fit-content;">
                <span>This is example page 1</span>
                <button @click="${() => router.navigate('page2')}">Navigate</button>
            </div>
        `;
    }
}

@customElement("page-2")
class ExamplePage2 extends Page<AppStateKeyed> {
    get name(): string { return ""; }
    stateChanged(state: AppStateKeyed): void {}
    render() {
        return html`
            <div style="display: flex; gap: 12px; align-items: center; height: fit-content;">
                <span>This is example page 2</span>
                <button @click="${() => router.navigate('page1')}">Navigate</button>
            </div>
        `;
    }
}

export default meta;