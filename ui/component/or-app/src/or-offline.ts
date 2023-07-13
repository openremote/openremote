import {Page} from "./types";
import {css, html, TemplateResult} from "lit";
import {customElement, state} from "lit/decorators.js";
import {AppStateKeyed} from "./app";
import {Store} from "@reduxjs/toolkit";
import {i18next} from "@openremote/or-translate";
import manager, {EventCallback, OREvent} from "@openremote/core";
import {asyncReplace} from 'lit/directives/async-replace.js';
import {when} from 'lit/directives/when.js';

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

async function* countDown(count: number) {
    while (count > 0) {
        yield count--;
        await new Promise((r) => setTimeout(r, 1000));
    }
}


@customElement("or-offline")
export class OrOffline extends Page<AppStateKeyed> {

    @state()
    protected timer?: AsyncGenerator<number>;

    protected eventCallback?: EventCallback;

    static get styles() {
        return [styling]
    }

    public stateChanged(state: AppStateKeyed) {
        this.timer = state.app.offline ? countDown(10) : undefined;
    }

    connectedCallback() {
        super.connectedCallback();
        this.eventCallback = (ev) => {
            if (ev === OREvent.AUTH_REFRESH_FAILED) {
                this.timer = countDown(10); // reset countdown
            } else if (ev === OREvent.AUTH_REFRESH_SUCCESS) {
                this.timer = undefined;
            }
        };
        manager.addListener(this.eventCallback);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        if (this.eventCallback !== undefined) {
            manager.removeListener(this.eventCallback);
        }
    }

    protected render(): TemplateResult {
        return html`
            <div id="offline-wrapper">
                <or-icon id="offline-icon" icon="web-off"></or-icon>
                <div id="offline-text-container">
                    <span id="offline-title"">${i18next.t('youAreOffline')}</span>
                    <span>${i18next.t('checkConnection')}</span>
                </div>
                <!-- Countdown to when it tries reconnecting again -->
                ${when(this.timer, () => {
                    const splitMsg = i18next.t("retryingConnection").split('{{seconds}}');
                    return html`${splitMsg[0]} ${asyncReplace(this.timer!)} ${splitMsg[1]}`;
                })}
            </div>
        `
    }

    get name(): string {
        return "";
    }
}
