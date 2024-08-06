import type {Meta, StoryObj} from '@storybook/web-components';
import {html} from "lit";
import {Store} from "@reduxjs/toolkit";
import {AppStateKeyed, PageProvider, router} from "@openremote/or-app";
import {customElement} from 'lit/decorators.js';
import {getWcStorybookHelpers} from 'wc-storybook-helpers';
import {MobilePage, MobilePageAnimation, OrMobileApp} from "@openremote/or-mobile-app";
import {pageAssetsProvider} from "@openremote/manager/pages/page-assets";
import {DefaultStore, getManagerAppConfig, loadOrMobileApp} from "./util/or-app-helpers";
import { until } from "lit/directives/until.js";

const { events, args, argTypes, template } = getWcStorybookHelpers("or-mobile-app");

const meta: Meta<OrMobileApp<any>> = {
    title: 'Internal/or-mobile-app',
    component: 'or-mobile-app',
    args,
    argTypes,
    parameters: {
        actions: {
            handles: events
        }
    },
};

type Story = StoryObj<any>;


/* ---------------------------------------------------- */
/*                    PRIMARY STORY                     */
/* ---------------------------------------------------- */

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

@customElement("mobile-page-1")
class ExamplePage1 extends MobilePage<AppStateKeyed> {
    get name(): string { return "Mobile page 1"; }
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

@customElement("mobile-page-2")
class ExamplePage2 extends MobilePage<AppStateKeyed> {
    get name(): string { return "Mobile page 2"; }
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

export const Primary: Story = {
    args: {
        appConfig: {
            pages: [examplePage1Provider(DefaultStore), examplePage2Provider(DefaultStore)]
        }
    },
    parameters: {
        docs: {
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
            orApp: await loadOrMobileApp(args.allArgs),
        })
    ],
    render: (args, { loaded: { orApp }}) => {
        return html`
            ${orApp}
        `;
    }
};


/* ------------------------------------------------------- */
/*                    STORY w/ LOADING                     */
/* ------------------------------------------------------- */

function examplePage3Provider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "page3",
        routes: ["page3"],
        pageCreator: () => {
            return new ExamplePage3(store);
        }
    };
}

function examplePage4Provider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "page4",
        routes: ["page4"],
        pageCreator: () => {
            return new ExamplePage4(store);
        }
    };
}

@customElement("mobile-page-3")
class ExamplePage3 extends MobilePage<AppStateKeyed> {
    get name(): string { return "Mobile page 3"; }
    stateChanged(state: AppStateKeyed): void {}
    get enterAnimation(): MobilePageAnimation {
        return MobilePageAnimation.SWIPE_RIGHT;
    }
    get exitAnimation(): MobilePageAnimation {
        return MobilePageAnimation.SWIPE_RIGHT;
    }
    render() {
        return html`
            <div>
                <div style="display: flex; gap: 12px; align-items: center; height: fit-content;">
                    <span>This is example page 3</span>
                    <button @click="${() => router.navigate('page4')}">Navigate</button>
                </div>
                <img src="https://openremote.io/wp-content/uploads/2023/01/Product_mobile_IoT-1024x600-1.png" style="width: 100%;" />
            </div>
        `;
    }
}

@customElement("mobile-page-4")
class ExamplePage4 extends MobilePage<AppStateKeyed> {
    get name(): string { return "Mobile page 4"; }
    stateChanged(state: AppStateKeyed): void {}
    async connectedCallback() {
        await new Promise(resolve => setTimeout(resolve, 1000));
        super.connectedCallback();
    }
    get enterAnimation(): MobilePageAnimation {
        return MobilePageAnimation.SWIPE_RIGHT;
    }
    get exitAnimation(): MobilePageAnimation {
        return MobilePageAnimation.SWIPE_RIGHT;
    }
    render() {
        return html`
            <div>
                <div style="display: flex; gap: 12px; align-items: center; height: fit-content;">
                    <span>This is example page 4</span>
                    <button @click="${() => router.navigate('page3')}">Navigate</button>
                </div>
                <img src="https://openremote.io/wp-content/uploads/2022/12/OpenRemote-Edge-Gateways.png" style="width: 100%;" />
            </div>
        `;
    }
}

export const WithLoader: Story = {
    args: {
        appConfig: {
            pages: [examplePage3Provider(DefaultStore), examplePage4Provider(DefaultStore)]
        },
        /*loadingPageProvider: loadingPage1Provider(store)*/
    },
    loaders: [
        async (args) => ({
            orApp: await loadOrMobileApp(args.allArgs),
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
        appConfig: {
            pages: [
                pageAssetsProvider(DefaultStore as any)
            ]
        },
    },*/
    loaders: [
        async (args) => ({
            orMobileApp: await loadOrMobileApp(args.allArgs, async (app) => getManagerAppConfig(app)),
        })
    ],
    render: (args, { loaded: { orMobileApp }}) => {
        return html`
            ${orMobileApp}
        `;
    }
};


/* ------------------------------------------------------- */

function getExampleCode() {

    //language=javascript
    return `
import {combineReducers, configureStore} from "@reduxjs/toolkit";
import {appReducer} from "@openremote/or-app";
import {OrMobileApp} from "@openremote/or-mobile-app";
import "@openremote/or-mobile-app"; // this is necessary

const rootReducer = combineReducers({
    app: appReducer
});
const store = configureStore({
    reducer: rootReducer 
});
const orMobileApp = new OrMobileApp(store);
document.body.appendChild(orMobileApp);
`
}

export default meta;
