import {PropertyValues} from "lit";
import { InitOptions, i18n } from "i18next";

declare type Constructor<T> = new (...args: any[]) => T;

interface CustomElement {
    connectedCallback?(): void;
    disconnectedCallback?(): void;
    readonly isConnected: boolean;
}

// TODO: Can't currently export declaration files with explicit LitElement type (see https://github.com/Microsoft/TypeScript/issues/17293)
export const translate = (i18next: i18n) => <T extends Constructor<CustomElement>>(base: T) =>
        class extends base {

            _i18nextJustInitialized = false;

            connectedCallback() {
                if (!i18next.language) {
                    i18next.on("initialized", this.initCallback);
                }

                i18next.on("languageChanged", this.langChangedCallback);

                if (super.connectedCallback) {
                    super.connectedCallback();
                }
            }

            disconnectedCallback() {
                i18next.off("initialized", this.initCallback);
                i18next.off("languageChanged", this.langChangedCallback);

                if (super.disconnectedCallback) {
                    super.disconnectedCallback();
                }
            }

            shouldUpdate(changedProps: PropertyValues) {
                if (this._i18nextJustInitialized) {
                    this._i18nextJustInitialized = false;
                    return true;
                }

                // @ts-ignore
                return super.shouldUpdate && super.shouldUpdate(changedProps);
            }

            public initCallback = (options: InitOptions) => {
                this._i18nextJustInitialized = true;
                // @ts-ignore
                if (this.requestUpdate) {
                    // @ts-ignore
                    this.requestUpdate();
                }
            };

            public langChangedCallback = () => {
                // @ts-ignore
                if (this.requestUpdate) {
                    // @ts-ignore
                    this.requestUpdate();
                }
            }
        };
