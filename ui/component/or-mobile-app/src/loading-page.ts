import {AppStateKeyed, Page} from "@openremote/or-app";
import {customElement} from "lit/decorators.js";
import {TemplateResult, html} from "lit";
import {Store} from "@reduxjs/toolkit";

export function loadingPageProvider(store: Store<AppStateKeyed>) {
    return {
        name: "loading",
        routes: [],
        hideHeader: true,
        pageCreator: () => new LoadingPage(store)
    };
}

@customElement("page-loading")
export class LoadingPage extends Page<AppStateKeyed> {

    get name(): string {
        return "loading";
    }

    stateChanged(state: AppStateKeyed): void {
    }

    protected render(): TemplateResult {
        return html`
            <div style="height: 100%; display: flex; justify-content: center; align-items: center;">
                <or-loading-indicator></or-loading-indicator>
            </div>
        `
    }

}