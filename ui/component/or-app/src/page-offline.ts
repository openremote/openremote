import {Page, PageProvider} from "./types";
import {css, html, TemplateResult} from "lit";
import {customElement, state} from "lit/decorators.js";
import {AppStateKeyed} from "./app";
import {Store} from "@reduxjs/toolkit";
import {i18next} from "@openremote/or-translate";
import manager, {OREvent} from "@openremote/core";
import {asyncReplace} from 'lit/directives/async-replace.js';
import {when} from 'lit/directives/when.js';

// language=css
const styling = css`
    #offline-wrapper {
        display: flex;
        justify-content: center;
        align-items: center;
        height: 100%;
        width: 100%;
        flex-direction: column;
        gap: 32px;
        padding: 0 32px;
    }

    #offline-icon {
        font-size: 64px;
    }

    #offline-text-container {
        display: flex;
        align-items: center;
        flex-direction: column;
        gap: 16px;
    }

    #offline-title {
        font-size: 24px;
        font-weight: bold;
    }
    
    #reconnecting-text:after {
        display: inline-block;
        animation: dotty steps(2,end) 2s infinite;
        content: '';
    }

    @keyframes dotty {
        0%   { content: ''; }
        25%  { content: '.'; }
        50%  { content: '..'; }
        75%  { content: '...'; }
        100% { content: ''; }
    }
`

async function* countDown(count: number) {
    while (count > 0) {
        yield count--;
        await new Promise((r) => setTimeout(r, 1000));
    }
}

export function pageOfflineProvider(store: Store<AppStateKeyed>): PageProvider<AppStateKeyed> {
    return {
        name: "offline",
        routes: [
            "offline"
        ],
        pageCreator: () => {
            return new PageOffline(store);
        }
    };
}


@customElement("page-offline")
export class PageOffline extends Page<AppStateKeyed> {

    static get styles() {
        return [styling]
    }

    public stateChanged(state: AppStateKeyed) {
    }

    protected render(): TemplateResult {
        return html`
            <div id="offline-wrapper">
                <or-icon id="offline-icon" icon="web-off"></or-icon>
                <div id="offline-text-container">
                    <span id="offline-title""><or-translate value="youAreOffline"></or-translate></span>
                    <span id="offline-subtitle"><or-translate value="checkConnection"></or-translate></span>
                </div>
                <div>
                    <span><or-translate id="reconnecting-text" value="reconnecting"></or-translate></span>
                </div>
            </div>
        `
    }

    get name(): string {
        return "offline";
    }
}
