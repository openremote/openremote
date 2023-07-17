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
    protected _timer?: AsyncGenerator<number | undefined>;

    protected _eventCallback?: EventCallback;

    static get styles() {
        return [styling]
    }

    public stateChanged(state: AppStateKeyed) {
        if(state.app.offline) {
            this._startTimer(10);
        }
    }

    connectedCallback() {
        super.connectedCallback();
        this._eventCallback = (ev) => {
            if (ev === OREvent.CONNECTING) {
                this._startTimer(10);
            }
        };
        manager.addListener(this._eventCallback);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        if (this._eventCallback !== undefined) {
            manager.removeListener(this._eventCallback);
        }
    }

    protected _startTimer(seconds: number) {
        const timer = countDown(seconds);
        this._timer = timer;
        setTimeout(() => {
            this._stopTimer(timer); // stop after the amount of seconds is passed.
        }, seconds * 1000)
    }

    // Stopping the timer/timeout if it is the current active timer
    protected _stopTimer(timer: AsyncGenerator<number | undefined>) {
        if(this._timer === timer) {
            delete this._timer;
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
                ${when(this._timer, () => {
                    const splitMsg = i18next.t("retryingConnection").split('{{seconds}}');
                    return html`${splitMsg[0]} ${asyncReplace(this._timer!)} ${splitMsg[1]}`;
                }, () => html`Reconnecting...`)}
            </div>
        `
    }

    get name(): string {
        return "";
    }
}
