import {Page, PageProvider} from "./types";
import {css, html, TemplateResult} from "lit";
import {customElement} from "lit/decorators.js";
import {AppStateKeyed} from "./app";
import { Store } from "@reduxjs/toolkit";
import {i18next} from "@openremote/or-translate";

export function getPageOffline(store: Store<AppStateKeyed>): Page<AppStateKeyed> {
    return new OrOffline(store);
}

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
`

@customElement("or-offline")
export class OrOffline extends Page<AppStateKeyed> {

    public stateChanged(state: AppStateKeyed) {}

    static get styles() {
        return [styling]
    }

    protected render(): TemplateResult {
        return html`
            <div id="offline-wrapper">
                <or-icon id="offline-icon" icon="web-off"></or-icon>
                <div id="offline-text-container">
                    <span id="offline-title"">${i18next.t('youAreOffline')}</span>
                    <span>${i18next.t('checkConnection')}</span>
                </div>
            </div>
        `
    }

    get name(): string {
        return "";
    }
}
